package com.linkdeck.app.pairing

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer

/** Decodes QR codes from camera frames. Pure ZXing core — no GMS, no Play Services. */
class QrAnalyzer(private val onCode: (String) -> Unit) : ImageAnalysis.Analyzer {

    private val reader = MultiFormatReader()

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: return imageProxy.close()
        val rotation = imageProxy.imageInfo.rotationDegrees

        try {
            val yBuffer = mediaImage.planes[0].buffer
            val yData = ByteArray(yBuffer.remaining())
            yBuffer.get(yData)

            val (data, width, height) = if (rotation == 90 || rotation == 270) {
                val (rotated, w, h) = rotate(yData, imageProxy.width, imageProxy.height)
                Triple(rotated, w, h)
            } else {
                Triple(yData, imageProxy.width, imageProxy.height)
            }

            val source = PlanarYUVLuminanceSource(
                data, width, height, 0, 0, width, height, false,
            )
            val result = reader.decodeWithState(
                BinaryBitmap(HybridBinarizer(source)),
                mapOf(DecodeHintType.TRY_HARDER to true),
            )
            onCode(result.text)
        } catch (_: NotFoundException) {
            // Not a QR frame — keep scanning.
        } catch (_: Exception) {
            // Malformed frame — drop it.
        } finally {
            imageProxy.close()
        }
    }

    /** Transposes the Y plane for 90/270 degree sensor orientation. */
    private fun rotate(data: ByteArray, width: Int, height: Int): Triple<ByteArray, Int, Int> {
        val rotated = ByteArray(data.size)
        for (y in 0 until height) {
            for (x in 0 until width) {
                rotated[x * height + (height - 1 - y)] = data[y * width + x]
            }
        }
        return Triple(rotated, height, width)
    }
}

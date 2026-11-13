package com.linkdeck.app.ui.mode

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linkdeck.app.settings.AppMode
import com.linkdeck.app.settings.SettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class ModeSelectionViewModel @Inject constructor(
    val settings: SettingsStore
) : ViewModel() {
    fun setMode(mode: AppMode, onDone: () -> Unit) {
        viewModelScope.launch {
            settings.setAppMode(mode) // ✅ استخدام الدالة الصح
            onDone()
        }
    }
}

@Composable
fun ModeSelectionScreen(
    onModeChosen: () -> Unit,
    vm: ModeSelectionViewModel = hiltViewModel()
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0E0B1F))
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .semantics { contentDescription = "LinkDeck logo animation" },
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.size(200.dp).border(1.dp, Color(0xFF2B2650), CircleShape))
            Box(modifier = Modifier.size(150.dp).border(1.dp, Color(0xFF4A4290), CircleShape))
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(scale)
                    .background(Color(0xFF5B4BD6), CircleShape)
                    .border(2.dp, Color(0xFF7B6FE6), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "LD",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "LinkDeck",
            color = Color(0xFFEDEBFF),
            fontSize = 36.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.5.sp
        )
        
        Text(
            text = "اختر وضع التشغيل لهذا الجهاز",
            color = Color(0xFFA9A4CF),
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 26.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // زرار Host
        Button(
            onClick = { vm.setMode(AppMode.HOST, onModeChosen) },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .semantics { contentDescription = "Select Host mode for phone" },
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5B4BD6))
        ) {
            Text(
                text = "جهاز مضيف (Host)",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            )
        }
        
        // زرار Controller
        OutlinedButton(
            onClick = { vm.setMode(AppMode.CONTROLLER, onModeChosen) },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .semantics { contentDescription = "Select Controller mode for tablet" },
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB9AFFF)),
            border = BorderStroke(1.5.dp, Color(0xFF2B2650))
        ) {
            Text(
                text = "جهاز متحكم (Controller)",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

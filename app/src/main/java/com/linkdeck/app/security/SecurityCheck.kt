package com.linkdeck.app.security
object SecurityCheck {
    fun isDeviceRooted(): Boolean {
        val paths = arrayOf(
            "/system/bin/su", "/system/xbin/su", "/sbin/su",
            "/system/app/Superuser.apk", "/magisk/.core/bin/su",
        )
        if (paths.any { java.io.File(it).exists() }) return true
        return runCatching {
            val p = Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
            val found = p.inputStream.bufferedReader().readLine() != null
            p.destroy()
            found
        }.getOrDefault(false)
    }
    fun isTestBuild(): Boolean =
        android.os.Build.TAGS?.contains("test-keys") == true
}

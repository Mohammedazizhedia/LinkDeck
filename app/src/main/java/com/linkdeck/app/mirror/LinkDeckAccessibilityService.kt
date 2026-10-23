package com.linkdeck.app.mirror

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.linkdeck.core.network.HostBridge

/**
 * HOST-side accessibility service (Normal mode): executes tap/swipe/key
 * gestures coming from the controller via the cmd channel.
 */
class LinkDeckAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        HostBridge.accessibilityService = this
    }

    override fun onDestroy() {
        if (HostBridge.accessibilityService === this) HostBridge.accessibilityService = null
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit
}

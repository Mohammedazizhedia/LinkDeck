package com.linkdeck.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.linkdeck.app.settings.AppMode
import com.linkdeck.app.ui.apps.AppsScreen
import com.linkdeck.app.ui.calls.CallsScreen
import com.linkdeck.app.ui.devices.DevicesScreen
import com.linkdeck.app.ui.files.FilesScreen
import com.linkdeck.app.ui.guide.BackgroundGuideScreen
import com.linkdeck.app.ui.home.ControllerHomeScreen
import com.linkdeck.app.ui.home.HostHomeScreen
import com.linkdeck.app.ui.messages.MessagesScreen
import com.linkdeck.app.ui.mirror.MirrorScreen
import com.linkdeck.app.ui.mode.ModeSelectionScreen
import com.linkdeck.app.ui.notifications.NotificationsScreen
import com.linkdeck.app.ui.pairing.ScanQrScreen
import com.linkdeck.app.ui.pairing.ShowQrScreen
import com.linkdeck.app.ui.permissions.PermissionCenterScreen
import com.linkdeck.app.ui.pro.ProWizardScreen
import com.linkdeck.app.ui.settings.NavigationCustomizeScreen
import com.linkdeck.app.ui.settings.SettingsScreen

object Routes {
    const val MODE = "mode"
    const val HOME = "home"
    const val DEVICES = "devices"
    const val SHOW_QR = "pair/show_qr"
    const val SCAN_QR = "pair/scan_qr"
    const val PERMISSIONS = "permissions"
    const val SETTINGS = "settings"
    const val MIRROR = "mirror"
    const val FILES = "files"
    const val NOTIFICATIONS = "notifications"
    const val MESSAGES = "messages"
    const val CALLS = "calls"
    const val APPS = "apps"
    const val PRO_WIZARD = "pro_wizard"
    const val GUIDE = "settings/background"
    const val CUSTOMIZE = "settings/nav"
}

@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.MODE) {
        composable(Routes.MODE) {
            ModeSelectionScreen(onModeChosen = {
                navController.navigate(Routes.HOME) { popUpTo(Routes.MODE) { inclusive = true } }
            })
        }
        composable(Routes.HOME) { HomeRouter(navController) }
        composable(Routes.DEVICES) { DevicesScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.SHOW_QR) { ShowQrScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.SCAN_QR) { ScanQrScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.PERMISSIONS) { PermissionCenterScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenGuide = { navController.navigate(Routes.GUIDE) },
                onOpenCustomize = { navController.navigate(Routes.CUSTOMIZE) },
            )
        }
        composable(Routes.MIRROR) { MirrorScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.FILES) { FilesScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.NOTIFICATIONS) { NotificationsScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.MESSAGES) { MessagesScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.CALLS) { CallsScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.APPS) { AppsScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.PRO_WIZARD) { ProWizardScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.GUIDE) { BackgroundGuideScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.CUSTOMIZE) { NavigationCustomizeScreen(onBack = { navController.popBackStack() }) }
    }
}

@Composable
private fun HomeRouter(navController: NavHostController) {
    val vm: MainViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val mode by vm.settings.appMode.collectAsState()
    val navigate: (String) -> Unit = { route ->
        if (navController.currentDestination?.route != route) navController.navigate(route)
    }
    when (mode) {
        AppMode.HOST -> HostHomeScreen(
            onShowQr = { navigate(Routes.SHOW_QR) },
            onOpenProWizard = { navigate(Routes.PRO_WIZARD) },
            onOpenPermissions = { navigate(Routes.PERMISSIONS) },
            onOpenSettings = { navigate(Routes.SETTINGS) },
        )
        AppMode.CONTROLLER -> ControllerHomeScreen(
            onScanQr = { navigate(Routes.SCAN_QR) },
            onNavigate = navigate,
            onOpenPermissions = { navigate(Routes.PERMISSIONS) },
            onOpenSettings = { navigate(Routes.SETTINGS) },
        )
        null -> ModeSelectionScreen(onModeChosen = {})
    }
}

/** Kept for older call-sites; all features are wired to real screens now. */
@Composable
fun PlaceholderScreen(titleRes: Int) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

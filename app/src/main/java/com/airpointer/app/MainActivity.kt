package com.airpointer.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.airpointer.app.bluetooth.AirPointerService
import com.airpointer.app.bluetooth.HidReportDescriptor
import com.airpointer.app.data.InputMode
import com.airpointer.app.ui.navigation.AppTab
import com.airpointer.app.ui.screens.ConnectionScreen
import com.airpointer.app.ui.screens.KeyboardScreen
import com.airpointer.app.ui.screens.SettingsScreen
import com.airpointer.app.ui.screens.TouchpadScreen
import com.airpointer.app.ui.theme.AirPointerTheme
import com.airpointer.app.ui.theme.BgDark
import com.airpointer.app.ui.theme.CyanAccent
import com.airpointer.app.ui.theme.SurfaceDark
import com.airpointer.app.ui.theme.SurfaceElevated
import com.airpointer.app.ui.theme.TextSecondary
import com.airpointer.app.ui.viewmodels.ConnectionViewModel
import com.airpointer.app.ui.viewmodels.KeyboardViewModel
import com.airpointer.app.ui.viewmodels.SettingsViewModel
import com.airpointer.app.ui.viewmodels.TouchpadViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var app: AirPointerApplication

    private val requiredPermissions: Array<String>
        get() {
            val list = mutableListOf<String>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                list.add(Manifest.permission.BLUETOOTH_CONNECT)
                list.add(Manifest.permission.BLUETOOTH_SCAN)
                list.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            } else {
                list.add(Manifest.permission.ACCESS_FINE_LOCATION)
                list.add(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                list.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            return list.toTypedArray()
        }

    private fun checkAllPermissionsGranted(): Boolean {
        return requiredPermissions.all { perm ->
            ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        app = application as AirPointerApplication

        setContent {
            AirPointerTheme {
                var hasPermissions by remember { mutableStateOf(checkAllPermissionsGranted()) }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) {
                    hasPermissions = checkAllPermissionsGranted()
                    if (hasPermissions) {
                        app.hidManager.initialize()
                        AirPointerService.startService(this@MainActivity)
                    }
                }

                LaunchedEffect(Unit) {
                    if (!hasPermissions) {
                        permissionLauncher.launch(requiredPermissions)
                    } else {
                        app.hidManager.initialize()
                        AirPointerService.startService(this@MainActivity)
                    }
                }

                LaunchedEffect(hasPermissions) {
                    if (hasPermissions) {
                        AirPointerService.startService(this@MainActivity)
                    }
                }

                val connectionViewModel = remember { ConnectionViewModel(app.hidManager) }
                val touchpadViewModel = remember {
                    TouchpadViewModel(app.hidManager, app.settingsRepository, app.hapticHelper, app.airMouseManager)
                }
                val keyboardViewModel = remember {
                    KeyboardViewModel(app.hidManager, app.settingsRepository, app.hapticHelper)
                }
                val settingsViewModel = remember {
                    SettingsViewModel(app.settingsRepository, app.hidManager, app.airMouseManager)
                }

                val userSettings by app.settingsRepository.settings.collectAsState()

                // Keep screen on logic
                LaunchedEffect(userSettings.keepScreenOn) {
                    if (userSettings.keepScreenOn) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }

                var currentTab by remember { mutableStateOf(AppTab.TOUCHPAD) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = BgDark,
                    bottomBar = {
                        AirPointerBottomNav(
                            currentTab = currentTab,
                            onTabSelected = { currentTab = it }
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        Crossfade(targetState = currentTab, label = "TabCrossfade") { tab ->
                            when (tab) {
                                AppTab.DEVICES -> ConnectionScreen(
                                    viewModel = connectionViewModel,
                                    hasPermissions = hasPermissions,
                                    onRequestPermissions = { permissionLauncher.launch(requiredPermissions) }
                                )
                                AppTab.TOUCHPAD -> TouchpadScreen(
                                    viewModel = touchpadViewModel
                                )
                                AppTab.KEYBOARD -> KeyboardScreen(
                                    viewModel = keyboardViewModel
                                )
                                AppTab.SETTINGS -> SettingsScreen(
                                    viewModel = settingsViewModel
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val mode = app.settingsRepository.settings.value.inputMode
        if (mode == InputMode.AIR_MOUSE || mode == InputMode.DESK_MOUSE) {
            app.airMouseManager.start()
        }
    }

    override fun onPause() {
        super.onPause()
        app.airMouseManager.stop()
    }

    // Intercept hardware volume keys when in app
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val settings = app.settingsRepository.settings.value
        val target = app.hidManager.connectedDevice.value

        if (settings.volumeButtonsAsMouse && target != null) {
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    val leftBtn = if (settings.swapLeftRight) {
                        HidReportDescriptor.MOUSE_BUTTON_RIGHT
                    } else {
                        HidReportDescriptor.MOUSE_BUTTON_LEFT
                    }
                    if (event?.repeatCount == 0) {
                        app.hapticHelper.click(settings.hapticFeedback)
                        lifecycleScope.launch {
                            app.hidManager.mouseSender.setButton(target, leftBtn, true)
                        }
                    }
                    return true
                }
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    val rightBtn = if (settings.swapLeftRight) {
                        HidReportDescriptor.MOUSE_BUTTON_LEFT
                    } else {
                        HidReportDescriptor.MOUSE_BUTTON_RIGHT
                    }
                    if (event?.repeatCount == 0) {
                        app.hapticHelper.click(settings.hapticFeedback)
                        lifecycleScope.launch {
                            app.hidManager.mouseSender.setButton(target, rightBtn, true)
                        }
                    }
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        val settings = app.settingsRepository.settings.value
        val target = app.hidManager.connectedDevice.value

        if (settings.volumeButtonsAsMouse && target != null) {
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    val leftBtn = if (settings.swapLeftRight) {
                        HidReportDescriptor.MOUSE_BUTTON_RIGHT
                    } else {
                        HidReportDescriptor.MOUSE_BUTTON_LEFT
                    }
                    lifecycleScope.launch {
                        app.hidManager.mouseSender.setButton(target, leftBtn, false)
                    }
                    return true
                }
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    val rightBtn = if (settings.swapLeftRight) {
                        HidReportDescriptor.MOUSE_BUTTON_LEFT
                    } else {
                        HidReportDescriptor.MOUSE_BUTTON_RIGHT
                    }
                    lifecycleScope.launch {
                        app.hidManager.mouseSender.setButton(target, rightBtn, false)
                    }
                    return true
                }
            }
        }
        return super.onKeyUp(keyCode, event)
    }
}

@Composable
fun AirPointerBottomNav(
    currentTab: AppTab,
    onTabSelected: (AppTab) -> Unit
) {
    NavigationBar(
        containerColor = SurfaceDark,
        tonalElevation = 8.dp
    ) {
        AppTab.entries.forEach { tab ->
            val isSelected = currentTab == tab
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.title
                    )
                },
                label = {
                    Text(
                        text = tab.title,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 11.sp
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = CyanAccent,
                    selectedTextColor = CyanAccent,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary,
                    indicatorColor = SurfaceElevated
                )
            )
        }
    }
}

package com.airpointer.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppTab(val title: String, val icon: ImageVector) {
    DEVICES("Connect", Icons.Default.Bluetooth),
    TOUCHPAD("Touchpad", Icons.Default.Mouse),
    KEYBOARD("Keyboard", Icons.Default.Keyboard),
    SETTINGS("Settings", Icons.Default.Settings)
}

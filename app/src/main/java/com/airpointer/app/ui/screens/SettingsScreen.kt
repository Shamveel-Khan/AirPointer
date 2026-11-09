package com.airpointer.app.ui.screens

import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airpointer.app.ui.theme.BgDark
import com.airpointer.app.ui.theme.BluePrimary
import com.airpointer.app.ui.theme.CyanAccent
import com.airpointer.app.ui.theme.EmeraldSuccess
import com.airpointer.app.ui.theme.RedDanger
import com.airpointer.app.ui.theme.SurfaceBorder
import com.airpointer.app.ui.theme.SurfaceDark
import com.airpointer.app.ui.theme.SurfaceElevated
import com.airpointer.app.ui.theme.TextPrimary
import com.airpointer.app.ui.theme.TextSecondary
import com.airpointer.app.ui.viewmodels.SettingsViewModel
import java.util.Locale

@SuppressLint("MissingPermission")
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val isHidSupported by viewModel.hidManager.isHidSupported.collectAsState()
    var showWindowsGuide by remember { mutableStateOf(false) }
    var showLinuxGuide by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        // Simple Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Tune, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "AirPointer Settings",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Fine-tune cursor, gestures, and connection",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Section: Cursor & Sensitivity
            item {
                Text(
                    text = "Cursor & Movement",
                    style = MaterialTheme.typography.titleMedium,
                    color = CyanAccent,
                    fontWeight = FontWeight.SemiBold
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(14.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Cursor Sensitivity", color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Text(
                                String.format(Locale.US, "%.1fx", settings.cursorSensitivity),
                                color = CyanAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Slider(
                            value = settings.cursorSensitivity,
                            onValueChange = { viewModel.setCursorSensitivity(it) },
                            valueRange = 0.5f..3.0f,
                            steps = 25,
                            colors = SliderDefaults.colors(
                                thumbColor = CyanAccent,
                                activeTrackColor = CyanAccent,
                                inactiveTrackColor = SurfaceElevated
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        SettingToggleRow(
                            title = "Pointer Acceleration",
                            subtitle = "Dynamic speed scaling for precise slow motions and rapid flicking",
                            isChecked = settings.pointerAcceleration,
                            onCheckedChange = { viewModel.setPointerAcceleration(it) }
                        )
                    }
                }
            }

            // Section: Scrolling & Touch Gestures
            item {
                Text(
                    text = "Scrolling & Gestures",
                    style = MaterialTheme.typography.titleMedium,
                    color = CyanAccent,
                    fontWeight = FontWeight.SemiBold
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(14.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Scroll Sensitivity", color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Text(
                                String.format(Locale.US, "%.1fx", settings.scrollSensitivity),
                                color = CyanAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Slider(
                            value = settings.scrollSensitivity,
                            onValueChange = { viewModel.setScrollSensitivity(it) },
                            valueRange = 0.5f..3.0f,
                            steps = 25,
                            colors = SliderDefaults.colors(
                                thumbColor = CyanAccent,
                                activeTrackColor = CyanAccent,
                                inactiveTrackColor = SurfaceElevated
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        SettingToggleRow(
                            title = "Natural Scrolling",
                            subtitle = "Invert two-finger vertical scroll direction",
                            isChecked = settings.naturalScrolling,
                            onCheckedChange = { viewModel.setNaturalScrolling(it) }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        SettingToggleRow(
                            title = "Left-Handed Mode",
                            subtitle = "Swap primary Left and Right click buttons",
                            isChecked = settings.swapLeftRight,
                            onCheckedChange = { viewModel.setSwapLeftRight(it) }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        SettingToggleRow(
                            title = "On-Screen Click Buttons",
                            subtitle = "Show physical-style Left/Middle/Right buttons below touchpad",
                            isChecked = settings.showClickButtons,
                            onCheckedChange = { viewModel.setShowClickButtons(it) }
                        )
                    }
                }
            }

            // Section: Feedback & Power
            item {
                Text(
                    text = "Feedback & Display",
                    style = MaterialTheme.typography.titleMedium,
                    color = CyanAccent,
                    fontWeight = FontWeight.SemiBold
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(14.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SettingToggleRow(
                            title = "Haptic Vibration Feedback",
                            subtitle = "Gentle vibration pulses on clicks, taps, and drag locks",
                            isChecked = settings.hapticFeedback,
                            onCheckedChange = { viewModel.setHapticFeedback(it) }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        SettingToggleRow(
                            title = "Keep Screen On",
                            subtitle = "Prevent device from sleeping while using touchpad",
                            isChecked = settings.keepScreenOn,
                            onCheckedChange = { viewModel.setKeepScreenOn(it) }
                        )
                    }
                }
            }

            // Section: Pairing Guides
            item {
                Text(
                    text = "Laptop Pairing Guides",
                    style = MaterialTheme.typography.titleMedium,
                    color = CyanAccent,
                    fontWeight = FontWeight.SemiBold
                )
            }

            item {
                GuideCard(
                    title = "How to Pair with Windows 10 & 11",
                    isExpanded = showWindowsGuide,
                    onToggle = { showWindowsGuide = !showWindowsGuide },
                    steps = listOf(
                        "1. In AirPointer, tap 'Make Discoverable' in the Connect tab.",
                        "2. On your Windows PC, open Settings → Bluetooth & devices.",
                        "3. Turn Bluetooth On and click 'Add device' → 'Bluetooth'.",
                        "4. Select 'AirPointer' from the list of found devices.",
                        "5. Confirm the pairing prompt on both your phone and laptop.",
                        "6. Windows will recognize AirPointer as a Standard Bluetooth Mouse & Keyboard with no drivers needed!"
                    )
                )
            }

            item {
                GuideCard(
                    title = "How to Pair with Linux (Ubuntu, Fedora, Arch)",
                    isExpanded = showLinuxGuide,
                    onToggle = { showLinuxGuide = !showLinuxGuide },
                    steps = listOf(
                        "1. In AirPointer, tap 'Make Discoverable' in the Connect tab.",
                        "2. Open your Linux Bluetooth Settings (GNOME Settings or bluetoothctl).",
                        "3. Click on 'AirPointer' and confirm the pairing pin.",
                        "4. Or in terminal: 'bluetoothctl' → 'scan on' → 'pair <MAC>' → 'trust <MAC>' → 'connect <MAC>'.",
                        "5. Linux Kernel HID (hid-generic) will automatically register the pointer and keyboard."
                    )
                )
            }

            // Section: Diagnostics & About
            item {
                Text(
                    text = "System Diagnostics & Hardware",
                    style = MaterialTheme.typography.titleMedium,
                    color = CyanAccent,
                    fontWeight = FontWeight.SemiBold
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(14.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        DiagnosticRow("Device", "${Build.MANUFACTURER} ${Build.MODEL}")
                        DiagnosticRow("Android Version", "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                        DiagnosticRow(
                            "Bluetooth HID Role",
                            if (isHidSupported) "Supported (API 28+)" else "Unsupported by ROM",
                            valueColor = if (isHidSupported) EmeraldSuccess else RedDanger
                        )
                        DiagnosticRow("App Name", "AirPointer")
                        DiagnosticRow("Tagline", "Your Phone. Your Mouse. No Wires.")
                        DiagnosticRow("Version", "1.0.0 (Release)")
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Text(text = subtitle, color = TextSecondary, fontSize = 11.sp, lineHeight = 14.sp)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = BgDark,
                checkedTrackColor = CyanAccent,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = SurfaceElevated
            )
        )
    }
}

@Composable
private fun GuideCard(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    steps: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Laptop, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
                Icon(
                    if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextSecondary
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    steps.forEach { step ->
                        Text(
                            text = step,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            modifier = Modifier.padding(vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = TextPrimary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextSecondary, fontSize = 12.sp)
        Text(text = value, color = valueColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

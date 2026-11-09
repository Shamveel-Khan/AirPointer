package com.airpointer.app.ui.screens

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardReturn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airpointer.app.bluetooth.HidReportDescriptor
import com.airpointer.app.ui.components.AppHeader
import com.airpointer.app.ui.theme.AmberWarning
import com.airpointer.app.ui.theme.BgDark
import com.airpointer.app.ui.theme.BluePrimary
import com.airpointer.app.ui.theme.CyanAccent
import com.airpointer.app.ui.theme.SurfaceBorder
import com.airpointer.app.ui.theme.SurfaceDark
import com.airpointer.app.ui.theme.SurfaceElevated
import com.airpointer.app.ui.theme.TextPrimary
import com.airpointer.app.ui.theme.TextSecondary
import com.airpointer.app.ui.viewmodels.KeyboardViewModel

@SuppressLint("MissingPermission")
@Composable
fun KeyboardScreen(
    viewModel: KeyboardViewModel,
    modifier: Modifier = Modifier
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val connectedDevice by viewModel.connectedDevice.collectAsState()
    val capsLockActive by viewModel.capsLockActive.collectAsState()

    val ctrlActive by viewModel.ctrlActive.collectAsState()
    val altActive by viewModel.altActive.collectAsState()
    val shiftActive by viewModel.shiftActive.collectAsState()
    val guiActive by viewModel.guiActive.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var showInfoDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        AppHeader(
            connectionState = connectionState,
            connectedDevice = connectedDevice
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Android Software Keyboard Input Field
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Type or Paste Text to Laptop",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Use your phone's keyboard (Gboard, Samsung, Voice Typing) to send text directly over Bluetooth HID.",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Type here...", color = TextSecondary, fontSize = 13.sp) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(
                                    onSend = {
                                        if (inputText.isNotEmpty()) {
                                            viewModel.typeText(inputText)
                                            inputText = ""
                                        }
                                    }
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyanAccent,
                                    unfocusedBorderColor = SurfaceBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (inputText.isNotEmpty()) {
                                        viewModel.typeText(inputText)
                                        inputText = ""
                                    }
                                },
                                enabled = inputText.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            // Latchable Modifiers Row
            item {
                Text(
                    text = "Modifier Keys (Latchable)",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ModifierKeyButton(
                        label = "Ctrl",
                        isActive = ctrlActive,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.toggleCtrl() }
                    )
                    ModifierKeyButton(
                        label = "Alt",
                        isActive = altActive,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.toggleAlt() }
                    )
                    ModifierKeyButton(
                        label = "Shift",
                        isActive = shiftActive,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.toggleShift() }
                    )
                    ModifierKeyButton(
                        label = "Win / ⌘",
                        isActive = guiActive,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.toggleGui() }
                    )
                    if (ctrlActive || altActive || shiftActive || guiActive) {
                        ModifierKeyButton(
                            label = "Clear",
                            isActive = false,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.clearAllModifiers() }
                        )
                    }
                }
            }

            // Quick Desktop Shortcuts
            item {
                Text(
                    text = "Quick Desktop Shortcuts",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ShortcutKeyButton(
                        label = "Ctrl + C",
                        desc = "Copy",
                        icon = Icons.Default.ContentCopy,
                        onClick = {
                            viewModel.sendShortcut(HidReportDescriptor.MODIFIER_LEFT_CTRL, HidReportDescriptor.KEY_C)
                        }
                    )
                    ShortcutKeyButton(
                        label = "Ctrl + V",
                        desc = "Paste",
                        icon = Icons.Default.ContentPaste,
                        onClick = {
                            viewModel.sendShortcut(HidReportDescriptor.MODIFIER_LEFT_CTRL, HidReportDescriptor.KEY_V)
                        }
                    )
                    ShortcutKeyButton(
                        label = "Ctrl + Z",
                        desc = "Undo",
                        onClick = {
                            viewModel.sendShortcut(HidReportDescriptor.MODIFIER_LEFT_CTRL, HidReportDescriptor.KEY_Z)
                        }
                    )
                    ShortcutKeyButton(
                        label = "Ctrl + A",
                        desc = "Select All",
                        onClick = {
                            viewModel.sendShortcut(HidReportDescriptor.MODIFIER_LEFT_CTRL, HidReportDescriptor.KEY_A)
                        }
                    )
                    ShortcutKeyButton(
                        label = "Alt + Tab",
                        desc = "Switch Window",
                        onClick = {
                            viewModel.sendShortcut(HidReportDescriptor.MODIFIER_LEFT_ALT, HidReportDescriptor.KEY_TAB)
                        }
                    )
                }
            }

            // Navigation & Function Keys Grid
            item {
                Text(
                    text = "Special & Navigation Keys",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StandardKeyButton("Esc", Modifier.weight(1f)) { viewModel.sendKey(HidReportDescriptor.KEY_ESC) }
                    StandardKeyButton("Tab", Modifier.weight(1f)) { viewModel.sendKey(HidReportDescriptor.KEY_TAB) }
                    StandardKeyButton("⌫ Back", Modifier.weight(1.2f)) { viewModel.sendKey(HidReportDescriptor.KEY_BACKSPACE) }
                    StandardKeyButton("Del", Modifier.weight(1f)) { viewModel.sendKey(HidReportDescriptor.KEY_DELETE) }
                    StandardKeyButton("⏎ Enter", Modifier.weight(1.2f)) { viewModel.sendKey(HidReportDescriptor.KEY_ENTER) }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StandardKeyButton("Home", Modifier.weight(1f)) { viewModel.sendKey(HidReportDescriptor.KEY_HOME) }
                    StandardKeyButton("End", Modifier.weight(1f)) { viewModel.sendKey(HidReportDescriptor.KEY_END) }
                    StandardKeyButton("PgUp", Modifier.weight(1f)) { viewModel.sendKey(HidReportDescriptor.KEY_PAGEUP) }
                    StandardKeyButton("PgDn", Modifier.weight(1f)) { viewModel.sendKey(HidReportDescriptor.KEY_PAGEDOWN) }
                    StandardKeyButton("Space", Modifier.weight(1.5f)) { viewModel.sendKey(HidReportDescriptor.KEY_SPACE) }
                }
            }

            // Arrow Keys Cluster
            item {
                Text(
                    text = "Arrow Navigation",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ArrowKeyButton(Icons.Default.ArrowUpward) { viewModel.sendKey(HidReportDescriptor.KEY_UP) }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        ArrowKeyButton(Icons.AutoMirrored.Filled.ArrowBack) { viewModel.sendKey(HidReportDescriptor.KEY_LEFT) }
                        ArrowKeyButton(Icons.Default.ArrowDownward) { viewModel.sendKey(HidReportDescriptor.KEY_DOWN) }
                        ArrowKeyButton(Icons.AutoMirrored.Filled.ArrowForward) { viewModel.sendKey(HidReportDescriptor.KEY_RIGHT) }
                    }
                }
            }

            // Function Keys (F1 - F12)
            item {
                Text(
                    text = "Function Keys (F1 - F12)",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val fKeys = listOf(
                        "F1" to HidReportDescriptor.KEY_F1,
                        "F2" to HidReportDescriptor.KEY_F2,
                        "F3" to HidReportDescriptor.KEY_F3,
                        "F4" to HidReportDescriptor.KEY_F4,
                        "F5" to HidReportDescriptor.KEY_F5,
                        "F6" to HidReportDescriptor.KEY_F6,
                        "F7" to HidReportDescriptor.KEY_F7,
                        "F8" to HidReportDescriptor.KEY_F8,
                        "F9" to HidReportDescriptor.KEY_F9,
                        "F10" to HidReportDescriptor.KEY_F10,
                        "F11" to HidReportDescriptor.KEY_F11,
                        "F12" to HidReportDescriptor.KEY_F12
                    )
                    fKeys.forEach { (name, code) ->
                        StandardKeyButton(name, Modifier.width(52.dp)) { viewModel.sendKey(code) }
                    }
                }
            }

            // Media & Consumer Controls
            item {
                Text(
                    text = "Media & Volume Controls",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MediaIconButton(Icons.AutoMirrored.Filled.VolumeMute, "Mute", Modifier.weight(1f)) {
                        viewModel.sendConsumerKey(HidReportDescriptor.CONSUMER_VOLUME_MUTE)
                    }
                    MediaIconButton(Icons.AutoMirrored.Filled.VolumeDown, "Vol -", Modifier.weight(1f)) {
                        viewModel.sendConsumerKey(HidReportDescriptor.CONSUMER_VOLUME_DOWN)
                    }
                    MediaIconButton(Icons.AutoMirrored.Filled.VolumeUp, "Vol +", Modifier.weight(1f)) {
                        viewModel.sendConsumerKey(HidReportDescriptor.CONSUMER_VOLUME_UP)
                    }
                    MediaIconButton(Icons.Default.SkipPrevious, "Prev", Modifier.weight(1f)) {
                        viewModel.sendConsumerKey(HidReportDescriptor.CONSUMER_PREV_TRACK)
                    }
                    MediaIconButton(Icons.Default.PlayArrow, "Play/Pause", Modifier.weight(1f)) {
                        viewModel.sendConsumerKey(HidReportDescriptor.CONSUMER_PLAY_PAUSE)
                    }
                    MediaIconButton(Icons.Default.SkipNext, "Next", Modifier.weight(1f)) {
                        viewModel.sendConsumerKey(HidReportDescriptor.CONSUMER_NEXT_TRACK)
                    }
                }
            }

            // Technical Notice on HID vs Unicode IME
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "HID Protocol Info: Standard US QWERTY scancodes are emulated. Full ASCII (letters, numbers, symbols) is translated directly. Non-ASCII unicode glyphs or emojis cannot be sent as hardware scancodes.",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
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
private fun ModifierKeyButton(
    label: String,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isActive) CyanAccent.copy(alpha = 0.25f) else SurfaceDark)
            .border(
                1.dp,
                if (isActive) CyanAccent else SurfaceBorder,
                RoundedCornerShape(10.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = if (isActive) CyanAccent else TextPrimary,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun ShortcutKeyButton(
    label: String,
    desc: String,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceDark)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(text = label, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Text(text = desc, color = TextSecondary, fontSize = 10.sp)
        }
    }
}

@Composable
private fun StandardKeyButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceDark)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(8.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ArrowKeyButton(
    icon: ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceElevated)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(10.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun MediaIconButton(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceDark)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(10.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = label, tint = CyanAccent, modifier = Modifier.size(20.dp))
    }
}

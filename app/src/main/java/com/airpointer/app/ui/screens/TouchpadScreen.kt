package com.airpointer.app.ui.screens

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MotionPhotosOn
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.airpointer.app.bluetooth.HidConnectionState
import com.airpointer.app.bluetooth.HidReportDescriptor
import com.airpointer.app.data.InputMode
import com.airpointer.app.ui.components.StatusBadge
import com.airpointer.app.ui.theme.AmberWarning
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
import com.airpointer.app.ui.theme.TrackpadBorder
import com.airpointer.app.ui.theme.TrackpadSurface
import com.airpointer.app.ui.viewmodels.TouchpadViewModel
import java.util.Locale
import kotlin.math.roundToInt

@SuppressLint("MissingPermission")
@Composable
fun TouchpadScreen(
    viewModel: TouchpadViewModel,
    modifier: Modifier = Modifier
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val connectedDevice by viewModel.connectedDevice.collectAsState()
    val isDragLocked by viewModel.isDragLocked.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val isGyroTracking by viewModel.isGyroTracking.collectAsState()
    val gyroPitch by viewModel.gyroPitch.collectAsState()
    val gyroYaw by viewModel.gyroYaw.collectAsState()

    var showGestureHelp by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        // Top Mini Control Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusBadge(
                connectionState = connectionState,
                connectedDevice = connectedDevice
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Drag Lock Button (only relevant in touchpad mode)
                if (settings.inputMode == InputMode.TOUCHPAD) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isDragLocked) AmberWarning.copy(alpha = 0.2f) else SurfaceElevated)
                            .border(
                                1.dp,
                                if (isDragLocked) AmberWarning else SurfaceBorder,
                                RoundedCornerShape(20.dp)
                            )
                            .clickable { viewModel.toggleDragLock() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isDragLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = "Drag Lock",
                                tint = if (isDragLocked) AmberWarning else TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isDragLocked) "Drag Locked" else "Drag Lock",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isDragLocked) AmberWarning else TextSecondary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // Help Button
                IconButton(
                    onClick = { showGestureHelp = !showGestureHelp },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.HelpOutline,
                        contentDescription = "Gesture Guide",
                        tint = if (showGestureHelp) CyanAccent else TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Mode Switcher Bar (Touchpad vs Desk Mouse vs Air Mouse)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ModeSelectPill(
                title = "Touchpad",
                icon = Icons.Default.TouchApp,
                isSelected = settings.inputMode == InputMode.TOUCHPAD,
                modifier = Modifier.weight(1f),
                onClick = { viewModel.setInputMode(InputMode.TOUCHPAD) }
            )
            ModeSelectPill(
                title = "Desk Mouse",
                icon = Icons.Default.Mouse,
                isSelected = settings.inputMode == InputMode.DESK_MOUSE,
                modifier = Modifier.weight(1.1f),
                onClick = { viewModel.setInputMode(InputMode.DESK_MOUSE) }
            )
            ModeSelectPill(
                title = "Air Mouse",
                icon = Icons.Default.MotionPhotosOn,
                isSelected = settings.inputMode == InputMode.AIR_MOUSE,
                modifier = Modifier.weight(1f),
                onClick = { viewModel.setInputMode(InputMode.AIR_MOUSE) }
            )
        }

        // Gesture Hints Overlay (Collapsible)
        AnimatedVisibility(visible = showGestureHelp) {
            when (settings.inputMode) {
                InputMode.TOUCHPAD -> GestureGuideCard(onDismiss = { showGestureHelp = false })
                InputMode.DESK_MOUSE -> DeskMouseGuideCard(onDismiss = { showGestureHelp = false })
                InputMode.AIR_MOUSE -> AirMouseGuideCard(onDismiss = { showGestureHelp = false })
            }
        }

        // Main Surface Area based on Selected Mode
        Crossfade(
            targetState = settings.inputMode,
            label = "InputModeTransition",
            modifier = Modifier.weight(1f)
        ) { mode ->
            when (mode) {
                InputMode.TOUCHPAD -> {
                    TouchpadSurfaceArea(
                        viewModel = viewModel,
                        isDragLocked = isDragLocked,
                        isConnected = connectionState == HidConnectionState.CONNECTED
                    )
                }
                InputMode.DESK_MOUSE -> {
                    DeskMouseSurfaceArea(
                        viewModel = viewModel,
                        isConnected = connectionState == HidConnectionState.CONNECTED,
                        hasMotionSensors = viewModel.airMouseManager.hasMotionSensors
                    )
                }
                InputMode.AIR_MOUSE -> {
                    AirMouseSurfaceArea(
                        viewModel = viewModel,
                        isTracking = isGyroTracking,
                        pitch = gyroPitch,
                        yaw = gyroYaw,
                        isConnected = connectionState == HidConnectionState.CONNECTED,
                        hasMotionSensors = viewModel.airMouseManager.hasMotionSensors
                    )
                }
            }
        }

        // Bottom Click Buttons (Left, Middle, Right)
        if (settings.showClickButtons) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MouseButtonCard(
                    title = if (settings.swapLeftRight) "Right Click" else "Left Click",
                    modifier = Modifier.weight(1.4f),
                    onPress = { viewModel.pressButton(HidReportDescriptor.MOUSE_BUTTON_LEFT) },
                    onRelease = { viewModel.releaseButton(HidReportDescriptor.MOUSE_BUTTON_LEFT) }
                )

                MouseButtonCard(
                    title = "Middle",
                    modifier = Modifier.weight(0.9f),
                    onPress = { viewModel.onMiddleClick() },
                    onRelease = {}
                )

                MouseButtonCard(
                    title = if (settings.swapLeftRight) "Left Click" else "Right Click",
                    modifier = Modifier.weight(1.4f),
                    onPress = { viewModel.pressButton(HidReportDescriptor.MOUSE_BUTTON_RIGHT) },
                    onRelease = { viewModel.releaseButton(HidReportDescriptor.MOUSE_BUTTON_RIGHT) }
                )
            }
        }
    }
}

@Composable
private fun ModeSelectPill(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) CyanAccent.copy(alpha = 0.2f) else SurfaceDark,
        label = "pillBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) CyanAccent else SurfaceBorder,
        label = "pillBorder"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) CyanAccent else TextSecondary,
        label = "pillContent"
    )

    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor
            )
        }
    }
}

@Composable
private fun TouchpadSurfaceArea(
    viewModel: TouchpadViewModel,
    isDragLocked: Boolean,
    isConnected: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(TrackpadSurface)
            .border(
                1.5.dp,
                if (isDragLocked) AmberWarning.copy(alpha = 0.8f) else TrackpadBorder,
                RoundedCornerShape(20.dp)
            )
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                object : View(ctx) {
                    @SuppressLint("ClickableViewAccessibility")
                    override fun onTouchEvent(event: MotionEvent): Boolean {
                        return viewModel.gestureHandler.onTouchEvent(event)
                    }
                }
            }
        )

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.TouchApp,
                contentDescription = null,
                tint = TextSecondary.copy(alpha = 0.15f),
                modifier = Modifier.size(54.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isConnected) "Touch & Glide to Move Cursor" else "Connect to Laptop in 'Connect' Tab",
                fontSize = 13.sp,
                color = TextSecondary.copy(alpha = 0.35f),
                fontWeight = FontWeight.Medium
            )
            if (isDragLocked) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "● DRAGGING ACTIVE (Left Button Held)",
                    fontSize = 11.sp,
                    color = AmberWarning.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun DeskMouseSurfaceArea(
    viewModel: TouchpadViewModel,
    isConnected: Boolean,
    hasMotionSensors: Boolean
) {
    val settings by viewModel.settings.collectAsState()
    val isTracking by viewModel.isGyroTracking.collectAsState()
    val deskSpeed by viewModel.deskSpeed.collectAsState()

    if (!hasMotionSensors) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = RedDanger, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Motion Sensors Unavailable", fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Please use Touchpad mode on this device.", color = TextSecondary, fontSize = 12.sp)
                }
            }
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(TrackpadSurface)
            .border(
                1.5.dp,
                if (isTracking) CyanAccent else TrackpadBorder,
                RoundedCornerShape(20.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Live Status Header with Freeze/Resume and Zero Bias buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tracking Active / Frozen Pill (Tap to lift/freeze mouse)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isTracking) EmeraldSuccess.copy(alpha = 0.18f) else AmberWarning.copy(alpha = 0.18f))
                        .border(
                            1.dp,
                            if (isTracking) EmeraldSuccess else AmberWarning,
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { viewModel.airMouseManager.toggleTracking() }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isTracking) EmeraldSuccess else AmberWarning)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isTracking) "Tracking Active (Tap to Freeze)" else "Frozen (Tap to Resume)",
                            color = if (isTracking) EmeraldSuccess else AmberWarning,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Zero Surface Bias
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceElevated)
                        .clickable { viewModel.calibrateGyro() }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Zero Bias", fontSize = 11.sp, color = CyanAccent)
                    }
                }
            }

            // Central Interactive Desk Mouse Visualizer & Instructions
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { viewModel.airMouseManager.toggleTracking() }
            ) {
                Box(
                    modifier = Modifier
                        .size(145.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(SurfaceElevated.copy(alpha = 0.5f))
                        .border(2.dp, if (isTracking) CyanAccent.copy(alpha = 0.6f) else TrackpadBorder, RoundedCornerShape(32.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val normalizedSpeed = (deskSpeed / 2.5f).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .size((64 + normalizedSpeed * 32).dp)
                            .clip(CircleShape)
                            .background(CyanAccent.copy(alpha = 0.08f + normalizedSpeed * 0.28f))
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Mouse,
                            contentDescription = null,
                            tint = if (isTracking) CyanAccent else AmberWarning,
                            modifier = Modifier.size(46.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (deskSpeed > 0.08f) "Sliding..." else "Resting on Table",
                            fontSize = 11.sp,
                            color = if (deskSpeed > 0.08f) CyanAccent else TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Slide phone flat across desk or mousepad",
                    fontSize = 12.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Zero-lag Kinematics • Wrist Arc Pivot Assist",
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }

            // Desk Sensitivity Quick-Adjustment Slider
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceDark)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Desk Sensitivity", fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                    }
                    Text(
                        String.format(Locale.US, "%.1fx", settings.deskSensitivity),
                        fontSize = 11.sp,
                        color = CyanAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = settings.deskSensitivity,
                    onValueChange = { viewModel.setDeskSensitivity(it) },
                    valueRange = 0.5f..3.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = CyanAccent,
                        activeTrackColor = CyanAccent,
                        inactiveTrackColor = SurfaceElevated
                    ),
                    modifier = Modifier.height(28.dp)
                )
            }

            // Physical Volume Buttons Notification / Shortcut Banner
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceDark)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Vol Down = Left Click • Vol Up = Right Click",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }

        // Side Vertical Scroll Strip (Slide thumb along right edge to scroll while sliding)
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(44.dp)
                .height(180.dp)
                .padding(end = 6.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceElevated.copy(alpha = 0.6f))
                .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val scrollStep = -(dragAmount.y * 0.15f).roundToInt()
                        if (scrollStep != 0) {
                            viewModel.onScroll(scrollStep.toFloat(), 0f)
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.UnfoldMore, contentDescription = "Scroll", tint = TextSecondary, modifier = Modifier.size(20.dp))
                Text("Scroll", fontSize = 9.sp, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun AirMouseSurfaceArea(
    viewModel: TouchpadViewModel,
    isTracking: Boolean,
    pitch: Float,
    yaw: Float,
    isConnected: Boolean,
    hasMotionSensors: Boolean
) {
    val settings by viewModel.settings.collectAsState()

    if (!hasMotionSensors) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = RedDanger, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Motion Sensors Unavailable", fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Please use Touchpad mode on this device.", color = TextSecondary, fontSize = 12.sp)
                }
            }
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(TrackpadSurface)
            .border(
                1.5.dp,
                if (isTracking) CyanAccent else TrackpadBorder,
                RoundedCornerShape(20.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Live Status Header with Freeze/Resume and Recenter buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tracking Active / Paused Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isTracking) EmeraldSuccess.copy(alpha = 0.18f) else AmberWarning.copy(alpha = 0.18f))
                        .border(
                            1.dp,
                            if (isTracking) EmeraldSuccess else AmberWarning,
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { viewModel.airMouseManager.toggleTracking() }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isTracking) EmeraldSuccess else AmberWarning)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isTracking) "Tracking Live (Tap to Pause)" else "Paused (Tap to Resume)",
                            color = if (isTracking) EmeraldSuccess else AmberWarning,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Recenter Gyro
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceElevated)
                        .clickable { viewModel.calibrateGyro() }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Recenter", fontSize = 11.sp, color = CyanAccent)
                    }
                }
            }

            // Central Interactive Motion Sphere / Horizon Reticle
            Box(
                modifier = Modifier
                    .size(170.dp)
                    .clip(CircleShape)
                    .background(SurfaceElevated.copy(alpha = 0.5f))
                    .border(2.dp, if (isTracking) CyanAccent.copy(alpha = 0.6f) else TrackpadBorder, CircleShape)
                    .clickable { viewModel.airMouseManager.toggleTracking() },
                contentAlignment = Alignment.Center
            ) {
                // Crosshairs
                Box(modifier = Modifier.width(150.dp).height(1.dp).background(TrackpadBorder))
                Box(modifier = Modifier.height(150.dp).width(1.dp).background(TrackpadBorder))

                // Dynamic reticle moving with pitch & yaw
                val reticleOffsetX = (yaw * -28f).coerceIn(-55f, 55f).roundToInt()
                val reticleOffsetY = (pitch * -28f).coerceIn(-55f, 55f).roundToInt()

                Box(
                    modifier = Modifier
                        .offset { IntOffset(reticleOffsetX, reticleOffsetY) }
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (isTracking) CyanAccent.copy(alpha = 0.35f) else AmberWarning.copy(alpha = 0.2f))
                        .border(1.5.dp, if (isTracking) CyanAccent else AmberWarning, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isTracking) CyanAccent else AmberWarning)
                    )
                }
            }

            // Motion Sensitivity Quick-Adjustment Slider
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceDark)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Motion Speed", fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                    }
                    Text(
                        String.format(Locale.US, "%.1fx", settings.gyroSensitivity),
                        fontSize = 11.sp,
                        color = CyanAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = settings.gyroSensitivity,
                    onValueChange = { viewModel.setGyroSensitivity(it) },
                    valueRange = 0.5f..3.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = CyanAccent,
                        activeTrackColor = CyanAccent,
                        inactiveTrackColor = SurfaceElevated
                    ),
                    modifier = Modifier.height(28.dp)
                )
            }

            // Physical Volume Buttons Notification / Shortcut Banner
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceDark)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Vol Down = Left Click • Vol Up = Right Click",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }

        // Side Vertical Scroll Strip (Slide thumb along right edge to scroll)
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(44.dp)
                .height(180.dp)
                .padding(end = 6.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceElevated.copy(alpha = 0.6f))
                .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val scrollStep = -(dragAmount.y * 0.15f).roundToInt()
                        if (scrollStep != 0) {
                            viewModel.onScroll(scrollStep.toFloat(), 0f)
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.UnfoldMore, contentDescription = "Scroll", tint = TextSecondary, modifier = Modifier.size(20.dp))
                Text("Scroll", fontSize = 9.sp, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun MouseButtonCard(
    title: String,
    modifier: Modifier = Modifier,
    onPress: () -> Unit,
    onRelease: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .height(58.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isPressed) CyanAccent.copy(alpha = 0.25f) else SurfaceElevated)
            .border(
                1.dp,
                if (isPressed) CyanAccent else SurfaceBorder,
                RoundedCornerShape(14.dp)
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        onPress()
                        tryAwaitRelease()
                        isPressed = false
                        onRelease()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.SemiBold,
            color = if (isPressed) CyanAccent else TextPrimary,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun GestureGuideCard(onDismiss: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Touchpad Gestures",
                    fontWeight = FontWeight.Bold,
                    color = CyanAccent,
                    fontSize = 14.sp
                )
                Text(
                    text = "Close",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable { onDismiss() }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            GestureRow(gesture = "1 Finger Move", action = "Move Mouse Cursor")
            GestureRow(gesture = "1 Finger Tap", action = "Left Click")
            GestureRow(gesture = "1 Finger Double Tap", action = "Double Click")
            GestureRow(gesture = "2 Finger Tap", action = "Right Click")
            GestureRow(gesture = "2 Finger Vertical Swipe", action = "Scroll Up / Down")
            GestureRow(gesture = "2 Finger Horizontal Swipe", action = "Horizontal Scroll")
            GestureRow(gesture = "Long Press (0.35s) & Move", action = "Drag & Drop (Left Click Held)")
        }
    }
}

@Composable
private fun DeskMouseGuideCard(onDismiss: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Desk Mouse Guide (Slide on Table)",
                    fontWeight = FontWeight.Bold,
                    color = CyanAccent,
                    fontSize = 14.sp
                )
                Text(
                    text = "Close",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable { onDismiss() }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            GestureRow(gesture = "Slide Phone Flat on Table", action = "Cursor Glides with Hand Movement")
            GestureRow(gesture = "Wrist Arc Pivoting", action = "Turn Wrist Left/Right for Natural Arc")
            GestureRow(gesture = "Tap Freeze / Resume", action = "Freeze to Pick Up & Reposition Mouse")
            GestureRow(gesture = "Volume Down Button", action = "Tactile Left Click")
            GestureRow(gesture = "Volume Up Button", action = "Tactile Right Click")
            GestureRow(gesture = "Slide Right Edge", action = "Scroll Wheel Up / Down")
            GestureRow(gesture = "Zero Bias Button", action = "Calibrate Surface Angle / Zero Drift")
        }
    }
}

@Composable
private fun AirMouseGuideCard(onDismiss: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Air Mouse Guide (Point in Air)",
                    fontWeight = FontWeight.Bold,
                    color = CyanAccent,
                    fontSize = 14.sp
                )
                Text(
                    text = "Close",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable { onDismiss() }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            GestureRow(gesture = "Point / Tilt Phone in Air", action = "Cursor Glides with Gyro Orientation")
            GestureRow(gesture = "Tap Live Status / Reticle", action = "Pause / Freeze Tracking to Reposition")
            GestureRow(gesture = "Volume Down Button", action = "Physical Left Click")
            GestureRow(gesture = "Volume Up Button", action = "Physical Right Click")
            GestureRow(gesture = "Slide Right Edge", action = "Scroll Wheel Up / Down")
            GestureRow(gesture = "Tap Recenter", action = "Calibrate Gyroscope Bias")
        }
    }
}

@Composable
private fun GestureRow(gesture: String, action: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = gesture, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Text(text = action, color = TextSecondary, fontSize = 12.sp)
    }
}

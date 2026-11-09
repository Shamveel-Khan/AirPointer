package com.airpointer.app.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.view.View
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.airpointer.app.bluetooth.HidConnectionState
import com.airpointer.app.bluetooth.HidReportDescriptor
import com.airpointer.app.ui.components.StatusBadge
import com.airpointer.app.ui.theme.AmberWarning
import com.airpointer.app.ui.theme.BgDark
import com.airpointer.app.ui.theme.BluePrimary
import com.airpointer.app.ui.theme.CyanAccent
import com.airpointer.app.ui.theme.EmeraldSuccess
import com.airpointer.app.ui.theme.SurfaceBorder
import com.airpointer.app.ui.theme.SurfaceDark
import com.airpointer.app.ui.theme.SurfaceElevated
import com.airpointer.app.ui.theme.TextPrimary
import com.airpointer.app.ui.theme.TextSecondary
import com.airpointer.app.ui.theme.TrackpadBorder
import com.airpointer.app.ui.theme.TrackpadSurface
import com.airpointer.app.ui.viewmodels.TouchpadViewModel

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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusBadge(
                connectionState = connectionState,
                connectedDevice = connectedDevice
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Drag Lock Button
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

                // Gesture Help Toggle
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

        // Gesture Hints Overlay (Collapsible)
        AnimatedVisibility(visible = showGestureHelp) {
            GestureGuideCard(onDismiss = { showGestureHelp = false })
        }

        // Main Touch Surface Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(TrackpadSurface)
                .border(1.5.dp, if (isDragLocked) AmberWarning.copy(alpha = 0.8f) else TrackpadBorder, RoundedCornerShape(20.dp))
        ) {
            // AndroidView handles raw MotionEvents for zero-latency responsiveness
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

            // Centered Subtle Watermark & Guide
            Column(
                modifier = Modifier
                    .align(Alignment.Center),
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
                    text = if (connectionState == HidConnectionState.CONNECTED) {
                        "Touch & Glide to Move Cursor"
                    } else {
                        "Connect to Laptop in 'Connect' Tab"
                    },
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

        // Optional Physical-style Bottom Buttons
        if (settings.showClickButtons) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Left Click Button
                MouseButtonCard(
                    title = if (settings.swapLeftRight) "Right Click" else "Left Click",
                    modifier = Modifier.weight(1.4f),
                    onPress = { viewModel.pressButton(HidReportDescriptor.MOUSE_BUTTON_LEFT) },
                    onRelease = { viewModel.releaseButton(HidReportDescriptor.MOUSE_BUTTON_LEFT) }
                )

                // Middle Click / Scroll Wheel Click Button
                MouseButtonCard(
                    title = "Middle",
                    modifier = Modifier.weight(0.9f),
                    onPress = { viewModel.onMiddleClick() },
                    onRelease = {}
                )

                // Right Click Button
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

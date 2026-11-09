package com.airpointer.app.ui.components

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airpointer.app.bluetooth.HidConnectionState
import com.airpointer.app.ui.theme.AmberWarning
import com.airpointer.app.ui.theme.AmberWarningBg
import com.airpointer.app.ui.theme.EmeraldSuccess
import com.airpointer.app.ui.theme.EmeraldSuccessBg
import com.airpointer.app.ui.theme.RedDanger
import com.airpointer.app.ui.theme.RedDangerBg
import com.airpointer.app.ui.theme.SurfaceElevated

@SuppressLint("MissingPermission")
@Composable
fun StatusBadge(
    connectionState: HidConnectionState,
    connectedDevice: BluetoothDevice?,
    modifier: Modifier = Modifier
) {
    val (statusText, dotColor, bgColor) = when (connectionState) {
        HidConnectionState.CONNECTED -> {
            val name = connectedDevice?.name ?: "Connected Device"
            Triple("Connected: $name", EmeraldSuccess, EmeraldSuccessBg)
        }
        HidConnectionState.CONNECTING -> {
            Triple("Connecting...", AmberWarning, AmberWarningBg)
        }
        HidConnectionState.DISCONNECTING -> {
            Triple("Disconnecting...", AmberWarning, AmberWarningBg)
        }
        HidConnectionState.DISCONNECTED -> {
            Triple("Not Connected", RedDanger, RedDangerBg)
        }
    }

    val animatedDotColor by animateColorAsState(targetValue = dotColor, label = "dotColor")

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .border(1.dp, dotColor.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(animatedDotColor)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = statusText,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

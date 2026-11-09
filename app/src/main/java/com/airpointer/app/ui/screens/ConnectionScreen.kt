package com.airpointer.app.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothDevice
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airpointer.app.bluetooth.HidConnectionState
import com.airpointer.app.ui.components.AppHeader
import com.airpointer.app.ui.theme.AmberWarning
import com.airpointer.app.ui.theme.AmberWarningBg
import com.airpointer.app.ui.theme.BgDark
import com.airpointer.app.ui.theme.BluePrimary
import com.airpointer.app.ui.theme.CyanAccent
import com.airpointer.app.ui.theme.EmeraldSuccess
import com.airpointer.app.ui.theme.EmeraldSuccessBg
import com.airpointer.app.ui.theme.RedDanger
import com.airpointer.app.ui.theme.RedDangerBg
import com.airpointer.app.ui.theme.SurfaceBorder
import com.airpointer.app.ui.theme.SurfaceDark
import com.airpointer.app.ui.theme.SurfaceElevated
import com.airpointer.app.ui.theme.TextPrimary
import com.airpointer.app.ui.theme.TextSecondary
import com.airpointer.app.ui.viewmodels.ConnectionViewModel

@SuppressLint("MissingPermission")
@Composable
fun ConnectionScreen(
    viewModel: ConnectionViewModel,
    hasPermissions: Boolean,
    onRequestPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isBluetoothEnabled by viewModel.isBluetoothEnabled.collectAsState()
    val isHidSupported by viewModel.isHidSupported.collectAsState()
    val isAppRegistered by viewModel.isAppRegistered.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val connectedDevice by viewModel.connectedDevice.collectAsState()
    val pairedDevices by viewModel.pairedDevices.collectAsState()
    val discoveredDevices by viewModel.discoveredDevices.collectAsState()
    val isDiscovering by viewModel.isDiscovering.collectAsState()

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
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Permission Banner (if missing)
            if (!hasPermissions) {
                item {
                    PermissionCard(onRequestPermissions = onRequestPermissions)
                }
            }

            // HID Compatibility Warning
            if (!isHidSupported) {
                item {
                    HidUnsupportedCard()
                }
            }

            // Bluetooth Disabled Card
            if (!isBluetoothEnabled) {
                item {
                    BluetoothDisabledCard()
                }
            }

            // Active Connection Card
            if (connectionState == HidConnectionState.CONNECTED && connectedDevice != null) {
                item {
                    ConnectedDeviceCard(
                        device = connectedDevice!!,
                        onDisconnect = { viewModel.disconnect() }
                    )
                }
            } else if (connectionState == HidConnectionState.CONNECTING) {
                item {
                    ConnectingCard(device = connectedDevice)
                }
            }

            // Pairing Actions Row
            item {
                ActionButtonsRow(
                    isDiscovering = isDiscovering,
                    onMakeDiscoverable = {
                        (context as? Activity)?.let { viewModel.makeDiscoverable(it) }
                    },
                    onScanDevices = {
                        if (isDiscovering) viewModel.cancelDiscovery() else viewModel.startDiscovery()
                    },
                    onRefreshPaired = { viewModel.refreshPaired() }
                )
            }

            // Paired Laptops & Computers Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Paired Computers & Devices",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${pairedDevices.size} paired",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            if (pairedDevices.isEmpty()) {
                item {
                    EmptyPairedCard(
                        onMakeDiscoverable = {
                            (context as? Activity)?.let { viewModel.makeDiscoverable(it) }
                        }
                    )
                }
            } else {
                items(pairedDevices, key = { it.address }) { device ->
                    DeviceItemCard(
                        device = device,
                        isConnected = connectedDevice?.address == device.address && connectionState == HidConnectionState.CONNECTED,
                        isConnecting = connectedDevice?.address == device.address && connectionState == HidConnectionState.CONNECTING,
                        isComputer = viewModel.isComputer(device),
                        onConnect = { viewModel.connect(device) },
                        onDisconnect = { viewModel.disconnect() }
                    )
                }
            }

            // Discovered Devices (Live Bluetooth Scan)
            if (isDiscovering || discoveredDevices.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Available Nearby Devices",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (isDiscovering) {
                                Spacer(modifier = Modifier.width(8.dp))
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = CyanAccent
                                )
                            }
                        }
                    }
                }

                val unbondedDiscovered = discoveredDevices.filter { dev ->
                    pairedDevices.none { it.address == dev.address }
                }

                if (unbondedDiscovered.isEmpty() && isDiscovering) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Scanning for nearby Bluetooth devices...",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(unbondedDiscovered, key = { it.address }) { device ->
                        DiscoveredDeviceCard(
                            device = device,
                            isComputer = viewModel.isComputer(device),
                            onPair = { viewModel.pair(device) }
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
private fun PermissionCard(onRequestPermissions: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AmberWarningBg),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(AmberWarning))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = AmberWarning)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Bluetooth Permissions Required",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 15.sp
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "AirPointer needs Bluetooth Connect, Scan, and Advertise permissions to emulate a wireless mouse on your PC.",
                color = TextSecondary,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onRequestPermissions,
                colors = ButtonDefaults.buttonColors(containerColor = AmberWarning, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Grant Permissions", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun HidUnsupportedCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = RedDangerBg),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(RedDanger))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = RedDanger)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "HID Device Role Not Supported",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 15.sp
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Your device's Bluetooth firmware or OEM ROM does not support the Bluetooth HID Device profile (API 28+). Please verify in Settings that Bluetooth is enabled.",
                color = TextSecondary,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun BluetoothDisabledCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Bluetooth, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Bluetooth is Turned Off",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 15.sp
                )
                Text(
                    text = "Turn on Bluetooth in your phone's settings to connect with your laptop.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun ConnectedDeviceCard(
    device: BluetoothDevice,
    onDisconnect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = EmeraldSuccessBg),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(EmeraldSuccess))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(EmeraldSuccess.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Computer, contentDescription = null, tint = EmeraldSuccess)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = device.name ?: "Connected PC",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Active HID Link • ${device.address}",
                            color = EmeraldSuccess,
                            fontSize = 12.sp
                        )
                    }
                }

                OutlinedButton(
                    onClick = onDisconnect,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RedDanger),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.LinkOff, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Disconnect", fontSize = 12.sp)
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun ConnectingCard(device: BluetoothDevice?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AmberWarningBg),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(AmberWarning))
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = AmberWarning,
                strokeWidth = 3.dp
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Connecting to ${device?.name ?: "Device"}...",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 15.sp
                )
                Text(
                    text = "Establishing Bluetooth HID handshake...",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun ActionButtonsRow(
    isDiscovering: Boolean,
    onMakeDiscoverable: () -> Unit,
    onScanDevices: () -> Unit,
    onRefreshPaired: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onMakeDiscoverable,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Make Discoverable", fontSize = 12.sp)
        }

        OutlinedButton(
            onClick = onScanDevices,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = if (isDiscovering) CyanAccent else TextPrimary
            )
        ) {
            Icon(
                if (isDiscovering) Icons.AutoMirrored.Filled.BluetoothSearching else Icons.Default.Bluetooth,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(if (isDiscovering) "Scanning" else "Scan", fontSize = 12.sp)
        }

        IconButton(
            onClick = onRefreshPaired,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceElevated)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TextPrimary)
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun DeviceItemCard(
    device: BluetoothDevice,
    isConnected: Boolean,
    isConnecting: Boolean,
    isComputer: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isConnecting) {
                if (isConnected) onDisconnect() else onConnect()
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) EmeraldSuccessBg else SurfaceDark
        ),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (isConnected) EmeraldSuccess else SurfaceBorder
            )
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isComputer) BluePrimary.copy(alpha = 0.2f) else SurfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isComputer) Icons.Default.Computer else Icons.Default.Devices,
                        contentDescription = null,
                        tint = if (isComputer) CyanAccent else TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = device.name ?: "Unnamed Device",
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        fontSize = 15.sp
                    )
                    Text(
                        text = device.address,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            if (isConnected) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Connected", color = EmeraldSuccess, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            } else if (isConnecting) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = AmberWarning)
            } else {
                Button(
                    onClick = onConnect,
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated, contentColor = CyanAccent),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text("Connect", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun DiscoveredDeviceCard(
    device: BluetoothDevice,
    isComputer: Boolean,
    onPair: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (isComputer) Icons.Default.Computer else Icons.Default.Devices,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = device.name ?: "Unknown Device",
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = device.address,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            OutlinedButton(
                onClick = onPair,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("Pair", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun EmptyPairedCard(onMakeDiscoverable: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Info, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "No Paired Computers Found",
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Tap 'Make Discoverable', then open Bluetooth Settings on your Windows or Linux laptop and add 'AirPointer' to pair.",
                color = TextSecondary,
                fontSize = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onMakeDiscoverable,
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Make Discoverable Now")
            }
        }
    }
}

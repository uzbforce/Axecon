package com.axecon.uzbforce.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.axecon.uzbforce.ui.theme.M3Error
import com.axecon.uzbforce.ui.theme.M3Success
import com.axecon.uzbforce.ui.theme.M3Warning
import com.axecon.uzbforce.ui.viewmodel.AirCursorViewModel
import com.axecon.uzbforce.ui.viewmodel.PermissionStatus

@Composable
fun PermissionScreen(
    viewModel: AirCursorViewModel,
    permissions: PermissionStatus,
    onAllPermissionsGranted: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.checkPermissions(context)
    }

    val totalRequired = 4
    val grantedCount = (if (permissions.isAccessibilityEnabled) 1 else 0) +
            (if (permissions.isOverlayPermissionGranted) 1 else 0) +
            (if (permissions.isBatteryOptimizationIgnored) 1 else 0) +
            (if (permissions.isNotificationPermissionGranted) 1 else 0)

    val progress = (grantedCount.toFloat() / totalRequired).coerceIn(0f, 1f)

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (onBack != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "System Permissions",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            } else {
                Spacer(modifier = Modifier.height(16.dp))

                // App Identity Header
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Security & Permissions",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Required Permissions",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Axecon requires Accessibility and Overlay access to navigate your smartphone using motion and physical buttons when the touchscreen is damaged or unresponsive.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Progress status card
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("permission_progress_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Setup Progress ($grantedCount / $totalRequired Granted)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        IconButton(
                            onClick = { viewModel.checkPermissions(context) },
                            modifier = Modifier.testTag("refresh_permissions_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Check Permissions",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (permissions.areMandatoryGranted) M3Success else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 1. Accessibility Service Permission Item
            PermissionCard(
                title = "1. Accessibility Service",
                tag = "MANDATORY",
                description = "Enables virtual cursor touch dispatch, tap / hold clicking, gesture scrolling, and volume button interception.",
                icon = Icons.Default.AccessibilityNew,
                isGranted = permissions.isAccessibilityEnabled,
                actionLabel = "Enable in Accessibility Settings",
                onAction = { viewModel.openAccessibilitySettings(context) }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Display Over Other Apps Permission Item
            PermissionCard(
                title = "2. Draw Over Other Apps",
                tag = "MANDATORY",
                description = "Permits Axecon to display the cursor crosshair pointer, action wheel, and visual ripples on top of all apps.",
                icon = Icons.Default.Layers,
                isGranted = permissions.isOverlayPermissionGranted,
                actionLabel = "Grant Overlay Permission",
                onAction = { viewModel.openOverlaySettings(context) }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Battery Optimization (Recommended)
            PermissionCard(
                title = "3. Unrestricted Battery",
                tag = "RECOMMENDED",
                description = "Prevents Android battery saver from killing the motion tracking sensor and background service.",
                icon = Icons.Default.BatteryChargingFull,
                isGranted = permissions.isBatteryOptimizationIgnored,
                actionLabel = if (permissions.isBatteryOptimizationIgnored) "Battery Limit Disabled" else "Disable Battery Limits",
                onAction = { viewModel.requestBatteryOptimizationExemption(context) }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Notifications Permission
            PermissionCard(
                title = "4. Background Notifications",
                tag = "RECOMMENDED",
                description = "Keeps Axecon service running continuously with quick status and calibration access from the notification tray.",
                icon = Icons.Default.Notifications,
                isGranted = permissions.isNotificationPermissionGranted,
                actionLabel = if (permissions.isNotificationPermissionGranted) "Notifications Enabled" else "Enable Notifications",
                onAction = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.openAppNotificationSettings(context)
                    }
                }
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Continue Button (Only enabled when all mandatory permissions granted)
            Button(
                onClick = onAllPermissionsGranted,
                enabled = permissions.areMandatoryGranted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("continue_to_app_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (permissions.areMandatoryGranted) "Launch Axecon Controller" else "Grant Mandatory Permissions to Continue",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}


@Composable
private fun PermissionCard(
    title: String,
    tag: String,
    description: String,
    icon: ImageVector,
    isGranted: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    val statusColor by animateColorAsState(
        targetValue = if (isGranted) M3Success else if (tag == "MANDATORY") M3Error else M3Warning,
        label = "statusColor"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isGranted) M3Success.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = if (isGranted) M3Success else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (tag == "MANDATORY") MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (tag == "MANDATORY") MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isGranted) "Granted & Active" else "Permission Missing",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }

                if (!isGranted) {
                    FilledTonalButton(
                        onClick = onAction,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Text(
                            text = "Grant Access",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

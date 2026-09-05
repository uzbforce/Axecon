package com.axecon.uzbforce.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.axecon.uzbforce.model.CursorAction
import com.axecon.uzbforce.model.CustomAppShortcut
import com.axecon.uzbforce.model.FeedbackSoundType
import com.axecon.uzbforce.model.HapticIntensity
import com.axecon.uzbforce.service.FeedbackHelper
import com.axecon.uzbforce.ui.viewmodel.AirCursorViewModel
import com.axecon.uzbforce.ui.viewmodel.UiState
import java.util.UUID

@Composable
fun AccessibilityToolsScreen(
    viewModel: AirCursorViewModel,
    uiState: UiState
) {
    val context = LocalContext.current
    val feedbackHelper = remember { FeedbackHelper(context) }
    var showAddShortcutDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: Action Button & Customizable Direct App Shortcuts
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Action Button Menu",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Overlay shortcuts for damaged screens",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = { viewModel.triggerActionFromApp(CursorAction.OPEN_ACTION_MENU) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .wrapContentWidth()
                                .testTag("open_action_menu_btn")
                        ) {
                            Text("Open Menu", maxLines = 1)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "The Action Menu overlay displays on-screen shortcuts for quick single-touch actions like answering calls, launching essential apps, recentering, or turning on the flashlight.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Floating On-Screen Action Pill",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Shows a small floating ⚡ bubble on screen edge",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Switch(
                            checked = uiState.config.floatingActionButtonEnabled,
                            onCheckedChange = { enabled ->
                                viewModel.updateConfig { it.copy(floatingActionButtonEnabled = enabled) }
                            },
                            modifier = Modifier.testTag("floating_action_btn_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // CUSTOMIZABLE DIRECT APP SHORTCUTS
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CUSTOM DIRECT APP SHORTCUTS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        TextButton(
                            onClick = { showAddShortcutDialog = true }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add App", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (uiState.customShortcuts.isEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No custom app shortcuts configured.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { showAddShortcutDialog = true },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("+ Add App Shortcut")
                                }
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            uiState.customShortcuts.forEach { shortcut ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(shortcut.colorArgb.toInt())),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = shortcut.label.take(1).uppercase(),
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = shortcut.label,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = shortcut.packageName,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                try {
                                                    val intent = context.packageManager.getLaunchIntentForPackage(shortcut.packageName)
                                                    if (intent != null) {
                                                        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                                        context.startActivity(intent)
                                                    }
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Launch,
                                                contentDescription = "Test launch",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { viewModel.removeCustomShortcut(shortcut.id) }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Vibration & Haptic Customization
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Vibration,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Vibration & Haptics",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Physical tactile feedback per action",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Switch(
                            checked = uiState.config.hapticFeedback,
                            onCheckedChange = { enabled ->
                                viewModel.updateConfig { it.copy(hapticFeedback = enabled) }
                            },
                            modifier = Modifier.testTag("master_haptic_switch")
                        )
                    }

                    AnimatedVisibility(visible = uiState.config.hapticFeedback) {
                        Column {
                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Haptic Intensity",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                HapticIntensity.entries.forEach { intensity ->
                                    val isSelected = uiState.config.hapticIntensity == intensity
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            viewModel.updateConfig { it.copy(hapticIntensity = intensity) }
                                            feedbackHelper.vibrateClick(intensity)
                                        },
                                        label = { Text(intensity.label.substringBefore(" ")) },
                                        leadingIcon = if (isSelected) {
                                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                        } else null,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "CUSTOMIZE ACTIONS TRIGGERING VIBRATION",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            FeedbackActionToggleRow(
                                title = "Touch Clicks & Double Taps",
                                subtitle = "Vibrates on single/double click at cursor",
                                checked = uiState.config.hapticOnClick,
                                onCheckedChange = { checked ->
                                    viewModel.updateConfig { it.copy(hapticOnClick = checked) }
                                    if (checked) feedbackHelper.vibrateClick(uiState.config.hapticIntensity)
                                }
                            )

                            FeedbackActionToggleRow(
                                title = "Long Press / Hold Clicks",
                                subtitle = "Distinct extended buzz on hold clicks",
                                checked = uiState.config.hapticOnLongClick,
                                onCheckedChange = { checked ->
                                    viewModel.updateConfig { it.copy(hapticOnLongClick = checked) }
                                    if (checked) feedbackHelper.vibrateLong(uiState.config.hapticIntensity)
                                }
                            )

                            FeedbackActionToggleRow(
                                title = "Continuous Scrolling Ticks",
                                subtitle = "Micro-haptic tick on each scroll step",
                                checked = uiState.config.hapticOnScroll,
                                onCheckedChange = { checked ->
                                    viewModel.updateConfig { it.copy(hapticOnScroll = checked) }
                                    if (checked) feedbackHelper.vibrateClick(uiState.config.hapticIntensity)
                                }
                            )

                            FeedbackActionToggleRow(
                                title = "Action Menu Open",
                                subtitle = "Double-pulse buzz when action menu launches",
                                checked = uiState.config.hapticOnActionMenu,
                                onCheckedChange = { checked ->
                                    viewModel.updateConfig { it.copy(hapticOnActionMenu = checked) }
                                    if (checked) feedbackHelper.vibratePulse(uiState.config.hapticIntensity)
                                }
                            )

                            FeedbackActionToggleRow(
                                title = "Recenter & Sensor Calibrate",
                                subtitle = "Firm confirmation pulse when zero angle is set",
                                checked = uiState.config.hapticOnRecenter,
                                onCheckedChange = { checked ->
                                    viewModel.updateConfig { it.copy(hapticOnRecenter = checked) }
                                    if (checked) feedbackHelper.vibratePulse(uiState.config.hapticIntensity)
                                }
                            )
                        }
                    }
                }
            }
        }

        // Section 3: Audio Tone Customization
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.tertiaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Audio Feedback Sounds",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Auditory confirmation for touchless navigation",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Switch(
                            checked = uiState.config.audioFeedback,
                            onCheckedChange = { enabled ->
                                viewModel.updateConfig { it.copy(audioFeedback = enabled) }
                            },
                            modifier = Modifier.testTag("master_audio_switch")
                        )
                    }

                    AnimatedVisibility(visible = uiState.config.audioFeedback) {
                        Column {
                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Sound Effect Style",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            FeedbackSoundType.entries.filter { it != FeedbackSoundType.NONE }.chunked(2).forEach { rowList ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowList.forEach { soundType ->
                                        val isSelected = uiState.config.soundType == soundType
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                viewModel.updateConfig { it.copy(soundType = soundType) }
                                                feedbackHelper.playSound(soundType)
                                            },
                                            label = { Text(soundType.label.substringBefore(" ("), maxLines = 1) },
                                            leadingIcon = if (isSelected) {
                                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                            } else null,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "CUSTOMIZE ACTIONS TRIGGERING SOUND",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            FeedbackActionToggleRow(
                                title = "Play on Clicks",
                                subtitle = "Plays tone when button triggers a click",
                                checked = uiState.config.audioOnClick,
                                onCheckedChange = { checked ->
                                    viewModel.updateConfig { it.copy(audioOnClick = checked) }
                                    if (checked) feedbackHelper.playSound(uiState.config.soundType)
                                }
                            )

                            FeedbackActionToggleRow(
                                title = "Play on Long Press",
                                subtitle = "Plays tone on hold click completion",
                                checked = uiState.config.audioOnLongClick,
                                onCheckedChange = { checked ->
                                    viewModel.updateConfig { it.copy(audioOnLongClick = checked) }
                                    if (checked) feedbackHelper.playSound(uiState.config.soundType)
                                }
                            )

                            FeedbackActionToggleRow(
                                title = "Play on Scrolling",
                                subtitle = "Soft tick sound per scrolling stroke",
                                checked = uiState.config.audioOnScroll,
                                onCheckedChange = { checked ->
                                    viewModel.updateConfig { it.copy(audioOnScroll = checked) }
                                    if (checked) feedbackHelper.playSound(uiState.config.soundType)
                                }
                            )

                            FeedbackActionToggleRow(
                                title = "Play on Action Menu & Recenter",
                                subtitle = "Chime tone when action menu or 0° calibrate fires",
                                checked = uiState.config.audioOnActionMenu,
                                onCheckedChange = { checked ->
                                    viewModel.updateConfig { it.copy(audioOnActionMenu = checked, audioOnRecenter = checked) }
                                    if (checked) feedbackHelper.playSound(uiState.config.soundType)
                                }
                            )
                        }
                    }
                }
            }
        }

        // Section 4: Broken Screen Text Input & D-Pad Bar
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Keyboard,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Typing & Navigation Bar",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Voice typing & system navigation overlay",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = { viewModel.triggerActionFromApp(CursorAction.SHOW_KEYBOARD_HELPER) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.wrapContentWidth()
                        ) {
                            Text("Toggle Bar", maxLines = 1)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Displays an on-screen floating bar with instant access to Voice Dictation, Back, Home, and Recents for effortless phone usage without touching the screen.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Add Custom App Shortcut Modal Dialog
    if (showAddShortcutDialog) {
        AddAppShortcutDialog(
            onDismiss = { showAddShortcutDialog = false },
            onAdd = { newShortcut ->
                viewModel.addCustomShortcut(newShortcut)
                showAddShortcutDialog = false
            }
        )
    }
}

@Composable
private fun AddAppShortcutDialog(
    onDismiss: () -> Unit,
    onAdd: (CustomAppShortcut) -> Unit
) {
    var label by remember { mutableStateOf("") }
    var packageName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(0xFF2563EB) }

    val presetApps = listOf(
        Triple("Phone", "com.google.android.dialer", 0xFF059669),
        Triple("Messages", "com.google.android.apps.messaging", 0xFF2563EB),
        Triple("Chrome", "com.android.chrome", 0xFFD97706),
        Triple("WhatsApp", "com.whatsapp", 0xFF10B981),
        Triple("YouTube", "com.google.android.youtube", 0xFFEF4444),
        Triple("Settings", "com.android.settings", 0xFF475569),
        Triple("Camera", "com.google.android.GoogleCamera", 0xFF8B5CF6)
    )

    val colorOptions = listOf(
        0xFF2563EBL,
        0xFF059669L,
        0xFFD97706L,
        0xFFEF4444L,
        0xFF8B5CF6L,
        0xFF06B6D4L,
        0xFF475569L
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Direct App Shortcut",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Quick App Presets:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetApps.forEach { (pLabel, pPkg, pColor) ->
                        FilterChip(
                            selected = packageName == pPkg,
                            onClick = {
                                label = pLabel
                                packageName = pPkg
                                selectedColor = pColor
                            },
                            label = { Text(pLabel, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = packageName,
                    onValueChange = { packageName = it },
                    label = { Text("Android Package Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Shortcut Color:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colorOptions.forEach { colorVal ->
                        val isSelected = selectedColor == colorVal
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(colorVal))
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = colorVal },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (label.isNotBlank() && packageName.isNotBlank()) {
                        onAdd(
                            CustomAppShortcut(
                                id = UUID.randomUUID().toString(),
                                label = label.trim(),
                                packageName = packageName.trim(),
                                colorArgb = selectedColor
                            )
                        )
                    }
                },
                enabled = label.isNotBlank() && packageName.isNotBlank()
            ) {
                Text("Add Shortcut")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun FeedbackActionToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    com.axecon.uzbforce.ui.components.AdaptiveSettingSwitchRow(
        title = title,
        subtitle = subtitle,
        checked = checked,
        onCheckedChange = onCheckedChange
    )
}

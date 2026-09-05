package com.axecon.uzbforce.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.axecon.uzbforce.model.NavigationBarStyle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.ViewSidebar
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.axecon.uzbforce.model.ButtonTrigger
import com.axecon.uzbforce.model.CursorAction
import com.axecon.uzbforce.model.CustomKeySequence
import com.axecon.uzbforce.model.KeyStep
import com.axecon.uzbforce.model.PresetProfile
import com.axecon.uzbforce.model.SideButtonIndicator
import com.axecon.uzbforce.service.AirCursorAccessibilityService
import com.axecon.uzbforce.ui.viewmodel.AirCursorViewModel
import com.axecon.uzbforce.ui.viewmodel.UiState
import java.util.UUID

@Composable
fun ButtonMappingScreen(
    viewModel: AirCursorViewModel,
    uiState: UiState
) {
    var showAddSequenceDialog by remember { mutableStateOf(false) }
    var editingSequence by remember { mutableStateOf<CustomKeySequence?>(null) }
    var selectedIndicatorId by remember { mutableIntStateOf(0) }

    val volUpTriggers = listOf(
        ButtonTrigger.VOL_UP_SINGLE,
        ButtonTrigger.VOL_UP_DOUBLE,
        ButtonTrigger.VOL_UP_LONG,
        ButtonTrigger.VOL_UP_DOUBLE_HOLD
    )

    val volDownTriggers = listOf(
        ButtonTrigger.VOL_DOWN_SINGLE,
        ButtonTrigger.VOL_DOWN_DOUBLE,
        ButtonTrigger.VOL_DOWN_LONG,
        ButtonTrigger.VOL_DOWN_DOUBLE_HOLD
    )

    val actionComboTriggers = listOf(
        ButtonTrigger.BOTH_VOL_PRESS,
        ButtonTrigger.BOTH_VOL_LONG_HOLD
    )

    val headsetTriggers = listOf(
        ButtonTrigger.HEADSET_HOOK_SINGLE,
        ButtonTrigger.HEADSET_HOOK_DOUBLE,
        ButtonTrigger.HEADSET_HOOK_LONG
    )

    val standardColorPresets = listOf(
        0xFF00E5FF to "Cyan",
        0xFF10B981 to "Green",
        0xFFF59E0B to "Amber",
        0xFFEC4899 to "Pink",
        0xFF8B5CF6 to "Violet",
        0xFFFFFFFF to "White"
    )

    val standoutHighlightColors = listOf(
        0xFFF59E0B to "Gold / Amber",
        0xFFEC4899 to "Hot Pink",
        0xFFFF5722 to "Bright Coral",
        0xFF84CC16 to "Lime Green",
        0xFF00E5FF to "Neon Cyan",
        0xFF38BDF8 to "Electric Sky"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: Button Click Visualizer (Individual Hardware Line Calibration)
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
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.tertiaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Button Click Visualizer",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Calibrate individual side indicator lines",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = uiState.config.sideIndicatorsEnabled,
                            onCheckedChange = { enabled ->
                                viewModel.updateConfig { it.copy(sideIndicatorsEnabled = enabled) }
                            }
                        )
                    }

                    AnimatedVisibility(visible = uiState.config.sideIndicatorsEnabled) {
                        Column {
                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "Select an individual button indicator below to adjust its position and size so it perfectly aligns with your phone's physical hardware buttons.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Individual Button Line Selector Chips
                            Text(
                                text = "Select Button Line to Configure:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val buttonLabels = listOf(
                                    0 to "Line 1: Volume Up",
                                    1 to "Line 2: Volume Down",
                                    2 to "Line 3: Power / Extra"
                                )

                                buttonLabels.forEach { (id, label) ->
                                    FilterChip(
                                        selected = selectedIndicatorId == id,
                                        onClick = { selectedIndicatorId = id },
                                        label = {
                                            Text(
                                                text = label,
                                                fontWeight = if (selectedIndicatorId == id) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Current selected indicator settings
                            val currentIndicators = uiState.config.individualIndicators
                            val activeInd = currentIndicators.find { it.id == selectedIndicatorId }
                                ?: SideButtonIndicator(
                                    id = selectedIndicatorId,
                                    label = when (selectedIndicatorId) {
                                        0 -> "Volume Up"
                                        1 -> "Volume Down"
                                        else -> "Power / Extra"
                                    },
                                    isEnabled = true,
                                    isOnRightSide = true,
                                    verticalOffsetRatio = when (selectedIndicatorId) {
                                        0 -> 0.38f
                                        1 -> 0.46f
                                        else -> 0.58f
                                    },
                                    lengthDp = 42,
                                    thicknessDp = 5,
                                    activeColorArgb = 0xFF00E5FF,
                                    inactiveColorArgb = 0x33FFFFFF
                                )

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${activeInd.label} Settings",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("Enabled", style = MaterialTheme.typography.labelSmall)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Switch(
                                                checked = activeInd.isEnabled,
                                                onCheckedChange = { en ->
                                                    viewModel.updateIndividualIndicator(activeInd.copy(isEnabled = en))
                                                }
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Edge placement
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Button Placement Side",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            FilterChip(
                                                selected = !activeInd.isOnRightSide,
                                                onClick = {
                                                    viewModel.updateIndividualIndicator(activeInd.copy(isOnRightSide = false))
                                                },
                                                label = { Text("Left Side") }
                                            )
                                            FilterChip(
                                                selected = activeInd.isOnRightSide,
                                                onClick = {
                                                    viewModel.updateIndividualIndicator(activeInd.copy(isOnRightSide = true))
                                                },
                                                label = { Text("Right Side") }
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Exact Vertical Position with fine-tuning
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Exact Physical Vertical Position",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${String.format(java.util.Locale.US, "%.1f", activeInd.verticalOffsetRatio * 100)}%",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Slider(
                                        value = activeInd.verticalOffsetRatio,
                                        onValueChange = { pos ->
                                            viewModel.updateIndividualIndicator(activeInd.copy(verticalOffsetRatio = pos))
                                        },
                                        valueRange = 0.10f..0.90f,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    // Micro-tuning buttons (-0.5% / +0.5%)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                val newPos = (activeInd.verticalOffsetRatio - 0.005f).coerceIn(0.10f, 0.90f)
                                                viewModel.updateIndividualIndicator(activeInd.copy(verticalOffsetRatio = newPos))
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("▲ Nudge Up (-0.5%)", style = MaterialTheme.typography.labelSmall)
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                val newPos = (activeInd.verticalOffsetRatio + 0.005f).coerceIn(0.10f, 0.90f)
                                                viewModel.updateIndividualIndicator(activeInd.copy(verticalOffsetRatio = newPos))
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("▼ Nudge Down (+0.5%)", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Line Length (Height)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Button Line Length",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${activeInd.lengthDp} dp",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Slider(
                                        value = activeInd.lengthDp.toFloat(),
                                        onValueChange = { len ->
                                            viewModel.updateIndividualIndicator(activeInd.copy(lengthDp = len.toInt()))
                                        },
                                        valueRange = 14f..90f,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    // Line Thickness (Width)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Button Line Thickness",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${activeInd.thicknessDp} dp",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Slider(
                                        value = activeInd.thicknessDp.toFloat(),
                                        onValueChange = { th ->
                                            viewModel.updateIndividualIndicator(activeInd.copy(thicknessDp = th.toInt()))
                                        },
                                        valueRange = 2f..14f,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Active Glow Color
                                    Text(
                                        text = "Active Glow Color",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        standardColorPresets.forEach { (colorLong, name) ->
                                            val isSelected = activeInd.activeColorArgb == colorLong
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = Color(colorLong.toInt()),
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .border(
                                                        width = if (isSelected) 3.dp else 1.dp,
                                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                                        shape = RoundedCornerShape(10.dp)
                                                    )
                                                    .clickable {
                                                        viewModel.updateIndividualIndicator(activeInd.copy(activeColorArgb = colorLong))
                                                    }
                                            ) {}
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Test button for selected line
                                    Button(
                                        onClick = {
                                            AirCursorAccessibilityService.highlightIndicator(selectedIndicatorId)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("⚡ Test & Highlight Line ${selectedIndicatorId + 1} on Screen")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Key Sequence Combo Timeout & Order Visualizer Settings
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
                    com.axecon.uzbforce.ui.components.AdaptiveSettingSwitchRow(
                        title = "Sequence Combo & Order Visualizer",
                        subtitle = "1s timeout gauge & ordered key badges",
                        checked = uiState.config.showSequenceVisualizer,
                        onCheckedChange = { show ->
                            viewModel.updateConfig { it.copy(showSequenceVisualizer = show) }
                        },
                        testTag = "switch_sequence_visualizer",
                        icon = {
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
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "When pressing buttons in a sequence, a second visualizer (countdown timer gauge) appears next to the button indicator, along with an HUD displaying the exact sequence order with the LAST pressed key highlighted in a distinct color.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Timeout Configuration
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Next Keypress Timeout",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${String.format(java.util.Locale.US, "%.1f", uiState.config.comboTimeoutMs / 1000f)}s",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Slider(
                        value = uiState.config.comboTimeoutMs.toFloat(),
                        onValueChange = { ms ->
                            viewModel.updateConfig { it.copy(comboTimeoutMs = ms.toLong()) }
                        },
                        valueRange = 400f..3000f,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Presets for timeout - Large touch-friendly blue buttons
                    val timeoutPresets = listOf(
                        500L to "0.5s",
                        800L to "0.8s",
                        1000L to "1.0s (Default)",
                        1500L to "1.5s",
                        2000L to "2.0s",
                        2500L to "2.5s"
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        timeoutPresets.chunked(3).forEach { rowPresets ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowPresets.forEach { (ms, lbl) ->
                                    val isSel = uiState.config.comboTimeoutMs == ms
                                    Surface(
                                        onClick = { viewModel.updateConfig { it.copy(comboTimeoutMs = ms) } },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isSel) {
                                            Color(0xFF2563EB) // Vibrant Electric Blue
                                        } else {
                                            Color(0xFF1E3A8A).copy(alpha = 0.22f) // Deep Blue tonal container
                                        },
                                        border = BorderStroke(
                                            width = if (isSel) 2.dp else 1.dp,
                                            color = if (isSel) Color(0xFF93C5FD) else Color(0xFF3B82F6).copy(alpha = 0.5f)
                                        ),
                                        shadowElevation = if (isSel) 4.dp else 0.dp
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center,
                                                modifier = Modifier.padding(horizontal = 4.dp)
                                            ) {
                                                if (isSel) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                }
                                                Text(
                                                    text = lbl,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.SemiBold,
                                                    color = if (isSel) Color.White else Color(0xFF60A5FA),
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Last Pressed Key Highlight Color
                    Text(
                        text = "Last Pressed Key Standout Color",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Distinct glowing color applied to the most recently tapped key in sequence",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        standoutHighlightColors.forEach { (colorLong, name) ->
                            val isSelected = uiState.config.lastPressedHighlightColorArgb == colorLong
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(colorLong.toInt()),
                                modifier = Modifier
                                    .size(40.dp)
                                    .border(
                                        width = if (isSelected) 3.5.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        viewModel.updateConfig { it.copy(lastPressedHighlightColorArgb = colorLong) }
                                    }
                            ) {}
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Live Visualizer Preview Box
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF0F172A)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "⚡ COMBO PREVIEW • 1.0s",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF94A3B8),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Order Flow",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF64748B)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Previous step badge 1
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF1E293B),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                                ) {
                                    Text(
                                        text = "① Vol +",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFE2E8F0),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Text("➔", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))

                                // Previous step badge 2
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF1E293B),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                                ) {
                                    Text(
                                        text = "② Vol -",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFE2E8F0),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Text("➔", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))

                                // Last pressed key badge (Highlighted!)
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(uiState.config.lastPressedHighlightColorArgb.toInt()),
                                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White)
                                ) {
                                    Text(
                                        text = "③ Vol + (NEW)",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // In-App Interactive Live Simulator
                    Text(
                        text = "Interactive Test (Simulates live overlay gauge & HUD):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { AirCursorAccessibilityService.simulateKeyStep(KeyStep.VOLUME_UP) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Text("Vol +", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = { AirCursorAccessibilityService.simulateKeyStep(KeyStep.VOLUME_DOWN) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Text("Vol -", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = { AirCursorAccessibilityService.simulateKeyStep(KeyStep.POWER) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Text("Power", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        }
                    }
                }
            }
        }

        // Section 3: Customizable Multi-Button Combination Sequences
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
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Custom Key Sequences",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Combine button clicks for custom actions",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { showAddSequenceDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.wrapContentWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Combo", maxLines = 1)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Define custom sequence patterns like: Vol Up ➔ Power ➔ Vol Up ➔ Vol Down to execute specific shortcut actions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (uiState.customSequences.isEmpty()) {
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
                                    text = "No custom key combinations configured yet.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { showAddSequenceDialog = true },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("+ Add First Combination")
                                }
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            uiState.customSequences.forEach { sequence ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = sequence.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Switch(
                                                checked = sequence.isEnabled,
                                                onCheckedChange = { viewModel.toggleCustomSequence(sequence.id) }
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            IconButton(
                                                onClick = { editingSequence = sequence },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Edit Sequence",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(2.dp))
                                            IconButton(
                                                onClick = { viewModel.deleteCustomSequence(sequence.id) },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Render sequence steps using FlowRow to wrap cleanly
                                        @OptIn(ExperimentalLayoutApi::class)
                                        FlowRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            sequence.steps.forEachIndexed { idx, step ->
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = MaterialTheme.colorScheme.primaryContainer
                                                    ) {
                                                        Text(
                                                            text = step.displayName,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                        )
                                                    }
                                                    if (idx < sequence.steps.size - 1) {
                                                        Text(
                                                            text = "➔",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Triggers Action: ",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.secondaryContainer
                                            ) {
                                                Text(
                                                    text = sequence.action.label,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
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
        }

        // Section 3: Preset Profiles Card
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
                    Text(
                        text = "Button Mapping Presets",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Quickly switch gesture setups for specific usage styles",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PresetProfile.entries.take(3).forEach { profile ->
                            FilledTonalButton(
                                onClick = { viewModel.applyPreset(profile) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = profile.profileName.substringBefore(" "),
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // Timing calibration sliders
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Gesture Timing Windows",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Double Click Speed Window",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${uiState.config.doubleClickSpeedMs} ms",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Slider(
                        value = uiState.config.doubleClickSpeedMs.toFloat(),
                        onValueChange = { speed ->
                            viewModel.updateConfig { it.copy(doubleClickSpeedMs = speed.toLong()) }
                        },
                        valueRange = 180f..550f,
                        steps = 7,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Long Press / Hold Threshold",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${uiState.config.longPressDurationMs} ms",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Slider(
                        value = uiState.config.longPressDurationMs.toFloat(),
                        onValueChange = { dur ->
                            viewModel.updateConfig { it.copy(longPressDurationMs = dur.toLong()) }
                        },
                        valueRange = 300f..900f,
                        steps = 6,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Action Combo (Both Volume Buttons)
        item {
            TriggerSection(
                title = "Action Button Combination (Vol+ & Vol-)",
                subtitle = "Simultaneous button trigger for overlay and quick menu",
                icon = Icons.Default.Bolt,
                triggers = actionComboTriggers,
                mappings = uiState.buttonMappings,
                onEditTrigger = { viewModel.setEditingTrigger(it) }
            )
        }

        // Volume Up Group
        item {
            TriggerSection(
                title = "Volume Up Button Gestures",
                subtitle = "Single click, double-click, hold, and double-tap & hold",
                icon = Icons.Default.VolumeUp,
                triggers = volUpTriggers,
                mappings = uiState.buttonMappings,
                onEditTrigger = { viewModel.setEditingTrigger(it) }
            )
        }

        // Volume Down Group
        item {
            TriggerSection(
                title = "Volume Down Button Gestures",
                subtitle = "Single click, double-click, hold, and double-tap & hold",
                icon = Icons.Default.VolumeDown,
                triggers = volDownTriggers,
                mappings = uiState.buttonMappings,
                onEditTrigger = { viewModel.setEditingTrigger(it) }
            )
        }

        // Headset Group
        item {
            TriggerSection(
                title = "Headset / Bluetooth Media Button",
                subtitle = "Wired headset or Bluetooth hook button clicks",
                icon = Icons.Default.Headphones,
                triggers = headsetTriggers,
                mappings = uiState.buttonMappings,
                onEditTrigger = { viewModel.setEditingTrigger(it) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(if (uiState.config.navBarStyle == NavigationBarStyle.PILL_IOS) 96.dp else 40.dp))
        }
    }

    // Modal Action Picker Dialog when editing a trigger
    uiState.editingTrigger?.let { trigger ->
        ActionPickerDialog(
            trigger = trigger,
            currentAction = uiState.buttonMappings[trigger] ?: CursorAction.NONE,
            onDismiss = { viewModel.setEditingTrigger(null) },
            onSelectAction = { action ->
                viewModel.updateButtonMapping(trigger, action)
            }
        )
    }

    // Modal Custom Sequence Builder Dialog (New)
    if (showAddSequenceDialog) {
        CustomSequenceBuilderDialog(
            initialSequence = null,
            onDismiss = { showAddSequenceDialog = false },
            onSave = { sequence ->
                viewModel.addCustomSequence(sequence)
                showAddSequenceDialog = false
            }
        )
    }

    // Modal Custom Sequence Editor Dialog (Edit existing)
    editingSequence?.let { seq ->
        CustomSequenceBuilderDialog(
            initialSequence = seq,
            onDismiss = { editingSequence = null },
            onSave = { updated ->
                viewModel.updateCustomSequence(updated)
                editingSequence = null
            }
        )
    }
}

@Composable
private fun CustomSequenceBuilderDialog(
    initialSequence: CustomKeySequence? = null,
    onDismiss: () -> Unit,
    onSave: (CustomKeySequence) -> Unit
) {
    var name by remember(initialSequence) { mutableStateOf(initialSequence?.name ?: "") }
    var steps by remember(initialSequence) { mutableStateOf(initialSequence?.steps ?: listOf<KeyStep>()) }
    var selectedAction by remember(initialSequence) { mutableStateOf(initialSequence?.action ?: CursorAction.TOGGLE_TORCH) }
    var showActionPicker by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var countdownSeconds by remember { mutableStateOf<Int?>(null) }

    DisposableEffect(isRecording) {
        if (isRecording) {
            AirCursorAccessibilityService.startRecordingSequence { step ->
                if (steps.size < 8) {
                    steps = steps + step
                }
            }
        } else {
            AirCursorAccessibilityService.stopRecordingSequence()
            countdownSeconds = null
        }
        onDispose {
            AirCursorAccessibilityService.stopRecordingSequence()
        }
    }

    LaunchedEffect(isRecording, steps.size) {
        if (isRecording && steps.isNotEmpty()) {
            // 2-second inactivity timer that automatically resets on every new button press
            countdownSeconds = 2
            delay(1000L)
            countdownSeconds = 1
            delay(1000L)
            countdownSeconds = null
            isRecording = false
            AirCursorAccessibilityService.stopRecordingSequence()
        } else {
            countdownSeconds = null
        }
    }

    val availableSteps = listOf(
        KeyStep.VOLUME_UP,
        KeyStep.VOLUME_DOWN,
        KeyStep.POWER,
        KeyStep.HEADSET_HOOK,
        KeyStep.BACK
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialSequence != null) "Edit Button Sequence" else "Record Button Sequence",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Sequence Name") },
                    placeholder = { Text("e.g. Flashlight Combo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Recording Control Header
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isRecording) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isRecording) {
                                        if (countdownSeconds != null) "🔴 Auto-finishing in ${countdownSeconds}s..." else "🔴 Recording: Press buttons in order..."
                                    } else "Physical Button Recording",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isRecording) {
                                        if (countdownSeconds != null) "Press next button to append or wait for timer to auto-finish" else "Buttons are intercepted temporarily. Press physical buttons now"
                                    } else "Click 'Begin' and press physical buttons in order",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Button(
                                onClick = { isRecording = !isRecording },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(if (isRecording) "Finish" else "Begin")
                            }
                        }
                    }
                }

                // Step Buttons
                Text(
                    text = "Buttons to add to sequence:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    availableSteps.forEach { step ->
                        FilledTonalButton(
                            onClick = {
                                if (steps.size < 8) {
                                    steps = steps + step
                                }
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("+ ${step.displayName}", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                // Sequence preview box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recorded Sequence (${steps.size}/8):",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (steps.isNotEmpty()) {
                                Row {
                                    TextButton(
                                        onClick = { steps = steps.dropLast(1) }
                                    ) {
                                        Text("Undo", style = MaterialTheme.typography.labelSmall)
                                    }
                                    TextButton(
                                        onClick = { steps = emptyList() }
                                    ) {
                                        Text("Clear", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        if (steps.isEmpty()) {
                            Text(
                                text = "No steps recorded yet. Click 'Begin' or tap buttons above.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                steps.forEachIndexed { idx, step ->
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    ) {
                                        Text(
                                            text = "${idx + 1}. ${step.displayName}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    if (idx < steps.size - 1) {
                                        Text("➔", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }

                Text(
                    text = "Action To Trigger:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showActionPicker = true },
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedAction.label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Change",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && steps.isNotEmpty()) {
                        onSave(
                            CustomKeySequence(
                                id = initialSequence?.id ?: UUID.randomUUID().toString(),
                                name = name.trim(),
                                steps = steps,
                                action = selectedAction,
                                isEnabled = initialSequence?.isEnabled ?: true
                            )
                        )
                    }
                },
                enabled = name.isNotBlank() && steps.isNotEmpty()
            ) {
                Text(if (initialSequence != null) "Save Changes" else "Save Sequence")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showActionPicker) {
        AlertDialog(
            onDismissRequest = { showActionPicker = false },
            title = { Text("Select Action", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(CursorAction.entries) { action ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedAction = action
                                    showActionPicker = false
                                },
                            shape = RoundedCornerShape(8.dp),
                            color = if (action == selectedAction) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ) {
                            Text(
                                text = action.label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (action == selectedAction) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showActionPicker = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun TriggerSection(
    title: String,
    subtitle: String,
    icon: ImageVector,
    triggers: List<ButtonTrigger>,
    mappings: Map<ButtonTrigger, CursorAction>,
    onEditTrigger: (ButtonTrigger) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            triggers.forEach { trigger ->
                val action = mappings[trigger] ?: CursorAction.NONE
                TriggerRowItem(
                    trigger = trigger,
                    action = action,
                    onClick = { onEditTrigger(trigger) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun TriggerRowItem(
    trigger: ButtonTrigger,
    action: CursorAction,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("trigger_item_${trigger.name}"),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = trigger.label.substringAfter(": "),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = trigger.shortcutDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = action.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun ActionPickerDialog(
    trigger: ButtonTrigger,
    currentAction: CursorAction,
    onDismiss: () -> Unit,
    onSelectAction: (CursorAction) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredActions = CursorAction.entries.filter { action ->
        searchQuery.isBlank() || action.label.contains(searchQuery, ignoreCase = true) || action.description.contains(searchQuery, ignoreCase = true)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Map Action: ${trigger.label.substringAfter(": ")}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = trigger.shortcutDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search actions (e.g., Click, Torch, Menu)...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredActions) { action ->
                        val isSelected = action == currentAction
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectAction(action) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = action.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = action.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

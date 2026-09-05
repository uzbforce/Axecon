package com.axecon.uzbforce.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.axecon.uzbforce.model.NavigationBarStyle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.axecon.uzbforce.model.CursorStyle
import com.axecon.uzbforce.model.SensorMode
import com.axecon.uzbforce.ui.components.AdaptiveSettingSwitchRow
import com.axecon.uzbforce.ui.components.InteractiveHotspotCalibrationPad
import com.axecon.uzbforce.ui.components.ScrollSpeedCalibrationCard
import com.axecon.uzbforce.ui.viewmodel.AirCursorViewModel
import com.axecon.uzbforce.ui.viewmodel.UiState

@Composable
fun MotionTuningScreen(
    viewModel: AirCursorViewModel,
    uiState: UiState
) {
    val cursorColors = listOf(
        0xFFFFFFFFL to "White",
        0xFF2563EBL to "Blue",
        0xFF06B6D4L to "Cyan",
        0xFF10B981L to "Green",
        0xFFF59E0BL to "Amber",
        0xFFEF4444L to "Red",
        0xFF8B5CF6L to "Purple",
        0xFF1E293BL to "Dark"
    )

    val outlineColors = listOf(
        0xFF000000L to "Black",
        0xFF1E293BL to "Dark",
        0xFFFFFFFFL to "White",
        0xFF00E5FFL to "Cyan",
        0xFFEF4444L to "Red"
    )

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.updateConfig { cfg ->
                cfg.copy(
                    cursorStyle = CursorStyle.CUSTOM_IMAGE,
                    customImageUri = it.toString()
                )
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Header
        item {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(
                    text = "Pointer & Motion",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Adjust movement speed, pointer style, click position, and sensors",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 2. Motion Sensitivity & Speed
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    SectionHeader(
                        title = "Speed & Sensitivity",
                        subtitle = "Fine-tune how fast the pointer moves with your hand",
                        icon = Icons.Default.Speed
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    TuningSliderItem(
                        label = "Horizontal Speed (X)",
                        valueText = "${String.format("%.1f", uiState.config.sensitivityX)}x",
                        value = uiState.config.sensitivityX,
                        valueRange = 0.5f..5.0f,
                        onValueChange = { sx -> viewModel.updateConfig { it.copy(sensitivityX = sx) } }
                    )

                    TuningSliderItem(
                        label = "Vertical Speed (Y)",
                        valueText = "${String.format("%.1f", uiState.config.sensitivityY)}x",
                        value = uiState.config.sensitivityY,
                        valueRange = 0.5f..5.0f,
                        onValueChange = { sy -> viewModel.updateConfig { it.copy(sensitivityY = sy) } }
                    )

                    TuningSliderItem(
                        label = "Smoothness Filter",
                        valueText = String.format("%.2f", uiState.config.smoothingFactor),
                        value = uiState.config.smoothingFactor,
                        valueRange = 0.05f..0.95f,
                        onValueChange = { sm -> viewModel.updateConfig { it.copy(smoothingFactor = sm) } }
                    )

                    TuningSliderItem(
                        label = "Stillness Deadzone",
                        valueText = String.format("%.3f", uiState.config.deadzone),
                        value = uiState.config.deadzone,
                        valueRange = 0.001f..0.15f,
                        onValueChange = { dz -> viewModel.updateConfig { it.copy(deadzone = dz) } }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    AdaptiveSettingSwitchRow(
                        title = "Speed Boost on Fast Moves",
                        subtitle = "Makes pointer travel faster across screen when moving quickly",
                        checked = uiState.config.accelerationEnabled,
                        onCheckedChange = { acc -> viewModel.updateConfig { it.copy(accelerationEnabled = acc) } },
                        testTag = "switch_acceleration"
                    )

                    AnimatedVisibility(visible = uiState.config.accelerationEnabled) {
                        Column {
                            Spacer(modifier = Modifier.height(6.dp))
                            TuningSliderItem(
                                label = "Boost Multiplier",
                                valueText = "${String.format("%.1f", uiState.config.accelerationFactor)}x",
                                value = uiState.config.accelerationFactor,
                                valueRange = 1.0f..3.0f,
                                onValueChange = { m -> viewModel.updateConfig { it.copy(accelerationFactor = m) } }
                            )
                        }
                    }
                }
            }
        }

        // 3. Pointer Style & Look
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    SectionHeader(
                        title = "Pointer Style",
                        subtitle = "Choose shape, color, size and high-contrast outline",
                        icon = Icons.Default.Palette
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Pointer Shape",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(CursorStyle.entries) { style ->
                            val isSelected = uiState.config.cursorStyle == style
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.updateConfig { it.copy(cursorStyle = style) } },
                                label = { Text(style.label, style = MaterialTheme.typography.labelSmall) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Pointer Color",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(cursorColors) { (colorArgb, name) ->
                            val isSelected = uiState.config.cursorColorArgb == colorArgb
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorArgb))
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        viewModel.updateConfig { it.copy(cursorColorArgb = colorArgb) }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = name,
                                        tint = if (colorArgb == 0xFFFFFFFFL || colorArgb == 0xFF06B6D4L) Color.Black else Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    AdaptiveSettingSwitchRow(
                        title = "High-Contrast Border",
                        subtitle = "Adds a clean outline so the pointer stays clear on any background",
                        checked = uiState.config.cursorOutlineEnabled,
                        onCheckedChange = { en -> viewModel.updateConfig { it.copy(cursorOutlineEnabled = en) } },
                        testTag = "switch_cursor_outline"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    TuningSliderItem(
                        label = "Pointer Size",
                        valueText = "${uiState.config.cursorSizeDp} dp",
                        value = uiState.config.cursorSizeDp.toFloat(),
                        valueRange = 16f..52f,
                        onValueChange = { sz -> viewModel.updateConfig { it.copy(cursorSizeDp = sz.toInt()) } }
                    )
                }
            }
        }

        // 4. Click Position & Tip Alignment
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    SectionHeader(
                        title = "Click Position & Alignment",
                        subtitle = if (uiState.config.cursorStyle == CursorStyle.DOT_POINTER)
                            "Center of dot is the default click hotspot (0, 0). Fine-tune or tap to offset"
                        else
                            "Pointer tip is the default click hotspot (0, 0). Fine-tune or tap to offset",
                        icon = Icons.Default.Mouse
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    InteractiveHotspotCalibrationPad(
                        config = uiState.config,
                        clickOffsetX = uiState.config.clickOffsetX,
                        clickOffsetY = uiState.config.clickOffsetY,
                        onOffsetChange = { x, y ->
                            viewModel.updateConfig { it.copy(clickOffsetX = x, clickOffsetY = y) }
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            viewModel.updateConfig { it.copy(clickOffsetX = 0f, clickOffsetY = 0f) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        val resetText = when (uiState.config.cursorStyle) {
                            CursorStyle.DOT_POINTER, CursorStyle.PRECISION_CROSSHAIR, CursorStyle.TARGET_RING -> "Reset to Exact Center (0, 0)"
                            else -> "Reset to Exact Tip (0, 0)"
                        }
                        Text(resetText)
                    }
                }
            }
        }

        // 5. Scroll Speed & Step Size Calibration
        item {
            ScrollSpeedCalibrationCard(
                continuousScrollSpeedMs = uiState.config.continuousScrollSpeedMs,
                scrollStepDistancePx = uiState.config.scrollStepDistancePx,
                onContinuousSpeedChange = { ms ->
                    viewModel.updateConfig { it.copy(continuousScrollSpeedMs = ms) }
                },
                onStepDistanceChange = { px ->
                    viewModel.updateConfig { it.copy(scrollStepDistancePx = px) }
                }
            )
        }

        // 5. Auto-Click on Hover
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    AdaptiveSettingSwitchRow(
                        title = "Auto-Click on Hover",
                        subtitle = "Automatically clicks when pointer stays still on an item",
                        checked = uiState.config.dwellAutoClickEnabled,
                        onCheckedChange = { dwell ->
                            viewModel.updateConfig { it.copy(dwellAutoClickEnabled = dwell) }
                        },
                        testTag = "switch_dwell_click"
                    )

                    AnimatedVisibility(visible = uiState.config.dwellAutoClickEnabled) {
                        Column {
                            Spacer(modifier = Modifier.height(10.dp))
                            TuningSliderItem(
                                label = "Hover Delay Before Click",
                                valueText = "${uiState.config.dwellTimeMs} ms",
                                value = uiState.config.dwellTimeMs.toFloat(),
                                valueRange = 500f..2500f,
                                onValueChange = { tm -> viewModel.updateConfig { it.copy(dwellTimeMs = tm.toLong()) } }
                            )
                        }
                    }
                }
            }
        }

        // 6. Sensor Tracking Modes
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    SectionHeader(
                        title = "Sensor Tracking Mode",
                        subtitle = "Choose which motion sensors control pointer movement",
                        icon = Icons.Default.Sensors
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    SensorMode.entries.forEach { mode ->
                        val isSelected = uiState.config.sensorMode == mode
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.updateConfig { it.copy(sensorMode = mode) }
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = mode.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = mode.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(if (uiState.config.navBarStyle == NavigationBarStyle.PILL_IOS) 96.dp else 24.dp))
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    icon: ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
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
}

@Composable
private fun TuningSliderItem(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

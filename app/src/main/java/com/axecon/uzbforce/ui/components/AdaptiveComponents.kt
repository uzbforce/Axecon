package com.axecon.uzbforce.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.axecon.uzbforce.model.CursorConfig
import com.axecon.uzbforce.model.CursorStyle
import kotlin.math.hypot
import kotlin.math.roundToInt
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.axecon.uzbforce.ui.theme.M3Success

/**
 * Responsive switch row that automatically wraps to a clean secondary line on narrow screens / high DPI,
 * eliminating any overlap with text or switches getting clipped.
 */
@Composable
fun AdaptiveSettingSwitchRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "",
    icon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true
) {
    val configuration = LocalConfiguration.current
    val isNarrow = configuration.screenWidthDp < 360

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val availableWidth = maxWidth

        if (availableWidth < 300.dp || isNarrow) {
            // Adaptive Stacked Layout for small widths / high density
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (icon != null) {
                        icon()
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Switch(
                        checked = checked,
                        onCheckedChange = onCheckedChange,
                        enabled = enabled,
                        modifier = Modifier.testTag(testTag)
                    )
                }
            }
        } else {
            // Standard Spacious Horizontal Layout with minimum text flex width
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = true)
                ) {
                    if (icon != null) {
                        icon()
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Column(modifier = Modifier.padding(end = 12.dp)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (subtitle != null) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    enabled = enabled,
                    modifier = Modifier.testTag(testTag)
                )
            }
        }
    }
}

/**
 * Interactive Live Hotspot Calibration Target Board.
 * Shows visual cursor tip or center alignment with exact touch point registration,
 * real-time hit testing, active cursor rendering, and fine-grain pixel nudges.
 */
@Composable
fun InteractiveHotspotCalibrationPad(
    clickOffsetX: Float,
    clickOffsetY: Float,
    onOffsetChange: (x: Float, y: Float) -> Unit,
    modifier: Modifier = Modifier,
    cursorStyle: CursorStyle = CursorStyle.DOT_POINTER,
    cursorColorArgb: Long = 0xFFFFFFFF,
    cursorOutlineColorArgb: Long = 0xFF1E293B,
    cursorOutlineEnabled: Boolean = true,
    cursorOutlineWidth: Float = 2.0f,
    cursorSizeDp: Int = 28
) {
    var hitPoint by remember { mutableStateOf<Offset?>(null) }
    var directSetMode by remember { mutableStateOf(false) }
    var hitResultText by remember {
        mutableStateOf(
            if (cursorStyle == CursorStyle.DOT_POINTER) "Center aligned (0px). Tap target to test click accuracy"
            else "Tip aligned (0px). Tap target to test click accuracy"
        )
    }
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.tertiary
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant

    Column(modifier = modifier.fillMaxWidth()) {
        // Mode & Status Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = "Active Cursor: ${cursorStyle.label}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            FilterChip(
                selected = directSetMode,
                onClick = { directSetMode = !directSetMode },
                label = {
                    Text(
                        if (directSetMode) "Mode: Tap to Position" else "Mode: Test Accuracy",
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (directSetMode) Icons.Default.TouchApp else Icons.Default.Adjust,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                }
            )
        }

        // 1. Live Target Canvas with Real-Time Active Cursor Rendering
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                .pointerInput(cursorStyle, directSetMode) {
                    detectTapGestures { tapOffset ->
                        val targetCenter = Offset(size.width / 2f, size.height / 2f)
                        if (directSetMode) {
                            val newX = (tapOffset.x - targetCenter.x).coerceIn(-100f, 100f)
                            val newY = (tapOffset.y - targetCenter.y).coerceIn(-100f, 100f)
                            onOffsetChange(newX, newY)
                            hitResultText = "Position Set: X ${newX.roundToInt()}px, Y ${newY.roundToInt()}px"
                        } else {
                            hitPoint = tapOffset
                            val touchPointX = targetCenter.x + clickOffsetX
                            val touchPointY = targetCenter.y + clickOffsetY
                            val dist = hypot((tapOffset.x - touchPointX).toDouble(), (tapOffset.y - touchPointY).toDouble()).toFloat()
                            hitResultText = if (dist < 14f) {
                                "🎯 BULLSEYE! Exact touch point hit (dist ${dist.roundToInt()}px)"
                            } else {
                                "Hit: ΔX ${(tapOffset.x - touchPointX).roundToInt()}px, ΔY ${(tapOffset.y - touchPointY).roundToInt()}px"
                            }
                        }
                    }
                }
                .testTag("interactive_calibration_pad"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f

                // Concentric Target Rings
                drawCircle(color = surfaceColor.copy(alpha = 0.5f), radius = 75.dp.toPx(), center = Offset(cx, cy), style = Stroke(width = 1.5.dp.toPx()))
                drawCircle(color = primaryColor.copy(alpha = 0.25f), radius = 50.dp.toPx(), center = Offset(cx, cy), style = Stroke(width = 1.5.dp.toPx()))
                drawCircle(color = primaryColor.copy(alpha = 0.45f), radius = 25.dp.toPx(), center = Offset(cx, cy), style = Stroke(width = 2.dp.toPx()))
                drawCircle(color = primaryColor, radius = 5.dp.toPx(), center = Offset(cx, cy))

                // Crosshair coordinate axes
                drawLine(color = primaryColor.copy(alpha = 0.35f), start = Offset(cx - 85.dp.toPx(), cy), end = Offset(cx + 85.dp.toPx(), cy), strokeWidth = 1.5.dp.toPx())
                drawLine(color = primaryColor.copy(alpha = 0.35f), start = Offset(cx, cy - 85.dp.toPx()), end = Offset(cx, cy + 85.dp.toPx()), strokeWidth = 1.5.dp.toPx())

                // 2. Render the Active Cursor Style exactly as it appears on screen
                val cursorColor = Color(cursorColorArgb)
                val outlineColor = Color(cursorOutlineColorArgb)
                val strokeW = cursorOutlineWidth.dp.toPx()

                when (cursorStyle) {
                    CursorStyle.DOT_POINTER -> {
                        val dotRadius = (cursorSizeDp * 0.38f).dp.toPx()
                        if (cursorOutlineEnabled) {
                            drawCircle(
                                color = outlineColor,
                                radius = dotRadius + strokeW / 2f,
                                center = Offset(cx, cy),
                                style = Stroke(width = strokeW)
                            )
                        }
                        drawCircle(color = cursorColor, radius = dotRadius, center = Offset(cx, cy))
                        // Subtle center guide dot to highlight exact geometrical center
                        drawCircle(color = outlineColor.copy(alpha = 0.6f), radius = 2.dp.toPx(), center = Offset(cx, cy))
                    }
                    CursorStyle.CLASSIC_ARROW -> {
                        val s = cursorSizeDp.dp.toPx()
                        val arrowPath = Path().apply {
                            moveTo(cx, cy)
                            lineTo(cx, cy + s)
                            lineTo(cx + s * 0.25f, cy + s * 0.64f)
                            lineTo(cx + s * 0.50f, cy + s * 0.93f)
                            lineTo(cx + s * 0.64f, cy + s * 0.82f)
                            lineTo(cx + s * 0.39f, cy + s * 0.54f)
                            lineTo(cx + s * 0.64f, cy + s * 0.54f)
                            close()
                        }
                        if (cursorOutlineEnabled) {
                            drawPath(arrowPath, color = outlineColor, style = Stroke(width = strokeW))
                        }
                        drawPath(arrowPath, color = cursorColor)
                    }
                    CursorStyle.PRECISION_CROSSHAIR -> {
                        val r = (cursorSizeDp * 0.45f).dp.toPx()
                        if (cursorOutlineEnabled) {
                            drawCircle(color = outlineColor, radius = r, center = Offset(cx, cy), style = Stroke(width = strokeW + 2f))
                        }
                        drawCircle(color = cursorColor, radius = r, center = Offset(cx, cy), style = Stroke(width = strokeW))
                        drawLine(color = cursorColor, start = Offset(cx - r * 1.35f, cy), end = Offset(cx + r * 1.35f, cy), strokeWidth = strokeW)
                        drawLine(color = cursorColor, start = Offset(cx, cy - r * 1.35f), end = Offset(cx, cy + r * 1.35f), strokeWidth = strokeW)
                        drawCircle(color = cursorColor, radius = 2.5.dp.toPx(), center = Offset(cx, cy))
                    }
                    CursorStyle.TARGET_RING -> {
                        val r = (cursorSizeDp * 0.48f).dp.toPx()
                        if (cursorOutlineEnabled) {
                            drawCircle(color = outlineColor, radius = r, center = Offset(cx, cy), style = Stroke(width = strokeW + 2f))
                        }
                        drawCircle(color = cursorColor, radius = r, center = Offset(cx, cy), style = Stroke(width = strokeW))
                        drawCircle(color = cursorColor, radius = r * 0.42f, center = Offset(cx, cy), style = Stroke(width = strokeW))
                        drawCircle(color = cursorColor, radius = 2.5.dp.toPx(), center = Offset(cx, cy))
                    }
                    CursorStyle.TEARDROP -> {
                        val s = cursorSizeDp.dp.toPx()
                        val tdPath = Path().apply {
                            moveTo(cx, cy)
                            cubicTo(cx + s * 0.7f, cy + s * 0.2f, cx + s * 0.7f, cy + s * 0.8f, cx + s * 0.35f, cy + s * 0.8f)
                            cubicTo(cx, cy + s * 0.8f, cx, cy + s * 0.4f, cx, cy)
                            close()
                        }
                        if (cursorOutlineEnabled) {
                            drawPath(tdPath, color = outlineColor, style = Stroke(width = strokeW))
                        }
                        drawPath(tdPath, color = cursorColor)
                    }
                    CursorStyle.STYLUS_PEN -> {
                        val s = cursorSizeDp.dp.toPx()
                        val penPath = Path().apply {
                            moveTo(cx, cy)
                            lineTo(cx + s * 0.3f, cy + s * 0.15f)
                            lineTo(cx + s * 0.8f, cy + s * 0.9f)
                            lineTo(cx + s * 0.65f, cy + s * 1.0f)
                            lineTo(cx + s * 0.15f, cy + s * 0.3f)
                            close()
                        }
                        if (cursorOutlineEnabled) {
                            drawPath(penPath, color = outlineColor, style = Stroke(width = strokeW))
                        }
                        drawPath(penPath, color = cursorColor)
                    }
                    CursorStyle.CUSTOM_IMAGE -> {
                        val r = (cursorSizeDp * 0.4f).dp.toPx()
                        drawCircle(color = cursorColor, radius = r, center = Offset(cx, cy))
                        drawCircle(color = outlineColor, radius = r, center = Offset(cx, cy), style = Stroke(width = strokeW))
                    }
                }

                // 3. Registered Touch Hit Point Dot
                val touchPointX = cx + clickOffsetX
                val touchPointY = cy + clickOffsetY
                drawCircle(color = Color(0xFFEF4444).copy(alpha = 0.35f), radius = 12.dp.toPx(), center = Offset(touchPointX, touchPointY), style = Stroke(width = 2.dp.toPx()))
                drawCircle(color = Color(0xFFEF4444), radius = 4.5.dp.toPx(), center = Offset(touchPointX, touchPointY))
                // Micro crosshair on click point
                drawLine(color = Color(0xFFEF4444), start = Offset(touchPointX - 6.dp.toPx(), touchPointY), end = Offset(touchPointX + 6.dp.toPx(), touchPointY), strokeWidth = 1.5.dp.toPx())
                drawLine(color = Color(0xFFEF4444), start = Offset(touchPointX, touchPointY - 6.dp.toPx()), end = Offset(touchPointX, touchPointY + 6.dp.toPx()), strokeWidth = 1.5.dp.toPx())

                // Draw user tap point if any
                hitPoint?.let { hp ->
                    drawCircle(color = Color(0xFF10B981), radius = 5.dp.toPx(), center = hp)
                    drawCircle(color = Color(0xFF10B981).copy(alpha = 0.3f), radius = 14.dp.toPx(), center = hp, style = Stroke(width = 2.dp.toPx()))
                }
            }

            // Status chip overlay
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
            ) {
                Text(
                    text = hitResultText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Vertical Y-Offset Adjuster with Pixel Nudges (Limit increased to 100px)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Vertical Click Offset (Y-Axis)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${clickOffsetY.roundToInt()} px (" + when {
                        clickOffsetY == 0f -> if (cursorStyle == CursorStyle.DOT_POINTER) "exact dot center" else "exact pointer tip"
                        clickOffsetY > 0 -> "clicks +${clickOffsetY.roundToInt()}px lower"
                        else -> "clicks ${clickOffsetY.roundToInt()}px higher"
                    } + ")",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilledTonalIconButton(
                    onClick = { onOffsetChange(clickOffsetX, (clickOffsetY - 10f).coerceIn(-100f, 100f)) },
                    modifier = Modifier.size(34.dp).testTag("btn_y_nudge_up_10")
                ) {
                    Text("-10", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                FilledTonalIconButton(
                    onClick = { onOffsetChange(clickOffsetX, (clickOffsetY - 1f).coerceIn(-100f, 100f)) },
                    modifier = Modifier.size(34.dp).testTag("btn_y_nudge_up_1")
                ) {
                    Icon(imageVector = Icons.Default.Remove, contentDescription = "Higher by 1px", modifier = Modifier.size(16.dp))
                }
                FilledTonalIconButton(
                    onClick = { onOffsetChange(clickOffsetX, (clickOffsetY + 1f).coerceIn(-100f, 100f)) },
                    modifier = Modifier.size(34.dp).testTag("btn_y_nudge_down_1")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Lower by 1px", modifier = Modifier.size(16.dp))
                }
                FilledTonalIconButton(
                    onClick = { onOffsetChange(clickOffsetX, (clickOffsetY + 10f).coerceIn(-100f, 100f)) },
                    modifier = Modifier.size(34.dp).testTag("btn_y_nudge_down_10")
                ) {
                    Text("+10", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }

        Slider(
            value = clickOffsetY,
            onValueChange = { y -> onOffsetChange(clickOffsetX, y) },
            valueRange = -100f..100f,
            modifier = Modifier.fillMaxWidth().testTag("slider_click_offset_y")
        )

        // 3. Quick Offset Presets (Contextual for Dot vs Arrow)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val presets = if (cursorStyle == CursorStyle.DOT_POINTER || cursorStyle == CursorStyle.PRECISION_CROSSHAIR || cursorStyle == CursorStyle.TARGET_RING) {
                listOf(
                    -20f to "High (-20px)",
                    0f to "Center (0px)",
                    20f to "Lower (+20px)",
                    40f to "Offset (+40px)"
                )
            } else {
                listOf(
                    0f to "Tip (0px)",
                    10f to "Under Tip (+10px)",
                    22f to "Body (+22px)",
                    45f to "Offset (+45px)"
                )
            }

            presets.forEach { (yVal, label) ->
                val isSelected = clickOffsetY.roundToInt() == yVal.roundToInt()
                FilterChip(
                    selected = isSelected,
                    onClick = { onOffsetChange(clickOffsetX, yVal) },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 4. Horizontal X-Offset Adjuster with Pixel Nudges (Limit increased to 100px)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Horizontal Click Offset (X-Axis)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${clickOffsetX.roundToInt()} px (" + when {
                        clickOffsetX == 0f -> "exact center"
                        clickOffsetX > 0 -> "+${clickOffsetX.roundToInt()}px right"
                        else -> "${clickOffsetX.roundToInt()}px left"
                    } + ")",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilledTonalIconButton(
                    onClick = { onOffsetChange((clickOffsetX - 10f).coerceIn(-100f, 100f), clickOffsetY) },
                    modifier = Modifier.size(34.dp).testTag("btn_x_nudge_left_10")
                ) {
                    Text("-10", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                FilledTonalIconButton(
                    onClick = { onOffsetChange((clickOffsetX - 1f).coerceIn(-100f, 100f), clickOffsetY) },
                    modifier = Modifier.size(34.dp).testTag("btn_x_nudge_left_1")
                ) {
                    Icon(imageVector = Icons.Default.Remove, contentDescription = "Move left 1px", modifier = Modifier.size(16.dp))
                }
                FilledTonalIconButton(
                    onClick = { onOffsetChange((clickOffsetX + 1f).coerceIn(-100f, 100f), clickOffsetY) },
                    modifier = Modifier.size(34.dp).testTag("btn_x_nudge_right_1")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Move right 1px", modifier = Modifier.size(16.dp))
                }
                FilledTonalIconButton(
                    onClick = { onOffsetChange((clickOffsetX + 10f).coerceIn(-100f, 100f), clickOffsetY) },
                    modifier = Modifier.size(34.dp).testTag("btn_x_nudge_right_10")
                ) {
                    Text("+10", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }

        Slider(
            value = clickOffsetX,
            onValueChange = { x -> onOffsetChange(x, clickOffsetY) },
            valueRange = -100f..100f,
            modifier = Modifier.fillMaxWidth().testTag("slider_click_offset_x")
        )
    }
}

/**
 * Convenience overload accepting CursorConfig directly.
 */
@Composable
fun InteractiveHotspotCalibrationPad(
    config: CursorConfig,
    clickOffsetX: Float,
    clickOffsetY: Float,
    onOffsetChange: (x: Float, y: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    InteractiveHotspotCalibrationPad(
        clickOffsetX = clickOffsetX,
        clickOffsetY = clickOffsetY,
        onOffsetChange = onOffsetChange,
        modifier = modifier,
        cursorStyle = config.cursorStyle,
        cursorColorArgb = config.cursorColorArgb,
        cursorOutlineColorArgb = config.cursorOutlineColorArgb,
        cursorOutlineEnabled = config.cursorOutlineEnabled,
        cursorOutlineWidth = config.cursorOutlineWidth,
        cursorSizeDp = config.cursorSizeDp
    )
}

/**
 * Scroll Speed & Step Size Calibration Card.
 * Allows users to configure continuous volume-scroll velocity (interval ms)
 * and step scroll distance (pixels per step), complete with live test playground.
 */
@Composable
fun ScrollSpeedCalibrationCard(
    continuousScrollSpeedMs: Long,
    scrollStepDistancePx: Int,
    onContinuousSpeedChange: (Long) -> Unit,
    onStepDistanceChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val speedDescriptor = when {
        continuousScrollSpeedMs <= 70L -> "⚡ Ultra Turbo (Fastest)"
        continuousScrollSpeedMs <= 110L -> "🏎️ Fast"
        continuousScrollSpeedMs <= 180L -> "🚶 Normal (Smooth)"
        else -> "🐢 Relaxed / Slow"
    }

    val stepDescriptor = when {
        scrollStepDistancePx <= 250 -> "Short Step (${scrollStepDistancePx}px)"
        scrollStepDistancePx <= 500 -> "Standard Step (${scrollStepDistancePx}px)"
        scrollStepDistancePx <= 750 -> "Deep Scroll (${scrollStepDistancePx}px)"
        else -> "Full Page Jump (${scrollStepDistancePx}px)"
    }

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.SwapVert,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Scroll Speed & Step Size",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Control continuous volume key scroll speed and step distance",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 1. Continuous Scroll Speed (Interval in ms)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Continuous Scroll Speed (Hold Key)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$speedDescriptor • ${continuousScrollSpeedMs}ms repeat interval",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilledTonalIconButton(
                        onClick = { onContinuousSpeedChange((continuousScrollSpeedMs + 15L).coerceIn(40L, 350L)) },
                        modifier = Modifier.size(34.dp).testTag("btn_scroll_slower")
                    ) {
                        Icon(imageVector = Icons.Default.Remove, contentDescription = "Slower", modifier = Modifier.size(16.dp))
                    }
                    FilledTonalIconButton(
                        onClick = { onContinuousSpeedChange((continuousScrollSpeedMs - 15L).coerceIn(40L, 350L)) },
                        modifier = Modifier.size(34.dp).testTag("btn_scroll_faster")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Faster", modifier = Modifier.size(16.dp))
                    }
                }
            }

            Slider(
                value = continuousScrollSpeedMs.toFloat(),
                onValueChange = { onContinuousSpeedChange(it.toLong()) },
                valueRange = 40f..350f,
                modifier = Modifier.fillMaxWidth().testTag("slider_continuous_scroll_speed")
            )

            // Speed Presets
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val speedPresets = listOf(
                    60L to "Turbo (60ms)",
                    110L to "Fast (110ms)",
                    160L to "Normal (160ms)",
                    250L to "Slow (250ms)"
                )
                speedPresets.forEach { (ms, label) ->
                    val isSelected = continuousScrollSpeedMs == ms
                    FilterChip(
                        selected = isSelected,
                        onClick = { onContinuousSpeedChange(ms) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 2. Step Scroll Distance (Pixels per step)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Step Scroll Distance (Single Tap)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$stepDescriptor • pixels per scroll gesture",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilledTonalIconButton(
                        onClick = { onStepDistanceChange((scrollStepDistancePx - 50).coerceIn(100, 1000)) },
                        modifier = Modifier.size(34.dp).testTag("btn_step_distance_decrease")
                    ) {
                        Icon(imageVector = Icons.Default.Remove, contentDescription = "Shorter distance", modifier = Modifier.size(16.dp))
                    }
                    FilledTonalIconButton(
                        onClick = { onStepDistanceChange((scrollStepDistancePx + 50).coerceIn(100, 1000)) },
                        modifier = Modifier.size(34.dp).testTag("btn_step_distance_increase")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Longer distance", modifier = Modifier.size(16.dp))
                    }
                }
            }

            Slider(
                value = scrollStepDistancePx.toFloat(),
                onValueChange = { onStepDistanceChange(it.roundToInt()) },
                valueRange = 100f..1000f,
                modifier = Modifier.fillMaxWidth().testTag("slider_scroll_step_distance")
            )

            // Step Presets
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val stepPresets = listOf(
                    250 to "Short (250px)",
                    450 to "Standard (450px)",
                    650 to "Long (650px)",
                    850 to "Page (850px)"
                )
                stepPresets.forEach { (px, label) ->
                    val isSelected = scrollStepDistancePx == px
                    FilterChip(
                        selected = isSelected,
                        onClick = { onStepDistanceChange(px) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Interactive Live Scroll Test Playground
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("scroll_test_playground")
                ) {
                    items((1..30).toList()) { idx ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = if (idx % 2 == 0) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Feed Item #$idx", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                Text("${idx * scrollStepDistancePx} px mark", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            Text(
                text = "💡 Practice scrolling this list using your volume buttons or swipe to feel the calibrated speed and step distance.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

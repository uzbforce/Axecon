package com.axecon.uzbforce.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.axecon.uzbforce.model.NavigationBarStyle
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.axecon.uzbforce.model.CursorAction
import com.axecon.uzbforce.model.CursorStyle
import com.axecon.uzbforce.ui.components.InteractiveHotspotCalibrationPad
import com.axecon.uzbforce.ui.theme.M3Success
import com.axecon.uzbforce.ui.viewmodel.AirCursorViewModel
import com.axecon.uzbforce.ui.viewmodel.UiState
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun TestPlaygroundScreen(
    viewModel: AirCursorViewModel,
    uiState: UiState
) {
    var tapScore by remember { mutableIntStateOf(0) }
    var doubleTapScore by remember { mutableIntStateOf(0) }
    var longPressScore by remember { mutableIntStateOf(0) }
    var dragDropScore by remember { mutableIntStateOf(0) }
    var swipeScore by remember { mutableIntStateOf(0) }
    var lastInteractedTarget by remember { mutableStateOf("None") }

    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    // Drag-and-drop items state
    var item1Offset by remember { mutableStateOf(Offset.Zero) }
    var item2Offset by remember { mutableStateOf(Offset.Zero) }
    var item3Offset by remember { mutableStateOf(Offset.Zero) }

    // Swipe test state
    var lastSwipeDirection by remember { mutableStateOf("Swipe on pad to test") }
    var lastSwipeSpeed by remember { mutableStateOf("0 px/s") }

    // Keypad entry state
    var enteredPhoneNumber by remember { mutableStateOf("") }
    var lastKeyPressed by remember { mutableStateOf("None") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Playground Header
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Gamepad,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Hands-Free Practice Arena",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Practice tilting to cursor targets and clicking with volume keys",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = {
                                tapScore = 0
                                doubleTapScore = 0
                                longPressScore = 0
                                dragDropScore = 0
                                swipeScore = 0
                                lastInteractedTarget = "Reset"
                                item1Offset = Offset.Zero
                                item2Offset = Offset.Zero
                                item3Offset = Offset.Zero
                                enteredPhoneNumber = ""
                                lastKeyPressed = "Reset"
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("reset_scores_btn")
                        ) {
                            Text("Reset")
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ScoreBadge(label = "Taps", score = tapScore, color = MaterialTheme.colorScheme.primary)
                        ScoreBadge(label = "Double Taps", score = doubleTapScore, color = MaterialTheme.colorScheme.secondary)
                        ScoreBadge(label = "Hold Presses", score = longPressScore, color = MaterialTheme.colorScheme.tertiary)
                        ScoreBadge(label = "Drag & Drop", score = dragDropScore, color = Color(0xFF8B5CF6))
                        ScoreBadge(label = "Swipes", score = swipeScore, color = Color(0xFF06B6D4))
                    }
                }
            }
        }

        // Target Clicks Grid
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
                        text = "Precision Target Clicking",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Move cursor with phone tilt, then press mapped button to click",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TargetButton(
                            label = "Target A",
                            color = MaterialTheme.colorScheme.primaryContainer,
                            onClick = {
                                tapScore++
                                lastInteractedTarget = "Target A"
                            },
                            modifier = Modifier.weight(1f)
                        )

                        TargetButton(
                            label = "Target B",
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            onClick = {
                                tapScore++
                                lastInteractedTarget = "Target B"
                            },
                            modifier = Modifier.weight(1f)
                        )

                        TargetButton(
                            label = "Target C",
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            onClick = {
                                tapScore++
                                lastInteractedTarget = "Target C"
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TargetButton(
                            label = "Double Tap Here",
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            onClick = {
                                doubleTapScore++
                                lastInteractedTarget = "Double Tap Zone"
                            },
                            modifier = Modifier.weight(1f)
                        )

                        TargetButton(
                            label = "Hold Press Zone",
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            onClick = {
                                longPressScore++
                                lastInteractedTarget = "Hold Press Zone"
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Active Cursor Hotspot & Click Alignment Testing Card
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
                                text = "Cursor Hotspot & Alignment Calibration",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (uiState.config.cursorStyle == CursorStyle.DOT_POINTER)
                                    "Active: ${uiState.config.cursorStyle.label} (clicks at dot center). Tap or fine-tune offset."
                                else
                                    "Active: ${uiState.config.cursorStyle.label} (clicks at pointer tip). Tap or fine-tune offset.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

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
                            CursorStyle.DOT_POINTER, CursorStyle.PRECISION_CROSSHAIR, CursorStyle.TARGET_RING -> "Reset to Exact Dot Center (0, 0)"
                            else -> "Reset to Exact Pointer Tip (0, 0)"
                        }
                        Text(resetText)
                    }
                }
            }
        }

        // Scrolling Test Arena
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
                        text = "Touchless Scrolling Practice",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Double tap & hold volume keys to trigger automated smooth scrolling",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Vertical Scroll Container
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(140.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .verticalScroll(verticalScrollState)
                                .padding(8.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                (1..15).forEach { item ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Vertical Feed #$item",
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(8.dp),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        // Horizontal Scroll Container
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(140.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .horizontalScroll(horizontalScrollState)
                                .padding(8.dp)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                (1..12).forEach { item ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.size(90.dp, 124.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "Card #$item",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
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

        // Drag & Drop Interactive Arena
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
                                text = "Drag & Drop Interactive Arena",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Hold and move items into Target Vaults A or B",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        TextButton(
                            onClick = {
                                item1Offset = Offset.Zero
                                item2Offset = Offset.Zero
                                item3Offset = Offset.Zero
                            }
                        ) {
                            Text("Reset Items")
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Draggable Items Shelf
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "DRAGGABLE ITEMS (Hold & Move)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                // Item 1: Target Disk
                                Surface(
                                    modifier = Modifier
                                        .offset { IntOffset(item1Offset.x.roundToInt(), item1Offset.y.roundToInt()) }
                                        .pointerInput(Unit) {
                                            detectDragGestures(
                                                onDragEnd = {
                                                    if (item1Offset.y > 60) {
                                                        val zone = if (item1Offset.x < 0) "Vault Alpha" else "Vault Beta"
                                                        dragDropScore++
                                                        lastInteractedTarget = "Dropped 🎯 Orb into $zone"
                                                    }
                                                }
                                            ) { change, dragAmount ->
                                                change.consume()
                                                item1Offset += dragAmount
                                            }
                                        },
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFF0284C7),
                                    border = BorderStroke(1.5.dp, Color.White),
                                    shadowElevation = 6.dp
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                    ) {
                                        Text("🎯", fontSize = 24.sp)
                                        Text("Orb", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // Item 2: Gem Crystal
                                Surface(
                                    modifier = Modifier
                                        .offset { IntOffset(item2Offset.x.roundToInt(), item2Offset.y.roundToInt()) }
                                        .pointerInput(Unit) {
                                            detectDragGestures(
                                                onDragEnd = {
                                                    if (item2Offset.y > 60) {
                                                        val zone = if (item2Offset.x < 0) "Vault Alpha" else "Vault Beta"
                                                        dragDropScore++
                                                        lastInteractedTarget = "Dropped 💎 Crystal into $zone"
                                                    }
                                                }
                                            ) { change, dragAmount ->
                                                change.consume()
                                                item2Offset += dragAmount
                                            }
                                        },
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFF7C3AED),
                                    border = BorderStroke(1.5.dp, Color.White),
                                    shadowElevation = 6.dp
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                    ) {
                                        Text("💎", fontSize = 24.sp)
                                        Text("Crystal", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // Item 3: Rocket Booster
                                Surface(
                                    modifier = Modifier
                                        .offset { IntOffset(item3Offset.x.roundToInt(), item3Offset.y.roundToInt()) }
                                        .pointerInput(Unit) {
                                            detectDragGestures(
                                                onDragEnd = {
                                                    if (item3Offset.y > 60) {
                                                        val zone = if (item3Offset.x < 0) "Vault Alpha" else "Vault Beta"
                                                        dragDropScore++
                                                        lastInteractedTarget = "Dropped 🚀 Rocket into $zone"
                                                    }
                                                }
                                            ) { change, dragAmount ->
                                                change.consume()
                                                item3Offset += dragAmount
                                            }
                                        },
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFFEA580C),
                                    border = BorderStroke(1.5.dp, Color.White),
                                    shadowElevation = 6.dp
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                    ) {
                                        Text("🚀", fontSize = 24.sp)
                                        Text("Rocket", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Drop Target Zones
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(100.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF0F766E).copy(alpha = 0.18f),
                            border = BorderStroke(1.5.dp, Color(0xFF14B8A6))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("📥", fontSize = 22.sp)
                                Text("Vault Alpha", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = Color(0xFF14B8A6))
                                Text("Drop Target A", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(100.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF6D28D9).copy(alpha = 0.18f),
                            border = BorderStroke(1.5.dp, Color(0xFFA78BFA))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("📥", fontSize = 22.sp)
                                Text("Vault Beta", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = Color(0xFFA78BFA))
                                Text("Drop Target B", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        // Swipe & Flick Velocity Pad (Tests SWIPE_TOGGLE Gesture)
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
                        text = "Swipe & Flick Velocity Pad",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Practice quick swipe flicks (Left, Right, Up, Down)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    var accumulatedDrag by remember { mutableStateOf(Offset.Zero) }
                    var dragStartTime by remember { mutableStateOf(0L) }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = {
                                        accumulatedDrag = Offset.Zero
                                        dragStartTime = System.currentTimeMillis()
                                    },
                                    onDragEnd = {
                                        val durationMs = (System.currentTimeMillis() - dragStartTime).coerceAtLeast(1L)
                                        val dx = accumulatedDrag.x
                                        val dy = accumulatedDrag.y
                                        val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                                        val speed = (dist / (durationMs / 1000f)).roundToInt()

                                        if (dist > 30) {
                                            swipeScore++
                                            val dir = if (abs(dx) > abs(dy)) {
                                                if (dx > 0) "➔ FLICK RIGHT" else "⬅ FLICK LEFT"
                                            } else {
                                                if (dy > 0) "⬇ FLICK DOWN" else "⬆ FLICK UP"
                                            }
                                            lastSwipeDirection = dir
                                            lastSwipeSpeed = "$speed px/s (${durationMs}ms)"
                                            lastInteractedTarget = "Swipe: $dir ($speed px/s)"
                                        }
                                    }
                                ) { change, dragAmount ->
                                    change.consume()
                                    accumulatedDrag += dragAmount
                                }
                            },
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF06B6D4).copy(alpha = 0.12f),
                        border = BorderStroke(1.5.dp, Color(0xFF06B6D4).copy(alpha = 0.6f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("⚡", fontSize = 24.sp)
                            Text(
                                text = lastSwipeDirection,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0891B2)
                            )
                            Text(
                                text = "Speed: $lastSwipeSpeed",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Phone Number Dialpad & Security Node Click Verification
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
                        text = "Phone Number & Permission Keypad Test",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Simulates WhatsApp number entry & secure permission dialog clicks",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Phone Number Display Field
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (enteredPhoneNumber.isEmpty()) "Enter phone number..." else enteredPhoneNumber,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (enteredPhoneNumber.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                            )

                            if (enteredPhoneNumber.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        enteredPhoneNumber = enteredPhoneNumber.dropLast(1)
                                        lastKeyPressed = "Backspace"
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Backspace,
                                        contentDescription = "Backspace",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Dialpad 3x4 grid
                    val dialpadRows = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("*", "0", "#")
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        dialpadRows.forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                row.forEach { digit ->
                                    Surface(
                                        onClick = {
                                            if (enteredPhoneNumber.length < 15) {
                                                enteredPhoneNumber += digit
                                            }
                                            tapScore++
                                            lastKeyPressed = digit
                                            lastInteractedTarget = "Keypad Digit: $digit"
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = digit,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "✓ Node Click Verified: Last key registered: [$lastKeyPressed] (${enteredPhoneNumber.length} digits)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF059669),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(if (uiState.config.navBarStyle == NavigationBarStyle.PILL_IOS) 96.dp else 20.dp))
        }
    }
}

@Composable
private fun ScoreBadge(label: String, score: Int, color: Color) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.12f),
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(
                text = "$score",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TargetButton(
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(56.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = color
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

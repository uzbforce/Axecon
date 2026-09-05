package com.axecon.uzbforce.ui

import android.provider.Settings
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.axecon.uzbforce.model.NavigationBarStyle
import com.axecon.uzbforce.ui.screens.ButtonMappingScreen
import com.axecon.uzbforce.ui.screens.DashboardScreen
import com.axecon.uzbforce.ui.screens.MotionTuningScreen
import com.axecon.uzbforce.ui.screens.PermissionScreen
import com.axecon.uzbforce.ui.screens.SettingsScreen
import com.axecon.uzbforce.ui.screens.TestPlaygroundScreen
import com.axecon.uzbforce.ui.theme.M3Success
import com.axecon.uzbforce.ui.viewmodel.AirCursorViewModel

data class NavTabItem(
    val label: String,
    val icon: ImageVector,
    val index: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AirCursorMainApp(viewModel: AirCursorViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.checkPermissions(context)
    }

    val showOnboarding = !uiState.hasCompletedPermissionFlow || !uiState.permissions.areMandatoryGranted
    val isReviewing = uiState.isReviewingPermissions

    val navTabs = listOf(
        NavTabItem("Home", Icons.Default.Dashboard, 0),
        NavTabItem("Testing", Icons.Default.Gamepad, 1),
        NavTabItem("Buttons", Icons.Default.VolumeUp, 2),
        NavTabItem("Pointer", Icons.Default.Tune, 3),
        NavTabItem("Settings", Icons.Default.Settings, 4)
    )

    if (showOnboarding || isReviewing) {
        PermissionScreen(
            viewModel = viewModel,
            permissions = uiState.permissions,
            onAllPermissionsGranted = {
                viewModel.checkPermissions(context)
                if (uiState.permissions.areMandatoryGranted) {
                    viewModel.confirmPermissionsAndProceed()
                }
            },
            onBack = if (isReviewing && uiState.permissions.areMandatoryGranted) {
                { viewModel.closePermissionsReview() }
            } else null
        )
    } else {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                TopAppBar(
                    windowInsets = TopAppBarDefaults.windowInsets,
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Dashboard,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Axecon",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (uiState.config.isEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = if (uiState.config.isEnabled) "ACTIVE" else "PAUSED",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (uiState.config.isEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Text(
                                    text = "Motion Pointer & Hardware Buttons",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.openPermissionsReview() },
                            modifier = Modifier.testTag("top_bar_permissions_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Permissions",
                                tint = if (uiState.permissions.areAllGranted) M3Success else MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(
                            onClick = { viewModel.recenterAndCalibrate() },
                            modifier = Modifier.testTag("top_bar_recenter_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Recenter",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            bottomBar = {
                val navInfo = rememberSystemNavInfo()
                if (uiState.config.navBarStyle == NavigationBarStyle.STANDARD_M3) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 3.dp,
                        windowInsets = WindowInsets(bottom = navInfo.bottomInset),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        navTabs.forEach { tab ->
                            val isSelected = uiState.selectedTab == tab.index
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { viewModel.setTab(tab.index) },
                                icon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.label,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("nav_tab_${tab.index}")
                            )
                        }
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            val isPillMode = uiState.config.navBarStyle == NavigationBarStyle.PILL_IOS

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = paddingValues.calculateTopPadding(),
                        bottom = if (isPillMode) 0.dp else paddingValues.calculateBottomPadding()
                    )
            ) {
                Crossfade(
                    targetState = uiState.selectedTab,
                    animationSpec = tween(durationMillis = 140),
                    label = "tabCrossfade",
                    modifier = Modifier.fillMaxSize()
                ) { tab ->
                    when (tab) {
                        0 -> DashboardScreen(viewModel = viewModel, uiState = uiState, onNavigateToTab = { viewModel.setTab(it) })
                        1 -> TestPlaygroundScreen(viewModel = viewModel, uiState = uiState)
                        2 -> ButtonMappingScreen(viewModel = viewModel, uiState = uiState)
                        3 -> MotionTuningScreen(viewModel = viewModel, uiState = uiState)
                        4 -> SettingsScreen(viewModel = viewModel, uiState = uiState)
                    }
                }

                // Immersive floating pill rendered purely on top of the scrolling canvas with zero background strip behind it
                if (isPillMode) {
                    IosPillNavigationBar(
                        selectedTab = uiState.selectedTab,
                        onTabSelected = { viewModel.setTab(it) },
                        tabs = navTabs,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }
}

/**
 * System navigation mode and inset detector.
 * Detects 3-button navigation (including Xiaomi MIUI / HyperOS and standard Android settings)
 * and measures the required inset so floating and docked navigation bars never get obstructed.
 */
data class SystemNavInfo(
    val isButtonNav: Boolean,
    val bottomInset: Dp
)

@Composable
fun rememberSystemNavInfo(): SystemNavInfo {
    val context = LocalContext.current
    val view = LocalView.current
    val density = LocalDensity.current

    // Android Settings.Secure.navigation_mode: 0 = 3 buttons, 1 = 2 buttons, 2 = gestures
    val secureNavMode = remember {
        try {
            Settings.Secure.getInt(context.contentResolver, "navigation_mode", -1)
        } catch (e: Exception) {
            -1
        }
    }

    // Xiaomi MIUI / HyperOS full screen gesture indicator setting:
    // 0 = 3 button navigation, 1 = full screen gestures
    val miuiFsg = remember {
        try {
            Settings.Global.getInt(context.contentResolver, "force_fsg_nav_bar", -1)
        } catch (e: Exception) {
            -1
        }
    }

    // Measure insets from Compose WindowInsets
    val navBarsBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val systemBarsBottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()
    val safeDrawingBottom = WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding()
    val composeBottom = maxOf(navBarsBottom, systemBarsBottom, safeDrawingBottom)

    // Measure insets from ViewCompat
    val viewNavBottom = remember(view) {
        val root = ViewCompat.getRootWindowInsets(view)
        val nav = root?.getInsets(WindowInsetsCompat.Type.navigationBars())?.bottom ?: 0
        val sys = root?.getInsets(WindowInsetsCompat.Type.systemBars())?.bottom ?: 0
        val maxPx = maxOf(nav, sys)
        if (maxPx > 0) with(density) { maxPx.toDp() } else 0.dp
    }

    // Fallback resource dimension for navigation bar height
    val resNavHeight = remember {
        val resId = context.resources.getIdentifier("navigation_bar_height", "dimen", "android")
        if (resId > 0) {
            with(density) { context.resources.getDimensionPixelSize(resId).toDp() }
        } else {
            48.dp
        }
    }

    // Determine if device is in 3-button navigation mode
    val isButtonNav = when {
        secureNavMode == 0 || secureNavMode == 1 -> true
        secureNavMode == 2 -> false
        miuiFsg == 0 -> true
        miuiFsg == 1 -> false
        composeBottom >= 36.dp || viewNavBottom >= 36.dp -> true
        else -> false
    }

    // Clearance calculation:
    // In 3-button mode: ensure clearance of at least 48dp (or measured height if larger)
    // In gesture mode: use gesture handle inset (usually 12-20dp, or 0 if hidden)
    val bottomInset = if (isButtonNav) {
        val detected = maxOf(composeBottom, viewNavBottom)
        if (detected >= 36.dp) detected else resNavHeight.coerceAtLeast(48.dp)
    } else {
        maxOf(composeBottom, viewNavBottom).coerceAtMost(24.dp)
    }

    return SystemNavInfo(isButtonNav = isButtonNav, bottomInset = bottomInset)
}

/**
 * iOS-style Floating Pill Navigation Bar
 * Features:
 * - Perfectly aligned floating capsule container with subtle border & depth
 * - Active pill highlighter with elastic wobble animation using spring physics
 * - Smooth transition and clean haptic/visual feedback
 * - Dynamic system navigation clearance (stays completely above 3-button navigation,
 *   and aligns cleanly at the bottom in gesture mode)
 */
@Composable
fun IosPillNavigationBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    tabs: List<NavTabItem>,
    modifier: Modifier = Modifier
) {
    val navInfo = rememberSystemNavInfo()

    // If 3-button navigation is active, elevate pill completely above buttons + 10dp breathing room.
    // If gesture navigation is active, align pill cleanly at bottom with subtle elevation.
    val bottomPadding = if (navInfo.isButtonNav) {
        navInfo.bottomInset + 10.dp
    } else {
        (navInfo.bottomInset + 8.dp).coerceAtLeast(10.dp)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = 4.dp,
                bottom = bottomPadding
            )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(32.dp),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    ambientColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                ),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
            tonalElevation = 6.dp
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(62.dp)
                    .padding(4.dp)
            ) {
                val tabWidth = maxWidth / tabs.size

                var startTab by remember { mutableStateOf(selectedTab) }
                LaunchedEffect(selectedTab) {
                    startTab = selectedTab
                }

                val animatedTabOffset by animateFloatAsState(
                    targetValue = selectedTab.toFloat(),
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy, // Zero wobble, zero overshoot
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "pillIndicatorSpring"
                )

                // iOS Liquid Glass Pill physics:
                // When starting to move, the pill expands in size as momentum builds.
                // At the peak of transit, it reaches maximum expansion.
                // As it settles into the destination slot, it smoothly contracts back to exact original size with zero wobble.
                val totalDistance = kotlin.math.abs(selectedTab - startTab).coerceAtLeast(1).toFloat()
                val distToTarget = kotlin.math.abs(animatedTabOffset - selectedTab.toFloat())
                val progress = (1f - (distToTarget / totalDistance)).coerceIn(0f, 1f)
                // Smooth symmetric parabolic bell curve: 4 * p * (1 - p) is 0 at start, 1.0 at midpoint, 0 at destination
                val liquidBellCurve = (4f * progress * (1f - progress)).coerceIn(0f, 1f)
                val fluidStretchFactor = liquidBellCurve * 0.22f

                val currentWidth = tabWidth * (1f + fluidStretchFactor)
                val currentHeight = 54.dp * (1f - (fluidStretchFactor * 0.15f))
                val xPos = (tabWidth * animatedTabOffset) - (tabWidth * fluidStretchFactor / 2f)
                val yPos = (54.dp - currentHeight) / 2f

                // Sliding Active Background Pill with iOS Liquid Glass styling
                Box(
                    modifier = Modifier
                        .offset(x = xPos, y = yPos)
                        .width(currentWidth)
                        .height(currentHeight)
                        .clip(RoundedCornerShape(26.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.88f)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(26.dp)
                        )
                )

                // Tab Item Buttons
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEach { tab ->
                        val isSelected = selectedTab == tab.index
                        val interactionSource = remember { MutableInteractionSource() }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .clip(RoundedCornerShape(26.dp))
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null
                                ) {
                                    onTabSelected(tab.index)
                                }
                                .testTag("nav_tab_${tab.index}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.label,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = tab.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

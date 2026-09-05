package com.axecon.uzbforce.model

enum class ThemeMode(val label: String, val description: String) {
    SYSTEM("System Default", "Follow phone light or dark mode"),
    LIGHT("Light Mode", "Clean bright look"),
    DARK("Dark Mode", "Easy on the eyes in dim lighting")
}

enum class AppColorProfile(
    val label: String,
    val description: String,
    val primaryHex: Long,
    val isDynamic: Boolean = false
) {
    OCEAN_BLUE("Ocean Blue", "Classic Axecon Azure Blue", 0xFF2563EB, false),
    EMERALD_GREEN("Emerald Green", "Clean mint & cyber emerald", 0xFF059669, false),
    NEON_PINK("Neon Pink", "Vibrant punchy magenta & rose", 0xFFDB2777, false),
    CYBER_PURPLE("Cyber Purple", "Electric neon violet & indigo", 0xFF7C3AED, false),
    SUNSET_AMBER("Sunset Amber", "Warm golden amber & flame orange", 0xFFD97706, false),
    SYSTEM_DYNAMIC("Dynamic M3 (Material You)", "Matches phone wallpaper (Android 12+ / M3E)", 0xFF3B82F6, true)
}

enum class NavigationBarStyle(val label: String, val description: String) {
    PILL_IOS("Floating Pill (iOS Style)", "Smooth floating bar with draggable elastic highlighter"),
    STANDARD_M3("Classic Bottom Bar", "Standard full-width navigation bar")
}

enum class SensorMode(val label: String, val description: String) {
    GYROSCOPE("Gyroscope (Air Pointer)", "Move pointer by turning your wrist"),
    ACCELEROMETER_TILT("Tilt Control (Joystick)", "Tilt the phone to glide the pointer"),
    FUSION("Hybrid (Recommended)", "Smooth gyro pointer with steady tilt stabilization")
}

enum class TrackingParadigm(val label: String, val shortDesc: String, val description: String) {
    PHYSICAL_WORLD_ANCHOR(
        "Tilt Motion",
        "Tilt phone to move pointer across screen",
        "The pointer moves smoothly as you rotate and tilt your device."
    )
}

enum class CursorStyle(val label: String, val description: String) {
    CLASSIC_ARROW("White Arrow", "Standard clear pointer arrow with black outline"),
    PRECISION_CROSSHAIR("Crosshair", "Fine target reticle for accurate pointing"),
    DOT_POINTER("Simple Dot", "Clean solid dot"),
    TARGET_RING("Target Ring", "Circular targeting ring"),
    TEARDROP("Teardrop", "Smooth droplet pointer"),
    STYLUS_PEN("Stylus", "Touch pen pointer"),
    CUSTOM_IMAGE("Custom Icon", "Choose your own image or preset icon")
}

enum class FeedbackSoundType(val label: String) {
    SYSTEM_CLICK("System Click"),
    POP("Soft Pop"),
    BEEP("Electronic Beep"),
    TICK("Mechanical Tick"),
    CHIME("Chime"),
    NONE("Mute / Off")
}

enum class HapticIntensity(val label: String, val strengthMillis: Long, val amplitude: Int) {
    LIGHT("Light Tap", 18L, 90),
    MEDIUM("Firm Click", 32L, 180),
    STRONG("Heavy Pulse", 55L, 255)
}

enum class ButtonTrigger(val label: String, val shortcutDesc: String) {
    VOL_UP_SINGLE("Volume Up: Single Click", "Click Volume Up once"),
    VOL_UP_DOUBLE("Volume Up: Double Click", "Quickly click Volume Up twice"),
    VOL_UP_LONG("Volume Up: Hold Down", "Press and hold Volume Up"),
    VOL_UP_DOUBLE_HOLD("Volume Up: Double-Click & Hold", "Double-click and hold down"),

    VOL_DOWN_SINGLE("Volume Down: Single Click", "Click Volume Down once"),
    VOL_DOWN_DOUBLE("Volume Down: Double Click", "Quickly click Volume Down twice"),
    VOL_DOWN_LONG("Volume Down: Hold Down", "Press and hold Volume Down"),
    VOL_DOWN_DOUBLE_HOLD("Volume Down: Double-Click & Hold", "Double-click and hold down"),

    BOTH_VOL_PRESS("Press Both Volume Buttons", "Press Volume Up and Down at the same time"),
    BOTH_VOL_LONG_HOLD("Hold Both Volume Buttons", "Hold Volume Up and Down together for 0.5s"),

    HEADSET_HOOK_SINGLE("Headset Button: Click", "Click wired or Bluetooth headset button"),
    HEADSET_HOOK_DOUBLE("Headset Button: Double Click", "Double-click headset button"),
    HEADSET_HOOK_LONG("Headset Button: Hold", "Hold headset button")
}

enum class CursorAction(val label: String, val category: String, val description: String) {
    CLICK("Tap / Click", "Cursor", "Tap at current pointer position"),
    LONG_CLICK("Long Press", "Cursor", "Hold down for 0.6s at pointer position"),
    DOUBLE_CLICK("Double Tap", "Cursor", "Double-tap at pointer position"),
    HOLD_TO_DRAG("Hold-to-Drag & Drop", "Cursor", "Hold button down to grab item, move pointer, release to drop"),
    DRAG_TOGGLE("Toggle Drag & Drop", "Cursor", "Tap once to pick up, move pointer, tap again to drop"),
    SWIPE_TOGGLE("Flick / Swipe", "Cursor", "Fast swipe from start point to destination without holding"),
    
    SCROLL_UP("Scroll Up", "Scrolling", "Scroll screen upward by one step"),
    SCROLL_DOWN("Scroll Down", "Scrolling", "Scroll screen downward by one step"),
    SCROLL_LEFT("Scroll Left", "Scrolling", "Scroll horizontally to the left"),
    SCROLL_RIGHT("Scroll Right", "Scrolling", "Scroll horizontally to the right"),
    CONTINUOUS_SCROLL_UP("Continuous Scroll Up", "Scrolling", "Keep scrolling up while button is held"),
    CONTINUOUS_SCROLL_DOWN("Continuous Scroll Down", "Scrolling", "Keep scrolling down while button is held"),
    
    OPEN_ACTION_MENU("Open Quick Menu", "Shortcuts", "Show floating menu with quick tools and shortcuts"),
    RECENTER_CALIBRATE("Recenter Pointer", "Cursor", "Move pointer to center and reset neutral angle"),
    TOGGLE_MASTER_SERVICE("Turn On / Off", "App Control", "Turn the entire pointer service on or off"),
    TOGGLE_PRECISION_MODE("Sniper Mode (0.25x Speed)", "Cursor", "Slow pointer down for small buttons & tiny text"),
    TOGGLE_TRACKING("Freeze / Unfreeze Pointer", "Cursor", "Lock pointer in place or resume tracking"),
    TOGGLE_TRACKING_PARADIGM("Reset Tilt Orientation", "Cursor", "Reset tilt tracking alignment"),
    
    VOLUME_UP("Volume Up", "Device", "Increase media / ring volume"),
    VOLUME_DOWN("Volume Down", "Device", "Decrease media / ring volume"),
    ANSWER_OR_END_CALL("Answer / Hang Up Call", "Phone", "Answer incoming phone call or hang up"),
    
    GLOBAL_BACK("Back", "Navigation", "Go back to previous screen"),
    GLOBAL_HOME("Home", "Navigation", "Go to phone home screen"),
    GLOBAL_RECENTS("Recent Apps", "Navigation", "Open app switcher"),
    GLOBAL_NOTIFICATIONS("Notifications", "Navigation", "Open notification shade"),
    GLOBAL_QUICK_SETTINGS("Quick Settings", "Navigation", "Open quick settings panel"),
    GLOBAL_LOCK_SCREEN("Lock Phone", "Device", "Turn screen off and lock"),
    GLOBAL_SCREENSHOT("Take Screenshot", "Device", "Capture phone screen"),
    GLOBAL_POWER_DIALOG("Power Menu", "Device", "Open restart and shutdown menu"),
    TOGGLE_FLASHLIGHT("Flashlight", "Device", "Turn phone flashlight on or off"),
    
    OPEN_PHONE_APP("Open Phone Dialer", "Apps", "Open phone call dialer"),
    OPEN_BROWSER_APP("Open Browser", "Apps", "Open web browser"),
    OPEN_SETTINGS_APP("Open Phone Settings", "Apps", "Open Android device settings"),
    OPEN_MESSAGES_APP("Open Messages", "Apps", "Open SMS messages"),
    OPEN_ACCESSIBILITY_SETTINGS("Open Accessibility", "Apps", "Open accessibility settings"),
    LAUNCH_CUSTOM_APP("Open Custom App", "Apps", "Open selected favorite app"),

    VOICE_TYPING("Voice Typing", "Typing", "Open voice dictation to speak text"),
    SHOW_KEYBOARD_HELPER("D-Pad Navigation Bar", "Typing", "Show keyboard arrows and text navigation bar"),
    TOGGLE_TORCH("Flashlight", "Device", "Turn flashlight on or off"),
    NONE("Do Nothing", "General", "Disable this button action")
}

enum class KeyStep(val label: String, val shortLabel: String) {
    VOLUME_UP("Volume Up", "Vol +"),
    VOLUME_DOWN("Volume Down", "Vol -"),
    HEADSET_HOOK("Headset Button", "Headset"),
    POWER("Power Button", "Power"),
    BACK("Back Button", "Back");

    val displayName: String get() = label
}

data class CustomKeySequence(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val steps: List<KeyStep>,
    val action: CursorAction,
    val customPackage: String? = null,
    val isEnabled: Boolean = true
) {
    val targetAction: CursorAction get() = action
}

data class CustomAppShortcut(
    val id: String = java.util.UUID.randomUUID().toString(),
    val label: String,
    val packageName: String,
    val iconPreset: String = "DEFAULT",
    val colorArgb: Long = 0xFF2563EB
)

data class SideButtonIndicator(
    val id: Int, // 0 = Volume Up, 1 = Volume Down, 2 = Power / Extra
    val label: String,
    val isEnabled: Boolean = true,
    val isOnRightSide: Boolean = true,
    val verticalOffsetRatio: Float = 0.35f,
    val lengthDp: Int = 40,
    val thicknessDp: Int = 5,
    val activeColorArgb: Long = 0xFF3B82F6,
    val inactiveColorArgb: Long = 0x3394A3B8
)

data class CursorConfig(
    val isEnabled: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val appColorProfile: AppColorProfile = AppColorProfile.OCEAN_BLUE,
    val navBarStyle: NavigationBarStyle = NavigationBarStyle.PILL_IOS,
    val trackingParadigm: TrackingParadigm = TrackingParadigm.PHYSICAL_WORLD_ANCHOR,
    val worldAnchorSensitivity: Float = 1.0f,
    val sensorMode: SensorMode = SensorMode.FUSION,
    val sensitivityX: Float = 3.8f,
    val sensitivityY: Float = 3.8f,
    val deadzone: Float = 0.04f,
    val smoothingFactor: Float = 0.35f,
    val accelerationEnabled: Boolean = true,
    val accelerationFactor: Float = 1.6f,
    val invertX: Boolean = false,
    val invertY: Boolean = false,
    val cursorSizeDp: Int = 28,
    val cursorColorArgb: Long = 0xFFFFFFFF,
    val cursorOutlineColorArgb: Long = 0xFF1E293B,
    val cursorOutlineWidth: Float = 2.0f,
    val cursorOutlineEnabled: Boolean = true,
    val cursorStyle: CursorStyle = CursorStyle.DOT_POINTER,
    val customImageUri: String? = null,
    val customImagePreset: String = "tech",
    val showRippleOnClick: Boolean = true,
    val showCoordinateHUD: Boolean = false,
    val precisionSpeedMultiplier: Float = 0.25f,
    val isPrecisionActive: Boolean = false,
    val isTrackingPaused: Boolean = false,
    val dwellAutoClickEnabled: Boolean = false,
    val dwellTimeMs: Long = 1200L,
    val clickOffsetX: Float = 0f,
    val clickOffsetY: Float = 0f,
    
    // Vibration Feedback
    val hapticFeedback: Boolean = true,
    val hapticIntensity: HapticIntensity = HapticIntensity.MEDIUM,
    val hapticOnClick: Boolean = true,
    val hapticOnLongClick: Boolean = true,
    val hapticOnScroll: Boolean = true,
    val hapticOnActionMenu: Boolean = true,
    val hapticOnRecenter: Boolean = true,

    // Audio Feedback
    val audioFeedback: Boolean = true,
    val soundType: FeedbackSoundType = FeedbackSoundType.SYSTEM_CLICK,
    val audioOnClick: Boolean = true,
    val audioOnLongClick: Boolean = true,
    val audioOnScroll: Boolean = false,
    val audioOnActionMenu: Boolean = true,
    val audioOnRecenter: Boolean = true,

    // Screen Edge Button Guides
    val sideIndicatorsEnabled: Boolean = true,
    val individualIndicators: List<SideButtonIndicator> = listOf(
        SideButtonIndicator(id = 0, label = "Volume Up", isEnabled = true, isOnRightSide = true, verticalOffsetRatio = 0.32f, lengthDp = 42, thicknessDp = 5, activeColorArgb = 0xFF3B82F6, inactiveColorArgb = 0x3394A3B8),
        SideButtonIndicator(id = 1, label = "Volume Down", isEnabled = true, isOnRightSide = true, verticalOffsetRatio = 0.44f, lengthDp = 42, thicknessDp = 5, activeColorArgb = 0xFF3B82F6, inactiveColorArgb = 0x3394A3B8),
        SideButtonIndicator(id = 2, label = "Power / Extra", isEnabled = true, isOnRightSide = true, verticalOffsetRatio = 0.58f, lengthDp = 34, thicknessDp = 5, activeColorArgb = 0xFF3B82F6, inactiveColorArgb = 0x3394A3B8)
    ),
    val sideIndicatorsOnRight: Boolean = true,
    val sideIndicatorPositionOffset: Float = 0.45f,
    val sideIndicatorWidthDp: Int = 5,
    val sideIndicatorHeightDp: Int = 38,
    val sideIndicatorSpacingDp: Int = 8,
    val sideIndicatorActiveColorArgb: Long = 0xFF3B82F6,
    val sideIndicatorInactiveColorArgb: Long = 0x3394A3B8,

    // Key Combination Sequences
    val comboTimeoutMs: Long = 1000L,
    val showSequenceVisualizer: Boolean = true,
    val lastPressedHighlightColorArgb: Long = 0xFFD97706,
    val sequenceMatchedColorArgb: Long = 0xFF059669,

    // Action Menu Customization
    val actionButtonEnabled: Boolean = true,
    val floatingActionButtonEnabled: Boolean = false,
    val enabledActionMenuNavItems: List<String> = listOf(
        "GLOBAL_BACK", "GLOBAL_HOME", "GLOBAL_RECENTS", "GLOBAL_NOTIFICATIONS", "GLOBAL_QUICK_SETTINGS", "GLOBAL_LOCK_SCREEN", "GLOBAL_SCREENSHOT", "GLOBAL_POWER_DIALOG"
    ),
    val enabledActionMenuToolItems: List<String> = listOf(
        "RECENTER_CALIBRATE", "TOGGLE_PRECISION_MODE", "TOGGLE_TRACKING", "DRAG_TOGGLE", "TOGGLE_FLASHLIGHT"
    ),

    val samplingRateHz: Int = 60,
    val autoRecenterOnScreenOn: Boolean = true,
    val doubleClickSpeedMs: Long = 280L,
    val longPressDurationMs: Long = 420L,
    val continuousScrollSpeedMs: Long = 160L,
    val scrollStepDistancePx: Int = 450
)

enum class PresetProfile(val profileName: String, val description: String) {
    ONE_HANDED(
        "One-Handed",
        "Easy single-hand holding: tap to click, double-click & hold to scroll"
    ),
    DESK_TABLE(
        "Desk & Table",
        "Comfortable sensitivity when phone is resting flat or in a stand"
    ),
    HIGH_PRECISION_SNIPER(
        "High Precision",
        "Extra smooth and gentle movement for tapping tiny links on websites"
    ),
    FAST_NAVIGATION(
        "Fast Navigation",
        "Quick responsive pointer for browsing articles and social feeds"
    ),
    TREMOR_REDUCTION(
        "Steady Hand Assist",
        "Filters out hand tremors and involuntary shakes"
    ),
    PHYSICAL_WORLD_STASIS(
        "Standard Tilt",
        "Balanced tilt sensitivity for everyday use"
    )
}

data class DragStateInfo(
    val isDragging: Boolean = false,
    val isHoldDrag: Boolean = false,
    val startX: Float = 0f,
    val startY: Float = 0f,
    val currentX: Float = 0f,
    val currentY: Float = 0f,
    val lastDroppedX: Float = 0f,
    val lastDroppedY: Float = 0f,
    val dropTimestamp: Long = 0L,
    val dropSuccessCount: Int = 0
)


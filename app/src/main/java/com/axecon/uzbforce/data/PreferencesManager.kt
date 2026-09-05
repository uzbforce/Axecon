package com.axecon.uzbforce.data

import android.content.Context
import android.content.SharedPreferences
import com.axecon.uzbforce.model.AppColorProfile
import com.axecon.uzbforce.model.ButtonTrigger
import com.axecon.uzbforce.model.CursorAction
import com.axecon.uzbforce.model.CursorConfig
import com.axecon.uzbforce.model.CursorStyle
import com.axecon.uzbforce.model.CustomAppShortcut
import com.axecon.uzbforce.model.CustomKeySequence
import com.axecon.uzbforce.model.FeedbackSoundType
import com.axecon.uzbforce.model.HapticIntensity
import com.axecon.uzbforce.model.KeyStep
import com.axecon.uzbforce.model.NavigationBarStyle
import com.axecon.uzbforce.model.PresetProfile
import com.axecon.uzbforce.model.SensorMode
import com.axecon.uzbforce.model.SideButtonIndicator
import com.axecon.uzbforce.model.ThemeMode
import com.axecon.uzbforce.model.TrackingParadigm
import com.axecon.uzbforce.service.AirCursorAccessibilityService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class PreferencesManager private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _configFlow = MutableStateFlow(loadConfig())
    val configFlow: StateFlow<CursorConfig> = _configFlow.asStateFlow()

    private val _buttonMappingsFlow = MutableStateFlow(loadButtonMappings())
    val buttonMappingsFlow: StateFlow<Map<ButtonTrigger, CursorAction>> = _buttonMappingsFlow.asStateFlow()

    private val _customSequencesFlow = MutableStateFlow(loadCustomSequences())
    val customSequencesFlow: StateFlow<List<CustomKeySequence>> = _customSequencesFlow.asStateFlow()

    private val _customShortcutsFlow = MutableStateFlow(loadCustomShortcuts())
    val customShortcutsFlow: StateFlow<List<CustomAppShortcut>> = _customShortcutsFlow.asStateFlow()

    fun getConfig(): CursorConfig = _configFlow.value

    fun getButtonMappings(): Map<ButtonTrigger, CursorAction> = _buttonMappingsFlow.value

    fun getCustomSequences(): List<CustomKeySequence> = _customSequencesFlow.value

    fun getCustomShortcuts(): List<CustomAppShortcut> = _customShortcutsFlow.value

    fun hasCompletedPermissionFlow(): Boolean = prefs.getBoolean(KEY_HAS_COMPLETED_PERMISSION_FLOW, false)

    fun setCompletedPermissionFlow(completed: Boolean) {
        prefs.edit().putBoolean(KEY_HAS_COMPLETED_PERMISSION_FLOW, completed).apply()
    }

    fun updateConfig(update: (CursorConfig) -> CursorConfig) {
        val newConfig = update(_configFlow.value)
        _configFlow.value = newConfig
        saveConfig(newConfig)
        AirCursorAccessibilityService.instance?.onConfigUpdated(newConfig)
    }

    fun resetAllToDefaults() {
        val defaultConfig = CursorConfig()
        _configFlow.value = defaultConfig
        saveConfig(defaultConfig)

        val defaultMappings = mutableMapOf<ButtonTrigger, CursorAction>()
        for (trigger in ButtonTrigger.entries) {
            defaultMappings[trigger] = defaultActionForTrigger(trigger)
        }
        _buttonMappingsFlow.value = defaultMappings
        saveButtonMappings(defaultMappings)

        val defaultSeqs = getDefaultSequences()
        _customSequencesFlow.value = defaultSeqs
        saveCustomSequences(defaultSeqs)

        val defaultScs = getDefaultShortcuts()
        _customShortcutsFlow.value = defaultScs
        saveCustomShortcuts(defaultScs)

        AirCursorAccessibilityService.instance?.onConfigUpdated(defaultConfig)
        AirCursorAccessibilityService.instance?.onSequencesUpdated(defaultSeqs)
    }

    fun updateButtonMapping(trigger: ButtonTrigger, action: CursorAction) {
        val current = _buttonMappingsFlow.value.toMutableMap()
        current[trigger] = action
        _buttonMappingsFlow.value = current
        saveButtonMappings(current)
    }

    fun setAllButtonMappings(mappings: Map<ButtonTrigger, CursorAction>) {
        _buttonMappingsFlow.value = mappings
        saveButtonMappings(mappings)
    }

    fun addCustomSequence(sequence: CustomKeySequence) {
        val current = _customSequencesFlow.value.toMutableList()
        current.add(sequence)
        _customSequencesFlow.value = current
        saveCustomSequences(current)
        AirCursorAccessibilityService.instance?.onSequencesUpdated(current)
    }

    fun removeCustomSequence(id: String) {
        val current = _customSequencesFlow.value.filter { it.id != id }
        _customSequencesFlow.value = current
        saveCustomSequences(current)
        AirCursorAccessibilityService.instance?.onSequencesUpdated(current)
    }

    fun deleteCustomSequence(id: String) = removeCustomSequence(id)

    fun toggleCustomSequence(id: String) {
        val current = _customSequencesFlow.value.map {
            if (it.id == id) it.copy(isEnabled = !it.isEnabled) else it
        }
        _customSequencesFlow.value = current
        saveCustomSequences(current)
        AirCursorAccessibilityService.instance?.onSequencesUpdated(current)
    }

    fun updateCustomSequence(sequence: CustomKeySequence) {
        val current = _customSequencesFlow.value.map {
            if (it.id == sequence.id) sequence else it
        }
        _customSequencesFlow.value = current
        saveCustomSequences(current)
        AirCursorAccessibilityService.instance?.onSequencesUpdated(current)
    }

    fun addCustomShortcut(shortcut: CustomAppShortcut) {
        val current = _customShortcutsFlow.value.toMutableList()
        current.add(shortcut)
        _customShortcutsFlow.value = current
        saveCustomShortcuts(current)
    }

    fun updateCustomShortcut(shortcut: CustomAppShortcut) {
        val current = _customShortcutsFlow.value.map {
            if (it.id == shortcut.id) shortcut else it
        }
        _customShortcutsFlow.value = current
        saveCustomShortcuts(current)
    }

    fun removeCustomShortcut(id: String) {
        val current = _customShortcutsFlow.value.filter { it.id != id }
        _customShortcutsFlow.value = current
        saveCustomShortcuts(current)
    }

    fun exportBackupJson(): String {
        return BackupRestoreManager(appContext).createBackupJsonString()
    }

    fun importBackupJson(json: String): Boolean {
        return BackupRestoreManager(appContext).restoreFromJsonString(json).isSuccess
    }

    fun applyPreset(preset: PresetProfile) {
        when (preset) {
            PresetProfile.ONE_HANDED -> {
                updateConfig {
                    it.copy(
                        sensorMode = SensorMode.FUSION,
                        sensitivityX = 3.8f,
                        sensitivityY = 3.8f,
                        deadzone = 0.04f,
                        smoothingFactor = 0.35f,
                        accelerationEnabled = true,
                        accelerationFactor = 1.6f,
                        cursorSizeDp = 28,
                        cursorColorArgb = 0xFFFFFFFF,
                        cursorStyle = CursorStyle.DOT_POINTER,
                        dwellAutoClickEnabled = false
                    )
                }
                setAllButtonMappings(
                    mapOf(
                        ButtonTrigger.VOL_UP_SINGLE to CursorAction.CLICK,
                        ButtonTrigger.VOL_UP_DOUBLE to CursorAction.DOUBLE_CLICK,
                        ButtonTrigger.VOL_UP_LONG to CursorAction.LONG_CLICK,
                        ButtonTrigger.VOL_UP_DOUBLE_HOLD to CursorAction.CONTINUOUS_SCROLL_UP,

                        ButtonTrigger.VOL_DOWN_SINGLE to CursorAction.CLICK,
                        ButtonTrigger.VOL_DOWN_DOUBLE to CursorAction.OPEN_ACTION_MENU,
                        ButtonTrigger.VOL_DOWN_LONG to CursorAction.RECENTER_CALIBRATE,
                        ButtonTrigger.VOL_DOWN_DOUBLE_HOLD to CursorAction.CONTINUOUS_SCROLL_DOWN,

                        ButtonTrigger.BOTH_VOL_PRESS to CursorAction.OPEN_ACTION_MENU,
                        ButtonTrigger.BOTH_VOL_LONG_HOLD to CursorAction.HOLD_TO_DRAG,
                        ButtonTrigger.HEADSET_HOOK_SINGLE to CursorAction.CLICK,
                        ButtonTrigger.HEADSET_HOOK_DOUBLE to CursorAction.RECENTER_CALIBRATE,
                        ButtonTrigger.HEADSET_HOOK_LONG to CursorAction.ANSWER_OR_END_CALL
                    )
                )
            }
            PresetProfile.DESK_TABLE -> {
                updateConfig {
                    it.copy(
                        sensorMode = SensorMode.ACCELEROMETER_TILT,
                        sensitivityX = 4.2f,
                        sensitivityY = 4.2f,
                        deadzone = 0.03f,
                        smoothingFactor = 0.40f,
                        accelerationEnabled = true,
                        accelerationFactor = 1.8f,
                        cursorSizeDp = 32,
                        cursorColorArgb = 0xFFFFFFFF,
                        cursorStyle = CursorStyle.DOT_POINTER,
                        dwellAutoClickEnabled = false
                    )
                }
            }
            PresetProfile.HIGH_PRECISION_SNIPER -> {
                updateConfig {
                    it.copy(
                        sensorMode = SensorMode.FUSION,
                        sensitivityX = 2.4f,
                        sensitivityY = 2.4f,
                        deadzone = 0.06f,
                        smoothingFactor = 0.55f,
                        accelerationEnabled = false,
                        cursorSizeDp = 24,
                        cursorColorArgb = 0xFFFFFFFF,
                        cursorStyle = CursorStyle.PRECISION_CROSSHAIR,
                        dwellAutoClickEnabled = false
                    )
                }
            }
            PresetProfile.FAST_NAVIGATION -> {
                updateConfig {
                    it.copy(
                        sensorMode = SensorMode.GYROSCOPE,
                        sensitivityX = 5.2f,
                        sensitivityY = 5.2f,
                        deadzone = 0.02f,
                        smoothingFactor = 0.20f,
                        accelerationEnabled = true,
                        accelerationFactor = 2.2f,
                        cursorSizeDp = 30,
                        cursorColorArgb = 0xFFFFFFFF,
                        cursorStyle = CursorStyle.CLASSIC_ARROW,
                        dwellAutoClickEnabled = false
                    )
                }
            }
            PresetProfile.TREMOR_REDUCTION -> {
                updateConfig {
                    it.copy(
                        sensorMode = SensorMode.FUSION,
                        sensitivityX = 2.8f,
                        sensitivityY = 2.8f,
                        deadzone = 0.12f,
                        smoothingFactor = 0.65f,
                        accelerationEnabled = false,
                        cursorSizeDp = 40,
                        cursorColorArgb = 0xFFFFFFFF,
                        cursorStyle = CursorStyle.TARGET_RING,
                        dwellAutoClickEnabled = true,
                        dwellTimeMs = 1500L
                    )
                }
            }
            PresetProfile.PHYSICAL_WORLD_STASIS -> {
                updateConfig {
                    it.copy(
                        sensorMode = SensorMode.FUSION,
                        sensitivityX = 3.6f,
                        sensitivityY = 3.6f,
                        deadzone = 0.035f,
                        smoothingFactor = 0.32f,
                        accelerationEnabled = false,
                        cursorSizeDp = 28,
                        cursorColorArgb = 0xFFFFFFFF,
                        cursorStyle = CursorStyle.DOT_POINTER,
                        dwellAutoClickEnabled = false
                    )
                }
            }
        }
    }

    private fun loadConfig(): CursorConfig {
        val rawNavActions = prefs.getString(KEY_ACTION_MENU_NAV_ITEMS, null)
        val navItems = if (rawNavActions != null) {
            try {
                val arr = JSONArray(rawNavActions)
                List(arr.length()) { arr.getString(it) }
            } catch (e: Exception) {
                getDefaultNavActions()
            }
        } else getDefaultNavActions()

        val rawToolActions = prefs.getString(KEY_ACTION_MENU_TOOL_ITEMS, null)
        val toolItems = if (rawToolActions != null) {
            try {
                val arr = JSONArray(rawToolActions)
                List(arr.length()) { arr.getString(it) }
            } catch (e: Exception) {
                getDefaultToolActions()
            }
        } else getDefaultToolActions()

        return CursorConfig(
            isEnabled = prefs.getBoolean(KEY_ENABLED, true),
            themeMode = ThemeMode.entries.getOrElse(prefs.getInt(KEY_THEME_MODE, ThemeMode.SYSTEM.ordinal)) { ThemeMode.SYSTEM },
            appColorProfile = AppColorProfile.entries.getOrElse(prefs.getInt(KEY_APP_COLOR_PROFILE, AppColorProfile.OCEAN_BLUE.ordinal)) { AppColorProfile.OCEAN_BLUE },
            navBarStyle = NavigationBarStyle.entries.getOrElse(prefs.getInt(KEY_NAV_BAR_STYLE, NavigationBarStyle.PILL_IOS.ordinal)) { NavigationBarStyle.PILL_IOS },
            trackingParadigm = TrackingParadigm.PHYSICAL_WORLD_ANCHOR,
            worldAnchorSensitivity = prefs.getFloat(KEY_WORLD_ANCHOR_SENS, 1.0f),
            sensorMode = SensorMode.entries.getOrElse(prefs.getInt(KEY_SENSOR_MODE, SensorMode.FUSION.ordinal)) { SensorMode.FUSION },
            sensitivityX = prefs.getFloat(KEY_SENS_X, 3.8f),
            sensitivityY = prefs.getFloat(KEY_SENS_Y, 3.8f),
            deadzone = prefs.getFloat(KEY_DEADZONE, 0.04f),
            smoothingFactor = prefs.getFloat(KEY_SMOOTHING, 0.35f),
            accelerationEnabled = prefs.getBoolean(KEY_ACCEL_ENABLED, true),
            accelerationFactor = prefs.getFloat(KEY_ACCEL_FACTOR, 1.6f),
            invertX = prefs.getBoolean(KEY_INVERT_X, false),
            invertY = prefs.getBoolean(KEY_INVERT_Y, false),
            cursorSizeDp = prefs.getInt(KEY_CURSOR_SIZE, 28),
            cursorColorArgb = prefs.getLong(KEY_CURSOR_COLOR, 0xFFFFFFFF),
            cursorOutlineColorArgb = prefs.getLong(KEY_CURSOR_OUTLINE_COLOR, 0xFF1E293B),
            cursorOutlineWidth = prefs.getFloat(KEY_CURSOR_OUTLINE_WIDTH, 2.0f),
            cursorOutlineEnabled = prefs.getBoolean(KEY_CURSOR_OUTLINE_ENABLED, true),
            cursorStyle = CursorStyle.entries.getOrElse(prefs.getInt(KEY_CURSOR_STYLE, CursorStyle.DOT_POINTER.ordinal)) { CursorStyle.DOT_POINTER },
            customImageUri = prefs.getString(KEY_CUSTOM_IMAGE_URI, null),
            customImagePreset = prefs.getString(KEY_CUSTOM_IMAGE_PRESET, "tech") ?: "tech",
            showRippleOnClick = prefs.getBoolean(KEY_RIPPLE, true),
            showCoordinateHUD = prefs.getBoolean(KEY_HUD, false),
            precisionSpeedMultiplier = prefs.getFloat(KEY_PRECISION_MULT, 0.25f),
            dwellAutoClickEnabled = prefs.getBoolean(KEY_DWELL_ENABLED, false),
            dwellTimeMs = prefs.getLong(KEY_DWELL_TIME, 1200L),
            clickOffsetX = prefs.getFloat(KEY_CLICK_OFFSET_X, 0f),
            clickOffsetY = prefs.getFloat(KEY_CLICK_OFFSET_Y, 0f),

            // Vibration Customization
            hapticFeedback = prefs.getBoolean(KEY_HAPTIC, true),
            hapticIntensity = HapticIntensity.entries.getOrElse(prefs.getInt(KEY_HAPTIC_INTENSITY, HapticIntensity.MEDIUM.ordinal)) { HapticIntensity.MEDIUM },
            hapticOnClick = prefs.getBoolean(KEY_HAPTIC_ON_CLICK, true),
            hapticOnLongClick = prefs.getBoolean(KEY_HAPTIC_ON_LONG_CLICK, true),
            hapticOnScroll = prefs.getBoolean(KEY_HAPTIC_ON_SCROLL, true),
            hapticOnActionMenu = prefs.getBoolean(KEY_HAPTIC_ON_ACTION_MENU, true),
            hapticOnRecenter = prefs.getBoolean(KEY_HAPTIC_ON_RECENTER, true),

            // Audio Customization
            audioFeedback = prefs.getBoolean(KEY_AUDIO, true),
            soundType = FeedbackSoundType.entries.getOrElse(prefs.getInt(KEY_SOUND_TYPE, FeedbackSoundType.SYSTEM_CLICK.ordinal)) { FeedbackSoundType.SYSTEM_CLICK },
            audioOnClick = prefs.getBoolean(KEY_AUDIO_ON_CLICK, true),
            audioOnLongClick = prefs.getBoolean(KEY_AUDIO_ON_LONG_CLICK, true),
            audioOnScroll = prefs.getBoolean(KEY_AUDIO_ON_SCROLL, false),
            audioOnActionMenu = prefs.getBoolean(KEY_AUDIO_ON_ACTION_MENU, true),
            audioOnRecenter = prefs.getBoolean(KEY_AUDIO_ON_RECENTER, true),

            // Side Indicator lines
            sideIndicatorsEnabled = prefs.getBoolean(KEY_SIDE_INDICATOR_ENABLED, true),
            individualIndicators = loadIndividualIndicators(),
            sideIndicatorsOnRight = prefs.getBoolean(KEY_SIDE_INDICATOR_RIGHT, true),
            sideIndicatorPositionOffset = prefs.getFloat(KEY_SIDE_INDICATOR_OFFSET_Y, 0.45f),
            sideIndicatorWidthDp = prefs.getInt(KEY_SIDE_INDICATOR_WIDTH, 5),
            sideIndicatorHeightDp = prefs.getInt(KEY_SIDE_INDICATOR_HEIGHT, 38),
            sideIndicatorSpacingDp = prefs.getInt(KEY_SIDE_INDICATOR_SPACING, 8),
            sideIndicatorActiveColorArgb = prefs.getLong(KEY_SIDE_INDICATOR_ACTIVE_COLOR, 0xFF3B82F6),
            sideIndicatorInactiveColorArgb = prefs.getLong(KEY_SIDE_INDICATOR_INACTIVE_COLOR, 0x3394A3B8),

            // Combo Sequence & Visualizer
            comboTimeoutMs = prefs.getLong(KEY_COMBO_TIMEOUT, 1000L),
            showSequenceVisualizer = prefs.getBoolean(KEY_SHOW_SEQUENCE_VISUALIZER, true),
            lastPressedHighlightColorArgb = prefs.getLong(KEY_LAST_PRESSED_COLOR, 0xFFD97706),
            sequenceMatchedColorArgb = prefs.getLong(KEY_SEQUENCE_MATCHED_COLOR, 0xFF059669),

            // Action Menu Customization
            actionButtonEnabled = prefs.getBoolean(KEY_ACTION_BUTTON_ENABLED, true),
            floatingActionButtonEnabled = prefs.getBoolean(KEY_FLOATING_ACTION_BTN, false),
            enabledActionMenuNavItems = navItems,
            enabledActionMenuToolItems = toolItems,

            samplingRateHz = prefs.getInt(KEY_SAMPLING_RATE, 60),
            autoRecenterOnScreenOn = prefs.getBoolean(KEY_AUTO_RECENTER, true),
            doubleClickSpeedMs = prefs.getLong(KEY_DBL_CLICK_SPEED, 280L),
            longPressDurationMs = prefs.getLong(KEY_LONG_PRESS_DUR, 420L),
            continuousScrollSpeedMs = prefs.getLong(KEY_CONTINUOUS_SCROLL_SPEED_MS, 160L),
            scrollStepDistancePx = prefs.getInt(KEY_SCROLL_STEP_DISTANCE_PX, 450)
        )
    }

    private fun getDefaultNavActions(): List<String> = listOf(
        "GLOBAL_BACK", "GLOBAL_HOME", "GLOBAL_RECENTS", "GLOBAL_NOTIFICATIONS", "GLOBAL_QUICK_SETTINGS", "GLOBAL_LOCK_SCREEN", "GLOBAL_SCREENSHOT", "GLOBAL_POWER_DIALOG"
    )

    private fun getDefaultToolActions(): List<String> = listOf(
        "RECENTER_CALIBRATE", "TOGGLE_PRECISION_MODE", "TOGGLE_TRACKING", "DRAG_TOGGLE", "TOGGLE_FLASHLIGHT"
    )

    private fun saveConfig(config: CursorConfig) {
        saveIndividualIndicators(config.individualIndicators)
        val navArr = JSONArray(config.enabledActionMenuNavItems)
        val toolArr = JSONArray(config.enabledActionMenuToolItems)

        prefs.edit()
            .putBoolean(KEY_ENABLED, config.isEnabled)
            .putInt(KEY_THEME_MODE, config.themeMode.ordinal)
            .putInt(KEY_APP_COLOR_PROFILE, config.appColorProfile.ordinal)
            .putInt(KEY_NAV_BAR_STYLE, config.navBarStyle.ordinal)
            .putInt(KEY_SENSOR_MODE, config.sensorMode.ordinal)
            .putFloat(KEY_SENS_X, config.sensitivityX)
            .putFloat(KEY_SENS_Y, config.sensitivityY)
            .putFloat(KEY_DEADZONE, config.deadzone)
            .putFloat(KEY_SMOOTHING, config.smoothingFactor)
            .putBoolean(KEY_ACCEL_ENABLED, config.accelerationEnabled)
            .putFloat(KEY_ACCEL_FACTOR, config.accelerationFactor)
            .putBoolean(KEY_INVERT_X, config.invertX)
            .putBoolean(KEY_INVERT_Y, config.invertY)
            .putInt(KEY_CURSOR_SIZE, config.cursorSizeDp)
            .putLong(KEY_CURSOR_COLOR, config.cursorColorArgb)
            .putLong(KEY_CURSOR_OUTLINE_COLOR, config.cursorOutlineColorArgb)
            .putFloat(KEY_CURSOR_OUTLINE_WIDTH, config.cursorOutlineWidth)
            .putBoolean(KEY_CURSOR_OUTLINE_ENABLED, config.cursorOutlineEnabled)
            .putInt(KEY_CURSOR_STYLE, config.cursorStyle.ordinal)
            .putString(KEY_CUSTOM_IMAGE_URI, config.customImageUri)
            .putString(KEY_CUSTOM_IMAGE_PRESET, config.customImagePreset)
            .putBoolean(KEY_RIPPLE, config.showRippleOnClick)
            .putBoolean(KEY_HUD, config.showCoordinateHUD)
            .putFloat(KEY_PRECISION_MULT, config.precisionSpeedMultiplier)
            .putBoolean(KEY_DWELL_ENABLED, config.dwellAutoClickEnabled)
            .putLong(KEY_DWELL_TIME, config.dwellTimeMs)
            .putFloat(KEY_CLICK_OFFSET_X, config.clickOffsetX)
            .putFloat(KEY_CLICK_OFFSET_Y, config.clickOffsetY)

            .putBoolean(KEY_HAPTIC, config.hapticFeedback)
            .putInt(KEY_HAPTIC_INTENSITY, config.hapticIntensity.ordinal)
            .putBoolean(KEY_HAPTIC_ON_CLICK, config.hapticOnClick)
            .putBoolean(KEY_HAPTIC_ON_LONG_CLICK, config.hapticOnLongClick)
            .putBoolean(KEY_HAPTIC_ON_SCROLL, config.hapticOnScroll)
            .putBoolean(KEY_HAPTIC_ON_ACTION_MENU, config.hapticOnActionMenu)
            .putBoolean(KEY_HAPTIC_ON_RECENTER, config.hapticOnRecenter)

            .putBoolean(KEY_AUDIO, config.audioFeedback)
            .putInt(KEY_SOUND_TYPE, config.soundType.ordinal)
            .putBoolean(KEY_AUDIO_ON_CLICK, config.audioOnClick)
            .putBoolean(KEY_AUDIO_ON_LONG_CLICK, config.audioOnLongClick)
            .putBoolean(KEY_AUDIO_ON_SCROLL, config.audioOnScroll)
            .putBoolean(KEY_AUDIO_ON_ACTION_MENU, config.audioOnActionMenu)
            .putBoolean(KEY_AUDIO_ON_RECENTER, config.audioOnRecenter)

            .putBoolean(KEY_SIDE_INDICATOR_ENABLED, config.sideIndicatorsEnabled)
            .putBoolean(KEY_SIDE_INDICATOR_RIGHT, config.sideIndicatorsOnRight)
            .putFloat(KEY_SIDE_INDICATOR_OFFSET_Y, config.sideIndicatorPositionOffset)
            .putInt(KEY_SIDE_INDICATOR_WIDTH, config.sideIndicatorWidthDp)
            .putInt(KEY_SIDE_INDICATOR_HEIGHT, config.sideIndicatorHeightDp)
            .putInt(KEY_SIDE_INDICATOR_SPACING, config.sideIndicatorSpacingDp)
            .putLong(KEY_SIDE_INDICATOR_ACTIVE_COLOR, config.sideIndicatorActiveColorArgb)
            .putLong(KEY_SIDE_INDICATOR_INACTIVE_COLOR, config.sideIndicatorInactiveColorArgb)

            .putLong(KEY_COMBO_TIMEOUT, config.comboTimeoutMs)
            .putBoolean(KEY_SHOW_SEQUENCE_VISUALIZER, config.showSequenceVisualizer)
            .putLong(KEY_LAST_PRESSED_COLOR, config.lastPressedHighlightColorArgb)
            .putLong(KEY_SEQUENCE_MATCHED_COLOR, config.sequenceMatchedColorArgb)

            .putBoolean(KEY_ACTION_BUTTON_ENABLED, config.actionButtonEnabled)
            .putBoolean(KEY_FLOATING_ACTION_BTN, config.floatingActionButtonEnabled)
            .putString(KEY_ACTION_MENU_NAV_ITEMS, navArr.toString())
            .putString(KEY_ACTION_MENU_TOOL_ITEMS, toolArr.toString())

            .putInt(KEY_TRACKING_PARADIGM, config.trackingParadigm.ordinal)
            .putFloat(KEY_WORLD_ANCHOR_SENS, config.worldAnchorSensitivity)

            .putInt(KEY_SAMPLING_RATE, config.samplingRateHz)
            .putBoolean(KEY_AUTO_RECENTER, config.autoRecenterOnScreenOn)
            .putLong(KEY_DBL_CLICK_SPEED, config.doubleClickSpeedMs)
            .putLong(KEY_LONG_PRESS_DUR, config.longPressDurationMs)
            .putLong(KEY_CONTINUOUS_SCROLL_SPEED_MS, config.continuousScrollSpeedMs)
            .putInt(KEY_SCROLL_STEP_DISTANCE_PX, config.scrollStepDistancePx)
            .apply()
    }

    fun updateIndividualIndicator(indicator: SideButtonIndicator) {
        val current = _configFlow.value
        val updatedList = current.individualIndicators.map {
            if (it.id == indicator.id) indicator else it
        }
        val updatedConfig = current.copy(individualIndicators = updatedList)
        _configFlow.value = updatedConfig
        saveConfig(updatedConfig)
        AirCursorAccessibilityService.instance?.onConfigUpdated(updatedConfig)
    }

    private fun loadIndividualIndicators(): List<SideButtonIndicator> {
        val jsonString = prefs.getString(KEY_INDIVIDUAL_INDICATORS, null) ?: return getDefaultIndicators()
        return try {
            val array = JSONArray(jsonString)
            val list = mutableListOf<SideButtonIndicator>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.optInt("id", i)
                val label = obj.optString("label", if (i == 0) "Volume Up" else if (i == 1) "Volume Down" else "Power / Extra")
                val isEnabled = obj.optBoolean("enabled", true)
                val isOnRight = obj.optBoolean("onRight", true)
                val offset = obj.optDouble("offset", if (i == 0) 0.32 else if (i == 1) 0.44 else 0.58).toFloat()
                val length = obj.optInt("length", if (i == 2) 34 else 42)
                val thickness = obj.optInt("thickness", 5)
                val activeColor = obj.optLong("activeColor", 0xFF3B82F6)
                val inactiveColor = obj.optLong("inactiveColor", 0x3394A3B8)
                list.add(SideButtonIndicator(id, label, isEnabled, isOnRight, offset, length, thickness, activeColor, inactiveColor))
            }
            if (list.isEmpty()) getDefaultIndicators() else list
        } catch (e: Exception) {
            e.printStackTrace()
            getDefaultIndicators()
        }
    }

    private fun saveIndividualIndicators(indicators: List<SideButtonIndicator>) {
        try {
            val array = JSONArray()
            for (ind in indicators) {
                val obj = JSONObject().apply {
                    put("id", ind.id)
                    put("label", ind.label)
                    put("enabled", ind.isEnabled)
                    put("onRight", ind.isOnRightSide)
                    put("offset", ind.verticalOffsetRatio.toDouble())
                    put("length", ind.lengthDp)
                    put("thickness", ind.thicknessDp)
                    put("activeColor", ind.activeColorArgb)
                    put("inactiveColor", ind.inactiveColorArgb)
                }
                array.put(obj)
            }
            prefs.edit().putString(KEY_INDIVIDUAL_INDICATORS, array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getDefaultIndicators(): List<SideButtonIndicator> {
        return listOf(
            SideButtonIndicator(id = 0, label = "Volume Up", isEnabled = true, isOnRightSide = true, verticalOffsetRatio = 0.32f, lengthDp = 42, thicknessDp = 5, activeColorArgb = 0xFF3B82F6, inactiveColorArgb = 0x3394A3B8),
            SideButtonIndicator(id = 1, label = "Volume Down", isEnabled = true, isOnRightSide = true, verticalOffsetRatio = 0.44f, lengthDp = 42, thicknessDp = 5, activeColorArgb = 0xFF3B82F6, inactiveColorArgb = 0x3394A3B8),
            SideButtonIndicator(id = 2, label = "Power / Extra", isEnabled = true, isOnRightSide = true, verticalOffsetRatio = 0.58f, lengthDp = 34, thicknessDp = 5, activeColorArgb = 0xFF3B82F6, inactiveColorArgb = 0x3394A3B8)
        )
    }

    private fun loadButtonMappings(): Map<ButtonTrigger, CursorAction> {
        val map = mutableMapOf<ButtonTrigger, CursorAction>()
        for (trigger in ButtonTrigger.entries) {
            val savedName = prefs.getString(KEY_TRIGGER_PREFIX + trigger.name, null)
            val action = if (savedName != null) {
                try {
                    CursorAction.valueOf(savedName)
                } catch (e: Exception) {
                    defaultActionForTrigger(trigger)
                }
            } else {
                defaultActionForTrigger(trigger)
            }
            map[trigger] = action
        }
        return map
    }

    private fun saveButtonMappings(map: Map<ButtonTrigger, CursorAction>) {
        val editor = prefs.edit()
        for ((trigger, action) in map) {
            editor.putString(KEY_TRIGGER_PREFIX + trigger.name, action.name)
        }
        editor.apply()
    }

    private fun loadCustomSequences(): List<CustomKeySequence> {
        val jsonString = prefs.getString(KEY_CUSTOM_SEQUENCES, null) ?: return getDefaultSequences()
        return try {
            val array = JSONArray(jsonString)
            val list = mutableListOf<CustomKeySequence>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.optString("id", java.util.UUID.randomUUID().toString())
                val name = obj.optString("name", "Custom Combination $i")
                val actionName = obj.optString("action", CursorAction.GLOBAL_HOME.name)
                val action = try { CursorAction.valueOf(actionName) } catch (e: Exception) { CursorAction.GLOBAL_HOME }
                val customPkg = obj.optString("pkg", null)
                val isEnabled = obj.optBoolean("enabled", true)
                val stepsArray = obj.getJSONArray("steps")
                val steps = mutableListOf<KeyStep>()
                for (j in 0 until stepsArray.length()) {
                    val stepName = stepsArray.getString(j)
                    try {
                        steps.add(KeyStep.valueOf(stepName))
                    } catch (e: Exception) {
                        steps.add(KeyStep.VOLUME_UP)
                    }
                }
                list.add(CustomKeySequence(id, name, steps, action, customPkg, isEnabled))
            }
            if (list.isEmpty()) getDefaultSequences() else list
        } catch (e: Exception) {
            e.printStackTrace()
            getDefaultSequences()
        }
    }

    fun saveCustomSequences(sequences: List<CustomKeySequence>) {
        _customSequencesFlow.value = sequences
        try {
            val array = JSONArray()
            for (seq in sequences) {
                val obj = JSONObject().apply {
                    put("id", seq.id)
                    put("name", seq.name)
                    put("action", seq.targetAction.name)
                    put("pkg", seq.customPackage)
                    put("enabled", seq.isEnabled)
                    val stepsArr = JSONArray()
                    seq.steps.forEach { stepsArr.put(it.name) }
                    put("steps", stepsArr)
                }
                array.put(obj)
            }
            prefs.edit().putString(KEY_CUSTOM_SEQUENCES, array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getDefaultSequences(): List<CustomKeySequence> {
        return listOf(
            CustomKeySequence(
                name = "Recenter Pointer (Up ➔ Down ➔ Up)",
                steps = listOf(KeyStep.VOLUME_UP, KeyStep.VOLUME_DOWN, KeyStep.VOLUME_UP),
                action = CursorAction.RECENTER_CALIBRATE
            ),
            CustomKeySequence(
                name = "Lock Screen (Down ➔ Down ➔ Up)",
                steps = listOf(KeyStep.VOLUME_DOWN, KeyStep.VOLUME_DOWN, KeyStep.VOLUME_UP),
                action = CursorAction.GLOBAL_LOCK_SCREEN
            ),
            CustomKeySequence(
                name = "Flashlight (Up ➔ Up ➔ Down ➔ Down)",
                steps = listOf(KeyStep.VOLUME_UP, KeyStep.VOLUME_UP, KeyStep.VOLUME_DOWN, KeyStep.VOLUME_DOWN),
                action = CursorAction.TOGGLE_FLASHLIGHT
            )
        )
    }

    private fun loadCustomShortcuts(): List<CustomAppShortcut> {
        val jsonString = prefs.getString(KEY_CUSTOM_SHORTCUTS, null) ?: return getDefaultShortcuts()
        return try {
            val array = JSONArray(jsonString)
            val list = mutableListOf<CustomAppShortcut>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.optString("id", java.util.UUID.randomUUID().toString())
                val label = obj.optString("label", "App $i")
                val pkg = obj.optString("pkg", "")
                val icon = obj.optString("icon", "DEFAULT")
                val color = obj.optLong("color", 0xFF2563EB)
                list.add(CustomAppShortcut(id, label, pkg, icon, color))
            }
            if (list.isEmpty()) getDefaultShortcuts() else list
        } catch (e: Exception) {
            e.printStackTrace()
            getDefaultShortcuts()
        }
    }

    fun saveCustomShortcuts(shortcuts: List<CustomAppShortcut>) {
        _customShortcutsFlow.value = shortcuts
        try {
            val array = JSONArray()
            for (sc in shortcuts) {
                val obj = JSONObject().apply {
                    put("id", sc.id)
                    put("label", sc.label)
                    put("pkg", sc.packageName)
                    put("icon", sc.iconPreset)
                    put("color", sc.colorArgb)
                }
                array.put(obj)
            }
            prefs.edit().putString(KEY_CUSTOM_SHORTCUTS, array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getDefaultShortcuts(): List<CustomAppShortcut> {
        return listOf(
            CustomAppShortcut(
                label = "Phone",
                packageName = "com.google.android.dialer",
                iconPreset = "PHONE",
                colorArgb = 0xFF10B981
            ),
            CustomAppShortcut(
                label = "Messages",
                packageName = "com.google.android.apps.messaging",
                iconPreset = "MESSAGES",
                colorArgb = 0xFF2563EB
            ),
            CustomAppShortcut(
                label = "Browser",
                packageName = "com.android.chrome",
                iconPreset = "BROWSER",
                colorArgb = 0xFFD97706
            ),
            CustomAppShortcut(
                label = "Settings",
                packageName = "com.android.settings",
                iconPreset = "SETTINGS",
                colorArgb = 0xFF64748B
            )
        )
    }

    private fun defaultActionForTrigger(trigger: ButtonTrigger): CursorAction {
        return when (trigger) {
            ButtonTrigger.VOL_UP_SINGLE -> CursorAction.CLICK
            ButtonTrigger.VOL_UP_DOUBLE -> CursorAction.DOUBLE_CLICK
            ButtonTrigger.VOL_UP_LONG -> CursorAction.LONG_CLICK
            ButtonTrigger.VOL_UP_DOUBLE_HOLD -> CursorAction.CONTINUOUS_SCROLL_UP

            ButtonTrigger.VOL_DOWN_SINGLE -> CursorAction.CLICK
            ButtonTrigger.VOL_DOWN_DOUBLE -> CursorAction.OPEN_ACTION_MENU
            ButtonTrigger.VOL_DOWN_LONG -> CursorAction.RECENTER_CALIBRATE
            ButtonTrigger.VOL_DOWN_DOUBLE_HOLD -> CursorAction.CONTINUOUS_SCROLL_DOWN

            ButtonTrigger.BOTH_VOL_PRESS -> CursorAction.OPEN_ACTION_MENU
            ButtonTrigger.BOTH_VOL_LONG_HOLD -> CursorAction.HOLD_TO_DRAG

            ButtonTrigger.HEADSET_HOOK_SINGLE -> CursorAction.CLICK
            ButtonTrigger.HEADSET_HOOK_DOUBLE -> CursorAction.RECENTER_CALIBRATE
            ButtonTrigger.HEADSET_HOOK_LONG -> CursorAction.ANSWER_OR_END_CALL
        }
    }

    companion object {
        private const val PREFS_NAME = "axecon_preferences"

        @Volatile
        private var INSTANCE: PreferencesManager? = null

        fun getInstance(context: Context): PreferencesManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PreferencesManager(context.applicationContext).also { INSTANCE = it }
            }
        }

        private const val KEY_ENABLED = "key_enabled"
        private const val KEY_THEME_MODE = "key_theme_mode"
        private const val KEY_APP_COLOR_PROFILE = "key_app_color_profile"
        private const val KEY_NAV_BAR_STYLE = "key_nav_bar_style"
        private const val KEY_SENSOR_MODE = "key_sensor_mode"
        private const val KEY_SENS_X = "key_sens_x"
        private const val KEY_SENS_Y = "key_sens_y"
        private const val KEY_DEADZONE = "key_deadzone"
        private const val KEY_SMOOTHING = "key_smoothing"
        private const val KEY_ACCEL_ENABLED = "key_accel_enabled"
        private const val KEY_ACCEL_FACTOR = "key_accel_factor"
        private const val KEY_INVERT_X = "key_invert_x"
        private const val KEY_INVERT_Y = "key_invert_y"
        private const val KEY_CURSOR_SIZE = "key_cursor_size"
        private const val KEY_CURSOR_COLOR = "key_cursor_color"
        private const val KEY_CURSOR_OUTLINE_COLOR = "key_cursor_outline_color"
        private const val KEY_CURSOR_OUTLINE_WIDTH = "key_cursor_outline_width"
        private const val KEY_CURSOR_OUTLINE_ENABLED = "key_cursor_outline_enabled"
        private const val KEY_CURSOR_STYLE = "key_cursor_style"
        private const val KEY_CUSTOM_IMAGE_URI = "key_custom_image_uri"
        private const val KEY_CUSTOM_IMAGE_PRESET = "key_custom_image_preset"
        private const val KEY_RIPPLE = "key_ripple"
        private const val KEY_HUD = "key_hud"
        private const val KEY_PRECISION_MULT = "key_precision_mult"
        private const val KEY_DWELL_ENABLED = "key_dwell_enabled"
        private const val KEY_DWELL_TIME = "key_dwell_time"
        private const val KEY_CLICK_OFFSET_X = "key_click_offset_x"
        private const val KEY_CLICK_OFFSET_Y = "key_click_offset_y"

        private const val KEY_HAPTIC = "key_haptic"
        private const val KEY_HAPTIC_INTENSITY = "key_haptic_intensity"
        private const val KEY_HAPTIC_ON_CLICK = "key_haptic_on_click"
        private const val KEY_HAPTIC_ON_LONG_CLICK = "key_haptic_on_long_click"
        private const val KEY_HAPTIC_ON_SCROLL = "key_haptic_on_scroll"
        private const val KEY_HAPTIC_ON_ACTION_MENU = "key_haptic_on_action_menu"
        private const val KEY_HAPTIC_ON_RECENTER = "key_haptic_on_recenter"

        private const val KEY_AUDIO = "key_audio"
        private const val KEY_SOUND_TYPE = "key_sound_type"
        private const val KEY_AUDIO_ON_CLICK = "key_audio_on_click"
        private const val KEY_AUDIO_ON_LONG_CLICK = "key_audio_on_long_click"
        private const val KEY_AUDIO_ON_SCROLL = "key_audio_on_scroll"
        private const val KEY_AUDIO_ON_ACTION_MENU = "key_audio_on_action_menu"
        private const val KEY_AUDIO_ON_RECENTER = "key_audio_on_recenter"

        private const val KEY_SIDE_INDICATOR_ENABLED = "key_side_indicator_enabled"
        private const val KEY_SIDE_INDICATOR_RIGHT = "key_side_indicator_right"
        private const val KEY_SIDE_INDICATOR_OFFSET_Y = "key_side_indicator_offset_y"
        private const val KEY_SIDE_INDICATOR_WIDTH = "key_side_indicator_width"
        private const val KEY_SIDE_INDICATOR_HEIGHT = "key_side_indicator_height"
        private const val KEY_SIDE_INDICATOR_SPACING = "key_side_indicator_spacing"
        private const val KEY_SIDE_INDICATOR_ACTIVE_COLOR = "key_side_indicator_active_color"
        private const val KEY_SIDE_INDICATOR_INACTIVE_COLOR = "key_side_indicator_inactive_color"
        private const val KEY_INDIVIDUAL_INDICATORS = "key_individual_indicators"
        private const val KEY_COMBO_TIMEOUT = "key_combo_timeout"
        private const val KEY_SHOW_SEQUENCE_VISUALIZER = "key_show_sequence_visualizer"
        private const val KEY_LAST_PRESSED_COLOR = "key_last_pressed_color"
        private const val KEY_SEQUENCE_MATCHED_COLOR = "key_sequence_matched_color"

        private const val KEY_ACTION_BUTTON_ENABLED = "key_action_button_enabled"
        private const val KEY_FLOATING_ACTION_BTN = "key_floating_action_btn"
        private const val KEY_ACTION_MENU_NAV_ITEMS = "key_action_menu_nav_items"
        private const val KEY_ACTION_MENU_TOOL_ITEMS = "key_action_menu_tool_items"

        private const val KEY_TRACKING_PARADIGM = "key_tracking_paradigm"
        private const val KEY_WORLD_ANCHOR_SENS = "key_world_anchor_sens"

        private const val KEY_SAMPLING_RATE = "key_sampling_rate"
        private const val KEY_AUTO_RECENTER = "key_auto_recenter"
        private const val KEY_DBL_CLICK_SPEED = "key_dbl_click_speed"
        private const val KEY_LONG_PRESS_DUR = "key_long_press_dur"
        private const val KEY_CONTINUOUS_SCROLL_SPEED_MS = "key_continuous_scroll_speed_ms"
        private const val KEY_SCROLL_STEP_DISTANCE_PX = "key_scroll_step_distance_px"

        private const val KEY_TRIGGER_PREFIX = "trigger_map_"
        private const val KEY_CUSTOM_SEQUENCES = "key_custom_sequences"
        private const val KEY_CUSTOM_SHORTCUTS = "key_custom_shortcuts"
        private const val KEY_HAS_COMPLETED_PERMISSION_FLOW = "key_has_completed_permission_flow"
    }
}

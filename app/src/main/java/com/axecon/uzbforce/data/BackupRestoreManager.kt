package com.axecon.uzbforce.data

import android.content.Context
import android.net.Uri
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
import com.axecon.uzbforce.model.SensorMode
import com.axecon.uzbforce.model.ThemeMode
import com.axecon.uzbforce.model.TrackingParadigm
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Manages backup and restore of all Axecon configuration profiles using .axcn files.
 */
class BackupRestoreManager(private val context: Context) {

    private val preferencesManager = PreferencesManager.getInstance(context)

    /**
     * Export all app configuration and button maps into a structured .axcn JSON string.
     */
    fun createBackupJsonString(): String {
        val config = preferencesManager.getConfig()
        val mappings = preferencesManager.buttonMappingsFlow.value
        val sequences = preferencesManager.customSequencesFlow.value
        val shortcuts = preferencesManager.customShortcutsFlow.value

        val root = JSONObject().apply {
            put("format", "AXCN_BACKUP")
            put("version", 3)
            put("timestamp", System.currentTimeMillis())
            put("date", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))
            put("appName", "Axecon")

            // 1. Config Object (Complete settings snapshot)
            val configObj = JSONObject().apply {
                put("isEnabled", config.isEnabled)
                put("sensorMode", config.sensorMode.name)
                put("trackingParadigm", config.trackingParadigm.name)
                put("worldAnchorSensitivity", config.worldAnchorSensitivity.toDouble())
                put("sensitivityX", config.sensitivityX.toDouble())
                put("sensitivityY", config.sensitivityY.toDouble())
                put("deadzone", config.deadzone.toDouble())
                put("smoothingFactor", config.smoothingFactor.toDouble())
                put("accelerationEnabled", config.accelerationEnabled)
                put("accelerationFactor", config.accelerationFactor.toDouble())
                put("cursorStyle", config.cursorStyle.name)
                put("cursorColorArgb", config.cursorColorArgb)
                put("cursorOutlineColorArgb", config.cursorOutlineColorArgb)
                put("cursorOutlineEnabled", config.cursorOutlineEnabled)
                put("cursorOutlineWidth", config.cursorOutlineWidth.toDouble())
                put("cursorSizeDp", config.cursorSizeDp)
                put("customImagePreset", config.customImagePreset)
                put("customImageUri", config.customImageUri ?: "")
                put("dwellAutoClickEnabled", config.dwellAutoClickEnabled)
                put("dwellTimeMs", config.dwellTimeMs)
                put("hapticFeedback", config.hapticFeedback)
                put("hapticIntensity", config.hapticIntensity.name)
                put("hapticOnClick", config.hapticOnClick)
                put("hapticOnLongClick", config.hapticOnLongClick)
                put("hapticOnScroll", config.hapticOnScroll)
                put("hapticOnActionMenu", config.hapticOnActionMenu)
                put("hapticOnRecenter", config.hapticOnRecenter)
                put("audioFeedback", config.audioFeedback)
                put("soundType", config.soundType.name)
                put("audioOnClick", config.audioOnClick)
                put("audioOnLongClick", config.audioOnLongClick)
                put("audioOnScroll", config.audioOnScroll)
                put("audioOnActionMenu", config.audioOnActionMenu)
                put("audioOnRecenter", config.audioOnRecenter)
                put("showCoordinateHUD", config.showCoordinateHUD)
                put("showRippleOnClick", config.showRippleOnClick)
                put("clickOffsetX", config.clickOffsetX.toDouble())
                put("clickOffsetY", config.clickOffsetY.toDouble())
                put("floatingActionButtonEnabled", config.floatingActionButtonEnabled)
                put("sideIndicatorsEnabled", config.sideIndicatorsEnabled)
                put("sideIndicatorsOnRight", config.sideIndicatorsOnRight)
                put("sideIndicatorHeightDp", config.sideIndicatorHeightDp)
                put("sideIndicatorWidthDp", config.sideIndicatorWidthDp)
                put("sideIndicatorPositionOffset", config.sideIndicatorPositionOffset.toDouble())
                put("sideIndicatorActiveColorArgb", config.sideIndicatorActiveColorArgb)
                put("sideIndicatorInactiveColorArgb", config.sideIndicatorInactiveColorArgb)
                put("showSequenceVisualizer", config.showSequenceVisualizer)
                put("sequenceMatchedColorArgb", config.sequenceMatchedColorArgb)
                put("lastPressedHighlightColorArgb", config.lastPressedHighlightColorArgb)
                put("comboTimeoutMs", config.comboTimeoutMs)
                put("precisionSpeedMultiplier", config.precisionSpeedMultiplier.toDouble())
                put("doubleClickSpeedMs", config.doubleClickSpeedMs)
                put("longPressDurationMs", config.longPressDurationMs)
                put("continuousScrollSpeedMs", config.continuousScrollSpeedMs)
                put("scrollStepDistancePx", config.scrollStepDistancePx)
                put("themeMode", config.themeMode.name)
                put("navBarStyle", config.navBarStyle.name)
                put("appColorProfile", config.appColorProfile.name)
            }
            put("cursorConfig", configObj)

            // 2. Button Mappings
            val mappingsObj = JSONObject()
            for (entry in mappings.entries) {
                mappingsObj.put(entry.key.name, entry.value.name)
            }
            put("buttonMappings", mappingsObj)

            // 3. Custom Key Sequences
            val seqArray = JSONArray()
            for (seq in sequences) {
                val sObj = JSONObject().apply {
                    put("id", seq.id)
                    put("name", seq.name)
                    put("action", seq.action.name)
                    put("isEnabled", seq.isEnabled)
                    val stepsArr = JSONArray()
                    for (step in seq.steps) {
                        stepsArr.put(step.name)
                    }
                    put("steps", stepsArr)
                }
                seqArray.put(sObj)
            }
            put("customSequences", seqArray)

            // 4. Custom App Shortcuts
            val shortArray = JSONArray()
            for (sc in shortcuts) {
                val scObj = JSONObject().apply {
                    put("id", sc.id)
                    put("label", sc.label)
                    put("packageName", sc.packageName)
                    put("iconPreset", sc.iconPreset)
                    put("colorArgb", sc.colorArgb)
                }
                shortArray.put(scObj)
            }
            put("customShortcuts", shortArray)
        }

        return root.toString(2)
    }

    /**
     * Write .axcn backup to a chosen Uri.
     */
    fun exportBackupToUri(uri: Uri): Result<Unit> {
        return try {
            val jsonString = createBackupJsonString()
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(jsonString)
                    writer.flush()
                }
            } ?: return Result.failure(Exception("Failed to open output stream for .axcn file"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Read and restore configuration from a chosen .axcn Uri.
     */
    fun importBackupFromUri(uri: Uri): Result<String> {
        return try {
            val contentBuilder = StringBuilder()
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String? = reader.readLine()
                    while (line != null) {
                        contentBuilder.append(line).append("\n")
                        line = reader.readLine()
                    }
                }
            } ?: return Result.failure(Exception("Failed to open input stream for .axcn file"))

            restoreFromJsonString(contentBuilder.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Parse and restore all settings from a backup JSON string.
     */
    fun restoreFromJsonString(jsonString: String): Result<String> {
        return try {
            val root = JSONObject(jsonString)
            val format = root.optString("format", "")
            val hasValidFormat = format.contains("AXCN", ignoreCase = true) ||
                    root.has("cursorConfig") ||
                    root.has("config") ||
                    root.has("buttonMappings")

            if (!hasValidFormat) {
                return Result.failure(IllegalArgumentException("Invalid file content. Expected an Axecon backup (.axcn or JSON)."))
            }

            var restoredCount = 0

            // 1. Restore CursorConfig (handles both "cursorConfig" and legacy "config" keys)
            val cfgKey = if (root.has("cursorConfig")) "cursorConfig" else if (root.has("config")) "config" else null
            if (cfgKey != null) {
                val cfgObj = root.getJSONObject(cfgKey)
                preferencesManager.updateConfig { current ->
                    current.copy(
                        isEnabled = cfgObj.optBoolean("isEnabled", current.isEnabled),
                        sensorMode = try { SensorMode.valueOf(cfgObj.optString("sensorMode", current.sensorMode.name)) } catch (_: Exception) { current.sensorMode },
                        trackingParadigm = try { TrackingParadigm.valueOf(cfgObj.optString("trackingParadigm", current.trackingParadigm.name)) } catch (_: Exception) { current.trackingParadigm },
                        worldAnchorSensitivity = cfgObj.optDouble("worldAnchorSensitivity", current.worldAnchorSensitivity.toDouble()).toFloat(),
                        sensitivityX = cfgObj.optDouble("sensitivityX", current.sensitivityX.toDouble()).toFloat(),
                        sensitivityY = cfgObj.optDouble("sensitivityY", current.sensitivityY.toDouble()).toFloat(),
                        deadzone = cfgObj.optDouble("deadzone", current.deadzone.toDouble()).toFloat(),
                        smoothingFactor = cfgObj.optDouble("smoothingFactor", current.smoothingFactor.toDouble()).toFloat(),
                        accelerationEnabled = cfgObj.optBoolean("accelerationEnabled", current.accelerationEnabled),
                        accelerationFactor = cfgObj.optDouble("accelerationFactor", current.accelerationFactor.toDouble()).toFloat(),
                        cursorStyle = try { CursorStyle.valueOf(cfgObj.optString("cursorStyle", current.cursorStyle.name)) } catch (_: Exception) { current.cursorStyle },
                        cursorColorArgb = cfgObj.optLong("cursorColorArgb", current.cursorColorArgb),
                        cursorOutlineColorArgb = cfgObj.optLong("cursorOutlineColorArgb", current.cursorOutlineColorArgb),
                        cursorOutlineEnabled = cfgObj.optBoolean("cursorOutlineEnabled", current.cursorOutlineEnabled),
                        cursorOutlineWidth = cfgObj.optDouble("cursorOutlineWidth", current.cursorOutlineWidth.toDouble()).toFloat(),
                        cursorSizeDp = cfgObj.optInt("cursorSizeDp", current.cursorSizeDp),
                        customImagePreset = cfgObj.optString("customImagePreset", current.customImagePreset),
                        customImageUri = cfgObj.optString("customImageUri", current.customImageUri ?: ""),
                        dwellAutoClickEnabled = cfgObj.optBoolean("dwellAutoClickEnabled", current.dwellAutoClickEnabled),
                        dwellTimeMs = cfgObj.optLong("dwellTimeMs", current.dwellTimeMs),
                        clickOffsetX = cfgObj.optDouble("clickOffsetX", current.clickOffsetX.toDouble()).toFloat(),
                        clickOffsetY = cfgObj.optDouble("clickOffsetY", current.clickOffsetY.toDouble()).toFloat(),
                        floatingActionButtonEnabled = cfgObj.optBoolean("floatingActionButtonEnabled", current.floatingActionButtonEnabled),
                        sideIndicatorsEnabled = cfgObj.optBoolean("sideIndicatorsEnabled", current.sideIndicatorsEnabled),
                        sideIndicatorsOnRight = cfgObj.optBoolean("sideIndicatorsOnRight", current.sideIndicatorsOnRight),
                        sideIndicatorHeightDp = cfgObj.optInt("sideIndicatorHeightDp", current.sideIndicatorHeightDp),
                        sideIndicatorWidthDp = cfgObj.optInt("sideIndicatorWidthDp", current.sideIndicatorWidthDp),
                        sideIndicatorPositionOffset = cfgObj.optDouble("sideIndicatorPositionOffset", current.sideIndicatorPositionOffset.toDouble()).toFloat(),
                        sideIndicatorActiveColorArgb = cfgObj.optLong("sideIndicatorActiveColorArgb", current.sideIndicatorActiveColorArgb),
                        sideIndicatorInactiveColorArgb = cfgObj.optLong("sideIndicatorInactiveColorArgb", current.sideIndicatorInactiveColorArgb),
                        showSequenceVisualizer = cfgObj.optBoolean("showSequenceVisualizer", current.showSequenceVisualizer),
                        sequenceMatchedColorArgb = cfgObj.optLong("sequenceMatchedColorArgb", current.sequenceMatchedColorArgb),
                        lastPressedHighlightColorArgb = cfgObj.optLong("lastPressedHighlightColorArgb", current.lastPressedHighlightColorArgb),
                        comboTimeoutMs = cfgObj.optLong("comboTimeoutMs", current.comboTimeoutMs),
                        precisionSpeedMultiplier = cfgObj.optDouble("precisionSpeedMultiplier", current.precisionSpeedMultiplier.toDouble()).toFloat(),
                        doubleClickSpeedMs = cfgObj.optLong("doubleClickSpeedMs", current.doubleClickSpeedMs),
                        longPressDurationMs = cfgObj.optLong("longPressDurationMs", current.longPressDurationMs),
                        hapticFeedback = cfgObj.optBoolean("hapticFeedback", current.hapticFeedback),
                        hapticIntensity = try { HapticIntensity.valueOf(cfgObj.optString("hapticIntensity", current.hapticIntensity.name)) } catch (_: Exception) { current.hapticIntensity },
                        hapticOnClick = cfgObj.optBoolean("hapticOnClick", current.hapticOnClick),
                        hapticOnLongClick = cfgObj.optBoolean("hapticOnLongClick", current.hapticOnLongClick),
                        hapticOnScroll = cfgObj.optBoolean("hapticOnScroll", current.hapticOnScroll),
                        hapticOnActionMenu = cfgObj.optBoolean("hapticOnActionMenu", current.hapticOnActionMenu),
                        hapticOnRecenter = cfgObj.optBoolean("hapticOnRecenter", current.hapticOnRecenter),
                        audioFeedback = cfgObj.optBoolean("audioFeedback", current.audioFeedback),
                        soundType = try { FeedbackSoundType.valueOf(cfgObj.optString("soundType", current.soundType.name)) } catch (_: Exception) { current.soundType },
                        audioOnClick = cfgObj.optBoolean("audioOnClick", current.audioOnClick),
                        audioOnLongClick = cfgObj.optBoolean("audioOnLongClick", current.audioOnLongClick),
                        audioOnScroll = cfgObj.optBoolean("audioOnScroll", current.audioOnScroll),
                        audioOnActionMenu = cfgObj.optBoolean("audioOnActionMenu", current.audioOnActionMenu),
                        audioOnRecenter = cfgObj.optBoolean("audioOnRecenter", current.audioOnRecenter),
                        showCoordinateHUD = cfgObj.optBoolean("showCoordinateHUD", current.showCoordinateHUD),
                        showRippleOnClick = cfgObj.optBoolean("showRippleOnClick", current.showRippleOnClick),
                        continuousScrollSpeedMs = cfgObj.optLong("continuousScrollSpeedMs", current.continuousScrollSpeedMs),
                        scrollStepDistancePx = cfgObj.optInt("scrollStepDistancePx", current.scrollStepDistancePx),
                        themeMode = try { ThemeMode.valueOf(cfgObj.optString("themeMode", current.themeMode.name)) } catch (_: Exception) { current.themeMode },
                        navBarStyle = try { NavigationBarStyle.valueOf(cfgObj.optString("navBarStyle", current.navBarStyle.name)) } catch (_: Exception) { current.navBarStyle },
                        appColorProfile = try { AppColorProfile.valueOf(cfgObj.optString("appColorProfile", current.appColorProfile.name)) } catch (_: Exception) { current.appColorProfile }
                    )
                }
                restoredCount++
            }

            // 2. Restore Button Mappings
            if (root.has("buttonMappings")) {
                val mappingsObj = root.getJSONObject("buttonMappings")
                val restoredMappings = mutableMapOf<ButtonTrigger, CursorAction>()
                val keys = mappingsObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    try {
                        val trigger = ButtonTrigger.valueOf(key)
                        val action = CursorAction.valueOf(mappingsObj.getString(key))
                        restoredMappings[trigger] = action
                    } catch (_: Exception) {}
                }
                if (restoredMappings.isNotEmpty()) {
                    preferencesManager.setAllButtonMappings(restoredMappings)
                }
                restoredCount++
            }

            // 3. Restore Custom Key Sequences
            if (root.has("customSequences")) {
                val seqArray = root.getJSONArray("customSequences")
                val restoredSequences = mutableListOf<CustomKeySequence>()
                for (i in 0 until seqArray.length()) {
                    val sObj = seqArray.getJSONObject(i)
                    val id = sObj.optString("id", java.util.UUID.randomUUID().toString())
                    val name = sObj.optString("name", "Sequence $i")
                    val action = try { CursorAction.valueOf(sObj.getString("action")) } catch (_: Exception) { CursorAction.NONE }
                    val isEnabled = sObj.optBoolean("isEnabled", true)
                    val stepsList = mutableListOf<KeyStep>()
                    val stepsArr = sObj.optJSONArray("steps")
                    if (stepsArr != null) {
                        for (s in 0 until stepsArr.length()) {
                            try {
                                stepsList.add(KeyStep.valueOf(stepsArr.getString(s)))
                            } catch (_: Exception) {}
                        }
                    }
                    if (stepsList.isNotEmpty()) {
                        restoredSequences.add(
                            CustomKeySequence(
                                id = id,
                                name = name,
                                steps = stepsList,
                                action = action,
                                isEnabled = isEnabled
                            )
                        )
                    }
                }
                if (restoredSequences.isNotEmpty()) {
                    preferencesManager.saveCustomSequences(restoredSequences)
                    restoredCount++
                }
            }

            // 4. Restore App Shortcuts
            if (root.has("customShortcuts")) {
                val shortcutsArr = root.getJSONArray("customShortcuts")
                val restoredShortcuts = mutableListOf<CustomAppShortcut>()
                for (i in 0 until shortcutsArr.length()) {
                    val item = shortcutsArr.getJSONObject(i)
                    restoredShortcuts.add(
                        CustomAppShortcut(
                            id = item.optString("id", java.util.UUID.randomUUID().toString()),
                            label = item.optString("label", "App"),
                            packageName = item.optString("packageName", ""),
                            iconPreset = item.optString("iconPreset", "GENERIC"),
                            colorArgb = item.optLong("colorArgb", 0xFF3B82F6)
                        )
                    )
                }
                if (restoredShortcuts.isNotEmpty()) {
                    preferencesManager.saveCustomShortcuts(restoredShortcuts)
                    restoredCount++
                }
            }

            val timestamp = root.optLong("timestamp", System.currentTimeMillis())
            val dateStr = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))
            Result.success("Restored successfully from backup ($dateStr)")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

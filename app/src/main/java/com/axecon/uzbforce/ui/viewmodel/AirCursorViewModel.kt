package com.axecon.uzbforce.ui.viewmodel

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axecon.uzbforce.data.PreferencesManager
import com.axecon.uzbforce.model.ButtonTrigger
import com.axecon.uzbforce.model.CursorAction
import com.axecon.uzbforce.model.CursorConfig
import com.axecon.uzbforce.model.CustomAppShortcut
import com.axecon.uzbforce.model.CustomKeySequence
import com.axecon.uzbforce.model.PresetProfile
import com.axecon.uzbforce.model.SideButtonIndicator
import com.axecon.uzbforce.sensor.SensorTelemetry
import com.axecon.uzbforce.service.AirCursorAccessibilityService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PermissionStatus(
    val isAccessibilityEnabled: Boolean = false,
    val isOverlayPermissionGranted: Boolean = false,
    val isBatteryOptimizationIgnored: Boolean = false,
    val isNotificationPermissionGranted: Boolean = true,
    val isPhonePermissionGranted: Boolean = false
) {
    // Both Accessibility Service and Draw Over Apps are strict mandatory requirements
    val areMandatoryGranted: Boolean
        get() = isAccessibilityEnabled && isOverlayPermissionGranted

    val areAllGranted: Boolean
        get() = isAccessibilityEnabled && isOverlayPermissionGranted && isBatteryOptimizationIgnored && isNotificationPermissionGranted
}

data class UiState(
    val config: CursorConfig = CursorConfig(),
    val buttonMappings: Map<ButtonTrigger, CursorAction> = emptyMap(),
    val customSequences: List<CustomKeySequence> = emptyList(),
    val customShortcuts: List<CustomAppShortcut> = emptyList(),
    val permissions: PermissionStatus = PermissionStatus(),
    val hasCompletedPermissionFlow: Boolean = false,
    val isReviewingPermissions: Boolean = false,
    val telemetry: SensorTelemetry = SensorTelemetry(),
    val lastTriggeredAction: String? = null,
    val selectedTab: Int = 0,
    val editingTrigger: ButtonTrigger? = null,
    val isSimulatingMotion: Boolean = false
)

class AirCursorViewModel(private val preferencesManager: PreferencesManager) : ViewModel() {

    private val _uiState = MutableStateFlow(
        UiState(hasCompletedPermissionFlow = preferencesManager.hasCompletedPermissionFlow())
    )
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val telemetryFlow: StateFlow<SensorTelemetry> = AirCursorAccessibilityService.telemetryFlow

    init {
        viewModelScope.launch {
            preferencesManager.configFlow.collect { cfg ->
                _uiState.value = _uiState.value.copy(config = cfg)
            }
        }

        viewModelScope.launch {
            preferencesManager.buttonMappingsFlow.collect { mappings ->
                _uiState.value = _uiState.value.copy(buttonMappings = mappings)
            }
        }

        viewModelScope.launch {
            preferencesManager.customSequencesFlow.collect { seqs ->
                _uiState.value = _uiState.value.copy(customSequences = seqs)
            }
        }

        viewModelScope.launch {
            preferencesManager.customShortcutsFlow.collect { shortcuts ->
                _uiState.value = _uiState.value.copy(customShortcuts = shortcuts)
            }
        }

        viewModelScope.launch {
            AirCursorAccessibilityService.lastTriggeredAction.collect { act ->
                _uiState.value = _uiState.value.copy(lastTriggeredAction = act)
            }
        }
    }

    fun checkPermissions(context: Context) {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        val enabledServices = am?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        var isA11yEnabled = enabledServices?.any {
            it.resolveInfo?.serviceInfo?.packageName == context.packageName
        } ?: false

        if (!isA11yEnabled) {
            try {
                val prefString = Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                )
                if (prefString != null && prefString.contains(context.packageName)) {
                    isA11yEnabled = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (AirCursorAccessibilityService.instance != null) {
            isA11yEnabled = true
        }

        val isOverlayGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isBatteryIgnored = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
        } else {
            true
        }

        val isNotifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        val isPhoneGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED

        val permStatus = PermissionStatus(
            isAccessibilityEnabled = isA11yEnabled,
            isOverlayPermissionGranted = isOverlayGranted,
            isBatteryOptimizationIgnored = isBatteryIgnored,
            isNotificationPermissionGranted = isNotifGranted,
            isPhonePermissionGranted = isPhoneGranted
        )

        _uiState.value = _uiState.value.copy(permissions = permStatus)
    }

    fun setTab(index: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = index)
    }

    fun confirmPermissionsAndProceed() {
        preferencesManager.setCompletedPermissionFlow(true)
        _uiState.value = _uiState.value.copy(hasCompletedPermissionFlow = true, isReviewingPermissions = false)
    }

    fun openPermissionsReview() {
        _uiState.value = _uiState.value.copy(isReviewingPermissions = true)
    }

    fun closePermissionsReview() {
        _uiState.value = _uiState.value.copy(isReviewingPermissions = false)
    }

    fun setEditingTrigger(trigger: ButtonTrigger?) {
        _uiState.value = _uiState.value.copy(editingTrigger = trigger)
    }

    fun toggleMasterEnabled() {
        preferencesManager.updateConfig {
            it.copy(isEnabled = !it.isEnabled)
        }
    }

    fun updateConfig(update: (CursorConfig) -> CursorConfig) {
        preferencesManager.updateConfig(update)
    }

    fun updateIndividualIndicator(indicator: SideButtonIndicator) {
        preferencesManager.updateIndividualIndicator(indicator)
    }

    fun updateButtonMapping(trigger: ButtonTrigger, action: CursorAction) {
        preferencesManager.updateButtonMapping(trigger, action)
        _uiState.value = _uiState.value.copy(editingTrigger = null)
    }

    fun applyPreset(preset: PresetProfile) {
        preferencesManager.applyPreset(preset)
    }

    // Custom Key Sequences
    fun addCustomSequence(seq: CustomKeySequence) {
        preferencesManager.addCustomSequence(seq)
    }

    fun updateCustomSequence(seq: CustomKeySequence) {
        preferencesManager.updateCustomSequence(seq)
    }

    fun deleteCustomSequence(id: String) {
        preferencesManager.deleteCustomSequence(id)
    }

    fun toggleCustomSequence(id: String) {
        preferencesManager.toggleCustomSequence(id)
    }

    // Custom App Shortcuts
    fun addCustomShortcut(shortcut: CustomAppShortcut) {
        preferencesManager.addCustomShortcut(shortcut)
    }

    fun removeCustomShortcut(id: String) {
        preferencesManager.removeCustomShortcut(id)
    }

    fun updateCustomShortcut(shortcut: CustomAppShortcut) {
        preferencesManager.updateCustomShortcut(shortcut)
    }

    fun recenterAndCalibrate() {
        AirCursorAccessibilityService.calibrateNeutral()
    }

    fun restartEngine(context: Context) {
        if (AirCursorAccessibilityService.instance != null) {
            AirCursorAccessibilityService.restartEngine(context)
        } else {
            checkPermissions(context)
            if (!_uiState.value.permissions.isAccessibilityEnabled) {
                openAccessibilitySettings(context)
            }
        }
    }

    fun resetAllSettings() {
        preferencesManager.resetAllToDefaults()
    }

    fun exportBackupJson(): String {
        return preferencesManager.exportBackupJson()
    }

    fun importBackupJson(json: String): Boolean {
        return preferencesManager.importBackupJson(json)
    }

    fun togglePrecisionMode(): Boolean {
        return AirCursorAccessibilityService.togglePrecision()
    }

    fun toggleTracking(): Boolean {
        return AirCursorAccessibilityService.toggleTracking()
    }

    fun triggerActionFromApp(action: CursorAction) {
        AirCursorAccessibilityService.triggerActionFromApp(action)
    }

    fun openAccessibilitySettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun openOverlaySettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun requestBatteryOptimizationExemption(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                try {
                    val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(fallback)
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }
    }

    fun openAppNotificationSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Backup and Restore
    private val _backupStatus = MutableStateFlow<String?>(null)
    val backupStatus: StateFlow<String?> = _backupStatus.asStateFlow()

    fun clearBackupStatus() {
        _backupStatus.value = null
    }

    fun exportBackup(context: Context, uri: Uri) {
        val manager = com.axecon.uzbforce.data.BackupRestoreManager(context)
        val result = manager.exportBackupToUri(uri)
        if (result.isSuccess) {
            _backupStatus.value = "Settings exported successfully to .axcn file!"
        } else {
            _backupStatus.value = "Failed to export: ${result.exceptionOrNull()?.message}"
        }
    }

    fun importBackup(context: Context, uri: Uri) {
        val manager = com.axecon.uzbforce.data.BackupRestoreManager(context)
        val result = manager.importBackupFromUri(uri)
        if (result.isSuccess) {
            _backupStatus.value = result.getOrNull() ?: "Backup successfully restored!"
            AirCursorAccessibilityService.restartEngine(context)
        } else {
            _backupStatus.value = "Failed to restore: ${result.exceptionOrNull()?.message}"
        }
    }

    fun getBackupJsonString(context: Context): String {
        val manager = com.axecon.uzbforce.data.BackupRestoreManager(context)
        return manager.createBackupJsonString()
    }

    fun restoreFromText(context: Context, text: String): Boolean {
        val manager = com.axecon.uzbforce.data.BackupRestoreManager(context)
        val result = manager.restoreFromJsonString(text)
        if (result.isSuccess) {
            _backupStatus.value = result.getOrNull() ?: "Backup successfully restored!"
            AirCursorAccessibilityService.restartEngine(context)
            return true
        } else {
            _backupStatus.value = "Failed to restore: ${result.exceptionOrNull()?.message}"
            return false
        }
    }

    fun openAppDetailsSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

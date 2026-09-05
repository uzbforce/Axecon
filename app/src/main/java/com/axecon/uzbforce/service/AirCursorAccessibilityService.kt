package com.axecon.uzbforce.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Path
import android.graphics.Rect
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.speech.RecognizerIntent
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import android.util.DisplayMetrics
import android.view.KeyEvent
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import com.axecon.uzbforce.MainActivity
import com.axecon.uzbforce.data.PreferencesManager
import com.axecon.uzbforce.model.ButtonTrigger
import com.axecon.uzbforce.model.CursorAction
import com.axecon.uzbforce.model.CursorConfig
import com.axecon.uzbforce.model.CustomKeySequence
import com.axecon.uzbforce.model.DragStateInfo
import com.axecon.uzbforce.model.HapticIntensity
import com.axecon.uzbforce.model.KeyStep
import com.axecon.uzbforce.model.SideButtonIndicator
import com.axecon.uzbforce.model.TrackingParadigm
import com.axecon.uzbforce.overlay.OverlayViewManager
import com.axecon.uzbforce.sensor.MotionSensorTracker
import com.axecon.uzbforce.sensor.SensorTelemetry
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AirCursorAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mainHandler = Handler(Looper.getMainLooper())

    private lateinit var preferencesManager: PreferencesManager
    private var motionTracker: MotionSensorTracker? = null
    private var overlayManager: OverlayViewManager? = null
    private var keyGestureProcessor: KeyGestureProcessor? = null
    private var feedbackHelper: FeedbackHelper? = null

    private var audioManager: AudioManager? = null
    private var isFlashlightOn = false

    private var realScreenW: Int = 1080
    private var realScreenH: Int = 2400

    // Dwell Click Tracking
    private var lastDwellX = 0f
    private var lastDwellY = 0f
    private var dwellStartTime = 0L
    private var isDwellCounting = false

    private val dwellCheckRunnable = object : Runnable {
        override fun run() {
            val cfg = preferencesManager.getConfig()
            if (!cfg.dwellAutoClickEnabled || motionTracker == null) return

            val curX = motionTracker!!.cursorX
            val curY = motionTracker!!.cursorY

            val dist = Math.hypot((curX - lastDwellX).toDouble(), (curY - lastDwellY).toDouble()).toFloat()
            if (dist < 18f) {
                if (!isDwellCounting) {
                    isDwellCounting = true
                    dwellStartTime = System.currentTimeMillis()
                }

                val elapsed = System.currentTimeMillis() - dwellStartTime
                val progress = (elapsed.toFloat() / cfg.dwellTimeMs).coerceIn(0f, 1f)
                overlayManager?.setDwellProgress(progress)

                if (elapsed >= cfg.dwellTimeMs) {
                    performClickAtCursor()
                    isDwellCounting = false
                    dwellStartTime = System.currentTimeMillis()
                    overlayManager?.setDwellProgress(0f)
                }
            } else {
                lastDwellX = curX
                lastDwellY = curY
                isDwellCounting = false
                dwellStartTime = System.currentTimeMillis()
                overlayManager?.setDwellProgress(0f)
            }

            mainHandler.postDelayed(this, 50L)
        }
    }

    // Dragging state
    private var isDragging = false
    private var isHoldDragging = false
    private var dragStartX = 0f
    private var dragStartY = 0f

    // Screen state receiver for 0% battery consumption when screen is off
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    motionTracker?.stop()
                }
                Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> {
                    val cfg = preferencesManager.getConfig()
                    if (cfg.isEnabled) {
                        motionTracker?.start()
                        if (cfg.autoRecenterOnScreenOn) {
                            motionTracker?.recenter()
                        }
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        _isServiceActive.value = true

        try {
            preferencesManager = PreferencesManager.getInstance(this)
            feedbackHelper = FeedbackHelper(this)
            audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isServiceActive.value = true

        try {
            preferencesManager = PreferencesManager.getInstance(this)
            feedbackHelper = FeedbackHelper(this)
            audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager

            initOverlayAndSensors()
            registerScreenStateReceiver()
            observePreferences()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initOverlayAndSensors() {
        try {
            val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val (screenW, screenH) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val bounds = windowManager.maximumWindowMetrics.bounds
                Pair(bounds.width(), bounds.height())
            } else {
                val metrics = DisplayMetrics()
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay.getRealMetrics(metrics)
                Pair(metrics.widthPixels, metrics.heightPixels)
            }
            realScreenW = screenW
            realScreenH = screenH

            val initialConfig = preferencesManager.getConfig()

            overlayManager = OverlayViewManager(this, initialConfig) { action ->
                executeAction(action)
            }

            motionTracker = MotionSensorTracker(this, initialConfig, screenW, screenH) { x, y ->
                overlayManager?.updateCursorPosition(x, y)
                if (isDragging) {
                    _dragStateFlow.value = _dragStateFlow.value.copy(
                        currentX = x,
                        currentY = y
                    )
                }
            }

            keyGestureProcessor = KeyGestureProcessor(
                getMappings = { preferencesManager.getButtonMappings() },
                getCustomSequences = { preferencesManager.getCustomSequences() },
                getDoubleTapWindowMs = { preferencesManager.getConfig().doubleClickSpeedMs },
                getLongPressWindowMs = { preferencesManager.getConfig().longPressDurationMs },
                getComboTimeoutMs = { preferencesManager.getConfig().comboTimeoutMs },
                getContinuousScrollIntervalMs = { preferencesManager.getConfig().continuousScrollSpeedMs },
                onActionTriggered = { action, trigger ->
                    _lastTriggeredAction.value = "${trigger.label} ➔ ${action.label}"
                    executeAction(action)
                },
                onSequenceMatched = { seq ->
                    _lastTriggeredAction.value = "Combo: ${seq.name} ➔ ${seq.targetAction.label}"
                    if (seq.targetAction == CursorAction.LAUNCH_CUSTOM_APP && !seq.customPackage.isNullOrBlank()) {
                        launchAppPackage(seq.customPackage)
                    } else {
                        executeAction(seq.targetAction)
                    }
                },
                onSequenceStepAdded = { steps, matchedSeq, timeoutMs ->
                    overlayManager?.updateSequenceCombo(steps, matchedSeq, timeoutMs)
                },
                onSequenceCleared = {
                    overlayManager?.clearSequenceCombo()
                },
                onKeyVisualized = { lineIndex ->
                    overlayManager?.highlightSideIndicator(lineIndex)
                },
                onContinuousScrollTick = { direction ->
                    val x = motionTracker?.cursorX ?: (screenW / 2f)
                    val y = motionTracker?.cursorY ?: (screenH / 2f)
                    val cfg = preferencesManager.getConfig()
                    feedbackHelper?.playFeedbackForAction(CursorAction.CONTINUOUS_SCROLL_UP, cfg)
                    when (direction) {
                        KeyGestureProcessor.ScrollDirection.UP -> dispatchScrollGesture(x, y, isScrollUp = true)
                        KeyGestureProcessor.ScrollDirection.DOWN -> dispatchScrollGesture(x, y, isScrollUp = false)
                        KeyGestureProcessor.ScrollDirection.LEFT -> dispatchHorizontalScrollGesture(x, y, isScrollLeft = true)
                        KeyGestureProcessor.ScrollDirection.RIGHT -> dispatchHorizontalScrollGesture(x, y, isScrollLeft = false)
                    }
                },
                onHoldActionReleased = { action, trigger ->
                    if (action == CursorAction.HOLD_TO_DRAG || isHoldDragging) {
                        endHoldDrag()
                    }
                }
            )

            if (initialConfig.isEnabled) {
                overlayManager?.attach()
                motionTracker?.start()
            }

            if (initialConfig.dwellAutoClickEnabled) {
                mainHandler.removeCallbacks(dwellCheckRunnable)
                mainHandler.post(dwellCheckRunnable)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun onConfigUpdated(newConfig: CursorConfig) {
        motionTracker?.updateConfig(newConfig)
        overlayManager?.updateConfig(newConfig)

        if (newConfig.isEnabled) {
            if (overlayManager?.isAttached() != true) {
                overlayManager?.attach()
            }
            motionTracker?.start()
            if (newConfig.dwellAutoClickEnabled) {
                mainHandler.removeCallbacks(dwellCheckRunnable)
                mainHandler.post(dwellCheckRunnable)
            } else {
                mainHandler.removeCallbacks(dwellCheckRunnable)
                overlayManager?.setDwellProgress(0f)
            }
        } else {
            mainHandler.removeCallbacks(dwellCheckRunnable)
            overlayManager?.setDwellProgress(0f)
            motionTracker?.stop()
            overlayManager?.detach()
            keyGestureProcessor?.reset()
        }
    }

    fun rebootAndReload() {
        try {
            mainHandler.removeCallbacksAndMessages(null)
            motionTracker?.stop()
            overlayManager?.detach()
            keyGestureProcessor?.reset()

            initOverlayAndSensors()
            feedbackHelper?.vibratePulse(com.axecon.uzbforce.model.HapticIntensity.STRONG)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun onSequencesUpdated(sequences: List<CustomKeySequence>) {
        // Automatically reflected via lambda closure in KeyGestureProcessor
    }

    private fun observePreferences() {
        serviceScope.launch {
            preferencesManager.configFlow.collect { cfg ->
                motionTracker?.updateConfig(cfg)
                overlayManager?.updateConfig(cfg)

                if (cfg.isEnabled) {
                    if (overlayManager?.isAttached() != true) {
                        overlayManager?.attach()
                    }
                    motionTracker?.start()

                    if (cfg.dwellAutoClickEnabled) {
                        mainHandler.removeCallbacks(dwellCheckRunnable)
                        mainHandler.post(dwellCheckRunnable)
                    } else {
                        mainHandler.removeCallbacks(dwellCheckRunnable)
                        overlayManager?.setDwellProgress(0f)
                    }
                } else {
                    mainHandler.removeCallbacks(dwellCheckRunnable)
                    overlayManager?.setDwellProgress(0f)
                    motionTracker?.stop()
                    overlayManager?.detach()
                    keyGestureProcessor?.reset()
                }
            }
        }

        // Collect motion telemetry
        serviceScope.launch {
            while (true) {
                motionTracker?.let {
                    _telemetryFlow.value = it.telemetry.value
                }
                kotlinx.coroutines.delay(100)
            }
        }
    }

    private fun registerScreenStateReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(screenReceiver, filter)
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        event ?: return false

        if (isRecordingSequence) {
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                val step = when (event.keyCode) {
                    KeyEvent.KEYCODE_VOLUME_UP -> KeyStep.VOLUME_UP
                    KeyEvent.KEYCODE_VOLUME_DOWN -> KeyStep.VOLUME_DOWN
                    KeyEvent.KEYCODE_HEADSETHOOK -> KeyStep.HEADSET_HOOK
                    KeyEvent.KEYCODE_POWER -> KeyStep.POWER
                    KeyEvent.KEYCODE_BACK -> KeyStep.BACK
                    else -> null
                }
                if (step != null) {
                    mainHandler.post {
                        sequenceRecordingListener?.invoke(step)
                    }
                    val idx = when (step) {
                        KeyStep.VOLUME_UP -> 0
                        KeyStep.VOLUME_DOWN -> 1
                        else -> 2
                    }
                    overlayManager?.highlightSideIndicator(idx)
                    feedbackHelper?.vibrateClick(HapticIntensity.LIGHT)
                }
            }
            // Consume event completely so system volume / default action is not triggered while recording
            return true
        }

        val intercepted = keyGestureProcessor?.onKeyEvent(event) ?: false
        return if (intercepted) true else super.onKeyEvent(event)
    }

    fun executeAction(action: CursorAction) {
        val cfg = preferencesManager.getConfig()
        feedbackHelper?.playFeedbackForAction(action, cfg)

        val curX = motionTracker?.cursorX ?: 500f
        val curY = motionTracker?.cursorY ?: 1000f

        when (action) {
            CursorAction.TOGGLE_MASTER_SERVICE -> {
                val current = cfg.isEnabled
                val newState = !current
                preferencesManager.updateConfig { it.copy(isEnabled = newState) }
                if (newState) {
                    feedbackHelper?.vibratePulse(HapticIntensity.STRONG)
                    motionTracker?.recenter()
                }
            }

            CursorAction.CLICK -> if (cfg.isEnabled) performClickAtCursor()
            CursorAction.LONG_CLICK -> if (cfg.isEnabled) performLongClickAtCursor()
            CursorAction.DOUBLE_CLICK -> if (cfg.isEnabled) performDoubleClickAtCursor()
            CursorAction.HOLD_TO_DRAG -> if (cfg.isEnabled) {
                if (isHoldDragging) endHoldDrag() else startHoldDrag()
            }
            CursorAction.DRAG_TOGGLE -> if (cfg.isEnabled) toggleDragMode()
            CursorAction.SWIPE_TOGGLE -> if (cfg.isEnabled) toggleSwipeMode()

            CursorAction.SCROLL_UP -> if (cfg.isEnabled) dispatchScrollGesture(curX, curY, isScrollUp = true)
            CursorAction.SCROLL_DOWN -> if (cfg.isEnabled) dispatchScrollGesture(curX, curY, isScrollUp = false)
            CursorAction.SCROLL_LEFT -> if (cfg.isEnabled) dispatchHorizontalScrollGesture(curX, curY, isScrollLeft = true)
            CursorAction.SCROLL_RIGHT -> if (cfg.isEnabled) dispatchHorizontalScrollGesture(curX, curY, isScrollLeft = false)
            CursorAction.CONTINUOUS_SCROLL_UP -> if (cfg.isEnabled) dispatchScrollGesture(curX, curY, isScrollUp = true)
            CursorAction.CONTINUOUS_SCROLL_DOWN -> if (cfg.isEnabled) dispatchScrollGesture(curX, curY, isScrollUp = false)

            CursorAction.OPEN_ACTION_MENU -> if (cfg.isEnabled) overlayManager?.showActionMenu()
            CursorAction.RECENTER_CALIBRATE -> {
                motionTracker?.recenter()
            }
            CursorAction.TOGGLE_PRECISION_MODE -> {
                val isPrec = motionTracker?.togglePrecisionMode() ?: false
                preferencesManager.updateConfig { it.copy(isPrecisionActive = isPrec) }
            }
            CursorAction.TOGGLE_TRACKING -> {
                if (!cfg.isEnabled) {
                    preferencesManager.updateConfig { it.copy(isEnabled = true) }
                    feedbackHelper?.vibratePulse(HapticIntensity.STRONG)
                    motionTracker?.recenter()
                } else {
                    val isPaused = motionTracker?.toggleTracking() ?: false
                    preferencesManager.updateConfig { it.copy(isTrackingPaused = isPaused) }
                }
            }
            CursorAction.TOGGLE_TRACKING_PARADIGM -> {
                motionTracker?.recenter()
            }
            CursorAction.TOGGLE_FLASHLIGHT,
            CursorAction.TOGGLE_TORCH -> toggleTorch()

            CursorAction.VOLUME_UP -> audioManager?.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
            CursorAction.VOLUME_DOWN -> audioManager?.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
            CursorAction.ANSWER_OR_END_CALL -> handlePhoneCall()

            CursorAction.GLOBAL_BACK -> performGlobalAction(GLOBAL_ACTION_BACK)
            CursorAction.GLOBAL_HOME -> performGlobalAction(GLOBAL_ACTION_HOME)
            CursorAction.GLOBAL_RECENTS -> performGlobalAction(GLOBAL_ACTION_RECENTS)
            CursorAction.GLOBAL_NOTIFICATIONS -> performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
            CursorAction.GLOBAL_QUICK_SETTINGS -> performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
            CursorAction.GLOBAL_POWER_DIALOG -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)
                }
            }
            CursorAction.GLOBAL_LOCK_SCREEN -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                }
            }
            CursorAction.GLOBAL_SCREENSHOT -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
                }
            }

            CursorAction.OPEN_PHONE_APP -> launchPhoneDialer()
            CursorAction.OPEN_BROWSER_APP -> launchWebBrowser()
            CursorAction.OPEN_SETTINGS_APP -> launchSettings()
            CursorAction.OPEN_MESSAGES_APP -> launchMessages()
            CursorAction.OPEN_ACCESSIBILITY_SETTINGS -> launchAccessibilitySettings()
            CursorAction.LAUNCH_CUSTOM_APP -> {
                val firstShortcut = preferencesManager.getCustomShortcuts().firstOrNull()
                if (firstShortcut != null) {
                    launchAppPackage(firstShortcut.packageName)
                }
            }

            CursorAction.VOICE_TYPING -> launchVoiceTyping()
            CursorAction.SHOW_KEYBOARD_HELPER -> overlayManager?.toggleDpadHelper()
            CursorAction.NONE -> {}
        }
    }

    private fun toggleTorch() {
        try {
            val cameraManager = getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return
            val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return
            isFlashlightOn = !isFlashlightOn
            cameraManager.setTorchMode(cameraId, isFlashlightOn)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private var lastClickTimeMs = 0L

    private fun performClickAtCursor(isExplicitDoubleClick: Boolean = false) {
        val now = SystemClock.uptimeMillis()
        if (!isExplicitDoubleClick && now - lastClickTimeMs < 90L) {
            // Mechanical switch bounce debounce
            return
        }
        lastClickTimeMs = now

        val cfg = preferencesManager.getConfig()
        val rawX = motionTracker?.cursorX ?: return
        val rawY = motionTracker?.cursorY ?: return

        overlayManager?.triggerClickAnimation()

        // If the action menu is currently displayed, route the cursor click to it directly
        if (overlayManager?.handleActionMenuClick(rawX, rawY) == true) {
            return
        }

        val maxW = realScreenW.toFloat()
        val maxH = realScreenH.toFloat()

        // Apply hotspot calibration offset so touch event lands exactly on the visual cursor hotspot
        val targetX = (rawX + cfg.clickOffsetX).coerceIn(0f, maxW)
        val targetY = (rawY + cfg.clickOffsetY).coerceIn(0f, maxH)

        // 1. Direct Accessibility Node Click ONLY on security-restricted dialogs
        // (Permission dialogs, package installer, incoming ringing calls where OS blocks simulated touches)
        if (tryClickSecurityNodeAt(targetX, targetY)) {
            return
        }

        // 2. Dispatch simulated touch tap
        // 35ms duration + micro-offset guarantees non-zero path length for PathMeasure,
        // delivering instant ACTION_DOWN followed immediately by ACTION_UP without lingering.
        val clickPath = Path().apply {
            moveTo(targetX, targetY)
            lineTo(targetX + 0.5f, targetY + 0.5f)
        }

        val stroke = GestureDescription.StrokeDescription(clickPath, 0, 35)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                feedbackHelper?.vibrateClick(HapticIntensity.LIGHT)
            }
            override fun onCancelled(gestureDescription: GestureDescription?) {
                // If touch was cancelled or blocked by OS (e.g. system security boundary),
                // fall back to node click
                tryClickFallbackNodeAt(targetX, targetY)
            }
        }, null)
    }

    private fun performLongClickAtCursor() {
        val cfg = preferencesManager.getConfig()
        val rawX = motionTracker?.cursorX ?: return
        val rawY = motionTracker?.cursorY ?: return

        overlayManager?.triggerClickAnimation()

        val maxW = realScreenW.toFloat()
        val maxH = realScreenH.toFloat()

        val targetX = (rawX + cfg.clickOffsetX).coerceIn(0f, maxW)
        val targetY = (rawY + cfg.clickOffsetY).coerceIn(0f, maxH)

        val path = Path().apply {
            moveTo(targetX, targetY)
            lineTo(targetX + 0.5f, targetY + 0.5f)
        }

        val duration = cfg.longPressDurationMs.coerceIn(500L, 1200L)
        val stroke = GestureDescription.StrokeDescription(path, 0, duration)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                feedbackHelper?.vibrateClick(HapticIntensity.STRONG)
            }
        }, null)
    }

    private fun performDoubleClickAtCursor() {
        val cfg = preferencesManager.getConfig()
        val rawX = motionTracker?.cursorX ?: return
        val rawY = motionTracker?.cursorY ?: return

        overlayManager?.triggerClickAnimation()

        val maxW = realScreenW.toFloat()
        val maxH = realScreenH.toFloat()

        val targetX = (rawX + cfg.clickOffsetX).coerceIn(0f, maxW)
        val targetY = (rawY + cfg.clickOffsetY).coerceIn(0f, maxH)

        if (tryClickSecurityNodeAt(targetX, targetY)) {
            return
        }

        val path1 = Path().apply {
            moveTo(targetX, targetY)
            lineTo(targetX + 0.5f, targetY + 0.5f)
        }
        val path2 = Path().apply {
            moveTo(targetX, targetY)
            lineTo(targetX + 0.5f, targetY + 0.5f)
        }

        // Single atomic gesture with two successive taps (0ms->35ms and 85ms->120ms)
        val stroke1 = GestureDescription.StrokeDescription(path1, 0, 35)
        val stroke2 = GestureDescription.StrokeDescription(path2, 85, 35)
        val gesture = GestureDescription.Builder()
            .addStroke(stroke1)
            .addStroke(stroke2)
            .build()

        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                feedbackHelper?.vibrateClick(HapticIntensity.MEDIUM)
            }
        }, null)
    }

    // Direct Accessibility Node Clicking ONLY for Permission Popups and Incoming Calls
    private fun tryClickSecurityNodeAt(x: Float, y: Float): Boolean {
        try {
            val root = rootInActiveWindow ?: return false
            val pkg = root.packageName?.toString()?.lowercase() ?: return false
            if (pkg == packageName.lowercase()) return false

            val isPermission = pkg.contains("permissioncontroller") ||
                    pkg.contains("packageinstaller")
            val isCallUI = pkg.contains("incall") ||
                    pkg.contains("dialer") ||
                    pkg.contains("telecom")

            if (!isPermission && !isCallUI) {
                return false
            }

            if (isCallUI) {
                try {
                    val audioMgr = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                    val telecomMgr = getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
                    val isRinging = audioMgr?.mode == AudioManager.MODE_RINGTONE
                    if (isRinging && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && telecomMgr != null) {
                        telecomMgr.acceptRingingCall()
                        feedbackHelper?.vibrateClick(HapticIntensity.STRONG)
                        return true
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            val pointX = x.toInt()
            val pointY = y.toInt()
            val rect = Rect()

            val node = findActionableNodeAt(root, pointX, pointY, rect)
                ?: findNearbyActionableNode(root, pointX, pointY, 36)
                ?: return false

            if (node.isClickable || node.actionList.any { it.id == AccessibilityNodeInfo.ACTION_CLICK }) {
                if (node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    feedbackHelper?.vibrateClick(HapticIntensity.LIGHT)
                    return true
                }
            }
            if (node.isCheckable || node.actionList.any { it.id == AccessibilityNodeInfo.ACTION_SELECT }) {
                if (node.performAction(AccessibilityNodeInfo.ACTION_SELECT) || node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    feedbackHelper?.vibrateClick(HapticIntensity.LIGHT)
                    return true
                }
            }
            val parent = node.parent
            if (parent != null && (parent.isClickable || parent.actionList.any { it.id == AccessibilityNodeInfo.ACTION_CLICK })) {
                if (parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    feedbackHelper?.vibrateClick(HapticIntensity.LIGHT)
                    return true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    private fun tryClickFallbackNodeAt(x: Float, y: Float): Boolean {
        try {
            val root = rootInActiveWindow ?: return false
            val pkg = root.packageName?.toString()?.lowercase() ?: ""
            if (pkg == packageName.lowercase()) return false

            val pointX = x.toInt()
            val pointY = y.toInt()
            val rect = Rect()

            val node = findActionableNodeAt(root, pointX, pointY, rect)
                ?: findNearbyActionableNode(root, pointX, pointY, 28)
                ?: return false

            if (node.isClickable || node.actionList.any { it.id == AccessibilityNodeInfo.ACTION_CLICK }) {
                if (node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    feedbackHelper?.vibrateClick(HapticIntensity.LIGHT)
                    return true
                }
            }
            if (node.isCheckable || node.actionList.any { it.id == AccessibilityNodeInfo.ACTION_SELECT }) {
                if (node.performAction(AccessibilityNodeInfo.ACTION_SELECT) || node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    feedbackHelper?.vibrateClick(HapticIntensity.LIGHT)
                    return true
                }
            }
            val parent = node.parent
            if (parent != null && (parent.isClickable || parent.actionList.any { it.id == AccessibilityNodeInfo.ACTION_CLICK })) {
                if (parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    feedbackHelper?.vibrateClick(HapticIntensity.LIGHT)
                    return true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    private fun findActionableNodeAt(node: AccessibilityNodeInfo, x: Int, y: Int, rect: Rect): AccessibilityNodeInfo? {
        node.getBoundsInScreen(rect)
        if (!rect.contains(x, y)) return null

        for (i in (node.childCount - 1) downTo 0) {
            val child = node.getChild(i) ?: continue
            val found = findActionableNodeAt(child, x, y, rect)
            if (found != null) return found
        }

        if (node.isClickable || node.isCheckable || node.isEditable ||
            node.actionList.any { it.id == AccessibilityNodeInfo.ACTION_CLICK || it.id == AccessibilityNodeInfo.ACTION_SELECT }) {
            return node
        }
        return null
    }

    private fun findNearbyActionableNode(root: AccessibilityNodeInfo, x: Int, y: Int, slopPx: Int): AccessibilityNodeInfo? {
        val rect = Rect()
        val slopRect = Rect(x - slopPx, y - slopPx, x + slopPx, y + slopPx)

        fun search(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
            node.getBoundsInScreen(rect)
            if (Rect.intersects(rect, slopRect) &&
                (node.isClickable || node.isCheckable ||
                 node.actionList.any { it.id == AccessibilityNodeInfo.ACTION_CLICK || it.id == AccessibilityNodeInfo.ACTION_SELECT })) {
                return node
            }
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                val found = search(child)
                if (found != null) return found
            }
            return null
        }

        return search(root)
    }

    private fun getCandidateRoots(): List<AccessibilityNodeInfo> {
        val list = mutableListOf<AccessibilityNodeInfo>()
        try {
            rootInActiveWindow?.let { root ->
                val pkg = root.packageName?.toString()?.lowercase() ?: ""
                if (pkg != packageName.lowercase()) {
                    list.add(root)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val sorted = windows.sortedWith(
                    compareByDescending<AccessibilityWindowInfo> { it.isActive || it.isFocused }
                        .thenByDescending { it.layer }
                )
                for (win in sorted) {
                    val root = win.root ?: continue
                    val pkg = root.packageName?.toString()?.lowercase() ?: ""
                    if (pkg == packageName.lowercase()) continue
                    if (!list.contains(root)) {
                        list.add(root)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun findScrollableNodeAt(node: AccessibilityNodeInfo, x: Int, y: Int, rect: Rect): AccessibilityNodeInfo? {
        node.getBoundsInScreen(rect)
        if (!rect.contains(x, y)) return null

        for (i in (node.childCount - 1) downTo 0) {
            val child = node.getChild(i) ?: continue
            val found = findScrollableNodeAt(child, x, y, rect)
            if (found != null) return found
        }

        if (node.isScrollable ||
            node.actionList.any { it.id == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD || it.id == AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD }) {
            return node
        }
        return null
    }

    private fun tryScrollAccessibilityNodeAt(x: Float, y: Float, isScrollUp: Boolean): Boolean {
        val rect = Rect()
        val roots = getCandidateRoots()
        for (root in roots) {
            val scrollNode = findScrollableNodeAt(root, x.toInt(), y.toInt(), rect)
            if (scrollNode != null) {
                val action = if (isScrollUp) AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD else AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                if (scrollNode.performAction(action)) {
                    feedbackHelper?.vibrateClick(HapticIntensity.LIGHT)
                    return true
                }
            }
        }
        return false
    }

    fun startHoldDrag() {
        val cfg = preferencesManager.getConfig()
        if (!cfg.isEnabled) return
        val rawX = motionTracker?.cursorX ?: (realScreenW / 2f)
        val rawY = motionTracker?.cursorY ?: (realScreenH / 2f)

        isHoldDragging = true
        isDragging = true
        dragStartX = rawX
        dragStartY = rawY
        overlayManager?.setDragging(true, dragStartX, dragStartY, isPhysicalHold = true)
        feedbackHelper?.vibrateClick(HapticIntensity.STRONG)

        _dragStateFlow.value = DragStateInfo(
            isDragging = true,
            isHoldDrag = true,
            startX = dragStartX,
            startY = dragStartY,
            currentX = dragStartX,
            currentY = dragStartY,
            dropSuccessCount = _dragStateFlow.value.dropSuccessCount
        )
    }

    fun endHoldDrag() {
        if (!isHoldDragging) return
        isHoldDragging = false
        isDragging = false
        val rawX = motionTracker?.cursorX ?: (realScreenW / 2f)
        val rawY = motionTracker?.cursorY ?: (realScreenH / 2f)

        overlayManager?.setDragging(false)
        feedbackHelper?.vibrateClick(HapticIntensity.MEDIUM)

        val nextCount = _dragStateFlow.value.dropSuccessCount + 1
        _dragStateFlow.value = DragStateInfo(
            isDragging = false,
            isHoldDrag = false,
            startX = dragStartX,
            startY = dragStartY,
            currentX = rawX,
            currentY = rawY,
            lastDroppedX = rawX,
            lastDroppedY = rawY,
            dropTimestamp = System.currentTimeMillis(),
            dropSuccessCount = nextCount
        )

        executeDragAndDrop(dragStartX, dragStartY, rawX, rawY)
    }

    private fun toggleDragMode() {
        val cfg = preferencesManager.getConfig()
        val rawX = motionTracker?.cursorX ?: 500f
        val rawY = motionTracker?.cursorY ?: 1000f

        if (!isDragging) {
            isDragging = true
            isHoldDragging = false
            dragStartX = rawX
            dragStartY = rawY
            overlayManager?.setDragging(true, dragStartX, dragStartY, isPhysicalHold = false)
            feedbackHelper?.vibrateClick(HapticIntensity.STRONG)

            _dragStateFlow.value = DragStateInfo(
                isDragging = true,
                isHoldDrag = false,
                startX = dragStartX,
                startY = dragStartY,
                currentX = dragStartX,
                currentY = dragStartY,
                dropSuccessCount = _dragStateFlow.value.dropSuccessCount
            )
        } else {
            isDragging = false
            isHoldDragging = false
            val endX = rawX
            val endY = rawY
            overlayManager?.setDragging(false)
            feedbackHelper?.vibrateClick(HapticIntensity.MEDIUM)

            val nextCount = _dragStateFlow.value.dropSuccessCount + 1
            _dragStateFlow.value = DragStateInfo(
                isDragging = false,
                isHoldDrag = false,
                startX = dragStartX,
                startY = dragStartY,
                currentX = endX,
                currentY = endY,
                lastDroppedX = endX,
                lastDroppedY = endY,
                dropTimestamp = System.currentTimeMillis(),
                dropSuccessCount = nextCount
            )

            executeDragAndDrop(dragStartX, dragStartY, endX, endY)
        }
    }

    private fun executeDragAndDrop(startX: Float, startY: Float, endX: Float, endY: Float) {
        val cfg = preferencesManager.getConfig()
        val maxW = realScreenW.toFloat()
        val maxH = realScreenH.toFloat()
        val startTargetX = (startX + cfg.clickOffsetX).coerceIn(0f, maxW)
        val startTargetY = (startY + cfg.clickOffsetY).coerceIn(0f, maxH)
        val endTargetX = (endX + cfg.clickOffsetX).coerceIn(0f, maxW)
        val endTargetY = (endY + cfg.clickOffsetY).coerceIn(0f, maxH)

        val distance = kotlin.math.hypot(endTargetX - startTargetX, endTargetY - startTargetY)

        // Case 1: Stationary hold in place (distance < 18px) - user held and dropped without moving
        if (distance < 18f) {
            performLongClickAtCursor()
            return
        }

        // Case 2: Full Drag-and-Drop (Hold to pick up item -> Move smoothly to destination -> Drop)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val holdPath = Path().apply {
                moveTo(startTargetX, startTargetY)
                lineTo(startTargetX, startTargetY)
            }

            val movePath = Path().apply {
                moveTo(startTargetX, startTargetY)
                lineTo(endTargetX, endTargetY)
            }

            val holdStroke = GestureDescription.StrokeDescription(holdPath, 0, 650, true)
            val holdGesture = GestureDescription.Builder().addStroke(holdStroke).build()

            dispatchGesture(holdGesture, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    feedbackHelper?.vibrateClick(HapticIntensity.LIGHT)

                    val moveDuration = (distance * 1.1f).toLong().coerceIn(600L, 1100L)
                    val moveStroke = holdStroke.continueStroke(movePath, 0, moveDuration, false)
                    val moveGesture = GestureDescription.Builder().addStroke(moveStroke).build()
                    dispatchGesture(moveGesture, object : GestureResultCallback() {
                        override fun onCompleted(gestureDescription: GestureDescription?) {
                            feedbackHelper?.vibrateClick(HapticIntensity.STRONG)
                        }

                        override fun onCancelled(gestureDescription: GestureDescription?) {
                            feedbackHelper?.vibrateClick(HapticIntensity.LIGHT)
                        }
                    }, null)
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    feedbackHelper?.vibrateClick(HapticIntensity.LIGHT)
                }
            }, null)
        } else {
            val dragPath = Path().apply {
                moveTo(startTargetX, startTargetY)
                lineTo(endTargetX, endTargetY)
            }
            val stroke = GestureDescription.StrokeDescription(dragPath, 0, 1100)
            val gesture = GestureDescription.Builder().addStroke(stroke).build()
            dispatchGesture(gesture, null, null)
        }
    }

    private var isSwiping = false
    private var swipeStartX = 0f
    private var swipeStartY = 0f

    private fun toggleSwipeMode() {
        val cfg = preferencesManager.getConfig()
        val rawX = motionTracker?.cursorX ?: 500f
        val rawY = motionTracker?.cursorY ?: 1000f

        if (!isSwiping) {
            isSwiping = true
            swipeStartX = rawX
            swipeStartY = rawY
            overlayManager?.setDragging(true, swipeStartX, swipeStartY)
            feedbackHelper?.vibrateClick(HapticIntensity.LIGHT)
        } else {
            isSwiping = false
            val endX = rawX
            val endY = rawY
            overlayManager?.setDragging(false)
            feedbackHelper?.vibrateClick(HapticIntensity.MEDIUM)

            val maxW = resources.displayMetrics.widthPixels.toFloat()
            val maxH = resources.displayMetrics.heightPixels.toFloat()
            val startTargetX = (swipeStartX + cfg.clickOffsetX).coerceIn(0f, maxW)
            val startTargetY = (swipeStartY + cfg.clickOffsetY).coerceIn(0f, maxH)
            val endTargetX = (endX + cfg.clickOffsetX).coerceIn(0f, maxW)
            val endTargetY = (endY + cfg.clickOffsetY).coerceIn(0f, maxH)

            // Fast Swipe / Flick: immediate stroke without long-press pickup delay
            val swipePath = Path().apply {
                moveTo(startTargetX, startTargetY)
                lineTo(endTargetX, endTargetY)
            }
            val stroke = GestureDescription.StrokeDescription(swipePath, 0, 160)
            val gesture = GestureDescription.Builder().addStroke(stroke).build()
            dispatchGesture(gesture, null, null)
        }
    }

    private var isScrollGestureActive = false

    private fun dispatchScrollGesture(x: Float, y: Float, isScrollUp: Boolean) {
        if (isScrollGestureActive) return

        val cfg = preferencesManager.getConfig()
        val scrollDistance = cfg.scrollStepDistancePx.toFloat().coerceIn(100f, 1500f)
        val path = Path()

        if (isScrollUp) {
            val startY = (y - 180f).coerceAtLeast(100f)
            val endY = (startY + scrollDistance)
            path.moveTo(x, startY)
            path.lineTo(x, endY)
        } else {
            val startY = (y + 180f)
            val endY = (startY - scrollDistance).coerceAtLeast(100f)
            path.moveTo(x, startY)
            path.lineTo(x, endY)
        }

        val strokeDuration = (cfg.continuousScrollSpeedMs / 2).coerceIn(40L, 120L)
        val stroke = GestureDescription.StrokeDescription(path, 0, strokeDuration)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        isScrollGestureActive = true
        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                isScrollGestureActive = false
            }
            override fun onCancelled(gestureDescription: GestureDescription?) {
                isScrollGestureActive = false
                tryScrollAccessibilityNodeAt(x, y, isScrollUp)
            }
        }, null)
    }

    private fun dispatchHorizontalScrollGesture(x: Float, y: Float, isScrollLeft: Boolean) {
        if (isScrollGestureActive) return
        val cfg = preferencesManager.getConfig()
        val scrollDistance = (cfg.scrollStepDistancePx * 0.8f).coerceIn(100f, 1500f)
        val path = Path()

        if (isScrollLeft) {
            val startX = (x - 120f).coerceAtLeast(50f)
            val endX = startX + scrollDistance
            path.moveTo(startX, y)
            path.lineTo(endX, y)
        } else {
            val startX = (x + 120f)
            val endX = (startX - scrollDistance).coerceAtLeast(50f)
            path.moveTo(startX, y)
            path.lineTo(endX, y)
        }

        val strokeDuration = (cfg.continuousScrollSpeedMs / 2).coerceIn(40L, 120L)
        val stroke = GestureDescription.StrokeDescription(path, 0, strokeDuration)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        isScrollGestureActive = true
        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                isScrollGestureActive = false
            }
            override fun onCancelled(gestureDescription: GestureDescription?) {
                isScrollGestureActive = false
            }
        }, null)
    }

    @SuppressLint("MissingPermission")
    private fun handlePhoneCall() {
        var handled = false
        val audioMgr = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val telecomManager = getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        val telephonyMgr = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

        val isRinging = audioMgr?.mode == AudioManager.MODE_RINGTONE ||
                audioMgr?.mode == AudioManager.MODE_IN_COMMUNICATION ||
                audioMgr?.mode == AudioManager.MODE_IN_CALL

        val candidateRoots = mutableListOf<AccessibilityNodeInfo>()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                for (win in windows.reversed()) {
                    win.root?.let { candidateRoots.add(it) }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (candidateRoots.isEmpty()) {
            rootInActiveWindow?.let { candidateRoots.add(it) }
        }

        // Check if any visible window or node is an incoming call UI (Google Dialer, Samsung InCallUI, WhatsApp, etc.)
        var hasIncomingCallUI = false
        val callDialerPackages = listOf("dialer", "incallui", "telecom", "phone", "whatsapp", "telegram")
        for (root in candidateRoots) {
            val pkg = root.packageName?.toString()?.lowercase() ?: ""
            if (callDialerPackages.any { pkg.contains(it) }) {
                hasIncomingCallUI = true
                break
            }
        }

        // If the phone is NOT ringing and NO incoming call UI is on screen,
        // DO NOT dispatch media key (which would erroneously pause/play music in Spotify)!
        if (!isRinging && !hasIncomingCallUI) {
            feedbackHelper?.vibratePulse(HapticIntensity.LIGHT)
            return
        }

        // 1. TelecomManager API (cleanest system-level answer on Android 8+)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && telecomManager != null) {
                telecomManager.acceptRingingCall()
                handled = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Search all window nodes for "Answer", "Accept", "Pick up" buttons and click them directly
        if (!handled) {
            try {
                val searchKeywords = listOf("answer", "accept", "call", "pick up", "swipe to answer", "swipe up", "talk")
                val searchIds = listOf("answer", "accept", "btn_answer", "call_answer", "action_answer")
                val clickRect = Rect()

                for (root in candidateRoots) {
                    if (handled) break
                    for (term in searchKeywords) {
                        val nodes = root.findAccessibilityNodeInfosByText(term)
                        for (node in nodes) {
                            if (node.isClickable) {
                                if (node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                                    handled = true
                                    break
                                }
                            } else if (node.parent?.isClickable == true) {
                                if (node.parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                                    handled = true
                                    break
                                }
                            } else {
                                // Node not marked clickable; tap its center coordinate via touch gesture
                                node.getBoundsInScreen(clickRect)
                                if (clickRect.width() > 0 && clickRect.height() > 0) {
                                    val clickPath = Path().apply {
                                        moveTo(clickRect.exactCenterX(), clickRect.exactCenterY())
                                        lineTo(clickRect.exactCenterX(), clickRect.exactCenterY())
                                    }
                                    dispatchGesture(GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(clickPath, 0, 40)).build(), null, null)
                                    handled = true
                                    break
                                }
                            }
                        }
                        if (handled) break
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 3. Fallback: Swipe gestures for lockscreen / heads-up incoming call dialogs
        if (!handled) {
            try {
                val maxW = realScreenW.toFloat()
                val maxH = realScreenH.toFloat()

                // Swipe Up gesture (Pixel / standard Android)
                val swipeUpPath = Path().apply {
                    moveTo(maxW / 2f, maxH * 0.78f)
                    lineTo(maxW / 2f, maxH * 0.35f)
                }
                dispatchGesture(GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(swipeUpPath, 0, 250)).build(), null, null)

                // Swipe Right gesture (Samsung One UI / EMUI)
                mainHandler.postDelayed({
                    val swipeRightPath = Path().apply {
                        moveTo(maxW * 0.22f, maxH * 0.72f)
                        lineTo(maxW * 0.78f, maxH * 0.72f)
                    }
                    dispatchGesture(GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(swipeRightPath, 0, 250)).build(), null, null)
                }, 100L)
                handled = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 4. Fallback: If still ringing, dispatch headset hook media key
        if (isRinging) {
            try {
                audioMgr?.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_HEADSETHOOK))
                audioMgr?.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_HEADSETHOOK))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        feedbackHelper?.vibrateClick(HapticIntensity.MEDIUM)
    }

    fun launchAppPackage(pkg: String) {
        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(launchIntent)
            } else {
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    setPackage(pkg)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun launchPhoneDialer() {
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun launchWebBrowser() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun launchSettings() {
        try {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun launchMessages() {
        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_APP_MESSAGING)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun launchAccessibilitySettings() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun launchVoiceTyping() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Axecon Broken-Screen Dictation: Speak now")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun highlightIndicator(index: Int) {
        overlayManager?.highlightSideIndicator(index)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {
        motionTracker?.stop()
        overlayManager?.detach()
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        _isServiceActive.value = false

        try {
            unregisterReceiver(screenReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        mainHandler.removeCallbacksAndMessages(null)
        motionTracker?.stop()
        overlayManager?.detach()
        keyGestureProcessor?.reset()
        feedbackHelper?.release()
    }

    companion object {
        const val CHANNEL_ID = "axecon_service_channel"
        const val NOTIFICATION_ID = 1001

        var instance: AirCursorAccessibilityService? = null
            private set

        private val _isServiceActive = MutableStateFlow(false)
        val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

        private val _telemetryFlow = MutableStateFlow(SensorTelemetry())
        val telemetryFlow: StateFlow<SensorTelemetry> = _telemetryFlow.asStateFlow()

        private val _lastTriggeredAction = MutableStateFlow<String?>(null)
        val lastTriggeredAction: StateFlow<String?> = _lastTriggeredAction.asStateFlow()

        private val _dragStateFlow = MutableStateFlow(DragStateInfo())
        val dragStateFlow: StateFlow<DragStateInfo> = _dragStateFlow.asStateFlow()

        var sequenceRecordingListener: ((KeyStep) -> Unit)? = null
        var isRecordingSequence: Boolean = false

        fun startRecordingSequence(listener: (KeyStep) -> Unit) {
            isRecordingSequence = true
            sequenceRecordingListener = listener
            instance?.keyGestureProcessor?.reset()
        }

        fun stopRecordingSequence() {
            isRecordingSequence = false
            sequenceRecordingListener = null
            instance?.keyGestureProcessor?.reset()
        }

        fun calibrateNeutral() {
            instance?.motionTracker?.recenter()
        }

        fun togglePrecision(): Boolean {
            return instance?.motionTracker?.togglePrecisionMode() ?: false
        }

        fun toggleTracking(): Boolean {
            return instance?.motionTracker?.toggleTracking() ?: false
        }

        fun triggerActionFromApp(action: CursorAction) {
            instance?.executeAction(action)
        }

        fun simulateStartHoldDrag() {
            instance?.startHoldDrag()
        }

        fun simulateEndHoldDrag() {
            instance?.endHoldDrag()
        }

        fun highlightIndicator(index: Int) {
            instance?.overlayManager?.highlightSideIndicator(index)
        }

        fun simulateKeyStep(step: KeyStep) {
            val idx = when (step) {
                KeyStep.VOLUME_UP -> 0
                KeyStep.VOLUME_DOWN -> 1
                else -> 2
            }
            instance?.overlayManager?.highlightSideIndicator(idx)
            val cfg = instance?.preferencesManager?.getConfig() ?: CursorConfig()
            val timeout = cfg.comboTimeoutMs
            instance?.overlayManager?.updateSequenceCombo(listOf(step), null, timeout)
        }

        fun restartEngine(context: Context) {
            instance?.rebootAndReload()
        }
    }
}

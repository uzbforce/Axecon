package com.axecon.uzbforce.service

import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import com.axecon.uzbforce.model.ButtonTrigger
import com.axecon.uzbforce.model.CursorAction
import com.axecon.uzbforce.model.CustomKeySequence
import com.axecon.uzbforce.model.KeyStep

class KeyGestureProcessor(
    private val getMappings: () -> Map<ButtonTrigger, CursorAction>,
    private val getCustomSequences: () -> List<CustomKeySequence> = { emptyList() },
    private val getDoubleTapWindowMs: () -> Long = { 280L },
    private val getLongPressWindowMs: () -> Long = { 420L },
    private val getComboTimeoutMs: () -> Long = { 1000L },
    private val onActionTriggered: (action: CursorAction, trigger: ButtonTrigger) -> Unit,
    private val onSequenceMatched: (sequence: CustomKeySequence) -> Unit = {},
    private val onSequenceStepAdded: (steps: List<KeyStep>, matchedSeq: CustomKeySequence?, timeoutMs: Long) -> Unit = { _, _, _ -> },
    private val onSequenceCleared: () -> Unit = {},
    private val onKeyVisualized: (lineIndex: Int) -> Unit = {},
    private val onContinuousScrollTick: (direction: ScrollDirection) -> Unit = {},
    private val getContinuousScrollIntervalMs: () -> Long = { 160L },
    private val onHoldActionReleased: (action: CursorAction, trigger: ButtonTrigger) -> Unit = { _, _ -> }
) {

    enum class ScrollDirection { UP, DOWN, LEFT, RIGHT }

    private val mainHandler = Handler(Looper.getMainLooper())

    private var isVolUpPressed = false
    private var isVolDownPressed = false
    private var isHeadsetPressed = false

    // Timing state for Vol Up
    private var volUpPressTime = 0L
    private var volUpTapCount = 0
    private var volUpLongPressHandled = false
    private var volUpDoubleHoldActive = false
    private var lastVolUpActionDownTime = 0L

    // Timing state for Vol Down
    private var volDownPressTime = 0L
    private var volDownTapCount = 0
    private var volDownLongPressHandled = false
    private var volDownDoubleHoldActive = false
    private var lastVolDownActionDownTime = 0L

    // Timing state for Headset
    private var headsetPressTime = 0L
    private var headsetTapCount = 0
    private var headsetLongPressHandled = false
    private var lastHeadsetActionDownTime = 0L

    // Rolling sequence history buffer
    private val activeSequence = mutableListOf<KeyStep>()
    private val sequenceTimeoutRunnable = Runnable {
        activeSequence.clear()
        onSequenceCleared()
    }

    // Dual volume button states
    private var isBothVolActive = false
    private var bothVolLongPressHandled = false
    private val bothVolLongPressRunnable = Runnable {
        if (isVolUpPressed && isVolDownPressed) {
            bothVolLongPressHandled = true
            triggerAction(ButtonTrigger.BOTH_VOL_LONG_HOLD)
        }
    }
    private var activeScrollDirection: ScrollDirection? = null
    private val scrollRunnable = object : Runnable {
        override fun run() {
            val dir = activeScrollDirection
            if (dir != null && (isVolUpPressed || isVolDownPressed || isHeadsetPressed)) {
                onContinuousScrollTick(dir)
                val delay = getContinuousScrollIntervalMs().coerceIn(40L, 500L)
                mainHandler.postDelayed(this, delay)
            } else {
                stopContinuousScroll()
            }
        }
    }

    // Pending tap timeouts
    private val volUpSingleTapRunnable = Runnable {
        if (volUpTapCount == 1 && !isVolUpPressed) {
            triggerAction(ButtonTrigger.VOL_UP_SINGLE)
            volUpTapCount = 0
        }
    }

    private val volDownSingleTapRunnable = Runnable {
        if (volDownTapCount == 1 && !isVolDownPressed) {
            triggerAction(ButtonTrigger.VOL_DOWN_SINGLE)
            volDownTapCount = 0
        }
    }

    private val headsetSingleTapRunnable = Runnable {
        if (headsetTapCount == 1 && !isHeadsetPressed) {
            triggerAction(ButtonTrigger.HEADSET_HOOK_SINGLE)
            headsetTapCount = 0
        }
    }

    // Long press runnables
    private val volUpLongPressRunnable = Runnable {
        if (isVolUpPressed) {
            volUpLongPressHandled = true
            if (volUpTapCount == 2) {
                volUpDoubleHoldActive = true
                triggerAction(ButtonTrigger.VOL_UP_DOUBLE_HOLD)
            } else {
                triggerAction(ButtonTrigger.VOL_UP_LONG)
            }
        }
    }

    private val volDownLongPressRunnable = Runnable {
        if (isVolDownPressed) {
            volDownLongPressHandled = true
            if (volDownTapCount == 2) {
                volDownDoubleHoldActive = true
                triggerAction(ButtonTrigger.VOL_DOWN_DOUBLE_HOLD)
            } else {
                triggerAction(ButtonTrigger.VOL_DOWN_LONG)
            }
        }
    }

    private val headsetLongPressRunnable = Runnable {
        if (isHeadsetPressed) {
            headsetLongPressHandled = true
            triggerAction(ButtonTrigger.HEADSET_HOOK_LONG)
        }
    }

    private fun checkCustomSequence(step: KeyStep): Boolean {
        mainHandler.removeCallbacks(sequenceTimeoutRunnable)
        activeSequence.add(step)
        // Keep max 10 steps
        while (activeSequence.size > 10) {
            activeSequence.removeAt(0)
        }

        val timeoutMs = getComboTimeoutMs().coerceAtLeast(300L)
        val sequences = getCustomSequences().filter { it.isEnabled && it.steps.isNotEmpty() }
        for (seq in sequences) {
            if (seq.steps.size <= activeSequence.size) {
                val sub = activeSequence.takeLast(seq.steps.size)
                if (sub == seq.steps) {
                    // Matched!
                    cancelAllPending()
                    val matchedSteps = activeSequence.toList()
                    onSequenceStepAdded(matchedSteps, seq, timeoutMs)
                    mainHandler.postDelayed({
                        activeSequence.clear()
                        onSequenceCleared()
                    }, 900L)
                    onSequenceMatched(seq)
                    return true
                }
            }
        }

        // Notify step added with timeout
        onSequenceStepAdded(activeSequence.toList(), null, timeoutMs)

        // Auto-clear sequence after timeoutMs of inactivity
        mainHandler.postDelayed(sequenceTimeoutRunnable, timeoutMs)
        return false
    }

    fun onKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        val action = event.action

        if (keyCode != KeyEvent.KEYCODE_VOLUME_UP &&
            keyCode != KeyEvent.KEYCODE_VOLUME_DOWN &&
            keyCode != KeyEvent.KEYCODE_HEADSETHOOK &&
            keyCode != KeyEvent.KEYCODE_POWER &&
            keyCode != KeyEvent.KEYCODE_BACK &&
            keyCode != KeyEvent.KEYCODE_CAMERA
        ) {
            return false
        }

        // Map key step
        val step = when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> KeyStep.VOLUME_UP
            KeyEvent.KEYCODE_VOLUME_DOWN -> KeyStep.VOLUME_DOWN
            KeyEvent.KEYCODE_HEADSETHOOK -> KeyStep.HEADSET_HOOK
            KeyEvent.KEYCODE_POWER -> KeyStep.POWER
            KeyEvent.KEYCODE_BACK -> KeyStep.BACK
            else -> KeyStep.VOLUME_UP
        }

        if (action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            // Visualize side line (0 = Vol Up, 1 = Vol Down, 2 = Power/Other)
            when (step) {
                KeyStep.VOLUME_UP -> onKeyVisualized(0)
                KeyStep.VOLUME_DOWN -> onKeyVisualized(1)
                KeyStep.HEADSET_HOOK, KeyStep.POWER, KeyStep.BACK -> onKeyVisualized(2)
            }

            // Check custom combination sequence first
            val sequenceMatched = checkCustomSequence(step)
            if (sequenceMatched) {
                return true
            }
        }

        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                if (action == KeyEvent.ACTION_DOWN) {
                    if (event.repeatCount == 0) {
                        val now = System.currentTimeMillis()
                        if (now - lastVolUpActionDownTime < 70L) {
                            // Suppress mechanical contact bounce
                            return true
                        }
                        lastVolUpActionDownTime = now

                        isVolUpPressed = true
                        volUpPressTime = now
                        volUpLongPressHandled = false

                        // Check dual button combo (Vol Up + Vol Down)
                        if (isVolDownPressed) {
                            cancelAllPending()
                            stopContinuousScroll()
                            isBothVolActive = true
                            bothVolLongPressHandled = false
                            val longPressMs = getLongPressWindowMs()
                            mainHandler.postDelayed(bothVolLongPressRunnable, longPressMs)
                            return true
                        }

                        mainHandler.removeCallbacks(volUpSingleTapRunnable)
                        volUpTapCount++

                        val longPressMs = getLongPressWindowMs()
                        mainHandler.postDelayed(volUpLongPressRunnable, longPressMs)
                    }
                    return true
                } else if (action == KeyEvent.ACTION_UP) {
                    isVolUpPressed = false
                    mainHandler.removeCallbacks(volUpLongPressRunnable)
                    stopContinuousScroll()

                    if (volUpDoubleHoldActive) {
                        volUpDoubleHoldActive = false
                        volUpTapCount = 0
                        return true
                    }

                    if (isBothVolActive) {
                        mainHandler.removeCallbacks(bothVolLongPressRunnable)
                        if (!bothVolLongPressHandled) {
                            triggerAction(ButtonTrigger.BOTH_VOL_PRESS)
                        } else {
                            triggerHoldRelease(ButtonTrigger.BOTH_VOL_LONG_HOLD)
                        }
                        isBothVolActive = false
                        volUpTapCount = 0
                        volDownTapCount = 0
                        return true
                    }

                    if (!volUpLongPressHandled) {
                        if (volUpTapCount == 1) {
                            val mappings = getMappings()
                            val doubleAction = mappings[ButtonTrigger.VOL_UP_DOUBLE]
                            if (doubleAction == null || doubleAction == CursorAction.NONE) {
                                // No double-tap mapped: fire single tap immediately with zero delay!
                                triggerAction(ButtonTrigger.VOL_UP_SINGLE)
                                volUpTapCount = 0
                            } else {
                                val doubleTapMs = getDoubleTapWindowMs().coerceIn(120L, 210L)
                                mainHandler.postDelayed(volUpSingleTapRunnable, doubleTapMs)
                            }
                        } else if (volUpTapCount >= 2) {
                            mainHandler.removeCallbacks(volUpSingleTapRunnable)
                            triggerAction(ButtonTrigger.VOL_UP_DOUBLE)
                            volUpTapCount = 0
                        }
                    } else {
                        triggerHoldRelease(ButtonTrigger.VOL_UP_LONG)
                        volUpLongPressHandled = false
                        volUpTapCount = 0
                    }
                    return true
                }
            }

            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (action == KeyEvent.ACTION_DOWN) {
                    if (event.repeatCount == 0) {
                        val now = System.currentTimeMillis()
                        if (now - lastVolDownActionDownTime < 70L) {
                            // Suppress mechanical contact bounce
                            return true
                        }
                        lastVolDownActionDownTime = now

                        isVolDownPressed = true
                        volDownPressTime = now
                        volDownLongPressHandled = false

                        // Check dual button combo (Vol Up + Vol Down)
                        if (isVolUpPressed) {
                            cancelAllPending()
                            stopContinuousScroll()
                            isBothVolActive = true
                            bothVolLongPressHandled = false
                            val longPressMs = getLongPressWindowMs()
                            mainHandler.postDelayed(bothVolLongPressRunnable, longPressMs)
                            return true
                        }

                        mainHandler.removeCallbacks(volDownSingleTapRunnable)
                        volDownTapCount++

                        val longPressMs = getLongPressWindowMs()
                        mainHandler.postDelayed(volDownLongPressRunnable, longPressMs)
                    }
                    return true
                } else if (action == KeyEvent.ACTION_UP) {
                    isVolDownPressed = false
                    mainHandler.removeCallbacks(volDownLongPressRunnable)
                    stopContinuousScroll()

                    if (volDownDoubleHoldActive) {
                        volDownDoubleHoldActive = false
                        volDownTapCount = 0
                        return true
                    }

                    if (isBothVolActive) {
                        mainHandler.removeCallbacks(bothVolLongPressRunnable)
                        if (!bothVolLongPressHandled) {
                            triggerAction(ButtonTrigger.BOTH_VOL_PRESS)
                        } else {
                            triggerHoldRelease(ButtonTrigger.BOTH_VOL_LONG_HOLD)
                        }
                        isBothVolActive = false
                        volDownTapCount = 0
                        volUpTapCount = 0
                        return true
                    }

                    if (!volDownLongPressHandled) {
                        if (volDownTapCount == 1) {
                            val mappings = getMappings()
                            val doubleAction = mappings[ButtonTrigger.VOL_DOWN_DOUBLE]
                            if (doubleAction == null || doubleAction == CursorAction.NONE) {
                                // No double-tap mapped: fire single tap immediately with zero delay!
                                triggerAction(ButtonTrigger.VOL_DOWN_SINGLE)
                                volDownTapCount = 0
                            } else {
                                val doubleTapMs = getDoubleTapWindowMs().coerceIn(120L, 210L)
                                mainHandler.postDelayed(volDownSingleTapRunnable, doubleTapMs)
                            }
                        } else if (volDownTapCount >= 2) {
                            mainHandler.removeCallbacks(volDownSingleTapRunnable)
                            triggerAction(ButtonTrigger.VOL_DOWN_DOUBLE)
                            volDownTapCount = 0
                        }
                    } else {
                        triggerHoldRelease(ButtonTrigger.VOL_DOWN_LONG)
                        volDownLongPressHandled = false
                        volDownTapCount = 0
                    }
                    return true
                }
            }

            KeyEvent.KEYCODE_HEADSETHOOK -> {
                if (action == KeyEvent.ACTION_DOWN) {
                    if (event.repeatCount == 0) {
                        val now = System.currentTimeMillis()
                        if (now - lastHeadsetActionDownTime < 70L) {
                            // Suppress mechanical contact bounce
                            return true
                        }
                        lastHeadsetActionDownTime = now

                        isHeadsetPressed = true
                        headsetPressTime = now
                        headsetLongPressHandled = false

                        mainHandler.removeCallbacks(headsetSingleTapRunnable)
                        headsetTapCount++

                        val longPressMs = getLongPressWindowMs()
                        mainHandler.postDelayed(headsetLongPressRunnable, longPressMs)
                    }
                    return true
                } else if (action == KeyEvent.ACTION_UP) {
                    isHeadsetPressed = false
                    mainHandler.removeCallbacks(headsetLongPressRunnable)
                    stopContinuousScroll()

                    if (!headsetLongPressHandled) {
                        if (headsetTapCount == 1) {
                            val doubleTapMs = getDoubleTapWindowMs()
                            mainHandler.postDelayed(headsetSingleTapRunnable, doubleTapMs)
                        } else if (headsetTapCount >= 2) {
                            mainHandler.removeCallbacks(headsetSingleTapRunnable)
                            triggerAction(ButtonTrigger.HEADSET_HOOK_DOUBLE)
                            headsetTapCount = 0
                        }
                    } else {
                        triggerHoldRelease(ButtonTrigger.HEADSET_HOOK_LONG)
                        headsetLongPressHandled = false
                        headsetTapCount = 0
                    }
                    return true
                }
            }
            KeyEvent.KEYCODE_POWER, KeyEvent.KEYCODE_BACK -> {
                // If consumed by a custom sequence, it already triggered above
                return false
            }
        }

        return false
    }

    private fun cancelAllPending() {
        mainHandler.removeCallbacks(volUpSingleTapRunnable)
        mainHandler.removeCallbacks(volDownSingleTapRunnable)
        mainHandler.removeCallbacks(headsetSingleTapRunnable)
        mainHandler.removeCallbacks(volUpLongPressRunnable)
        mainHandler.removeCallbacks(volDownLongPressRunnable)
        mainHandler.removeCallbacks(headsetLongPressRunnable)
        mainHandler.removeCallbacks(bothVolLongPressRunnable)
        volUpTapCount = 0
        volDownTapCount = 0
        headsetTapCount = 0
        isBothVolActive = false
        bothVolLongPressHandled = false
    }

    fun startContinuousScroll(direction: ScrollDirection) {
        stopContinuousScroll()
        activeScrollDirection = direction
        mainHandler.post(scrollRunnable)
    }

    fun stopContinuousScroll() {
        activeScrollDirection = null
        mainHandler.removeCallbacks(scrollRunnable)
    }

    private fun triggerAction(trigger: ButtonTrigger) {
        val mappings = getMappings()
        val action = mappings[trigger] ?: CursorAction.NONE
        if (action == CursorAction.CONTINUOUS_SCROLL_UP) {
            startContinuousScroll(ScrollDirection.UP)
        } else if (action == CursorAction.CONTINUOUS_SCROLL_DOWN) {
            startContinuousScroll(ScrollDirection.DOWN)
        }
        onActionTriggered(action, trigger)
    }

    private fun triggerHoldRelease(trigger: ButtonTrigger) {
        val mappings = getMappings()
        val action = mappings[trigger] ?: CursorAction.NONE
        onHoldActionReleased(action, trigger)
    }

    fun reset() {
        cancelAllPending()
        stopContinuousScroll()
        activeSequence.clear()
        isVolUpPressed = false
        isVolDownPressed = false
        isHeadsetPressed = false
        volUpDoubleHoldActive = false
        volDownDoubleHoldActive = false
    }
}

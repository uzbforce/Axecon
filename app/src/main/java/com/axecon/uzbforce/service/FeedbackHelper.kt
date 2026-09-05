package com.axecon.uzbforce.service

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.axecon.uzbforce.model.CursorAction
import com.axecon.uzbforce.model.CursorConfig
import com.axecon.uzbforce.model.FeedbackSoundType
import com.axecon.uzbforce.model.HapticIntensity

class FeedbackHelper(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vm?.defaultVibrator ?: context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private var toneGenerator: ToneGenerator? = try {
        ToneGenerator(AudioManager.STREAM_MUSIC, 70)
    } catch (e: Exception) {
        null
    }

    fun playFeedbackForAction(action: CursorAction, config: CursorConfig) {
        // Evaluate vibration
        if (config.hapticFeedback) {
            val shouldVibrate = when (action) {
                CursorAction.CLICK, CursorAction.DOUBLE_CLICK, CursorAction.DRAG_TOGGLE, CursorAction.HOLD_TO_DRAG -> config.hapticOnClick
                CursorAction.LONG_CLICK -> config.hapticOnLongClick
                CursorAction.SCROLL_UP, CursorAction.SCROLL_DOWN, CursorAction.SCROLL_LEFT,
                CursorAction.SCROLL_RIGHT, CursorAction.CONTINUOUS_SCROLL_UP, CursorAction.CONTINUOUS_SCROLL_DOWN -> config.hapticOnScroll
                CursorAction.OPEN_ACTION_MENU -> config.hapticOnActionMenu
                CursorAction.RECENTER_CALIBRATE, CursorAction.TOGGLE_PRECISION_MODE, CursorAction.TOGGLE_TRACKING -> config.hapticOnRecenter
                else -> true
            }

            if (shouldVibrate) {
                when (action) {
                    CursorAction.LONG_CLICK -> vibrateLong(config.hapticIntensity)
                    CursorAction.DOUBLE_CLICK -> vibrateDouble(config.hapticIntensity)
                    CursorAction.RECENTER_CALIBRATE, CursorAction.OPEN_ACTION_MENU -> vibratePulse(config.hapticIntensity)
                    else -> vibrateClick(config.hapticIntensity)
                }
            }
        }

        // Evaluate audio sound
        if (config.audioFeedback && config.soundType != FeedbackSoundType.NONE) {
            val shouldPlayAudio = when (action) {
                CursorAction.CLICK, CursorAction.DOUBLE_CLICK, CursorAction.DRAG_TOGGLE, CursorAction.HOLD_TO_DRAG -> config.audioOnClick
                CursorAction.LONG_CLICK -> config.audioOnLongClick
                CursorAction.SCROLL_UP, CursorAction.SCROLL_DOWN, CursorAction.SCROLL_LEFT,
                CursorAction.SCROLL_RIGHT, CursorAction.CONTINUOUS_SCROLL_UP, CursorAction.CONTINUOUS_SCROLL_DOWN -> config.audioOnScroll
                CursorAction.OPEN_ACTION_MENU -> config.audioOnActionMenu
                CursorAction.RECENTER_CALIBRATE, CursorAction.TOGGLE_PRECISION_MODE, CursorAction.TOGGLE_TRACKING -> config.audioOnRecenter
                else -> true
            }

            if (shouldPlayAudio) {
                playSound(config.soundType)
            }
        }
    }

    fun vibrateClick(intensity: HapticIntensity = HapticIntensity.MEDIUM) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(intensity.strengthMillis, intensity.amplitude))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(intensity.strengthMillis)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun vibrateDouble(intensity: HapticIntensity = HapticIntensity.MEDIUM) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, intensity.strengthMillis, 50, intensity.strengthMillis)
                val amps = intArrayOf(0, intensity.amplitude, 0, intensity.amplitude)
                vibrator?.vibrate(VibrationEffect.createWaveform(timings, amps, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 30, 50, 30), -1)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun vibrateLong(intensity: HapticIntensity = HapticIntensity.MEDIUM) {
        try {
            val duration = (intensity.strengthMillis * 2.5f).toLong().coerceAtLeast(60L)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(duration, intensity.amplitude))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(duration)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun vibratePulse(intensity: HapticIntensity = HapticIntensity.MEDIUM) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 20, 40, 40)
                val amps = intArrayOf(0, (intensity.amplitude * 0.7f).toInt(), 0, intensity.amplitude)
                vibrator?.vibrate(VibrationEffect.createWaveform(timings, amps, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(70)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playSound(soundType: FeedbackSoundType) {
        try {
            when (soundType) {
                FeedbackSoundType.SYSTEM_CLICK -> {
                    audioManager?.playSoundEffect(AudioManager.FX_KEY_CLICK, 0.8f)
                }
                FeedbackSoundType.POP -> {
                    audioManager?.playSoundEffect(AudioManager.FX_KEYPRESS_SPACEBAR, 1.0f)
                }
                FeedbackSoundType.BEEP -> {
                    toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 35)
                }
                FeedbackSoundType.TICK -> {
                    audioManager?.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, 0.9f)
                }
                FeedbackSoundType.CHIME -> {
                    toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 50)
                }
                FeedbackSoundType.NONE -> {}
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun release() {
        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

package com.axecon.uzbforce.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import com.axecon.uzbforce.model.CursorConfig
import com.axecon.uzbforce.model.SensorMode
import com.axecon.uzbforce.model.TrackingParadigm
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SensorTelemetry(
    val pitchDeg: Float = 0f,
    val rollDeg: Float = 0f,
    val gyroRateX: Float = 0f,
    val gyroRateY: Float = 0f,
    val deltaX: Float = 0f,
    val deltaY: Float = 0f,
    val cursorX: Float = 0f,
    val cursorY: Float = 0f,
    val fps: Int = 0,
    val isTrackingActive: Boolean = false,
    val isPrecisionActive: Boolean = false
)

class MotionSensorTracker(
    private val context: Context,
    private var config: CursorConfig,
    private var screenWidth: Int = 1080,
    private var screenHeight: Int = 2400,
    private val onCursorMoved: (x: Float, y: Float) -> Unit = { _, _ -> }
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val gyroSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val accelSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gravitySensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
    private val rotationVectorSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
        ?: sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private var isListening = false

    // Current cursor coordinates (pixels)
    var cursorX: Float = screenWidth / 2f
        private set
    var cursorY: Float = screenHeight / 2f
        private set

    // Filtered delta values
    private var filteredDeltaX = 0f
    private var filteredDeltaY = 0f

    // 3D Physical World Anchor matrix & state
    private val currentRotMatrix = FloatArray(9)
    private val anchorRotMatrix = FloatArray(9)
    private var hasAnchor = false

    // Neutral calibration offsets for tilt mode
    private var neutralPitch = 45f // default ~45 degree hold
    private var neutralRoll = 0f

    // Current orientation angles & vectors
    private var currentPitch = 45f
    private var currentRoll = 0f
    private var currentPitchRad = 0.785f
    private var currentRollRad = 0f

    // Gravity vector estimate
    private var gravX = 0f
    private var gravY = 6.9f
    private var gravZ = 6.9f

    // Current raw gyroscope rates (rad/s)
    private var currentGyroX = 0f
    private var currentGyroY = 0f
    private var currentGyroZ = 0f

    // Zero-drift tracking for gyro
    private var gyroBiasX = 0f
    private var gyroBiasY = 0f
    private var gyroBiasZ = 0f
    private var stationaryCount = 0

    // Time-delta tracking for rate-independent integration
    private var lastGyroTimestampNs: Long = 0L
    private var lastAccelTimestampNs: Long = 0L

    // FPS / update rate tracking
    private var frameCount = 0
    private var lastFpsTimestamp = SystemClock.elapsedRealtime()
    private var currentFps = 0

    // Telemetry flow
    private val _telemetry = MutableStateFlow(SensorTelemetry())
    val telemetry: StateFlow<SensorTelemetry> = _telemetry.asStateFlow()

    // Precision Sniper mode state
    var isPrecisionActive = false
    var isTrackingPaused = false

    init {
        recenter(screenWidth / 2f, screenHeight / 2f)
    }

    fun updateScreenDimensions(width: Int, height: Int) {
        if (width > 0 && height > 0) {
            this.screenWidth = width
            this.screenHeight = height
            clampCursor()
        }
    }

    fun updateConfig(newConfig: CursorConfig) {
        val needsSensorRestart = isListening && (newConfig.samplingRateHz != config.samplingRateHz || newConfig.sensorMode != config.sensorMode)
        this.config = newConfig
        if (needsSensorRestart) {
            stop()
            start()
        }
    }

    fun start() {
        if (isListening) return

        val sensorDelay = when (config.samplingRateHz) {
            20 -> SensorManager.SENSOR_DELAY_NORMAL // ~20Hz
            30 -> SensorManager.SENSOR_DELAY_UI // ~30-40Hz
            else -> SensorManager.SENSOR_DELAY_GAME // ~50-60Hz
        }

        lastGyroTimestampNs = 0L
        lastAccelTimestampNs = 0L

        when (config.sensorMode) {
            SensorMode.GYROSCOPE -> {
                // Register gyro, plus gravity/accel for orientation compensation
                gyroSensor?.let { sensorManager.registerListener(this, it, sensorDelay) }
                    ?: accelSensor?.let { sensorManager.registerListener(this, it, sensorDelay) }
                (gravitySensor ?: accelSensor)?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
            }
            SensorMode.ACCELEROMETER_TILT -> {
                (gravitySensor ?: accelSensor)?.let { sensorManager.registerListener(this, it, sensorDelay) }
            }
            SensorMode.FUSION -> {
                gyroSensor?.let { sensorManager.registerListener(this, it, sensorDelay) }
                (gravitySensor ?: accelSensor)?.let { sensorManager.registerListener(this, it, sensorDelay) }
            }
        }

        // Always listen to rotation vector for 3D Physical World Anchor tracking
        rotationVectorSensor?.let {
            sensorManager.registerListener(this, it, sensorDelay)
        }

        isListening = true
        _telemetry.value = _telemetry.value.copy(isTrackingActive = true)
    }

    fun stop() {
        if (!isListening) return
        sensorManager.unregisterListener(this)
        isListening = false
        hasAnchor = false
        filteredDeltaX = 0f
        filteredDeltaY = 0f
        lastGyroTimestampNs = 0L
        lastAccelTimestampNs = 0L
        _telemetry.value = _telemetry.value.copy(isTrackingActive = false)
    }

    fun calibrateNeutralHold() {
        neutralPitch = currentPitch
        neutralRoll = currentRoll
        hasAnchor = false
        filteredDeltaX = 0f
        filteredDeltaY = 0f
        gyroBiasX = currentGyroX
        gyroBiasY = currentGyroY
        gyroBiasZ = currentGyroZ
        stationaryCount = 0
    }

    fun recenter(x: Float = screenWidth / 2f, y: Float = screenHeight / 2f) {
        cursorX = x
        cursorY = y
        filteredDeltaX = 0f
        filteredDeltaY = 0f
        hasAnchor = false
        calibrateNeutralHold()
        clampCursor()
        onCursorMoved(cursorX, cursorY)
    }

    fun togglePrecisionMode(): Boolean {
        isPrecisionActive = !isPrecisionActive
        _telemetry.value = _telemetry.value.copy(isPrecisionActive = isPrecisionActive)
        return isPrecisionActive
    }

    fun toggleTracking(): Boolean {
        isTrackingPaused = !isTrackingPaused
        filteredDeltaX = 0f
        filteredDeltaY = 0f
        return isTrackingPaused
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        if (isTrackingPaused) return

        when (event.sensor.type) {
            Sensor.TYPE_GYROSCOPE -> {
                val nowNs = event.timestamp
                val dt = if (lastGyroTimestampNs != 0L) {
                    ((nowNs - lastGyroTimestampNs) / 1_000_000_000.0f).coerceIn(0.001f, 0.05f)
                } else {
                    1f / config.samplingRateHz.toFloat().coerceAtLeast(30f)
                }
                lastGyroTimestampNs = nowNs

                // Gyroscope outputs rad/sec around X, Y, Z axes
                currentGyroX = event.values[0]
                currentGyroY = event.values[1]
                currentGyroZ = event.values[2]

                // Dynamic zero-rate bias calibration when stationary
                updateGyroDriftBias(currentGyroX, currentGyroY, currentGyroZ)

                val correctedGx = currentGyroX - gyroBiasX
                val correctedGy = currentGyroY - gyroBiasY
                val correctedGz = currentGyroZ - gyroBiasZ

                when (config.sensorMode) {
                    SensorMode.GYROSCOPE -> {
                        processGyroMovement(correctedGx, correctedGy, correctedGz, dt)
                    }
                    SensorMode.FUSION -> {
                        processFusionMovement(correctedGx, correctedGy, correctedGz, dt)
                    }
                    SensorMode.ACCELEROMETER_TILT -> {
                        // In pure tilt mode, gyro events are not used for displacement
                    }
                }
            }

            Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_GRAVITY -> {
                val nowNs = event.timestamp
                val dt = if (lastAccelTimestampNs != 0L) {
                    ((nowNs - lastAccelTimestampNs) / 1_000_000_000.0f).coerceIn(0.001f, 0.05f)
                } else {
                    1f / config.samplingRateHz.toFloat().coerceAtLeast(30f)
                }
                lastAccelTimestampNs = nowNs

                val ax = event.values[0]
                val ay = event.values[1]
                val az = event.values[2]

                if (event.sensor.type == Sensor.TYPE_GRAVITY) {
                    gravX = ax
                    gravY = ay
                    gravZ = az
                } else {
                    // Low-pass filter accelerometer to isolate gravity
                    val alpha = 0.15f
                    gravX = gravX * (1f - alpha) + ax * alpha
                    gravY = gravY * (1f - alpha) + ay * alpha
                    gravZ = gravZ * (1f - alpha) + az * alpha
                }

                // Calculate tilt angles in radians and degrees
                // Roll: tilt left/right (ax)
                // Pitch: tilt forward/backward (ay vs az)
                currentRollRad = atan2(gravX.toDouble(), max(0.001, sqrt((gravY * gravY + gravZ * gravZ).toDouble()))).toFloat()
                currentPitchRad = atan2(gravY.toDouble(), max(0.001, gravZ.toDouble())).toFloat()

                currentRoll = Math.toDegrees(currentRollRad.toDouble()).toFloat()
                currentPitch = Math.toDegrees(currentPitchRad.toDouble()).toFloat()

                if (config.sensorMode == SensorMode.ACCELEROMETER_TILT) {
                    processTiltMovement(currentRoll, currentPitch, dt)
                }
            }

            Sensor.TYPE_ROTATION_VECTOR, Sensor.TYPE_GAME_ROTATION_VECTOR -> {
                val nowNs = event.timestamp
                val dt = if (lastAccelTimestampNs != 0L) {
                    ((nowNs - lastAccelTimestampNs) / 1_000_000_000.0f).coerceIn(0.001f, 0.05f)
                } else {
                    1f / config.samplingRateHz.toFloat().coerceAtLeast(30f)
                }
                lastAccelTimestampNs = nowNs
                if (config.trackingParadigm == TrackingParadigm.PHYSICAL_WORLD_ANCHOR) {
                    processWorldAnchorRotationVector(event.values, dt)
                }
            }
        }

        updateTelemetryFps()
    }

    private fun processWorldAnchorRotationVector(rotVector: FloatArray, dt: Float) {
        if (!hasAnchor) {
            try {
                SensorManager.getRotationMatrixFromVector(anchorRotMatrix, rotVector)
                hasAnchor = true
                cursorX = screenWidth / 2f
                cursorY = screenHeight / 2f
                filteredDeltaX = 0f
                filteredDeltaY = 0f
                onCursorMoved(cursorX, cursorY)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return
        }

        try {
            SensorManager.getRotationMatrixFromVector(currentRotMatrix, rotVector)

            // Calculate device-relative projection of the anchored 3D world ray (0, 0, 1)
            val vx = currentRotMatrix[0] * anchorRotMatrix[2] + currentRotMatrix[3] * anchorRotMatrix[5] + currentRotMatrix[6] * anchorRotMatrix[8]
            val vy = currentRotMatrix[1] * anchorRotMatrix[2] + currentRotMatrix[4] * anchorRotMatrix[5] + currentRotMatrix[7] * anchorRotMatrix[8]
            val vz = max(0.15f, currentRotMatrix[2] * anchorRotMatrix[2] + currentRotMatrix[5] * anchorRotMatrix[5] + currentRotMatrix[8] * anchorRotMatrix[8])

            val projX = vx / vz
            val projY = vy / vz

            // Base scale in pixels calibrated so ~18-25 degree tilt sweeps across the phone display
            val baseScale = screenWidth * 2.75f * config.worldAnchorSensitivity * (config.sensitivityX / 3.8f)

            var targetX = (screenWidth / 2f) + (projX * baseScale)
            var targetY = (screenHeight / 2f) - (projY * baseScale)

            if (config.invertX) targetX = screenWidth - targetX
            if (config.invertY) targetY = screenHeight - targetY

            // Precision Sniper Mode dampening
            if (isPrecisionActive) {
                val centerX = screenWidth / 2f
                val centerY = screenHeight / 2f
                targetX = centerX + (targetX - centerX) * config.precisionSpeedMultiplier
                targetY = centerY + (targetY - centerY) * config.precisionSpeedMultiplier
            }

            // Adaptive smoothing / micro-tremor filter
            val alpha = (1.0f - config.smoothingFactor).coerceIn(0.12f, 0.95f)
            cursorX = (cursorX * (1f - alpha)) + (targetX * alpha)
            cursorY = (cursorY * (1f - alpha)) + (targetY * alpha)

            clampCursor()
            onCursorMoved(cursorX, cursorY)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateGyroDriftBias(gx: Float, gy: Float, gz: Float) {
        val totalMagnitude = sqrt((gx * gx + gy * gy + gz * gz).toDouble()).toFloat()
        if (totalMagnitude < 0.025f) {
            stationaryCount++
            if (stationaryCount > 8) {
                // Gently blend towards zero-bias
                gyroBiasX = gyroBiasX * 0.95f + gx * 0.05f
                gyroBiasY = gyroBiasY * 0.95f + gy * 0.05f
                gyroBiasZ = gyroBiasZ * 0.95f + gz * 0.05f
            }
        } else {
            stationaryCount = 0
        }
    }

    /**
     * Mode 1: Pure Gyroscope Air Mouse Pointer
     * Uses pitch-compensated 3D rotational projection so horizontal sweeps work
     * effortlessly at any phone tilt angle (upright, 45-degree hold, or flat table).
     */
    private fun processGyroMovement(gx: Float, gy: Float, gz: Float, dt: Float) {
        if (config.trackingParadigm == TrackingParadigm.PHYSICAL_WORLD_ANCHOR && rotationVectorSensor != null) {
            return
        }

        // Compute effective pitch angle for trigonometric projection
        val effectivePitch = currentPitchRad.coerceIn(-1.45f, 1.45f)
        val sinP = sin(effectivePitch)
        val cosP = cos(effectivePitch)

        // Pointing horizontally:
        // Turning phone to right produces +gy (when vertical) and -gz (when flat).
        // Adding a natural wrist roll assistance term (0.35 * gy) guarantees immediate,
        // smooth horizontal response whether the user pans, twists, or points.
        val omegaHorizontal = (gy * sinP - gz * cosP) + (gy * 0.35f)
        val omegaVertical = -gx

        // Base sensitivity calibrated to standard screen DPI
        val baseScale = 920f * (screenWidth / 1080f)

        var rawDx = omegaHorizontal * baseScale * config.sensitivityX * dt
        var rawDy = omegaVertical * baseScale * config.sensitivityY * dt

        if (config.trackingParadigm == TrackingParadigm.PHYSICAL_WORLD_ANCHOR) {
            rawDx = -rawDx * config.worldAnchorSensitivity
            rawDy = -rawDy * config.worldAnchorSensitivity
        }

        if (config.invertX) rawDx = -rawDx
        if (config.invertY) rawDy = -rawDy

        applyMotionFilter(rawDx, rawDy, dt, isFusionMode = false)
    }

    /**
     * Mode 2: Accelerometer Tilt-Steering Joystick
     * Moves cursor at a smooth velocity proportional to how far phone is tilted
     * from the calibrated neutral holding posture.
     */
    private fun processTiltMovement(roll: Float, pitch: Float, dt: Float) {
        if (config.trackingParadigm == TrackingParadigm.PHYSICAL_WORLD_ANCHOR && rotationVectorSensor != null) {
            return
        }

        val diffRoll = roll - neutralRoll
        val diffPitch = pitch - neutralPitch

        // Deadzone in degrees (e.g., config.deadzone * 50 = ~2.0° to 10.0°)
        val deadzoneDeg = (config.deadzone * 45f).coerceIn(1.2f, 9.0f)

        val absRoll = abs(diffRoll)
        val absPitch = abs(diffPitch)

        val tiltSpeedX = if (absRoll > deadzoneDeg) {
            sign(diffRoll) * (absRoll - deadzoneDeg).pow(1.28f)
        } else {
            0f
        }

        val tiltSpeedY = if (absPitch > deadzoneDeg) {
            sign(diffPitch) * (absPitch - deadzoneDeg).pow(1.28f)
        } else {
            0f
        }

        // Base tilt gliding speed in pixels per second per degree
        val baseTiltSpeed = 62f * (screenWidth / 1080f)

        var rawDx = tiltSpeedX * baseTiltSpeed * config.sensitivityX * dt
        var rawDy = -tiltSpeedY * baseTiltSpeed * config.sensitivityY * dt

        if (config.trackingParadigm == TrackingParadigm.PHYSICAL_WORLD_ANCHOR) {
            rawDx = -rawDx * config.worldAnchorSensitivity
            rawDy = -rawDy * config.worldAnchorSensitivity
        }

        if (config.invertX) rawDx = -rawDx
        if (config.invertY) rawDy = -rawDy

        applyMotionFilter(rawDx, rawDy, dt, isFusionMode = false)
    }

    /**
     * Mode 3: 6-Axis Sensor Fusion
     * Combines high-speed Gyroscope rotation with continuous Gravity Orientation
     * and Adaptive Micro-Tremor Suppression (dynamic 1€ filter).
     */
    private fun processFusionMovement(gx: Float, gy: Float, gz: Float, dt: Float) {
        if (config.trackingParadigm == TrackingParadigm.PHYSICAL_WORLD_ANCHOR && rotationVectorSensor != null) {
            return
        }

        val sinP = sin(currentPitchRad)
        val cosP = cos(currentPitchRad)
        val sinR = sin(currentRollRad)
        val cosR = cos(currentRollRad)

        // 3D coordinate transformation into virtual screen coordinates
        val pointingYaw = (gy * sinP - gz * cosP)
        val pointingPitch = -gx

        // Compensate for phone roll (device held sideways or tilted)
        val screenOmegaX = (pointingYaw * cosR + pointingPitch * sinR) + (gy * 0.25f)
        val screenOmegaY = (pointingPitch * cosR - pointingYaw * sinR)

        val baseScale = 920f * (screenWidth / 1080f)

        var rawDx = screenOmegaX * baseScale * config.sensitivityX * dt
        var rawDy = screenOmegaY * baseScale * config.sensitivityY * dt

        if (config.trackingParadigm == TrackingParadigm.PHYSICAL_WORLD_ANCHOR) {
            rawDx = -rawDx * config.worldAnchorSensitivity
            rawDy = -rawDy * config.worldAnchorSensitivity
        }

        if (config.invertX) rawDx = -rawDx
        if (config.invertY) rawDy = -rawDy

        applyMotionFilter(rawDx, rawDy, dt, isFusionMode = true)
    }

    private fun applyMotionFilter(rawDx: Float, rawDy: Float, dt: Float, isFusionMode: Boolean) {
        // 1. Deadzone filtering (in pixel displacement)
        val mag = sqrt((rawDx * rawDx + rawDy * rawDy).toDouble()).toFloat()
        val deadzoneThreshold = (config.deadzone * 28f * (screenWidth / 1080f)).coerceAtLeast(0.01f)

        val (dzDx, dzDy) = if (mag < deadzoneThreshold) {
            0f to 0f
        } else {
            val scale = (mag - deadzoneThreshold) / mag
            (rawDx * scale) to (rawDy * scale)
        }

        // 2. Velocity Acceleration Curve (Non-linear acceleration for fast flicks)
        val (accelDx, accelDy) = if (config.accelerationEnabled && (dzDx != 0f || dzDy != 0f) && dt > 0f) {
            val speedPxPerSec = sqrt((dzDx * dzDx + dzDy * dzDy).toDouble()).toFloat() / dt
            val refSpeed = 500f * (screenWidth / 1080f)
            val mult = if (speedPxPerSec > refSpeed) {
                1.0f + ((speedPxPerSec - refSpeed) / refSpeed).pow(config.accelerationFactor - 0.8f).coerceIn(0f, 3.2f)
            } else {
                1.0f
            }
            (dzDx * mult) to (dzDy * mult)
        } else {
            dzDx to dzDy
        }

        // 3. Precision (Sniper) dampening
        val (precDx, precDy) = if (isPrecisionActive) {
            (accelDx * config.precisionSpeedMultiplier) to (accelDy * config.precisionSpeedMultiplier)
        } else {
            accelDx to accelDy
        }

        // 4. Adaptive Smoothing (Micro-Tremor Suppression in Fusion mode, EMA in others)
        val alpha = if (isFusionMode && dt > 0f) {
            // Adaptive filter: high smoothing for micro-shakes, instant response for fast strokes
            val speed = sqrt((precDx * precDx + precDy * precDy).toDouble()).toFloat() / dt
            val baseAlpha = (1.0f - config.smoothingFactor).coerceIn(0.08f, 0.95f)
            if (speed > 600f) {
                (baseAlpha + 0.35f).coerceAtMost(0.98f) // Fast motion -> Instant 0-lag tracking
            } else if (speed < 120f) {
                (baseAlpha * 0.45f).coerceAtLeast(0.08f) // Tiny tremor -> High stability filter
            } else {
                baseAlpha
            }
        } else {
            (1.0f - config.smoothingFactor).coerceIn(0.08f, 0.95f)
        }

        filteredDeltaX = (filteredDeltaX * (1f - alpha)) + (precDx * alpha)
        filteredDeltaY = (filteredDeltaY * (1f - alpha)) + (precDy * alpha)

        if (abs(filteredDeltaX) < 0.02f) filteredDeltaX = 0f
        if (abs(filteredDeltaY) < 0.02f) filteredDeltaY = 0f

        // Apply displacement to cursor
        cursorX += filteredDeltaX
        cursorY += filteredDeltaY

        clampCursor()

        onCursorMoved(cursorX, cursorY)
    }

    private fun clampCursor() {
        val maxX = screenWidth.toFloat()
        val maxY = screenHeight.toFloat()

        cursorX = cursorX.coerceIn(0f, maxX)
        cursorY = cursorY.coerceIn(0f, maxY)
    }

    private var lastTelemetryEmitMs = 0L

    private fun updateTelemetryFps() {
        frameCount++
        val now = SystemClock.elapsedRealtime()
        if (now - lastFpsTimestamp >= 500) {
            currentFps = (frameCount * 1000 / (now - lastFpsTimestamp)).toInt()
            frameCount = 0
            lastFpsTimestamp = now
        }

        // Fluid 60fps telemetry emission (~16ms)
        if (now - lastTelemetryEmitMs >= 16) {
            lastTelemetryEmitMs = now
            _telemetry.value = SensorTelemetry(
                pitchDeg = currentPitch,
                rollDeg = currentRoll,
                gyroRateX = currentGyroX,
                gyroRateY = currentGyroY,
                deltaX = filteredDeltaX,
                deltaY = filteredDeltaY,
                cursorX = cursorX,
                cursorY = cursorY,
                fps = if (currentFps > 0) currentFps else 60,
                isTrackingActive = isListening && !isTrackingPaused,
                isPrecisionActive = isPrecisionActive
            )
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}

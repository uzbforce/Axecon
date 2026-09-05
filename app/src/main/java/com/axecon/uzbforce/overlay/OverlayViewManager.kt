package com.axecon.uzbforce.overlay

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.axecon.uzbforce.R
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.graphics.Typeface
import android.view.animation.LinearInterpolator
import com.axecon.uzbforce.data.PreferencesManager
import com.axecon.uzbforce.model.CursorAction
import com.axecon.uzbforce.model.CursorConfig
import com.axecon.uzbforce.model.CursorStyle
import com.axecon.uzbforce.model.CustomAppShortcut
import com.axecon.uzbforce.model.CustomKeySequence
import com.axecon.uzbforce.model.KeyStep
import com.axecon.uzbforce.model.SideButtonIndicator
import com.axecon.uzbforce.model.TrackingParadigm
import kotlin.math.cos
import kotlin.math.sin

class OverlayViewManager(
    private val context: Context,
    private var config: CursorConfig,
    private val onActionSelected: (CursorAction) -> Unit
) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    // Cursor overlay view & params
    private var cursorView: CursorOverlayView? = null
    private var cursorParams: WindowManager.LayoutParams? = null

    // Action Menu overlay
    private var actionMenuView: ActionMenuOverlayView? = null
    private var actionMenuParams: WindowManager.LayoutParams? = null
    var isActionMenuVisible: Boolean = false
        private set

    // Floating Action Button Overlay (optional on-screen trigger)
    private var floatingActionButtonView: View? = null
    private var floatingActionButtonParams: WindowManager.LayoutParams? = null

    // Keyboard / Broken screen typing helper overlay
    private var dpadHelperView: View? = null
    private var dpadHelperParams: WindowManager.LayoutParams? = null
    var isDpadHelperVisible: Boolean = false
        private set

    private var isDragging = false

    fun updateConfig(newConfig: CursorConfig) {
        this.config = newConfig
        cursorView?.updateConfig(newConfig)
        if (newConfig.floatingActionButtonEnabled) {
            showFloatingActionButton()
        } else {
            hideFloatingActionButton()
        }
    }

    fun isAttached(): Boolean = cursorView != null

    private fun getOverlayWindowType(): Int {
        return WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
    }

    @SuppressLint("InflateParams")
    fun attach() {
        if (cursorView != null) return

        try {
            cursorView = CursorOverlayView(context, config)
            val overlayType = getOverlayWindowType()

            cursorParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                overlayType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 0
                y = 0
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    layoutInDisplayCutoutMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                    } else {
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    fitInsetsTypes = 0
                    fitInsetsSides = 0
                }
            }

            windowManager.addView(cursorView, cursorParams)

            if (config.floatingActionButtonEnabled) {
                showFloatingActionButton()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun detach() {
        try {
            if (cursorView != null) {
                windowManager.removeView(cursorView)
                cursorView = null
            }
            hideActionMenu()
            hideDpadHelper()
            hideFloatingActionButton()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateCursorPosition(x: Float, y: Float) {
        cursorView?.setCursorPosition(x, y)
        if (isActionMenuVisible) {
            actionMenuView?.updateHover(x, y)
        }
    }

    fun handleActionMenuClick(x: Float, y: Float): Boolean {
        if (!isActionMenuVisible) return false
        return actionMenuView?.handleClick(x, y) ?: false
    }

    fun triggerClickAnimation() {
        cursorView?.animateClickRipple()
    }

    fun highlightSideIndicator(index: Int) {
        cursorView?.highlightSideIndicator(index)
    }

    fun updateSequenceCombo(steps: List<KeyStep>, matchedSeq: CustomKeySequence?, timeoutMs: Long) {
        cursorView?.updateSequenceCombo(steps, matchedSeq, timeoutMs)
    }

    fun clearSequenceCombo() {
        cursorView?.clearSequenceCombo()
    }

    fun setDragging(dragging: Boolean, startX: Float = 0f, startY: Float = 0f, isPhysicalHold: Boolean = false) {
        this.isDragging = dragging
        cursorView?.setDragging(dragging, startX, startY, isPhysicalHold)
    }

    fun setDwellProgress(progress: Float) {
        cursorView?.setDwellProgress(progress)
    }

    fun showFloatingActionButton() {
        if (floatingActionButtonView != null) return
        try {
            val density = context.resources.displayMetrics.density
            val btn = FrameLayout(context).apply {
                val backgroundDrawable = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(0xEE1E293B.toInt())
                    setStroke((2 * density).toInt(), 0xFF3B82F6.toInt())
                }
                background = backgroundDrawable
                setPadding((12 * density).toInt(), (12 * density).toInt(), (12 * density).toInt(), (12 * density).toInt())

                val iconText = TextView(context).apply {
                    text = "⚡"
                    textSize = 20f
                    gravity = Gravity.CENTER
                }
                addView(iconText)

                setOnClickListener {
                    showActionMenu()
                }
            }

            val overlayType = getOverlayWindowType()

            floatingActionButtonParams = WindowManager.LayoutParams(
                (54 * density).toInt(),
                (54 * density).toInt(),
                overlayType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.END or Gravity.CENTER_VERTICAL
                x = (16 * density).toInt()
                y = 0
            }

            floatingActionButtonView = btn
            windowManager.addView(floatingActionButtonView, floatingActionButtonParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun hideFloatingActionButton() {
        try {
            if (floatingActionButtonView != null) {
                windowManager.removeView(floatingActionButtonView)
                floatingActionButtonView = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun showActionMenu() {
        if (isActionMenuVisible) {
            hideActionMenu()
            return
        }

        try {
            actionMenuView = ActionMenuOverlayView(context) { action ->
                hideActionMenu()
                onActionSelected(action)
            }

            val overlayType = getOverlayWindowType()

            actionMenuParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                overlayType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
            }

            windowManager.addView(actionMenuView, actionMenuParams)
            isActionMenuVisible = true

            // Ensure cursorView is stacked above the action menu modal
            if (cursorView != null && cursorParams != null) {
                try {
                    windowManager.removeView(cursorView)
                    windowManager.addView(cursorView, cursorParams)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun hideActionMenu() {
        if (!isActionMenuVisible) return
        try {
            if (actionMenuView != null) {
                windowManager.removeView(actionMenuView)
                actionMenuView = null
            }
            isActionMenuVisible = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toggleDpadHelper() {
        if (isDpadHelperVisible) {
            hideDpadHelper()
        } else {
            showDpadHelper()
        }
    }

    fun showDpadHelper() {
        if (isDpadHelperVisible) return
        try {
            val density = context.resources.displayMetrics.density
            val root = FrameLayout(context).apply {
                val bg = GradientDrawable().apply {
                    setColor(0xF018181B.toInt())
                    cornerRadius = 24 * density
                    setStroke((1 * density).toInt(), 0x33FFFFFF)
                }
                background = bg
                setPadding((12 * density).toInt(), (10 * density).toInt(), (12 * density).toInt(), (10 * density).toInt())
            }

            val layout = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }

            val actions = listOf(
                "🎤 Dictate" to CursorAction.VOICE_TYPING,
                "⬅️ Back" to CursorAction.GLOBAL_BACK,
                "🏠 Home" to CursorAction.GLOBAL_HOME,
                "📱 Recents" to CursorAction.GLOBAL_RECENTS,
                "🎯 Recenter" to CursorAction.RECENTER_CALIBRATE,
                "❌ Close" to CursorAction.NONE
            )

            for ((label, act) in actions) {
                val btn = TextView(context).apply {
                    text = label
                    setTextColor(Color.WHITE)
                    textSize = 12f
                    val btnBg = GradientDrawable().apply {
                        setColor(0xFF27272A.toInt())
                        cornerRadius = 12 * density
                    }
                    background = btnBg
                    setPadding((14 * density).toInt(), (10 * density).toInt(), (14 * density).toInt(), (10 * density).toInt())
                    val lp = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins((4 * density).toInt(), 0, (4 * density).toInt(), 0)
                    }
                    layoutParams = lp
                    setOnClickListener {
                        if (act == CursorAction.NONE) {
                            hideDpadHelper()
                        } else {
                            onActionSelected(act)
                        }
                    }
                }
                layout.addView(btn)
            }

            root.addView(layout)
            dpadHelperView = root

            val overlayType = getOverlayWindowType()

            dpadHelperParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                overlayType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.BOTTOM
                y = (16 * density).toInt()
            }

            windowManager.addView(dpadHelperView, dpadHelperParams)
            isDpadHelperVisible = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun hideDpadHelper() {
        if (!isDpadHelperVisible) return
        try {
            if (dpadHelperView != null) {
                windowManager.removeView(dpadHelperView)
                dpadHelperView = null
            }
            isDpadHelperVisible = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Custom Surface / View for rendering high visibility cursor, effects, and side button indicators
     */
    inner class CursorOverlayView(context: Context, private var cfg: CursorConfig) : View(context) {

        private var posX = 500f
        private var posY = 1000f

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = Color.BLACK
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 28f
            setShadowLayer(4f, 1f, 1f, Color.BLACK)
        }
        private val sideIndicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        private var cachedBitmap: Bitmap? = null
        private var cachedBitmapKey: String? = null
        private var cachedBitmapSize: Int = 0

        private var rippleRadius = 0f
        private var rippleAlpha = 0
        private var rippleAnimator: ValueAnimator? = null

        private var dwellProgress: Float = 0f
        private var isDragActive: Boolean = false
        private var isHoldDragMode: Boolean = false
        private var dragOriginX: Float = 0f
        private var dragOriginY: Float = 0f

        // Side Indicator Glow animations per indicator ID
        private val indicatorGlows = mutableMapOf<Int, Float>()
        private val indicatorAnimators = mutableMapOf<Int, ValueAnimator>()
        private var lastActiveIndicatorIndex: Int = 0

        // Sequence Combo State & Second Visualizer
        private var comboActiveSteps: List<KeyStep> = emptyList()
        private var comboMatchedSeq: CustomKeySequence? = null
        private var comboTimeoutMs: Long = 1000L
        private var comboProgress: Float = 0f
        private var comboAnimator: ValueAnimator? = null

        fun updateConfig(newConfig: CursorConfig) {
            val imageChanged = (this.cfg.customImageUri != newConfig.customImageUri) ||
                    (this.cfg.customImagePreset != newConfig.customImagePreset) ||
                    (this.cfg.cursorSizeDp != newConfig.cursorSizeDp) ||
                    (this.cfg.cursorStyle != newConfig.cursorStyle) ||
                    (this.cfg.cursorColorArgb != newConfig.cursorColorArgb) ||
                    (this.cfg.cursorOutlineColorArgb != newConfig.cursorOutlineColorArgb) ||
                    (this.cfg.cursorOutlineWidth != newConfig.cursorOutlineWidth)
            if (imageChanged) {
                cachedBitmap = null
                cachedBitmapKey = null
            }
            this.cfg = newConfig
            invalidate()
        }

        fun setCursorPosition(x: Float, y: Float) {
            posX = x
            posY = y
            invalidate()
        }

        fun setDragging(dragging: Boolean, startX: Float = posX, startY: Float = posY, isPhysicalHold: Boolean = false) {
            isDragActive = dragging
            isHoldDragMode = isPhysicalHold
            dragOriginX = startX
            dragOriginY = startY
            invalidate()
        }

        fun setDwellProgress(progress: Float) {
            dwellProgress = progress
            invalidate()
        }

        fun animateClickRipple() {
            post {
                rippleAnimator?.cancel()
                val maxRadius = (cfg.cursorSizeDp * 2.5f)
                rippleAnimator = ValueAnimator.ofFloat(0f, maxRadius).apply {
                    duration = 300
                    interpolator = AccelerateDecelerateInterpolator()
                    addUpdateListener { anim ->
                        val fraction = anim.animatedFraction
                        rippleRadius = anim.animatedValue as Float
                        rippleAlpha = ((1f - fraction) * 220).toInt()
                        invalidate()
                    }
                    start()
                }
            }
        }

        fun highlightSideIndicator(index: Int) {
            post {
                lastActiveIndicatorIndex = index
                indicatorAnimators[index]?.cancel()
                val anim = ValueAnimator.ofFloat(1f, 0f).apply {
                    duration = 380
                    interpolator = AccelerateDecelerateInterpolator()
                    addUpdateListener {
                        indicatorGlows[index] = it.animatedValue as Float
                        invalidate()
                    }
                }
                indicatorAnimators[index] = anim
                anim.start()
            }
        }

        fun updateSequenceCombo(steps: List<KeyStep>, matchedSeq: CustomKeySequence?, timeoutMs: Long) {
            post {
                comboActiveSteps = steps
                comboMatchedSeq = matchedSeq
                comboTimeoutMs = timeoutMs
                comboAnimator?.cancel()
                comboProgress = 1f
                comboAnimator = ValueAnimator.ofFloat(1f, 0f).apply {
                    duration = timeoutMs
                    interpolator = LinearInterpolator()
                    addUpdateListener {
                        comboProgress = it.animatedValue as Float
                        invalidate()
                    }
                    start()
                }
                invalidate()
            }
        }

        fun clearSequenceCombo() {
            post {
                comboAnimator?.cancel()
                comboActiveSteps = emptyList()
                comboMatchedSeq = null
                comboProgress = 0f
                invalidate()
            }
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            val density = resources.displayMetrics.density

            // 1. Draw Side Button Click Visualizers (configurable per physical button)
            if (cfg.sideIndicatorsEnabled) {
                drawSideIndicators(canvas, density)
            }

            // 2. Draw Key Sequence Combo Visualizer with order and last pressed highlight
            if (cfg.showSequenceVisualizer && comboActiveSteps.isNotEmpty()) {
                drawSequenceComboVisualizer(canvas, density)
            }

            val color = cfg.cursorColorArgb.toInt()
            val size = (cfg.cursorSizeDp * density).coerceAtLeast(16f)

            // Outline configuration
            borderPaint.color = cfg.cursorOutlineColorArgb.toInt()
            val outlineWidth = (cfg.cursorOutlineWidth * density).coerceAtLeast(1f)
            borderPaint.strokeWidth = outlineWidth

            // Draw Click Ripple if active
            if (rippleAlpha > 0 && cfg.showRippleOnClick) {
                val rippleX = posX + cfg.clickOffsetX
                val rippleY = posY + cfg.clickOffsetY
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 5f
                paint.color = color
                paint.alpha = rippleAlpha
                canvas.drawCircle(rippleX, rippleY, rippleRadius, paint)

                paint.style = Paint.Style.FILL
                paint.alpha = (rippleAlpha * 0.3f).toInt()
                canvas.drawCircle(rippleX, rippleY, rippleRadius, paint)
            }

            // Draw Dwell Progress Ring if active
            if (dwellProgress > 0f && cfg.dwellAutoClickEnabled) {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 6f
                paint.color = Color.argb(230, 245, 158, 11) // Amber
                val dwellRect = RectF(posX - size, posY - size, posX + size, posY + size)
                canvas.drawArc(dwellRect, -90f, dwellProgress * 360f, false, paint)
            }

            // Draw Drag indicator with origin anchor, tether line and holding halo
            if (isDragActive) {
                val dragDist = kotlin.math.hypot(posX - dragOriginX, posY - dragOriginY)
                val dragColor = if (isHoldDragMode) 0xFF6366F1.toInt() else 0xFFEF4444.toInt() // Indigo for physical hold, Red for toggle
                val dragFillColor = if (isHoldDragMode) 0x666366F1.toInt() else 0x66EF4444.toInt()

                // Origin Anchor
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 3f * density
                paint.color = dragColor
                canvas.drawCircle(dragOriginX, dragOriginY, 14f * density, paint)
                paint.style = Paint.Style.FILL
                paint.color = dragFillColor
                canvas.drawCircle(dragOriginX, dragOriginY, 8f * density, paint)

                // Tether Line to current cursor
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 2.5f * density
                paint.color = (dragColor and 0x00FFFFFF) or 0xBB000000.toInt()
                canvas.drawLine(dragOriginX, dragOriginY, posX, posY, paint)

                // Drag Cursor Halo
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 4f * density
                paint.color = dragColor
                canvas.drawCircle(posX, posY, size * 1.25f, paint)

                // Holding badge text
                paint.style = Paint.Style.FILL
                paint.color = 0xEE0F172A.toInt()
                val badgeText = if (isHoldDragMode) "✋ HOLDING • RELEASE TO DROP (${dragDist.toInt()}px)" else "DRAGGING • CLICK TO DROP (${dragDist.toInt()}px)"
                textPaint.textSize = 10f * density
                textPaint.color = Color.WHITE
                val tW = textPaint.measureText(badgeText)
                val badgeW = (tW + 18f * density).coerceAtLeast(80f * density)
                val badgeH = 22f * density
                val bRect = RectF(posX - badgeW / 2f, posY - size - badgeH - 6f, posX + badgeW / 2f, posY - size - 6f)
                canvas.drawRoundRect(bRect, 6f * density, 6f * density, paint)

                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1.5f * density
                paint.color = dragColor
                canvas.drawRoundRect(bRect, 6f * density, 6f * density, paint)

                canvas.drawText(badgeText, posX - tW / 2f, posY - size - 11f, textPaint)
            }

            // Draw Sniper Mode Indicator
            if (cfg.isPrecisionActive) {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 3f
                paint.color = Color.MAGENTA
                canvas.drawCircle(posX, posY, size * 1.3f, paint)
            }

            // Draw Cursor based on selected style
            when (cfg.cursorStyle) {
                CursorStyle.CLASSIC_ARROW -> drawClassicArrow(canvas, posX, posY, size, color)
                CursorStyle.PRECISION_CROSSHAIR -> drawPrecisionCrosshair(canvas, posX, posY, size, color)
                CursorStyle.DOT_POINTER -> drawDotPointer(canvas, posX, posY, size, color)
                CursorStyle.TARGET_RING -> drawTargetRing(canvas, posX, posY, size, color)
                CursorStyle.TEARDROP -> drawTeardrop(canvas, posX, posY, size, color)
                CursorStyle.STYLUS_PEN -> drawStylusPen(canvas, posX, posY, size, color)
                CursorStyle.CUSTOM_IMAGE -> drawCustomImageCursor(canvas, posX, posY, size)
            }

            // Coordinate HUD
            if (cfg.showCoordinateHUD) {
                val hudText = "X: ${posX.toInt()}, Y: ${posY.toInt()}"
                canvas.drawText(hudText, posX + size + 8f, posY + size + 8f, textPaint)
            }
        }

        private fun drawSideIndicators(canvas: Canvas, density: Float) {
            val indicators = if (cfg.individualIndicators.isNotEmpty()) {
                cfg.individualIndicators
            } else {
                listOf(
                    SideButtonIndicator(0, "Volume Up", true, cfg.sideIndicatorsOnRight, cfg.sideIndicatorPositionOffset - 0.06f, cfg.sideIndicatorHeightDp, cfg.sideIndicatorWidthDp, cfg.sideIndicatorActiveColorArgb, cfg.sideIndicatorInactiveColorArgb),
                    SideButtonIndicator(1, "Volume Down", true, cfg.sideIndicatorsOnRight, cfg.sideIndicatorPositionOffset, cfg.sideIndicatorHeightDp, cfg.sideIndicatorWidthDp, cfg.sideIndicatorActiveColorArgb, cfg.sideIndicatorInactiveColorArgb),
                    SideButtonIndicator(2, "Extra / Power", true, cfg.sideIndicatorsOnRight, cfg.sideIndicatorPositionOffset + 0.08f, (cfg.sideIndicatorHeightDp * 0.8f).toInt(), cfg.sideIndicatorWidthDp, cfg.sideIndicatorActiveColorArgb, cfg.sideIndicatorInactiveColorArgb)
                )
            }

            for (ind in indicators) {
                if (!ind.isEnabled) continue

                val lineWidth = (ind.thicknessDp * density).coerceAtLeast(3f)
                val lineHeight = (ind.lengthDp * density).coerceAtLeast(14f)
                val centerY = height * ind.verticalOffsetRatio
                val top = centerY - (lineHeight / 2f)
                val bottom = centerY + (lineHeight / 2f)

                val startX = if (ind.isOnRightSide) {
                    width - lineWidth - (4 * density)
                } else {
                    4 * density
                }

                val rect = RectF(startX, top, startX + lineWidth, bottom)
                val glow = indicatorGlows[ind.id] ?: 0f
                val activeColor = ind.activeColorArgb.toInt()
                val inactiveColor = ind.inactiveColorArgb.toInt()

                if (glow > 0.04f) {
                    // Active glowing state
                    sideIndicatorPaint.style = Paint.Style.FILL
                    sideIndicatorPaint.color = activeColor
                    sideIndicatorPaint.alpha = (glow * 255).toInt().coerceIn(0, 255)
                    canvas.drawRoundRect(rect, lineWidth / 2f, lineWidth / 2f, sideIndicatorPaint)

                    // Outer halo stroke
                    sideIndicatorPaint.style = Paint.Style.STROKE
                    sideIndicatorPaint.strokeWidth = 2.5f * density
                    sideIndicatorPaint.alpha = (glow * 180).toInt().coerceIn(0, 255)
                    val expandedRect = RectF(
                        rect.left - 2 * density,
                        rect.top - 2 * density,
                        rect.right + 2 * density,
                        rect.bottom + 2 * density
                    )
                    canvas.drawRoundRect(expandedRect, lineWidth, lineWidth, sideIndicatorPaint)
                } else {
                    // Subtle inactive translucent bar
                    sideIndicatorPaint.style = Paint.Style.FILL
                    sideIndicatorPaint.color = inactiveColor
                    sideIndicatorPaint.alpha = Color.alpha(inactiveColor)
                    canvas.drawRoundRect(rect, lineWidth / 2f, lineWidth / 2f, sideIndicatorPaint)
                }
            }
        }

        private fun drawSequenceComboVisualizer(canvas: Canvas, density: Float) {
            val indicators = cfg.individualIndicators
            val activeInd = indicators.find { it.id == lastActiveIndicatorIndex }
                ?: indicators.firstOrNull()
                ?: SideButtonIndicator(0, "Volume Up", true, true, 0.45f, 40, 5, 0xFF00E5FF, 0x33FFFFFF)

            val isOnRight = activeInd.isOnRightSide
            val centerY = height * activeInd.verticalOffsetRatio
            val btnThickness = (activeInd.thicknessDp * density).coerceAtLeast(3f)

            // Button visualizer edge coordinate
            val btnEdgeX = if (isOnRight) {
                width - btnThickness - (4 * density)
            } else {
                4 * density + btnThickness
            }

            // 1. Second Visualizer: 1s Countdown Progress Rail directly alongside button visualizer line
            val timerWidth = 6 * density
            val timerHeight = (activeInd.lengthDp * density).coerceAtLeast(42 * density)
            val timerTop = centerY - (timerHeight / 2f)
            val timerBottom = centerY + (timerHeight / 2f)
            val timerX = if (isOnRight) {
                btnEdgeX - (8 * density) - timerWidth
            } else {
                btnEdgeX + (8 * density)
            }

            val timerRect = RectF(timerX, timerTop, timerX + timerWidth, timerBottom)

            // Draw Timer Background Rail
            sideIndicatorPaint.style = Paint.Style.FILL
            sideIndicatorPaint.color = 0xAA0F172A.toInt() // Dark Slate
            canvas.drawRoundRect(timerRect, timerWidth / 2f, timerWidth / 2f, sideIndicatorPaint)

            sideIndicatorPaint.style = Paint.Style.STROKE
            sideIndicatorPaint.strokeWidth = 1f * density
            sideIndicatorPaint.color = 0x44FFFFFF
            canvas.drawRoundRect(timerRect, timerWidth / 2f, timerWidth / 2f, sideIndicatorPaint)

            // Draw Timer Progress Fill (depleting from 1.0 down to 0.0)
            if (comboProgress > 0.01f) {
                val fillHeight = timerHeight * comboProgress
                val fillTop = timerBottom - fillHeight
                val fillRect = RectF(timerX, fillTop, timerX + timerWidth, timerBottom)

                sideIndicatorPaint.style = Paint.Style.FILL
                val fillColor = if (comboMatchedSeq != null) {
                    cfg.sequenceMatchedColorArgb.toInt()
                } else if (comboProgress < 0.35f) {
                    0xFFEF4444.toInt() // Red warning low time
                } else {
                    cfg.lastPressedHighlightColorArgb.toInt() // Standout color
                }
                sideIndicatorPaint.color = fillColor
                sideIndicatorPaint.alpha = 240
                canvas.drawRoundRect(fillRect, timerWidth / 2f, timerWidth / 2f, sideIndicatorPaint)
            }

            // 2. Sequence Order HUD Card: Shows buttons pressed in order with the LAST PRESSED KEY standing out
            val stepCount = comboActiveSteps.size
            if (stepCount == 0) return

            val badgePadH = 10 * density
            val badgePadV = 6 * density
            val badgeSpacing = 6 * density
            val arrowWidth = 14 * density

            // Measure step labels
            val stepInfoList = comboActiveSteps.mapIndexed { index, step ->
                val orderNum = when (index) {
                    0 -> "①"
                    1 -> "②"
                    2 -> "③"
                    3 -> "④"
                    4 -> "⑤"
                    5 -> "⑥"
                    6 -> "⑦"
                    else -> "#${index + 1}"
                }
                val keyName = when (step) {
                    KeyStep.VOLUME_UP -> "Vol +"
                    KeyStep.VOLUME_DOWN -> "Vol -"
                    KeyStep.HEADSET_HOOK -> "Hook"
                    KeyStep.POWER -> "Power"
                    KeyStep.BACK -> "Back"
                }
                val isLast = (index == stepCount - 1)
                val label = "$orderNum $keyName"
                Triple(label, isLast, step)
            }

            // Text Paint for badges
            val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 12 * density
                typeface = Typeface.DEFAULT_BOLD
            }

            var totalBadgesWidth = 0f
            val widths = stepInfoList.map { (label, isLast, _) ->
                val w = badgePaint.measureText(label) + (badgePadH * 2)
                totalBadgesWidth += w
                w
            }
            totalBadgesWidth += (stepCount - 1) * (arrowWidth + badgeSpacing * 2)

            val headerText = if (comboMatchedSeq != null) {
                "✓ MATCHED: ${comboMatchedSeq?.name}"
            } else {
                val remainingSec = String.format(java.util.Locale.US, "%.1fs", (comboProgress * comboTimeoutMs) / 1000f)
                "⚡ COMBO SEQUENCE • $remainingSec"
            }

            badgePaint.textSize = 10 * density
            val headerWidth = badgePaint.measureText(headerText)
            val cardContentWidth = maxOf(totalBadgesWidth, headerWidth) + (24 * density)
            val cardHeight = 64 * density

            val cardLeft = if (isOnRight) {
                timerX - (10 * density) - cardContentWidth
            } else {
                timerX + timerWidth + (10 * density)
            }
            val cardRight = cardLeft + cardContentWidth
            val cardTop = centerY - (cardHeight / 2f)
            val cardBottom = centerY + (cardHeight / 2f)
            val cardRect = RectF(cardLeft, cardTop, cardRight, cardBottom)

            // Draw HUD Background Card
            sideIndicatorPaint.style = Paint.Style.FILL
            sideIndicatorPaint.color = 0xF00F172A.toInt() // Translucent Dark Slate
            canvas.drawRoundRect(cardRect, 14 * density, 14 * density, sideIndicatorPaint)

            // Card Border
            sideIndicatorPaint.style = Paint.Style.STROKE
            sideIndicatorPaint.strokeWidth = 1.5f * density
            sideIndicatorPaint.color = if (comboMatchedSeq != null) {
                cfg.sequenceMatchedColorArgb.toInt()
            } else {
                0x6638BDF8 // Sky blue border
            }
            canvas.drawRoundRect(cardRect, 14 * density, 14 * density, sideIndicatorPaint)

            // Draw Header
            badgePaint.textSize = 10 * density
            badgePaint.color = if (comboMatchedSeq != null) {
                cfg.sequenceMatchedColorArgb.toInt()
            } else {
                0xFF94A3B8.toInt() // Light Slate
            }
            canvas.drawText(headerText, cardLeft + (12 * density), cardTop + (16 * density), badgePaint)

            // Draw Step Badges
            var curX = cardLeft + (12 * density)
            val badgeY = cardTop + (26 * density)
            val badgeH = 26 * density

            for (i in stepInfoList.indices) {
                val (label, isLast, _) = stepInfoList[i]
                val bWidth = widths[i]
                val bRect = RectF(curX, badgeY, curX + bWidth, badgeY + badgeH)

                if (isLast) {
                    // ★ LAST PRESSED KEY STANDS OUT WITH DIFFERENT COLOR & GLOW ★
                    sideIndicatorPaint.style = Paint.Style.FILL
                    sideIndicatorPaint.color = cfg.lastPressedHighlightColorArgb.toInt() // Glowing Amber/Gold/Standout
                    canvas.drawRoundRect(bRect, 8 * density, 8 * density, sideIndicatorPaint)

                    // Bold border
                    sideIndicatorPaint.style = Paint.Style.STROKE
                    sideIndicatorPaint.strokeWidth = 2 * density
                    sideIndicatorPaint.color = Color.WHITE
                    canvas.drawRoundRect(bRect, 8 * density, 8 * density, sideIndicatorPaint)

                    // Text
                    badgePaint.textSize = 12 * density
                    badgePaint.typeface = Typeface.DEFAULT_BOLD
                    badgePaint.color = 0xFF000000.toInt() // Dark text on standout bright color
                    val textW = badgePaint.measureText(label)
                    val textX = curX + (bWidth - textW) / 2f
                    val textY = badgeY + (badgeH / 2f) + (4 * density)
                    canvas.drawText(label, textX, textY, badgePaint)
                } else {
                    // Previous Step Badge (Clean Cool Muted Theme)
                    sideIndicatorPaint.style = Paint.Style.FILL
                    sideIndicatorPaint.color = 0xFF1E293B.toInt() // Slate 800
                    canvas.drawRoundRect(bRect, 8 * density, 8 * density, sideIndicatorPaint)

                    sideIndicatorPaint.style = Paint.Style.STROKE
                    sideIndicatorPaint.strokeWidth = 1 * density
                    sideIndicatorPaint.color = 0xFF334155.toInt() // Slate 700
                    canvas.drawRoundRect(bRect, 8 * density, 8 * density, sideIndicatorPaint)

                    badgePaint.textSize = 11 * density
                    badgePaint.typeface = Typeface.DEFAULT
                    badgePaint.color = 0xFFE2E8F0.toInt() // Light Silver
                    val textW = badgePaint.measureText(label)
                    val textX = curX + (bWidth - textW) / 2f
                    val textY = badgeY + (badgeH / 2f) + (4 * density)
                    canvas.drawText(label, textX, textY, badgePaint)
                }

                curX += bWidth

                // Draw connector arrow between steps
                if (i < stepCount - 1) {
                    curX += badgeSpacing
                    badgePaint.textSize = 10 * density
                    badgePaint.color = 0xFF64748B.toInt()
                    val arrowY = badgeY + (badgeH / 2f) + (3.5f * density)
                    canvas.drawText("➔", curX, arrowY, badgePaint)
                    curX += arrowWidth + badgeSpacing
                }
            }
        }

        private fun drawClassicArrow(canvas: Canvas, x: Float, y: Float, size: Float, color: Int) {
            val arrowLength = size * 1.15f
            val path = Path().apply {
                moveTo(x, y)
                lineTo(x, y + arrowLength)
                lineTo(x + arrowLength * 0.28f, y + arrowLength * 0.72f)
                lineTo(x + arrowLength * 0.54f, y + arrowLength * 1.05f)
                lineTo(x + arrowLength * 0.70f, y + arrowLength * 0.92f)
                lineTo(x + arrowLength * 0.44f, y + arrowLength * 0.60f)
                lineTo(x + arrowLength * 0.72f, y + arrowLength * 0.60f)
                close()
            }

            if (cfg.cursorOutlineEnabled) {
                borderPaint.style = Paint.Style.STROKE
                canvas.drawPath(path, borderPaint)
            }

            paint.style = Paint.Style.FILL
            paint.color = color
            paint.alpha = 255
            canvas.drawPath(path, paint)
        }

        private fun drawPrecisionCrosshair(canvas: Canvas, x: Float, y: Float, size: Float, color: Int) {
            val radius = size * 0.55f
            val gap = radius * 0.35f

            if (cfg.cursorOutlineEnabled) {
                borderPaint.style = Paint.Style.STROKE
                canvas.drawCircle(x, y, radius, borderPaint)
                canvas.drawLine(x - radius - 6f, y, x - gap, y, borderPaint)
                canvas.drawLine(x + gap, y, x + radius + 6f, y, borderPaint)
                canvas.drawLine(x, y - radius - 6f, x, y - gap, borderPaint)
                canvas.drawLine(x, y + gap, x, y + radius + 6f, borderPaint)
            }

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 3f
            paint.color = color
            paint.alpha = 255

            canvas.drawCircle(x, y, radius, paint)
            canvas.drawLine(x - radius - 4f, y, x - gap, y, paint)
            canvas.drawLine(x + gap, y, x + radius + 4f, y, paint)
            canvas.drawLine(x, y - radius - 4f, x, y - gap, paint)
            canvas.drawLine(x, y + gap, x, y + radius + 4f, paint)

            paint.style = Paint.Style.FILL
            canvas.drawCircle(x, y, 3f, paint)
        }

        private fun drawDotPointer(canvas: Canvas, x: Float, y: Float, size: Float, color: Int) {
            val radius = size * 0.35f
            if (cfg.cursorOutlineEnabled) {
                borderPaint.style = Paint.Style.STROKE
                canvas.drawCircle(x, y, radius, borderPaint)
            }
            paint.style = Paint.Style.FILL
            paint.color = color
            paint.alpha = 255
            canvas.drawCircle(x, y, radius, paint)
        }

        private fun drawTargetRing(canvas: Canvas, x: Float, y: Float, size: Float, color: Int) {
            val radius = size * 0.55f
            if (cfg.cursorOutlineEnabled) {
                borderPaint.style = Paint.Style.STROKE
                canvas.drawCircle(x, y, radius, borderPaint)
                canvas.drawCircle(x, y, radius * 0.4f, borderPaint)
            }

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 4f
            paint.color = color
            paint.alpha = 255
            canvas.drawCircle(x, y, radius, paint)

            paint.style = Paint.Style.FILL
            canvas.drawCircle(x, y, radius * 0.35f, paint)
        }

        private fun drawTeardrop(canvas: Canvas, x: Float, y: Float, size: Float, color: Int) {
            val radius = size * 0.45f
            val path = Path().apply {
                moveTo(x, y)
                quadTo(x + radius * 1.5f, y + radius * 0.5f, x + radius * 0.8f, y + radius * 1.5f)
                quadTo(x, y + radius * 2f, x - radius * 0.8f, y + radius * 1.5f)
                quadTo(x - radius * 1.5f, y + radius * 0.5f, x, y)
                close()
            }

            if (cfg.cursorOutlineEnabled) {
                borderPaint.style = Paint.Style.STROKE
                canvas.drawPath(path, borderPaint)
            }

            paint.style = Paint.Style.FILL
            paint.color = color
            paint.alpha = 255
            canvas.drawPath(path, paint)
        }

        private fun drawStylusPen(canvas: Canvas, x: Float, y: Float, size: Float, color: Int) {
            val path = Path().apply {
                moveTo(x, y)
                lineTo(x + size * 0.3f, y + size * 0.8f)
                lineTo(x + size * 0.8f, y + size * 1.4f)
                lineTo(x + size * 0.5f, y + size * 1.7f)
                lineTo(x, y + size * 1.1f)
                close()
            }

            if (cfg.cursorOutlineEnabled) {
                borderPaint.style = Paint.Style.STROKE
                canvas.drawPath(path, borderPaint)
            }

            paint.style = Paint.Style.FILL
            paint.color = color
            paint.alpha = 255
            canvas.drawPath(path, paint)
        }

        private fun drawCustomImageCursor(canvas: Canvas, x: Float, y: Float, size: Float) {
            val sizeInt = size.toInt().coerceAtLeast(16)
            val currentKey = "${cfg.customImageUri}_${cfg.customImagePreset}_$sizeInt"

            if (cachedBitmap == null || cachedBitmapKey != currentKey || cachedBitmapSize != sizeInt) {
                cachedBitmap = loadAndScaleCursorBitmap(sizeInt)
                cachedBitmapKey = currentKey
                cachedBitmapSize = sizeInt
            }

            val bmp = cachedBitmap
            if (bmp != null && !bmp.isRecycled) {
                if (cfg.customImageUri != null || cfg.customImagePreset != "tech") {
                    canvas.drawBitmap(bmp, x - bmp.width / 2f, y - bmp.height / 2f, null)
                } else {
                    canvas.drawBitmap(bmp, x, y, null)
                }
            } else {
                drawClassicArrow(canvas, x, y, size, cfg.cursorColorArgb.toInt())
            }
        }

        private fun drawPhysicalWorldAnchorReticle(canvas: Canvas, x: Float, y: Float, size: Float, color: Int) {
            val density = resources.displayMetrics.density
            val radius = size * 0.78f
            val innerRadius = radius * 0.44f

            // Outer horizon stasis ring
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2.2f * density
            paint.color = color
            paint.alpha = 220
            canvas.drawCircle(x, y, radius, paint)

            // Inner target ring
            paint.strokeWidth = 1.6f * density
            paint.alpha = 240
            canvas.drawCircle(x, y, innerRadius, paint)

            // Center focal point
            paint.style = Paint.Style.FILL
            paint.alpha = 255
            canvas.drawCircle(x, y, 3.5f * density, paint)

            // 4 Cardinal World Ticks
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2.2f * density
            val tickLen = 6f * density
            canvas.drawLine(x, y - radius - tickLen, x, y - radius + (2f * density), paint) // North
            canvas.drawLine(x, y + radius - (2f * density), x, y + radius + tickLen, paint) // South
            canvas.drawLine(x - radius - tickLen, y, x - radius + (2f * density), y, paint) // West
            canvas.drawLine(x + radius - (2f * density), y, x + radius + tickLen, y, paint) // East

            // 4 Diagonal Stabilizer Notches
            paint.strokeWidth = 1.4f * density
            paint.alpha = 150
            val diag = radius * 0.707f
            val dLen = 3.5f * density
            canvas.drawLine(x - diag, y - diag, x - diag - dLen, y - diag - dLen, paint)
            canvas.drawLine(x + diag, y - diag, x + diag + dLen, y - diag - dLen, paint)
            canvas.drawLine(x - diag, y + diag, x - diag - dLen, y + diag + dLen, paint)
            canvas.drawLine(x + diag, y + diag, x + diag + dLen, y + diag + dLen, paint)
        }

        private fun loadAndScaleCursorBitmap(targetSize: Int): Bitmap? {
            try {
                if (!cfg.customImageUri.isNullOrBlank()) {
                    val uri = Uri.parse(cfg.customImageUri)
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val original = BitmapFactory.decodeStream(stream)
                        if (original != null) {
                            return Bitmap.createScaledBitmap(original, targetSize, targetSize, true)
                        }
                    }
                }

                val resId = when (cfg.customImagePreset) {
                    "HAND_GESTURE" -> R.drawable.ic_cursor_hand
                    "TECH_DIAMOND" -> R.drawable.ic_cursor_diamond
                    "NEON_STAR" -> R.drawable.ic_cursor_star
                    else -> R.drawable.ic_cursor_tech
                }

                val drawable = ContextCompat.getDrawable(context, resId)
                if (drawable != null) {
                    return drawable.toBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }
    }

    /**
     * M3E Material Action Button Radial & Grid Overlay Wheel with Dynamic App Shortcuts & Mouse-Interactive Clickability
     */
    inner class ActionMenuOverlayView(
        context: Context,
        private val onSelect: (CursorAction) -> Unit
    ) : FrameLayout(context) {

        private val interactiveItems = mutableListOf<Pair<View, () -> Unit>>()
        private val cardContainer: LinearLayout

        init {
            setBackgroundColor(0xCC090D16.toInt())
            setOnClickListener { hideActionMenu() }

            val density = resources.displayMetrics.density

            val scrollView = ScrollView(context).apply {
                isFillViewport = true
                val lp = LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.CENTER
                }
                layoutParams = lp
            }

            cardContainer = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                val bg = GradientDrawable().apply {
                    setColor(0xFF1E2433.toInt())
                    cornerRadius = 28 * density
                    setStroke((1 * density).toInt(), 0x334B5563)
                }
                background = bg
                setPadding((18 * density).toInt(), (18 * density).toInt(), (18 * density).toInt(), (18 * density).toInt())
                val lp = FrameLayout.LayoutParams(
                    (340 * density).toInt(),
                    FrameLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.CENTER
                    setMargins((16 * density).toInt(), (20 * density).toInt(), (16 * density).toInt(), (20 * density).toInt())
                }
                layoutParams = lp
                isClickable = true
            }

            // Header
            val headerLayout = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 0, 0, (12 * density).toInt())
            }

            val iconBadge = TextView(context).apply {
                text = "⚡"
                textSize = 18f
                setPadding((6 * density).toInt(), (4 * density).toInt(), (6 * density).toInt(), (4 * density).toInt())
            }
            headerLayout.addView(iconBadge)

            val titleText = TextView(context).apply {
                text = "Axecon Action Menu"
                textSize = 17f
                setTextColor(Color.WHITE)
                paint.isFakeBoldText = true
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            headerLayout.addView(titleText)

            val headerCloseBtn = TextView(context).apply {
                text = " ✕ "
                textSize = 18f
                setTextColor(Color.WHITE)
                paint.isFakeBoldText = true
                val closeBg = GradientDrawable().apply {
                    setColor(0x44EF4444.toInt())
                    cornerRadius = 14 * density
                    setStroke((1 * density).toInt(), 0xAAEF4444.toInt())
                }
                background = closeBg
                setPadding((12 * density).toInt(), (6 * density).toInt(), (12 * density).toInt(), (6 * density).toInt())
                setOnClickListener { hideActionMenu() }
            }
            headerLayout.addView(headerCloseBtn)
            interactiveItems.add(headerCloseBtn to { hideActionMenu() })
            cardContainer.addView(headerLayout)

            // Dynamic User App Shortcuts
            val customShortcuts = PreferencesManager.getInstance(context).getCustomShortcuts()
            if (customShortcuts.isNotEmpty()) {
                addSectionHeader(cardContainer, "QUICK APPS", density)
                var currentRow = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER
                }
                customShortcuts.forEachIndexed { index, shortcut ->
                    if (index > 0 && index % 3 == 0) {
                        cardContainer.addView(currentRow)
                        currentRow = LinearLayout(context).apply {
                            orientation = LinearLayout.HORIZONTAL
                            gravity = Gravity.CENTER
                        }
                    }
                    addAppShortcutButton(currentRow, shortcut, density)
                }
                cardContainer.addView(currentRow)
            }

            // System Navigation (Dynamic based on settings)
            val cfg = PreferencesManager.getInstance(context).configFlow.value
            val navItemEnums: List<CursorAction> = cfg.enabledActionMenuNavItems.mapNotNull { name ->
                try { CursorAction.valueOf(name) } catch (e: Exception) { null }
            }
            if (navItemEnums.isNotEmpty()) {
                addSectionHeader(cardContainer, "SYSTEM NAVIGATION", density)
                var currentRow = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER
                }
                navItemEnums.forEachIndexed { index: Int, action: CursorAction ->
                    if (index > 0 && index % 3 == 0) {
                        cardContainer.addView(currentRow)
                        currentRow = LinearLayout(context).apply {
                            orientation = LinearLayout.HORIZONTAL
                            gravity = Gravity.CENTER
                        }
                    }
                    val label = when (action) {
                        CursorAction.GLOBAL_BACK -> "Back"
                        CursorAction.GLOBAL_HOME -> "Home"
                        CursorAction.GLOBAL_RECENTS -> "Recents"
                        CursorAction.GLOBAL_NOTIFICATIONS -> "Notifications"
                        CursorAction.GLOBAL_QUICK_SETTINGS -> "Settings"
                        CursorAction.GLOBAL_LOCK_SCREEN -> "Lock"
                        CursorAction.GLOBAL_SCREENSHOT -> "Screenshot"
                        CursorAction.GLOBAL_POWER_DIALOG -> "Power Menu"
                        else -> action.label
                    }
                    addButton(currentRow, label, action, 0xFF334155.toInt(), density)
                }
                cardContainer.addView(currentRow)
            }

            // Cursor Tools (Dynamic based on settings)
            val toolItemEnums: List<CursorAction> = cfg.enabledActionMenuToolItems.mapNotNull { name ->
                try { CursorAction.valueOf(name) } catch (e: Exception) { null }
            }
            if (toolItemEnums.isNotEmpty()) {
                addSectionHeader(cardContainer, "CURSOR TOOLS", density)
                var currentRow = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER
                }
                toolItemEnums.forEachIndexed { index: Int, action: CursorAction ->
                    if (index > 0 && index % 3 == 0) {
                        cardContainer.addView(currentRow)
                        currentRow = LinearLayout(context).apply {
                            orientation = LinearLayout.HORIZONTAL
                            gravity = Gravity.CENTER
                        }
                    }
                    val pair: Pair<String, Int> = when (action) {
                        CursorAction.RECENTER_CALIBRATE -> Pair("Recenter", 0xFF0284C7.toInt())
                        CursorAction.TOGGLE_PRECISION_MODE -> Pair("Sniper (0.25x)", 0xFF2563EB.toInt())
                        CursorAction.TOGGLE_TRACKING -> Pair("Freeze", 0xFF475569.toInt())
                        CursorAction.DRAG_TOGGLE -> Pair("Drag Mode", 0xFF7C3AED.toInt())
                        CursorAction.HOLD_TO_DRAG -> Pair("Hold Drag", 0xFF6366F1.toInt())
                        CursorAction.TOGGLE_FLASHLIGHT, CursorAction.TOGGLE_TORCH -> Pair("Flashlight", 0xFF059669.toInt())
                        CursorAction.VOICE_TYPING -> Pair("Voice Type", 0xFFD97706.toInt())
                        else -> Pair(action.label, 0xFF334155.toInt())
                    }
                    addButton(currentRow, pair.first, action, pair.second, density)
                }
                cardContainer.addView(currentRow)
            }

            // Close button at bottom
            val closeBtn = TextView(context).apply {
                text = "✕ Close Action Menu"
                setTextColor(Color.WHITE)
                textSize = 14f
                paint.isFakeBoldText = true
                gravity = Gravity.CENTER
                val closeBg = GradientDrawable().apply {
                    setColor(0xFF334155.toInt())
                    cornerRadius = 16 * density
                    setStroke((1.5f * density).toInt(), 0x6694A3B8)
                }
                background = closeBg
                setPadding(0, (14 * density).toInt(), 0, (14 * density).toInt())
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, (14 * density).toInt(), 0, 0)
                }
                layoutParams = lp
                setOnClickListener { hideActionMenu() }
            }
            cardContainer.addView(closeBtn)
            interactiveItems.add(closeBtn to { hideActionMenu() })

            scrollView.addView(cardContainer)
            addView(scrollView)
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            if (event.action == MotionEvent.ACTION_UP) {
                handleClick(event.rawX, event.rawY)
            }
            return true
        }

        fun handleClick(x: Float, y: Float): Boolean {
            val loc = IntArray(2)
            val density = resources.displayMetrics.density
            val hitMargin = (14 * density).toInt()

            for ((view, callback) in interactiveItems) {
                if (view.isAttachedToWindow && view.isShown) {
                    view.getLocationOnScreen(loc)
                    val vx = loc[0]
                    val vy = loc[1]
                    val vw = view.width
                    val vh = view.height
                    if (x >= (vx - hitMargin) && x <= (vx + vw + hitMargin) &&
                        y >= (vy - hitMargin) && y <= (vy + vh + hitMargin)
                    ) {
                        view.animate().scaleX(0.92f).scaleY(0.92f).setDuration(60).withEndAction {
                            view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(80).start()
                            callback()
                        }.start()
                        return true
                    }
                }
            }

            // If outside cardContainer, dismiss menu
            cardContainer.getLocationOnScreen(loc)
            val cx = loc[0]
            val cy = loc[1]
            val cw = cardContainer.width
            val ch = cardContainer.height
            if (x < cx || x > cx + cw || y < cy || y > cy + ch) {
                hideActionMenu()
                return true
            }

            return true
        }

        fun updateHover(x: Float, y: Float) {
            val loc = IntArray(2)
            for ((view, _) in interactiveItems) {
                if (view.isAttachedToWindow && view.isShown) {
                    view.getLocationOnScreen(loc)
                    val vx = loc[0]
                    val vy = loc[1]
                    val vw = view.width
                    val vh = view.height
                    val isHovered = (x >= vx && x <= vx + vw && y >= vy && y <= vy + vh)
                    val targetScale = if (isHovered) 1.05f else 1.0f
                    if (view.scaleX != targetScale) {
                        view.animate().scaleX(targetScale).scaleY(targetScale).setDuration(70).start()
                    }
                }
            }
        }

        private fun addSectionHeader(parent: LinearLayout, title: String, density: Float) {
            val tv = TextView(context).apply {
                text = title
                textSize = 10f
                setTextColor(0xFF94A3B8.toInt())
                paint.isFakeBoldText = true
                setPadding((4 * density).toInt(), (8 * density).toInt(), 0, (4 * density).toInt())
            }
            parent.addView(tv)
        }

        private fun addAppShortcutButton(parent: LinearLayout, shortcut: CustomAppShortcut, density: Float) {
            val btn = TextView(context).apply {
                text = shortcut.label
                setTextColor(Color.WHITE)
                textSize = 12f
                gravity = Gravity.CENTER
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                val bg = GradientDrawable().apply {
                    setColor(shortcut.colorArgb.toInt())
                    cornerRadius = 12 * density
                }
                background = bg
                setPadding((6 * density).toInt(), (10 * density).toInt(), (6 * density).toInt(), (10 * density).toInt())
                val lp = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                ).apply {
                    setMargins((3 * density).toInt(), (3 * density).toInt(), (3 * density).toInt(), (3 * density).toInt())
                }
                layoutParams = lp
                val callback = {
                    hideActionMenu()
                    try {
                        val launchIntent = context.packageManager.getLaunchIntentForPackage(shortcut.packageName)
                        if (launchIntent != null) {
                            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(launchIntent)
                        } else {
                            val intent = Intent(Intent.ACTION_MAIN).apply {
                                addCategory(Intent.CATEGORY_LAUNCHER)
                                setPackage(shortcut.packageName)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                setOnClickListener { callback() }
                interactiveItems.add(this to callback)
            }
            parent.addView(btn)
        }

        private fun addCustomActionButton(parent: LinearLayout, label: String, bgColor: Int, density: Float, onClick: () -> Unit) {
            val btn = TextView(context).apply {
                text = label
                setTextColor(Color.WHITE)
                textSize = 12f
                gravity = Gravity.CENTER
                maxLines = 1
                val bg = GradientDrawable().apply {
                    setColor(bgColor)
                    cornerRadius = 12 * density
                }
                background = bg
                setPadding((6 * density).toInt(), (10 * density).toInt(), (6 * density).toInt(), (10 * density).toInt())
                val lp = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                ).apply {
                    setMargins((3 * density).toInt(), (3 * density).toInt(), (3 * density).toInt(), (3 * density).toInt())
                }
                layoutParams = lp
                setOnClickListener { onClick() }
                interactiveItems.add(this to onClick)
            }
            parent.addView(btn)
        }

        private fun addButton(parent: LinearLayout, label: String, action: CursorAction, bgColor: Int, density: Float) {
            val btn = TextView(context).apply {
                text = label
                setTextColor(Color.WHITE)
                textSize = 12f
                gravity = Gravity.CENTER
                maxLines = 1
                val bg = GradientDrawable().apply {
                    setColor(bgColor)
                    cornerRadius = 12 * density
                }
                background = bg
                setPadding((6 * density).toInt(), (10 * density).toInt(), (6 * density).toInt(), (10 * density).toInt())
                val lp = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                ).apply {
                    setMargins((3 * density).toInt(), (3 * density).toInt(), (3 * density).toInt(), (3 * density).toInt())
                }
                layoutParams = lp
                val callback = { onSelect(action) }
                setOnClickListener { callback() }
                interactiveItems.add(this to callback)
            }
            parent.addView(btn)
        }
    }
}

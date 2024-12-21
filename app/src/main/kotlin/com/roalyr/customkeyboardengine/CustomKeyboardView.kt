package com.roalyr.customkeyboardengine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.TypedValue
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View

class CustomKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    interface OnKeyboardActionListener {
        fun onKey(code: Int?, label: CharSequence?)
    }

    // Keyboard data.
    private var keyboard: CustomKeyboard? = null
    private var keys: List<Key>? = null

    // Drawing.
    private var buffer: Bitmap? = null
    private var paint: Paint = Paint()
    private val iconCache = mutableMapOf<String, Drawable?>()

    // TODO: Style data, move everything into json eventually.
    private val keyTextColor = context.resources.getColor(R.color.key_text_color, null)
    private val keySmallTextColor = context.resources.getColor(R.color.key_small_text_color, null)
    private val keyBackgroundColor = context.resources.getColor(R.color.key_background_color, null)
    private val keyModifierBackgroundColor = context.resources.getColor(R.color.key_background_color_modifier, null)

    // Scaling factors for rendering.
    private var scaleX = 1f
    private var scaleY = 1f

    // Logical dimensions of the keyboard (unscaled)
    private var logicalWidth = 0f
    private var logicalHeight = 0f

    // Rendered dimensions of the keyboard (scaled for display)
    private var renderedWidth = 0f
    private var renderedHeight = 0f


    private var keyboardActionListener: OnKeyboardActionListener? = null
    private var isShiftOn = false
    private var isCtrlOn = false
    private var isAltOn = false
    private var isCapsLockOn = false
    private var isKeyRepeated = false
    private var isLongPressHandled = false
    private var downKeyIndex = -1
    private var currentKey: Key? = null
    private var repeatKeyRunnable: Runnable? = null

    companion object {
        private const val TAG = "CustomKeyboardView"
        private const val MSG_LONGPRESS = 1
        private const val LONGPRESS_TIMEOUT = 250
        private const val REPEAT_DELAY = 50
        private const val REPEAT_START_DELAY = 250
    }

    ///////////////////////////////////
    // INIT
    init {
        isFocusable = true
        isFocusableInTouchMode = true
        paint.isAntiAlias = true
        paint.textAlign = Paint.Align.CENTER
        paint.alpha = 255
    }

    fun setKeyboard(keyboard: CustomKeyboard) {
        this.keyboard = keyboard
        keys = keyboard.getAllKeys()
        invalidateAllKeys()
    }


    /////////////////////////////////////
    // Handle events

    private val handlerCallback = Handler.Callback { msg ->
        if (msg.what == MSG_LONGPRESS) {
            val key = msg.obj as? Key
            if (key != null && !isLongPressHandled) {
                handleLongPress(key)
            }
            true
        } else {
            false
        }
    }

    private val handler = Handler(Looper.getMainLooper(), handlerCallback)

    private fun handleTouchDown(x: Int, y: Int) {
        val key = keyboard?.getKeyAt(x.toFloat(), y.toFloat()) ?: return

        downKeyIndex = keys?.indexOf(key) ?: -1
        isLongPressHandled = false
        isKeyRepeated = false

        // Start repeatable keys only if repeatable
        if (key.isRepeatable) {
            startKeyRepeat(key)
        } else {
            // Schedule long press for non-repeatable keys
            scheduleLongPress(key)
        }

        // Save key reference for later use in handleTouchUp
        currentKey = key
    }

    private fun handleTouchUp(x: Int, y: Int) {
        // Cancel repeat and long press behavior
        cancelRepeatKey()
        handler.removeMessages(MSG_LONGPRESS)

        currentKey?.let { key ->
            when {
                isLongPressHandled -> {
                    // Skip short press if long press is already handled
                }
                isKeyRepeated -> {
                    // Skip short press if key repeat is already handled
                }
                else -> {
                    keyboardActionListener?.onKey(key.keyCode, key.label)
                }
            }
        }

        // Reset states
        isLongPressHandled = false
        isKeyRepeated = false
        currentKey = null
    }

    private fun handleLongPress(key: Key) {
        isLongPressHandled = true
        keyboardActionListener?.onKey(key.keyCodeLongPress, key.smallLabel)
    }

    private fun scheduleLongPress(key: Key) {
        handler.obtainMessage(MSG_LONGPRESS, key).let {
            handler.sendMessageDelayed(it, LONGPRESS_TIMEOUT.toLong())
        }
    }

    private fun startKeyRepeat(key: Key) {
        repeatKeyRunnable = object : Runnable {
            override fun run() {
                isKeyRepeated = true // Mark repeat triggered
                keyboardActionListener?.onKey(key.keyCode, key.label)
                handler.postDelayed(this, REPEAT_DELAY.toLong())
            }
        }
        handler.postDelayed(repeatKeyRunnable!!, REPEAT_START_DELAY.toLong())
    }

    private fun cancelRepeatKey() {
        repeatKeyRunnable?.let {
            handler.removeCallbacks(it)
        }
        repeatKeyRunnable = null
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        requestFocus()
    }

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        return super.dispatchTouchEvent(event)
    }

    private fun isMetaKeyToggled(code: Int?): Boolean {
        return when (code) {
            KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT -> isShiftOn
            KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.KEYCODE_CTRL_RIGHT -> isCtrlOn
            KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.KEYCODE_ALT_RIGHT -> isAltOn
            KeyEvent.KEYCODE_CAPS_LOCK -> isCapsLockOn
            else -> false
        }
    }


    fun setOnKeyboardActionListener(listener: OnKeyboardActionListener) {
        keyboardActionListener = listener
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.action
        val scaledX = (event.x / scaleX).toInt()
        val scaledY = (event.y / scaleY).toInt()

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                handleTouchDown(scaledX, scaledY)
                performClick()
            }

            MotionEvent.ACTION_MOVE -> handleTouchMove(scaledX, scaledY)
            MotionEvent.ACTION_UP -> handleTouchUp(scaledX, scaledY)
        }
        return true
    }

    private fun handleTouchMove(x: Int, y: Int) {
        // Optional: Cancel repeat/long press if the finger moves off the key
        if (keyboard?.getKeyAt(x.toFloat(), y.toFloat()) == null) {
            cancelRepeatKey()
            handler.removeMessages(MSG_LONGPRESS)
        }
    }

    fun isLongPressing(): Boolean {
        return isLongPressHandled
    }




    /////////////////////////////////////
    // RENDERED VIEW SCALING LOGIC
    // Helper to calculate keyboard height dynamically in pixels
    private fun calculateKeyboardHeight(context: Context): Float {
        val keyboard = keyboard ?: return 0f
        var totalHeight = 0f

        keyboard.rows.forEach { row ->
            val rowHeight = row.rowHeight ?: keyboard.defaultKeyHeight
            totalHeight += rowHeight.toPx(context)
        }

        return totalHeight
    }

    private fun Float.toPx(context: Context): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, this, context.resources.displayMetrics
        )
    }



    /////////////////////////////////////
    // MEASURE VIEW DIMENSIONS
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Calculate the desired keyboard height in pixels
        val desiredHeight = calculateKeyboardHeight(context)

        // Measure width
        val measuredWidth = when (MeasureSpec.getMode(widthMeasureSpec)) {
            MeasureSpec.EXACTLY -> MeasureSpec.getSize(widthMeasureSpec)
            else -> suggestedMinimumWidth
        }

        // Measure height
        val measuredHeight = when (MeasureSpec.getMode(heightMeasureSpec)) {
            MeasureSpec.EXACTLY -> MeasureSpec.getSize(heightMeasureSpec).toFloat()
            else -> minOf(desiredHeight, MeasureSpec.getSize(heightMeasureSpec).toFloat())
        }

        // Store rendered dimensions
        renderedWidth = measuredWidth.toFloat()
        renderedHeight = measuredHeight

        // Set the measured dimensions
        setMeasuredDimension(measuredWidth, measuredHeight.toInt())
    }


    /////////////////////////////////////
    // ON SIZE CHANGE: CALCULATE SCALES
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        keyboard?.let { kb ->
            // Cache logical dimensions
            logicalWidth = kb.totalLogicalWidth.toFloat()
            logicalHeight = kb.totalLogicalHeight.toFloat()

            // Compute scaling factors
            scaleX = if (logicalWidth > 0) w / logicalWidth else 1f
            scaleY = if (logicalHeight > 0) h / logicalHeight else 1f

        }
    }


    ///////////////////////////////////////
    // DRAWING LOGIC
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val keyboard = keyboard ?: return
        val keys = keys ?: return

        val totalWidth = width.toFloat() // Screen width
        val scaleX = totalWidth / keyboard.totalLogicalWidth
        val scaleY = height.toFloat() / keyboard.totalLogicalHeight

        var currentY = 0f

        keyboard.rows.forEach { row ->
            val rowHeight = row.rowHeight?.times(scaleY) ?: (Constants.DEFAULT_KEY_HEIGHT * scaleY)
            val defaultKeyWidth = row.defaultKeyWidth?.times(scaleX) ?: (Constants.DEFAULT_KEY_WIDTH * scaleX)
            var currentX = 0f

            row.keys.forEach { key ->
                // Resolve key-specific height, fallback to row height
                val keyHeight = key.keyHeight?.times(scaleY) ?: rowHeight

                // Resolve key-specific width, fallback to row default
                val keyWidth = key.keyWidth?.times(scaleX) ?: defaultKeyWidth

                // Key position
                val keyX = currentX
                val keyY = currentY // Align with row baseline; do not adjust upwards

                // Key bounds for drawing
                val keyBounds = RectF(keyX, keyY, keyX + keyWidth, keyY + keyHeight)

                // Draw the key background
                paint.color = if (key.isModifier) keyModifierBackgroundColor else keyBackgroundColor
                canvas.drawRect(keyBounds, paint)

                // --- Draw key background ---
                paint.color = if (key.isModifier) keyModifierBackgroundColor else keyBackgroundColor
                canvas.drawRect(keyBounds, paint)

                // --- Draw icon if present ---
                key.icon?.let { iconName ->
                    val drawable = getIconDrawable(iconName)
                    drawable?.let {
                        val iconSize = (keyHeight * 0.6f).toInt()
                        val iconLeft = keyX.toInt() + ((keyWidth - iconSize) / 2).toInt()
                        val iconTop = keyY.toInt() + ((keyHeight - iconSize) / 2).toInt()
                        it.setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)
                        it.draw(canvas)
                    }
                } ?: run {
                    // Check if small label exists
                    if (key.smallLabel != null) {
                        // --- Draw small label (secondary text) ---
                        key.smallLabel?.let { smallLabel ->
                            paint.color = keySmallTextColor
                            paint.textSize = keyHeight * 0.3f
                            paint.textAlign = Paint.Align.CENTER

                            // Transform the label dynamically based on meta states
                            val renderedLabel = if (key.isModifier && isMetaKeyToggled(key.keyCode)) {
                                smallLabel.uppercase()
                            } else if (!key.isModifier && (isShiftOn || isCapsLockOn)) {
                                smallLabel.uppercase()
                            } else {
                                smallLabel.lowercase()
                            }

                            canvas.drawText(
                                renderedLabel,
                                keyBounds.centerX(),
                                keyBounds.top + keyHeight * 0.35f, // Upper half position
                                paint
                            )
                        }

                        // --- Draw primary label (main text) ---
                        key.label?.let { label ->
                            paint.color = keyTextColor
                            paint.textSize = keyHeight * 0.35f
                            paint.textAlign = Paint.Align.CENTER

                            // Transform the label dynamically based on meta states
                            val renderedLabel = if (key.isModifier && isMetaKeyToggled(key.keyCode)) {
                                label.uppercase()
                            } else if (!key.isModifier && (isShiftOn || isCapsLockOn)) {
                                label.uppercase()
                            } else {
                                label.lowercase()
                            }

                            canvas.drawText(
                                renderedLabel,
                                keyBounds.centerX(),
                                keyBounds.centerY() + keyHeight * 0.3f, // Slightly lower in the middle half
                                paint
                            )
                        }
                    } else {
                        // --- Draw primary label only (centered) ---
                        key.label?.let { label ->
                            paint.color = keyTextColor
                            paint.textSize = keyHeight * 0.4f
                            paint.textAlign = Paint.Align.CENTER

                            // Transform the label dynamically based on meta states
                            val renderedLabel = if (key.isModifier && isMetaKeyToggled(key.keyCode)) {
                                label.uppercase()
                            } else if (!key.isModifier && (isShiftOn || isCapsLockOn)) {
                                label.uppercase()
                            } else {
                                label.lowercase()
                            }

                            canvas.drawText(
                                renderedLabel,
                                keyBounds.centerX(),
                                keyBounds.centerY() + (paint.textSize / 3), // Centered vertically
                                paint
                            )
                        }
                    }
                }


                // Increment X for next key
                currentX += keyWidth + (key.keyGap?.times(scaleX) ?: 0f)
            }

            // Increment Y for the next row
            currentY += rowHeight + (row.rowGap?.times(scaleY) ?: 0f)
        }
    }

    fun updateMetaState(shiftOn: Boolean, ctrlOn: Boolean, altOn: Boolean, capsLockOn: Boolean) {
        isShiftOn = shiftOn
        isCtrlOn = ctrlOn
        isAltOn = altOn
        isCapsLockOn = capsLockOn
        invalidateAllKeys()
    }

    // Helper function to fetch drawable icons
    private fun getIconDrawable(iconName: String): Drawable? {
        return iconCache.getOrPut(iconName) {
            val resId = context.resources.getIdentifier(iconName, "drawable", context.packageName)
            if (resId != 0) context.getDrawable(resId) else null
        }
    }

    fun invalidateAllKeys() {
        buffer = null
        invalidate() // Forces the entire view to be redrawn
    }

}



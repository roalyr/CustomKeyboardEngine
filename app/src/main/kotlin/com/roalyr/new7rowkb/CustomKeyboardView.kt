package com.roalyr.new7rowkb

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
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
        fun onKey(primaryCode: Int, keyCodes: IntArray, label: CharSequence?)
        fun onText(text: CharSequence)
        fun onRelease(codes: Int)
    }

    private var keyboardActionListener: OnKeyboardActionListener? = null
    private var keyboard: CustomKeyboard? = null
    private var keys: List<Key>? = null
    private var buffer: Bitmap? = null
    private var paint: Paint = Paint()
    private val handler = Handler(Looper.getMainLooper()) // Ensure handler uses main thread

    private val handlerCallback = Handler.Callback { msg ->
        if (msg.what == MSG_LONGPRESS) {
            handleLongPress(msg.arg1) // Pass the long-press key code
            true
        } else {
            false
        }
    }
    private fun handleLongPress(keyCode: Int) {
        val key = keys?.find { it.codesLongPress == keyCode } ?: return
        isLongPressHandled = true
        Log.d("CustomKeyboardView", "Long Press Triggered for Key: ${key.label}")

        // Dispatch the long press action to the listener
        keyboardActionListener?.onKey(key.codesLongPress, intArrayOf(key.codesLongPress), key.label)
    }


    private var downKeyIndex = -1

    private var isLongPressHandled = false
    private var repeatKeyRunnable: Runnable? = null
    private val repeatDelay = 50L

    private val keyTextColor = context.resources.getColor(R.color.key_text_color, null)
    private val keySmallTextColor = context.resources.getColor(R.color.key_small_text_color, null)
    private val keyBackgroundColor = context.resources.getColor(R.color.key_background_color, null)
    private val keyModifierBackgroundColor = context.resources.getColor(R.color.key_background_color_modifier, null)

    // Icon cache to prevent repeated resource lookups
    private val iconCache = mutableMapOf<String, Drawable?>()

    private var isShiftOn = false
    private var isCtrlOn = false
    private var isAltOn = false
    private var isCapsLockOn = false

    private var scaleX = 1f
    private var scaleY = 1f


    init {
        isFocusable = true
        isFocusableInTouchMode = true
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        requestFocus()
    }

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        return super.dispatchTouchEvent(event)
    }



    fun updateMetaState(shiftOn: Boolean, ctrlOn: Boolean, altOn: Boolean, capsLockOn: Boolean) {
        isShiftOn = shiftOn
        isCtrlOn = ctrlOn
        isAltOn = altOn
        isCapsLockOn = capsLockOn

        keys?.forEach { key ->
            if (key.modifier) {
                // Update meta key labels dynamically based on their state
                key.label = key.label?.let {
                    if (isMetaKeyToggled(key.codes)) it.uppercase() else it.lowercase()
                }
            } else {
                // Update non-modifier key labels dynamically
                key.label = key.label?.let {
                    if (shiftOn || capsLockOn) it.uppercase() else it.lowercase()
                }
            }
        }
        invalidateAllKeys()
    }

    private fun isMetaKeyToggled(code: Int): Boolean {
        return when (code) {
            KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT -> isShiftOn
            KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.KEYCODE_CTRL_RIGHT -> isCtrlOn
            KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.KEYCODE_ALT_RIGHT -> isAltOn
            KeyEvent.KEYCODE_CAPS_LOCK -> isCapsLockOn
            else -> false
        }
    }



    // Start repeating a key
    private fun startKeyRepeat(key: Key) {
        repeatKeyRunnable = object : Runnable {
            override fun run() {
                keyboardActionListener?.onKey(key.codes, intArrayOf(key.codes), key.label)
                handler?.postDelayed(this, repeatDelay)
            }
        }
        handler?.postDelayed(repeatKeyRunnable!!, LONGPRESS_TIMEOUT.toLong())
    }

    // Schedule a long press
    private fun scheduleLongPress(key: Key) {
        handler?.obtainMessage(MSG_LONGPRESS, key.codesLongPress, 0)?.let {
            handler?.sendMessageDelayed(
                it,
                LONGPRESS_TIMEOUT.toLong()
            )
        }
    }

    private fun cancelRepeatKey() {
        repeatKeyRunnable?.let {
            handler?.removeCallbacks(it)
        }
        repeatKeyRunnable = null
    }

    companion object {
        private const val TAG = "CustomKeyboardView"
        private const val MSG_LONGPRESS = 1
        private const val LONGPRESS_TIMEOUT = 250
    }

    init {
        paint.isAntiAlias = true
        paint.textAlign = Paint.Align.CENTER
        paint.alpha = 255
    }

    fun setKeyboard(keyboard: CustomKeyboard) {
        this.keyboard = keyboard
        keys = keyboard.getAllKeys()
        invalidateAllKeys()
    }

    fun setOnKeyboardActionListener(listener: OnKeyboardActionListener) {
        keyboardActionListener = listener
    }





    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val keyboard = keyboard ?: return
        val keys = keys ?: return

        val totalWidth = width.toFloat() // View width
        val totalHeight = height.toFloat() // View height
        val totalLogicalHeight = keyboard.totalLogicalHeight.toFloat()

        var currentY = 0f

        keyboard.rows.forEach { row ->
            // Scale row height proportionally
            val rowHeight = (row.height / totalLogicalHeight) * totalHeight
            var currentX = 0f

            row.keys.forEach { key ->
                // Scale key dimensions dynamically
                val keyWidth = (key.width / 100f) * totalWidth
                val keyHeight = rowHeight

                val keyX = currentX
                val keyY = currentY

                // Update logical key positions for touch detection
                key.x = (keyX / scaleX).toInt()
                key.y = (keyY / scaleY).toInt()
                key.width = (keyWidth / scaleX).toInt()
                key.height = (keyHeight / scaleY).toInt()

                // Draw key background
                val keyBounds = RectF(keyX, keyY, keyX + keyWidth, keyY + keyHeight)
                paint.color = if (key.modifier) keyModifierBackgroundColor else keyBackgroundColor
                canvas.drawRect(keyBounds, paint)

                // Draw key label
                key.label?.let { label ->
                    paint.color = keyTextColor
                    paint.textSize = keyHeight * 0.4f
                    paint.textAlign = Paint.Align.CENTER
                    canvas.drawText(label, keyBounds.centerX(), keyBounds.centerY() + (paint.textSize / 3), paint)
                }

                currentX += keyWidth
            }
            currentY += rowHeight
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        keyboard?.let { keyboard ->
            scaleX = w.toFloat() / keyboard.totalLogicalWidth.toFloat()
            scaleY = h.toFloat() / keyboard.totalLogicalHeight.toFloat()
            Log.d(TAG, "ScaleX: $scaleX, ScaleY: $scaleY")
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.action
        val scaledX = (event.x / scaleX).toInt()
        val scaledY = (event.y / scaleY).toInt()

        Log.d(TAG, "Raw Touch - X: ${event.x}, Y: ${event.y}")
        Log.d(TAG, "Scaled Touch - X: $scaledX, Y: $scaledY")

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




    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredHeight = calculateKeyboardHeight(context)

        // Measure the width
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val measuredWidth = if (widthMode == MeasureSpec.EXACTLY) {
            widthSize
        } else {
            suggestedMinimumWidth
        }

        // Measure the height
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val measuredHeight = if (heightMode == MeasureSpec.EXACTLY) {
            heightSize
        } else {
            desiredHeight.coerceAtMost(heightSize) // Use content height
        }

        // Apply the measured dimensions
        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    // Helper to calculate keyboard height dynamically
    fun calculateKeyboardHeight(context: Context): Int {
        val keyboard = keyboard ?: return 0
        return keyboard.rows.sumOf { row -> row.height.toPx(context) }
    }

    // Convert dp to pixels
    private fun Int.toPx(context: Context): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), context.resources.displayMetrics
        ).toInt()
    }



    private fun drawKey(canvas: Canvas, key: Key) {
        // Total keyboard dimensions
        val totalWidth = width
        val totalHeight = height

        // Calculate actual dimensions
        val keyWidth = (key.width / 100f) * totalWidth
        val keyHeight = (key.height / 100f) * totalHeight
        val keyX = (key.x / 100f) * totalWidth
        val keyY = (key.y / 100f) * totalHeight

        val keyBounds = Rect(keyX.toInt(), keyY.toInt(), (keyX + keyWidth).toInt(), (keyY + keyHeight).toInt())

        // Draw background
        paint.color = if (key.modifier) keyModifierBackgroundColor else keyBackgroundColor
        canvas.drawRect(keyBounds, paint)

        // Draw icon if present
        key.icon?.let { iconName ->
            val drawable = getIconDrawable(iconName)
            drawable?.let {
                val iconSize = (keyHeight * 0.6).toInt()
                val iconLeft = keyX.toInt() + ((keyWidth - iconSize) / 2).toInt()
                val iconTop = keyY.toInt() + ((keyHeight - iconSize) / 2).toInt()
                it.setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)
                it.draw(canvas)
                return
            }
        }

        // Draw small label
        key.labelSmall?.let { smallLabel ->
            paint.color = keySmallTextColor
            paint.textSize = keyHeight * 0.2f
            canvas.drawText(
                smallLabel,
                keyX + keyWidth / 2f,
                keyY + keyHeight * 0.3f,
                paint.apply { textAlign = Paint.Align.CENTER }
            )
        }

        // Draw primary label
        key.label?.let { label ->
            paint.color = keyTextColor
            paint.textSize = keyHeight * 0.4f
            canvas.drawText(
                label,
                keyX + keyWidth / 2f,
                keyY + keyHeight * 0.65f,
                paint.apply { textAlign = Paint.Align.CENTER }
            )
        }
    }



    // Helper function to fetch drawable icons
    private fun getIconDrawable(iconName: String): Drawable? {
        return iconCache.getOrPut(iconName) {
            val resId = context.resources.getIdentifier(iconName, "drawable", context.packageName)
            if (resId != 0) context.getDrawable(resId) else null
        }
    }


    private fun handleTouchDown(x: Int, y: Int) {
        Log.d("CustomKeyboardView", "handleTouchDown - Scaled Touch X: $x, Y: $y")

        val key = keyboard?.getKeyAt(x, y) ?: return
        Log.d("CustomKeyboardView", "Key Pressed: ${key.label}, Bounds: X=${key.x}, Y=${key.y}, Width=${key.width}, Height=${key.height}")

        downKeyIndex = keys?.indexOf(key) ?: -1
        isLongPressHandled = false

        // Trigger the key press immediately
        keyboardActionListener?.onKey(key.codes, intArrayOf(key.codes), key.label)

        // Handle repeatable and long press behavior
        when {
            key.repeatable -> startKeyRepeat(key)
            else -> scheduleLongPress(key)
        }
    }




    private fun handleTouchMove(x: Int, y: Int) {
        // Optional: Cancel repeat/long press if the finger moves off the key
        if (keyboard?.getKeyAt(x, y) == null) {
            cancelRepeatKey()
            handler?.removeMessages(MSG_LONGPRESS)
        }
    }

    fun isLongPressing(): Boolean {
        return isLongPressHandled
    }


    private fun handleTouchUp(x: Int, y: Int) {
        Log.d("CustomKeyboardView", "handleTouchUp - X: $x, Y: $y")

        // Cancel repeat and long press behavior
        cancelRepeatKey()
        handler.removeMessages(MSG_LONGPRESS)

        val key = keyboard?.getKeyAt(x, y)
        if (key != null && !isLongPressHandled) {
            // Handle a regular key tap if long press was not triggered
            Log.d("CustomKeyboardView", "Key Released: ${key.label}")
            keyboardActionListener?.onRelease(key.codes)
        }
    }


    fun invalidateAllKeys() {
        buffer = null
        invalidate()
    }
}



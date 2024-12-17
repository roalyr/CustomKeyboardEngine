package com.roalyr.new7rowkb

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Message
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
    }

    private var mKeyboardActionListener: OnKeyboardActionListener? = null
    private var mKeyboard: CustomKeyboard? = null
    private var mKeys: List<Key>? = null
    private var mBuffer: Bitmap? = null
    private var mCanvas: Canvas? = null
    private var mPaint: Paint = Paint()
    private var mHandler: Handler? = null

    private var mDrawPending: Boolean = false

    private var mDownKeyIndex = -1

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

    init {
        isFocusable = true
        isFocusableInTouchMode = true
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        requestFocus()
    }

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        //Log.d("CustomKeyboardView", "Dispatch Touch Event: ${event?.action}")
        return super.dispatchTouchEvent(event)
    }



    fun updateMetaState(shiftOn: Boolean, ctrlOn: Boolean, altOn: Boolean, capsLockOn: Boolean) {
        isShiftOn = shiftOn
        isCtrlOn = ctrlOn
        isAltOn = altOn
        isCapsLockOn = capsLockOn

        mKeys?.forEach { key ->
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
                mKeyboardActionListener?.onKey(key.codes, intArrayOf(key.codes), key.label)
                mHandler?.postDelayed(this, repeatDelay)
            }
        }
        mHandler?.postDelayed(repeatKeyRunnable!!, LONGPRESS_TIMEOUT.toLong())
    }

    // Schedule a long press
    private fun scheduleLongPress(key: Key) {
        mHandler?.obtainMessage(MSG_LONGPRESS, key.codesLongPress, 0)?.let {
            mHandler?.sendMessageDelayed(
                it,
                LONGPRESS_TIMEOUT.toLong()
            )
        }
    }

    private fun cancelRepeatKey() {
        repeatKeyRunnable?.let {
            mHandler?.removeCallbacks(it)
        }
        repeatKeyRunnable = null
    }

    companion object {
        private const val TAG = "CustomKeyboardView"
        private const val MSG_LONGPRESS = 4
        private const val LONGPRESS_TIMEOUT = 500
    }

    init {
        mPaint.isAntiAlias = true
        mPaint.textAlign = Paint.Align.CENTER
        mPaint.alpha = 255
        setupHandler()
    }

    fun setKeyboard(keyboard: CustomKeyboard) {
        mKeyboard = keyboard
        mKeys = keyboard.getAllKeys()
        invalidateAllKeys()
    }

    fun setOnKeyboardActionListener(listener: OnKeyboardActionListener) {
        mKeyboardActionListener = listener
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mBuffer = null
        mKeyboard?.resize(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val keyboard = mKeyboard ?: return // Use mKeyboard here
        val keys = mKeys ?: return // Ensure keys are not null

        if (keys.isEmpty()) {
            Log.e("CustomKeyboardView", "No keys to render in keyboard.")
            return
        }

        val totalWidth = width.toFloat() // Full screen width
        val totalKeyboardHeight = keyboard.rows.sumOf { it.height.toPx(context) }.toFloat() // Sum of row heights in pixels

        var currentY = 0f

        keyboard.rows.forEach { row ->
            // Convert row height from dp to pixels
            val rowHeight = row.height.toPx(context)

            var currentX = 0f

            // Calculate scaling factor if row width exceeds 100%
            val totalRowWidth = row.keys.sumOf { it.width + it.gap }.toFloat()
            val scale = if (totalRowWidth > 100f) 100f / totalRowWidth else 1f

            row.keys.forEach { key ->
                // Scale key dimensions dynamically
                val keyWidth = ((key.width * scale) / 100f) * totalWidth
                val keyHeight = rowHeight
                val keyX = currentX
                val keyY = currentY

                // Draw key background
                val keyBounds = RectF(keyX, keyY, keyX + keyWidth, keyY + keyHeight)
                mPaint.color = if (key.modifier) keyModifierBackgroundColor else keyBackgroundColor
                canvas.drawRect(keyBounds, mPaint)

                // Draw key label
                key.label?.let { label ->
                    mPaint.color = keyTextColor
                    mPaint.textSize = keyHeight * 0.4f
                    mPaint.textAlign = Paint.Align.CENTER
                    val centerX = keyBounds.centerX()
                    val centerY = keyBounds.centerY() + (mPaint.textSize / 3)
                    canvas.drawText(label, centerX, centerY, mPaint)
                }

                // Increment x position for next key
                currentX += keyWidth + ((key.gap * scale) / 100f) * totalWidth
            }

            // Increment Y position for the next row
            currentY += rowHeight
        }
        mKeys?.forEach { key ->
            Log.d("CustomKeyboardView", "Key: ${key.label}, X: ${key.x}, Y: ${key.y}, Width: ${key.width}, Height: ${key.height}")
        }

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
        val keyboard = mKeyboard ?: return 0
        return keyboard.rows.sumOf { row -> row.height.toPx(context) }
    }

    // Convert dp to pixels
    private fun Int.toPx(context: Context): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), context.resources.displayMetrics
        ).toInt()
    }


    fun getKeyboard(): CustomKeyboard? {
        return mKeyboard
    }

    private fun onBufferDraw() {
        val width = Math.max(1, width)
        val height = Math.max(1, height)
        if (mBuffer == null || mBuffer!!.width != width || mBuffer!!.height != height) {
            mBuffer = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            mCanvas = Canvas(mBuffer!!)
        }
        mCanvas?.drawColor(0, PorterDuff.Mode.CLEAR)

        mKeys?.forEach { key ->
            drawKey(mCanvas!!, key)
        }
        mDrawPending = false
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
        mPaint.color = if (key.modifier) keyModifierBackgroundColor else keyBackgroundColor
        canvas.drawRect(keyBounds, mPaint)

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
            mPaint.color = keySmallTextColor
            mPaint.textSize = keyHeight * 0.2f
            canvas.drawText(
                smallLabel,
                keyX + keyWidth / 2f,
                keyY + keyHeight * 0.3f,
                mPaint.apply { textAlign = Paint.Align.CENTER }
            )
        }

        // Draw primary label
        key.label?.let { label ->
            mPaint.color = keyTextColor
            mPaint.textSize = keyHeight * 0.4f
            canvas.drawText(
                label,
                keyX + keyWidth / 2f,
                keyY + keyHeight * 0.65f,
                mPaint.apply { textAlign = Paint.Align.CENTER }
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


    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.action
        val touchX = event.x.toInt()
        val touchY = event.y.toInt()

        //Log.d("CustomKeyboardView", "Touch Event - Action: $action, X: $touchX, Y: $touchY")

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                Log.d("CustomKeyboardView", "Action Down detected, X: $touchX, Y: $touchY")
                handleTouchDown(touchX, touchY)
                performClick() // Important for accessibility
            }
            MotionEvent.ACTION_MOVE -> {
                //Log.d("CustomKeyboardView", "Action Move detected")
                handleTouchMove(touchX, touchY)
            }
            MotionEvent.ACTION_UP -> {
                //Log.d("CustomKeyboardView", "Action Up detected")
                handleTouchUp(touchX, touchY)
            }
            else -> {
                Log.d("CustomKeyboardView", "Unknown Action: $action")
            }
        }
        return true // Always consume the event
    }

    override fun performClick(): Boolean {
        //Log.d("CustomKeyboardView", "performClick triggered")
        return super.performClick()
    }



    private fun handleTouchDown(x: Int, y: Int) {
        val key = mKeyboard?.getKeyAt(x, y) ?: return // Safely retrieve the key at the given coordinates

        // Determine the index of the key being pressed
        mDownKeyIndex = mKeys?.indexOf(key) ?: -1
        isLongPressHandled = false

        Log.d("CustomKeyboardView", "Key Pressed: ${key.label}, Repeatable: ${key.repeatable}")

        // Handle repeatable and long press behavior
        when {
            key.repeatable -> startKeyRepeat(key) // Start key repeat behavior
            else -> scheduleLongPress(key)       // Schedule a long-press check
        }
    }


    private fun handleTouchMove(x: Int, y: Int) {
        // Optional: Cancel repeat/long press if the finger moves off the key
        if (mKeyboard?.getKeyAt(x, y) == null) {
            cancelRepeatKey()
            mHandler?.removeMessages(MSG_LONGPRESS)
        }
    }

    fun isLongPressing(): Boolean {
        return isLongPressHandled
    }


    private fun handleTouchUp(x: Int, y: Int) {
        cancelRepeatKey()
        mHandler?.removeMessages(MSG_LONGPRESS)
        isLongPressHandled = false // Reset long-press state
        mDownKeyIndex = -1
    }


    private fun setupHandler() {
        mHandler = object : Handler() {
            override fun handleMessage(msg: Message) {
                if (msg.what == MSG_LONGPRESS && !isLongPressHandled) {
                    val key = mKeyboard?.getKeyByCode(msg.arg1)
                    key?.let {
                        mKeyboardActionListener?.onKey(it.codesLongPress, intArrayOf(it.codesLongPress), it.labelSmall)
                        isLongPressHandled = true
                    }
                }
            }
        }
    }

    fun invalidateAllKeys() {
        mBuffer = null
        invalidate()
    }
}

fun CustomKeyboard.resize(newWidth: Int, newHeight: Int) {
    rows.forEach { row ->
        var x = 0
        row.keys.forEach { key ->
            // Scale key dimensions and positions based on percentages
            key.width = (key.width / 100f * newWidth).toInt()
            key.height = (key.height / 100f * newHeight).toInt()
            key.x = x
            x += key.width + (key.gap / 100f * newWidth).toInt()
        }
    }
}

package com.roalyr.new7rowkb

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Message
import android.util.AttributeSet
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
    private var mAbortKey: Boolean = false
    private var mDefaultVerticalGap: Int = 0
    private var shifted: Boolean = false

    private var mLastKeyIndex = -1
    private var mCurrentKeyIndex = -1
    private var mDownKeyIndex = -1

    private var repeatKeyIndex = -1

    private var isLongPressHandled = false
    private var repeatKeyRunnable: Runnable? = null
    private val repeatDelay = 50L

    private val keyTextColor = context.resources.getColor(R.color.key_text_color, null)
    private val keySmallTextColor = context.resources.getColor(R.color.key_small_text_color, null)
    private val keyBackgroundColor = context.resources.getColor(R.color.key_background_color, null)
    private val keyModifierBackgroundColor = context.resources.getColor(R.color.key_background_color_modifier, null)

    // Icon cache to prevent repeated resource lookups
    private val iconCache = mutableMapOf<String, Drawable?>()

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
        if (mDrawPending || mBuffer == null) {
            onBufferDraw()
        }
        canvas.drawBitmap(mBuffer!!, 0f, 0f, null)
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
        val keyBounds = Rect(key.x, key.y, key.x + key.width, key.y + key.height)

        // Draw background color
        mPaint.color = if (key.modifier) keyModifierBackgroundColor else keyBackgroundColor
        canvas.drawRect(keyBounds, mPaint)

        // Draw icon if present
        key.icon?.let { iconName ->
            val drawable = getIconDrawable(iconName)
            drawable?.let {
                val iconSize = (key.height * 0.6).toInt() // Icon size at 60% of key height
                val iconLeft = key.x + (key.width - iconSize) / 2
                val iconTop = key.y + (key.height - iconSize) / 2
                it.setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)
                it.draw(canvas)
                return // If icon is drawn, skip further label drawing
            }
        }

        // Draw small label if available
        key.labelSmall?.let { smallLabel ->
            mPaint.color = keySmallTextColor
            mPaint.textSize = key.height * 0.2f
            canvas.drawText(
                smallLabel,
                key.x + key.width / 2f,
                key.y + key.height * 0.3f, // Position small label above the primary label
                mPaint
            )
        }

        // Draw primary label
        key.label?.let { label ->
            mPaint.color = keyTextColor
            mPaint.textSize = key.height * 0.4f
            canvas.drawText(
                label,
                key.x + key.width / 2f,
                key.y + key.height * 0.65f, // Lower to leave space for the small label
                mPaint
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

        when (action) {
            MotionEvent.ACTION_DOWN -> handleTouchDown(touchX, touchY)
            MotionEvent.ACTION_MOVE -> handleTouchMove(touchX, touchY)
            MotionEvent.ACTION_UP -> handleTouchUp(touchX, touchY)
        }
        return true
    }

    private fun handleTouchDown(x: Int, y: Int) {
        val key = mKeyboard?.getKeyAt(x, y)
        if (key != null) {
            mDownKeyIndex = mKeys?.indexOf(key) ?: -1
            isLongPressHandled = false

            if (key.repeatable) {
                startKeyRepeat(key)
            } else {
                scheduleLongPress(key)
            }
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

// Updated CustomKeyboard method:
fun CustomKeyboard.resize(newWidth: Int, newHeight: Int) {
    rows.forEach { row ->
        var x = 0
        val scaleFactor = newWidth / row.keys.sumOf { it.width + it.gap }.toFloat()
        row.keys.forEach { key ->
            key.width = (key.width * scaleFactor).toInt()
            key.x = x
            x += key.width + key.gap
        }
    }
}

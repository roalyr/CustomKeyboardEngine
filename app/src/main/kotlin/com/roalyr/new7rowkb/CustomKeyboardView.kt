package com.roalyr.new7rowkb
import android.content.Context
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Message
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import java.lang.ref.WeakReference

class CustomKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    interface OnKeyboardActionListener {
        fun onPress(primaryCode: Int)
        fun onRelease(primaryCode: Int)
        fun onKey(primaryCode: Int, keyCodes: IntArray, label: CharSequence?)
        fun onText(text: CharSequence)
    }

    private var mKeyboardActionListener: OnKeyboardActionListener? = null
    private var mKeyboard: CustomKeyboard? = null
    private var mKeys: List<Key>? = null
    private var mKeyboardChanged: Boolean = false
    private var mAbortKey: Boolean = false
    private var mDefaultVerticalGap: Int = 0
    private var shifted: Boolean = false

    private var mPaint: Paint
    private var mPadding: Rect = Rect(0, 0, 0, 0)
    private var mKeyBackground: Drawable? = null
    private var mKeyBackgroundModifier: Drawable? = null
    private var mVerticalCorrection: Int = 0
    private var keyTextSize: Int = 18
    private var mKeyTextColor: Int = 0xFF000000.toInt()
    private var mLabelTextSize: Int = 14
    private var mShadowColor: Int = 0
    private var mShadowRadius: Float = 0f
    private var mKeySmallTextColor: Int = 0xFF000000.toInt()
    private var mKeyBackgroundTop: Drawable? = null
    private var mKeyBackgroundBottom: Drawable? = null

    private val mHandler = CustomKeyboardHandler(this)

    companion object {
        const val MSG_SHOW_PREVIEW = 1
        const val MSG_REMOVE_PREVIEW = 2
        const val MSG_REPEAT = 3
        const val MSG_LONGPRESS = 4
        const val REPEAT_INTERVAL = 50
    }

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.KeyboardView, defStyleAttr, 0)

        mKeyBackground = a.getDrawable(R.styleable.KeyboardView_keyBackground)
        mKeyBackgroundModifier = a.getDrawable(R.styleable.KeyboardView_keyBackgroundModifier)
        mVerticalCorrection = a.getDimensionPixelOffset(R.styleable.KeyboardView_verticalCorrection, 0)
        keyTextSize = a.getDimensionPixelSize(R.styleable.KeyboardView_keyTextSize, 18)
        mKeyTextColor = a.getColor(R.styleable.KeyboardView_keyTextColor, 0xFF000000.toInt())
        mLabelTextSize = a.getDimensionPixelSize(R.styleable.KeyboardView_labelTextSize, 14)
        mShadowColor = a.getColor(R.styleable.KeyboardView_shadowColor, 0)
        mShadowRadius = a.getFloat(R.styleable.KeyboardView_shadowRadius, 0f)

        // Custom attributes
        mKeySmallTextColor = a.getColor(R.styleable.KeyboardView_keySmallTextColor, 0xFF000000.toInt())
        mKeyBackgroundTop = a.getDrawable(R.styleable.KeyboardView_keyBackgroundTop)
        mKeyBackgroundBottom = a.getDrawable(R.styleable.KeyboardView_keyBackgroundBottom)

        // Initialize padding from key background
        mKeyBackground?.getPadding(mPadding)

        a.recycle()

        mPaint = Paint().apply {
            isAntiAlias = true
            textSize = keyTextSize.toFloat()
            textAlign = Paint.Align.CENTER
            alpha = 255
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // Handler is initialized as static, no need for re-initialization
    }

    fun setOnKeyboardActionListener(listener: OnKeyboardActionListener) {
        mKeyboardActionListener = listener
    }

    fun setKeyboard(keyboard: CustomKeyboard) {
        if (mKeyboard != null) {
            showKey(-1) // Hide preview
        }
        mKeyboard = keyboard
        mKeys = keyboard.getAllKeys()
        requestLayout()
        mKeyboardChanged = true
        invalidate() // Redraw keys
        mAbortKey = true // Cancel pending keys
        mDefaultVerticalGap = keyboard.verticalGap // Custom vertical gap
    }

    fun isShifted(): Boolean {
        return shifted
    }

    fun setShifted(shifted: Boolean) {
        this.shifted = shifted
    }

    fun toggleShift() {
        shifted = !shifted
    }

    private fun adjustCase(label: CharSequence?): CharSequence? {
        return if (isShifted() && label != null && label.length < 3 && Character.isLowerCase(label[0])) {
            label.toString().uppercase()
        } else {
            label
        }
    }

    private fun showKey(keyIndex: Int) {
        Log.d("CustomKeyboardView", "Showing key: $keyIndex")
    }

    private fun hideKeyPreview() {
        Log.d("CustomKeyboardView", "Hiding key preview")
    }

    private fun repeatKey(): Boolean {
        Log.d("CustomKeyboardView", "Repeating key")
        return true
    }

    private fun handleLongPress(event: MotionEvent) {
        Log.d("CustomKeyboardView", "Handling long press at (${event.x}, ${event.y})")
    }

    private fun resetMultiTap() {
        // Implement multi-tap logic if needed
    }

    private class CustomKeyboardHandler(view: CustomKeyboardView) : Handler() {
        private val viewRef = WeakReference(view)

        override fun handleMessage(msg: Message) {
            val view = viewRef.get() ?: return
            when (msg.what) {
                MSG_SHOW_PREVIEW -> view.showKey(msg.arg1)
                MSG_REMOVE_PREVIEW -> view.hideKeyPreview()
                MSG_REPEAT -> if (view.repeatKey()) {
                    sendMessageDelayed(obtainMessage(MSG_REPEAT), REPEAT_INTERVAL.toLong())
                }
                MSG_LONGPRESS -> view.handleLongPress(msg.obj as MotionEvent)
            }
        }
    }
}

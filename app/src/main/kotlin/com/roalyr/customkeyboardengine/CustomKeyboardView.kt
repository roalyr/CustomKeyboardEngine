package com.roalyr.customkeyboardengine

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
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
        fun onKey(code: Int?, label: CharSequence?)
    }

    // Keyboard data.
    var keyboard: CustomKeyboard? = null
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

    private var repeatKeyRunnable: Runnable? = null

    private val activeKeys = mutableMapOf<Int, Key?>() // Track active keys by pointer ID

    // Define specific meta key codes for shifting case.
    private val metaKeyCodes = arrayOf(
        KeyEvent.KEYCODE_SHIFT_LEFT,
        KeyEvent.KEYCODE_SHIFT_RIGHT,
        KeyEvent.KEYCODE_CTRL_LEFT,
        KeyEvent.KEYCODE_CTRL_RIGHT,
        KeyEvent.KEYCODE_ALT_LEFT,
        KeyEvent.KEYCODE_ALT_RIGHT,
        KeyEvent.KEYCODE_CAPS_LOCK
    )

    companion object {
        private const val TAG = "CustomKeyboardView"
        private const val MSG_LONGPRESS = 1
        private const val LONGPRESS_TIMEOUT = 250
        private const val REPEAT_DELAY = 50
        private const val REPEAT_START_DELAY = 250
    }

    // INIT
    init {
        isFocusable = true
        isFocusableInTouchMode = true
        paint.isAntiAlias = true
        paint.textAlign = Paint.Align.CENTER
        paint.alpha = 255
    }

    fun updateKeyboard(keyboard: CustomKeyboard) {
        this.keyboard = keyboard
        keys = keyboard.getAllKeys()
        invalidateAllKeys()
    }

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

    private fun handleTouchDown(x: Int, y: Int, pointerId: Int) {
        val key = keyboard?.getKeyAt(x.toFloat(), y.toFloat()) ?: return

        downKeyIndex = keys?.indexOf(key) ?: -1
        isLongPressHandled = false
        isKeyRepeated = false

        // Start repeatable keys only if repeatable
        if (key.isRepeatable == true) {
            startKeyRepeat(key)
        } else {
            // Schedule long press for non-repeatable keys
            scheduleLongPress(key)
        }

        // Save key reference in the activeKeys map
        activeKeys[pointerId] = key
    }


    private fun handleTouchUp(pointerId: Int) {
        cancelRepeatKey()
        handler.removeMessages(MSG_LONGPRESS)

        // Retrieve the key associated with this pointer ID
        val key = activeKeys[pointerId]

        key?.let {
            when {
                isLongPressHandled -> {
                    return
                }
                isKeyRepeated -> {
                    return
                }
                else -> {
                    keyboardActionListener?.onKey(it.keyCode, it.label)
                }
            }
        }

        // Reset states for this pointer
        isLongPressHandled = false
        isKeyRepeated = false
        activeKeys.remove(pointerId)
    }



    private fun handleLongPress(key: Key) {
        isLongPressHandled = true
        keyboardActionListener?.onKey(key.keyCodeLongPress, key.labelLongPress)
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

    fun cancelAllEvents() {
        cancelRepeatKey()
        handler.removeMessages(MSG_LONGPRESS)
        isKeyRepeated = false
        isLongPressHandled = false
        activeKeys.clear()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cancelAllEvents()
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (!hasWindowFocus) {
            cancelAllEvents()
        }
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility != VISIBLE) {
            cancelAllEvents()
        }
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
        val actionMasked = event.actionMasked // Use actionMasked for multi-touch support
        val pointerIndex = event.actionIndex // Index of the pointer causing the event
        val pointerId = event.getPointerId(pointerIndex)

        when (actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                // Handle new touch
                val scaledX = (event.getX(pointerIndex) / scaleX).toInt()
                val scaledY = (event.getY(pointerIndex) / scaleY).toInt()
                handleTouchDown(scaledX, scaledY, pointerId)
                performClick()
                //Log.d("TOUCH", "Pointer Down: Index $pointerIndex, x=$scaledX, y=$scaledY")
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                // Handle touch release
                val scaledX = (event.getX(pointerIndex) / scaleX).toInt()
                val scaledY = (event.getY(pointerIndex) / scaleY).toInt()
                handleTouchUp(pointerId)
                performClick()
                //Log.d("TOUCH", "Pointer Up: Index $pointerIndex, x=$scaledX, y=$scaledY")
            }

            MotionEvent.ACTION_MOVE -> {
                // Handle touch move for all active pointers
                //for (i in 0 until event.pointerCount) {
                    //val scaledX = (event.getX(i) / scaleX).toInt()
                    //val scaledY = (event.getY(i) / scaleY).toInt()
                    //handleTouchMove(scaledX, scaledY)
                    //Log.d("TOUCH", "Pointer Move: Index $i, x=$scaledX, y=$scaledY")
                //}
            }

            MotionEvent.ACTION_CANCEL -> {
                // Handle cancel action if necessary
                cancelAllEvents()
            }
        }
        return true
    }


    override fun performClick(): Boolean {
        // Call the superclass implementation (important for accessibility events)
        super.performClick()
        return true
    }

    // TODO: Swipes
    private fun handleTouchMove(x: Int, y: Int) {
        // Optional: Cancel repeat/long press if the finger moves off the key
        if (keyboard?.getKeyAt(x.toFloat(), y.toFloat()) == null) {
            cancelRepeatKey()
            handler.removeMessages(MSG_LONGPRESS)
        }
    }





    /////////////////////////////////////
    // RENDERED VIEW SCALING LOGIC
    // Helper to calculate keyboard height dynamically in pixels
    private fun calculateKeyboardHeight(context: Context): Float {
        val keyboard = keyboard ?: return 0f
        var totalHeight = 0f

        keyboard.rows.forEach { row ->
            val rowHeight = row.defaultKeyHeight ?: Constants.DEFAULT_KEY_HEIGHT
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

        // Resolve accent color or fallback to a soft violet-purple
        val accentColor = context.resolveThemeColor(android.R.attr.colorAccent, 0xFF6A5ACD.toInt()) // Soft purple fallback

        // Define colors based on theme
        val isDarkTheme = try {
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        } catch (e: Exception) {
            Log.w("ThemeDetection", "Failed to detect UI mode, falling back to dark theme.")
            true // Fallback to dark theme
        }

        val keyboardBackgroundColor = if (isDarkTheme) adjustColor(accentColor, 0.2f, 0.6f) else adjustColor(accentColor, 0.99f, 0.2f)
        val keyBackgroundColor = if (isDarkTheme) adjustColor(accentColor, 0.25f, 0.4f) else adjustColor(accentColor, 1.0f, 0.1f)
        val keyModifierBackgroundColor = if (isDarkTheme) adjustColor(accentColor, 1.2f, 0.7f) else adjustColor(accentColor, 1.0f, 1.0f)
        val keyLabelTextColor = if (isDarkTheme) 0xFFCCCCCC.toInt() else 0xFF333333.toInt()
        val keyLabelLongPressTextColor = if (isDarkTheme) adjustColor(accentColor, 1.4f, 0.9f) else adjustColor(accentColor, 0.95f, 1.0f)

        // Keep those values the same as text or background colors.
        val keyIconColor = keyLabelTextColor
        val keyModifierIconColor = keyboardBackgroundColor
        val keyModifierLabelTextColor = keyboardBackgroundColor
        val keyModifierSmallLabelTextColor = keyboardBackgroundColor


        // Draw keyboard background
        paint.color = keyboardBackgroundColor
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        // Scaling factors
        val scaleX = width.toFloat() / keyboard.totalLogicalWidth
        val scaleY = height.toFloat() / keyboard.totalLogicalHeight

        // Rendered gaps
        val renderedKeyGap = 5f // Adjust horizontal gap in px
        val renderedRowGap = 5f // Adjust vertical gap in px

        var currentY = 0f

        keyboard.rows.forEach { row ->
            val rowHeight = row.defaultKeyHeight?.times(scaleY) ?: (Constants.DEFAULT_KEY_HEIGHT * scaleY)
            val logicalRowGap = row.logicalRowGap?.times(scaleY) ?: (Constants.DEFAULT_LOGICAL_ROW_GAP * scaleY)
            var currentX = 0f

            row.keys.forEach { key ->
                val logicalKeyWidth = key.keyWidth?.times(scaleX) ?: (row.defaultKeyWidth?.times(scaleX) ?: (Constants.DEFAULT_KEY_WIDTH * scaleX))
                val logicalKeyHeight = key.keyHeight?.times(scaleY) ?: rowHeight
                val logicalKeyGap = key.logicalKeyGap?.times(scaleX) ?: (row.defaultLogicalKeyGap?.times(scaleX) ?: (Constants.DEFAULT_LOGICAL_KEY_GAP * scaleX))

                // Adjust for rendered gaps
                val rectKeyX = currentX + renderedKeyGap / 2
                val rectKeyY = currentY + renderedRowGap / 2
                val rectKeyWidth = logicalKeyWidth - renderedKeyGap
                val rectKeyHeight = logicalKeyHeight - renderedRowGap

                // Define key bounds
                val keyBounds = RectF(rectKeyX, rectKeyY, rectKeyX + rectKeyWidth, rectKeyY + rectKeyHeight)

                // Define corner radius
                val cornerRadius = rectKeyHeight * 0.1f

                // Draw key background
                paint.color = if (key.isModifier == true) keyModifierBackgroundColor else keyBackgroundColor
                canvas.drawRoundRect(keyBounds, cornerRadius, cornerRadius, paint)

                // --- Draw icon if present ---
                key.icon?.let { iconName ->
                    val drawable = getIconDrawable(iconName)
                    drawable?.let {
                        // TODO: add key icon / label size overrides
                        val iconSize = (rectKeyHeight * 0.6f).toInt()
                        val iconLeft = rectKeyX.toInt() + ((rectKeyWidth - iconSize) / 2).toInt()
                        val iconTop = rectKeyY.toInt() + ((rectKeyHeight - iconSize) / 2).toInt()
                        it.setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)

                        // Modulate the icon color by setting a color filter directly on the drawable
                        it.colorFilter = PorterDuffColorFilter(
                            if (key.isModifier == true) keyModifierIconColor else keyIconColor,
                            PorterDuff.Mode.SRC_IN
                        )

                        // Draw the drawable
                        it.draw(canvas)
                    }
                } ?: run {
                    // Check if small label exists
                    if (key.labelLongPress != null) {
                        // --- Draw small label (secondary text) ---
                        key.labelLongPress.let {
                            paint.color = if (key.isModifier == true) keyModifierSmallLabelTextColor else keyLabelLongPressTextColor
                            paint.textSize = rectKeyHeight * 0.32f
                            paint.textAlign = Paint.Align.CENTER

                            // Transform the small label dynamically based on meta states
                            val renderedSmallLabel = updateSmallLabelState(key) ?: ""

                            // Draw the small label if it's not empty
                            if (renderedSmallLabel.isNotEmpty()) {
                                canvas.drawText(
                                    renderedSmallLabel,
                                    keyBounds.centerX(),
                                    keyBounds.top + rectKeyHeight * 0.35f, // Upper half position
                                    paint
                                )
                            }
                        }

                        // --- Draw primary label (main text) ---
                        key.label?.let {
                            paint.color = if (key.isModifier == true) keyModifierLabelTextColor else keyLabelTextColor
                            paint.textSize = rectKeyHeight * 0.37f
                            paint.textAlign = Paint.Align.CENTER

                            // Transform the label dynamically based on meta states
                            var renderedLabel = updateLabelState(key) ?: ""

                            var textX = keyBounds.centerX()

                            // Dynamically set label if the key is a clipboard key
                            if (key.keyCode == Constants.KEYCODE_CLIPBOARD_ENTRY) {
                                paint.textAlign = Paint.Align.LEFT
                                // Correct the padding issue by adjusting the starting position
                                textX = keyBounds.left + rectKeyHeight * 0.1f // Minimal padding from the left
                                key.label = CustomKeyboardClipboard.getClipboardEntry(key.id) ?: "" // This text is committed
                                renderedLabel = CustomKeyboardClipboard.getClipboardEntry(key.id) ?: "" // This text is rendered
                            }

                            // Draw the label if it's not empty
                            if (renderedLabel.isNotEmpty()) {
                                canvas.drawText(
                                    renderedLabel,
                                    textX,
                                    keyBounds.centerY() + rectKeyHeight * 0.3f, // Slightly lower in the middle half
                                    paint
                                )
                            }
                        }
                    } else {
                        // --- Draw primary label only (centered) ---
                        key.label?.let {
                            paint.color = if (key.isModifier == true) keyModifierLabelTextColor else keyLabelTextColor
                            paint.textSize = if (key.isModifier == true) rectKeyHeight * 0.4f else rectKeyHeight * 0.5f
                            paint.textAlign = Paint.Align.CENTER

                            // Transform the label dynamically based on meta states
                            var renderedLabel = updateLabelState(key) ?: ""

                            var textX = keyBounds.centerX()

                            // Dynamically set label if the key is a clipboard key
                            if (key.keyCode == Constants.KEYCODE_CLIPBOARD_ENTRY) {
                                paint.textAlign = Paint.Align.LEFT
                                // Correct the padding issue by adjusting the starting position
                                textX = keyBounds.left + rectKeyHeight * 0.1f // Minimal padding from the left
                                key.label = CustomKeyboardClipboard.getClipboardEntry(key.id) ?: "" // This text is committed
                                renderedLabel = CustomKeyboardClipboard.getClipboardEntry(key.id) ?: "" // This text is rendered
                            }

                            // Draw the label if it's not empty
                            if (renderedLabel.isNotEmpty()) {
                                canvas.drawText(
                                    renderedLabel,
                                    textX,
                                    keyBounds.centerY() + (paint.textSize / 3), // Centered vertically
                                    paint
                                )
                            }
                        }
                    }
                }

                // Increment X for the next key
                currentX += logicalKeyWidth + logicalKeyGap
            }

            // Increment Y for the next row
            currentY += rowHeight + logicalRowGap
        }
    }

    // Adjust Color Method
    private fun adjustColor(color: Int, brightnessFactor: Float, intensityFactor: Float): Int {
        val alpha = color shr 24 and 0xFF
        val red = color shr 16 and 0xFF
        val green = color shr 8 and 0xFF
        val blue = color and 0xFF

        // Adjust intensity (blend with white)
        val blendedRed = (red + (255 - red) * (1 - intensityFactor)).toInt()
        val blendedGreen = (green + (255 - green) * (1 - intensityFactor)).toInt()
        val blendedBlue = (blue + (255 - blue) * (1 - intensityFactor)).toInt()

        // Adjust brightness
        val adjustedRed = (blendedRed * brightnessFactor).coerceIn(0f, 255f).toInt()
        val adjustedGreen = (blendedGreen * brightnessFactor).coerceIn(0f, 255f).toInt()
        val adjustedBlue = (blendedBlue * brightnessFactor).coerceIn(0f, 255f).toInt()

        return (alpha shl 24) or (adjustedRed shl 16) or (adjustedGreen shl 8) or adjustedBlue
    }

    // Extension to resolve theme color
    private fun Context.resolveThemeColor(attr: Int, fallbackColor: Int): Int {
        val typedValue = TypedValue()
        val resolved = theme.resolveAttribute(attr, typedValue, true)
        return if (resolved) {
            typedValue.data
        } else {
            Log.w("ThemeResolution", "Failed to resolve attribute: $attr. Using fallback color.")
            fallbackColor
        }
    }

    fun updateMetaState(shiftOn: Boolean, ctrlOn: Boolean, altOn: Boolean, capsLockOn: Boolean) {
        isShiftOn = shiftOn
        isCtrlOn = ctrlOn
        isAltOn = altOn
        isCapsLockOn = capsLockOn
        invalidateAllKeys()
    }

    private fun updateLabelState(key: Key): String? {

        // If preserveLabelCase is true, return the label as is.
        if (key.preserveLabelCase == true) {
            return key.label
        }

        val isMetaKeyToggled = isMetaKeyToggled(key.keyCode)

        return when {
            key.keyCode in metaKeyCodes && isMetaKeyToggled -> {
                // For meta keys (CAP, CTRL, SHIFT, ALT) toggled on, use uppercase.
                key.label?.uppercase()
            }
            key.keyCode in metaKeyCodes && !isMetaKeyToggled -> {
                // For meta keys toggled off, use lowercase.
                key.label?.lowercase()
            }
            key.isModifier == true && key.keyCode !in metaKeyCodes -> {
                // Modifier keys not in meta keys list (like Esc, Ent, Tab) retain their label.
                key.label
            }
            key.isModifier == false && (isShiftOn || isCapsLockOn) -> {
                // Non-modifier keys (ordinary characters) shift to uppercase when Shift or Caps Lock is active.
                key.label?.uppercase()
            }
            key.isModifier == false -> {
                // Non-modifier keys (ordinary characters) shift to lowercase when Shift and Caps Lock are inactive.
                key.label?.lowercase()
            }
            else -> {
                // Default case for any other keys, retain the original label.
                key.label
            }
        }
    }

    private fun updateSmallLabelState(key: Key): String? {
        // If preserveSmallLabelCase is true, return the small label as is.
        if (key.preserveSmallLabelCase == true) {
            return key.labelLongPress
        }

        val isMetaKeyToggled = key.keyCodeLongPress?.let { isMetaKeyToggled(it) } ?: false

        return when {
            key.keyCodeLongPress in metaKeyCodes && isMetaKeyToggled -> {
                // For meta keys (CAP, CTRL, SHIFT, ALT) toggled on, use uppercase.
                key.labelLongPress?.uppercase()
            }
            key.keyCodeLongPress in metaKeyCodes && !isMetaKeyToggled -> {
                // For meta keys toggled off, use lowercase.
                key.labelLongPress?.lowercase()
            }
            key.isModifier == true && key.keyCodeLongPress !in metaKeyCodes -> {
                // Modifier keys not in meta keys list (like Esc, Ent, Tab) retain their small label.
                key.labelLongPress
            }
            key.isModifier == false && (isShiftOn || isCapsLockOn) -> {
                // Non-modifier keys (ordinary characters) shift to uppercase when Shift or Caps Lock is active.
                key.labelLongPress?.uppercase()
            }
            key.isModifier == false -> {
                // Non-modifier keys (ordinary characters) shift to lowercase when Shift and Caps Lock are inactive.
                key.labelLongPress?.lowercase()
            }
            else -> {
                // Default case for any other keys, retain the original small label.
                key.labelLongPress
            }
        }
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



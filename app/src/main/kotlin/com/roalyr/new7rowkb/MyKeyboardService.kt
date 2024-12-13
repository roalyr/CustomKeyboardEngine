package com.roalyr.new7rowkb

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.inputmethodservice.InputMethodService
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.Toast

class MyKeyboardService : InputMethodService() {

    private lateinit var windowManager: WindowManager
    private var floatingKeyboardView: KeyboardView? = null
    private var keyboardView: KeyboardView? = null
    private var placeholderView: KeyboardView? = null
    private var inputView: View? = null

    private var isFloatingKeyboardOpen = false
    private var isFloatingKeyboard = false
    private var floatingKeyboardWidth: Int = 0
    private var floatingKeyboardHeight: Int = 0
    private var floatingKeyboardPosX: Int = 0
    private var floatingKeyboardPosY: Int = 0

    private var screenWidth: Int = 0
    private var screenHeight: Int = 0

    private var isReceiverRegistered = false

    private val overlayPermissionReceiver = OverlayPermissionReceiver()
    private val handler = Handler(Looper.getMainLooper())

    private var isShiftPressed = false
    private var isCtrlPressed = false
    private var isAltPressed = false
    private var isCapsPressed = false

    companion object {
        private const val ACTION_MANAGE_OVERLAY_PERMISSION = "android.settings.action.MANAGE_OVERLAY_PERMISSION"
        const val NAV_BAR_HEIGHT_PLACEHOLDER = 135 // In order to not cover the nav bar
        const val KEYBOARD_MINIMAL_WIDTH = 500
        const val KEYBOARD_MINIMAL_HEIGHT = 500
        const val KEYBOARD_TRANSLATION_INCREMENT = 50
        const val KEYBOARD_TRANSLATION_BOTTOM_OFFSET = 40 // dp
        const val KEYBOARD_SCALE_INCREMENT = 50
        const val NOT_A_KEY = -1
        const val KEYCODE_CLOSE_FLOATING_KEYBOARD = -10
        const val KEYCODE_OPEN_FLOATING_KEYBOARD = -11
        const val KEYCODE_SWITCH_KEYBOARD_MODE = -12
        const val KEYCODE_ENLARGE_FLOATING_KEYBOARD = -13
        const val KEYCODE_SHRINK_FLOATING_KEYBOARD = -14
        const val KEYCODE_ENLARGE_FLOATING_KEYBOARD_VERT = -15
        const val KEYCODE_SHRINK_FLOATING_KEYBOARD_VERT = -16
        const val KEYCODE_MOVE_FLOATING_KEYBOARD_LEFT = -17
        const val KEYCODE_MOVE_FLOATING_KEYBOARD_RIGHT = -18
        const val KEYCODE_MOVE_FLOATING_KEYBOARD_UP = -19
        const val KEYCODE_MOVE_FLOATING_KEYBOARD_DOWN = -20
    }


    ////////////////////////////////////////////
    // Init the keyboard
    override fun onCreateInputView(): View? {
        // Check width to prevent keyboard from crossing screen
        screenWidth = getScreenWidth()
        screenHeight = getScreenHeight()

        if (floatingKeyboardWidth == 0) {
            floatingKeyboardWidth = screenWidth
        }
        if (floatingKeyboardWidth > screenWidth) {
            floatingKeyboardWidth = screenWidth
        }

        /*if (floatingKeyboardHeight == 0) {
            floatingKeyboardHeight = screenHeight // TODO?
        }
        if (floatingKeyboardHeight > screenHeight) {
            floatingKeyboardHeight = screenHeight
        }*/

        createInputView()
        return if (isFloatingKeyboard) {
            createPlaceholderKeyboard()
            createFloatingKeyboard()
            inputView
        } else {
            createStandardKeyboard()
            inputView
        } ?: run {
            Log.e("MyKeyboardService", "Error creating input view")
            null
        }
    }

    override fun onCreate() {
        super.onCreate()
        initWindowManager()
        createInputView()
        checkAndRequestOverlayPermission()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterOverlayPermissionReceiver()
        closeAllKeyboards()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        // Close the floating keyboard before the input view is recreated
        // TODO: make sure the window is properly bounded.
        closeFloatingKeyboard()
        super.onConfigurationChanged(newConfig)
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        Log.d("MyKeyboardService", "onStartInputView called, restarting: $restarting")
        Log.d("MyKeyboardService", "inputView: $inputView")
        Log.d("MyKeyboardService", "keyboardView: $keyboardView")
        Log.d("MyKeyboardService", "placeholderView: $placeholderView")
        Log.d("MyKeyboardService", "floatingKeyboardView: $floatingKeyboardView")
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        Log.d("MyKeyboardService", "onFinishInputView called, finishingInput: $finishingInput")
    }


    ////////////////////////////////////////////
    // Handle window creation and inflation
    private fun createFloatingKeyboard() {

        inputView?.findViewById<View>(R.id.placeholder_view)?.requestFocus()

        // Check screen width every time to keep window size within
        screenWidth = getScreenWidth()
        floatingKeyboardView =
            layoutInflater.inflate(R.layout.floating_keyboard_view, null) as? KeyboardView
        val keyboardView = floatingKeyboardView
        if (keyboardView != null) {
            keyboardView.keyboard = Keyboard(this, R.xml.keyboard)

            val params = WindowManager.LayoutParams(
                floatingKeyboardWidth,
                WindowManager.LayoutParams.WRAP_CONTENT,
                floatingKeyboardPosX,
                floatingKeyboardPosY,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )

            windowManager.addView(keyboardView, params)

            setKeyboardActionListener(keyboardView)
            isFloatingKeyboardOpen = true
        } else {
            Log.e("MyKeyboardService", "Failed to inflate floating keyboard view")
        }
    }

    private fun createStandardKeyboard(): View? {
        val inputView = inputView
        if (inputView != null) {
            keyboardView = inputView.findViewById(R.id.keyboard_view)
            val keyboardView = keyboardView
            if (keyboardView != null) {
                keyboardView.keyboard = Keyboard(this, R.xml.keyboard)
                setKeyboardActionListener(keyboardView)
                return inputView
            } else {
                Log.e("MyKeyboardService", "Keyboard view not found in input view")
            }
        } else {
            Log.e("MyKeyboardService", "Input view is null")
        }
        return null
    }

    private fun createPlaceholderKeyboard(): View? {
        val inputView = inputView
        if (inputView != null) {
            placeholderView = inputView.findViewById(R.id.placeholder_view)
            val placeholderView = placeholderView
            if (placeholderView != null) {
                placeholderView.keyboard = Keyboard(this, R.xml.floating_keyboard_nav)
                setKeyboardActionListener(placeholderView)
                return inputView
            } else {
                Log.e("MyKeyboardService", "Placeholder view not found in input view")
            }
        } else {
            Log.e("MyKeyboardService", "Input view is null")
        }
        return null
    }
    
    private fun createInputView() {
        inputView = null
        inputView = layoutInflater.inflate(R.layout.input_view, null)
    }

    private fun initWindowManager() {
        if (::windowManager.isInitialized) return // Check if already initialized

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    private fun closeFloatingKeyboard() {
        if (floatingKeyboardView != null && isFloatingKeyboardOpen && ::windowManager.isInitialized) {
            try {
                windowManager.removeView(floatingKeyboardView)
                isFloatingKeyboardOpen = false // Update the flag
            } catch (e: IllegalArgumentException) {
                Log.e("MyKeyboardService", "Error closing floating keyboard: View not attached", e)
            } catch (e: Exception) {
                Log.e("MyKeyboardService", "Error closing floating keyboard", e)
            } finally {
                floatingKeyboardView = null // Reset the view reference
            }
        }
    }

    private fun closeStandardKeyboard() {
        val keyboardView = keyboardView
        if (keyboardView != null && keyboardView.parent != null) {
            (keyboardView.parent as? ViewGroup)?.removeView(keyboardView)
        }
    }

    private fun closePlaceholderKeyboard() {
        val placeholderView = placeholderView
        if (placeholderView != null && placeholderView.parent != null) {
            (placeholderView.parent as? ViewGroup)?.removeView(placeholderView)
        }
    }

    private fun closeAllKeyboards() {
        closeStandardKeyboard()
        closePlaceholderKeyboard()
        closeFloatingKeyboard()
        // ... close any other keyboard views
    }

    private fun switchKeyboardMode() {
        closeAllKeyboards() // Close any existing keyboards

        isFloatingKeyboard = !isFloatingKeyboard // Toggle the keyboard mode

        val newInputView = if (isFloatingKeyboard) {
            createInputView()
            createPlaceholderKeyboard()
            createFloatingKeyboard()
            inputView
        } else {
            createInputView()
            createStandardKeyboard()
            inputView

        }

        if (newInputView != null) {
            inputView = newInputView
            setInputView(newInputView) // Update the input view
        } else {
            Log.e("MyKeyboardService", "Error creating new input view")
        }
    }

    ////////////////////////////////////////////
    // Handle floating window size and position
    private fun resizeFloatingKeyboard(increment: Int) {
        if (floatingKeyboardView != null && isFloatingKeyboardOpen) {
            floatingKeyboardWidth += increment
            floatingKeyboardWidth = floatingKeyboardWidth.coerceIn(KEYBOARD_MINIMAL_WIDTH,screenWidth)
            closeFloatingKeyboard()
            createFloatingKeyboard()
        }
    }

    // Translation
    private fun translateFloatingKeyboard(xOffset: Int) {
        if (floatingKeyboardView != null && isFloatingKeyboardOpen) {
            floatingKeyboardPosX += xOffset
            floatingKeyboardPosX = floatingKeyboardPosX.coerceIn(-(screenWidth/2 - floatingKeyboardWidth/2),
                (screenWidth/2 - floatingKeyboardWidth/2))
            closeFloatingKeyboard()
            createFloatingKeyboard()
        }
    }

    private fun translateVertFloatingKeyboard(yOffset: Int) {
        if (floatingKeyboardView != null && isFloatingKeyboardOpen) {
            floatingKeyboardPosY += yOffset

            // Get the height of the floating keyboard
            floatingKeyboardView!!.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            floatingKeyboardHeight = floatingKeyboardView!!.measuredHeight

            // Get the height of bottom nav bar (or use placeholder assumption value)
            var navigationBarHeight = 0
            val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
            if (resourceId > 0) {
                navigationBarHeight = resources.getDimensionPixelSize(resourceId)
            } else {
                navigationBarHeight = NAV_BAR_HEIGHT_PLACEHOLDER
            }

            // get pixel density to convert dp to px
            val density = resources.displayMetrics.density
            val bottomOffsetPixels = KEYBOARD_TRANSLATION_BOTTOM_OFFSET * density

            // Keep some space in the bottom
            floatingKeyboardPosY = floatingKeyboardPosY.coerceIn(
                -(screenHeight/2 - floatingKeyboardHeight/2),
                ((screenHeight/2 - floatingKeyboardHeight/2 - bottomOffsetPixels - navigationBarHeight).toInt())
            )

            closeFloatingKeyboard()
            createFloatingKeyboard()
        }
    }

    private fun getScreenWidth(): Int {
        val displayMetrics = DisplayMetrics()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display?.getRealMetrics(displayMetrics)
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getMetrics(displayMetrics)
        }
        return displayMetrics.widthPixels
    }

    private fun getScreenHeight(): Int {
        val displayMetrics = DisplayMetrics()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display?.getRealMetrics(displayMetrics)
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getMetrics(displayMetrics)
        }
        return displayMetrics.heightPixels
    }




    ////////////////////////////////////////////
    // Listeners
    private fun setKeyboardActionListener(keyboardView: KeyboardView) {
        keyboardView.onKeyboardActionListener = object : KeyboardView.OnKeyboardActionListener {

            private var metaState = 0 // Combined meta state

            override fun onKey(primaryCode: Int, keyCodes: IntArray?, label: CharSequence?) {
                // Intercept key events and update metaState
                when (primaryCode) {
                    KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT -> {
                        // Toggle Shift
                        metaState = if (isShiftPressed) {
                            metaState and KeyEvent.META_SHIFT_ON.inv()
                        } else {
                            metaState or KeyEvent.META_SHIFT_ON
                        }
                        isShiftPressed = !isShiftPressed
                    }
                    KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.KEYCODE_CTRL_RIGHT -> {
                        // Toggle Ctrl
                        metaState = if (isCtrlPressed) {
                            metaState and KeyEvent.META_CTRL_ON.inv()
                        } else {
                            metaState or KeyEvent.META_CTRL_ON
                        }
                        isCtrlPressed = !isCtrlPressed
                    }
                    KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.KEYCODE_ALT_RIGHT -> {
                        // Toggle Alt
                        metaState = if (isAltPressed) {
                            metaState and KeyEvent.META_ALT_ON.inv()
                        } else {
                            metaState or KeyEvent.META_ALT_ON
                        }
                        isAltPressed = !isAltPressed
                    }
                    KeyEvent.KEYCODE_CAPS_LOCK -> {
                        // Toggle Caps
                        isCapsPressed = !isCapsPressed
                    }

                    else -> {
                        // Modify metaState for injected events
                        val modifiedMetaState = if (isCapsPressed) {
                            metaState or KeyEvent.META_CAPS_LOCK_ON
                        } else {
                            metaState
                        }
                        // Manually apply metaState to the key event
                        val modifiedEvent = KeyEvent(0, 0, KeyEvent.ACTION_DOWN,
                            primaryCode, 0, modifiedMetaState)

                        // Inject the modified key event
                        currentInputConnection?.sendKeyEvent(modifiedEvent) // Use stored InputConnection

                        // Reset metaState after other keys (except modifiers)
                        metaState = 0 // Reset meta state
                        isShiftPressed = false // Reset Shift state
                        isCtrlPressed = false // Reset Ctrl state
                        isAltPressed = false // Reset Alt state
                    }
                }

                handleKey(primaryCode, keyCodes, label) // Call handleKey here
            }

            override fun onPress(primaryCode: Int) {
                // No need to handle key repetition here, KeyboardView does it
            }

            override fun onRelease(primaryCode: Int) {
                // No need to handle key repetition here, KeyboardView does it
            }

            override fun onText(text: CharSequence?) {
                val inputConnection = currentInputConnection
                inputConnection.commitText(text, 1)
            }

            override fun swipeLeft() {
                // Handle swipe left action (if needed)
            }

            override fun swipeRight() {
                // Handle swipe right action (if needed)
            }

            override fun swipeDown() {
                // Handle swipe down action (if needed)
            }

            override fun swipeUp() {
                // Handle swipe up action (if needed)
            }

        } // Listeners
    }

    ////////////////////////////////////////////
    // Handle key events
    private fun handleKey(primaryCode: Int, keyCodes: IntArray?, label: CharSequence?) {

        // Handle meta key labels.
        if (isShiftPressed) {
            updateKeyLabels(true, KeyEvent.KEYCODE_SHIFT_LEFT)
            updateKeyLabels(true, KeyEvent.KEYCODE_SHIFT_RIGHT)
        } else {
            updateKeyLabels(false, KeyEvent.KEYCODE_SHIFT_LEFT)
            updateKeyLabels(false, KeyEvent.KEYCODE_SHIFT_RIGHT)
        }

        if (isCtrlPressed) {
            updateKeyLabels(true, KeyEvent.KEYCODE_CTRL_LEFT)
            updateKeyLabels(true, KeyEvent.KEYCODE_CTRL_RIGHT)
        } else {
            updateKeyLabels(false, KeyEvent.KEYCODE_CTRL_LEFT)
            updateKeyLabels(false, KeyEvent.KEYCODE_CTRL_RIGHT)
        }

        if (isAltPressed) {
            updateKeyLabels(true, KeyEvent.KEYCODE_ALT_LEFT)
            updateKeyLabels(true, KeyEvent.KEYCODE_ALT_RIGHT)
        } else {
            updateKeyLabels(false, KeyEvent.KEYCODE_ALT_LEFT)
            updateKeyLabels(false, KeyEvent.KEYCODE_ALT_RIGHT)
        }

        if (isCapsPressed) {
            updateKeyLabels(true, KeyEvent.KEYCODE_CAPS_LOCK)
        } else {
            updateKeyLabels(false, KeyEvent.KEYCODE_CAPS_LOCK)
        }

        // Handle custom key codes related to this application.
        if (primaryCode != NOT_A_KEY) {
            when (primaryCode) {

                KEYCODE_CLOSE_FLOATING_KEYBOARD -> {
                    if (isFloatingKeyboardOpen) {
                        closeFloatingKeyboard()
                    }
                }

                KEYCODE_OPEN_FLOATING_KEYBOARD -> {
                    if (!isFloatingKeyboardOpen) {
                        createFloatingKeyboard()
                    }
                }

                KEYCODE_SWITCH_KEYBOARD_MODE -> switchKeyboardMode()
                KEYCODE_ENLARGE_FLOATING_KEYBOARD -> resizeFloatingKeyboard(+KEYBOARD_SCALE_INCREMENT)
                KEYCODE_SHRINK_FLOATING_KEYBOARD -> resizeFloatingKeyboard(-KEYBOARD_SCALE_INCREMENT)
                KEYCODE_MOVE_FLOATING_KEYBOARD_LEFT -> translateFloatingKeyboard(-KEYBOARD_TRANSLATION_INCREMENT)
                KEYCODE_MOVE_FLOATING_KEYBOARD_RIGHT -> translateFloatingKeyboard(
                    KEYBOARD_TRANSLATION_INCREMENT
                )

                KEYCODE_MOVE_FLOATING_KEYBOARD_UP -> translateVertFloatingKeyboard(-KEYBOARD_TRANSLATION_INCREMENT)
                KEYCODE_MOVE_FLOATING_KEYBOARD_DOWN -> translateVertFloatingKeyboard(
                    KEYBOARD_TRANSLATION_INCREMENT
                )

            }
        } else {
            // If no key codes for - commit their labels.
            // This is for ordinary UTF-8 keys and so on.
            val keyLabel = label.toString()
            val finalKeyLabel = if (isShiftPressed || isCapsPressed){
                keyLabel.uppercase()
            } else {
                keyLabel.lowercase()
            }
            currentInputConnection.commitText(finalKeyLabel, 1)
        }
    }


    private fun updateKeyLabels(isKeyToggled: Boolean, toggledKeyCode: Int) {
        for (key in keyboardView!!.keyboard.keys) {
            if (key.codes.isNotEmpty() && key.label != null) {
                if (key.codes[0] == toggledKeyCode) { // Check if this is the toggled key
                    // Update label based on Shift key state
                    key.label = if (isKeyToggled) {
                        key.label.toString().uppercase() // Convert to uppercase
                    } else {
                        key.label.toString().lowercase() // Convert to lowercase
                    }
                    // Redraw keyboard
                    keyboardView!!.invalidateAllKeys()
                    break // Exit loop after updating the toggled key
                }
            }
        }
    }


    ////////////////////////////////////////////
    // Ask for permission
    private fun checkAndRequestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            registerReceiver(overlayPermissionReceiver, IntentFilter(ACTION_MANAGE_OVERLAY_PERMISSION), RECEIVER_EXPORTED) // Use this as context
            isReceiverRegistered = true // Set the flag after registering
            startActivity(intent)
        }
    }

    private fun unregisterOverlayPermissionReceiver() {
        if (isReceiverRegistered) {
            unregisterReceiver(overlayPermissionReceiver)
            isReceiverRegistered = false
        }
    }

    inner class OverlayPermissionReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(context)) {
                // Permission granted, show the floating keyboard
                val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                inputMethodManager.showInputMethodPicker()
            } else {
                // Permission denied, show a message or take other action
                Toast.makeText(context, "Overlay permission denied", Toast.LENGTH_SHORT).show()
                // You might also consider disabling keyboard functionality or showing a more prominent message
            }
            // No need to unregister the receiver here
        }
    }
}
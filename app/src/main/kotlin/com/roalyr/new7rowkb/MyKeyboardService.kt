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
    private var floatingKeyboardPosX: Int = 0
    private var floatingKeyboardPosY: Int = 0

    private var screenWidth: Int = 0

    private var isReceiverRegistered = false

    private val overlayPermissionReceiver = OverlayPermissionReceiver()
    private val handler = Handler(Looper.getMainLooper())

    private var isBackspacePressed = false

    companion object {
        private const val ACTION_MANAGE_OVERLAY_PERMISSION = "android.settings.action.MANAGE_OVERLAY_PERMISSION"
        const val KEYBOARD_MINIMAL_WIDTH = 500
        const val KEYBOARD_TRANSLATION_INCREMENT = 50
        const val KEYBOARD_SCALE_INCREMENT = 50
        const val KEY_REPEAT_DELAY = 100L
        const val KEYCODE_SPACE = 62
        const val KEYCODE_ENTER = 66
        const val KEYCODE_BACKSPACE = 67
        const val KEYCODE_CLOSE_FLOATING_KEYBOARD = -10
        const val KEYCODE_OPEN_FLOATING_KEYBOARD = -11
        const val KEYCODE_SWITCH_KEYBOARD_MODE = -12
        const val KEYCODE_ENLARGE_FLOATING_KEYBOARD = -13
        const val KEYCODE_SHRINK_FLOATING_KEYBOARD = -14
        const val KEYCODE_ENLARGE_FLOATING_KEYBOARD_VERT = -15
        const val KEYCODE_SHRINK_FLOATING_KEYBOARD_VERT = -16
        const val KEYCODE_MOVE_FLOATING_KEYBOARD_LEFT = -17
        const val KEYCODE_MOVE_FLOATING_KEYBOARD_RIGHT = -18
    }


    ////////////////////////////////////////////
    // Init the keyboard
    override fun onCreateInputView(): View? {
        // Check width to prevent keyboard from crossing screen
        screenWidth = getScreenWidth()
        if (floatingKeyboardWidth == 0) {
            floatingKeyboardWidth = screenWidth
        }
        if (floatingKeyboardWidth > screenWidth) {
            floatingKeyboardWidth = screenWidth
        }

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
            floatingKeyboardPosX = floatingKeyboardPosX.coerceIn(-(screenWidth - floatingKeyboardWidth), (screenWidth - floatingKeyboardWidth))
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

    ////////////////////////////////////////////
    // Listeners
    private fun setKeyboardActionListener(keyboardView: KeyboardView) {
        keyboardView.onKeyboardActionListener = object : KeyboardView.OnKeyboardActionListener {
            override fun onKey(primaryCode: Int, keyCodes: IntArray?, label: CharSequence?) {
                if (!isBackspacePressed) { // Only handle non-repeating keys in onKey
                    handleKey(primaryCode, keyCodes, label)
                }
            }

            override fun onPress(primaryCode: Int) {
                if (primaryCode == KEYCODE_BACKSPACE) {
                    isBackspacePressed = true
                    handler.postDelayed({
                        handleKey(primaryCode,null, null) // Call handleKey directly for each repetition
                        onPress(primaryCode) // Recursively call onPress for repetition
                    }, KEY_REPEAT_DELAY)
                }
            }

            override fun onRelease(primaryCode: Int) {
                if (primaryCode == KEYCODE_BACKSPACE) {
                    isBackspacePressed = false
                    handler.removeCallbacksAndMessages(null)
                }
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
        val inputConnection = currentInputConnection
        when (primaryCode) {

            // Keys for which codes are defined
            KEYCODE_SPACE -> inputConnection.commitText(" ", 1)
            KEYCODE_ENTER -> inputConnection.commitText("\n", 1)
            KEYCODE_BACKSPACE -> inputConnection.deleteSurroundingText(1, 0)
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
            KEYCODE_MOVE_FLOATING_KEYBOARD_RIGHT -> translateFloatingKeyboard(KEYBOARD_TRANSLATION_INCREMENT)

            // Ignore key codes for all other keys and commit their labels
            else -> {
                val keyLabel = label.toString()
                inputConnection.commitText(keyLabel, 1)
            }
        }
    }


    private fun handleKeyPress(primaryCode: Int) {
        // Logic to handle key press based on primaryCode
    }

    private fun handleKeyRelease(primaryCode: Int) {
        // Logic to handle key release (if needed)
    }

    ////////////////////////////////////////////
    // Key events functions


    ////////////////////////////////////////////
    // Ask for permission
    private fun checkAndRequestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            registerReceiver(overlayPermissionReceiver, IntentFilter(ACTION_MANAGE_OVERLAY_PERMISSION)) // Use this as context
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
package com.roalyr.new7rowkb

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.inputmethodservice.InputMethodService
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.Toast

class MyKeyboardService : InputMethodService() {

    private lateinit var windowManager: WindowManager // Always initialized in onCreate()
    private var floatingKeyboardView: KeyboardView? = null // Can be null if not shown
    private var keyboardView: KeyboardView? = null // Can be null if standard keyboard is not active
    private var placeholderView: KeyboardView? = null // Can be null if floating keyboard is not active
    private var inputView: View? = null // Can be null before creation or after closing

    private var isFloatingKeyboardOpen = false
    private var isFloatingKeyboard = false
    private var isReceiverRegistered = false
    private var isFloating = false

    private val overlayPermissionReceiver = OverlayPermissionReceiver()
    private val handler = Handler(Looper.getMainLooper())

    private var isBackspacePressed = false

    companion object {
        private const val ACTION_MANAGE_OVERLAY_PERMISSION = "android.settings.action.MANAGE_OVERLAY_PERMISSION"
        const val KEY_REPEAT_DELAY = 100L
        const val KEYCODE_SPACE = 62
        const val KEYCODE_ENTER = 66
        const val KEYCODE_BACKSPACE = 67
        const val KEYCODE_CLOSE_FLOATING_KEYBOARD = -10
        const val KEYCODE_OPEN_FLOATING_KEYBOARD = -11
        const val KEYCODE_SWITCH_KEYBOARD_MODE = -12
    }

    ////////////////////////////////////////////
    // Create the keyboard view
    override fun onCreateInputView(): View? { // Return nullable view
        // First inflate the input view
        createInputView()

        val viewToReturn = if (isFloating) {
            // Inflate the placeholder keyboard
            createPlaceholderKeyboard()

            // Inflate the floating keyboard
            createFloatingKeyboard()

            // Return input view with placeholder keyboard
            inputView
        } else {
            // Inflate standard keyboard view
            createStandardKeyboard()

            // Return input view with standard keyboard
            inputView
        }
        // Return the local variable, handling null case
        return viewToReturn ?: run {
            Log.e("MyKeyboardService", "Error creating input view")
            null // Or throw an exception if appropriate
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
        floatingKeyboardView = layoutInflater.inflate(R.layout.floating_keyboard_view, null) as? KeyboardView
        val keyboardView = floatingKeyboardView // Assign to a local variable
        if (keyboardView != null) {
            val keyboard = Keyboard(this, R.xml.keyboard)
            keyboardView.keyboard = keyboard // Use the local variable

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )

            if (::windowManager.isInitialized) {
                windowManager.addView(floatingKeyboardView, params)
            } else {
                Log.e("MyKeyboardService", "Window Manager not initialized")
            }

            setKeyboardActionListener(floatingKeyboardView!!)
            isFloatingKeyboardOpen = true
        } else {
            Log.e("MyKeyboardService", "Failed to inflate floating keyboard view")
        }
    }

    private fun createStandardKeyboard(): View? { // Return the updated inputView
        val inputView = inputView
        if (inputView != null) {
            keyboardView = inputView.findViewById(R.id.keyboard_view)
            val keyboardView = keyboardView
            if (keyboardView != null) {
                val keyboard = Keyboard(this, R.xml.keyboard)
                keyboardView.keyboard = keyboard
                setKeyboardActionListener(keyboardView)
                return inputView // Return the updated inputView
            } else {
                Log.e("MyKeyboardService", "Keyboard view not found in input view")
            }
        } else {
            Log.e("MyKeyboardService", "Input view is null")
        }
        return null // Return null if there was an error
    }

    private fun createPlaceholderKeyboard(): View? {
        val inputView = inputView
        if (inputView != null) {
            placeholderView = inputView.findViewById(R.id.placeholder_view)
            val placeholderView = placeholderView
            if (placeholderView != null) {
                val keyboard = Keyboard(this, R.xml.floating_keyboard_nav)
                placeholderView.keyboard = keyboard
                setKeyboardActionListener(placeholderView)
                return inputView // Return the inputView
            } else {
                Log.e("MyKeyboardService", "Placeholder view not found in input view")
            }
        } else {
            Log.e("MyKeyboardService", "Input view is null")
        }
        return null // Return null if there was an error
    }
    
    private fun createInputView() {
        inputView = layoutInflater.inflate(R.layout.input_view, null)
    }

    private fun initWindowManager() {
        if (::windowManager.isInitialized) return // Check if already initialized

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    private fun closeFloatingKeyboard() {
        if (floatingKeyboardView != null && ::windowManager.isInitialized) {
            try {
                windowManager.removeView(floatingKeyboardView)
            } catch (e: IllegalArgumentException) {
                // View not attached to window manager
                Log.e("MyKeyboardService", "Error closing floating keyboard: View not attached", e)
            } catch (e: Exception) {
                // Other exceptions
                Log.e("MyKeyboardService", "Error closing floating keyboard", e)
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
        //Log.d("MyKeyboardService", "Switching keyboard mode")
        closeAllKeyboards()
        inputView = null

        if (isFloatingKeyboard) {
            //Log.d("MyKeyboardService", "Creating floating keyboard")
            createInputView()
            inputView = createPlaceholderKeyboard()
            createFloatingKeyboard()
        } else {
            //Log.d("MyKeyboardService", "Creating standard keyboard")
            createInputView()
            inputView = createStandardKeyboard()
        }
        isFloatingKeyboard = !isFloatingKeyboard
        //Log.d("MyKeyboardService", "Keyboard mode switched, isFloatingKeyboard: $isFloatingKeyboard")
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
                    isFloatingKeyboardOpen = false
                }
            }
            KEYCODE_OPEN_FLOATING_KEYBOARD -> {
                if (!isFloatingKeyboardOpen) {
                    createFloatingKeyboard()
                    isFloatingKeyboardOpen = true
                }
            }
            KEYCODE_SWITCH_KEYBOARD_MODE -> switchKeyboardMode()

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
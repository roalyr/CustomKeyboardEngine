package com.roalyr.new7rowkb

import android.content.BroadcastReceiver
import android.content.Context
import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.WindowManager
import android.view.KeyEvent

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast


class MyKeyboardService : InputMethodService() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingKeyboardView: KeyboardView
    private lateinit var keyboardView: KeyboardView
    private lateinit var placeholderView: KeyboardView
    private lateinit var inputView: View


    private var isFloating = true

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
    }

    ////////////////////////////////////////////
    // Create the floating keyboard additional window.
/*

    val floatingKeyboardView = layoutInflater.inflate(R.layout.floating_keyboard, null) as KeyboardView
    val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT
    )
    windowManager.addView(floatingKeyboardView, params)
*/

    ////////////////////////////////////////////
    // Create the keyboard view
    override fun onCreateInputView(): View {

        // First inflate the input view
        inputView = layoutInflater.inflate(R.layout.input_view, null)

        return if (isFloating) {
            // Inflate a placeholder view for floating keyboard
            placeholderView = inputView.findViewById(R.id.placeholder_view)

            // Test this
            // You might add a click listener here to show/hide the floating keyboard
            placeholderView.setOnClickListener {
                // Toggle floating keyboard visibility
                if (floatingKeyboardView.visibility == View.VISIBLE) {
                    floatingKeyboardView.visibility = View.GONE
                } else {
                    floatingKeyboardView.visibility = View.VISIBLE
                }
            }

            // Inflate the floating keyboard
            createFloatingKeyboard()

            // Return input view with placeholder keyboard
            inputView

        } else {

            // Inflate standard keyboard view
            createStandardKeyboard()

            // Return input view with standard keyboard
            inputView

        } // Else

    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        checkAndRequestOverlayPermission()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(overlayPermissionReceiver)
    }

    ////////////////////////////////////////////
    // Handle window creation and inflation
    private fun createFloatingKeyboard() {
        // Inflate the floating keyboard view and add layout
        floatingKeyboardView = layoutInflater.inflate(R.layout.floating_keyboard_view, null) as KeyboardView
        val keyboard = Keyboard(this, R.xml.keyboard)
        floatingKeyboardView.keyboard = keyboard

        // Initiate window manager params
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        windowManager.addView(floatingKeyboardView, params)

        // Listeners
        setKeyboardActionListener(floatingKeyboardView)
    }

    private fun createStandardKeyboard() {
        // Inflate the regular keyboard view and add layout
        keyboardView = inputView.findViewById(R.id.keyboard_view)
        val keyboard = Keyboard(this, R.xml.keyboard)
        keyboardView.keyboard = keyboard

        // Listeners
        setKeyboardActionListener(keyboardView)
    }

    ////////////////////////////////////////////
    // Listeners
    private fun setKeyboardActionListener(keyboardView: KeyboardView) {
        keyboardView.onKeyboardActionListener = object : KeyboardView.OnKeyboardActionListener {
            override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
                if (!isBackspacePressed) { // Only handle non-repeating keys in onKey
                    handleKey(primaryCode, keyCodes)
                }
            }

            override fun onPress(primaryCode: Int) {
                if (primaryCode == KEYCODE_BACKSPACE) {
                    isBackspacePressed = true
                    handler.postDelayed({
                        handleKey(
                            primaryCode,
                            null
                        ) // Call handleKey directly for each repetition
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
                // Handle text input (if needed)
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
    private fun handleKey(primaryCode: Int, keyCodes: IntArray?) {
        val inputConnection = currentInputConnection
        //Log.d("Keyboard", "primaryCode in handleKey: $primaryCode") // Log primaryCode
        when (primaryCode) {
            KEYCODE_SPACE -> inputConnection.commitText(" ", 1)
            KEYCODE_ENTER -> inputConnection.commitText("\n", 1)
            KEYCODE_BACKSPACE -> inputConnection.deleteSurroundingText(1, 0)
            KEYCODE_CLOSE_FLOATING_KEYBOARD -> closeFloatingKeyboard()
            else -> {
                val isShiftLocked = false // Replace with your actual shift state logic
                val codeToUse = keyCodes?.getOrNull(0) ?: primaryCode
                val character = if (isShiftLocked) {
                    codeToUse.toChar().uppercaseChar().toString()
                } else {
                    codeToUse.toChar().lowercaseChar().toString()
                }
                inputConnection.commitText(character, 1)
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
    private fun closeFloatingKeyboard() {
        if (this::floatingKeyboardView.isInitialized) {
            try {
                windowManager.removeView(floatingKeyboardView)
            } catch (e: Exception) {
                Log.e("MyKeyboardService", "Error closing floating keyboard", e)            }
        }
    }

    ////////////////////////////////////////////
    // Ask for permission
    private fun checkAndRequestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            val receiver = OverlayPermissionReceiver()
            registerReceiver(receiver, IntentFilter(ACTION_MANAGE_OVERLAY_PERMISSION))

            startActivity(intent)
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
        }
    }
}
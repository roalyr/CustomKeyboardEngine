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
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyCharacterMap
import android.widget.Toast

//import com.roalyr.new7rowkb.Keyboard
//import com.roalyr.new7rowkb.KeyboardView

class MyKeyboardService : InputMethodService() {

    private lateinit var windowManager: WindowManager
    private lateinit var keyboardView: KeyboardView

    private val overlayPermissionReceiver = OverlayPermissionReceiver()
    private val handler = Handler(Looper.getMainLooper())

    private var isBackspacePressed = false

    companion object {
        private const val ACTION_MANAGE_OVERLAY_PERMISSION = "android.settings.action.MANAGE_OVERLAY_PERMISSION"
        const val KEY_REPEAT_DELAY = 100L
        const val KEYCODE_SPACE = 32
        const val KEYCODE_ENTER = 10
        const val KEYCODE_BACKSPACE = -5
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

        val inputView = layoutInflater.inflate(R.layout.input_view, null)
        keyboardView = inputView.findViewById(R.id.keyboard_view)
        val keyboard = Keyboard(this, R.xml.keyboard)
        keyboardView.requestLayout()
        keyboardView.keyboard = keyboard


        keyboardView.setOnKeyboardActionListener(object : KeyboardView.OnKeyboardActionListener {
            override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
                if (!isBackspacePressed) { // Only handle non-repeating keys in onKey
                    handleKey(primaryCode, keyCodes)
                }
            }

            override fun onPress(primaryCode: Int) {
                if (primaryCode == KEYCODE_BACKSPACE) {
                    isBackspacePressed = true
                    handler.postDelayed({
                        handleKey(primaryCode, null) // Call handleKey directly for each repetition
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

            // ... other listener methods
        })



        return inputView
    }

    override fun onInitializeInterface() {
        super.onInitializeInterface()
        checkAndRequestOverlayPermission()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(overlayPermissionReceiver)
        if (this::keyboardView.isInitialized) {
            windowManager.removeView(keyboardView)
        }
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
package com.roalyr.new7rowkb

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

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

    private var isShiftPressed = false
    private var isCtrlPressed = false
    private var isAltPressed = false
    private var isCapsPressed = false

    private val overlayPermissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Constants.ACTION_CHECK_OVERLAY_PERMISSION) {
                checkAndRequestOverlayPermission()
            }
        }
    }

    private val storagePermissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Constants.ACTION_CHECK_STORAGE_PERMISSIONS) {
                checkAndRequestStoragePermissions()
            }
        }
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

        // Register broadcast receivers
        registerReceiver(overlayPermissionReceiver, IntentFilter(Constants.ACTION_CHECK_OVERLAY_PERMISSION), RECEIVER_NOT_EXPORTED)
        registerReceiver(storagePermissionReceiver, IntentFilter(Constants.ACTION_CHECK_STORAGE_PERMISSIONS), RECEIVER_NOT_EXPORTED)

        // Force ask for permissions
        sendBroadcast(Intent(Constants.ACTION_CHECK_STORAGE_PERMISSIONS))
        sendBroadcast(Intent(Constants.ACTION_CHECK_OVERLAY_PERMISSION))

        copyDefaultKeyboardLayout()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister broadcast receivers
        unregisterReceiver(overlayPermissionReceiver)
        unregisterReceiver(storagePermissionReceiver)

        closeAllKeyboards()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        // Close the floating keyboard before the input view is recreated
        // TODO: make sure the window is properly bounded.
        screenWidth = getScreenWidth()
        screenHeight = getScreenHeight()

        // After rotating the screen - reposition the floating KB vertically within new limits.
        translateVertFloatingKeyboard(floatingKeyboardPosY)

        // Prevents bug where the thing doesn't close somehow.
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
        // Check if overlay permission is granted
        if (Settings.canDrawOverlays(this)) {
            inputView?.findViewById<View>(R.id.placeholder_view)?.requestFocus()

            // Check screen width every time to keep window size within
            screenWidth = getScreenWidth()
            floatingKeyboardView =
                layoutInflater.inflate(R.layout.floating_keyboard_view, null) as? KeyboardView
            val keyboardView = floatingKeyboardView
            if (keyboardView != null) {
                keyboardView.keyboard = Keyboard(this, R.xml.keyboard_default)

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
                floatingKeyboardView?.post {
                    floatingKeyboardHeight = floatingKeyboardView!!.measuredHeight
                }

                setKeyboardActionListener(keyboardView)
                isFloatingKeyboardOpen = true
            } else {
                Log.e("MyKeyboardService", "Failed to inflate floating keyboard view")
            }
        } else {
            // Permission denied, handle accordingly (e.g., show a message or request permission)
            // You might want to show a message to the user or redirect them to the settings to grant the permission
            Log.e("MyKeyboardService", "Overlay permission denied")
        }
    }

    private fun updateFloatingKeyboard() {
        if (floatingKeyboardView != null && isFloatingKeyboardOpen && ::windowManager.isInitialized) {
            // Check screen width to keep window size within
            screenWidth = getScreenWidth()

            // Update layout parameters
            val params = WindowManager.LayoutParams(
                floatingKeyboardWidth,
                WindowManager.LayoutParams.WRAP_CONTENT,
                floatingKeyboardPosX,
                floatingKeyboardPosY,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )

            // Update the floating keyboard view's layout
            windowManager.updateViewLayout(floatingKeyboardView, params)
            floatingKeyboardView?.invalidateAllKeys()
        }
    }

    private fun createStandardKeyboard(): View? {
        val inputView = inputView
        val keyboardView = inputView?.findViewById<KeyboardView>(R.id.keyboard_view)
        if (keyboardView != null) {
            val customKeyboard = try {
                val json = resources.openRawResource(R.raw.keyboard_default)
                    .bufferedReader().use { it.readText() }
                CustomKeyboard.fromJson(this, json) // Create CustomKeyboard from JSON
            } catch (e: Exception) {
                Log.e("MyKeyboardService", "Error loading JSON keyboard: ${e.message}")
                null
            }

            if (customKeyboard != null) {
                // Assuming you've adapted KeyboardView to work with CustomKeyboard
                keyboardView.keyboard = customKeyboard // TODO: link to KeyboardView properly
                setKeyboardActionListener(keyboardView) // Assuming this is adapted for CustomKeyboard
                return inputView
            } else {
                Log.e("MyKeyboardService", "Failed to create keyboard")
            }
        } else {
            Log.e("MyKeyboardService", "Keyboard view is null")
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
                this.floatingKeyboardView = null
            }
        }
    }

    private fun closeStandardKeyboard() {
        val keyboardView = keyboardView
        if (keyboardView != null && keyboardView.parent != null) {
            (keyboardView.parent as? ViewGroup)?.removeView(keyboardView)
            this.keyboardView = null
        }
    }

    private fun closePlaceholderKeyboard() {
        val placeholderView = placeholderView
        if (placeholderView != null && placeholderView.parent != null) {
            (placeholderView.parent as? ViewGroup)?.removeView(placeholderView)
            this.placeholderView = null
        }
    }

    private fun closeAllKeyboards() {
        closeStandardKeyboard()
        closePlaceholderKeyboard()
        closeFloatingKeyboard()
        // ... close any other keyboard views
    }

    private fun invalidateAllKeysOnBothKeyboards() {
        floatingKeyboardView?.invalidateAllKeys()
        keyboardView?.invalidateAllKeys()
        placeholderView?.invalidateAllKeys()
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

        updateModifierKeyLabels()
    }

    ////////////////////////////////////////////
    // Handle floating window size and position
    private fun resizeFloatingKeyboard(increment: Int) {
        if (floatingKeyboardView != null && isFloatingKeyboardOpen) {
            floatingKeyboardWidth += increment
            floatingKeyboardWidth =
                floatingKeyboardWidth.coerceIn(Constants.KEYBOARD_MINIMAL_WIDTH, screenWidth)

            // Update layout parameters and apply to the view
            // TODO: a poor workaround to mitigate flickering.
            floatingKeyboardView?.let { keyboardView ->
                val layoutParams = keyboardView.layoutParams as WindowManager.LayoutParams
                layoutParams.width = floatingKeyboardWidth
                windowManager.updateViewLayout(keyboardView, layoutParams)
            }

            closeFloatingKeyboard()
            createFloatingKeyboard()
        }
    }

    // Translation
    private fun translateFloatingKeyboard(xOffset: Int) {
        if (floatingKeyboardView != null && isFloatingKeyboardOpen) {
            floatingKeyboardPosX += xOffset
            floatingKeyboardPosX = floatingKeyboardPosX.coerceIn(
                -(screenWidth / 2 - floatingKeyboardWidth / 2),
                (screenWidth / 2 - floatingKeyboardWidth / 2)
            )
            updateFloatingKeyboard()
        }
    }

    private fun translateVertFloatingKeyboard(yOffset: Int) {
        if (floatingKeyboardView != null && isFloatingKeyboardOpen) {
            floatingKeyboardPosY += yOffset

            // Get the screen density
            val density = resources.displayMetrics.density

            // Convert dp offsets to pixel offsets
            val bottomOffsetPx = Constants.KEYBOARD_TRANSLATION_BOTTOM_OFFSET * density

            // Keep some space in the bottom and top
            val coerceTop = (-(screenHeight / 2.0  - floatingKeyboardHeight / 2.0)).toInt()
            val coerceBottom = (screenHeight / 2.0 - bottomOffsetPx - floatingKeyboardHeight / 2.0).toInt()
            floatingKeyboardPosY = floatingKeyboardPosY.coerceIn(
                coerceTop,
                coerceBottom
            )
            updateFloatingKeyboard()
        }
    }

    private fun getScreenWidth(): Int {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics.widthPixels
    }

    private fun getScreenHeight(): Int {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics.heightPixels
    }





    ////////////////////////////////////////////
    // Listeners
    private fun setKeyboardActionListener(keyboardView: KeyboardView) {
        keyboardView.onKeyboardActionListener = object : KeyboardView.OnKeyboardActionListener {

            private var metaState = 0 // Combined meta state
            private var modifiedMetaState = 0 // If Caps Lock is on.

            override fun onKey(primaryCode: Int, keyCodes: IntArray?, label: CharSequence?) {
                if (isLongPressing(keyboardView)) {
                    Log.d("MyKeyboard", "Long press detected, suppressing primary key processing")
                    // You might need to reset the flag here if it's not automatically reset elsewhere
                    keyboardView.mIsLongPressing = false
                    return // Early return to prevent primary key processing
                }
                // Handle custom keycodes. If key codes do not match - it will be skipped.
                handleCustomKey(primaryCode, keyCodes, label)

                // Intercept key events and update metaState and then handle ordinary keycodes or key labels.
                when (primaryCode) {
                    KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT -> {
                        // Toggle Shift
                        metaState = if (isShiftPressed) {
                            metaState and KeyEvent.META_SHIFT_ON.inv()
                        } else {
                            metaState or KeyEvent.META_SHIFT_ON
                        }
                        isShiftPressed = !isShiftPressed
                        updateModifierKeyLabels()
                    }
                    KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.KEYCODE_CTRL_RIGHT -> {
                        // Toggle Ctrl
                        metaState = if (isCtrlPressed) {
                            metaState and KeyEvent.META_CTRL_ON.inv()
                        } else {
                            metaState or KeyEvent.META_CTRL_ON
                        }
                        isCtrlPressed = !isCtrlPressed
                        updateModifierKeyLabels()
                    }
                    KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.KEYCODE_ALT_RIGHT -> {
                        // Toggle Alt
                        metaState = if (isAltPressed) {
                            metaState and KeyEvent.META_ALT_ON.inv()
                        } else {
                            metaState or KeyEvent.META_ALT_ON
                        }
                        isAltPressed = !isAltPressed
                        updateModifierKeyLabels()
                    }
                    KeyEvent.KEYCODE_CAPS_LOCK -> {
                        // Toggle Caps
                        isCapsPressed = !isCapsPressed
                        updateModifierKeyLabels()
                    }

                    else -> {
                        // Modify metaState for injected events
                        modifiedMetaState = if (isCapsPressed) {
                            metaState or KeyEvent.META_CAPS_LOCK_ON
                        } else {
                            metaState
                        }

                        // Handle ordinary keys according to meta state.
                        handleKey(primaryCode, keyCodes, label, modifiedMetaState)

                        // Reset metaState after other keys (except modifiers)
                        metaState = 0 // Reset meta state
                        isShiftPressed = false // Reset Shift state
                        isCtrlPressed = false // Reset Ctrl state
                        isAltPressed = false // Reset Alt state
                        // Keep caps lock on until toggled manually.
                        updateModifierKeyLabels()
                    }
                }
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
    private fun isLongPressing(kbView: KeyboardView?): Boolean {
        return kbView?.isLongPressing ?: false // Use safe call and elvis operator for null safety
    }

    private fun handleKey(primaryCode: Int, keyCodes: IntArray?, label: CharSequence?,modifiedMetaState: Int) {

        // Manually apply metaState to the key event if key code is written in xml.
        if (primaryCode != Constants.NOT_A_KEY) {
            injectKeyEvent(primaryCode, modifiedMetaState)
        } else {
            // If no key codes for a key - attempt to get key code from label.
            if (label != null) { // Check if label is not null
                val keyLabel = label.toString()
                val keyCode = getKeycodeFromLabel(keyLabel)

                if (keyCode != null) {
                    injectKeyEvent(keyCode, modifiedMetaState)
                } else {
                    // If no key code found, commit label as text "as is".
                    val finalKeyLabel = if (isShiftPressed || isCapsPressed) {
                        keyLabel.uppercase()
                    } else {
                        keyLabel.lowercase()
                    }
                    currentInputConnection.commitText(finalKeyLabel, 1)
                }
            } else {
                // Handle cases where label is null (e.g., keys with icons)
                // You might want to log a message or perform other actions here
                Log.d("Keyboard", "Key with null label pressed (primaryCode: $primaryCode)")
            }
        }
    }

    private fun handleCustomKey(primaryCode: Int, keyCodes: IntArray?, label: CharSequence?)  {
        // Handle custom key codes related to this application.
        if (primaryCode != Constants.NOT_A_KEY) {
            when (primaryCode) {

                Constants.KEYCODE_CLOSE_FLOATING_KEYBOARD -> {
                    if (isFloatingKeyboardOpen) {
                        closeFloatingKeyboard()
                    }
                }

                Constants.KEYCODE_OPEN_FLOATING_KEYBOARD -> {
                    if (!isFloatingKeyboardOpen) {
                        createFloatingKeyboard()
                    }
                }

                Constants.KEYCODE_SWITCH_KEYBOARD_MODE -> switchKeyboardMode()
                Constants.KEYCODE_ENLARGE_FLOATING_KEYBOARD -> resizeFloatingKeyboard(+Constants.KEYBOARD_SCALE_INCREMENT)
                Constants.KEYCODE_SHRINK_FLOATING_KEYBOARD -> resizeFloatingKeyboard(-Constants.KEYBOARD_SCALE_INCREMENT)
                Constants.KEYCODE_MOVE_FLOATING_KEYBOARD_LEFT -> translateFloatingKeyboard(-Constants.KEYBOARD_TRANSLATION_INCREMENT)
                Constants.KEYCODE_MOVE_FLOATING_KEYBOARD_RIGHT -> translateFloatingKeyboard(
                    Constants.KEYBOARD_TRANSLATION_INCREMENT
                )

                Constants.KEYCODE_MOVE_FLOATING_KEYBOARD_UP -> translateVertFloatingKeyboard(-Constants.KEYBOARD_TRANSLATION_INCREMENT)
                Constants.KEYCODE_MOVE_FLOATING_KEYBOARD_DOWN -> translateVertFloatingKeyboard(
                    Constants.KEYBOARD_TRANSLATION_INCREMENT
                )

            }
        }
    }

    ////////////////////////////////////////////
    // Event injections

    private fun injectMetaModifierKeysDown(metaState: Int) {
        if (metaState and KeyEvent.META_SHIFT_ON != 0) {
            injectKeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SHIFT_LEFT, 0) // Or KEYCODE_SHIFT_RIGHT
        }
        if (metaState and KeyEvent.META_CTRL_ON != 0) {
            injectKeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_CTRL_LEFT, 0) // Or KEYCODE_CTRL_RIGHT
        }
        if (metaState and KeyEvent.META_ALT_ON != 0) {
            injectKeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ALT_LEFT, 0) // Or KEYCODE_ALT_RIGHT
        }
    }

    private fun injectMetaModifierKeyUpKeys(metaState: Int) {
        if (metaState and KeyEvent.META_SHIFT_ON != 0) {
            injectKeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_SHIFT_LEFT, 0) // Or KEYCODE_SHIFT_RIGHT
        }
        if (metaState and KeyEvent.META_CTRL_ON != 0) {
            injectKeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_CTRL_LEFT, 0) // Or KEYCODE_CTRL_RIGHT
        }
        if (metaState and KeyEvent.META_ALT_ON != 0) {
            injectKeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ALT_LEFT, 0) // Or KEYCODE_ALT_RIGHT
        }
    }

    private fun injectKeyEvent(keyCode: Int, metaState: Int) {
        // Inject meta modifier key events if present in metaState
        injectMetaModifierKeysDown(metaState)

        // Inject the main key event
        injectKeyEvent(KeyEvent.ACTION_DOWN, keyCode, metaState)
        injectKeyEvent(KeyEvent.ACTION_UP, keyCode, metaState)

        // Inject meta modifier key up events if present in metaState
        injectMetaModifierKeyUpKeys(metaState)
    }

    private fun injectKeyEvent(action: Int, keyCode: Int, metaState: Int) {
        val event = KeyEvent(0, 0, action, keyCode, 0, metaState)
        currentInputConnection?.sendKeyEvent(event)
    }

    ////////////////////////////////////////////
    // Label handling
    private fun updateModifierKeyLabels() {
        updateKeyLabels(isShiftPressed, KeyEvent.KEYCODE_SHIFT_LEFT)
        updateKeyLabels(isShiftPressed, KeyEvent.KEYCODE_SHIFT_RIGHT)
        updateKeyLabels(isCtrlPressed, KeyEvent.KEYCODE_CTRL_LEFT)
        updateKeyLabels(isCtrlPressed, KeyEvent.KEYCODE_CTRL_RIGHT)
        updateKeyLabels(isAltPressed, KeyEvent.KEYCODE_ALT_LEFT)
        updateKeyLabels(isAltPressed, KeyEvent.KEYCODE_ALT_RIGHT)
        updateKeyLabels(isCapsPressed, KeyEvent.KEYCODE_CAPS_LOCK)
        updateKeyLabels(isFloatingKeyboard, Constants.KEYCODE_SWITCH_KEYBOARD_MODE)
    }

    private fun updateKeyLabels(isKeyToggled: Boolean, toggledKeyCode: Int) {
        val keyboardViews = listOfNotNull(keyboardView, floatingKeyboardView, placeholderView)
        for (keyboardView in keyboardViews) { // Iterate through the keyboard views
            for (key in keyboardView.keyboard.keys) {
                if (key.codes.isNotEmpty() && key.label != null) {
                    if (key.codes[0] == toggledKeyCode) { // Check if this is the toggled key
                        key.label = if (isKeyToggled) {
                            key.label.toString().uppercase() // Convert to uppercase
                        } else {
                            key.label.toString().lowercase() // Convert to lowercase
                        }
                        // Redraw keyboard
                        invalidateAllKeysOnBothKeyboards()
                        break // Exit loop after updating the toggled key
                    }
                }
            }
        }
    }

    ////////////////////////////////////////////
    // Handle primary key code lookup.
    private fun getKeycodeFromLabel(label: String): Int? {
        // Create a mapping of labels to key codes
        val labelToKeycodeMap = mapOf(
            "a" to KeyEvent.KEYCODE_A,
            "b" to KeyEvent.KEYCODE_B,
            "c" to KeyEvent.KEYCODE_C,
            "d" to KeyEvent.KEYCODE_D,
            "e" to KeyEvent.KEYCODE_E,
            "f" to KeyEvent.KEYCODE_F,
            "g" to KeyEvent.KEYCODE_G,
            "h" to KeyEvent.KEYCODE_H,
            "i" to KeyEvent.KEYCODE_I,
            "j" to KeyEvent.KEYCODE_J,
            "k" to KeyEvent.KEYCODE_K,
            "l" to KeyEvent.KEYCODE_L,
            "m" to KeyEvent.KEYCODE_M,
            "n" to KeyEvent.KEYCODE_N,
            "o" to KeyEvent.KEYCODE_O,
            "p" to KeyEvent.KEYCODE_P,
            "q" to KeyEvent.KEYCODE_Q,
            "r" to KeyEvent.KEYCODE_R,
            "s" to KeyEvent.KEYCODE_S,
            "t" to KeyEvent.KEYCODE_T,
            "u" to KeyEvent.KEYCODE_U,
            "v" to KeyEvent.KEYCODE_V,
            "w" to KeyEvent.KEYCODE_W,
            "x" to KeyEvent.KEYCODE_X,
            "y" to KeyEvent.KEYCODE_Y,
            "z" to KeyEvent.KEYCODE_Z,

            "0" to KeyEvent.KEYCODE_0,
            "1" to KeyEvent.KEYCODE_1,
            "2" to KeyEvent.KEYCODE_2,
            "3" to KeyEvent.KEYCODE_3,
            "4" to KeyEvent.KEYCODE_4,
            "5" to KeyEvent.KEYCODE_5,
            "6" to KeyEvent.KEYCODE_6,
            "7" to KeyEvent.KEYCODE_7,
            "8" to KeyEvent.KEYCODE_8,
            "9" to KeyEvent.KEYCODE_9,

            " " to KeyEvent.KEYCODE_SPACE,
            "." to KeyEvent.KEYCODE_PERIOD,
            "," to KeyEvent.KEYCODE_COMMA,
            ";" to KeyEvent.KEYCODE_SEMICOLON,
            "'" to KeyEvent.KEYCODE_APOSTROPHE,
            "/" to KeyEvent.KEYCODE_SLASH,
            "\\" to KeyEvent.KEYCODE_BACKSLASH,
            "`" to KeyEvent.KEYCODE_GRAVE,
            "[" to KeyEvent.KEYCODE_LEFT_BRACKET,
            "]" to KeyEvent.KEYCODE_RIGHT_BRACKET,
            "-" to KeyEvent.KEYCODE_MINUS,
            "=" to KeyEvent.KEYCODE_EQUALS,

            // Additional Symbols and Characters
            "!" to KeyEvent.KEYCODE_SHIFT_LEFT + KeyEvent.KEYCODE_1,
            "@" to KeyEvent.KEYCODE_SHIFT_LEFT + KeyEvent.KEYCODE_2,
            "#" to KeyEvent.KEYCODE_SHIFT_LEFT + KeyEvent.KEYCODE_3,
            "$" to KeyEvent.KEYCODE_SHIFT_LEFT + KeyEvent.KEYCODE_4,
            "%" to KeyEvent.KEYCODE_SHIFT_LEFT + KeyEvent.KEYCODE_5,
            "^" to KeyEvent.KEYCODE_SHIFT_LEFT + KeyEvent.KEYCODE_6,
            "&" to KeyEvent.KEYCODE_SHIFT_LEFT + KeyEvent.KEYCODE_7,
            "*" to KeyEvent.KEYCODE_SHIFT_LEFT + KeyEvent.KEYCODE_8,
            "(" to KeyEvent.KEYCODE_SHIFT_LEFT + KeyEvent.KEYCODE_9,
            ")" to KeyEvent.KEYCODE_SHIFT_LEFT + KeyEvent.KEYCODE_0,
            "_" to KeyEvent.KEYCODE_SHIFT_LEFT + KeyEvent.KEYCODE_MINUS,
            "+" to KeyEvent.KEYCODE_SHIFT_LEFT + KeyEvent.KEYCODE_EQUALS,
            ":" to KeyEvent.KEYCODE_SHIFT_LEFT + KeyEvent.KEYCODE_SEMICOLON,
            "\"" to KeyEvent.KEYCODE_SHIFT_LEFT + KeyEvent.KEYCODE_APOSTROPHE,
            "<" to KeyEvent.KEYCODE_SHIFT_LEFT + KeyEvent.KEYCODE_COMMA,
            ">" to KeyEvent.KEYCODE_SHIFT_LEFT + KeyEvent.KEYCODE_PERIOD,
            "?" to KeyEvent.KEYCODE_SHIFT_LEFT + KeyEvent.KEYCODE_SLASH,
            "|" to KeyEvent.KEYCODE_SHIFT_LEFT + KeyEvent.KEYCODE_BACKSLASH,
            "{" to KeyEvent.KEYCODE_SHIFT_LEFT + KeyEvent.KEYCODE_LEFT_BRACKET,
            "}" to KeyEvent.KEYCODE_SHIFT_LEFT + KeyEvent.KEYCODE_RIGHT_BRACKET,
            "~" to KeyEvent.KEYCODE_SHIFT_LEFT + KeyEvent.KEYCODE_GRAVE,
        )

        // Check if the label exists in the mapping
        return labelToKeycodeMap[label.lowercase()] // Convert label to lowercase for case-insensitivity
    }


    ////////////////////////////////////////////
    // Handle files
    private fun ensureLayoutsDirectoryExists(): Boolean {
        val layoutsDir = File(getExternalFilesDir(null), "New7rowKB/layouts")
        if (!layoutsDir.exists()) {
            return layoutsDir.mkdirs()
        }
        return true
    }

    private fun copyDefaultKeyboardLayout() {
        if (!ensureLayoutsDirectoryExists()) {
            Log.e("MyKeyboardService", "Failed to create layouts directory")
            return
        }

        val defaultLayoutFile = File(getExternalFilesDir(null), "New7rowKB/layouts/keyboard-default.xml")
        if (!defaultLayoutFile.exists()) {
            try {
                val inputStream = resources.openRawResource(R.raw.keyboard)
                val outputStream = FileOutputStream(defaultLayoutFile)
                val buffer = ByteArray(1024)
                var length: Int
                while (inputStream.read(buffer).also { length = it } > 0) {
                    outputStream.write(buffer, 0, length)
                }
                outputStream.close()
                inputStream.close()
            } catch (e: IOException) {
                Log.e("MyKeyboardService", "Failed to copy default keyboard layout", e)
            }
        }
    }

    ////////////////////////////////////////////
    // Ask for permission
    private fun checkAndRequestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            // Start permission request activity
            val intent = Intent(this, PermissionRequestActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.putExtra(Constants.EXTRA_PERMISSION_TYPE, Constants.PERMISSION_TYPE_OVERLAY) // Add extra to specify permission type
            ContextCompat.startActivity(this, intent, null)
        }
    }

    private fun checkAndRequestStoragePermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            // Start permission request activity
            val intent = Intent(this, PermissionRequestActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK // Add this line
            ContextCompat.startActivity(this, intent, null)
        }
    }


}
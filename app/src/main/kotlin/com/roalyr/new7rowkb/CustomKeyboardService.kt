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
import android.widget.Toast
import androidx.core.content.ContextCompat
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class CustomKeyboardService : InputMethodService() {

    private lateinit var windowManager: WindowManager
    private var floatingKeyboardView: CustomKeyboardView? = null
    private var keyboardView: CustomKeyboardView? = null
    private var serviceKeyboardView: CustomKeyboardView? = null
    private var inputView: View? = null

    private var isFloatingKeyboardOpen = false
    private var isFloatingKeyboard = false
    private var floatingKeyboardWidth: Int = 0
    private var floatingKeyboardHeight: Int = 0
    private var floatingKeyboardPosX: Int = 0
    private var floatingKeyboardPosY: Int = 0

    private var screenWidth: Int = 0
    private var screenHeight: Int = 0

    private var isShiftPressed = false
    private var isCtrlPressed = false
    private var isAltPressed = false
    private var isCapsPressed = false

    companion object {
        private const val TAG = "CustomKeyboardService"
    }

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
            createServiceKeyboard()
            createFloatingKeyboard()
            inputView
        } else {
            createStandardKeyboard()
            inputView
        } ?: run {
            Log.e(TAG, "Error creating input view")
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
        copyDefaultKeyboardLayouts()
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
        Log.d(TAG, "onStartInputView called, restarting: $restarting")
        Log.d(TAG, "inputView: $inputView")
        Log.d(TAG, "keyboardView: $keyboardView")
        Log.d(TAG, "placeholderView: $serviceKeyboardView")
        Log.d(TAG, "floatingKeyboardView: $floatingKeyboardView")
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        Log.d(TAG, "onFinishInputView called, finishingInput: $finishingInput")
    }


    ////////////////////////////////////////////
    // Handle window creation and inflation
    private fun createInputView() {
        inputView = null
        inputView = layoutInflater.inflate(R.layout.input_view, null)
    }


    private fun initWindowManager() {
        if (::windowManager.isInitialized) return // Check if already initialized
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    private fun createFloatingKeyboard() {
        if (!Settings.canDrawOverlays(this)) {
            Log.e(TAG, "Overlay permission denied")
            return
        }

        inputView?.findViewById<View>(R.id.service_keyboard_view)?.requestFocus()
        screenWidth = getScreenWidth()

        floatingKeyboardView = layoutInflater.inflate(R.layout.floating_keyboard_view, null) as? CustomKeyboardView

        floatingKeyboardView?.let { view ->
            val customKeyboard = loadKeyboardFromJson(R.raw.keyboard_default)
            if (customKeyboard != null) {
                view.setKeyboard(customKeyboard)
                setKeyboardActionListener(view)

                val params = WindowManager.LayoutParams(
                    floatingKeyboardWidth,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    floatingKeyboardPosX,
                    floatingKeyboardPosY,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT
                )

                windowManager.addView(view, params)
                view.post { floatingKeyboardHeight = view.measuredHeight }
                isFloatingKeyboardOpen = true
            } else {
                Log.e(TAG, "Failed to load keyboard for floating view")
            }
        } ?: Log.e(TAG, "Failed to inflate floating keyboard view")
    }

    private fun createServiceKeyboard(): View? {
        val rootView = inputView ?: run {
            Log.e(TAG, "Input view is null")
            return null
        }
        serviceKeyboardView = rootView.findViewById(R.id.service_keyboard_view) as? CustomKeyboardView
        serviceKeyboardView?.let { view ->
            val customKeyboard = loadKeyboardFromJson(R.raw.keyboard_service)
            if (customKeyboard != null) {
                view.setKeyboard(customKeyboard)
                setKeyboardActionListener(view)
                return rootView
            } else {
                Log.e(TAG, "Failed to load service keyboard layout")
            }
        } ?: Log.e(TAG, "Service keyboard view not found")
        return null
    }


    private fun createStandardKeyboard(): View? {
        inputView?.let { rootView ->
            keyboardView = rootView.findViewById(R.id.keyboard_view) as? CustomKeyboardView

            keyboardView?.let { view ->
                Log.d(TAG, "Keyboard view initialized successfully.")

                // Load the keyboard layout from JSON
                val customKeyboard = loadKeyboardFromJson(R.raw.keyboard_default)

                if (customKeyboard != null) {
                    Log.d(TAG, "Keyboard layout loaded successfully: ${customKeyboard.rows.size} rows")

                    // Debug: Log details of the keyboard layout
                    customKeyboard.rows.forEachIndexed { rowIndex, row ->
                        row.keys.forEachIndexed { keyIndex, key ->
                            Log.d(TAG, "Row $rowIndex, Key $keyIndex: label=${key.label}, x=${key.x}, y=${key.y}, width=${key.width}, height=${key.height}")
                        }
                    }

                    view.setKeyboard(customKeyboard)

                    // Set keyboard action listener
                    setKeyboardActionListener(view)
                    Log.d(TAG, "Keyboard action listener set.")

                    return rootView
                } else {
                    Log.e(TAG, "Failed to load standard keyboard layout")
                }
            } ?: Log.e(TAG, "Keyboard view is null")
        } ?: Log.e(TAG, "Input view is null")
        return null
    }


    ////////////////////////////////////////////
    // Unified method for keyboard switching
    private fun switchKeyboardMode() {
        closeAllKeyboards()
        isFloatingKeyboard = !isFloatingKeyboard

        inputView = if (isFloatingKeyboard) {
            createInputView()
            createServiceKeyboard()
            createFloatingKeyboard()
            inputView
        } else {
            createInputView()
            createStandardKeyboard()
            inputView
        }

        inputView?.let {
            setInputView(it)
        } ?: Log.e(TAG, "Error creating new input view")

        invalidateAllKeysOnBothKeyboards()
    }

    ////////////////////////////////////////////
    // Helper functions for closing keyboards
    private fun closeAllKeyboards() {
        closeStandardKeyboard()
        closeServiceKeyboard()
        closeFloatingKeyboard()
    }

    private fun closeFloatingKeyboard() {
        floatingKeyboardView?.let { view ->
            if (isFloatingKeyboardOpen && ::windowManager.isInitialized) {
                try {
                    windowManager.removeView(view)
                    isFloatingKeyboardOpen = false
                } catch (e: Exception) {
                    Log.e(TAG, "Error closing floating keyboard", e)
                } finally {
                    floatingKeyboardView = null
                }
            }
        }
    }

    private fun closeStandardKeyboard() {
        keyboardView?.let { view ->
            (view.parent as? ViewGroup)?.removeView(view)
            keyboardView = null
        }
    }

    private fun closeServiceKeyboard() {
        serviceKeyboardView?.let { view ->
            (view.parent as? ViewGroup)?.removeView(view)
            serviceKeyboardView = null
        }
    }

    ////////////////////////////////////////////
    // Helper functions
    private fun loadKeyboardFromJson(resourceId: Int): CustomKeyboard? {
        return try {
            val inputStream = resources.openRawResource(resourceId)
            val jsonContent = inputStream.bufferedReader().use { it.readText() }

            // Use a Json instance that ignores unknown keys
            val json = Json { ignoreUnknownKeys = true }

            // Decode JSON into KeyboardLayout
            val keyboardLayout = json.decodeFromString<KeyboardLayout>(jsonContent)
            Log.d(TAG, "Successfully loaded KeyboardLayout: ${keyboardLayout.rows.size} rows")

            // Return a CustomKeyboard instance
            CustomKeyboard(this, keyboardLayout)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading JSON keyboard: ${e.message}")
            Toast.makeText(this, "Error loading keyboard: ${e.message}", Toast.LENGTH_LONG).show()
            null
        }
    }


    private fun invalidateAllKeysOnBothKeyboards() {
        floatingKeyboardView?.invalidateAllKeys()
        keyboardView?.invalidateAllKeys()
        serviceKeyboardView?.invalidateAllKeys()
    }

    ////////////////////////////////////////////
    // Helper functions for floating keyboard
    private fun updateFloatingKeyboard() {
        if (isFloatingKeyboardOpen && ::windowManager.isInitialized) {
            floatingKeyboardView?.let { view ->
                screenWidth = getScreenWidth() // Update screen width dynamically

                val params = WindowManager.LayoutParams(
                    floatingKeyboardWidth,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    floatingKeyboardPosX,
                    floatingKeyboardPosY,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT
                )

                windowManager.updateViewLayout(view, params)
                view.invalidateAllKeys()
            }
        }
    }

    private fun resizeFloatingKeyboard(increment: Int) {
        if (isFloatingKeyboardOpen) {
            floatingKeyboardWidth = (floatingKeyboardWidth + increment)
                .coerceIn(Constants.KEYBOARD_MINIMAL_WIDTH, screenWidth)

            floatingKeyboardView?.let { view ->
                val layoutParams = view.layoutParams as WindowManager.LayoutParams
                layoutParams.width = floatingKeyboardWidth
                windowManager.updateViewLayout(view, layoutParams)
            }

            // Workaround to mitigate flickering
            closeFloatingKeyboard()
            createFloatingKeyboard()
        }
    }

    private fun translateFloatingKeyboard(xOffset: Int) {
        if (isFloatingKeyboardOpen) {
            floatingKeyboardPosX = (floatingKeyboardPosX + xOffset).coerceIn(
                -(screenWidth / 2 - floatingKeyboardWidth / 2),
                (screenWidth / 2 - floatingKeyboardWidth / 2)
            )
            updateFloatingKeyboard()
        }
    }

    private fun translateVertFloatingKeyboard(yOffset: Int) {
        if (isFloatingKeyboardOpen) {
            floatingKeyboardPosY += yOffset

            val density = resources.displayMetrics.density
            val bottomOffsetPx = (Constants.KEYBOARD_TRANSLATION_BOTTOM_OFFSET * density).toInt()

            val coerceTop = (-(screenHeight / 2.0 - floatingKeyboardHeight / 2.0)).toInt()
            val coerceBottom = (screenHeight / 2.0 - bottomOffsetPx - floatingKeyboardHeight / 2.0).toInt()

            floatingKeyboardPosY = floatingKeyboardPosY.coerceIn(coerceTop, coerceBottom)
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
    private fun setKeyboardActionListener(keyboardView: CustomKeyboardView) {
        keyboardView.setOnKeyboardActionListener(object : CustomKeyboardView.OnKeyboardActionListener {

            private var metaState = 0 // Combined meta state
            private var modifiedMetaState = 0 // Modified meta state for handling caps lock

            override fun onKey(primaryCode: Int, keyCodes: IntArray, label: CharSequence?) {
                if (keyboardView.isLongPressing()) {
                    Log.d(TAG, "Long press detected, suppressing key handling")
                    return
                }

                // Handle custom keycodes. If key codes do not match - it will be skipped.
                handleCustomKey(primaryCode, keyCodes, label)

                when (primaryCode) {
                    KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT -> {
                        metaState = toggleMetaState(metaState, KeyEvent.META_SHIFT_ON)
                        isShiftPressed = !isShiftPressed
                    }
                    KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.KEYCODE_CTRL_RIGHT -> {
                        metaState = toggleMetaState(metaState, KeyEvent.META_CTRL_ON)
                        isCtrlPressed = !isCtrlPressed
                    }
                    KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.KEYCODE_ALT_RIGHT -> {
                        metaState = toggleMetaState(metaState, KeyEvent.META_ALT_ON)
                        isAltPressed = !isAltPressed
                    }
                    KeyEvent.KEYCODE_CAPS_LOCK -> {
                        isCapsPressed = !isCapsPressed
                    }
                    else -> {
                        modifiedMetaState = if (isCapsPressed) {
                            metaState or KeyEvent.META_CAPS_LOCK_ON
                        } else {
                            metaState
                        }
                        // Update all meta states in the view
                        keyboardView.updateMetaState(isShiftPressed, isCtrlPressed, isAltPressed, isCapsPressed)
                        handleKey(primaryCode, keyCodes, label, modifiedMetaState)
                        resetMetaStates()
                    }
                }
            }

            override fun onText(text: CharSequence) {
                currentInputConnection.commitText(text, 1)
            }

            private fun toggleMetaState(metaState: Int, metaFlag: Int): Int {
                return metaState xor metaFlag // XOR toggles the state
            }

            private fun resetMetaStates() {
                // Store those values in service class.
                metaState = 0
                isShiftPressed = false
                isCtrlPressed = false
                isAltPressed = false
            }
        })
    }






    ////////////////////////////////////////////
    // Handle key events
    private fun handleKey(primaryCode: Int, keyCodes: IntArray?, label: CharSequence?,modifiedMetaState: Int) {

        // Manually apply metaState to the key event if key code is written in layout.
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
                Log.d(TAG, "Key with null label pressed (primaryCode: $primaryCode)")
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
    private fun injectMetaModifierKeys(metaState: Int, action: Int) {
        if (metaState and KeyEvent.META_SHIFT_ON != 0) {
            injectKeyEvent(action, KeyEvent.KEYCODE_SHIFT_LEFT) // Or KEYCODE_SHIFT_RIGHT
        }
        if (metaState and KeyEvent.META_CTRL_ON != 0) {
            injectKeyEvent(action, KeyEvent.KEYCODE_CTRL_LEFT) // Or KEYCODE_CTRL_RIGHT
        }
        if (metaState and KeyEvent.META_ALT_ON != 0) {
            injectKeyEvent(action, KeyEvent.KEYCODE_ALT_LEFT) // Or KEYCODE_ALT_RIGHT
        }
    }

    private fun injectKeyEvent(keyCode: Int, metaState: Int) {
        // Inject meta modifier keys down
        injectMetaModifierKeys(metaState, KeyEvent.ACTION_DOWN)

        // Inject the main key event
        injectKeyEventInternal(KeyEvent.ACTION_DOWN, keyCode, metaState)
        injectKeyEventInternal(KeyEvent.ACTION_UP, keyCode, metaState)

        // Inject meta modifier keys up
        injectMetaModifierKeys(metaState, KeyEvent.ACTION_UP)
    }

    private fun injectKeyEventInternal(action: Int, keyCode: Int, metaState: Int) {
        val eventTime = System.currentTimeMillis()
        val event = KeyEvent(eventTime, eventTime, action, keyCode, 0, metaState)
        if (currentInputConnection != null) {
            currentInputConnection?.sendKeyEvent(event)
        } else {
            Log.w(TAG, "CurrentInputConnection is null. Failed to send key event: $keyCode")
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
        val layoutsDir = File(Constants.LAYOUTS_DIRECTORY)
        if (!layoutsDir.exists()) {
            return layoutsDir.mkdirs()
        }
        return true
    }

    private fun copyKeyboardLayoutIfMissing(fileName: String, rawResourceId: Int) {
        val layoutFile = File(Constants.LAYOUTS_DIRECTORY, fileName)

        if (!layoutFile.exists()) {
            try {
                val inputStream = resources.openRawResource(rawResourceId)
                val outputStream = FileOutputStream(layoutFile)
                val buffer = ByteArray(1024)
                var length: Int
                while (inputStream.read(buffer).also { length = it } > 0) {
                    outputStream.write(buffer, 0, length)
                }
                outputStream.close()
                inputStream.close()
                Log.i(TAG, "Successfully copied $fileName to layouts directory")
            } catch (e: IOException) {
                Log.e(TAG, "Failed to copy $fileName", e)
            }
        } else {
            Log.i(TAG, "$fileName already exists, skipping copy")
        }
    }





    private fun copyDefaultKeyboardLayouts() {
        if (!ensureLayoutsDirectoryExists()) {
            Log.e(TAG, "Failed to create layouts directory")
            return
        }

        copyKeyboardLayoutIfMissing("keyboard-default.json", R.raw.keyboard_default)
        copyKeyboardLayoutIfMissing("keyboard-service.json", R.raw.keyboard_service)
    }


    ////////////////////////////////////////////
    // Ask for permissions
    private fun checkAndRequestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            // Start permission request activity
            val intent = Intent(this, PermissionRequestActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra(Constants.EXTRA_PERMISSION_TYPE, Constants.PERMISSION_TYPE_OVERLAY)
            }
            startActivity(intent)
        }
    }

    private fun checkAndRequestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ does not require WRITE_EXTERNAL_STORAGE permission
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                startPermissionRequestActivity(Constants.PERMISSION_TYPE_STORAGE)
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ with scoped storage
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                startPermissionRequestActivity(Constants.PERMISSION_TYPE_STORAGE)
            }
        } else {
            // For Android 9 and below
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                startPermissionRequestActivity(Constants.PERMISSION_TYPE_STORAGE)
            }
        }
    }

    private fun startPermissionRequestActivity(permissionType: String) {
        val intent = Intent(this, PermissionRequestActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(Constants.EXTRA_PERMISSION_TYPE, permissionType)
        }
        startActivity(intent)
    }


}
package com.roalyr.new7rowkb

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.inputmethodservice.InputMethodService
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import java.io.File

class CustomKeyboardService : InputMethodService() {

    private lateinit var windowManager: WindowManager
    private var floatingKeyboardView: CustomKeyboardView? = null
    private var keyboardView: CustomKeyboardView? = null
    private var serviceKeyboardView: CustomKeyboardView? = null
    private var inputView: View? = null

    private var languageLayouts = mutableListOf<Pair<CustomKeyboard, Boolean>>()
    private var serviceLayouts = mutableMapOf<String, Pair<CustomKeyboard, Boolean>>()
    private var currentLanguageLayoutIndex = 0

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
            if (intent.action == Constants.Actions.CHECK_OVERLAY_PERMISSION) {
                ActivityPermissionRequest.checkAndRequestOverlayPermission(context)
            }
        }
    }

    private val storagePermissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Constants.Actions.CHECK_STORAGE_PERMISSIONS) {
                ActivityPermissionRequest.checkAndRequestStoragePermissions(context)
            }
        }
    }

    ////////////////////////////////////////////
    // Init the keyboard
    override fun onCreateInputView(): View? {
        createInputView()
        return inputView
    }

    override fun onCreate() {
        initWindowManager()
        ClassFunctionsFiles.ensureMediaDirectoriesExistAndCopyDefaults(windowManager, this, resources)

        // Register broadcast receivers
        registerReceiver(overlayPermissionReceiver, IntentFilter(Constants.Actions.CHECK_OVERLAY_PERMISSION), RECEIVER_NOT_EXPORTED)
        registerReceiver(storagePermissionReceiver, IntentFilter(Constants.Actions.CHECK_STORAGE_PERMISSIONS), RECEIVER_NOT_EXPORTED)
        // Force ask for permissions
        sendBroadcast(Intent(Constants.Actions.CHECK_STORAGE_PERMISSIONS))
        sendBroadcast(Intent(Constants.Actions.CHECK_OVERLAY_PERMISSION))
        super.onCreate()
    }

    override fun onDestroy() {
        // Unregister broadcast receivers
        unregisterReceiver(overlayPermissionReceiver)
        unregisterReceiver(storagePermissionReceiver)
        closeAllKeyboards()
        super.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        recreateKeyboards()
        // After rotating the screen - reposition the floating KB vertically within new limits.
        translateVertFloatingKeyboard(floatingKeyboardPosY)
        super.onConfigurationChanged(newConfig)
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        if (!restarting){
            Log.i(TAG, "Starting input view. Reloading layouts.")
            reloadKeyboardLayouts()
            recreateKeyboards()
        } else {
            //Log.i(TAG, "Restarting input view on the same editor. NOT Reloading layouts.")
        }
        super.onStartInputView(info, restarting)
    }


    ////////////////////////////////////////////
    // Handle window creation and inflation
    private fun createInputView() {
        inputView = null
        inputView = layoutInflater.inflate(R.layout.standard_keyboard_view, null)
    }

    private fun initWindowManager() {
        if (::windowManager.isInitialized) return // Check if already initialized
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    private fun createFloatingKeyboard() {
        if (!Settings.canDrawOverlays(this)) {
            val errorMsg = "Overlay permission denied"
            ClassFunctionsPopups.showErrorPopup(windowManager, this, TAG, errorMsg)
            return
        }

        floatingKeyboardView = layoutInflater.inflate(R.layout.floating_keyboard_view, null) as? CustomKeyboardView

        floatingKeyboardView?.let { view ->
            val (customKeyboard, isFallback) = languageLayouts.getOrNull(currentLanguageLayoutIndex)
                ?: run {
                    val errorMsg = "No valid language layout found at index: $currentLanguageLayoutIndex"
                    ClassFunctionsPopups.showErrorPopup(windowManager, this, TAG, errorMsg)
                    return
                }

            view.setKeyboard(customKeyboard)
            setKeyboardActionListener(view)

            if (isFallback) {
                val errorMsg = "Using fallback language layout at index: $currentLanguageLayoutIndex"
                ClassFunctionsPopups.showErrorPopup(windowManager, this, TAG, errorMsg)
            }

            val params = WindowManager.LayoutParams(
                floatingKeyboardWidth,
                WindowManager.LayoutParams.WRAP_CONTENT,
                floatingKeyboardPosX,
                floatingKeyboardPosY,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )

            try {
                windowManager.addView(view, params)
                isFloatingKeyboardOpen = true
                view.post { floatingKeyboardHeight = view.measuredHeight }
            } catch (e: Exception) {
                val errorMsg = "Failed to add floating keyboard view: ${e.message}"
                ClassFunctionsPopups.showErrorPopup(windowManager, this, TAG, errorMsg)
            }
        } ?: run {
            val errorMsg = "Failed to inflate floating keyboard view"
            ClassFunctionsPopups.showErrorPopup(windowManager, this, TAG, errorMsg)
        }
    }


    private fun createServiceKeyboard(): View? {
        val rootView = inputView ?: run {
            val errorMsg = "Input view is null"
            ClassFunctionsPopups.showErrorPopup(windowManager, this, TAG, errorMsg)
            return null
        }

        serviceKeyboardView = rootView.findViewById(R.id.service_keyboard_view) as? CustomKeyboardView
        serviceKeyboardView?.let { view ->
            // TODO: Load a service keyboard according to settings, For now - only the one with default name.
            val customKeyboardPair = serviceLayouts[Constants.LAYOUT_SERVICE_DEFAULT]
            if (customKeyboardPair != null) {
                val (customKeyboard, isFallback) = customKeyboardPair
                view.setKeyboard(customKeyboard)
                setKeyboardActionListener(view)

                if (isFallback) {
                    val errorMsg = "Using fallback service layout $Constants.LAYOUT_SERVICE_DEFAULT."
                    ClassFunctionsPopups.showErrorPopup(windowManager, this, TAG, errorMsg)
                }
            } else {
                val errorMsg = "Service keyboard layout not found."
                ClassFunctionsPopups.showErrorPopup(windowManager, this, TAG, errorMsg)
            }
        } ?: run {
            val errorMsg = "Service keyboard view not found."
            ClassFunctionsPopups.showErrorPopup(windowManager, this, TAG, errorMsg)
        }
        return rootView
    }


    private fun createStandardKeyboard(): View? {
        val rootView = inputView ?: run {
            val errorMsg = "Input view is null."
            ClassFunctionsPopups.showErrorPopup(windowManager, this, TAG, errorMsg)
            return null
        }

        keyboardView = rootView.findViewById(R.id.keyboard_view) as? CustomKeyboardView
        keyboardView?.let { view ->
            val (customKeyboard, isFallback) = languageLayouts.getOrNull(currentLanguageLayoutIndex)
                ?: run {
                    val errorMsg = "No valid language layout found at index: $currentLanguageLayoutIndex"
                    ClassFunctionsPopups.showErrorPopup(windowManager, this, TAG, errorMsg)
                    return rootView
                }

            view.setKeyboard(customKeyboard)
            setKeyboardActionListener(view)

            if (isFallback) {
                val errorMsg = "Using fallback language layout at index: $currentLanguageLayoutIndex"
                ClassFunctionsPopups.showErrorPopup(windowManager, this, TAG, errorMsg)
            }

            return rootView
        } ?: run {
            val errorMsg = "Keyboard view is null."
            ClassFunctionsPopups.showErrorPopup(windowManager, this, TAG, errorMsg)
        }
        return null
    }




    ////////////////////////////////////////////
    // Unified method for keyboard switching
    private fun cycleLanguageLayout() {
        currentLanguageLayoutIndex = (currentLanguageLayoutIndex + 1) % languageLayouts.size
        Log.i(TAG, "Cycled to language layout index: $currentLanguageLayoutIndex")
        reloadKeyboardLayouts() // Always reload to reflect changes
        recreateKeyboards()
    }

    private fun switchKeyboardMode() {
        isFloatingKeyboard = !isFloatingKeyboard
        Log.i(TAG, "Switched keyboard mode to ${if (isFloatingKeyboard) "Floating" else "Standard"}.")
        reloadKeyboardLayouts() // Reload and apply the updated mode
        recreateKeyboards()
    }


    private fun recreateKeyboards(){
        closeAllKeyboards()
        screenHeight = getScreenHeight()
        screenWidth = getScreenWidth()

        inputView = if (isFloatingKeyboard) {
            // Check width to prevent keyboard from crossing screen
            if (floatingKeyboardWidth == 0) {
                floatingKeyboardWidth = getScreenWidth()
            }
            if (floatingKeyboardWidth > screenWidth) {
                floatingKeyboardWidth = getScreenWidth()
            }
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
        } ?: {
            val errorMsg = "Error creating new input view"
            ClassFunctionsPopups.showErrorPopup(windowManager, this, TAG, errorMsg)
        }

        invalidateAllKeysOnAllKeyboards()
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
                    val errorMsg = "Error closing floating keyboard: ${e.message}"
                    ClassFunctionsPopups.showErrorPopup(windowManager, this, TAG, errorMsg)
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
    private fun invalidateAllKeysOnAllKeyboards() {
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
            updateFloatingKeyboard()
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
            Log.i(TAG, "translateVertFloatingKeyboard called with yOffset: $yOffset")

            floatingKeyboardPosY += yOffset
            Log.i(TAG, "Updated floatingKeyboardPosY: $floatingKeyboardPosY")
            Log.i(TAG, "Updated floatingKeyboardHEIGH: $floatingKeyboardHeight")


            val density = resources.displayMetrics.density
            val bottomOffsetPx = (Constants.KEYBOARD_TRANSLATION_BOTTOM_OFFSET * density).toInt()
            Log.i(TAG, "Calculated bottomOffsetPx: $bottomOffsetPx")

            val coerceTop = (-(getScreenHeight() / 2.0 - floatingKeyboardHeight / 2.0)).toInt()
            val coerceBottom = (getScreenHeight() / 2.0 - bottomOffsetPx - floatingKeyboardHeight / 2.0).toInt()
            Log.i(TAG, "Coerce limits - Top: $coerceTop, Bottom: $coerceBottom")

            floatingKeyboardPosY = floatingKeyboardPosY.coerceIn(coerceTop, coerceBottom)
            Log.i(TAG, "Coerced floatingKeyboardPosY: $floatingKeyboardPosY")

            updateFloatingKeyboard()
        } else {
            Log.w(TAG, "translateVertFloatingKeyboard called but floating keyboard is not open.")
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

            override fun onKey(primaryCode: Int, label: CharSequence?) {
                if (keyboardView.isLongPressing()) {
                    //Long press detected, suppressing key handling
                    return
                }

                // Handle custom keycodes. If key codes do not match - it will be skipped.
                handleCustomKey(primaryCode, label)

                when (primaryCode) {
                    KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT -> {
                        metaState = toggleMetaState(metaState, KeyEvent.META_SHIFT_ON)
                        isShiftPressed = !isShiftPressed
                        keyboardView.updateMetaState(isShiftPressed, isCtrlPressed, isAltPressed, isCapsPressed)
                    }
                    KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.KEYCODE_CTRL_RIGHT -> {
                        metaState = toggleMetaState(metaState, KeyEvent.META_CTRL_ON)
                        isCtrlPressed = !isCtrlPressed
                        keyboardView.updateMetaState(isShiftPressed, isCtrlPressed, isAltPressed, isCapsPressed)
                    }
                    KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.KEYCODE_ALT_RIGHT -> {
                        metaState = toggleMetaState(metaState, KeyEvent.META_ALT_ON)
                        isAltPressed = !isAltPressed
                        keyboardView.updateMetaState(isShiftPressed, isCtrlPressed, isAltPressed, isCapsPressed)
                    }
                    KeyEvent.KEYCODE_CAPS_LOCK -> {
                        isCapsPressed = !isCapsPressed
                        keyboardView.updateMetaState(isShiftPressed, isCtrlPressed, isAltPressed, isCapsPressed)
                    }
                    else -> {
                        modifiedMetaState = if (isCapsPressed) {
                            metaState or KeyEvent.META_CAPS_LOCK_ON
                        } else {
                            metaState
                        }
                        handleKey(primaryCode, label, modifiedMetaState)
                        resetMetaStates()
                        keyboardView.updateMetaState(isShiftPressed, isCtrlPressed, isAltPressed, isCapsPressed)
                    }
                }
            }

            override fun onText(text: CharSequence) {
                val processedText = if (isShiftPressed || isCapsPressed) {
                    resetMetaStates()
                    keyboardView.updateMetaState(isShiftPressed, isCtrlPressed, isAltPressed, isCapsPressed)
                    text.toString().uppercase()
                } else {
                    text.toString()
                }
                currentInputConnection.commitText(processedText, 1)
            }


            override fun onRelease(codes: Int) {
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
    private fun handleKey(primaryCode: Int, label: CharSequence?,modifiedMetaState: Int) {

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
            }
        }
    }

    private fun handleCustomKey(primaryCode: Int, label: CharSequence?)  {
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

                Constants.KEYCODE_CYCLE_LANGUAGE_LAYOUT -> {
                    cycleLanguageLayout()
                }

            }
        }
    }





    ////////////////////////////////////////////
    // Event injections
    private fun injectMetaModifierKeys(metaState: Int, action: Int) {
        // Inject meta keys (SHIFT, CTRL, ALT) as separate key events
        if (metaState and KeyEvent.META_SHIFT_ON != 0) {
            injectKeyEventInternal(action, KeyEvent.KEYCODE_SHIFT_LEFT, 0)
        }
        if (metaState and KeyEvent.META_CTRL_ON != 0) {
            injectKeyEventInternal(action, KeyEvent.KEYCODE_CTRL_LEFT, 0)
        }
        if (metaState and KeyEvent.META_ALT_ON != 0) {
            injectKeyEventInternal(action, KeyEvent.KEYCODE_ALT_LEFT, 0)
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
        currentInputConnection?.sendKeyEvent(event)
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

    private fun reloadKeyboardLayouts() {
        Log.i(TAG, "Reloading all keyboard layouts.")

        // Clear existing layouts
        ClassFunctionsFiles.ensureMediaDirectoriesExistAndCopyDefaults(windowManager, this, resources)
        languageLayouts.clear()
        serviceLayouts.clear()

        val languageDirectory = File(Constants.MEDIA_LAYOUTS_LANGUAGE_DIRECTORY)
        val serviceDirectory = File(Constants.MEDIA_LAYOUTS_SERVICE_DIRECTORY)

        // Reload language layouts
        reloadLayouts(
            directory = languageDirectory,
            layoutType = "Language",
            onFileParsed = { keyboard, fileName, isFallback ->
                languageLayouts.add(Pair(keyboard, isFallback))
                Log.i(TAG, "Loaded language layout: $fileName (Fallback: $isFallback)")
            },
            onFallback = { fileName ->
                val fallbackLayout = loadFallbackLanguageLayout()
                languageLayouts.add(Pair(fallbackLayout, true))
                Log.w(TAG, "Using fallback language layout for: $fileName")
            }
        )

        // Reload service layouts
        reloadLayouts(
            directory = serviceDirectory,
            layoutType = "Service",
            onFileParsed = { keyboard, fileName, isFallback ->
                serviceLayouts[fileName] = Pair(keyboard, isFallback)
                Log.i(TAG, "Loaded service layout: $fileName (Fallback: $isFallback)")
            },
            onFallback = { fileName ->
                val fallbackLayout = loadFallbackServiceLayout()
                serviceLayouts[fileName] = Pair(fallbackLayout, true)
                Log.w(TAG, "Using fallback service layout for: $fileName")
            }
        )

        // Reset to a valid index
        if (currentLanguageLayoutIndex >= languageLayouts.size || currentLanguageLayoutIndex < 0) {
            currentLanguageLayoutIndex = 0
            Log.i(TAG, "Resetting currentLanguageLayoutIndex to 0")
        }

        Log.i(TAG, "Loaded ${languageLayouts.size} language layouts and ${serviceLayouts.size} service layouts.")
    }

    private fun reloadLayouts(
        directory: File,
        layoutType: String,
        onFileParsed: (CustomKeyboard, String, Boolean) -> Unit,
        onFallback: (String) -> Unit
    ) {
        Log.i(TAG, "Checking $layoutType layouts directory: ${directory.absolutePath}")

        if (!directory.exists() || !directory.isDirectory) {
            val errorMsg = "$layoutType layouts directory does not exist or is not a directory: ${directory.absolutePath}"
            ClassFunctionsPopups.showErrorPopup(windowManager, this, TAG, errorMsg)
            onFallback(Constants.LAYOUT_LANGUAGE_DEFAULT.takeIf { layoutType == "Language" }
                ?: Constants.LAYOUT_SERVICE_DEFAULT)
            return
        }

        val files = directory.listFiles()
            ?.filter { it.isFile && it.canRead() && it.name.endsWith(".json") }
            ?.sortedBy { it.name.lowercase() } // Sort files alphabetically, case-insensitive
            ?: emptyList()

        Log.i(TAG, "Found ${files.size} valid $layoutType layout files in directory.")

        if (files.isEmpty()) {
            Log.w(TAG, "No valid $layoutType layouts found. Loading default fallback layout.")
            onFallback(Constants.LAYOUT_LANGUAGE_DEFAULT.takeIf { layoutType == "Language" }
                ?: Constants.LAYOUT_SERVICE_DEFAULT)
            return
        }

        files.forEach { file ->
            try {
                Log.i(TAG, "Attempting to load $layoutType layout: ${file.name}")
                CustomKeyboard.fromJsonFile(this, file) { error ->
                    val errorMsg = "Error loading $layoutType layout from file: ${file.name}. Error: $error"
                    ClassFunctionsPopups.showErrorPopup(windowManager, this, TAG, errorMsg)
                    onFallback(file.name)
                }?.let { keyboard ->
                    onFileParsed(keyboard, file.nameWithoutExtension, false)
                    Log.i(TAG, "Successfully loaded $layoutType layout: ${file.name}")
                }
            } catch (e: Exception) {
                val parseError = "Failed to parse $layoutType layout: ${file.name}. Error: ${e.message}"
                Log.e(TAG, parseError)
                ClassFunctionsPopups.showErrorPopup(windowManager, this, TAG, parseError)
                onFallback(file.name)
            }
        }
    }






    private fun loadFallbackLanguageLayout(): CustomKeyboard {
        return try {
            val json = resources.openRawResource(R.raw.keyboard_default).bufferedReader().use { it.readText() }
            CustomKeyboard.fromJson(this, json)?.also {
                Log.i(TAG, "Fallback language layout loaded successfully.")
            } ?: throw Exception("Parsed fallback language layout is null")
        } catch (e: Exception) {
            val errorMsg = "Error loading fallback language layout: ${e.message}"
            ClassFunctionsPopups.showErrorPopup(windowManager, this, TAG, errorMsg)
            throw e
        }
    }

    private fun loadFallbackServiceLayout(): CustomKeyboard {
        return try {
            val json = resources.openRawResource(R.raw.keyboard_service).bufferedReader().use { it.readText() }
            CustomKeyboard.fromJson(this, json)?.also {
                Log.i(TAG, "Fallback service layout loaded successfully.")
            } ?: throw Exception("Parsed fallback service layout is null")
        } catch (e: Exception) {
            val errorMsg = "Error loading fallback service layout: ${e.message}"
            ClassFunctionsPopups.showErrorPopup(windowManager, this, TAG, errorMsg)
            throw e
        }
    }


}
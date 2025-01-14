package com.roalyr.customkeyboardengine

import android.os.Environment

class Constants {
    companion object{
        const val LAYOUT_LANGUAGE_DEFAULT = "keyboard-en-default"
        const val LAYOUT_SERVICE_DEFAULT = "keyboard-service-default"
        const val LAYOUT_CLIPBOARD_DEFAULT = "keyboard-clipboard-default"
        const val REFERENCE_DEFAULT = "reference.md"


        // Keyboard internals.
        const val KEYBOARD_MINIMAL_WIDTH = 500
        const val KEYBOARD_MINIMAL_HEIGHT = 500
        const val KEYBOARD_TRANSLATION_INCREMENT = 50
        const val KEYBOARD_TRANSLATION_TOP_OFFSET = 20
        const val KEYBOARD_TRANSLATION_BOTTOM_OFFSET = 40
        const val KEYBOARD_SCALE_INCREMENT = 50
        const val KEYCODE_IGNORE = -1 // If this is assigned - commit label or small label
        const val CLIPBOARD_MAX_SIZE = 10 // Define maximum clipboard size

        // Custom keycodes.
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
        const val KEYCODE_CYCLE_LANGUAGE_LAYOUT = -21
        const val KEYCODE_CLIPBOARD_ENTRY = -22
        const val KEYCODE_CLIPBOARD_ERASE = -23
        const val KEYCODE_OPEN_CLIPBOARD = -24


        // Default values.
        const val DEFAULT_KEY_HEIGHT = 40f // Logical DP
        const val DEFAULT_KEY_WIDTH = 10f  // Percentage of row width
        const val DEFAULT_LOGICAL_ROW_GAP = 0f // Logical DP
        const val DEFAULT_LOGICAL_KEY_GAP = 0f // Percentage of row width
        const val TOTAL_LOGICAL_WIDTH = 100f

        // Paths
        const val APP_MEDIA_NAME = "com.roalyr.customkeyboardengine"
        private const val LAYOUTS_DIRECTORY_NAME = "layouts"
        private const val LAYOUTS_LANGUAGE_NAME = "layouts-language"
        private const val LAYOUTS_SERVICE_NAME = "layouts-service"

        val MEDIA_APP_DIRECTORY: String
            get() = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).parent}/Android/media/$APP_MEDIA_NAME/"

        val MEDIA_LAYOUTS_LANGUAGE_DIRECTORY: String
            get() = "$MEDIA_APP_DIRECTORY/$LAYOUTS_DIRECTORY_NAME/$LAYOUTS_LANGUAGE_NAME"

        val MEDIA_LAYOUTS_SERVICE_DIRECTORY: String
            get() = "$MEDIA_APP_DIRECTORY/$LAYOUTS_DIRECTORY_NAME/$LAYOUTS_SERVICE_NAME"

    }

    object Actions {
        const val CHECK_OVERLAY_PERMISSION = "com.roalyr.customkeyboardengine.CHECK_OVERLAY_PERMISSION"
        const val CHECK_STORAGE_PERMISSIONS = "com.roalyr.customkeyboardengine.CHECK_STORAGE_PERMISSIONS"
    }

    object PermissionTypes {
        const val EXTRA_TYPE = "EXTRA_PERMISSION_TYPE"
        const val OVERLAY = "PERMISSION_TYPE_OVERLAY"
        const val STORAGE = "PERMISSION_TYPE_STORAGE"
    }

    object RequestCodes {
        const val STORAGE_PERMISSIONS = 1001
    }


}
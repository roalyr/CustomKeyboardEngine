package com.roalyr.new7rowkb

import android.os.Environment

class Constants {
    companion object{
        const val LAYOUT_LANGUAGE_DEFAULT = "keyboard-en-default"
        const val LAYOUT_SERVICE_DEFAULT = "keyboard-service-default"

        // Keyboard internals.
        const val KEYBOARD_MINIMAL_WIDTH = 500
        const val KEYBOARD_MINIMAL_HEIGHT = 500
        const val KEYBOARD_TRANSLATION_INCREMENT = 50
        const val KEYBOARD_TRANSLATION_TOP_OFFSET = 20
        const val KEYBOARD_TRANSLATION_BOTTOM_OFFSET = 40
        const val KEYBOARD_SCALE_INCREMENT = 50
        const val NOT_A_KEY = -1
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
        // Default values.
        const val DEFAULT_KEY_HEIGHT = 40f
        const val DEFAULT_KEY_WIDTH = 10f
        const val DEFAULT_ROW_GAP = 1f
        const val DEFAULT_KEY_GAP = 1f

        // Paths
        const val APP_MEDIA_NAME = "com.roalyr.new7rowkb"
        private const val LAYOUTS_DIRECTORY_NAME = "layouts"
        private const val LAYOUTS_LANGUAGE_NAME = "layouts-language"
        private const val LAYOUTS_SERVICE_NAME = "layouts-service"

        val MEDIA_DIRECTORY: String
            get() = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).parent}/Android/media/$APP_MEDIA_NAME"

        val MEDIA_LAYOUTS_LANGUAGE_DIRECTORY: String
            get() = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).parent}/Android/media/$APP_MEDIA_NAME/$LAYOUTS_DIRECTORY_NAME/$LAYOUTS_LANGUAGE_NAME"

        val MEDIA_LAYOUTS_SERVICE_DIRECTORY: String
            get() = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).parent}/Android/media/$APP_MEDIA_NAME/$LAYOUTS_DIRECTORY_NAME/$LAYOUTS_SERVICE_NAME"


    }

    object Actions {
        const val CHECK_OVERLAY_PERMISSION = "com.roalyr.new7rowkb.CHECK_OVERLAY_PERMISSION"
        const val CHECK_STORAGE_PERMISSIONS = "com.roalyr.new7rowkb.CHECK_STORAGE_PERMISSIONS"
    }

    object PermissionTypes {
        const val EXTRA_TYPE = "EXTRA_PERMISSION_TYPE"
        const val OVERLAY = "PERMISSION_TYPE_OVERLAY"
        const val STORAGE = "PERMISSION_TYPE_STORAGE"
    }

    object RequestCodes {
        const val STORAGE_PERMISSIONS = 1001
        const val OVERLAY_PERMISSION = 1002
    }


}
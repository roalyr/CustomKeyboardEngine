package com.roalyr.new7rowkb

import android.os.Environment

class Constants {
    companion object{
        // Actions.
        const val ACTION_CHECK_OVERLAY_PERMISSION = "com.roalyr.new7rowkb.CHECK_OVERLAY_PERMISSION"
        const val ACTION_CHECK_STORAGE_PERMISSIONS = "com.roalyr.new7rowkb.CHECK_STORAGE_PERMISSIONS"
        const val ACTION_MANAGE_OVERLAY_PERMISSION = "android.settings.action.MANAGE_OVERLAY_PERMISSION"
        //Permissions.
        const val EXTRA_PERMISSION_TYPE = "EXTRA_PERMISSION_TYPE"
        const val PERMISSION_TYPE_OVERLAY = "PERMISSION_TYPE_OVERLAY"
        const val PERMISSION_TYPE_STORAGE = "PERMISSION_TYPE_STORAGE"
        const val REQUEST_CODE_STORAGE_PERMISSIONS = 1001
        const val REQUEST_CODE_OVERLAY_PERMISSION = 1002
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
        // Default values.
        const val DEFAULT_KEY_HEIGHT = 40f
        const val DEFAULT_KEY_WIDTH = 10f
        const val DEFAULT_ROW_GAP = 1f
        const val DEFAULT_KEY_GAP = 1f

        // Paths
        // Base App Directory
        val APP_DIRECTORY: String
            get() = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)}/New7rowKB"

        // Layouts subdirectory
        val LAYOUTS_DIRECTORY: String
            get() = "$APP_DIRECTORY/layouts"
    }
}
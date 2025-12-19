package com.roalyr.customkeyboardengine

import android.os.Environment

class Constants {
    companion object{
        const val LAYOUT_LANGUAGE_DEFAULT = "keyboard_en_default"
        const val LAYOUT_SERVICE_DEFAULT = "keyboard_service_default"
        const val LAYOUT_CLIPBOARD_DEFAULT = "keyboard_clipboard_default"
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

        // Rendering defaults.
        const val DEFAULT_RENDERED_KEY_GAP = 5f
        const val DEFAULT_RENDERED_ROW_GAP = 5f
        const val DEFAULT_KEY_CORNER_RADIUS_FACTOR = 0.1f

        /**
         * Default accent color used for rendering when theme resolution fails.
         * Value: Soft purple (#6A5ACD)
         */
        const val DEFAULT_ACCENT_COLOR = 0xFF6A5ACD.toInt()

        /**
         * Default text color for dark theme rendering.
         * Value: Light grey (#CCCCCC)
         */
        const val TEXT_COLOR_DARK_THEME = 0xFFCCCCCC.toInt()

        /**
         * Default text color for light theme rendering.
         * Value: Dark grey (#333333)
         */
        const val TEXT_COLOR_LIGHT_THEME = 0xFF333333.toInt()

        /**
         * Margin from the top of the screen for error popups.
         */
        const val ERROR_POPUP_MARGIN_TOP = 50

        // Rendering text size factors (relative to key height)
        const val TEXT_SIZE_FACTOR_ICON = 0.6f
        const val TEXT_SIZE_FACTOR_SMALL_LABEL = 0.32f
        const val TEXT_SIZE_FACTOR_PRIMARY_LABEL = 0.37f
        const val TEXT_SIZE_FACTOR_CENTERED_LABEL = 0.5f
        const val TEXT_SIZE_FACTOR_MODIFIER_LABEL = 0.4f
        const val SMALL_LABEL_Y_POSITION_FACTOR = 0.35f
        const val LABEL_Y_OFFSET_FACTOR = 0.3f
        const val CLIPBOARD_KEY_PADDING_FACTOR = 0.1f

        // Paths
        private const val APP_MEDIA_NAME = "com.roalyr.customkeyboardengine"
        private const val LAYOUTS_DIRECTORY_NAME = "layouts"
        private const val LAYOUTS_LANGUAGE_NAME = "layouts-language"
        private const val LAYOUTS_SERVICE_NAME = "layouts-service"

        // Settings file constants
        /**
         * Filename for user-overridable settings (`settings.json`).
         * Located at: `Android/media/com.roalyr.customkeyboardengine/settings.json`
         */
        const val SETTINGS_FILENAME = "settings.json"

        /**
         * Resource name for default settings (`R.raw.settings_default`).
         * Used as fallback when user settings are missing or invalid.
         */
        const val SETTINGS_DEFAULT = "settings_default"

        val MEDIA_APP_DIRECTORY: String
            get() = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).parent}/Android/media/$APP_MEDIA_NAME/"

        val MEDIA_LAYOUTS_LANGUAGE_DIRECTORY: String
            get() = "$MEDIA_APP_DIRECTORY/$LAYOUTS_DIRECTORY_NAME/$LAYOUTS_LANGUAGE_NAME"

        val MEDIA_LAYOUTS_SERVICE_DIRECTORY: String
            get() = "$MEDIA_APP_DIRECTORY/$LAYOUTS_DIRECTORY_NAME/$LAYOUTS_SERVICE_NAME"

        val MEDIA_SETTINGS_FILE: String
            get() = "${MEDIA_APP_DIRECTORY}${SETTINGS_FILENAME}"

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
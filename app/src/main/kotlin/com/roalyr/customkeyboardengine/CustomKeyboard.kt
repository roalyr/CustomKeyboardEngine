package com.roalyr.customkeyboardengine

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Represents the root keyboard layout structure with default spacing and sizing values.
 *
 * @property rows List of [Row] objects defining the keyboard layout.
 * @property defaultKeyHeight Default height for all keys in DP (fallback for Row and Key levels).
 * @property defaultKeyWidth Default width for all keys as percentage of row width.
 * @property defaultLogicalRowGap Default vertical gap between rows in DP.
 * @property defaultLogicalKeyGap Default horizontal gap between keys as percentage of row width.
 */
@Serializable
data class KeyboardLayout(
    val rows: List<Row>,
    val defaultKeyHeight: Float? = null, // Absolute logical DP (null = use settings or Constants)
    val defaultKeyWidth: Float? = null,  // Percentage of row width (null = use settings or Constants)
    val defaultLogicalRowGap: Float? = null, // Absolute logical DP (null = use settings or Constants)
    val defaultLogicalKeyGap: Float? = null  // Percentage of row width (null = use settings or Constants)
)

/**
 * Represents a single row in the keyboard with optional overrides for spacing and sizing.
 *
 * @property keys List of [Key] objects in this row.
 * @property logicalRowGap Optional vertical gap below this row; overrides [KeyboardLayout.defaultLogicalRowGap].
 * @property defaultKeyHeight Optional height for keys in this row; overrides [KeyboardLayout.defaultKeyHeight].
 * @property defaultKeyWidth Optional width for keys in this row; overrides [KeyboardLayout.defaultKeyWidth].
 * @property defaultLogicalKeyGap Optional horizontal gap for keys in this row; overrides [KeyboardLayout.defaultLogicalKeyGap].
 */
@Serializable
data class Row(
    val keys: List<Key>,
    val logicalRowGap: Float? = null, // Overrides KeyboardLayout.defaultLogicalRowGap for this row
    val defaultKeyHeight: Float? = null, // Overrides KeyboardLayout.defaultKeyHeight for this row
    val defaultKeyWidth: Float? = null, // Overrides KeyboardLayout.defaultKeyWidth for this row
    val defaultLogicalKeyGap: Float? = null // Overrides KeyboardLayout.defaultLogicalKeyGap for this row
)

/**
 * Represents a single key in the keyboard with positioning, styling, and action properties.
 *
 * @property keyCode Primary key code; if null, the key commits its [label].
 * @property keyCodeLongPress Optional key code triggered on long press.
 * @property label Primary text displayed on the key.
 * @property labelLongPress Optional secondary text shown on long press.
 * @property icon Optional path to drawable icon (e.g., `@drawable/ic_backspace`).
 * @property keyWidth Optional width as percentage of row width.
 * @property keyHeight Optional height in DP.
 * @property logicalKeyGap Optional horizontal gap to next key as percentage of row width.
 * @property isRepeatable If true, key repeats when held.
 * @property isModifier If true, key is rendered as a modifier key (e.g., Shift, Tab).
 * @property preserveLabelCase If true, label case is not changed by Shift or Caps Lock.
 * @property preserveSmallLabelCase If true, small label case is not changed by Shift or Caps Lock.
 * @property x Logical X position (auto-calculated, do not set manually).
 * @property y Logical Y position (auto-calculated, do not set manually).
 * @property id Unique key identifier within layout (auto-calculated, do not set manually).
 */
@Serializable
data class Key(
    val keyCode: Int? = null,
    val keyCodeLongPress: Int? = null,
    var label: String? = null,
    val labelLongPress: String? = null,
    val icon: String? = null,

    var keyWidth: Float? = null, // Width percentage
    var keyHeight: Float? = null, // Absolute logical DP
    var logicalKeyGap: Float? = null, // Gap percentage

    val isRepeatable: Boolean? = false,
    val isModifier: Boolean? = false,

    val preserveLabelCase: Boolean? = false,
    val preserveSmallLabelCase: Boolean? = false,

    var x: Float = 0f, // Logical X
    var y: Float = 0f,  // Logical Y
    var id: Int = 0 // Unique ID for each key
)

/**
 * User-overridable settings loaded from `settings.json`.
 *
 * All properties are non-null with defaults so kotlinx.serialization can safely coerce missing or null values.
 */
@Serializable
data class KeyboardSettings(
    // Keyboard dimensions
    val keyboardMinimalWidth: Int = Constants.KEYBOARD_MINIMAL_WIDTH,
    val keyboardMinimalHeight: Int = Constants.KEYBOARD_MINIMAL_HEIGHT,

    // Translation/Scale
    val keyboardTranslationIncrement: Int = Constants.KEYBOARD_TRANSLATION_INCREMENT,
    val keyboardScaleIncrement: Int = Constants.KEYBOARD_SCALE_INCREMENT,

    // Key defaults
    val defaultKeyHeight: Float = Constants.DEFAULT_KEY_HEIGHT,
    val defaultKeyWidth: Float = Constants.DEFAULT_KEY_WIDTH,
    val defaultLogicalRowGap: Float = Constants.DEFAULT_LOGICAL_ROW_GAP,
    val defaultLogicalKeyGap: Float = Constants.DEFAULT_LOGICAL_KEY_GAP,

    // Rendering
    val renderedKeyGap: Float = Constants.DEFAULT_RENDERED_KEY_GAP,
    val renderedRowGap: Float = Constants.DEFAULT_RENDERED_ROW_GAP,
    val keyCornerRadiusFactor: Float = Constants.DEFAULT_KEY_CORNER_RADIUS_FACTOR
)


/**
 * Manages keyboard layout structure, key positioning, and layout calculations.
 *
 * @property context Android context for resource access.
 * @property layout The root [KeyboardLayout] configuration.
 * @property rows Computed list of [Row] objects with calculated positions and dimensions.
 */
class CustomKeyboard(
    private val context: Context,
    private val layout: KeyboardLayout, // Declare as a class property with `val`
    private val settings: KeyboardSettings? = null // Optional user settings for defaults override
) {
    // Effective default values: Layout JSON > Settings > Constants
    private val effectiveDefaultKeyHeight: Float
        get() = layout.defaultKeyHeight ?: settings?.defaultKeyHeight ?: Constants.DEFAULT_KEY_HEIGHT
    private val effectiveDefaultKeyWidth: Float
        get() = layout.defaultKeyWidth ?: settings?.defaultKeyWidth ?: Constants.DEFAULT_KEY_WIDTH
    private val effectiveDefaultLogicalRowGap: Float
        get() = layout.defaultLogicalRowGap ?: settings?.defaultLogicalRowGap ?: Constants.DEFAULT_LOGICAL_ROW_GAP
    private val effectiveDefaultLogicalKeyGap: Float
        get() = layout.defaultLogicalKeyGap ?: settings?.defaultLogicalKeyGap ?: Constants.DEFAULT_LOGICAL_KEY_GAP

    /** Pre-calculated rows with computed key positions and dimensions. */
    val rows: List<Row> = buildRows(layout)

    /**
     * Total logical width of the keyboard (fixed at 100 logical units).
     */
    val totalLogicalWidth: Float
        get() = Constants.TOTAL_LOGICAL_WIDTH

    /**
     * Total logical height of the keyboard, calculated as sum of all row heights and gaps.
     */
    val totalLogicalHeight: Float
        get() {
            var totalHeight = 0f
            for (row in rows) {
                val rowHeight = row.defaultKeyHeight ?: effectiveDefaultKeyHeight
                val rowGap = row.logicalRowGap ?: effectiveDefaultLogicalRowGap
                totalHeight += rowHeight + rowGap
            }
            return totalHeight
        }

    /**
     * Constructs keyboard rows with calculated positions and dimensions for each key.
     *
     * Performs cascade resolution of sizing and spacing values from Key → Row → KeyboardLayout.
     * Assigns unique IDs and logical coordinates (x, y) to each key.
     *
     * @param layout The keyboard layout containing row definitions.
     * @return List of rows with calculated key positions and dimensions.
     * @throws IllegalArgumentException if layout contains no rows.
     */
    private fun buildRows(layout: KeyboardLayout): List<Row> {
        val builtRows = mutableListOf<Row>()
        var currentY = 0f
        var uniqueIdCounter = 0 // Unique ID for all keys

        layout.rows.forEach { row ->
            val rowHeight = row.defaultKeyHeight ?: effectiveDefaultKeyHeight
            val rowGap = row.logicalRowGap ?: effectiveDefaultLogicalRowGap
            val defaultWidth = row.defaultKeyWidth ?: effectiveDefaultKeyWidth
            val defaultGap = row.defaultLogicalKeyGap ?: effectiveDefaultLogicalKeyGap

            var currentX = 0f
            val keys = row.keys.map { key ->
                key.apply {
                    id = uniqueIdCounter++ // Assign a unique ID to each key
                    keyWidth = keyWidth ?: defaultWidth
                    keyHeight = keyHeight ?: rowHeight
                    logicalKeyGap = logicalKeyGap ?: defaultGap

                    x = currentX
                    y = currentY
                }
                currentX += (key.keyWidth ?: defaultWidth) + (key.logicalKeyGap ?: defaultGap)
                key
            }

            builtRows.add(Row(keys, rowGap, rowHeight, defaultWidth, defaultGap))
            currentY += rowHeight + rowGap
        }
        if (builtRows.isEmpty()) {
            throw IllegalArgumentException("No rows defined in the keyboard layout")
        }
        return builtRows
    }


    /**
     * Retrieves all keys from all rows in the keyboard layout.
     *
     * @return Flat list of all [Key] objects in layout order.
     */
    fun getAllKeys(): List<Key> {
        return rows.flatMap { it.keys }
    }

    /**
     * Retrieves the key at the specified logical coordinates.
     *
     * Uses hit-testing to find the key containing the point (x, y).
     *
     * @param x Logical X coordinate.
     * @param y Logical Y coordinate.
     * @return The [Key] at the given coordinates, or null if no key is found.
     */
    fun getKeyAt(x: Float, y: Float): Key? {
        return rows.flatMap { it.keys }.firstOrNull { key ->
            x >= key.x && x < (key.x + (key.keyWidth ?: 0f)) &&
                    y >= key.y && y < (key.y + (key.keyHeight ?: 0f))
        }
    }

    /**
     * Retrieves a key by its key code or long-press key code.
     *
     * @param code The key code to search for (matches either [Key.keyCode] or [Key.keyCodeLongPress]).
     * @return The [Key] with the matching code, or null if not found.
     */
    fun getKeyByCode(code: Int): Key? {
        rows.forEach { row ->
            row.keys.find { it.keyCode == code || it.keyCodeLongPress == code }?.let { return it }
        }
        return null
    }

    companion object {
        private const val TAG = "CustomKeyboard"

        /**
         * Creates a [CustomKeyboard] instance from a JSON string.
         *
         * Uses kotlinx.serialization with [coerceInputValues] enabled to handle missing optional fields gracefully.
         *
         * @param context Android context for resource access.
         * @param json JSON string containing keyboard layout definition.
         * @param settings Optional user settings to override layout defaults.
         * @return New [CustomKeyboard] instance.
         * @throws SerializationException if JSON is invalid or cannot be parsed.
         */
        fun fromJson(context: Context, json: String, settings: KeyboardSettings? = null): CustomKeyboard {
            val layout = Json { coerceInputValues = true }.decodeFromString<KeyboardLayout>(json)
            return CustomKeyboard(context, layout, settings)
        }

        /**
         * Creates a [CustomKeyboard] instance from a JSON file.
         *
         * Safely handles file reading errors and parsing exceptions via callback.
         *
         * @param context Android context for resource access.
         * @param file File containing keyboard layout JSON definition.
         * @param settings Optional user settings to override layout defaults.
         * @param onError Callback invoked with error message if parsing fails.
         * @return New [CustomKeyboard] instance, or null if parsing failed.
         */
        fun fromJsonFile(
            context: Context,
            file: File,
            settings: KeyboardSettings? = null,
            onError: (String) -> Unit // Error handler callback
        ): CustomKeyboard? {
            return try {
                val content = file.readText()
                fromJson(context, content, settings)
            } catch (e: Exception) {
                val errorMessage = "$TAG: Error parsing file ${file.name}: ${e.message}"
                onError(errorMessage) // Trigger the error callback
                null
            }
        }
    }
}

package com.roalyr.customkeyboardengine

import android.content.Context
import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class KeyboardLayout(
    val rows: List<Row>,
    val defaultKeyHeight: Float = Constants.DEFAULT_KEY_HEIGHT,     // Absolute value in DP
    val defaultKeyWidth: Float = Constants.DEFAULT_KEY_WIDTH,      // Percentage of row width
    val defaultRowGap: Float = Constants.DEFAULT_ROW_GAP,         // Absolute value in DP (space between rows)
    val defaultKeyGap: Float = Constants.DEFAULT_KEY_GAP          // Absolute value in DP (space between keys)
)

@Serializable
data class Row(
    val keys: List<Key>,
    val rowHeight: Float? = null,         // Row-specific height, fallback to KeyboardLayout.defaultKeyHeight
    val rowGap: Float? = null,            // Space below this row, fallback to KeyboardLayout.defaultRowGap
    val defaultKeyWidth: Float? = null,   // Default width for keys in this row, fallback to KeyboardLayout.defaultKeyWidth
    val defaultKeyGap: Float? = null      // Default gap for keys in this row, fallback to KeyboardLayout.defaultKeyGap
)

@Serializable
data class Key(
    val keyCode: Int = Constants.NOT_A_KEY,
    val keyCodeLongPress: Int? = null,
    val isRepeatable: Boolean = false,
    val isModifier: Boolean = false,
    val isSticky: Boolean = false,
    var label: String? = null,
    val smallLabel: String? = null,
    val icon: String? = null,
    var keyWidth: Float? = null,          // Width percentage, fallback to Row.defaultKeyWidth or Layout.defaultKeyWidth
    var keyHeight: Float? = null,         // Absolute DP value, fallback to Row.rowHeight or Layout.defaultKeyHeight
    var keyGap: Float? = null,            // Gap percentage, fallback to Row.defaultKeyGap or Layout.defaultKeyGap
    var x: Float = 0f,                     // Logical X position (calculated)
    var y: Float = 0f                      // Logical Y position (calculated)
)

class CustomKeyboard(
    private val context: Context,
    private val layout: KeyboardLayout // Declare as a class property with `val`
) {
    val rows: List<Row> = buildRows(layout)
    val defaultKeyHeight: Float = layout.defaultKeyHeight ?: Constants.DEFAULT_KEY_HEIGHT

    // Total logical width of the keyboard
    val totalLogicalWidth: Float
        get() {
            var maxWidth = 0f
            for (row in rows) {
                var rowWidth = 0f
                for (key in row.keys) {
                    rowWidth += key.keyWidth ?: layout.defaultKeyWidth
                }
                if (rowWidth > maxWidth) {
                    maxWidth = rowWidth
                }
            }
            return maxWidth
        }

    // Total logical height of the keyboard
    val totalLogicalHeight: Float
        get() {
            var totalHeight = 0f
            for (row in rows) {
                val rowHeight = row.rowHeight ?: layout.defaultKeyHeight
                val rowGap = row.rowGap ?: layout.defaultRowGap
                totalHeight += rowHeight + rowGap
            }
            return totalHeight
        }


    private fun buildRows(layout: KeyboardLayout): List<Row> {
        var currentY = 0f

        return layout.rows.map { row ->
            val rowHeight = row.rowHeight ?: layout.defaultKeyHeight
            val rowGap = row.rowGap ?: layout.defaultRowGap
            val defaultWidth = row.defaultKeyWidth ?: layout.defaultKeyWidth
            val defaultGap = row.defaultKeyGap ?: layout.defaultKeyGap

            var currentX = 0f
            val keys = row.keys.map { key ->
                key.apply {
                    keyWidth = keyWidth ?: defaultWidth
                    keyHeight = keyHeight ?: rowHeight
                    keyGap = keyGap ?: defaultGap

                    x = currentX
                    y = currentY
                }
                currentX += key.keyWidth!! + key.keyGap!!
                key
            }
            currentY += rowHeight + rowGap
            Row(keys, rowHeight, rowGap, defaultWidth, defaultGap)
        }
    }


    // Retrieve all keys
    fun getAllKeys(): List<Key> = rows.flatMap { it.keys }

    // Retrieve key at logical coordinates
    fun getKeyAt(x: Float, y: Float): Key? {
        return rows.flatMap { it.keys }.firstOrNull { key ->
            x >= key.x && x < (key.x + (key.keyWidth ?: 0f)) &&
                    y >= key.y && y < (key.y + (key.keyHeight ?: 0f))
        }
    }


    // Fetch key by its code
    fun getKeyByCode(code: Int): Key? {
        rows.forEach { row ->
            row.keys.find { it.keyCode == code || it.keyCodeLongPress == code }?.let { return it }
        }
        return null
    }

    companion object {
        private const val TAG = "CustomKeyboard"
        fun fromJson(context: Context, json: String): CustomKeyboard {
            val layout = Json { coerceInputValues = true }.decodeFromString<KeyboardLayout>(json)
            return CustomKeyboard(context, layout)
        }

        fun fromJsonFile(
            context: Context,
            file: File,
            onError: (String) -> Unit // Error handler callback
        ): CustomKeyboard? {
            return try {
                val content = file.readText()
                Log.i(TAG, "Attempting to parse file: ${file.name}")
                fromJson(context, content)
            } catch (e: Exception) {
                val errorMessage = "$TAG: Error parsing file ${file.name}: ${e.message}"
                onError(errorMessage) // Trigger the error callback
                null
            }
        }


    }
}

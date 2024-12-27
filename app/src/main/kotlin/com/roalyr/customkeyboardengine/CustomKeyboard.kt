package com.roalyr.customkeyboardengine

import android.content.Context
import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class KeyboardLayout(
    val rows: List<Row>,
    val defaultKeyHeight: Float = Constants.DEFAULT_KEY_HEIGHT, // Absolute logical DP
    val defaultKeyWidth: Float = Constants.DEFAULT_KEY_WIDTH,  // Percentage of row width
    val defaultLogicalRowGap: Float = Constants.DEFAULT_LOGICAL_ROW_GAP, // Absolute logical DP
    val defaultLogicalKeyGap: Float = Constants.DEFAULT_LOGICAL_KEY_GAP  // Percentage of row width
)

@Serializable
data class Row(
    val keys: List<Key>,
    val logicalRowGap: Float? = null, // Overrides KeyboardLayout.defaultLogicalRowGap for this row
    val defaultKeyHeight: Float? = null, // Overrides KeyboardLayout.defaultKeyHeight for this row
    val defaultKeyWidth: Float? = null, // Overrides KeyboardLayout.defaultKeyWidth for this row
    val defaultLogicalKeyGap: Float? = null // Overrides KeyboardLayout.defaultLogicalKeyGap for this row
)

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
    var y: Float = 0f  // Logical Y
)


class CustomKeyboard(
    private val context: Context,
    private val layout: KeyboardLayout // Declare as a class property with `val`
) {
    val rows: List<Row> = buildRows(layout)

    // Total logical width of the keyboard
    val totalLogicalWidth: Float
        get() = Constants.TOTAL_LOGICAL_WIDTH

    // Total logical height of the keyboard
    val totalLogicalHeight: Float
        get() {
            var totalHeight = 0f
            for (row in rows) {
                val rowHeight = row.defaultKeyHeight ?: layout.defaultKeyHeight
                val rowGap = row.logicalRowGap ?: layout.defaultLogicalRowGap
                totalHeight += rowHeight + rowGap
            }
            return totalHeight
        }

    private fun buildRows(layout: KeyboardLayout): List<Row> {
        var currentY = 0f

        return layout.rows.map { row ->
            val rowHeight = row.defaultKeyHeight ?: layout.defaultKeyHeight
            val rowGap = row.logicalRowGap ?: layout.defaultLogicalRowGap
            val defaultWidth = row.defaultKeyWidth ?: layout.defaultKeyWidth
            val defaultGap = row.defaultLogicalKeyGap ?: layout.defaultLogicalKeyGap

            var currentX = 0f
            val keys = row.keys.map { key ->
                key.apply {
                    keyWidth = keyWidth ?: defaultWidth
                    keyHeight = keyHeight ?: rowHeight
                    logicalKeyGap = logicalKeyGap ?: defaultGap

                    x = currentX
                    y = currentY
                }
                currentX += key.keyWidth!! + key.logicalKeyGap!!
                key
            }
            currentY += rowHeight + rowGap
            Row(keys, rowGap, rowHeight, defaultWidth, defaultGap)
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

package com.roalyr.new7rowkb

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class KeyboardLayout(
    val rows: List<Row>,
    val defaultWidth: Int = 50,
    val defaultHeight: Int = 50,
    val horizontalGap: Int = 5,
    val verticalGap: Int = 5,
)

@Serializable
data class Row(
    val keys: List<Key>,
    var height: Int = 40, //TODO: read from json
)

@Serializable
data class Key(
    var codes: Int = -1,
    var codesLongPress: Int = -1,
    var repeatable: Boolean = false,
    var modifier: Boolean = false,
    var label: String? = null,
    var labelSmall: String? = null,
    var icon: String? = null,
    var width: Int = 8,     // Default percentage width
    var height: Int = 10,    // Default percentage height
    var gap: Int = 0,        // Default percentage gap
    var x: Int = 0,          // Percentage-based x position
    var y: Int = 0           // Percentage-based y position
)

class CustomKeyboard(private val context: Context, layout: KeyboardLayout) {
    val width: Int
        get() = rows.maxOfOrNull { row -> row.keys.sumOf { it.width + it.gap } } ?: 0

    val height: Int
        get() = rows.sumOf { it.height }

    val totalLogicalWidth: Int
        get() = rows.maxOfOrNull { row -> row.keys.sumOf { it.width + it.gap } } ?: 0

    val totalLogicalHeight: Int
        get() = rows.sumOf { it.height }

    val rows: List<Row> = buildRows(layout.rows)
    private fun buildRows(rowDataList: List<Row>): List<Row> {
        var currentY = 0 // Tracks the Y-coordinate of each row
        return rowDataList.map { row ->
            var currentX = 0 // Tracks the X-coordinate for keys in the row

            // Update each key's position within the row
            val updatedKeys = row.keys.map { key ->
                key.apply {
                    x = currentX
                    y = currentY
                }
                currentX += key.width + key.gap // Advance X position by key width and gap
                key
            }

            currentY += row.height // Move to the next row by adding the row height
            Row(updatedKeys)
        }
    }

    companion object {
        fun fromJson(context: Context, json: String): CustomKeyboard {
            val layout = Json.decodeFromString<KeyboardLayout>(json)
            return CustomKeyboard(context, layout)
        }

        fun fromJsonFile(context: Context, file: File): CustomKeyboard? {
            return if (file.exists()) {
                file.readText().let { fromJson(context, it) }
            } else null
        }
    }

    fun getKeyAt(x: Int, y: Int): Key? {
        return getAllKeys().firstOrNull { key ->
            x >= key.x && x < key.x + key.width &&
                    y >= key.y && y < key.y + key.height
        }
    }

    fun getAllKeys(): List<Key> {
        return rows.flatMap { it.keys }
    }

    fun getKeyByCode(code: Int): Key? {
        rows.forEach { row ->
            row.keys.find { it.codes == code || it.codesLongPress == code }?.let { return it }
        }
        return null
    }
}

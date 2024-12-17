package com.roalyr.new7rowkb

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.DisplayMetrics
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class KeyboardLayout(
    val rows: List<Row>,
    val defaultWidth: Int = 50,
    val defaultHeight: Int = 50,
    val horizontalGap: Int = 5,
    val verticalGap: Int = 5
)

@Serializable
data class Row(
    val keys: List<Key>,
    var height: Int = 50,
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
    var width: Int = 15,     // Default percentage width
    var height: Int = 15,    // Default percentage height
    var gap: Int = 2,        // Default percentage gap
    var x: Int = 0,          // Percentage-based x position
    var y: Int = 0           // Percentage-based y position
)


fun Int.toPx(displayMetrics: DisplayMetrics): Int = (this * displayMetrics.density + 0.5f).toInt()

class CustomKeyboard(private val context: Context, layout: KeyboardLayout) {

    val rows: List<Row> = buildRows(layout.rows)
    private val displayMetrics: DisplayMetrics = context.resources.displayMetrics
    private val drawableCache = mutableMapOf<String, Drawable?>()
    private val keyBackground: Drawable? = null

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

    private fun buildRows(rowDataList: List<Row>): List<Row> {
        var currentY = 0
        return rowDataList.map { row ->
            var currentX = 0
            val keys = row.keys.map { key ->
                val newKey = key.copy(
                    x = currentX,
                    y = currentY
                )
                currentX += newKey.width + newKey.gap // Width and gap are percentages
                newKey
            }
            currentY += 15 // Default key height percentage increment
            Row(keys)
        }
    }


    fun resize(newWidth: Int, newHeight: Int) {
        rows.forEach { row ->
            var x = 0
            val scaleFactor = newWidth / row.keys.sumOf { it.width + it.gap }.toFloat()
            row.keys.forEach { key ->
                key.width = (key.width * scaleFactor).toInt()
                key.x = x
                x += key.width + key.gap
            }
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

    fun getKeyBackground(): Drawable? {
        return keyBackground
    }

    fun getKeyByCode(code: Int): Key? {
        rows.forEach { row ->
            row.keys.find { it.codes == code || it.codesLongPress == code }?.let { return it }
        }
        return null
    }

    fun logKeys() {
        rows.forEachIndexed { rowIndex, row ->
            println("Row $rowIndex:")
            row.keys.forEach { key ->
                println("Key: Code=${key.codes}, Label=${key.label}, Position=(${key.x}, ${key.y}), Size=(${key.width}, ${key.height})")
            }
        }
    }
}

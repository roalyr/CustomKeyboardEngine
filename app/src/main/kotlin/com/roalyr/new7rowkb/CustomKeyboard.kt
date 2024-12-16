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
    val keys: List<Key>
)

@Serializable
data class Key(
    val code: Int,
    val label: String? = null,
    val icon: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val gap: Int? = null,
    val compound: Boolean = false,
    val labelSmall: String? = null,
    val codesLongPress: Int? = null,
    val rowEdgeFlags: String? = null, // Determines key alignment (e.g., "bottom", "top").
    val repeatable: Boolean = false,

    var x: Int = 0, // Calculated during layout
    var y: Int = 0,  // Calculated during layout
)

// Extension function for dp-to-px conversion
fun Int.toPx(displayMetrics: DisplayMetrics): Int = (this * displayMetrics.density + 0.5f).toInt()

class CustomKeyboard(private val context: Context, layout: KeyboardLayout) {

    private val resources = context.resources
    private val displayMetrics: DisplayMetrics = resources.displayMetrics

    private val defaultWidth: Int = layout.defaultWidth.toPx(displayMetrics)
    private val defaultHeight: Int = layout.defaultHeight.toPx(displayMetrics)
    private val horizontalGap: Int = layout.horizontalGap.toPx(displayMetrics)
    val verticalGap: Int = layout.verticalGap.toPx(displayMetrics)

    private val drawableCache = mutableMapOf<String, Drawable?>()

    private val rows: List<Row> = buildRows(layout.rows)
    private val keys: List<Key> = rows.flatMap { it.keys }

    private fun buildRows(rowDataList: List<Row>): List<Row> {
        var currentY = 0
        return rowDataList.map { row ->
            val (newRow, nextY) = createRow(row, currentY)
            currentY = nextY
            newRow
        }
    }

    private fun createRow(rowData: Row, startY: Int): Pair<Row, Int> {
        var currentX = 0
        val keys = rowData.keys.map { key ->
            val keyWidth = key.width?.toPx(displayMetrics) ?: defaultWidth
            val keyHeight = key.height?.toPx(displayMetrics) ?: defaultHeight
            val keyGap = key.gap?.toPx(displayMetrics) ?: horizontalGap

            // Load icon if needed
            val iconDrawable = key.icon?.let { loadDrawable(it) }

            val newKey = key.copy(
                x = currentX,
                y = startY,
                width = keyWidth,
                height = keyHeight,
                gap = keyGap,
                icon = iconDrawable?.toString()
            )
            currentX += keyWidth + keyGap
            newKey
        }
        val rowHeight = (rowData.keys.maxOfOrNull { it.height?.toPx(displayMetrics) ?: defaultHeight } ?: defaultHeight)
        return rowData.copy(keys = keys) to (startY + rowHeight + verticalGap)
    }

    private fun loadDrawable(assetPath: String): Drawable? {
        return drawableCache.getOrPut(assetPath) {
            try {
                Drawable.createFromStream(resources.assets.open(assetPath), null)
            } catch (e: Exception) {
                null
            }
        }
    }

    fun getDimensionOrFraction(value: String?, base: Int, defValue: Int): Int {
        return when {
            value == null -> defValue
            value.endsWith("%p") -> {
                val fraction = value.removeSuffix("%p").toFloatOrNull() ?: return defValue
                (base * fraction / 100).toInt()
            }
            value.endsWith("dp") -> {
                val dpValue = value.removeSuffix("dp").toFloatOrNull() ?: return defValue
                (dpValue * displayMetrics.density + 0.5f).toInt()
            }
            value.endsWith("px") -> {
                value.removeSuffix("px").toIntOrNull() ?: defValue
            }
            else -> value.toIntOrNull() ?: defValue
        }
    }

    fun getKeyAt(x: Int, y: Int): Key? {
        return rows.flatMap { it.keys }.find { key ->
            val keyRight = key.x + (key.width ?: defaultWidth)
            val keyBottom = key.y + (key.height ?: defaultHeight)
            x in key.x until keyRight && y in key.y until keyBottom
        }
    }

    fun getAllKeys(): List<Key> = keys

    fun logKeys() {
        rows.forEachIndexed { rowIndex, row ->
            row.keys.forEachIndexed { keyIndex, key ->
                println("Row $rowIndex, Key $keyIndex: Code=${key.code}, Position=(${key.x}, ${key.y}), Size=(${key.width}, ${key.height})")
            }
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
}

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
    val repeatable: Boolean = false
)

// Extension function for dp-to-px conversion
fun Int.toPx(displayMetrics: DisplayMetrics): Int = (this * displayMetrics.density + 0.5f).toInt()

class CustomKeyboard(private val context: Context, layout: KeyboardLayout) {

    private val resources = context.resources
    private val displayMetrics: DisplayMetrics = resources.displayMetrics

    private val defaultWidth: Int = layout.defaultWidth.toPx(displayMetrics)
    private val defaultHeight: Int = layout.defaultHeight.toPx(displayMetrics)
    private val horizontalGap: Int = layout.horizontalGap.toPx(displayMetrics)
    private val verticalGap: Int = layout.verticalGap.toPx(displayMetrics)

    private val rows: List<Row> = layout.rows.map { createRow(it) }
    private val drawableCache = mutableMapOf<String, Drawable?>()

    private fun createRow(rowData: Row): Row {
        return rowData.copy(keys = rowData.keys.map { createKey(it) })
    }

    private fun createKey(keyData: Key): Key {
        val iconDrawable = keyData.icon?.let { loadDrawable(it) }
        return keyData.copy(
            icon = iconDrawable?.toString(),
            width = keyData.width?.toPx(displayMetrics) ?: defaultWidth,
            height = keyData.height?.toPx(displayMetrics) ?: defaultHeight,
            gap = keyData.gap?.toPx(displayMetrics) ?: horizontalGap
        )
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

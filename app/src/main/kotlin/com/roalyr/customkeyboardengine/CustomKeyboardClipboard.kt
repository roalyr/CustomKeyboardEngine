package com.roalyr.customkeyboardengine

import android.util.Log

object CustomKeyboardClipboard {
    val TAG = "CustomKeyboardClipboard"
    val clipboardMap = mutableMapOf<Int, String?>() // Map of key ID to clipboard content

    fun addClipboardEntry(keyId: Int, content: String?) {
        clipboardMap[keyId] = content
    }

    fun getClipboardEntry(keyId: Int): String? {
        return clipboardMap[keyId]
    }

    fun clearClipboard() {
        clipboardMap.clear()
    }

    fun containsValue(value: String): Boolean {
        return clipboardMap.values.any { it == value }
    }

    fun shiftClipboardEntries(clipboardKeys: List<Key>) {
        for (i in 1 until clipboardKeys.size) {
            val currentKey = clipboardKeys[i]
            val previousKey = clipboardKeys[i - 1]
            val currentContent = getClipboardEntry(currentKey.id)

            if (currentContent != null) {
                addClipboardEntry(previousKey.id, currentContent)
                Log.i(TAG, "Shifted content from key ID ${currentKey.id} to ${previousKey.id}")
            }
        }
        // Clear the last key after shifting
        val lastKey = clipboardKeys.last()
        addClipboardEntry(lastKey.id, null)
        Log.i(TAG, "Cleared content from key ID ${lastKey.id}")
    }



}

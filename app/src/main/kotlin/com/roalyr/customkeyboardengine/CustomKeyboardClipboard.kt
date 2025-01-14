package com.roalyr.customkeyboardengine

object CustomKeyboardClipboard {
    private val TAG = "Clipboard"
    private val clipboardMap = mutableMapOf<Int, String?>()

    fun initializeClipboardKeys(keyIds: List<Int>) {
        if (keyIds.isEmpty()) {
            //Log.i(TAG, "No clipboard keys to initialize.")
            return
        }

        // Preserve existing entries for matching keys
        val currentEntries = clipboardMap.toMap()
        clipboardMap.clear()
        keyIds.forEachIndexed { index, keyId ->
            clipboardMap[keyId] = currentEntries[keyId] ?: currentEntries.values.elementAtOrNull(index) ?: null
        }

        //Log.i(TAG, "Initialized clipboard keys: $keyIds")
    }


    fun ensureMapSize(size: Int) {
        if (clipboardMap.size < size) {
            val missingKeys = (clipboardMap.size until size).toList()
            missingKeys.forEach { clipboardMap[it] = null }
            //Log.i(TAG, "Ensured clipboard map size: $size, added keys: $missingKeys")
        }
    }


    fun containsValue(value: String?): Boolean {
        return clipboardMap.values.any { it == value }
    }


    fun addClipboardEntry(content: String) {
        if (clipboardMap.isEmpty()) {
            //Log.w(TAG, "Clipboard map is empty. No keys available to store the entry.")
            return
        }

        val keys = clipboardMap.keys.toList()
        if (containsValue(content)) {
            return
        }

        // Shift entries and add the new content to the first key
        for (i in keys.size - 1 downTo 1) {
            clipboardMap[keys[i]] = clipboardMap[keys[i - 1]]
        }
        clipboardMap[keys.first()] = content
    }

    fun getClipboardEntry(keyId: Int): String? {
        return clipboardMap[keyId]
    }

    fun clearClipboard() {
        clipboardMap.keys.forEach { clipboardMap[it] = null }
        //Log.i(TAG, "Cleared all clipboard entries.")
    }

}

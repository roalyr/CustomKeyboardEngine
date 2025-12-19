package com.roalyr.customkeyboardengine

import android.content.Context
import android.content.res.Resources
import android.view.WindowManager
import kotlinx.serialization.json.Json
import java.io.File

class ClassFunctionsFiles {
    companion object {
        private const val TAG = "ClassFunctionsFiles"

        fun ensureMediaDirectoriesExistAndCopyDefaults(
            windowManager: WindowManager,
            context: Context,
            resources: Resources
        ): Boolean {
            val appDir = File(Constants.MEDIA_APP_DIRECTORY)
            val languageDir = File(Constants.MEDIA_LAYOUTS_LANGUAGE_DIRECTORY)
            val serviceDir = File(Constants.MEDIA_LAYOUTS_SERVICE_DIRECTORY)

            // Ensure the language directory exists
            if (!languageDir.exists() && !languageDir.mkdirs()) {
                val errorMsg = "Failed to create language directory: ${languageDir.absolutePath}"
                ClassFunctionsPopups.showErrorPopup(windowManager, context, TAG, errorMsg)
                return false
            }

            // Ensure the service directory exists
            if (!serviceDir.exists() && !serviceDir.mkdirs()) {
                val errorMsg = "Failed to create service directory: ${serviceDir.absolutePath}"
                ClassFunctionsPopups.showErrorPopup(windowManager, context, TAG, errorMsg)
                return false
            }

            // Ensure default files are copied (overwritten if they exist)
            return listOf(
                copyDefaultFileAlways(
                    "${languageDir.absolutePath}/${Constants.LAYOUT_LANGUAGE_DEFAULT}.json",
                    R.raw.keyboard_default,
                    context,
                    windowManager,
                    resources
                ),
                copyDefaultFileAlways(
                    "${serviceDir.absolutePath}/${Constants.LAYOUT_SERVICE_DEFAULT}.json",
                    R.raw.keyboard_service,
                    context,
                    windowManager,
                    resources
                ),
                copyDefaultFileAlways(
                    "${serviceDir.absolutePath}/${Constants.LAYOUT_CLIPBOARD_DEFAULT}.json",
                    R.raw.keyboard_clipboard_default,
                    context,
                    windowManager,
                    resources
                ),
                copyDefaultFileAlways(
                    "${appDir.absolutePath}/${Constants.REFERENCE_DEFAULT}",
                    R.raw.reference,
                    context,
                    windowManager,
                    resources
                )
            ).all { it } // Return true only if all operations succeeded
        }

        private fun copyDefaultFileAlways(
            targetPath: String,
            rawResourceId: Int,
            context: Context,
            windowManager: WindowManager,
            resources: Resources
        ): Boolean {
            return try {
                val targetFile = File(targetPath)
                resources.openRawResource(rawResourceId).use { inputStream ->
                    targetFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                //Log.i(TAG, "Copied (or overwritten) default file to: $targetPath")
                true
            } catch (e: Exception) {
                val errorMsg = "Failed to copy default file to $targetPath. Error: ${e.message}"
                ClassFunctionsPopups.showErrorPopup(windowManager, context, TAG, errorMsg)
                false
            }
        }
    }
}

object SettingsManager {
    private const val TAG = "SettingsManager"

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    @Volatile
    private var cachedSettings: KeyboardSettings? = null

    /**
     * Loads keyboard settings from user file or defaults with caching.
     *
     * Attempts to load `settings.json` from the media directory. If the file doesn't exist
     * or parsing fails, falls back to built-in defaults from `R.raw.settings_default`.
     * Result is cached for subsequent calls.
     *
     * @param context Android context for resource access.
     * @param onError Callback invoked if parsing fails (receives error message).
     * @return [KeyboardSettings] instance with user or default values (never null).
     */
    fun loadSettings(
        context: Context,
        onError: (String) -> Unit
    ): KeyboardSettings {
        cachedSettings?.let { return it }

        val userFile = File(Constants.MEDIA_SETTINGS_FILE)
        android.util.Log.d(TAG, "Looking for settings at: ${userFile.absolutePath}")
        android.util.Log.d(TAG, "File exists: ${userFile.exists()}")
        
        val settings = if (userFile.exists()) {
            try {
                val content = userFile.readText()
                android.util.Log.d(TAG, "Settings file size: ${content.length} chars")
                json.decodeFromString<KeyboardSettings>(content)
            } catch (e: Exception) {
                onError(
                    "Failed to parse ${Constants.SETTINGS_FILENAME} at ${userFile.absolutePath}: ${e.message}. " +
                        "Falling back to defaults."
                )
                loadDefaultSettings(context, onError)
            }
        } else {
            android.util.Log.d(TAG, "Settings file not found, using defaults")
            loadDefaultSettings(context, onError)
        }

        android.util.Log.d(TAG, "Loaded settings: $settings")
        cachedSettings = settings
        return settings
    }

    /**
     * Loads built-in default settings from resources.
     *
     * Falls back to hard-coded [KeyboardSettings]() constructor if resource parsing fails,
     * ensuring settings are always available (never null).
     *
     * @param context Android context for resource access.
     * @param onError Callback invoked if resource parsing fails (receives error message).
     * @return [KeyboardSettings] instance with default values (never null).
     */
    private fun loadDefaultSettings(
        context: Context,
        onError: (String) -> Unit
    ): KeyboardSettings {
        return try {
            val text = context.resources.openRawResource(R.raw.settings_default)
                .bufferedReader()
                .use { it.readText() }
            json.decodeFromString<KeyboardSettings>(text)
        } catch (e: Exception) {
            onError(
                "Failed to parse built-in default settings resource (R.raw.settings_default): ${e.message}. " +
                    "Using hardcoded defaults."
            )
            KeyboardSettings() // Hard-coded fallback; never null
        }
    }

    /**
     * Clears the cached settings, forcing a reload on the next [loadSettings] call.
     *
     * Useful when settings file has been updated externally and should be re-read.
     */
    fun reloadSettings() {
        cachedSettings = null
    }
}

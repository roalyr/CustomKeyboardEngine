package com.roalyr.customkeyboardengine

import android.content.Context
import android.content.res.Resources
import android.util.Log
import android.view.WindowManager
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
                Log.i(TAG, "Copied (or overwritten) default file to: $targetPath")
                true
            } catch (e: Exception) {
                val errorMsg = "Failed to copy default file to $targetPath. Error: ${e.message}"
                ClassFunctionsPopups.showErrorPopup(windowManager, context, TAG, errorMsg)
                false
            }
        }
    }
}

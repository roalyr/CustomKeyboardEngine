package com.roalyr.new7rowkb

import android.content.Context
import android.content.res.Resources
import android.util.Log
import android.view.WindowManager
import java.io.File


class ClassFunctionsFiles {
    companion object{
        private const val TAG = "ClassFunctionsFiles"

        fun ensureMediaDirectoriesExistAndCopyDefaults(
            windowManager: WindowManager,
            context: Context,
            resources: Resources
        ) {
            val languageDir = File(Constants.MEDIA_LAYOUTS_LANGUAGE_DIRECTORY)
            val serviceDir = File(Constants.MEDIA_LAYOUTS_SERVICE_DIRECTORY)

            if (!languageDir.exists() && !languageDir.mkdirs()) {
                val errorMsg = "Failed to create language directory: ${languageDir.absolutePath}"
                ClassFunctionsPopups.showErrorPopup(windowManager, context, TAG, errorMsg)
                return
            }

            if (!serviceDir.exists() && !serviceDir.mkdirs()) {
                val errorMsg = "Failed to create service directory: ${serviceDir.absolutePath}"
                ClassFunctionsPopups.showErrorPopup(windowManager, context, TAG, errorMsg)
                return
            }

            copyDefaultLayoutIfMissing(
                "${languageDir.absolutePath}/${Constants.LAYOUT_LANGUAGE_DEFAULT}.json",
                R.raw.keyboard_default,
                context,
                resources
            )

            copyDefaultLayoutIfMissing(
                "${serviceDir.absolutePath}/${Constants.LAYOUT_SERVICE_DEFAULT}.json",
                R.raw.keyboard_service,
                context,
                resources
            )
        }

        private fun copyDefaultLayoutIfMissing(
            targetPath: String,
            rawResourceId: Int,
            context: Context,
            resources: Resources
        ) {
            try {
                val targetFile = File(targetPath)
                if (!targetFile.exists()) {
                    val inputStream = resources.openRawResource(rawResourceId)
                    targetFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    Log.i(TAG, "Copied default layout to: $targetPath")
                } else {
                    Log.i(TAG, "Default layout already exists: $targetPath")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to copy default layout to $targetPath. Error: ${e.message}", e)
            }
        }


    }
}
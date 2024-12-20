package com.roalyr.new7rowkb

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.util.Log
import android.view.WindowManager
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class ClassFunctionsFiles {
    companion object{
        private const val TAG = "ClassFunctionsFiles"

        private fun ensureDirectoryExists(directoryPath: String): Boolean {
            val directory = File(directoryPath)
            if (!directory.exists()) {
                return directory.mkdirs()
            }
            return true
        }

        fun forceDirectoryRescan(context: Context, directory: File) {
            if (directory.exists() && directory.isDirectory) {
                directory.listFiles()?.forEach { file ->
                    val scanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).apply {
                        data = Uri.fromFile(file)
                    }
                    context.sendBroadcast(scanIntent)
                }
            }
            Log.i(TAG, "Requested MediaScanner scan for directory: ${directory.absolutePath}")
        }


        private fun copyKeyboardLayoutIfMissing(windowManager: WindowManager, context: Context, resources: Resources, filePath: String, rawResourceId: Int) {
            val layoutFile = File(filePath)

            if (!layoutFile.exists()) {
                try {
                    val inputStream = resources.openRawResource(rawResourceId)
                    val outputStream = FileOutputStream(layoutFile)
                    val buffer = ByteArray(1024)
                    var length: Int
                    while (inputStream.read(buffer).also { length = it } > 0) {
                        outputStream.write(buffer, 0, length)
                    }
                    outputStream.close()
                    inputStream.close()
                    Log.i(TAG, "Successfully copied ${layoutFile.name} to ${layoutFile.parent}")
                } catch (e: IOException) {
                    val errorMsg = "Failed to copy ${layoutFile.name}, ${e.message}"
                    ClassFunctionsPopups.showErrorPopup(windowManager, context, TAG, errorMsg)
                }
            } else {
                Log.i(TAG, "${layoutFile.name} already exists, skipping copy")
            }
        }

        fun copyDefaultKeyboardLayouts(windowManager: WindowManager, context: Context, resources: Resources) {
            // Ensure base and subdirectories exist
            if (!ensureDirectoryExists(Constants.LAYOUTS_LANGUAGE_DIRECTORY)) {
                val errorMsg = "Failed to create layouts-language directory"
                ClassFunctionsPopups.showErrorPopup(windowManager, context, TAG, errorMsg)
                return
            }
            if (!ensureDirectoryExists(Constants.LAYOUTS_SERVICE_DIRECTORY)) {
                val errorMsg = "Failed to create layouts-service directory"
                ClassFunctionsPopups.showErrorPopup(windowManager, context, TAG, errorMsg)
                return
            }

            // Copy files to the respective directories
            copyKeyboardLayoutIfMissing(windowManager, context, resources,
                "${Constants.LAYOUTS_LANGUAGE_DIRECTORY}/${Constants.LAYOUT_LANGUAGE_DEFAULT}.json",
                R.raw.keyboard_default
            )
            copyKeyboardLayoutIfMissing(windowManager, context, resources,
                "${Constants.LAYOUTS_SERVICE_DIRECTORY}/${Constants.LAYOUT_SERVICE_DEFAULT}.json",
                R.raw.keyboard_service
            )
        }
    }
}
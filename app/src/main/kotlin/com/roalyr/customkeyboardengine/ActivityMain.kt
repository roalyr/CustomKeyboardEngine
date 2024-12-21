package com.roalyr.customkeyboardengine


import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.roalyr.customkeyboardengine.ui.theme.New7rowKBTheme
import java.io.File

class ActivityMain : ComponentActivity() {
    // Register the overlay permission launcher
    private val overlayPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (Settings.canDrawOverlays(this)) {
                Log.i("ActivityMain", "Overlay permission granted")
            } else {
                Toast.makeText(this, "Overlay permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    override fun onCreate(savedInstanceState: Bundle?) {
        // Force ask for permissions
        sendBroadcast(Intent(Constants.Actions.CHECK_STORAGE_PERMISSIONS))
        sendBroadcast(Intent(Constants.Actions.CHECK_OVERLAY_PERMISSION))

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            New7rowKBTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(onClick = { requestPermissions() }) {
                            Text("Grant permissions")
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(onClick = { openInputMethodSettings() }) {
                            Text("Change Input Method")
                        }
                        Spacer(modifier = Modifier.height(32.dp))

                        Button(onClick = { openKeyboardLayoutsDirectory() }) {
                            Text("Open Keyboard Layouts Directory")
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(onClick = { openUrl("https://github.com/roalyr/CustomKeyboardEngine") }) {
                            Text("Visit GitHub Page")
                        }
                    }
                }
            }
        }
        // Ensure media directories exist
        ensureMediaDirectories()

        // Request permissions
        //requestPermissions()

    }

    private fun requestPermissions() {
        // Check and request storage permissions dynamically
        if (ActivityPermissionRequest.checkAndRequestStoragePermissions(this)) {
            Toast.makeText(this, "Storage permissions already granted.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Requesting storage permissions.", Toast.LENGTH_SHORT).show()
        }

        // Check and request overlay permission
        if (ActivityPermissionRequest.checkAndRequestOverlayPermission(this)) {
            Toast.makeText(this, "Overlay permission already granted.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Requesting overlay permission.", Toast.LENGTH_SHORT).show()
        }
    }


    private fun ensureMediaDirectories() {
        try {
            ClassFunctionsFiles.ensureMediaDirectoriesExistAndCopyDefaults(
                windowManager = getSystemService(WINDOW_SERVICE) as android.view.WindowManager,
                context = this,
                resources = resources
            )
            Log.i("ActivityMain", "Media directories ensured successfully.")
        } catch (e: Exception) {
            Log.e("ActivityMain", "Error ensuring media directories: ${e.message}")
            Toast.makeText(this, "Error initializing media directories.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openKeyboardLayoutsDirectory() {
        val appDir = File(Constants.MEDIA_LAYOUTS_LANGUAGE_DIRECTORY)

        // Ensure the directory exists
        if (!appDir.exists() && !appDir.mkdirs()) {
            Toast.makeText(this, "Failed to create directory", Toast.LENGTH_SHORT).show()
            Log.e("ActivityMain", "Failed to create directory: ${appDir.absolutePath}")
            return
        }

        try {
            // Use FileProvider to create a URI
            val uri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.provider",
                appDir
            )

            // Create an intent with ACTION_VIEW and use */* MIME
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "*/*") // Fallback for directory opening
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // Verify if an app can handle the intent
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(this, "No file manager found to open the directory", Toast.LENGTH_SHORT).show()
                Log.e("ActivityMain", "No app found to handle ACTION_VIEW for this directory")
            }
        } catch (e: Exception) {
            Log.e("ActivityMain", "Failed to open directory: ${e.message}")
            Toast.makeText(this, "Failed to open directory.", Toast.LENGTH_SHORT).show()
        }
    }



    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }

    private fun openInputMethodSettings() {
        val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }
}
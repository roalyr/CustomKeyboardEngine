package com.roalyr.new7rowkb


import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import com.roalyr.new7rowkb.ui.theme.New7rowKBTheme
import java.io.File

class ActivityMain : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Force ask for permissions
        sendBroadcast(Intent(Constants.ACTION_CHECK_STORAGE_PERMISSIONS))
        sendBroadcast(Intent(Constants.ACTION_CHECK_OVERLAY_PERMISSION))

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
                        Button(onClick = { openInputMethodSettings() }) {
                            Text("Change Input Method")
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(onClick = { openKeyboardLayoutsDirectory() }) {
                            Text("TODO:Open Keyboard Layouts Directory")
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(onClick = { openUrl("https://github.com/roalyr/New7rowKB") }) {
                            Text("Visit GitHub Project")
                        }
                    }
                }
            }
        }
    }

    private fun openKeyboardLayoutsDirectory() {
        val appDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            "New7rowKB"
        )

        // Ensure the directory exists
        if (!appDir.exists()) {
            if (!appDir.mkdirs()) {
                Toast.makeText(this, "Failed to create directory", Toast.LENGTH_SHORT).show()
                Log.e("MainActivity", "Failed to create New7rowKB directory")
                return
            }
        }

        try {
            // Use FileProvider to generate content URI
            val uri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.provider",
                appDir
            )
            Log.d("MainActivity", "Generated URI: $uri")

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "*/*") // Use */* to let file manager decide
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // Verify if there's an app to handle the intent
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(this, "No file manager found to open the directory", Toast.LENGTH_SHORT).show()
                Log.e("MainActivity", "No file manager found to handle ACTION_VIEW")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to open directory: ${e.message}")
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
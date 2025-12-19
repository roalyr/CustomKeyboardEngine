package com.roalyr.customkeyboardengine

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roalyr.customkeyboardengine.ui.theme.CustomKeyboardEngineTheme
import java.io.File

class ActivityMain : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CustomKeyboardEngineTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()) // Enable scrolling
                            .padding(horizontal = 24.dp)
                            .padding(WindowInsets.systemBars.asPaddingValues()) // Respect notification bar
                            .imePadding()
                        , // Adjust for keyboard
                        verticalArrangement = Arrangement.spacedBy(8.dp), // Uniform spacing
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Spacer for Title
                        Spacer(modifier = Modifier.height(16.dp)) // Add initial padding for safe area



                        // Buttons
                        Button(
                            onClick = { requestStoragePermission() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text("1. Grant Storage Permission")
                        }

                        Button(
                            onClick = { requestOverlayPermission() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text("2. Grant Overlay Permission")
                        }

                        Button(
                            onClick = { openInputMethodSettings() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text("3. Change Input Method")
                        }

                        Button(
                            onClick = { copyDefaults() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text("4. Copy (rewrite) default layouts and reference manual to working folder")
                        }

                        Button(
                            onClick = { copyDefaultSettings() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text("5. Copy (rewrite) default settings.json to working folder")
                        }

                        // Path Reference
                        Text(
                            text = "📂 Working Directory Path:",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Start,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                        )
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                .padding(8.dp)
                                .fillMaxWidth()
                        ) {
                            Text(
                                text = "/storage/emulated/0/Android/media/com.roalyr.customkeyboardengine/",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Hint Messages
                        Text(
                            text = "ℹ️ HINT: Look for `.json` files in the directory above. " +
                                    "You can edit them using any external text editor. Refer to `${Constants.REFERENCE_DEFAULT}` in the folder for more information.",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = "ℹ️ HINT: If the directory is not created automatically, " +
                                    "create it manually and restart the app.",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Warning Message
                        Text(
                            text = "⚠️ IMPORTANT: Backup files in the directory above to prevent data loss before uninstalling the app.",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Text Input Field
                        val text = remember { mutableStateOf("") }
                        androidx.compose.material3.TextField(
                            value = text.value,
                            onValueChange = { text.value = it },
                            placeholder = { Text("Type here to test keyboard...") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // GitHub Link
                        Button(
                            onClick = { openUrl("https://github.com/roalyr/CustomKeyboardEngine") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text("Visit GitHub Page")
                        }

                        Spacer(modifier = Modifier.height(16.dp)) // Add space at the bottom
                    }
                }
            }
        }
    }

    private fun requestOverlayPermission() {
        ActivityPermissionRequest.startPermissionRequest(this, Constants.PermissionTypes.OVERLAY)
    }

    private fun requestStoragePermission() {
        ActivityPermissionRequest.startPermissionRequest(this, Constants.PermissionTypes.STORAGE)
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

    private fun copyDefaults() {
        if (ClassFunctionsFiles.ensureMediaDirectoriesExistAndCopyDefaults(windowManager, this, resources)){
            Toast.makeText(this, "Default layouts and reference copied", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Copies default settings.json from resources to the app working directory.
     *
     * Creates the media directory if it doesn't exist, then copies `R.raw.settings_default`
     * to `Android/media/com.roalyr.customkeyboardengine/settings.json`. If the file already exists,
     * it is overwritten.
     *
     * Shows toast notifications for success or failure.
     */
    private fun copyDefaultSettings() {
        val appDir = File(Constants.MEDIA_APP_DIRECTORY)
        if (!appDir.exists() && !appDir.mkdirs()) {
            Toast.makeText(this, "Failed to create working folder", Toast.LENGTH_SHORT).show()
            return
        }

        val targetFile = File(Constants.MEDIA_SETTINGS_FILE)
        try {
            resources.openRawResource(R.raw.settings_default).use { inputStream ->
                targetFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            Toast.makeText(this, "Default settings.json copied", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to copy settings.json: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

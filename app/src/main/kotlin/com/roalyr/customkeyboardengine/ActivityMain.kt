package com.roalyr.customkeyboardengine

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roalyr.customkeyboardengine.ui.theme.CustomKeyboardEngineTheme

class ActivityMain : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
                            .padding(horizontal = 24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // App Title
                        Text(
                            text = "CustomKeyboardEngine",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 32.dp)
                        )

                        // Buttons
                        Button(
                            onClick = { requestStoragePermission() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text("1. Grant Storage Permission")
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { requestOverlayPermission() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text("2. Grant Overlay Permission")
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { openInputMethodSettings() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text("3. Change Input Method")
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Warning Message
                        Text(
                            text = "⚠️ IMPORTANT: Backup files in /Android/media/com.roalyr.customkeyboardengine " +
                                    "to prevent data loss before uninstalling the app.",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // GitHub Link
                        Button(
                            onClick = { openUrl("https://github.com/roalyr/CustomKeyboardEngine") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text("Visit GitHub Page")
                        }
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
}

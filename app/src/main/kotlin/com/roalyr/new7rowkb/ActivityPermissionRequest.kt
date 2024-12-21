package com.roalyr.new7rowkb

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class ActivityPermissionRequest : AppCompatActivity() {

    companion object {
        private const val TAG = "ActivityPermissionRequest"

        fun checkAndRequestStoragePermissions(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                if (ContextCompat.checkSelfPermission(
                        context, Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        context as Activity,
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        Constants.RequestCodes.STORAGE_PERMISSIONS
                    )
                    false // Permissions not yet granted
                } else {
                    true // Permissions already granted
                }
            } else {
                Log.i(TAG, "No explicit storage permissions required for Android 10+")
                true // No permissions required for Android 10+
            }
        }

        fun checkAndRequestOverlayPermission(context: Context): Boolean {
            return if (!Settings.canDrawOverlays(context)) {
                val intent = Intent(context, ActivityPermissionRequest::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra(Constants.PermissionTypes.EXTRA_TYPE, Constants.PermissionTypes.OVERLAY)
                }
                ContextCompat.startActivity(context, intent, null)
                false // Overlay permission not yet granted
            } else {
                true // Overlay permission already granted
            }
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val permissionType = intent.getStringExtra(Constants.PermissionTypes.EXTRA_TYPE)
        when (permissionType) {
            Constants.PermissionTypes.STORAGE -> requestStoragePermissions()
            Constants.PermissionTypes.OVERLAY -> requestOverlayPermission()
        }
    }

    private fun requestStoragePermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                Constants.RequestCodes.STORAGE_PERMISSIONS
            )
        }
    }

    // Register the Activity Result callback for overlay permission
    private val overlayPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
            if (Settings.canDrawOverlays(this)) {
                Log.i(TAG, "Overlay permission granted")
            } else {
                Log.e(TAG, "Overlay permission denied")
            }
            finish() // Finish the activity after handling the result
        }

    private fun requestOverlayPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
            data = Uri.parse("package:$packageName")
        }
        overlayPermissionLauncher.launch(intent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Constants.RequestCodes.STORAGE_PERMISSIONS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "Storage permission granted")
            } else {
                Log.e(TAG, "Storage permission denied")
            }
        }
    }
}

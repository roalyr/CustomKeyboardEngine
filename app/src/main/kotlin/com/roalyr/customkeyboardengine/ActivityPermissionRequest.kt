package com.roalyr.customkeyboardengine

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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class ActivityPermissionRequest : AppCompatActivity() {

    companion object {
        private const val TAG = "ActivityPermissionRequest"

        // Launch specific permission request
        fun startPermissionRequest(context: Context, permissionType: String) {
            val intent = Intent(context, ActivityPermissionRequest::class.java).apply {
                putExtra(Constants.PermissionTypes.EXTRA_TYPE, permissionType)
            }
            context.startActivity(intent)
        }

        fun checkAndRequestOverlayPermission(context: Context): Boolean {
            return if (!Settings.canDrawOverlays(context)) {
                val intent = Intent(context, ActivityPermissionRequest::class.java).apply {
                    putExtra(Constants.PermissionTypes.EXTRA_TYPE, Constants.PermissionTypes.OVERLAY)
                }
                context.startActivity(intent)
                false
            } else {
                true
            }
        }

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
                    false
                } else {
                    true
                }
            } else {
                Log.i(TAG, "No explicit storage permissions required for Android 10+")
                true
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val permissionType = intent.getStringExtra(Constants.PermissionTypes.EXTRA_TYPE)
        when (permissionType) {
            Constants.PermissionTypes.OVERLAY -> requestOverlayPermission()
            Constants.PermissionTypes.STORAGE -> requestStoragePermissions()
            else -> {
                Log.e(TAG, "Unknown permission type: $permissionType")
                Toast.makeText(this, "Unknown permission type.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun requestOverlayPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
            data = Uri.parse("package:$packageName")
        }
        startActivity(intent)
        finish()
    }

    private fun requestStoragePermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                Constants.RequestCodes.STORAGE_PERMISSIONS
            )
        } else {
            Log.i(TAG, "Storage permission not required for Android 10+")
            Toast.makeText(this, "Storage permission not required for Android 10+.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}

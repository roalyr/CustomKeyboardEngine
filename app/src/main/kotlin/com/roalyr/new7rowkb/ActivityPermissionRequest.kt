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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity

class ActivityPermissionRequest : AppCompatActivity() {

    companion object{
        ////////////////////////////////////////////
        // Ask for permissions
        fun checkAndRequestOverlayPermission(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                // Start permission request activity
                val intent = Intent(context, ActivityPermissionRequest::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra(Constants.EXTRA_PERMISSION_TYPE, Constants.PERMISSION_TYPE_OVERLAY)
                }
                startActivity(context, intent, null)
            }
        }

        fun checkAndRequestStoragePermissions(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+ does not require WRITE_EXTERNAL_STORAGE permission
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                    startPermissionRequestActivity(context, Constants.PERMISSION_TYPE_STORAGE)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ with scoped storage
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    startPermissionRequestActivity(context, Constants.PERMISSION_TYPE_STORAGE)
                }
            } else {
                // For Android 9 and below
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    startPermissionRequestActivity(context, Constants.PERMISSION_TYPE_STORAGE)
                }
            }
        }

        private fun startPermissionRequestActivity(context: Context, permissionType: String) {
            val intent = Intent(context, ActivityPermissionRequest::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra(Constants.EXTRA_PERMISSION_TYPE, permissionType)
            }
            startActivity(context, intent, null)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val permissionType = intent.getStringExtra(Constants.EXTRA_PERMISSION_TYPE) ?: Constants.PERMISSION_TYPE_STORAGE

        when (permissionType) {
            Constants.PERMISSION_TYPE_STORAGE -> {
                // Request storage permissions
                ActivityCompat.requestPermissions(this,
                    arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    Constants.REQUEST_CODE_STORAGE_PERMISSIONS)
            }
            Constants.PERMISSION_TYPE_OVERLAY -> {
                // Request overlay permission
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                startActivityForResult(intent, Constants.REQUEST_CODE_OVERLAY_PERMISSION)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Constants.REQUEST_CODE_STORAGE_PERMISSIONS) {
            val result = if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Activity.RESULT_OK
            } else {
                Activity.RESULT_CANCELED
            }
            setResult(result)
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Constants.REQUEST_CODE_OVERLAY_PERMISSION) {
            // Overlay permission result is handled automatically by the system
            // You can check if the permission is granted using Settings.canDrawOverlays(this)
            finish() // Finish the activity after handling the result
        }
    }
}
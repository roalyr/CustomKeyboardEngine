package com.roalyr.new7rowkb

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class PermissionRequestActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val permissionType = intent.getIntExtra(Constants.EXTRA_PERMISSION_TYPE, Constants.PERMISSION_TYPE_STORAGE)

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
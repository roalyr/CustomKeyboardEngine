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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class ActivityPermissionRequest : AppCompatActivity() {

    companion object {
        private const val TAG = "ActivityPermissionRequest"
        private const val SAF_PREFS_NAME = "SAF_PREFS"
        private const val SAF_URI_KEY = "SAF_URI_KEY"

        fun checkAndRequestOverlayPermission(context: Context) {
            if (!Settings.canDrawOverlays(context)) {
                val intent = Intent(context, ActivityPermissionRequest::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra(Constants.PermissionTypes.EXTRA_TYPE, Constants.PermissionTypes.OVERLAY)
                }
                ContextCompat.startActivity(context, intent, null)
            }
        }

        fun checkAndRequestStoragePermissions(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+ (Media-specific access)
                if (ContextCompat.checkSelfPermission(
                        context, Manifest.permission.READ_MEDIA_IMAGES
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        context as Activity,
                        arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                        Constants.RequestCodes.STORAGE_PERMISSIONS
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10-12 (Scoped storage)
                if (ContextCompat.checkSelfPermission(
                        context, Manifest.permission.READ_EXTERNAL_STORAGE
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        context as Activity,
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                        Constants.RequestCodes.STORAGE_PERMISSIONS
                    )
                }
            } else {
                // Android 9 and below (Full external storage access)
                if (ContextCompat.checkSelfPermission(
                        context, Manifest.permission.READ_EXTERNAL_STORAGE
                    ) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(
                        context, Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        context as Activity,
                        arrayOf(
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ),
                        Constants.RequestCodes.STORAGE_PERMISSIONS
                    )
                }
            }
        }

        fun requestDocumentTreeAccess(context: Context) {
            val intent = Intent(context, ActivityPermissionRequest::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra(Constants.PermissionTypes.EXTRA_TYPE, Constants.PermissionTypes.STORAGE)
                putExtra("useSAF", true)
            }
            ContextCompat.startActivity(context, intent, null)
        }

        fun getPersistedSafUri(context: Context): Uri? {
            val prefs = context.getSharedPreferences(SAF_PREFS_NAME, Context.MODE_PRIVATE)
            val uriString = prefs.getString(SAF_URI_KEY, null)
            return uriString?.let { Uri.parse(it) }
        }


    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val permissionType = intent.getStringExtra(Constants.PermissionTypes.EXTRA_TYPE)

        when (permissionType) {
            Constants.PermissionTypes.STORAGE -> {
                val useSAF = intent.getBooleanExtra("useSAF", false)
                if (useSAF) {
                    requestSafAccess()
                } else {
                    requestStoragePermissions()
                }
            }
            Constants.PermissionTypes.OVERLAY -> requestOverlayPermission()
        }
    }

    private fun requestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                Constants.RequestCodes.STORAGE_PERMISSIONS
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                Constants.RequestCodes.STORAGE_PERMISSIONS
            )
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                Constants.RequestCodes.STORAGE_PERMISSIONS
            )
        }
    }

    private fun requestSafAccess() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
        startActivityForResult(intent, Constants.RequestCodes.STORAGE_PERMISSIONS)
    }

    private fun requestOverlayPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
            data = Uri.parse("package:$packageName")
        }
        startActivityForResult(intent, Constants.RequestCodes.OVERLAY_PERMISSION)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Constants.RequestCodes.STORAGE_PERMISSIONS && resultCode == RESULT_OK) {
            val uri = data?.data
            if (uri != null) {
                persistSafUri(uri)
                Log.i(TAG, "Granted SAF access to URI: $uri")
            } else {
                Log.e(TAG, "No URI returned from SAF picker")
            }
        } else if (requestCode == Constants.RequestCodes.OVERLAY_PERMISSION) {
            Log.i(TAG, "Overlay permission result handled")
        }
        finish()
    }

    private fun persistSafUri(uri: Uri) {
        contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        val prefs = getSharedPreferences(SAF_PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(SAF_URI_KEY, uri.toString()).apply()
    }
}

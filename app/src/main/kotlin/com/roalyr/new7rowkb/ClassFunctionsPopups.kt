package com.roalyr.new7rowkb

import android.content.Context
import android.graphics.PixelFormat
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView

class ClassFunctionsPopups {
    companion object {
        private var errorPopupView: View? = null

        fun showErrorPopup(windowManager: WindowManager, context: Context, tag: String, message: String) {
            // Remove existing popup to prevent duplicates
            errorPopupView?.let {
                windowManager.removeView(it)
                errorPopupView = null
            }

            // Inflate the custom error popup layout
            val inflater = LayoutInflater.from(context)
            errorPopupView = inflater.inflate(R.layout.error_notification_view, null).apply {
                findViewById<TextView>(R.id.error_message).text = message

                // Add click listener to a close button (e.g., ImageView with ID: error_close)
                findViewById<View>(R.id.error_close).setOnClickListener {
                    dismissErrorPopup(windowManager)
                }

                // Optional: Add a click listener anywhere on the popup to dismiss
                setOnClickListener {
                    dismissErrorPopup(windowManager)
                }
            }

            // Define WindowManager.LayoutParams for positioning
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, // Full width
                WindowManager.LayoutParams.WRAP_CONTENT, // Auto height
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, // Overlay permission
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER or Gravity.CENTER_HORIZONTAL // Position at the center
                y = 50 // Optional: Add some margin from the top edge
            }

            try {
                // Add the error popup to the WindowManager
                windowManager.addView(errorPopupView, params)
                // Pass the error to the log as well.
                Log.e(tag, message)
            } catch (e: Exception) {
                Log.e(tag, "Error adding popup view: ${e.message}")
            }
        }

        // Function to dismiss the error popup manually
        private fun dismissErrorPopup(windowManager: WindowManager) {
            errorPopupView?.let {
                windowManager.removeView(it)
                errorPopupView = null
            }
        }
    }
}
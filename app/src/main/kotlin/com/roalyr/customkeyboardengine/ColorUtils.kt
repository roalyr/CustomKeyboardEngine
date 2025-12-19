package com.roalyr.customkeyboardengine

import android.graphics.Color

/**
 * Utility functions for color parsing and manipulation.
 */
object ColorUtils {
    /**
     * Parses a hex color string into an ARGB integer.
     * Supports:
     * - #RRGGBB (opaque)
     * - #AARRGGBB (with alpha)
     * - RRGGBB (opaque, no hash)
     * - AARRGGBB (with alpha, no hash)
     *
     * @param hex The hex string to parse.
     * @return The ARGB integer, or null if parsing fails.
     */
    fun parseHexColor(hex: String): Int? {
        if (hex.isBlank()) return null
        
        val cleanedHex = hex.trim().removePrefix("#")
        
        return try {
            when (cleanedHex.length) {
                6 -> {
                    // RRGGBB -> AARRGGBB (FF for alpha)
                    Color.parseColor("#FF$cleanedHex")
                }
                8 -> {
                    // AARRGGBB
                    Color.parseColor("#$cleanedHex")
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
}

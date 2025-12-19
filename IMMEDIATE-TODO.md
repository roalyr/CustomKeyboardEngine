# IMMEDIATE-TODO.md
**Sprint:** Touch Correction & Theme Settings (from TODO.md Priority 1-2)
**Date:** 2025-12-19

---

## 1. CONTEXT

Implement the next priority features from TODO.md:
1. **Touch correction offsets** (Priority 1) - Add horizontal/vertical touch offset settings to `KeyboardSettings`
2. **Theme settings** (Priority 2) - Add JSON settings for theme (dark/light), accent color, and key rounding

These features extend the existing `KeyboardSettings` infrastructure to provide user-customizable touch and visual behavior.

---

## 2. ARCHITECTURE CHECK

| Component | Location |
|-----------|----------|
| Settings Data Class | `app/src/main/kotlin/com/roalyr/customkeyboardengine/CustomKeyboard.kt` (`KeyboardSettings`) |
| Constants | `app/src/main/kotlin/com/roalyr/customkeyboardengine/Constants.kt` |
| Default Settings JSON | `app/src/main/res/raw/settings_default.json` |
| Documentation | `app/src/main/res/raw/reference.md` |
| Touch Handling | `app/src/main/kotlin/com/roalyr/customkeyboardengine/CustomKeyboardView.kt` (`onTouchEvent`) |
| Rendering Logic | `app/src/main/kotlin/com/roalyr/customkeyboardengine/CustomKeyboardView.kt` (`onDraw`) |
| Settings Loader | `app/src/main/kotlin/com/roalyr/customkeyboardengine/ClassFunctionsFiles.kt` (`SettingsManager`) |

---

## 3. ATOMIC TASKS

### Task 1: Add Touch Correction Offset Settings (PRIORITY 1)

- [x] **1.1 Add touch offset constants to Constants.kt**
  - **TARGET FILE:** `app/src/main/kotlin/com/roalyr/customkeyboardengine/Constants.kt`
  - **DEPENDENCIES:** None
  - **PSEUDO-CODE:**
    ```kotlin
    // Touch correction defaults (in pixels, applied after scaling)
    const val DEFAULT_TOUCH_CORRECTION_X = 0f
    const val DEFAULT_TOUCH_CORRECTION_Y = 0f
    ```
  - **SUCCESS CRITERIA:** Constants compile without errors

- [x] **1.2 Add touch offset properties to KeyboardSettings**
  - **TARGET FILE:** `app/src/main/kotlin/com/roalyr/customkeyboardengine/CustomKeyboard.kt`
  - **DEPENDENCIES:** Task 1.1
  - **PSEUDO-CODE:**
    ```kotlin
    @Serializable
    data class KeyboardSettings(
        // ... existing properties ...
        
        // Touch correction (pixel offset applied to touch coordinates)
        val touchCorrectionX: Float = Constants.DEFAULT_TOUCH_CORRECTION_X,
        val touchCorrectionY: Float = Constants.DEFAULT_TOUCH_CORRECTION_Y
    )
    ```
  - **SUCCESS CRITERIA:** Data class compiles; serialization handles missing fields gracefully

- [x] **1.3 Update settings_default.json with touch offsets**
  - **TARGET FILE:** `app/src/main/res/raw/settings_default.json`
  - **DEPENDENCIES:** Task 1.2
  - **PSEUDO-CODE:**
    ```json
    {
      "touchCorrectionX": 0.0,
      "touchCorrectionY": 0.0
    }
    ```
  - **SUCCESS CRITERIA:** JSON parses correctly into `KeyboardSettings`

- [x] **1.4 Apply touch correction in CustomKeyboardView.onTouchEvent**
  - **TARGET FILE:** `app/src/main/kotlin/com/roalyr/customkeyboardengine/CustomKeyboardView.kt`
  - **DEPENDENCIES:** Task 1.2
  - **PSEUDO-CODE:**
    ```kotlin
    // In handleTouchDown/handleTouchUp:
    val correctedX = scaledX + (settings?.touchCorrectionX ?: 0f).toInt()
    val correctedY = scaledY + (settings?.touchCorrectionY ?: 0f).toInt()
    ```
  - **CHANGES REQUIRED:**
    - Add `settings: KeyboardSettings?` property to `CustomKeyboardView`
    - Add `setSettings(settings: KeyboardSettings)` method
    - Call from `CustomKeyboardService` when settings are loaded
    - Apply correction in `ACTION_DOWN` and `ACTION_POINTER_DOWN` handlers
  - **SUCCESS CRITERIA:** Touch events are offset by configured values; default (0,0) has no effect

- [x] **1.5 Update reference.md documentation**
  - **TARGET FILE:** `app/src/main/res/raw/reference.md`
  - **DEPENDENCIES:** Tasks 1.1-1.4
  - **CONTENT TO ADD (Section 4. KeyboardSettings table):**
    ```markdown
    | `touchCorrectionX`    | Float | 0.0  | Horizontal touch offset in logical pixels (positive = right) |
    | `touchCorrectionY`    | Float | 0.0  | Vertical touch offset in logical pixels (positive = down)    |
    ```
  - **SUCCESS CRITERIA:** Documentation matches implementation

---

### Task 2: Add Theme Override Settings (PRIORITY 2)

- [x] **2.1 Add theme constants to Constants.kt**
  - **TARGET FILE:** `app/src/main/kotlin/com/roalyr/customkeyboardengine/Constants.kt`
  - **DEPENDENCIES:** None
  - **PSEUDO-CODE:**
    ```kotlin
    // Theme override constants
    const val THEME_MODE_SYSTEM = 0  // Follow system dark/light
    const val THEME_MODE_DARK = 1    // Force dark theme
    const val THEME_MODE_LIGHT = 2   // Force light theme
    const val DEFAULT_THEME_MODE = THEME_MODE_SYSTEM
    const val DEFAULT_CUSTOM_ACCENT_COLOR = 0  // 0 = use system accent
    ```
  - **SUCCESS CRITERIA:** Constants compile without errors

- [x] **2.2 Add theme properties to KeyboardSettings**
  - **TARGET FILE:** `app/src/main/kotlin/com/roalyr/customkeyboardengine/CustomKeyboard.kt`
  - **DEPENDENCIES:** Task 2.1
  - **PSEUDO-CODE:**
    ```kotlin
    @Serializable
    data class KeyboardSettings(
        // ... existing properties ...
        
        // Theme overrides
        val themeMode: Int = Constants.DEFAULT_THEME_MODE,        // 0=system, 1=dark, 2=light
        val customAccentColor: Int = Constants.DEFAULT_CUSTOM_ACCENT_COLOR  // 0=system, else ARGB int
    )
    ```
  - **SUCCESS CRITERIA:** Data class compiles; defaults to system behavior

- [x] **2.3 Update settings_default.json with theme settings**
  - **TARGET FILE:** `app/src/main/res/raw/settings_default.json`
  - **DEPENDENCIES:** Task 2.2
  - **PSEUDO-CODE:**
    ```json
    {
      "themeMode": 0,
      "customAccentColor": 0
    }
    ```
  - **SUCCESS CRITERIA:** JSON parses correctly

- [x] **2.4 Apply theme overrides in CustomKeyboardView.onDraw**
  - **TARGET FILE:** `app/src/main/kotlin/com/roalyr/customkeyboardengine/CustomKeyboardView.kt`
  - **DEPENDENCIES:** Task 2.2, Task 1.4 (settings property)
  - **PSEUDO-CODE:**
    ```kotlin
    // Replace current theme detection (~line 395):
    val isDarkTheme = when (settings?.themeMode ?: Constants.DEFAULT_THEME_MODE) {
        Constants.THEME_MODE_DARK -> true
        Constants.THEME_MODE_LIGHT -> false
        else -> { /* existing system detection */ }
    }
    
    // Replace accent color resolution (~line 391):
    val accentColor = if ((settings?.customAccentColor ?: 0) != 0) {
        settings!!.customAccentColor
    } else {
        context.resolveThemeColor(android.R.attr.colorAccent, Constants.DEFAULT_ACCENT_COLOR)
    }
    ```
  - **SUCCESS CRITERIA:** Theme and accent color can be overridden via settings.json

- [x] **2.5 Update reference.md documentation**
  - **TARGET FILE:** `app/src/main/res/raw/reference.md`
  - **DEPENDENCIES:** Tasks 2.1-2.4
  - **CONTENT TO ADD (Section 4. KeyboardSettings table):**
    ```markdown
    | `themeMode`           | Int   | 0    | Theme override: 0=system, 1=force dark, 2=force light        |
    | `customAccentColor`   | Int   | 0    | Custom accent color as ARGB int (0=use system accent)        |
    ```
  - **SUCCESS CRITERIA:** Documentation matches implementation

- [x] **2.6 Add color example table to reference.md**
  - **TARGET FILE:** `app/src/main/res/raw/reference.md`
  - **DEPENDENCIES:** Task 2.5
  - **CONTENT TO ADD:** A table with common colors (Soft Purple, Crimson, etc.) and their ARGB decimal values.
  - **SUCCESS CRITERIA:** Users have a reference for `customAccentColor` values.

---

### Task 3: Wire Settings to CustomKeyboardView (INFRASTRUCTURE)

- [x] **3.1 Add settings property and setter to CustomKeyboardView**
  - **TARGET FILE:** `app/src/main/kotlin/com/roalyr/customkeyboardengine/CustomKeyboardView.kt`
  - **DEPENDENCIES:** None (but required by Tasks 1.4 and 2.4)
  - **PSEUDO-CODE:**
    ```kotlin
    class CustomKeyboardView(...) : View(...) {
        // Add near other properties (~line 45)
        private var settings: KeyboardSettings? = null
        
        fun setSettings(settings: KeyboardSettings) {
            this.settings = settings
            invalidate() // Redraw with new settings
        }
    }
    ```
  - **SUCCESS CRITERIA:** Settings can be passed to view; view redraws on settings change

- [x] **3.2 Pass settings from CustomKeyboardService to views**
  - **TARGET FILE:** `app/src/main/kotlin/com/roalyr/customkeyboardengine/CustomKeyboardService.kt`
  - **DEPENDENCIES:** Task 3.1
  - **PSEUDO-CODE:**
    ```kotlin
    // In createInputView() and createFloatingKeyboard():
    keyboardView?.setSettings(settings)
    floatingKeyboardView?.setSettings(settings)
    serviceKeyboardView?.setSettings(settings)
    ```
  - **SUCCESS CRITERIA:** All keyboard views receive settings on creation

---

## 4. CONSTRAINTS

1. **Backwards Compatibility:** Missing JSON properties must fall back to defaults (kotlinx.serialization `coerceInputValues`)
2. **Null Safety:** Use `settings?.property ?: default` pattern consistently
3. **No Breaking Changes:** Existing settings.json files without new fields must still work
4. **Documentation First:** Update reference.md before considering task complete
5. **Incremental Testing:** Verify build after each task with `./gradlew assembleDebug`
6. **Touch Correction Units:** Use logical pixels (pre-scaling) for consistency with other settings
7. **Color Format:** Use Hex string format for `customAccentColor` (e.g., `"#6A5ACD"` or `"#FF6A5ACD"`)

---

## 5. VERIFICATION CHECKLIST

After completing all tasks:
- [x] `./gradlew assembleDebug` succeeds
- [x] App launches without crashes
- [x] Default settings (all zeros) behave same as before
- [x] Touch correction: Setting `touchCorrectionY: -20.0` shifts touch detection upward
- [x] Theme mode: Setting `themeMode: 1` forces dark theme regardless of system
- [x] Theme mode: Setting `themeMode: 2` forces light theme regardless of system
- [x] Accent color: Setting `customAccentColor: "#CC6699"` changes keyboard accent
- [x] Old settings.json files (without new fields) still load correctly
- [x] Reference manual contains color example table for user convenience

---

## 6. TASK EXECUTION ORDER

Execute in this order:
1. **Task 3.1** (settings property) - Infrastructure for other tasks
2. **Task 1.1** (touch constants) - Foundation for touch correction
3. **Task 1.2** (KeyboardSettings) - Add touch properties
4. **Task 1.3** (JSON default) - Add touch defaults
5. **Task 1.4** (apply touch correction) - Implement touch logic
6. **Task 2.1** (theme constants) - Foundation for theme overrides
7. **Task 2.2** (KeyboardSettings) - Add theme properties
8. **Task 2.3** (JSON default) - Add theme defaults
9. **Task 2.4** (apply theme) - Implement theme logic
10. **Task 3.2** (wire settings) - Connect service to views
11. **Task 1.5, 2.5** (documentation) - Update reference.md

---

## 7. FILE CHANGE SUMMARY

| File | Changes |
|------|---------|
| `Constants.kt` | Add 7 new constants (touch + theme defaults) |
| `CustomKeyboard.kt` | Add 4 new properties to `KeyboardSettings` |
| `settings_default.json` | Add 4 new JSON fields |
| `CustomKeyboardView.kt` | Add `settings` property, `setSettings()`, modify `onTouchEvent` and `onDraw` |
| `CustomKeyboardService.kt` | Add `setSettings()` calls for all views |
| `reference.md` | Add 4 new rows to KeyboardSettings table |

---

## 8. ROLLBACK PLAN

If issues arise:
1. Revert `KeyboardSettings` changes (remove new properties)
2. Revert `Constants.kt` changes (remove new constants)
3. Revert `settings_default.json` to previous state
4. Settings with unknown fields are ignored due to `ignoreUnknownKeys = true`

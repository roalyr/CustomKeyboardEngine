# IMMEDIATE-TODO.md
**Sprint:** Architecture Cleanup (from MAINTENANCE-PLAN.md)
**Date:** 2025-12-19

---

## 1. CONTEXT

Execute high and medium priority cleanup tasks from MAINTENANCE-PLAN.md to reduce technical debt:
1. **Extract UI strings to resources** - Improve localization readiness
2. **Extract color constants** - Eliminate magic numbers for theme colors
3. **Remove dead code** - Unused color fields in CustomKeyboardView.kt
4. **Consolidate duplicated methods** - Reduce maintenance burden

This sprint focuses on code quality without changing functionality.

---

## 2. ARCHITECTURE CHECK

| Component | Location |
|-----------|----------|
| String resources | `app/src/main/res/values/strings.xml` |
| Constants | `app/src/main/kotlin/com/roalyr/customkeyboardengine/Constants.kt` |
| Main Activity | `app/src/main/kotlin/com/roalyr/customkeyboardengine/ActivityMain.kt` |
| Permission Activity | `app/src/main/kotlin/com/roalyr/customkeyboardengine/ActivityPermissionRequest.kt` |
| Keyboard View | `app/src/main/kotlin/com/roalyr/customkeyboardengine/CustomKeyboardView.kt` |
| Keyboard Service | `app/src/main/kotlin/com/roalyr/customkeyboardengine/CustomKeyboardService.kt` |
| Popups | `app/src/main/kotlin/com/roalyr/customkeyboardengine/ClassFunctionsPopups.kt` |

---

## 3. ATOMIC TASKS

### Task 1: Extract UI Strings to Resources (HIGH PRIORITY)
- [x] **1.1 Add string resources to strings.xml**
  - **TARGET FILE:** `app/src/main/res/values/strings.xml`
  - **DEPENDENCIES:** None
  - **CONTENT TO ADD:**
    ```xml
    <!-- Buttons -->
    <string name="btn_grant_storage">1. Grant Storage Permission</string>
    <string name="btn_grant_overlay">2. Grant Overlay Permission</string>
    <string name="btn_change_input">3. Change Input Method</string>
    <string name="btn_copy_defaults">4. Copy (rewrite) default layouts and reference manual to working folder</string>
    <string name="btn_copy_settings">5. Copy (rewrite) default settings.json to working folder</string>
    <string name="btn_github">Visit GitHub Page</string>
    
    <!-- Labels -->
    <string name="label_working_dir">📂 Working Directory Path:</string>
    <string name="working_dir_path">/storage/emulated/0/Android/media/com.roalyr.customkeyboardengine/</string>
    
    <!-- Hints -->
    <string name="hint_json_files">ℹ️ HINT: Look for .json files in the directory above. You can edit them using any external text editor. Refer to reference.md in the folder for more information.</string>
    <string name="hint_directory">ℹ️ HINT: If the directory is not created automatically, create it manually and restart the app.</string>
    <string name="hint_test_input">Type here to test keyboard…</string>
    
    <!-- Warnings -->
    <string name="warning_backup">⚠️ IMPORTANT: Backup files in the directory above to prevent data loss before uninstalling the app.</string>
    
    <!-- Toasts -->
    <string name="toast_defaults_copied">Default layouts and reference copied</string>
    <string name="toast_settings_copied">Default settings.json copied</string>
    <string name="toast_settings_failed">Failed to copy settings.json: %1$s</string>
    <string name="toast_folder_failed">Failed to create working folder</string>
    <string name="error_unknown_permission">Unknown permission type.</string>
    <string name="info_storage_not_required">Storage permission not required for Android 10+.</string>
    ```
  - **SUCCESS CRITERIA:** All strings compile without errors

- [x] **1.2 Update ActivityMain.kt to use string resources**
  - **TARGET FILE:** `app/src/main/kotlin/com/roalyr/customkeyboardengine/ActivityMain.kt`
  - **DEPENDENCIES:** Task 1.1
  - **CHANGES:**
    - Import `androidx.compose.ui.res.stringResource`
    - Replace all hardcoded strings with `stringResource(R.string.xxx)`
    - For Toast messages, use `getString(R.string.xxx)` or format with `getString(R.string.xxx, arg)`
  - **SUCCESS CRITERIA:** No hardcoded UI strings remain in ActivityMain.kt

- [x] **1.3 Update ActivityPermissionRequest.kt to use string resources**
  - **TARGET FILE:** `app/src/main/kotlin/com/roalyr/customkeyboardengine/ActivityPermissionRequest.kt`
  - **DEPENDENCIES:** Task 1.1
  - **CHANGES:**
    - Replace `"Unknown permission type."` with `getString(R.string.error_unknown_permission)`
    - Replace `"Storage permission not required..."` with `getString(R.string.info_storage_not_required)`
  - **SUCCESS CRITERIA:** No hardcoded Toast strings remain

---

### Task 2: Extract Color Constants (HIGH PRIORITY)
- [x] **2.1 Add color constants to Constants.kt**
  - **TARGET FILE:** `app/src/main/kotlin/com/roalyr/customkeyboardengine/Constants.kt`
  - **DEPENDENCIES:** None
  - **CONTENT TO ADD (inside companion object):**
    ```kotlin
    // Theme color fallbacks
    const val DEFAULT_ACCENT_COLOR = 0xFF6A5ACD.toInt() // Soft purple fallback
    const val TEXT_COLOR_DARK_THEME = 0xFFCCCCCC.toInt()
    const val TEXT_COLOR_LIGHT_THEME = 0xFF333333.toInt()
    
    // Popup positioning
    const val ERROR_POPUP_MARGIN_TOP = 50
    ```
  - **SUCCESS CRITERIA:** Constants compile successfully

- [x] **2.2 Update CustomKeyboardView.kt to use color constants**
  - **TARGET FILE:** `app/src/main/kotlin/com/roalyr/customkeyboardengine/CustomKeyboardView.kt`
  - **DEPENDENCIES:** Task 2.1
  - **CHANGES:**
    - Line ~399: Replace `0xFF6A5ACD.toInt()` with `Constants.DEFAULT_ACCENT_COLOR`
    - Line ~410: Replace `0xFFCCCCCC.toInt()` with `Constants.TEXT_COLOR_DARK_THEME`
    - Line ~410: Replace `0xFF333333.toInt()` with `Constants.TEXT_COLOR_LIGHT_THEME`
  - **SUCCESS CRITERIA:** No hardcoded color values in onDraw()

- [x] **2.3 Update ClassFunctionsPopups.kt to use margin constant**
  - **TARGET FILE:** `app/src/main/kotlin/com/roalyr/customkeyboardengine/ClassFunctionsPopups.kt`
  - **DEPENDENCIES:** Task 2.1
  - **CHANGES:**
    - Replace `y = 50` with `y = Constants.ERROR_POPUP_MARGIN_TOP`
  - **SUCCESS CRITERIA:** Magic number eliminated

---

### Task 3: Remove Unused Code (MEDIUM PRIORITY)
- [x] **3.1 Remove unused color fields in CustomKeyboardView.kt**
  - **TARGET FILE:** `app/src/main/kotlin/com/roalyr/customkeyboardengine/CustomKeyboardView.kt`
  - **DEPENDENCIES:** None
  - **LINES TO REMOVE (~37-40):**
    ```kotlin
    private val keyTextColor = context.resources.getColor(R.color.key_text_color, null)
    private val keySmallTextColor = context.resources.getColor(R.color.key_small_text_color, null)
    private val keyBackgroundColor = context.resources.getColor(R.color.key_background_color, null)
    private val keyModifierBackgroundColor = context.resources.getColor(R.color.key_background_color_modifier, null)
    ```
  - **SUCCESS CRITERIA:** No compiler warnings about unused variables; app still renders correctly

---

### Task 4: Consolidate Duplicate Code (MEDIUM PRIORITY)
- [x] **4.1 Consolidate fallback layout loading methods**
  - **TARGET FILE:** `app/src/main/kotlin/com/roalyr/customkeyboardengine/CustomKeyboardService.kt`
  - **DEPENDENCIES:** None
  - **REFACTOR:**
    Replace `loadFallbackLanguageLayout()`, `loadFallbackServiceLayout()`, `loadFallbackClipboardLayout()` with:
    ```kotlin
    private fun loadFallbackLayout(resourceId: Int, layoutName: String): CustomKeyboard {
        return try {
            val json = resources.openRawResource(resourceId).bufferedReader().use { it.readText() }
            CustomKeyboard.fromJson(this, json, settings)
                ?: throw Exception("Parsed fallback $layoutName layout is null")
        } catch (e: Exception) {
            val errorMsg = "Error loading fallback $layoutName layout: ${e.message}"
            ClassFunctionsPopups.showErrorPopup(windowManager, this, TAG, errorMsg)
            throw e
        }
    }
    ```
  - **UPDATE CALLERS in handleFallback():**
    ```kotlin
    "Language" -> loadFallbackLayout(R.raw.keyboard_default, "language")
    "Service" -> loadFallbackLayout(R.raw.keyboard_service, "service")
    "Clipboard" -> loadFallbackLayout(R.raw.keyboard_clipboard_default, "clipboard")
    ```
  - **SUCCESS CRITERIA:** Three methods reduced to one; all fallbacks still work

---

### Task 5: Add Rendering Factor Constants (LOW PRIORITY - OPTIONAL)
- [x] **5.1 Add text size factor constants to Constants.kt**
  - **TARGET FILE:** `app/src/main/kotlin/com/roalyr/customkeyboardengine/Constants.kt`
  - **DEPENDENCIES:** None
  - **CONTENT TO ADD:**
    ```kotlin
    // Rendering text size factors (relative to key height)
    const val TEXT_SIZE_FACTOR_ICON = 0.6f
    const val TEXT_SIZE_FACTOR_SMALL_LABEL = 0.32f
    const val TEXT_SIZE_FACTOR_PRIMARY_LABEL = 0.37f
    const val TEXT_SIZE_FACTOR_CENTERED_LABEL = 0.5f
    const val TEXT_SIZE_FACTOR_MODIFIER_LABEL = 0.4f
    const val SMALL_LABEL_Y_POSITION_FACTOR = 0.35f
    const val LABEL_Y_OFFSET_FACTOR = 0.3f
    const val CLIPBOARD_KEY_PADDING_FACTOR = 0.1f
    ```
  - **SUCCESS CRITERIA:** Constants defined and ready for use

- [x] **5.2 Update CustomKeyboardView.kt to use rendering constants**
  - **TARGET FILE:** `app/src/main/kotlin/com/roalyr/customkeyboardengine/CustomKeyboardView.kt`
  - **DEPENDENCIES:** Task 5.1
  - **CHANGES:** Replace all magic float values in onDraw() with Constants references
  - **SUCCESS CRITERIA:** No magic floats in rendering code

---

## 4. CONSTRAINTS

1. **No Functional Changes:** This is a refactoring sprint - behavior must remain identical
2. **Null Safety:** Maintain all existing null-safe patterns
3. **Resource Naming:** Use snake_case for string resources (e.g., `btn_grant_storage`)
4. **Testing:** After each task, verify app builds and runs correctly
5. **Backwards Compatibility:** String resources should not break existing functionality
6. **Incremental Commits:** Each task should be a separate logical unit

---

## 5. VERIFICATION CHECKLIST

After completing all tasks:
- [ ] `./gradlew assembleDebug` succeeds
- [ ] App launches without crashes
- [ ] All buttons in ActivityMain work correctly
- [ ] Keyboard renders correctly (standard and floating modes)
- [ ] Error popups display correctly
- [ ] Fallback layouts load when needed
- [ ] No compiler warnings about unused variables

---

## 6. TASK PRIORITY ORDER

Execute in this order:
1. Task 1.1 (strings.xml) - Foundation for other string tasks
2. Task 2.1 (color constants) - Foundation for other constant tasks
3. Task 3.1 (remove unused code) - Quick win
4. Task 1.2, 1.3 (use string resources) - Depends on 1.1
5. Task 2.2, 2.3 (use color constants) - Depends on 2.1
6. Task 4.1 (consolidate fallbacks) - Independent refactor
7. Task 5.1, 5.2 (rendering constants) - Optional enhancement

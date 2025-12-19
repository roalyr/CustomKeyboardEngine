# MAINTENANCE-PLAN.md

**Architecture Audit Report**  
**Generated:** 2025-12-19  
**Status:** Ready for Cleanup

---

## Summary

Overall architecture is **clean and well-organized**. The codebase follows good separation of concerns with distinct files for data classes, views, services, and utilities. Minor improvements identified below.

---

## 1. CODE DUPLICATION

### 1.1 Duplicate Color Logic in CustomKeyboardView.kt
**Severity:** Low  
**Location:** [CustomKeyboardView.kt](app/src/main/kotlin/com/roalyr/customkeyboardengine/CustomKeyboardView.kt#L37-L40) & [CustomKeyboardView.kt](app/src/main/kotlin/com/roalyr/customkeyboardengine/CustomKeyboardView.kt#L408-L418)

**Issue:** Color resources loaded twice - once in class properties (lines 37-40) and again in `onDraw()` (lines 408-418). The class properties are unused.

- [ ] **Task:** Remove unused color fields at lines 37-40:
  ```kotlin
  private val keyTextColor = context.resources.getColor(R.color.key_text_color, null)
  private val keySmallTextColor = context.resources.getColor(R.color.key_small_text_color, null)
  private val keyBackgroundColor = context.resources.getColor(R.color.key_background_color, null)
  private val keyModifierBackgroundColor = context.resources.getColor(R.color.key_background_color_modifier, null)
  ```

---

### 1.2 Duplicate Label/SmallLabel Update Logic
**Severity:** Low  
**Location:** [CustomKeyboardView.kt](app/src/main/kotlin/com/roalyr/customkeyboardengine/CustomKeyboardView.kt#L609-L673)

**Issue:** `updateLabelState()` and `updateSmallLabelState()` share 80% identical logic for meta key handling.

- [ ] **Task:** Consider refactoring to a single helper method:
  ```kotlin
  private fun updateTextState(
      text: String?,
      keyCode: Int?,
      preserveCase: Boolean?,
      isModifier: Boolean?
  ): String?
  ```

---

### 1.3 Duplicate Fallback Layout Loading
**Severity:** Low  
**Location:** [CustomKeyboardService.kt](app/src/main/kotlin/com/roalyr/customkeyboardengine/CustomKeyboardService.kt#L846-L903)

**Issue:** `loadFallbackLanguageLayout()`, `loadFallbackServiceLayout()`, and `loadFallbackClipboardLayout()` are nearly identical except for resource ID.

- [ ] **Task:** Consolidate into single method:
  ```kotlin
  private fun loadFallbackLayout(resourceId: Int, layoutName: String): CustomKeyboard
  ```

---

### 1.4 Duplicate Error Popup Calls
**Severity:** Low  
**Location:** Multiple files

**Issue:** Error popup invocation pattern repeated throughout. Consider creating extension function.

- [ ] **Task (Optional):** Create extension:
  ```kotlin
  fun Context.showKeyboardError(windowManager: WindowManager, message: String)
  ```

---

## 2. MAGIC NUMBERS

### 2.1 CustomKeyboardView.kt - Missing Constants
**Severity:** Medium  
**Location:** [CustomKeyboardView.kt](app/src/main/kotlin/com/roalyr/customkeyboardengine/CustomKeyboardView.kt)

| Line | Value | Suggested Constant |
|------|-------|-------------------|
| 83 | `MSG_LONGPRESS = 1` | ✅ Already defined |
| 84 | `LONGPRESS_TIMEOUT = 250` | ✅ Already defined |
| 85 | `REPEAT_DELAY = 50` | ✅ Already defined |
| 86 | `REPEAT_START_DELAY = 250` | ✅ Already defined |
| 399 | `0xFF6A5ACD` | Move to `Constants.kt` as `DEFAULT_ACCENT_COLOR` |
| 410 | `0xFFCCCCCC` | Move to `Constants.kt` as `LIGHT_TEXT_COLOR` |
| 410 | `0xFF333333` | Move to `Constants.kt` as `DARK_TEXT_COLOR` |

- [ ] **Task:** Add to Constants.kt:
  ```kotlin
  const val DEFAULT_ACCENT_COLOR = 0xFF6A5ACD.toInt()
  const val LIGHT_TEXT_COLOR_DARK_THEME = 0xFFCCCCCC.toInt()
  const val DARK_TEXT_COLOR_LIGHT_THEME = 0xFF333333.toInt()
  ```

---

### 2.2 Drawing Scaling Factors
**Severity:** Low  
**Location:** [CustomKeyboardView.kt](app/src/main/kotlin/com/roalyr/customkeyboardengine/CustomKeyboardView.kt#L462-L547)

| Line | Value | Context |
|------|-------|---------|
| 468 | `0.6f` | Icon size factor |
| 481 | `0.32f` | Small label text size factor |
| 493 | `0.35f` | Small label Y position factor |
| 503 | `0.37f` | Primary label text size factor |
| 517 | `0.1f` | Clipboard key left padding factor |
| 537 | `0.3f` | Label Y offset factor |
| 544 | `0.4f` | Modifier text size |
| 544 | `0.5f` | Regular text size |

- [ ] **Task:** Consider adding these to `KeyboardSettings` for user customization, or move to `Constants.kt`:
  ```kotlin
  // Rendering text size factors
  const val TEXT_SIZE_FACTOR_ICON = 0.6f
  const val TEXT_SIZE_FACTOR_SMALL_LABEL = 0.32f
  const val TEXT_SIZE_FACTOR_PRIMARY_LABEL = 0.37f
  const val TEXT_SIZE_FACTOR_CENTERED_LABEL = 0.5f
  const val TEXT_SIZE_FACTOR_MODIFIER_LABEL = 0.4f
  ```

---

### 2.3 ClassFunctionsPopups.kt
**Severity:** Low  
**Location:** [ClassFunctionsPopups.kt](app/src/main/kotlin/com/roalyr/customkeyboardengine/ClassFunctionsPopups.kt#L41)

| Line | Value | Suggested |
|------|-------|-----------|
| 41 | `y = 50` | Move to Constants as `ERROR_POPUP_MARGIN_TOP` |

- [ ] **Task:** Add to Constants.kt:
  ```kotlin
  const val ERROR_POPUP_MARGIN_TOP = 50
  ```

---

## 3. HARDCODED STRINGS

### 3.1 ActivityMain.kt - UI Strings
**Severity:** Medium  
**Location:** [ActivityMain.kt](app/src/main/kotlin/com/roalyr/customkeyboardengine/ActivityMain.kt)

**Issue:** All button labels and hint texts are hardcoded in Compose code.

| Line | String | Suggested Resource |
|------|--------|-------------------|
| 75 | "1. Grant Storage Permission" | `@string/btn_grant_storage` |
| 82 | "2. Grant Overlay Permission" | `@string/btn_grant_overlay` |
| 89 | "3. Change Input Method" | `@string/btn_change_input` |
| 96 | "4. Copy (rewrite) default layouts..." | `@string/btn_copy_defaults` |
| 107 | "5. Copy (rewrite) default settings..." | `@string/btn_copy_settings` |
| 115 | "📂 Working Directory Path:" | `@string/label_working_dir` |
| 131 | Path string | `@string/working_dir_path` |
| 139-146 | Hint about JSON files | `@string/hint_json_files` |
| 149-155 | Hint about directory | `@string/hint_directory` |
| 159-165 | Warning about backup | `@string/warning_backup` |
| 171 | "Type here to test keyboard..." | `@string/hint_test_input` |
| 181 | "Visit GitHub Page" | `@string/btn_github` |
| 208 | "Default layouts and reference copied" | `@string/toast_defaults_copied` |
| 229 | "Default settings.json copied" | `@string/toast_settings_copied` |
| 231 | "Failed to copy settings.json" | `@string/toast_settings_failed` |
| 224 | "Failed to create working folder" | `@string/toast_folder_failed` |

- [ ] **Task:** Add all UI strings to `res/values/strings.xml`
- [ ] **Task:** Update ActivityMain.kt to use `stringResource(R.string.xxx)`

---

### 3.2 Toast Messages in ActivityPermissionRequest.kt
**Severity:** Low  
**Location:** [ActivityPermissionRequest.kt](app/src/main/kotlin/com/roalyr/customkeyboardengine/ActivityPermissionRequest.kt)

| Line | String | Suggested |
|------|--------|-----------|
| 37 | "Unknown permission type." | `@string/error_unknown_permission` |
| 54 | "Storage permission not required for Android 10+." | `@string/info_storage_not_required` |

- [ ] **Task:** Move to strings.xml

---

### 3.3 Error Messages in Service/Utilities
**Severity:** Low (Internal logging)  
**Location:** Multiple files

**Issue:** Error messages shown to users via popup are hardcoded.

- [ ] **Task (Low Priority):** Consider moving user-facing error messages to strings.xml for future localization support.

---

## 4. JSON SCHEMA CONSISTENCY

### 4.1 Documentation Status: ✅ CONSISTENT

**Verified:**
- [reference.md](app/src/main/res/raw/reference.md) documents all JSON attributes correctly
- `KeyboardLayout`, `Row`, `Key` data classes match documentation
- `KeyboardSettings` schema is documented with correct types and defaults
- Custom keycodes table matches `Constants.kt` values
- Available icons list matches `res/drawable/` contents

### 4.2 Minor Documentation Updates Needed
**Severity:** Low

- [ ] **Task:** Add missing `KEYCODE_ENLARGE_FLOATING_KEYBOARD_VERT` and `KEYCODE_SHRINK_FLOATING_KEYBOARD_VERT` implementation status note (currently marked "TODO" in reference.md which is correct)

---

## 5. ARCHITECTURE OBSERVATIONS

### 5.1 File Organization: ✅ GOOD
- Data models in `CustomKeyboard.kt`
- View logic in `CustomKeyboardView.kt`
- Service logic in `CustomKeyboardService.kt`
- Constants centralized in `Constants.kt`
- Utility functions split appropriately

### 5.2 Suggested Improvements

- [ ] **Task (Optional):** Consider creating a `RenderingConfig` data class for text size factors instead of magic numbers in `onDraw()`

- [ ] **Task (Optional):** The `labelToKeycodeMap` in CustomKeyboardService.kt (lines 625-673) could be moved to Constants.kt or a dedicated `KeycodeMapping.kt` file

---

## 6. CLEANUP CHECKLIST SUMMARY

### High Priority
- [ ] Add UI strings to `strings.xml` (ActivityMain.kt)
- [ ] Extract color constants for fallback colors in CustomKeyboardView.kt

### Medium Priority
- [ ] Remove unused color fields in CustomKeyboardView.kt (lines 37-40)
- [ ] Add rendering factor constants to Constants.kt
- [ ] Add Toast messages to strings.xml

### Low Priority (Optional Refactoring)
- [ ] Consolidate `updateLabelState()` / `updateSmallLabelState()`
- [ ] Consolidate fallback layout loading methods
- [ ] Create extension function for error popups
- [ ] Move `labelToKeycodeMap` to Constants.kt

---

## 7. FILES REVIEWED

| File | Status | Issues |
|------|--------|--------|
| Constants.kt | ✅ Clean | - |
| CustomKeyboard.kt | ✅ Clean | - |
| CustomKeyboardClipboard.kt | ✅ Clean | - |
| CustomKeyboardService.kt | ⚠️ Minor | Duplicate fallback methods |
| CustomKeyboardView.kt | ⚠️ Minor | Unused fields, magic numbers |
| ClassFunctionsFiles.kt | ✅ Clean | - |
| ClassFunctionsPopups.kt | ✅ Clean | Magic number (margin) |
| ActivityMain.kt | ⚠️ Moderate | All strings hardcoded |
| ActivityPermissionRequest.kt | ⚠️ Minor | Toast strings hardcoded |
| reference.md | ✅ Consistent | Matches code |
| strings.xml | ⚠️ Sparse | Only app_name defined |
| colors.xml | ✅ Clean | - |

---

**Next Steps:** Work through checklist items by priority. Start with High Priority items for immediate architecture improvement.

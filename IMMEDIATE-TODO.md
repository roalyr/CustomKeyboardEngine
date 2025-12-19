# IMMEDIATE-TODO.md
**Sprint:** Priority 1 - Critical Core Functionality
**Date:** 2025-12-19

---

## 1. CONTEXT

Implement the first three Priority 1 items from TODO.md to establish a robust foundation:
1. **Defensive null handling** for keys without code/label/icon
2. **JSON settings file** (`settings.json`) for user-overridable constants
3. **Touch correction offsets** as part of the settings system

These are foundational improvements that affect reliability and user customization.

---

## 2. ARCHITECTURE CHECK

| Component | Location |
|-----------|----------|
| Settings data class | `app/src/main/kotlin/com/roalyr/customkeyboardengine/CustomKeyboard.kt` |
| Settings loading logic | `app/src/main/kotlin/com/roalyr/customkeyboardengine/ClassFunctionsFiles.kt` |
| Constants integration | `app/src/main/kotlin/com/roalyr/customkeyboardengine/Constants.kt` |
| View touch handling | `app/src/main/kotlin/com/roalyr/customkeyboardengine/CustomKeyboardView.kt` |
| Service integration | `app/src/main/kotlin/com/roalyr/customkeyboardengine/CustomKeyboardService.kt` |
| Default settings file | `app/src/main/res/raw/settings_default.json` |
| Documentation | `app/src/main/res/raw/reference.md` |

---

## 3. ATOMIC TASKS

### Task 1: Defensive Null Handling for Keys
- [x] **1.1 Add safe key rendering in CustomKeyboardView.kt**
  - **TARGET FILE:** `app/src/main/kotlin/com/roalyr/customkeyboardengine/CustomKeyboardView.kt`
  - **DEPENDENCIES:** None
  - **PSEUDO-CODE:**
    ```kotlin
    // In onDraw(), wrap key rendering in null-safe checks:
    private fun shouldRenderKey(key: Key): Boolean {
        return key.keyCode != null || !key.label.isNullOrEmpty() || !key.icon.isNullOrEmpty()
    }
    
    // Skip drawing label/icon if both are null/empty
    // Ensure keyCode fallback doesn't crash if all are null
    ```
  - **SUCCESS CRITERIA:** 
    - App does not crash when a key has no keyCode, label, or icon
    - Empty keys render as blank rectangles (background only)

- [x] **1.2 Add safe key action handling in CustomKeyboardService.kt**
  - **TARGET FILE:** `app/src/main/kotlin/com/roalyr/customkeyboardengine/CustomKeyboardService.kt`
  - **DEPENDENCIES:** Task 1.1
  - **PSEUDO-CODE:**
    ```kotlin
    // In handleKey() and handleCustomKey():
    // Early return if code is null AND label is null/empty
    private fun handleKey(code: Int?, label: CharSequence?) {
        if (code == null && label.isNullOrEmpty()) return
        // ... existing logic
    }
    ```
  - **SUCCESS CRITERIA:**
    - Pressing a key with no code/label does nothing (no crash, no output)

---

### Task 2: JSON Settings File Infrastructure
- [x] **2.1 Create Settings data class**
  - **TARGET FILE:** `app/src/main/kotlin/com/roalyr/customkeyboardengine/CustomKeyboard.kt`
  - **DEPENDENCIES:** None
  - **PSEUDO-CODE:**
    ```kotlin
    @Serializable
    data class KeyboardSettings(
        // Keyboard dimensions
        val keyboardMinimalWidth: Int = Constants.KEYBOARD_MINIMAL_WIDTH,
        val keyboardMinimalHeight: Int = Constants.KEYBOARD_MINIMAL_HEIGHT,
        
        // Translation/Scale
        val keyboardTranslationIncrement: Int = Constants.KEYBOARD_TRANSLATION_INCREMENT,
        val keyboardScaleIncrement: Int = Constants.KEYBOARD_SCALE_INCREMENT,
        
        // Touch correction
        val touchCorrectionX: Float = 0f,
        val touchCorrectionY: Float = 0f,
        
        // Key defaults
        val defaultKeyHeight: Float = Constants.DEFAULT_KEY_HEIGHT,
        val defaultKeyWidth: Float = Constants.DEFAULT_KEY_WIDTH,
        val defaultLogicalRowGap: Float = Constants.DEFAULT_LOGICAL_ROW_GAP,
        val defaultLogicalKeyGap: Float = Constants.DEFAULT_LOGICAL_KEY_GAP,
        
        // Rendering
        val renderedKeyGap: Float = 5f,
        val renderedRowGap: Float = 5f,
        val keyCornerRadiusFactor: Float = 0.1f
    )
    ```
  - **SUCCESS CRITERIA:** Data class compiles with kotlinx.serialization

- [x] **2.2 Create default settings.json resource**
  - **TARGET FILE:** `app/src/main/res/raw/settings_default.json`
  - **DEPENDENCIES:** Task 2.1
  - **PSEUDO-CODE:**
    ```json
    {
      "keyboardMinimalWidth": 500,
      "keyboardMinimalHeight": 500,
      "keyboardTranslationIncrement": 50,
      "keyboardScaleIncrement": 50,
      "touchCorrectionX": 0.0,
      "touchCorrectionY": 0.0,
      "defaultKeyHeight": 40.0,
      "defaultKeyWidth": 10.0,
      "defaultLogicalRowGap": 0.0,
      "defaultLogicalKeyGap": 0.0,
      "renderedKeyGap": 5.0,
      "renderedRowGap": 5.0,
      "keyCornerRadiusFactor": 0.1
    }
    ```
  - **SUCCESS CRITERIA:** Valid JSON that can be parsed into KeyboardSettings

- [x] **2.3 Add settings file path to Constants.kt**
  - **TARGET FILE:** `app/src/main/kotlin/com/roalyr/customkeyboardengine/Constants.kt`
  - **DEPENDENCIES:** None
  - **PSEUDO-CODE:**
    ```kotlin
    const val SETTINGS_FILENAME = "settings.json"
    const val SETTINGS_DEFAULT = "settings_default"
    
    val MEDIA_SETTINGS_FILE: String
        get() = "$MEDIA_APP_DIRECTORY/$SETTINGS_FILENAME"
    ```
  - **SUCCESS CRITERIA:** Path constants are accessible

- [x] **2.4 Implement settings loading in ClassFunctionsFiles.kt**
  - **TARGET FILE:** `app/src/main/kotlin/com/roalyr/customkeyboardengine/ClassFunctionsFiles.kt`
  - **DEPENDENCIES:** Tasks 2.1, 2.2, 2.3
  - **PSEUDO-CODE:**
    ```kotlin
    object SettingsManager {
        private var cachedSettings: KeyboardSettings? = null
        
        fun loadSettings(context: Context, onError: (String) -> Unit): KeyboardSettings {
            cachedSettings?.let { return it }
            
            val file = File(Constants.MEDIA_SETTINGS_FILE)
            return if (file.exists()) {
                try {
                    Json.decodeFromString<KeyboardSettings>(file.readText())
                } catch (e: Exception) {
                    onError("Failed to parse settings: ${e.message}")
                    loadDefaultSettings(context)
                }
            } else {
                loadDefaultSettings(context)
            }.also { cachedSettings = it }
        }
        
        private fun loadDefaultSettings(context: Context): KeyboardSettings {
            val inputStream = context.resources.openRawResource(R.raw.settings_default)
            return Json.decodeFromString(inputStream.bufferedReader().readText())
        }
        
        fun reloadSettings() { cachedSettings = null }
    }
    ```
  - **SUCCESS CRITERIA:** Settings load from user file or fallback to defaults

- [x] **2.5 Copy default settings file functionality in ActivityMain.kt**
  - **TARGET FILE:** `app/src/main/kotlin/com/roalyr/customkeyboardengine/ActivityMain.kt`
  - **DEPENDENCIES:** Tasks 2.2, 2.3
  - **PSEUDO-CODE:**
    ```kotlin
    // Add button/function to copy default settings.json to media directory
    // Similar to existing layout copying functionality
    ```
  - **SUCCESS CRITERIA:** User can copy default settings.json to external storage

---

### Task 3: Integrate Touch Correction
- [ ] **3.1 Apply touch correction in CustomKeyboardView.kt**
  - **TARGET FILE:** `app/src/main/kotlin/com/roalyr/customkeyboardengine/CustomKeyboardView.kt`
  - **DEPENDENCIES:** Tasks 2.1, 2.4
  - **PSEUDO-CODE:**
    ```kotlin
    // Add settings reference
    private var settings: KeyboardSettings? = null
    
    fun updateSettings(settings: KeyboardSettings) {
        this.settings = settings
    }
    
    // In handleTouchDown():
    private fun handleTouchDown(x: Int, y: Int, pointerId: Int) {
        val correctedX = x + (settings?.touchCorrectionX ?: 0f).toInt()
        val correctedY = y + (settings?.touchCorrectionY ?: 0f).toInt()
        val key = keyboard?.getKeyAt(correctedX.toFloat(), correctedY.toFloat()) ?: return
        // ...
    }
    ```
  - **SUCCESS CRITERIA:** Touch input respects correction offsets from settings

- [ ] **3.2 Load and pass settings in CustomKeyboardService.kt**
  - **TARGET FILE:** `app/src/main/kotlin/com/roalyr/customkeyboardengine/CustomKeyboardService.kt`
  - **DEPENDENCIES:** Tasks 2.4, 3.1
  - **PSEUDO-CODE:**
    ```kotlin
    private var settings: KeyboardSettings? = null
    
    override fun onCreate() {
        // ... existing code
        settings = SettingsManager.loadSettings(this) { error ->
            ClassFunctionsPopups.showErrorPopup(windowManager, this, TAG, error)
        }
    }
    
    // Pass settings to keyboard views after creation
    keyboardView?.updateSettings(settings!!)
    floatingKeyboardView?.updateSettings(settings!!)
    ```
  - **SUCCESS CRITERIA:** Settings are loaded once at service start and passed to views

---

### Task 4: Documentation Update
- [ ] **4.1 Update reference.md with settings documentation**
  - **TARGET FILE:** `app/src/main/res/raw/reference.md`
  - **DEPENDENCIES:** Tasks 2.1, 2.2
  - **PSEUDO-CODE:**
    ```markdown
    ## Settings File (settings.json)
    
    | Attribute | Type | Default | Description |
    |-----------|------|---------|-------------|
    | touchCorrectionX | Float | 0.0 | Horizontal touch offset in logical units |
    | touchCorrectionY | Float | 0.0 | Vertical touch offset in logical units |
    | ... (all other settings)
    ```
  - **SUCCESS CRITERIA:** All settings attributes documented with types and defaults

---

## 4. CONSTRAINTS

1. **Serialization:** Use `kotlinx.serialization` for all JSON parsing
2. **Null Safety:** Use `?.` and `?:` operators; never use `!!` except where guaranteed
3. **Backwards Compatibility:** App must work without settings.json file (use defaults)
4. **Error Handling:** All file operations must have error callbacks
5. **Single Source of Truth:** Settings values should come from `KeyboardSettings` object, not `Constants` directly (except as defaults)
6. **File Locations:**
   - User settings: `Android/media/com.roalyr.customkeyboardengine/settings.json`
   - Default settings: `res/raw/settings_default.json`

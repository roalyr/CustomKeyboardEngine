# CustomKeyboardEngine reference manual

### [Hint: Use Markor text editor to view the document in a more convenient way](https://github.com/gsantner/markor)
### [Download layout .json files here](https://github.com/roalyr/CustomKeyboardEngine-layouts-language)

## Folder Structure
```
Android/media/com.roalyr.customkeyboardengine
├── reference.md                            # This documentation file is also copied to the folder
├── layouts/
│   ├── layouts-language/                   # Language-specific layouts (any number any names)
│   │   ├── keyboard_default.json           # Default layout that is copied from the app
│   │   ├── keyboard_ua.json                # Custom layout that can be written by user
│   │   └── ... (other layouts)
│   ├── layouts-service/                    # Special layouts (have strictly defined names)
│   │   ├── keyboard_service_default.json   # Small layout to operate floating window
│   │   ├── keyboard_clipboard_default.json # Clipboard layout with 10 entries
└── ...
```

---
## JSON Layout Attributes

Attributes with types defined with a question mark (e.g., `Float?`) are optional and will fallback to the previous level of defaults:
(Key → Row → KeyboardLayout).

---

### 1.KeyboardLayout

The root object of the layout file.

| Attribute              | Type        | Description                                                  |
|------------------------|-------------|--------------------------------------------------------------|
| `rows`                 | List<Row>   | List of rows containing the keyboard keys.                   |
| `defaultKeyHeight`     | Float       | Default key height (in DP).                                  |
| `defaultKeyWidth`      | Float       | Default key width (percentage of layout width).              |
| `defaultLogicalRowGap` | Float       | Default gap between rows (in DP).                            |
| `defaultLogicalKeyGap` | Float       | Default gap between keys (percentage of row width).          |

---

### 2.Row

Defines a row in the keyboard.

| Attribute               | Type        | Description                                                                           |
|-------------------------|-------------|---------------------------------------------------------------------------------------|
| `keys`                  | List<Key>   | List of keys in this row.                                                             |
| `logicalRowGap`         | Float?      | Space below this row (fallback to `KeyboardLayout.defaultLogicalRowGap`).             |
| `defaultKeyHeight`      | Float?      | Height of the row (fallback to `KeyboardLayout.defaultKeyHeight`).                    |
| `defaultKeyWidth`       | Float?      | Default width for keys in this row (fallback to `KeyboardLayout.defaultKeyWidth`).    |
| `defaultLogicalKeyGap`  | Float?      | Default gap for keys in this row (fallback to `KeyboardLayout.defaultLogicalKeyGap`). |

---

### 3.Key

Defines individual key attributes.

| Attribute                | Type     | Description                                                                                  |
|--------------------------|----------|----------------------------------------------------------------------------------------------|
| `keyCode`                | Int?     | Primary key code (if not defined, the key will commit its label).                            |
| `keyCodeLongPress`       | Int?     | Key code triggered on long press.                                                            |
| `label`                  | String?  | Primary text label on the key.                                                               |
| `labelLongPress`         | String?  | Small secondary label shown on long press.                                                   |
| `icon`                   | String?  | Path to the key's drawable icon (e.g., `@drawable/icon_name`).                               |
| `keyWidth`               | Float?   | Width of the key (fallback to `Row.defaultKeyWidth` or `KeyboardLayout.defaultKeyWidth`).    |
| `keyHeight`              | Float?   | Height of the key (fallback to `Row.defaultKeyHeight` or `KeyboardLayout.defaultKeyHeight`). |
| `logicalKeyGap`          | Float?   | Gap to the next key (fallback to gap defaults).                                              |
| `isRepeatable`           | Boolean? | If true, the key repeats when held.                                                          |
| `isModifier`             | Boolean? | If true, the key is painted as a modifier (Shift, Tab, Esc, etc.).                           |
| `preserveLabelCase`      | Boolean? | If true, do not change key label case (on Shift or Caps Lock).                               |
| `preserveSmallLabelCase` | Boolean? | If true, do not change small label case (e.g., Shift or Caps Lock).                          |
| `x`                      | Float    | Logical X position (calculated automatically, do not set).                                   |
| `y`                      | Float    | Logical Y position (calculated automatically, do not set).                                   |
| `id`                     | Int      | Unique key ID for current layout (calculated automatically, do not set).                     |

---

## 4. KeyboardSettings (settings.json)

Optional user-configurable settings file (`settings.json`) that provides default values for layouts.

**Priority Cascade**: Layout JSON value > settings.json value > Built-in defaults.
This means individual layouts can override settings.json values by specifying them explicitly.

| Attribute                 | Type  | Default                              | Description                                                |
|---------------------------|-------|--------------------------------------|------------------------------------------------------------|
| `keyboardMinimalWidth`    | Int   | 500                                  | Minimum keyboard width in pixels                           |
| `keyboardMinimalHeight`   | Int   | 500                                  | Minimum keyboard height in pixels                          |
| `keyboardTranslationIncrement` | Int | 50                              | Pixel offset for floating keyboard movement               |
| `keyboardScaleIncrement`  | Int   | 50                                   | Pixel increment for floating keyboard resizing             |
| `defaultKeyHeight`        | Float | 40.0                                 | Default key height in DP (when not specified in layout)    |
| `defaultKeyWidth`         | Float | 10.0                                 | Default key width as percentage of row width               |
| `defaultLogicalRowGap`    | Float | 0.0                                  | Default vertical gap between rows in DP                    |
| `defaultLogicalKeyGap`    | Float | 0.0                                  | Default horizontal gap between keys as percentage          |
| `renderedKeyGap`          | Float | 5.0                                  | Visual gap between keys on screen in pixels                |
| `renderedRowGap`          | Float | 5.0                                  | Visual gap between rows on screen in pixels                |
| `keyCornerRadiusFactor`   | Float | 0.1                                  | Corner radius multiplier (0.1 = 10% of key height)        |

**File Location**: `Android/media/com.roalyr.customkeyboardengine/settings.json`

If `settings.json` is missing or invalid, the app uses built-in defaults from `R.raw.settings_default`.

---

## Available Icons

An example of declaring a key icon: `"icon": "@drawable/ic_tab"`

| File Name                       | Icon description                |
|---------------------------------|---------------------------------|
| ic_anchor                       | Anchor                          |
| ic_backspace                    | Backspace                       |
| ic_clipboard                    | Clipboard                       |
| ic_close                        | Close                           |
| ic_down_bold_arrow              | Down Bold Arrow                 |
| ic_down_triangle_arrow          | Down Triangle Arrow             |
| ic_emoji                        | Emoji                           |
| ic_eye                          | Eye                             |
| ic_eye_disabled                 | Eye Disabled                    |
| ic_launcher_background          | Launcher Background             |
| ic_launcher_foreground          | Launcher Foreground             |
| ic_left_bold_arrow              | Left Bold Arrow                 |
| ic_left_triangle_arrow          | Left Triangle Arrow             |
| ic_right_bold_arrow             | Right Bold Arrow                |
| ic_right_triangle_arrow         | Right Triangle Arrow            |
| ic_space                        | Space                           |
| ic_tab                          | Tab                             |
| ic_up_bold_arrow                | Up Bold Arrow                   |
| ic_up_triangle_arrow            | Up Triangle Arrow               |
| ic_warning                      | Warning                         |
| ic_zoom_in                      | Zoom In                         |
| ic_zoom_out                     | Zoom Out                        |


---

## Custom Keycodes

Custom keycodes are used to trigger special actions. These are defined in the `Constants.kt` file:

| Keycode                                  | Value | Description                                                        |
|------------------------------------------|-------|--------------------------------------------------------------------|
| `KEYCODE_CLOSE_FLOATING_KEYBOARD`        | -10   | Closes the floating keyboard                                       |
| `KEYCODE_OPEN_FLOATING_KEYBOARD`         | -11   | Opens the floating keyboard                                        |
| `KEYCODE_SWITCH_KEYBOARD_MODE`           | -12   | Switches the keyboard mode                                         |
| `KEYCODE_ENLARGE_FLOATING_KEYBOARD`      | -13   | Enlarges the floating keyboard horizontally                        |
| `KEYCODE_SHRINK_FLOATING_KEYBOARD`       | -14   | Shrinks the floating keyboard horizontally                         |
| `KEYCODE_ENLARGE_FLOATING_KEYBOARD_VERT` | -15   | Enlarges the floating keyboard vertically (TODO)                   |
| `KEYCODE_SHRINK_FLOATING_KEYBOARD_VERT`  | -16   | Shrinks the floating keyboard vertically (TODO)                    |
| `KEYCODE_MOVE_FLOATING_KEYBOARD_LEFT`    | -17   | Moves the floating keyboard to the left                            |
| `KEYCODE_MOVE_FLOATING_KEYBOARD_RIGHT`   | -18   | Moves the floating keyboard to the right                           |
| `KEYCODE_MOVE_FLOATING_KEYBOARD_UP`      | -19   | Moves the floating keyboard upward                                 |
| `KEYCODE_MOVE_FLOATING_KEYBOARD_DOWN`    | -20   | Moves the floating keyboard downward                               |
| `KEYCODE_CYCLE_LANGUAGE_LAYOUT`          | -21   | Cycles through available language layouts                          |
| `KEYCODE_CLIPBOARD_ENTRY`                | -22   | Defines a key to be a clipboard text holder (see clipboard layout) |
| `KEYCODE_CLIPBOARD_ERASE`                | -23   | Clears all clipboard keys (emptying clipboard)                     |
| `KEYCODE_OPEN_CLIPBOARD`                 | -24   | Open or close clipboard special layout                             |
| `KEYCODE_IGNORE`                         | -1    | Placeholder key code for short or long press to perform no action  |

For all other standard keycodes, refer to [Android KeyEvent Documentation](https://developer.android.com/reference/android/view/KeyEvent).

---

## Layout Dynamic Reset

Layouts are dynamically reloaded when initiating an input view or cycling layouts:

1. **Language Layouts** are loaded from `layouts-language/`.
2. **Service (special) Layouts** are loaded from `layouts-service/`.

---

## Default Layout Copying

You can manually copy default layouts from app menu. 
If it fails to create a folder, make it manually with file manager (`/storage/emulated/0/Android/media/com.roalyr.customkeyboardengine`).

- `layouts-language/keyboard_default.json`
- `layouts-service/keyboard_service_default.json`
- `layouts-service/keyboard_clipboard_default.json`

If these files are missing or corrupted, fallback layouts are loaded from built-in resources.
You can use those layouts as templates by saving them under different names.

---

## How to Use

1. **Place Layout Files**: Add (or edit existing) JSON files in `layouts-language/` or `layouts-service/`.
2. **Edit Layouts**: Customize keys, dimensions, and behaviors in the JSON files.
3. **Restart Keyboard**: Cycle layouts / re-open keyboard / switch mode for changes to take effect.

---

## Notes

- Long-press actions can be defined using `keyCodeLongPress` and `smallLabel`.
- Keys can extend into adjacent rows using custom heights (like a space key in default layout).

---

## Debugging

- Parsing errors are displayed as **error popups** on the screen with details.
- Every error is also mirrored via `Log.e()` call.
- `Log.i()`, `Log.d()` are (should be) suppressed, leaving no trace of app activity.

---

## Troubleshooting

- **Issue**: Layouts are not updating.  
  **Solution**: Check for syntax errors in the JSON files. Ensure correct file placement.

- **Issue**: Keyboard doesn't show.  
  **Solution**: Verify overlay and storage permissions are granted.

- **Issue**: Keyboard crashes when loading a layout.  
  **Solution**: Report an issue (because fallback layouts must always work).


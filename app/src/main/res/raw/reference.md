# CustomKeyboardEngine reference manual

### [Hint: Use Markor text editor to view the document in a more convenient way](https://github.com/gsantner/markor)

## Folder Structure
```
Android/media/com.roalyr.customkeyboardengine
├── reference.md
├── layouts/
│   ├── layouts-language/       # Language-specific layouts
│   │   ├── keyboard-default.json
│   │   ├── keyboard-ua.json
│   │   └── ... (other layouts)
│   ├── layouts-service/        # Service-specific layouts
│   │   ├── keyboard-service.json
└── ...
```

---

## JSON Layout Attributes

Attributes with type defined with question mark (i.e. Float?) are optional and are set to fallback to previous level
(Key -> Row -> KeyboardLayout).

### 1. **KeyboardLayout**
The root object of the layout file.

| Attribute            | Type          | Description                                         |
|----------------------|---------------|-----------------------------------------------------|
| `rows`               | List<Row>     | List of rows that contain the keyboard keys.        |
| `defaultKeyHeight`   | Float         | Default key height (in DP).                         |
| `defaultKeyWidth`    | Float         | Default key width (percentage of layout width).     |
| `defaultRowGap`      | Float         | Default gap between rows (in DP).                   |
| `defaultKeyGap`      | Float         | Default gap between keys (percentage of row width). |

---

### 2. **Row**
Defines a row in the keyboard.

| Attribute            | Type          | Description                                        |
|----------------------|---------------|----------------------------------------------------|
| `keys`               | List<Key>     | List of keys in this row.                          |
| `rowHeight`          | Float?        | Height of the row (fallback to `defaultKeyHeight`).|
| `rowGap`             | Float?        | Space below this row (in DP).                      |
| `defaultKeyWidth`    | Float?        | Default width for keys in this row (percentage).   |
| `defaultKeyGap`      | Float?        | Default gap for keys in this row (percentage).     |

---

### 3. **Key**
Defines individual key attributes.

| Attribute                | Type          | Description                                                                               |
|--------------------------|---------------|-------------------------------------------------------------------------------------------|
| `keyCode`                | Int?          | Primary key code (if not defined, key will commit its label.                              |
| `keyCodeLongPress`       | Int?          | Key code triggered on long press.                                                         |
| `isRepeatable`           | Boolean?      | If true, the key repeats when held.                                                       |
| `isModifier`             | Boolean?      | If true, the key is painted as modifier (Shift, Tab, Esc, etc).                           |
| `preserveLabelCase`      | Boolean?      | If true, do not change key label case (on Shift or Caps Lock).                            |
| `preserveSmallLabelCase` | Boolean?      | If true, do not change small label case (e.g., Shift or Caps Lock).                       |
| `label`                  | String?       | Primary text label on the key.                                                            |
| `smallLabel`             | String?       | Small secondary label.                                                                    |
| `icon`                   | String?       | Path to the key's drawable icon (e.g., @drawable/icon_name, nullable).                    |
| `keyWidth`               | Float?        | Width of the key (fallback to `Row.defaultKeyWidth` or `KeyboardLayout.defaultKeyWidth`). |
| `keyHeight`              | Float?        | Height of the key (fallback to `Row.rowHeight` or `KeyboardLayout.defaultKeyHeight`).     |
| `keyGap`                 | Float?        | Gap to the next key (fallback to gap defaults).                                           |
| `x`                      | Float         | Logical X position (calculated automatically, do not set).                                |
| `y`                      | Float         | Logical Y position (calculated automatically, do not set).                                |

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

| Keycode                                  | Value | Description                                             |
|------------------------------------------|-------|---------------------------------------------------------|
| `KEYCODE_CLOSE_FLOATING_KEYBOARD`        | -10   | Closes the floating keyboard.                           |
| `KEYCODE_OPEN_FLOATING_KEYBOARD`         | -11   | Opens the floating keyboard.                            |
| `KEYCODE_SWITCH_KEYBOARD_MODE`           | -12   | Switches the keyboard mode.                             |
| `KEYCODE_ENLARGE_FLOATING_KEYBOARD`      | -13   | Enlarges the floating keyboard horizontally.            |
| `KEYCODE_SHRINK_FLOATING_KEYBOARD`       | -14   | Shrinks the floating keyboard horizontally.             |
| `KEYCODE_ENLARGE_FLOATING_KEYBOARD_VERT` | -15   | Enlarges the floating keyboard vertically. (TODO)       |
| `KEYCODE_SHRINK_FLOATING_KEYBOARD_VERT`  | -16   | Shrinks the floating keyboard vertically. (TODO)        |
| `KEYCODE_MOVE_FLOATING_KEYBOARD_LEFT`    | -17   | Moves the floating keyboard to the left.                |
| `KEYCODE_MOVE_FLOATING_KEYBOARD_RIGHT`   | -18   | Moves the floating keyboard to the right.               |
| `KEYCODE_MOVE_FLOATING_KEYBOARD_UP`      | -19   | Moves the floating keyboard upward.                     |
| `KEYCODE_MOVE_FLOATING_KEYBOARD_DOWN`    | -20   | Moves the floating keyboard downward.                   |
| `KEYCODE_CYCLE_LANGUAGE_LAYOUT`          | -21   | Cycles through available language layouts.              |
| `KEYCODE_IGNORE`                         | -1    | Placeholder.                                            |

For all other standard keycodes, refer to [Android KeyEvent Documentation](https://developer.android.com/reference/android/view/KeyEvent).

---

## Layout Dynamic Reset

Layouts are dynamically reloaded when initiating an input view or cycling layouts:

1. **Language Layouts** are loaded from `layouts-language/`.
2. **Service Layouts** are loaded from `layouts-service/`.

---

## Default Layout Copying

Upon every launch (TODO: make it on-demand), the app copies default layouts into the following folders if they are missing:

- `layouts-language/keyboard-default.json`
- `layouts-service/keyboard-service.json`

If these files are missing or corrupted, fallback layouts are loaded from built-in resources.
You can use those layouts as templates by saving them under different names.

---

## How to Use

1. **Place Layout Files**: Add your JSON files to `layouts-language/` or `layouts-service/`. (TODO: implement custom keycodes for service keyboards).
2. **Edit Layouts**: Customize keys, dimensions, and behaviors in the JSON files.
3. **Restart Keyboard**: The keyboard detects changes and reloads layouts dynamically.
4. **Switch Layouts**: Use the `KEYCODE_CYCLE_LANGUAGE_LAYOUT` key to cycle through language layouts (all valid layout files in the directory).

---

## Notes

- Long-press actions can be defined using `keyCodeLongPress` or `smallLabel`.
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
  **Solution**: Check the error popups or logs for parsing issues and fix the JSON structure.


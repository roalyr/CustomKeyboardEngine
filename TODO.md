# CustomKeyboardEngine TODO

## Priority 1: Critical / Core Functionality
- [ ] Ensure safe operation if no code / label / icon are set for key (defensive null handling).
- [ ] JSON settings file: Implement custom values for certain constants that can be overridden.
- [ ] JSON setting for horizontal and vertical touch correction offsets.

## Priority 2: High / User Experience
- [ ] Implement snippet templates (declared in `snippets.json` file).
- [ ] Implement `layouts-swipes` and declare 4 types of swipes across the keyboard (up, down, left, right).
- [ ] Add link to https://github.com/roalyr/CustomKeyboardEngine-layouts-language into main activity and documentation.
- [ ] JSON setting to define dark/bright theme and accent color and key rounding (until full-feature themes are implemented).

## Priority 3: Medium / Feature Enhancements
- [ ] Implement emoji picker.
- [ ] Implement different popups for errors vs warnings (visual distinction).
- [ ] Implement unification in SVG icon sizes, introduce icon size overrides per-key.
- [ ] Implement pop-up rows inflation for additional symbols (`popupShortPress` and `popupLongPress` attributes).
- [ ] Provide multiple layout variants within single JSON (e.g., narrow 7-row and wide 3-row), with dedicated keycode for switching variants.

## Priority 4: Low / Future Features
- [ ] Implement vertical shrinking of the floating window (if useful).
- [ ] Add `keyColor`, `keyLabelColor`, `keySmallLabelColor`, `keyIconColor` Key attributes for per-key color override.
- [ ] Implement UTF-8 symbol picker (iterate through range and render symbols).
- [ ] Implement manual folder backup to archive (export layouts).
- [ ] JSON themes (separate theme files to be referenced by settings).

## Priority 5: Research / Investigation
- [ ] Implement basic file viewer and .json text editor (filter out all other files and folders).
- [ ] Investigate drawable loading from external storage.
- [ ] Investigate accessing drawables from OS resources.
- [ ] Investigate possibility to make a touchpad-emulating key (useful in wide landscape layout).
- [ ] Investigate double-floating (mirrored translation and scale) split layout.
- [ ] Investigate implementing built-in spell checker integration.

---

## Completed (1.2.x)
- [x] Notification messages added instead of SAF.
- [x] Copy documentation .txt file to working folder.
- [x] Better control over default keyboard layouts copying (make it manual from main activity).
- [x] Make logical key size larger and key gap absolute. Row and Key gap defaults = 0.
- [x] Better key gap management: keyGap and rowGap - logical grid gaps, rendered key gaps should not affect touch detection.
- [x] Rendered gaps - absolute offsets.
- [x] Implement clipboard.
- [x] Refine default keyboards.
- [x] Implement a list of drawable key icons with documentation and fail-safety for icon loading.
- [x] Add key icon color value and make all icons #FFFFFFFF.
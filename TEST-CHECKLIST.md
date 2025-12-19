# TEST-CHECKLIST.md - Manual App Behavior Testing

---

## Pre-Test Setup
- [PASS] Build APK: `./gradlew assembleDebug`
- [PASS] Install on device/emulator
- [PASS] Enable keyboard in Settings → System → Keyboard
- [PASS] Set as default input method

---

## 1. Basic Keyboard Functionality
- [PASS] Keyboard appears when tapping text field
- [PASS] Keyboard dismisses properly
- [PASS] Key labels display correctly
- [PASS] Key icons render (Tab, Backspace, Arrows)

## 2. Key Input
- [PASS] Letter key → correct character
- [PASS] Number/symbol keys → correct character
- [PASS] Space key → space inserted
- [PASS] Backspace → deletes character
- [PASS (Doesn't act as a search button in context windows that explicitly require search button press like on GBoard)] Enter → newline/action
- [PASS] Tab → tab character

## 3. Long Press
- [PASS] Long press with `labelLongPress` → secondary char
- [PASS] Long press with `keyCodeLongPress` → action
- [PASS] Key repeat on Backspace/Arrows

## 4. Modifier Keys
- [PASS] Shift → uppercase labels
- [PASS] Shift + letter → uppercase, then reverts
- [PASS] Caps Lock → locks uppercase
- [PASS] Ctrl modifier works
- [PASS] Alt modifier works

## 5. Layout Cycling
- [PASS] Cycle layout (keycode -21) → next layout
- [PASS] All layouts in `layouts-language/` detected
- [PASS] Layout indicator updates

## 6. Floating Keyboard
- [PASS] Open floating (keycode -11)
- [PASS] Close floating (keycode -10)
- [PASS] Switch mode (keycode -12)
- [PASS] Floating renders over apps
- [PASS] Input works from floating

## 7. Floating Controls
- [PASS] Enlarge horizontal (keycode -13)
- [PASS] Shrink horizontal (keycode -14)
- [PASS] Move left (keycode -17)
- [PASS] Move right (keycode -18)
- [PASS] Move up (keycode -19)
- [PASS] Move down (keycode -20)
- [PASS] Min size enforced

## 8. Clipboard
- [PASS] Open clipboard (keycode -24)
- [PASS] Entries display stored text
- [PASS] Tap entry → paste text
- [PASS] Erase clipboard (keycode -23)

## 9. Arrow/Navigation
- [PASS] Left/Right arrows move cursor
- [PASS] Up/Down arrows (multiline)
- [Didn't implement yet] Home/End keys
- [Didi't implement yet] Page Up/Down

## 10. Layout Loading
- [PASS] Default layouts copy via menu
- [PASS] Custom layouts load from `layouts-language/`
- [PASS] Service layouts load from `layouts-service/`
- [PASS] Invalid JSON shows error popup
- [PASS] Missing layout → fallback

## 11. Settings (settings.json) - NEW
- [FAIL] Custom `settings.json` loads
- [ ] `keyboardMinimalWidth/Height` enforced
- [ ] `touchCorrectionX/Y` adjusts touches
- [ ] `renderedKeyGap/renderedRowGap` applies
- [ ] `keyCornerRadiusFactor` affects corners
- [FAIL] Invalid settings → fallback to defaults

## 12. Null Key Handling - NEW
- [PASS] Key with no keyCode/label/icon renders (blank rect)
- [PASS] Pressing empty key → no crash, no output

## 13. Visual/UI
- [Not implemented (for privacy)] Key press feedback visible
- [PASS] Modifier active state shown
- [PASS] Caps Lock indicator
- [PASS] Key gaps render correctly

## 14. Edge Cases
- [PASS] Empty layout → fallback
- [PASS] Very long labels → handled
- [PASS] Rapid tapping → no missed input
- [PASS] Screen rotation → state preserved

---

**Report**: PASS / FAIL with error description (build, crash, behavior).

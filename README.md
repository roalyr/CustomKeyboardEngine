# New7rowKB
![layout](doc/img_main.png)

- Privacy-friendly (no data collection, no taps indication).
- Coding-oriented (all the symbols and necessary keys up front).
- Uses `sendKeyEvent()` to simulate physical keyboard as close as possible.
- Suitable for any screen, has floating mode (like Gboard).

## Screenshots
![floating mode portrait](doc/screen_floating.png)
![floating mode landscape](doc/screen_floating_landscape.png)

## TODO
- Implement simple layout scrolling.
- Display layout name on the space button (?).
- Enable floating keyboard from persistent notification.
- Enable preferences storage and save KB parameters.
- Enable color picker and font scaling in the app.
- Enable .xml uploading from the storage.

## Not working:
- Google chat (have to switch to gboard to type anything, since it is 
accepting things via `currentInputConnection.commitText(finalKeyLabel, 1)`
and cursor operations are different.

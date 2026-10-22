# Purgr

Swipe through your gallery like Tinder: **right = keep, left = delete**.

- Swipes only *mark* items. Nothing is deleted per swipe.
- Tap **Review (N)** to end a session: you see everything you marked, tap anything to rescue it, then confirm.
- Confirmed items go to Android's system trash (restorable for ~30 days) and show up in Purgr's **Trash** screen
  (bin icon, top right), where you can restore them or delete them forever.
- Decisions are remembered, so photos you kept won't show up again. "Start over" resets this.
- Undo button reverts the last swipe. Tap a video card to play it.

Requires Android 11+ (uses `MediaStore.createTrashRequest`).

## Build

Open in Android Studio and run, or `./gradlew assembleDebug`
(APK at `app/build/outputs/apk/debug/app-debug.apk`). The GitHub Actions workflow also builds the APK on every push.

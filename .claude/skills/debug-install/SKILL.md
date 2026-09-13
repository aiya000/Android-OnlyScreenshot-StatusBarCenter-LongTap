---
name: debug-install
description: Install the built debug APK of this OnlyScreenshot app on the connected device with adb. Use when the user asks to install or deploy the debug build; build it first with the `debug-build` skill if needed.
---

# debug-install

Install the debug APK on the device connected via adb.

## Environment

- The device is usually connected with **wireless adb**. The address (`<ip>:<port>`) changes between sessions and
  is not stored in the repository. It is shown on the device under
  設定 → 開発者向けオプション → ワイヤレスデバッグ
- adb has reached the device from **inside** the Bash sandbox on this machine. Try the plain command first and
  only fall back to `dangerouslyDisableSandbox: true` if `adb devices` comes back empty

## Behavior

1. Make sure the APK exists and is fresh:

    ```
    app/build/outputs/apk/debug/app-debug.apk
    ```

    If it is missing or older than the latest source change, run the `debug-build` skill first

2. Check the device:

    ```bash
    adb devices
    ```

    - If no device is listed, run `adb connect <ip>:<port>` when the address is known from the conversation,
      otherwise ask the user to enable wireless debugging and tell you the address
3. Install:

    ```bash
    adb install -r app/build/outputs/apk/debug/app-debug.apk
    ```

4. Report `Success` or the adb error verbatim

## After installing: the service has to be switched on by hand

The app does nothing at all until its accessibility service is enabled, and **only the user can enable it** --
there is no adb command for it that works without root, and `settings put secure enabled_accessibility_services`
needs WRITE_SECURE_SETTINGS. Tell the user to:

1. Open the app and press ユーザー補助の設定を開く (or 設定 → ユーザー補助 → OnlyScreenshot debug)
2. If the app is **not listed, or the toggle is greyed out**, this is Android's restricted-setting block on
   sideloaded apps: 設定 → アプリ → OnlyScreenshot debug → ⋮ → 制限付き設定を許可, then enable it again

To check whether it is on:

```bash
adb shell settings get secure enabled_accessibility_services
```

## Notes

- To launch the settings screen:
  `adb shell am start -n io.github.aiya000.onlyscreenshot.debug/io.github.aiya000.onlyscreenshot.MainActivity`
  -- the application id carries the `.debug` suffix, the activity class does not
- The hot zone sits over the status bar, so a screenshot taken with `adb exec-out screencap` shows it only when
  領域を表示する is on
- On a foldable the device has several displays, and `screencap` without `-d` warns and picks an arbitrary one.
  List the display ids with `adb shell dumpsys SurfaceFlinger --display-id` and pass the active one:
  `adb exec-out screencap -p -d <display-id> > shot.png`
- Never install while the user has asked to wait ("インストールは待って") -- build only

# Asd app

Asd app is a tablet-first Android app for picture, word, and sound association. It is designed around a calm landscape layout for neurodivergent children: a compact panel list on the left and a large 2x2 picture grid on the right.

## MVP features

- Tablet landscape layout with a compact left panel and large main picture area.
- Editable panels with emoji icons and names.
- Four large card slots per panel, with empty placeholders allowed.
- Bordered photo area so each picture panel is visually distinct.
- Text label below each photo.
- Red `Play` audio button visible in child mode.
- Edit-mode image positioning: drag to pan, pinch to zoom, or use fine-tune buttons.
- Edit mode for caregivers to add/change pictures, labels, and recorded audio.
- Local-only storage using on-device preferences and files.
- Content is padded below the Android status bar so the app title and edit controls do not overlap system icons.

## Beginner installation guide

### If you only have a tablet

You do **not** need a PC just to install the app, but you do need an APK file first. An APK is the Android installer file, similar to an `.exe` installer on Windows or an app package on other systems.

Because this project is source code, the app must be **built** into an APK before a tablet can install it. With only a tablet, the practical options are:

1. **Easiest:** get a prebuilt `app-debug.apk` file from the developer or from this repository's GitHub Actions build.
2. **If this code is on GitHub:** use the **Build debug APK** workflow in the GitHub website, then download the APK artifact on your tablet.
3. **If someone else has a computer:** ask them to open the project in Android Studio, build the APK, and send `app-debug.apk` to you.

Once you have the APK file on your tablet, you can install it directly from the tablet. See **Install the APK directly on your tablet** below.

### Tablet-only path using GitHub Actions

Use these steps if the project is available in a GitHub repository and you are using only your tablet:

1. Open the GitHub repository in your tablet browser.
2. Tap the **Actions** tab.
3. Tap **Build debug APK**.
4. Tap **Run workflow**.
5. Wait until the workflow finishes with a green check mark.
6. Open the finished workflow run.
7. Scroll to **Artifacts**.
8. Download `picture-sound-panels-debug-apk`.
9. If the download is a `.zip` file, open it with the tablet's Files app and extract it.
10. Find `app-debug.apk`.
11. Tap `app-debug.apk` to install it.

GitHub artifact downloads may expire after some time. If the artifact is gone, run the workflow again.

### Install the APK directly on your tablet

After you have `app-debug.apk` on the tablet:

1. Open the **Files** app on the tablet.
2. Go to **Downloads** or wherever the APK was saved.
3. Tap `app-debug.apk`.
4. Android may say that installing unknown apps is blocked. Tap **Settings** if prompted.
5. Allow the current app, usually **Files**, **Chrome**, or your browser, to **Install unknown apps**.
6. Go back to the APK and tap it again.
7. Tap **Install**.
8. Tap **Open** when installation finishes.

You can turn off **Install unknown apps** again after installing if you prefer.

### If you do have a computer later

If you later have access to a Windows, macOS, or Linux computer, the easiest development path is to use **Android Studio**. Android Studio is the official Android app-building program. It will download the Android tools this project needs and can install the app directly onto your tablet.

There are three computer-based ways to install the app:

1. **Recommended for beginners with a computer:** Android Studio installs it on your tablet.
2. **Share an APK from Android Studio:** build the app, send the APK to the tablet, then tap it on the tablet.
3. **Command line install:** build the app and install it with `adb`.

## What you need for the computer-based options

- A Windows, macOS, or Linux computer.
- An Android tablet.
- A USB cable that can transfer data, not just charge.
- Android Studio installed on the computer.

Download Android Studio from:

```text
https://developer.android.com/studio
```

## Computer option 1: Install with Android Studio, recommended

### Step 1: Open the project

1. Start Android Studio.
2. Choose **Open**.
3. Select this project folder, the folder containing this `README.md` file.
4. Wait for Android Studio to finish loading the project.
5. If Android Studio asks to install missing SDK, Gradle, or Android plugin components, accept the prompts.

### Step 2: Enable Developer Options on the tablet

On the Android tablet:

1. Open **Settings**.
2. Go to **About tablet**.
3. Find **Build number**.
4. Tap **Build number** seven times.
5. Android should say that Developer Options are enabled.

On some tablets, **Build number** may be under **Settings > About tablet > Software information**.

### Step 3: Enable USB debugging

On the Android tablet:

1. Open **Settings**.
2. Go to **System > Developer options**.
3. Turn on **USB debugging**.
4. If Android shows a warning, accept it.

The exact menu name can vary by tablet brand. Search Settings for `USB debugging` if you cannot find it.

### Step 4: Connect the tablet

1. Connect the tablet to the computer with USB.
2. Unlock the tablet screen.
3. If the tablet asks to **Allow USB debugging**, tap **Allow**.
4. If the tablet asks what USB mode to use, choose **File transfer** or **MTP**.

### Step 5: Run the app

In Android Studio:

1. Look near the top toolbar for the device selector.
2. Select your connected tablet.
3. Press the green **Run** button.
4. Wait for Android Studio to build and install the app.
5. The app should open on the tablet automatically.

If the tablet does not appear in Android Studio, unplug and reconnect the USB cable, unlock the tablet, and check again for the **Allow USB debugging** prompt.

## Computer option 2: Build an APK and install it manually

Use this option if you want Android Studio to create an APK file that you can send to the tablet yourself. This is helpful if the tablet is not nearby, or if you want to share a test build with someone else.

Important note: a debug APK is for personal testing only. If Android Studio asks whether you want to create a signed release build, you can skip that for now and use the debug APK while you are testing.

### Step 1: Build the APK

From Android Studio:

1. Open the project.
2. Choose **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
3. Wait for the build to finish.
4. Android Studio will show a notification with a **locate** link. Click it to find the APK.

Or from a terminal in the project folder, run:

```bash
gradle assembleDebug
```

The debug APK will be created at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

### Step 2: Copy the APK to the tablet

You can move the APK to the tablet in any convenient way. The simplest choices are USB file transfer, Google Drive, Dropbox, email, or another file-sharing app.

For USB transfer:

1. Connect the tablet to the computer with USB.
2. Choose **File transfer** or **MTP** on the tablet if prompted.
3. Copy this file to the tablet, for example into the **Downloads** folder:

```text
app/build/outputs/apk/debug/app-debug.apk
```

### Step 3: Install the APK on the tablet

Follow the tablet-only **Install the APK directly on your tablet** steps above.

## Computer option 3: Install with ADB from the command line

This option is useful if you are comfortable using a terminal.

### Step 1: Build the debug APK

From the project root, run:

```bash
gradle assembleDebug
```

### Step 2: Confirm the tablet is connected

Make sure USB debugging is enabled, then run:

```bash
adb devices
```

You should see your tablet listed. If it says `unauthorized`, unlock the tablet and accept the **Allow USB debugging** prompt.

### Step 3: Install the APK

Run:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The `-r` means reinstall/update the app if it is already installed.

## How to run the debug APK

A debug APK is a test build of the app. To run it on your tablet, you first install it, then open it like any other Android app.

### Run it directly on the tablet

Use these steps after you have downloaded or received `app-debug.apk`:

1. Open the tablet's **Files** app.
2. Go to **Downloads** or the folder containing `app-debug.apk`.
3. Tap `app-debug.apk`.
4. If Android asks for permission to install unknown apps, tap **Settings**.
5. Turn on **Allow from this source** or **Install unknown apps** for the app you are using, usually **Files**, **Chrome**, or your browser.
6. Go back to `app-debug.apk` and tap it again.
7. Tap **Install**.
8. When installation finishes, tap **Open**.
9. Later, you can open it from the tablet's app drawer as **Asd app**.

If Android says the app is unsafe because it is from outside the Play Store, that is expected for a debug APK. Only install APKs you built yourself or received from someone you trust.

### Run it from Android Studio

If you later use a computer with Android Studio, you usually do not need to manually tap the APK. Connect the tablet, select it in Android Studio, and press the green **Run** button. Android Studio builds, installs, and opens the debug app automatically.

### Run it with ADB

If you have a computer with Android platform tools, install the debug APK with:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Then open **Asd app** on the tablet, or launch it from ADB with:

```bash
adb shell monkey -p com.example.picturesoundpanels 1
```

## First-time app setup on the tablet

1. Open **Asd app**.
2. Rotate the tablet to landscape. The app is designed for landscape use.
3. Tap **Edit** in the top-right area.
4. Tap a card placeholder.
5. Type the word or short phrase, for example `Apple`.
6. Tap **Add picture** and choose a photo from the tablet.
7. Tap **Record audio** and allow microphone permission when Android asks.
8. Speak the word or phrase, then tap **Stop**.
9. Tap **Save**.
10. Tap **Done** to return to child mode.
11. If the picture needs adjustment, stay in **Edit** mode and drag the picture to move it, pinch to zoom, or use the card editor buttons.
12. Tap the card or the red **Play** button to hear the recording.

## Troubleshooting

### Android Studio cannot see my tablet

- Make sure the tablet is unlocked.
- Try a different USB cable. Some cables only charge and do not transfer data.
- Turn on **USB debugging** in Developer Options.
- Look for an **Allow USB debugging** prompt on the tablet.
- Try changing the USB mode to **File transfer** or **MTP**.

### The APK will not install manually

- Make sure you tapped the APK file on the tablet, not on the computer.
- Allow **Install unknown apps** when Android asks.
- If an older version is already installed and Android refuses to install, uninstall the old app first and try again.

### Audio recording does not work

- When Android asks for microphone permission, choose **Allow**.
- If permission was denied, open **Settings > Apps > Asd app > Permissions** and allow **Microphone**.

### Photos do not appear later

- Use the app's **Add picture** button so Android grants the app permission to keep reading that selected photo.
- If a photo was moved or deleted from the tablet, select the photo again in edit mode.

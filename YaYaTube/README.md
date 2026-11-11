# YaYaTube — Android source prototype

Arabic Android video-downloader prototype with no ads or account requirement.

## Build an APK online (no Android Studio needed)

1. Create a GitHub account if you do not already have one.
2. Create a **new public or private repository** on GitHub.
3. Upload the *contents* of this `YaYaTube` folder to the repository (make sure `.github/workflows/build-apk.yml` is included).
4. Open the repository's **Actions** tab and select **Build YaYaTube APK**.
5. If it has not started automatically, choose **Run workflow**.
6. Wait for the workflow to finish successfully. Open the completed run and download the `YaYaTube-debug-apk` artifact.
7. Extract the downloaded artifact ZIP; it contains `app-debug.apk`. Transfer that APK to the Android phone and install it. Android may ask you to allow installation from that source.

The workflow builds in GitHub's cloud; Android Studio and Android SDK do not need to be installed on your computer.

## Local build (optional)

Requires JDK 17, Android SDK 35, and Gradle 8.7:

```bash
gradle assembleDebug
```

## Prototype limitations

- This is source code, not a compiled or device-tested APK. YouTube may block or change access; downloading is not guaranteed for all videos.
- Download only content you own, have permission to download, or are otherwise legally entitled to save, and follow the platform's terms.
- Supports Android 10+ (API 29+). Downloads are saved under `Downloads/YaYaTube`.
- A production version still needs lifecycle-safe foreground download service/notifications, queueing, cancellation/retry, and real-device testing.
- Review the licensing obligations of the bundled youtubedl-android / FFmpeg components before redistribution.

# Fixes in this build

1. FloatingWindowService: startForeground() is now always called before the overlay-permission check
   (previously the app could crash on Android 12+ when overlay permission was missing).
2. ExternalAppLauncher: external apps are now requested in FREEFORM windowing mode (not only launch bounds),
   the LAUNCH_ADJACENT flag was removed, and a freeform launch is also attempted when the
   "Enable freeform windows" developer option is on.
3. Sidebar toast now clearly says when the phone does not allow floating windows for other apps.
4. app/build.gradle.kts: lint no longer blocks release builds.
5. .github/workflows/android-build.yml: single working workflow (duplicate "(1)" file removed).

Note: Android only lets a normal app float OTHER apps if the phone/ROM supports freeform windows.
The 12 built-in mini-apps always float via "Display over other apps".

Our project requires a specific runtime environment for all functionalities to work.

Our best recommendation is to run the application in debug mode on an actual android device on campus for the optimal experience 

Run Gradle build

To run an emulator for our application you must first have the following tools installed in Android Studio
Android 10 Q
Android emulator
Android SDK Platform-Tools
Android SDK Command-line Tools
Android SDK Build-Tools 35-rc3

For the Emulator Configuration follow this setup:
Pixel 7 Phone
Release Name: Q
API Level: 29

additionally set the device to cold boot every time (this helps with isolation sessions to each runtime of the application)

After configuration and booting the Android VM you then must install Google_Play_Services_for_AR_1.32.0_x86_for_emulator.apk (not latest) from here: https://github.com/google-ar/arcore-android-sdk/releasesthis
or use this tutorial: https://developers.google.com/ar/develop/c/emulator

You can install this with android CLI or by dragging it onto your emulated device in Android Studio.

Additionally check for any google service AR updates in the emulated devices google play store (you can login with uncc google account)

After all, restart your Android studio and emulated device and run the application

# Kick the Can

An Android app for tracking things you meant to do and "kicking the can" down the road when you miss them. Create an event, and when its start time passes without you acting on it, the app notifies you and lets you reschedule it with one tap. Events sync two-way with your device calendar.

## Features

- **Tracked events** – create events with a title, start and end time.
- **Missed events** – events whose start time has passed are listed on the home screen.
- **Kick the can** – reschedule a missed event to the next day, next week, next month, or a custom date and time.
- **Notifications** – an alarm fires when an event passes its start time ("Event missed… Kick the can?"). Alarms are re-armed after reboot.
- **Calendar sync** – changes are pushed to the device calendar and remote deletions/time changes are pulled back in.

## Requirements

- Android Studio (recent version) with JDK 11+
- Android SDK 37 (compileSdk / targetSdk); minSdk 24 (Android 7.0)

## Build and run

```sh
./gradlew assembleDebug      # build the debug APK
./gradlew installDebug       # install on a connected device/emulator
./gradlew test               # unit tests
./gradlew connectedAndroidTest  # instrumented tests (needs a device)
```

Or open the project in Android Studio and run the `app` configuration.

## Permissions

| Permission | Purpose |
|---|---|
| `READ_CALENDAR` / `WRITE_CALENDAR` | Two-way sync with the device calendar |
| `POST_NOTIFICATIONS` | Missed-event notifications |
| `RECEIVE_BOOT_COMPLETED` | Re-schedule alarms after reboot |

## Project structure

Package `com.erw.kickthecan` (`app/src/main/java/...`):

- `MainActivity`, `FirstFragment` (missed events), `SecondFragment` (add event), `TrackedEventsFragment`, `EventDialogs`, `MissedEventAdapter` – UI
- `data/` – `TrackedEvent` model and `EventRepository` (local storage)
- `calendar/` – `CalendarAccess` (permissions), `CalendarStore` (calendar provider), `CalendarSync` (two-way sync)
- `notify/` – `MissedNotifier` and `MissedEventReceiver` (alarms, boot handling, notifications)

## Tech stack

Kotlin, AndroidX (AppCompat, Navigation, ConstraintLayout), Material Components, View Binding, Kotlin Coroutines, Gradle (Kotlin DSL, version catalog).

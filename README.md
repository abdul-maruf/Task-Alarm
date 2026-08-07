# TaskAlarm — To-Do & Deadline Alarm App

A modern Android to-do app (Kotlin + Jetpack Compose) that alarms you at a task's
deadline and then keeps re-notifying you **up to 4 times** if you miss it,
until you mark the task done, snooze it, or mute it.

## How the missed-deadline system works

1. When you save a task, `AlarmScheduler` schedules an exact `AlarmManager` alarm
   for the deadline (`AlarmReceiver`).
2. At the deadline, `AlarmReceiver` fires a "Deadline reached" notification and
   kicks off `MissedDeadlineWorker` (WorkManager).
3. `MissedDeadlineWorker` checks whether the task is still incomplete, fires a
   "Still overdue" notification, then re-enqueues itself for the next reminder —
   up to 4 total — using the intervals you set in **Settings** (default: 15 min,
   1 hr, 3 hr, 1 day after the deadline).
4. Tapping **Mark Done** or **Snooze** on a notification cancels the remaining chain.
5. `BootReceiver` re-schedules everything after a phone restart, since Android
   wipes `AlarmManager` alarms on reboot.

## Project structure

```
app/src/main/java/com/marufapps/taskalarm/
├── data/            Room entity, DAO, database, repository, DataStore settings
├── alarm/            AlarmScheduler, AlarmReceiver, MissedDeadlineWorker, BootReceiver
├── notification/     NotificationHelper, NotificationActionReceiver
├── di/               Hilt modules
├── viewmodel/        TaskViewModel
└── ui/
    ├── theme/        Material 3 theme, colors, typography
    ├── components/   TaskItem card
    ├── screens/       Home, AddEditTask, TaskDetail, Settings
    └── navigation/    Nav graph
```

## Setup (Android Studio, if you have a PC)

1. Open the `TaskAlarmApp` folder in **Android Studio (Koala or newer)**.
2. Let Gradle sync (wrapper is already included, so this should just work).
3. Run on a device/emulator with **API 26+**.
4. On first launch, the app will ask for:
   - Notification permission (Android 13+)
   - "Allow exact alarms" (Android 12+) — required for on-time deadline alerts

## Getting an installable APK without Android Studio (GitHub Actions)

This project includes `.github/workflows/build.yml`, which auto-builds a debug
APK every time you push to `main`/`master`, and also lets you trigger it manually.

1. Create a new GitHub repo and push this project to it:
   ```
   cd TaskAlarmApp
   git init
   git add .
   git commit -m "Initial commit"
   git branch -M main
   git remote add origin https://github.com/<your-username>/<repo-name>.git
   git push -u origin main
   ```
2. Go to your repo's **Actions** tab — the "Build APK" workflow will run automatically.
3. Once it finishes (a few minutes), open the completed run and download the
   **TaskAlarm-debug-apk** artifact (a zip containing `app-debug.apk`).
4. Transfer that APK to your phone (email it to yourself, Google Drive, USB, etc.),
   then tap it to install. You'll need to allow "Install unknown apps" for
   whichever app you open it from (Settings → Apps → Special access, on most
   Android versions this prompts automatically the first time).

This produces a **debug build** — fine for personal use and testing. It is not
signed for the Play Store; that requires a release signing key, which is a
separate step if you ever want to publish it.

## Gradle wrapper

The real `gradlew` / `gradlew.bat` / `gradle-wrapper.jar` (Gradle 8.7) are
included, so both Android Studio and the GitHub Actions workflow can build
immediately — no manual wrapper regeneration needed.

## Customizing

- **Reminder intervals**: Settings screen lets you change the 4 missed-reminder
  timings (in minutes after the deadline) and the snooze duration.
- **Colors**: edit `ui/theme/Color.kt` — `StatusMissed`, `StatusDueSoon`, etc.
- **Typography**: `ui/theme/Type.kt`.
- **Notification channels**: users can further customize sound/vibration per
  channel from Android's system settings ("Task Deadlines" / "Missed Deadlines").

## Known limitations / next steps

- No cloud sync — tasks are local-only (Room DB on-device).
- No recurring/repeating tasks yet (each task is a single deadline).
- Written and structured carefully, but not compiled in this environment
  (no Android SDK/emulator available here) — do a build/sync pass in Android
  Studio and fix any last-mile import or Gradle version mismatches it flags.
- Consider adding: recurring tasks, categories as a filter chip row on Home,
  a widget, and Do Not Disturb-aware notification timing.

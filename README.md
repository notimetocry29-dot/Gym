# Gym Tracker

An Android app for planning and logging gym workouts. It works offline and keeps all data on your phone.

## Install

1. On your phone, open the repository's **Releases** page and pick **Gym Tracker (latest build)**.
2. Download `GymTracker.apk` and open it. Android will ask you to allow installing from your browser/files app. Allow it once.
3. Every push builds a new APK. Installing a newer one updates the app in place and keeps your data, because every build is signed with the same key (`app/gym.keystore`).

## What it does

- **Exercise library**: about 105 exercises with target muscle, equipment and step-by-step instructions. You can add your own.
- **Workout days**: ready-made Upper A/B, Lower A/B, Push/Pull/Legs A/B and Full Body A/B/C. Each exercise has a target (sets × rep range) that you can edit. You can also create your own days.
- **Programs**: start from a template (Upper/Lower, PPL, Full Body, a PPL + Upper/Lower hybrid, or 4 weeks U/L then 4 weeks PPL) or from a blank program.
  - Set a start date. Each phase runs for a set number of weeks, and the app shows the dates.
  - A phase's rotation can mix days from any split. Days repeat in order, whatever weekday you train.
  - **Today** shows which workout is next. You can skip it or do a different day instead.
- **Logging**: weight × reps for each set, plus notes per exercise and per workout. Next to each set you see what you did last time.
- **Weight suggestions**: if you hit the top of the rep range on every set last time, the app suggests +2.5 kg (upper body) or +5 kg (lower body). Otherwise it suggests the same weight. You can change the step for each exercise.
- **History**, with repeat and edit.
- **Progress**: estimated 1RM, top weight, volume and rep charts, plus personal records.
- **Body weight** log with a chart.
- **Backup**: Settings → Export/Restore a JSON file (for example to Google Drive).

## Build

```
./gradlew testDebugUnitTest assembleRelease
```

Tech: Kotlin, Jetpack Compose (Material 3), Room. `AppFlowTest` runs the real app in Robolectric and saves screenshots to `app/build/screens`.

# QZen Timer

Android-Timer-App mit Kotlin und Jetpack Compose (Material 3). Drei Modi:

- **Manuell:** Zeit über einen runden Regler einstellen und starten
- **Pomodoro:** Arbeits- und Pausenphasen in 4 Zyklen, nach dem vierten folgt die lange Pause
- **Tabata:** Vorbereitung, Arbeit und Pause in 8 Zyklen, die Dauer der Phasen ist einstellbar

Die Oberfläche ist auf Deutsch (`app/src/main/res/values/strings.xml`).

## Bauen

Voraussetzung: Android Studio oder JDK 17 und das Android SDK (minSdk 26, targetSdk 35).

```bash
./gradlew assembleDebug
```

Die APK liegt danach unter `app/build/outputs/apk/debug/`.

## Aufbau

- `app/src/main/java/com/example/qzentimer/MainActivity.kt`: Oberfläche und Timer-Logik (`TimerScreen`, `CircularTimerPicker`)
- `app/src/main/java/com/example/qzentimer/ui/theme/`: Farben, Typografie und Theme

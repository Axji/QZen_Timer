# QZen Timer

Android-Timer-App mit Kotlin und Jetpack Compose (Material 3). Drei Modi:

- **Manuell:** Zeit über einen runden Regler einstellen (Drehen = Minuten), Tippen auf die Zeit startet und pausiert
- **Pomodoro:** 25 Minuten Arbeit, 5 Minuten Pause, nach dem vierten Zyklus 20 Minuten lange Pause
- **Tabata:** 10 Sekunden Vorbereitung, dann 8 Zyklen Arbeit und Pause (je 20 und 10 Sekunden, einstellbar). In den letzten drei Sekunden und am Phasenende ertönt ein Signal.

Der Timer läuft bei ausgeschaltetem Bildschirm oder in einer anderen App weiter (Vordergrunddienst mit Benachrichtigung).
Während er läuft, bleibt der Bildschirm an. Der Zustand übersteht das Drehen des Geräts.
Die Oberfläche ist auf Deutsch (`app/src/main/res/values/strings.xml`).

## Bauen und testen

Voraussetzung: Android Studio oder JDK 21 und das Android SDK (compileSdk 36, minSdk 26).

```bash
./gradlew testDebugUnitTest assembleDebug
```

Die APK liegt danach unter `app/build/outputs/apk/debug/`.

## Aufbau

- `TimerEngine.kt`: reine Timer-Logik (Phasen, Zyklen, Töne) ohne Android-Abhängigkeit, getestet in `app/src/test`
- `TimerViewModel.kt`: hält den Zustand, taktet im Sekundentakt und spielt die Töne
- `TimerService.kt`: Vordergrunddienst, der den Prozess am Leben hält, solange der Timer läuft
- `TimerScreen.kt`, `CircularTimerPicker.kt`: Oberfläche
- `ui/theme/`: Farben, Typografie und Theme

## Hinweis

Der Vordergrunddienst und die Benachrichtigung sind nur gebaut, aber nicht auf einem Gerät getestet.
Auf Android 13 und neuer fragt die App beim Start nach der Erlaubnis für Benachrichtigungen.

Lizenz: MIT

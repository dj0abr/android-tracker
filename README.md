# Android APRS Tracker – erster Funktionstest

Natives Android-17-Projekt (API 37), Java 17, klassische XML-Views. Zielgerät: Google Pixel 11 Pro. Automatische GPS-Logs ausschließlich bei ausgewählter Auto-Bluetooth-Classic-Verbindung **und** kabelloser Versorgung; noch kein APRS-IS, TCP, SmartBeaconing oder Netzwerkzugriff.

## Bauen

Benötigt: Linux, vollständiges JDK 17, Android SDK Platform 37.0, Build Tools 37.0.0 und Platform Tools. Android Gradle Plugin 9.2.0 und Gradle 9.4.1 sind festgelegt. Der echte Gradle Wrapper samt JAR, Unix-/Windows-Skripten und SHA-256-Prüfung ist eingecheckt. Eine globale Gradle-Installation ist nicht nötig.

SDK-Pfad einmal lokal angeben, etwa in der nicht eingecheckten `local.properties`:

```properties
sdk.dir=/home/kurt/Android/Sdk
```

Alternativ `ANDROID_HOME` setzen. Fehlende SDK-Komponenten mit der aktuellen Android CLI (`android sdk install`) oder `sdkmanager 'platforms;android-37.0' 'build-tools;37.0.0' 'platform-tools'` installieren. SDK-Lizenzen müssen akzeptiert sein.

```sh
./gradlew assembleDebug
./gradlew testDebugUnitTest lintDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`. Für die Kompilierung wählt die Gradle-Java-Toolchain das installierte JDK 17; ein vorhandenes Java-21-JRE allein reicht nicht als Compiler.

## Einmalig einrichten

1. APK auf dem Pixel installieren und die App einmal öffnen. Die Automatik ist zunächst aus.
2. „Berechtigungen erteilen“: präzisen Standort, Geräte in der Nähe/Bluetooth und Benachrichtigungen erlauben.
3. „Standort Immer zulassen einrichten“: nach der Erklärung in den Android-App-Einstellungen unter Berechtigungen → Standort **Immer zulassen** und präzisen Standort wählen. Diese Freigabe wird separat angefordert.
4. Die Freisprecheinrichtung zunächst in Android koppeln. In der App „Auto-Bluetooth auswählen“ und die Companion-Zuordnung im Android-Systemdialog bestätigen. Das Auto muss dabei erreichbar sein; Bluetooth und Systemstandort einschalten.
5. „Automatik aktivieren“. Die App darf danach geschlossen bzw. aus der Übersicht gewischt werden. Bluetooth-Verbindung + kabellose Versorgung starten den Standort-Foreground-Service ohne Activity-Wechsel. Bei Wegfall einer Bedingung wird der GPS-Listener entfernt und der Service beendet.

Rufzeichen, APRS-Passcode und Server kommen erst mit der späteren APRS-IS-Etappe hinzu. Der Funktionstest enthält dafür bewusst noch keine wirkungslosen Felder.

## Installieren und beobachten

```sh
adb devices -l
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n de.kurt.aprstracker/.MainActivity
adb logcat -v time AprsTracker:I AndroidRuntime:E '*:S'
```

Der wiederholbare Basis-Gerätetest baut nicht selbst und verändert keine Berechtigungen oder Bluetooth-Zuordnungen:

```sh
./scripts/pixel-smoke-test.sh SERIAL
```

Er installiert die vorhandene APK, öffnet die Activity und speichert Screenshot, Logs und Servicezustand unter `app/build/device-tests/`. Für Qi-Tests drahtloses ADB verwenden oder USB vor dem Laden entfernen; parallele USB-Versorgung kann die von Android gemeldete Ladequelle verändern.

Log-Marken: `PROCESS`, `PRESENCE`, `STATE`, `GPS_START`, `GPS_STARTED`, `GPS`, `GPS_STOPPED`, `RECOVERY`. Positionszeilen enthalten Koordinaten, Genauigkeit, Geschwindigkeit, Kurs, Zeit und Mock-Kennzeichen. Sie werden nur im Debug-Build geschrieben. Keine dauerhafte Positionsdatei.

## Aufbau und Grenzen

- `TrackerController`: aktuelle Systemzustände lesen, Gate auswerten, Dienst starten/stoppen; keine gespeicherte Verbindungswahrheit.
- `CarCompanionService`: Android bindet bei Gerätepräsenz; der Prozess kann dadurch während der Auto-Verbindung Ladeereignisse empfangen.
- `SystemEventReceiver`: Wiederherstellung nach Boot/Update und Bluetooth-Ereignisse als zusätzlicher Auslöser.
- `GpsTrackingService`: vorgeschriebene leise FGS-Benachrichtigung und GPS-Updates ungefähr alle fünf Sekunden, keine Audio- oder Overlay-Funktionen.
- `TrackingGateTest`: alle 256 Kombinationen der Startbedingungen und Ladequellen/-zustände.

Kein GPS vor dem ersten Entsperren nach Boot. Nach Force-Stop, System-Stopp, entzogenen Berechtigungen oder restriktiven Systemeinstellungen kann erneutes Öffnen nötig sein. Android meldet „kabellose Stromversorgung“, keine Identität eines Qi-Laders. Ladepausen mit `NOT_CHARGING` stoppen den Test. Das Gate zählt `FULL` auf dem kabellosen Lader als versorgt.

Details: [recherchierte Android-Startregeln](docs/ANDROID_BACKGROUND_START.md), [Pixel-Testplan](docs/DEVICE_TEST.md), [tatsächlicher Prüfstand](docs/TEST_RESULTS.md), [Projektkonventionen](AGENTS.md).

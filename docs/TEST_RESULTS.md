# Prüfprotokoll – 1. November 2026

## Entwicklungsumgebung

- Projekt: `/Daten/source/android-tracker`, Linux x86_64.
- Android SDK Platform 37.0 Revision 2 und Build Tools 37.0.0 installiert.
- Android Gradle Plugin 9.2.0, Gradle Wrapper 9.4.1; SHA-256 der Gradle-Distribution geprüft und im Wrapper festgelegt.
- Gradle-Laufzeit: OpenJDK 21; Java-Compiler: installiertes vollständiges OpenJDK 17 über Java-Toolchain. Kein Kotlin-/Compose-Appcode und keine App-Laufzeitbibliotheken hinzugefügt.

## Erfolgreich ausgeführt

| Prüfung | Tatsächliches Ergebnis |
| --- | --- |
| `./gradlew assembleDebug` | BUILD SUCCESSFUL, auch als eigener Einzelbefehl |
| `./gradlew assembleDebug testDebugUnitTest lintDebug` | BUILD SUCCESSFUL |
| JUnit | 6 Tests, 0 Fehler, 0 Exceptions, 0 übersprungen |
| Gate-Abdeckung | Alle 256 Kombinationen von acht Bedingungen; zusätzlich 80 Ladequellen/-status-Kombinationen und gezielte Grenzfälle |
| Android Lint | 0 Fehler, 2 Hinweise: neuere Gradle-Version verfügbar; GPS als erforderliches Feature deklariert |
| `apksigner verify --verbose` | Signatur gültig, APK Signature Scheme v2, ein Debug-Signer |
| Gepacktes Manifest | compile/min/target API 37, Location-FGS-Typ, Companion-Start-/Presence- und Standortberechtigungen vorhanden, kein INTERNET-Recht |
| `bash -n scripts/pixel-smoke-test.sh` | Erfolgreich |
| `git diff --check` | Erfolgreich |

Die Gradle-Version bleibt bewusst zur dokumentierten AGP-Kompatibilität festgelegt. GPS ist für diesen Tracker erforderlich. AGP gibt außerdem einen Build-Hinweis zur künftigen Entfernung von `android.builtInKotlin=false` in AGP 10 aus; der Java-Build mit AGP 9.2 ist erfolgreich.

Erste APK vor der unten beschriebenen Adresskorrektur: `app/build/outputs/apk/debug/app-debug.apk`, 92241 Bytes.
SHA-256: `b392671a48b6647faa31baee3f6698264d735826ba68606babe028e947676ebf`.

## Pixel-Gerätetest per USB

Am 1. November 2026 um 23:08 Uhr (Europe/Berlin) auf dem wieder verbundenen **Google Pixel 11 Pro**, Modell `Pixel_11_Pro`, Android **17**, SDK **37** ausgeführt.

| Prüfung | Tatsächliches Ergebnis |
| --- | --- |
| APK-Installation mit `adb install -r` | `Success`; installierte APK entspricht der oben genannten Prüfsumme |
| `./scripts/pixel-smoke-test.sh` | Nach Skript-Korrektur vollständig erfolgreich, Exitcode 0 |
| Kaltstart der Activity | `Status: ok`, `LaunchState: COLD`, `TotalTime: 228 ms` |
| Prozess/Crash-Log | App-Prozess läuft; Crash-Buffer seit Start enthält keinen Tracker-Absturz |
| Oberfläche | Screenshot visuell geprüft: Einrichtungsschritte, Automatikschalter und mehrzeiliger Status korrekt sichtbar; Inhalt scrollbar |
| Tatsächliche Ladequelle | Android meldet USB=true, Wireless=false; Logcat `plugged=2 batteryStatus=2 wireless=false` |
| Frische Installation | Automatik aus, keine Auto-Zuordnung, keine Standortfreigaben; `gate=DISABLED`; kein GPS-/Companion-Service aktiv |
| Automatik ohne Auto-Zuordnung eingeschaltet | UI meldet „Auto-Zuordnung fehlt“, GPS bleibt gestoppt; kein `GpsTrackingService` in `dumpsys activity services` |
| Automatik wieder ausgeschaltet | UI meldet „Automatik ausgeschaltet“; Ausgangszustand wiederhergestellt; kein laufender Service |

Lokale, nicht eingecheckte Artefakte einschließlich Screenshot, Geräte-Fingerprint, Installations-/Startausgabe, Logcat und Gate-Prüfungen: `app/build/device-tests/20261101-230823/`.

Beim ersten Versuch nach Wiederverbinden war die APK bereits installiert, bevor das Testskript an der Datumsabfrage abbrach: ADB hatte das Datumsformat mit Leerzeichen ohne ausreichende Remote-Shell-Quotierung übergeben. Die Quotierung wurde korrigiert, `bash -n` besteht, und der anschließende vollständige Durchlauf war erfolgreich.

Zustand nach diesem Basistest: App geöffnet, Automatik aus. Berechtigungen und Auto-Zuordnung wurden nicht per ADB erzwungen. Der beobachtete inaktive Dienst ist ein Nachweis für den sicheren Anfangszustand und die fehlende Zuordnung; die physische Bluetooth-/Qi-Kombination wurde dadurch noch nicht geprüft.

## Einrichtung und Korrektur des Bluetooth-Status

Die Einrichtung wurde anschließend vom Nutzer über die Android-Oberfläche abgeschlossen. Auf dem Pixel geprüft:

- Präziser und grober Standort, Hintergrundstandort, Bluetooth und Benachrichtigungen: alle erteilt.
- Auto „Kurt BMW NA05“ über CompanionDeviceManager zugeordnet; Presence-Beobachtung aktiv, Zuordnung gültig.
- Automatik eingeschaltet und zugehörige Association-ID gespeichert.

Das Auslesen der tatsächlichen Verbindung deckte einen Fehler auf: `MacAddress.toString()` liefert eine kleingeschriebene Adresse, die `BluetoothAdapter.getRemoteDevice(String)` auf dem Pixel ablehnt. Die Adresse wird nun mit `MacAddress.toByteArray()` direkt an `getRemoteDevice(byte[])` übergeben. Die Oberfläche war zuvor trotz abgefangener Ausnahme bedienbar; die Verbindungserkennung hätte damit aber nicht funktioniert.

Nach der Korrektur erfolgreich ausgeführt: `./gradlew assembleDebug testDebugUnitTest lintDebug` (6 Tests, keine Lint-Fehler), Installation per Update und erneuter Pixel-Smoke-Test. Berechtigungen, Auto-Zuordnung und eingeschaltete Automatik blieben erhalten. Logcat bestätigt nun vollständig gelesene Systemzustände: `enabled=true associated=true bt=false wireless=false plugged=2 fine=true background=true gpsEnabled=true gate=DISCONNECTED`, ohne MAC-Adressfehler.

Zusätzlich beobachtet: `MY_PACKAGE_REPLACED` → erneute Presence-Registrierung nach dem APK-Update. Das bestätigt diesen Wiederherstellungspfad; ein echter Boot-Test steht weiterhin aus.

Artefakte: `app/build/device-tests/20261101-231336-setup/` und `app/build/device-tests/20261101-231433/`.

## Vorbereiteter Test ohne USB

Am 1. November 2026 um 23:14 Uhr (Europe/Berlin) wurde die Activity in den Hintergrund gelegt und der inaktive Tracker-Prozess mit `am kill` beendet. Es wurde kein Force-Stop verwendet. Der Prozess ist danach nicht mehr vorhanden; die gültige Companion-Zuordnung mit aktivierter Presence-Beobachtung bleibt im System bestehen. Damit ist der nächste physische Verbindungsaufbau ein Test des unbeaufsichtigten Starts aus einem beendeten App-Prozess.

Für diesen einzelnen Gerätetest läuft ein vom APK unabhängiger `logcat`-Prozess unter der ADB-Shell. Er zeichnet nur `AprsTracker` und `AndroidRuntime` in temporäre Dateien auf dem Pixel auf, auch nach Abziehen von USB. Rotation begrenzt die Dateien auf insgesamt ungefähr 3 MiB. Metadaten zum gezielten Einsammeln und Beenden: `app/build/device-tests/20261101-231456-field/recorder.json`. Nach Rückkehr die Aufzeichnung beenden, Dateien lokal auswerten und vom Gerät entfernen. Keine Koordinaten in Git übernehmen.

Testablauf: Tracker im Hintergrund lassen; im stehenden Auto Bluetooth verbinden; Pixel zwei Minuten auf Qi legen und Bildschirm sperren; für 20 Sekunden vom Lader nehmen; erneut eine Minute auflegen; vom Lader nehmen und wieder am Rechner per USB anschließen. Start, GPS-Fixes, Stoppen und erneuter Start sind erst nach Auswertung der Aufzeichnung nachgewiesen.

## Noch offene Hardware-Abnahme

Die Einrichtung einschließlich echter Auto-Zuordnung ist bestätigt. Noch offen sind die physische Verbindung zum Auto, beide Reihenfolgen von Bluetooth/Qi, echte GPS-Fixes, Start ohne sichtbare Activity/bei gesperrtem Bildschirm, Stoppen bei Wegfall einer Bedingung, Wiederanlauf nach Prozessverlust während aktiven Trackings, Neustart sowie Parallelbetrieb mit Browser/Android Auto. Benötigt werden dafür die erreichbare Freisprecheinrichtung und der Qi-Lader. Diese Punkte sind **noch nicht als erfolgreich getestet** zu betrachten. Vollständiger Ablauf: [DEVICE_TEST.md](DEVICE_TEST.md).

Aktuelle APK nach Adresskorrektur: 92297 Bytes; SHA-256: `5b5ad76ce8d6ffc980cc5970c291428ed1c146ea3e3c40079fe302108725b9b6`.

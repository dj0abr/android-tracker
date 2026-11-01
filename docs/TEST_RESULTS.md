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

## Ausgewerteter Auto-/Qi-Test ohne USB

Am 1. November 2026, Zeiten in Europe/Berlin. Der Nutzer führte den Test im Auto ohne USB-Verbindung durch und meldete Schwierigkeiten beim Positionieren auf dem Qi-Lader. Die vom Pixel aufgezeichneten Zustandswechsel sind trotzdem für den Kernfunktionstest auswertbar.

Die App war vor dem Test im Hintergrund und ihr Prozess beendet. Bei Bluetooth-Ereignissen lief sie in einem neuen Prozess wieder an; keine Tracker-Activity musste für den GPS-Start geöffnet werden. Die folgende Sequenz ist in den gesicherten Logs nachgewiesen:

| Uhrzeit | Beobachtung |
| --- | --- |
| 23:16:23.322 | Neuer App-Prozess startet bei Bluetooth-Ereignissen; ausgewähltes Auto noch nicht als Classic-Verbindung verbunden; GPS bleibt aus |
| 23:17:28.716 | BLE-Presence (`event=0`); kein GPS-Start |
| 23:17:39.736 | Classic-Verbindung des ausgewählten Autos (`event=2`); `bt=true`, ohne kabellose Ladung weiterhin kein GPS |
| 23:17:50.638 | Kabellose Ladung erkannt (`plugged=4`, `batteryStatus=2`), Gate `READY` |
| 23:17:50.684 | Location-FGS/GPS automatisch gestartet; kein abgewiesener Hintergrundstart |
| 23:17:59.848 | Erste echte GPS-Position, `mock=false`, rund 9,2 Sekunden nach Dienststart |
| 23:20:35.076 | Android meldet keine aktive Ladung mehr (`batteryStatus=4`), trotz zunächst noch gesetztem Wireless-Bit |
| 23:20:35.098 | GPS-Listener entfernt und Dienst beendet, 22 ms nach verarbeitetem Zustandswechsel |
| 23:20:37.251 | Wireless-Stromquelle ebenfalls weg (`plugged=0`); GPS bleibt aus |
| 23:21:24.651 | Erneut kabellose Ladung und Gate `READY` |
| 23:21:24.701 | Zweiter automatischer GPS-Start |
| 23:21:25.288 | Erneut echte GPS-Position, rund 0,6 Sekunden nach Dienststart |
| 23:21:46.295 | Android meldet erneut keine aktive Ladung (`batteryStatus=4`) |
| 23:21:46.312 | Zweiter GPS-Stopp mit Listener-Freigabe, nach 17 ms |

Erste GPS-Phase: **31 Positionsmeldungen in rund 2:44 Minuten**. Zweite GPS-Phase: **5 Positionsmeldungen in rund 22 Sekunden**. Insgesamt **36 echte, nicht simulierte Positionsmeldungen**, typische Folgezeit rund **5 Sekunden**. Die von Android gemeldete Genauigkeit verbessert sich in der ersten Phase von zunächst 113,8 m bis auf 9,4 m. Das ist eine Genauigkeitsschätzung des Empfängers, kein Vermessungsnachweis.

Zwischen den beiden Dienstphasen sind keine GPS-Positionszeilen aufgezeichnet. Keine Tracker-Fehler/FGS-Startausnahmen in der gesicherten Feldaufzeichnung und kein Tracker-Absturz im ausgelesenen Crash-Buffer. Nach Rückkehr am USB-Kabel sind weder Standort- noch Companion-Service aktiv; die Automatik und Presence-Zuordnung bleiben eingerichtet.

Damit sind der **erste Hintergrundstart nach zuvor beendetem Prozess**, **Bluetooth-Classic-Erkennung**, **Abgrenzung gegenüber bloßer BLE-Nähe**, **kabellose Ladeerkennung**, **echte GPS-Updates**, **Stoppen bei Ladeunterbrechung** und **erneuter automatischer Start** auf dem Pixel nachgewiesen. Die Ursache der Ladeunterbrechungen selbst lässt sich aus `BatteryManager` nicht bestimmen; die App reagiert auf den vom System gemeldeten Zustand.

Artefakte: `app/build/device-tests/20261101-231456-field/` mit Rohaufzeichnung, `summary.json`, Prüfsummen und abschließenden Systemzuständen. Die 8-KiB-Aufzeichnung enthält eine unvollständige letzte Zeile um 23:22:02; alle oben genannten Start-/Stoppereignisse und 36 Positionen sind vollständig enthalten. Ein späterer Bluetooth-Trennzeitpunkt ist daraus nicht nachgewiesen. Nach Sicherung wurden die Geräte- und lokalen Prüfsummen verglichen, der temporäre Recorder beendet und seine Datei vom Pixel entfernt. Die Rohdatei mit Koordinaten bleibt ausschließlich lokal und wird nicht eingecheckt.

## Noch offene Hardware-Abnahme

Der beschriebene erste Auto-/Qi-Kernfunktionstest ist bestanden. Noch separat zu prüfen sind die umgekehrte Startreihenfolge (zuerst Qi, dann Auto-Verbindung), Bluetooth-Trennung bei weiter aktiver Qi-Ladung, Wiederanlauf nach Prozessverlust während aktiven Trackings, ein echter Geräteneustart, Langzeitbetrieb, die übrigen Berechtigungs-/Ladezustands-Grenzfälle sowie Parallelbetrieb mit Browser/Android Auto. Der Hintergrundstart wurde mit zuvor beendeter App getestet; der Sperrzustand des Bildschirms wurde im Logger nicht separat erfasst. Diese zusätzlichen Punkte sind **noch nicht als erfolgreich getestet** zu betrachten. Vollständiger Ablauf: [DEVICE_TEST.md](DEVICE_TEST.md).

Aktuelle APK nach Adresskorrektur: 92297 Bytes; SHA-256: `5b5ad76ce8d6ffc980cc5970c291428ed1c146ea3e3c40079fe302108725b9b6`.

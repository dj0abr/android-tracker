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

APK: `app/build/outputs/apk/debug/app-debug.apk`, 92241 Bytes.
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

Die App bleibt geöffnet, die Automatik ist aus. Berechtigungen und Auto-Zuordnung wurden nicht per ADB erzwungen. Der beobachtete inaktive Dienst ist ein Nachweis für den sicheren Anfangszustand und die fehlende Zuordnung; die physische Bluetooth-/Qi-Kombination wurde dadurch noch nicht geprüft.

## Noch offene Hardware-Abnahme

Offen sind die Einrichtung über die Android-Berechtigungs-/Companion-Dialoge, die echte Auto-Zuordnung, beide Reihenfolgen von Bluetooth/Qi, echte GPS-Fixes, Start ohne sichtbare Activity/bei gesperrtem Bildschirm, Stoppen bei Wegfall einer Bedingung, Prozessverlust, Neustart sowie Parallelbetrieb mit Browser/Android Auto. Benötigt werden dafür die erreichbare Freisprecheinrichtung und der Qi-Lader. Diese Punkte sind **noch nicht als erfolgreich getestet** zu betrachten. Vollständiger Ablauf: [DEVICE_TEST.md](DEVICE_TEST.md).

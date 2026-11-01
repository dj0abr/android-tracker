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

## Pixel-Verbindung und offene Abnahme

Zu Beginn per ADB tatsächlich erkannt: **Google Pixel 11 Pro**, Modell `Pixel_11_Pro`, Android **17**, SDK **37**. Vor dem ersten abgeschlossenen APK-Build war dieses Gerät nicht mehr in `adb devices -l` aufgeführt.

`./scripts/pixel-smoke-test.sh` wurde versucht und endete mit Exitcode 2: kein verbundenes ADB-Gerät. Somit wurden die neue APK **noch nicht auf dem Pixel installiert** und die Activity noch nicht auf dem Gerät gestartet.

Weiter offen sind die physische Auto-Zuordnung, beide Reihenfolgen von Bluetooth/Qi, echte GPS-Fixes, Start ohne sichtbare Activity/bei gesperrtem Bildschirm, Stoppen bei Wegfall einer Bedingung, Prozessverlust, Neustart sowie störungsfreier Parallelbetrieb mit Browser/Android Auto. Dafür fehlen aktuell eine ADB-Verbindung und die erreichbare Freisprecheinrichtung samt Qi-Lader. Diese Punkte sind **nicht als erfolgreich getestet** zu betrachten. Vollständiger Ablauf: [DEVICE_TEST.md](DEVICE_TEST.md).

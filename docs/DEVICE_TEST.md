# Pixel 11 Pro: Funktionstest

## Vorbereitung

`./gradlew assembleDebug testDebugUnitTest lintDebug`, dann `./scripts/pixel-smoke-test.sh SERIAL`. Der Basis-Smoke-Test prüft Installation, Activity-Start und Absturzfreiheit; er ersetzt keine physische Bluetooth-/Qi-Prüfung. Anschließend die Einrichtung gemäß README durchführen. Keine Standortberechtigungen per ADB erzwingen, damit auch der tatsächliche Einrichtungsablauf geprüft wird.

Zum Mitschneiden: `adb logcat -v time AprsTracker:I AndroidRuntime:E '*:S'`. Physische Fahrtprüfung nur mit gesicherter Bedienung durch einen Beifahrer oder im Stand. Für Qi möglichst Wireless Debugging nutzen. Bei USB-Verbindung zusätzlich den gemeldeten `plugged`-Wert beobachten.

## Abnahmematrix

| Situation/Aktion | Erwartung |
| --- | --- |
| Neu installiert, keine Berechtigungen | UI erreichbar, kein GPS/FGS, keine Ausnahme |
| Automatik aus, Auto + Qi vorhanden | `gate=DISABLED`, GPS bleibt aus |
| Auto zugeordnet, präziser Standort erlaubt, Hintergrundstandort fehlt | Kein Hintergrund-GPS; Diagnose benennt fehlende Freigabe, sobald Auto + Qi vorliegen |
| Auto verbunden, kein Lader | `bt=true wireless=false`, kein GPS-FGS |
| Qi aktiv, Auto getrennt | `bt=false wireless=true`, kein GPS-FGS |
| Erst Auto verbinden, danach Qi auflegen | `READY` → `GPS_START` → `GPS_STARTED` → echte `GPS`-Positionen |
| Erst Qi auflegen, danach Auto verbinden | Derselbe automatische Start ohne Öffnen der App |
| Beliebiges anderes Bluetooth-Gerät verbinden | Kein Start, solange das ausgewählte Auto getrennt ist |
| Auto nur in BLE-Reichweite | Ohne Classic-Verbindung kein Start |
| Qi entfernen bei verbundenem Auto | `NOT_WIRELESS`, `GPS_STOPPED`, danach keine Positionszeilen |
| Auto trennen bei weiter aktivem Qi | `DISCONNECTED`, `GPS_STOPPED`, danach keine Positionszeilen |
| USB/AC statt Qi | Kein GPS, sofern Android keine zusätzliche kabellose Versorgung meldet |
| Akku voll auf Qi | GPS läuft weiter bei `batteryStatus=5` und Wireless-Bit |
| Ladepause/Übertemperatur meldet `NOT_CHARGING` | GPS stoppt; bei wieder einsetzender Ladung automatischer Neustart |
| Standort/GPS abschalten | GPS stoppt; erneutes Einschalten bei Auto + Qi startet wieder |
| Automatik während Tracking ausschalten | GPS und FGS-Benachrichtigung verschwinden |
| App aus der Übersicht wischen, Bildschirm sperren | Bedingungen steuern weiter den Dienst; kein erneutes App-Öffnen nötig |
| Browser/Android Auto im Vordergrund, Start/Stop auslösen | Vordergrund-App bleibt sichtbar, kein Ton, kein Audio-Fokuswechsel |
| Prozessverlust während Auto + Qi | System-Wiederanlauf: frische Zustände, genau ein GPS-Listener; `stickyRestart=true` möglich |
| Neustart, erstes Entsperren, Auto + Qi | `RECOVERY`, Presence-Beobachtung und GPS starten ohne Öffnen der Tracker-Activity |
| Neustart außerhalb Auto | Kein dauerhafter Dienst, keine GPS-Anforderung |
| Force-Stop/System-Stopp | Kein erzwungener Wiederanlauf; App bei Bedarf manuell öffnen |
| Bluetooth-/Standortberechtigung entziehen | GPS endet; keine Crash-Schleife; UI zeigt fehlende Berechtigung |

## Nachweis

Für jeden Versuch Android-Build, Uhrzeit, Ausgangsbedingungen, STATE-/PRESENCE-/GPS-Zeilen und Ergebnis festhalten. Echte Koordinaten nicht in Git einchecken. `adb shell dumpsys activity services de.kurt.aprstracker` zeigt Servicezustände. `adb shell dumpsys companiondevice` zeigt die Zuordnung/Beobachtung (Ausgabe lokal halten).

Für Prozessverlust im Debug-Build kann der eigene App-Prozess gezielt per `run-as` beendet werden. `am force-stop` ist dafür ungeeignet, weil es den expliziten Nutzer-Stopp simuliert. Neustart und Bluetooth-Umschalten erst in einer passenden Testsituation durchführen; der Smoke-Test führt sie nicht automatisch aus.

Wenn die App während Auto-Verbindung ohne Qi keinen Prozess mehr hat, besonders die Systembindung des Companion-Service prüfen. Ein nur manifestregistrierter Power-Receiver wäre keine verlässliche Lösung. Keinen dauerhaften Platzhalter-FGS hinzufügen, um den Fehler zu verdecken.

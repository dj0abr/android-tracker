# Hintergrundstart auf Android 17 / API 37

Geprüft am 1. November 2026 anhand der offiziellen Android-Dokumentation.

## Entscheidung vor der Implementierung

Es gibt zwei voneinander unabhängige Schranken:

1. **FGS aus dem Hintergrund starten:** Ab API 31 braucht die App eine dokumentierte Ausnahme. Eine vom Nutzer bestätigte CompanionDeviceManager-Zuordnung zusammen mit `REQUEST_COMPANION_START_FOREGROUND_SERVICES_FROM_BACKGROUND` ist eine solche Ausnahme. Nur `ACCESS_BACKGROUND_LOCATION` oder ein Lade-Broadcast genügt dafür nicht.
2. **Standort im so gestarteten FGS verwenden:** Der Dienst deklariert `foregroundServiceType="location"`, `FOREGROUND_SERVICE` und `FOREGROUND_SERVICE_LOCATION`. Für den unbeaufsichtigten Hintergrundstart verlangt dieser Prototyp zusätzlich `ACCESS_FINE_LOCATION` und `ACCESS_BACKGROUND_LOCATION`. Standortdienste/GPS müssen eingeschaltet sein. Die Companion-Ausnahme ersetzt die Standortberechtigungen nicht.

Die App fordert zunächst groben und genauen Vordergrundstandort gemeinsam an. „Immer zulassen“ wird anschließend separat und mit Erklärung in den Android-App-Einstellungen eingerichtet. Eine einmalige Einrichtung einschließlich Bluetooth-Zuordnung ist unvermeidbar. Eine frisch installierte, noch nie geöffnete App kann sich nicht selbst einrichten.

## Companion Device Presence

Verwendung der aktuellen API ab API 36: `startObservingDevicePresence(ObservingDevicePresenceRequest)` mit Association-ID und `CompanionDeviceService.onDevicePresenceEvent(DevicePresenceEvent)`. Die ältere String-/MAC-API und `onDeviceAppeared` sind veraltet.

Bei Bluetooth Classic meldet Android Verbindungsaufbau und Trennung; bei BLE kann schon Funknähe ein Ereignis auslösen. Daher prüft das Gate immer die tatsächliche Classic-Verbindung über die öffentliche API `BluetoothDevice.isConnected(TRANSPORT_BREDR)` (ab API 36.1, auf API 37 vorhanden). BLE-Nähe startet GPS nicht. Die Zuordnung erzeugt keine Verbindung und übernimmt weder HFP noch Audio-Routing. Das Auto muss in Android normal gekoppelt und verbunden sein.

Während das Companion-Gerät verbunden ist, bindet Android den Companion-Service. Im App-Prozess registrierte Battery-/Bluetooth-/Location-Receiver bewerten die Bedingungen bei Zustandsänderungen neu. So funktioniert sowohl „erst Bluetooth, dann Qi“ als auch die umgekehrte Reihenfolge. Außerhalb des Autos gibt es keinen dauerhaften FGS, keine eigenen Bluetooth-Scans, keine GPS-Abfragen, Timer, Jobs oder Wake Locks.

## Qi und Lifecycle

`ACTION_BATTERY_CHANGED` ist ein sticky, dynamisch empfangener System-Broadcast. `EXTRA_PLUGGED & BATTERY_PLUGGED_WIRELESS` kennzeichnet kabellose Versorgung, nicht das Qi-Protokoll oder die Identität des Ladegeräts. Zusammen mit der ausgewählten Auto-Verbindung wird dies als „im Auto kabellos versorgt“ interpretiert. `CHARGING` und `FULL` zählen; USB/AC allein und `NOT_CHARGING` nicht. Thermische Ladepausen können damit GPS stoppen und später wieder starten. Keine Behauptung, Android könne einen bestimmten Qi-Lader identifizieren.

`BOOT_COMPLETED` (nach dem ersten Entsperren) und `MY_PACKAGE_REPLACED` stellen Presence-Beobachtung wieder her und lesen aktuelle Zustände. Kein GPS vor dem Entsperren, kein Direct Boot. Ein während einer Fahrt vom System beendeter Standortdienst nutzt `START_STICKY` und prüft beim Wiederanlauf alle Bedingungen neu. Es wird kein Verbindungszustand dauerhaft gespeichert.

FGS-Startfehler und Berechtigungsfehler werden abgefangen und als Diagnose ausgegeben. Keine automatische Activity und keine aufdringliche Benachrichtigung: nur die vorgeschriebene laufende Standort-Benachrichtigung mit niedriger Priorität. Keine Audio-APIs und kein Fokuswechsel; Browser und Android Auto behalten ihren Vordergrund.

## Zuverlässigkeitsgrenzen

Die Konstruktion verwendet dokumentierte Ausnahmen; eine uneingeschränkte Autostart-Garantie kann Android keiner normalen App geben. Nach Force-Stop, Widerruf von Berechtigungen, manuellem System-Stopp oder restriktiven Systemeinstellungen kann eine erneute Nutzeraktion nötig sein. Keine Wiederbelebung gegen den ausdrücklichen Nutzerwillen. Bluetooth-Neuverbindung, Prozessverlust, Neustart und Qi-Ladepausen müssen auf dem Zielgerät gemessen werden. Eine Installation per ADB ist noch kein Nachweis des kompletten Fahrtablaufs.

Bei einer späteren Play-Veröffentlichung sind zusätzlich die dann geltenden Background-Location-/FGS-Richtlinien und Deklarationen zu prüfen; diese Entscheidung betrifft zunächst den lokalen Funktionstest.

## Primärquellen

- [Android 17 SDK: compileSdk/targetSdk 37](https://developer.android.com/about/versions/17/setup-sdk)
- [FGS-Hintergrundstart und Companion-Ausnahme](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start)
- [FGS-Typ location und ACCESS_BACKGROUND_LOCATION](https://developer.android.com/develop/background-work/services/fgs/service-types#location)
- [Companion-Zuordnung](https://developer.android.com/develop/connectivity/bluetooth/companion-device-pairing)
- [Presence-Beobachtung](https://developer.android.com/reference/android/companion/CompanionDeviceManager#startObservingDevicePresence(android.companion.ObservingDevicePresenceRequest))
- [DevicePresenceEvent](https://developer.android.com/reference/android/companion/DevicePresenceEvent)
- [BluetoothDevice.isConnected(int)](https://developer.android.com/reference/android/bluetooth/BluetoothDevice#isConnected(int))
- [Hintergrundstandort separat freigeben](https://developer.android.com/develop/sensors-and-location/location/permissions/background)
- [BatteryManager](https://developer.android.com/reference/android/os/BatteryManager)
- [Ausnahmen für manifestregistrierte Bluetooth-/Boot-Broadcasts](https://developer.android.com/develop/background-work/background-tasks/broadcasts/broadcast-exceptions)
- [Android 17: alle Apps](https://developer.android.com/about/versions/17/behavior-changes-all), [targetSdk 37](https://developer.android.com/about/versions/17/behavior-changes-17)
- [AGP 9.2: API 37, Gradle 9.4.1, JDK ab 17](https://developer.android.com/build/releases/agp-9-2-0-release-notes)

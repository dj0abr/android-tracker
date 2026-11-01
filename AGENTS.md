# Projektkonventionen

- Erstes Etappenziel: Android-17-Funktionstest für Auto-Bluetooth + Qi → GPS + Logcat. Kein APRS-IS, TCP oder SmartBeaconing, bis der Gerätetest bestätigt wurde.
- Natives Android, ausschließlich Java und klassische Views/XML, kein Kotlin und kein Compose. compileSdk/targetSdk 37, minSdk 37 für diesen gerätespezifischen Prototyp.
- Allman Style: öffnende Java-/Groovy-Klammern auf eigener Zeile, vier Leerzeichen, keine Tabs. Kompakter Code; Leerzeilen nur zwischen logischen Abschnitten/Methoden. Immer Klammern für Kontrollblöcke.
- Vor Änderungen vorhandenen Git-Status prüfen. Nutzeränderungen erhalten. Kleine, nachvollziehbare Commits mit aussagekräftigen englischen Nachrichten; kein automatischer Push.
- Build ausschließlich über den eingecheckten Gradle Wrapper. Nach funktionalen Änderungen `./gradlew assembleDebug testDebugUnitTest lintDebug` ausführen. Für Änderungen der Hintergrundlogik zusätzlich den Pixel-Testplan in docs/DEVICE_TEST.md abarbeiten, soweit die Hardware verfügbar ist.
- Hintergrundstarts müssen die FGS-Startausnahme UND die Standortberechtigungen erfüllen. Siehe docs/ANDROID_BACKGROUND_START.md. Keine Overlays, Accessibility-Dienste, Audio-Fokus-Tricks, exakten Alarme oder pauschalen Akku-Ausnahmen.
- Echter Bluetooth-Classic-Verbindungsstatus zählt, BLE-Nähe allein nicht. Kabellose Stromversorgung aus BatteryManager lesen, keine USB-/Netzladung als Qi behandeln. Unbekannte Zustände schließen das GPS-Gate.
- Laufzeitzustände und Positionen mit dem Tag `AprsTracker` protokollieren. Keine Passcodes protokollieren. Koordinaten sind nur für den ausdrücklich gewünschten Debug-Funktionstest vorgesehen; vor produktiver Nutzung Logging neu bewerten.
- Kein dauerhaft gespeicherter „Bluetooth verbunden“-Wert. Nach Prozessneustart/Boot aktuelle Systemzustände abfragen. GPS-Listener beim Stoppen entfernen, keine Wake Locks, kein Polling außerhalb des Autos.
- Dienste dürfen keine Activity automatisch öffnen. Einrichtung und Berechtigungen brauchen einmalige Nutzerinteraktion. Force-Stop und System-Stopp respektieren.
- README und Testprotokoll müssen tatsächliche Prüfungen von noch offenen Hardwaretests unterscheiden. Keine erfolgreiche Auto-/Qi-/GPS-Prüfung ohne Messbeleg behaupten.

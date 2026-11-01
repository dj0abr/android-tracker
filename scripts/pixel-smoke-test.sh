#!/usr/bin/env bash
set -euo pipefail
tracker_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
tracker_sdk="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}}"
tracker_adb="$tracker_sdk/platform-tools/adb"
if [[ ! -x "$tracker_adb" ]]; then
    tracker_adb="$(command -v adb)"
fi
tracker_serial="${1:-${ANDROID_SERIAL:-}}"
if [[ -z "$tracker_serial" ]]; then
    mapfile -t tracker_devices < <("$tracker_adb" devices | awk 'NR > 1 && $2 == "device" {print $1}')
    if [[ ${#tracker_devices[@]} -ne 1 ]]; then
        echo 'Bitte genau ein Gerät verbinden oder dessen ADB-SERIAL als Argument angeben.' >&2
        exit 2
    fi
    tracker_serial="${tracker_devices[0]}"
fi
tracker_device=("$tracker_adb" -s "$tracker_serial")
tracker_api="$("${tracker_device[@]}" shell getprop ro.build.version.sdk | tr -d '\r')"
if [[ "$tracker_api" != 37 ]]; then
    echo "Erwartet Android API 37, gefunden: $tracker_api" >&2
    exit 2
fi
tracker_apk="$tracker_root/app/build/outputs/apk/debug/app-debug.apk"
if [[ ! -f "$tracker_apk" ]]; then
    echo 'APK fehlt. Zuerst ./gradlew assembleDebug ausführen.' >&2
    exit 2
fi
tracker_output="$tracker_root/app/build/device-tests/$(date +%Y%m%d-%H%M%S)"
mkdir -p "$tracker_output"
"${tracker_device[@]}" shell getprop ro.product.model > "$tracker_output/model.txt"
"${tracker_device[@]}" shell getprop ro.build.fingerprint > "$tracker_output/fingerprint.txt"
"${tracker_device[@]}" install -r "$tracker_apk" | tee "$tracker_output/install.txt"
tracker_start="$("${tracker_device[@]}" shell date '+%m-%d %H:%M:%S.000' | tr -d '\r')"
"${tracker_device[@]}" shell am start -W -n de.kurt.aprstracker/.MainActivity | tee "$tracker_output/start.txt"
sleep 2
"${tracker_device[@]}" shell pidof de.kurt.aprstracker > "$tracker_output/pid.txt"
"${tracker_device[@]}" shell dumpsys activity services de.kurt.aprstracker > "$tracker_output/services.txt"
"${tracker_device[@]}" shell dumpsys package de.kurt.aprstracker > "$tracker_output/package.txt"
"${tracker_device[@]}" exec-out screencap -p > "$tracker_output/screen.png"
"${tracker_device[@]}" logcat -d -v threadtime -T "$tracker_start" AprsTracker:I AndroidRuntime:E '*:S' > "$tracker_output/logcat.txt"
"${tracker_device[@]}" logcat -d -b crash -v threadtime -T "$tracker_start" > "$tracker_output/crash.txt"
if grep -q 'Process: de.kurt.aprstracker,' "$tracker_output/crash.txt"; then
    echo "Tracker-Absturz gefunden. Siehe $tracker_output/crash.txt" >&2
    exit 1
fi
if ! grep -q '^Status: ok' "$tracker_output/start.txt"; then
    echo "Activity-Start prüfen: $tracker_output/start.txt" >&2
    exit 1
fi
echo "Basis-Smoke-Test: Activity läuft ohne eigenen Startabsturz. Artefakte: $tracker_output"
echo 'Auto-Verbindung, Qi, echte GPS-Fixes und Hintergrund-Wiederanlauf gemäß docs/DEVICE_TEST.md prüfen.'

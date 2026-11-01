package de.kurt.aprstracker;

import android.os.BatteryManager;

final class TrackingGate
{
    enum Reason
    {
        DISABLED, NO_ASSOCIATION, BLUETOOTH_PERMISSION, DISCONNECTED,
        NOT_WIRELESS, FINE_PERMISSION, BACKGROUND_PERMISSION, LOCATION_DISABLED, READY
    }
    private TrackingGate()
    {
    }
    static Reason evaluate(boolean enabled, boolean associated, boolean bluetoothPermission,
        boolean connected, boolean wirelessPowered, boolean finePermission,
        boolean backgroundPermission, boolean locationEnabled)
    {
        if (!enabled)
        {
            return Reason.DISABLED;
        }
        if (!associated)
        {
            return Reason.NO_ASSOCIATION;
        }
        if (!bluetoothPermission)
        {
            return Reason.BLUETOOTH_PERMISSION;
        }
        if (!connected)
        {
            return Reason.DISCONNECTED;
        }
        if (!wirelessPowered)
        {
            return Reason.NOT_WIRELESS;
        }
        if (!finePermission)
        {
            return Reason.FINE_PERMISSION;
        }
        if (!backgroundPermission)
        {
            return Reason.BACKGROUND_PERMISSION;
        }
        if (!locationEnabled)
        {
            return Reason.LOCATION_DISABLED;
        }
        return Reason.READY;
    }
    static boolean isWirelessPowered(int plugged, int status)
    {
        return (plugged & BatteryManager.BATTERY_PLUGGED_WIRELESS) != 0
            && (status == BatteryManager.BATTERY_STATUS_CHARGING
                || status == BatteryManager.BATTERY_STATUS_FULL);
    }
}

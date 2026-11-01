package de.kurt.aprstracker;

import android.os.BatteryManager;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class TrackingGateTest
{
    @Test
    public void all256GateCombinationsFailClosedUnlessEveryConditionIsMet()
    {
        for (int mask = 0; mask < 256; mask++)
        {
            boolean[] flags = new boolean[8];
            for (int bit = 0; bit < flags.length; bit++)
            {
                flags[bit] = (mask & (1 << bit)) != 0;
            }
            TrackingGate.Reason result = TrackingGate.evaluate(flags[0], flags[1], flags[2],
                flags[3], flags[4], flags[5], flags[6], flags[7]);
            assertEquals("mask=" + mask, mask == 255, result == TrackingGate.Reason.READY);
        }
    }
    @Test
    public void missingBackgroundPermissionBlocksOtherwiseReadyStart()
    {
        assertEquals(TrackingGate.Reason.BACKGROUND_PERMISSION,
            TrackingGate.evaluate(true, true, true, true, true, true, false, true));
    }
    @Test
    public void disablingAutomationStopsOtherwiseReadyTracking()
    {
        assertEquals(TrackingGate.Reason.DISABLED,
            TrackingGate.evaluate(false, true, true, true, true, true, true, true));
    }
    @Test
    public void wirelessPowerRequiresChargingOrFullBattery()
    {
        for (int plugged = 0; plugged < 16; plugged++)
        {
            for (int status = 1; status <= 5; status++)
            {
                boolean expected = (plugged & 4) != 0 && (status == 2 || status == 5);
                assertEquals("plugged=" + plugged + " status=" + status,
                    expected, TrackingGate.isWirelessPowered(plugged, status));
            }
        }
    }
    @Test
    public void fullBatteryOnWirelessChargerKeepsTrackingEligible()
    {
        assertTrue(TrackingGate.isWirelessPowered(BatteryManager.BATTERY_PLUGGED_WIRELESS,
            BatteryManager.BATTERY_STATUS_FULL));
    }
    @Test
    public void usbAndThermalPauseDoNotCountAsWirelessCharging()
    {
        assertFalse(TrackingGate.isWirelessPowered(BatteryManager.BATTERY_PLUGGED_USB,
            BatteryManager.BATTERY_STATUS_CHARGING));
        assertFalse(TrackingGate.isWirelessPowered(BatteryManager.BATTERY_PLUGGED_WIRELESS,
            BatteryManager.BATTERY_STATUS_NOT_CHARGING));
    }
}

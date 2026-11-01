package de.kurt.aprstracker;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public final class SystemEventReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
        String action = intent.getAction();
        TrackerController controller = TrackerApplication.controller(context);
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
            || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action))
        {
            Log.i(TrackerController.TAG, "RECOVERY " + action);
            controller.configureObservation();
        }
        else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)
            || BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action))
        {
            // Only the selected device can affect the gate; snapshot reads its live state.
            controller.refresh(action);
        }
    }
}

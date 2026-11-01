package de.kurt.aprstracker;

import android.Manifest;
import android.app.ForegroundServiceStartNotAllowedException;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.companion.AssociationInfo;
import android.companion.CompanionDeviceManager;
import android.companion.ObservingDevicePresenceRequest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.util.Log;

final class TrackerController
{
    static final String TAG = "AprsTracker";
    static final String ACTION_STATUS = "de.kurt.aprstracker.STATUS";
    private final Context context;
    final TrackerSettings settings;
    private boolean startPending;
    private boolean running;
    private String lastState = "";
    private String diagnostic = "";
    private final BroadcastReceiver events = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context receiverContext, Intent intent)
        {
            refresh(intent.getAction());
        }
    };
    TrackerController(Context context)
    {
        this.context = context;
        settings = new TrackerSettings(context);
    }
    void initialize()
    {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(LocationManager.MODE_CHANGED_ACTION);
        filter.addAction(LocationManager.PROVIDERS_CHANGED_ACTION);
        // Bluetooth broadcasts can originate in the privileged Bluetooth process.
        context.registerReceiver(events, filter, Context.RECEIVER_EXPORTED);
        Log.i(TAG, "PROCESS initialized; no persisted connection state");
    }
    boolean hasPermission(String permission)
    {
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }
    AssociationInfo selectedAssociation()
    {
        CompanionDeviceManager manager = context.getSystemService(CompanionDeviceManager.class);
        if (manager == null || !context.getPackageManager().hasSystemFeature(
            PackageManager.FEATURE_COMPANION_DEVICE_SETUP))
        {
            return null;
        }
        for (AssociationInfo association : manager.getMyAssociations())
        {
            if (association.getId() == settings.getAssociationId()
                && association.getDeviceMacAddress() != null)
            {
                return association;
            }
        }
        return null;
    }
    void configureObservation()
    {
        AssociationInfo association = selectedAssociation();
        if (association == null)
        {
            refresh("no_association");
            return;
        }
        CompanionDeviceManager manager = context.getSystemService(CompanionDeviceManager.class);
        ObservingDevicePresenceRequest request = new ObservingDevicePresenceRequest.Builder()
            .setAssociationId(association.getId()).build();
        try
        {
            if (settings.isEnabled())
            {
                manager.startObservingDevicePresence(request);
            }
            else
            {
                manager.stopObservingDevicePresence(request);
            }
            diagnostic = "";
            Log.i(TAG, "PRESENCE observation=" + settings.isEnabled()
                + " association=" + association.getId());
        }
        catch (RuntimeException exception)
        {
            reportError("Presence registration failed", exception);
        }
        refresh("configure_observation");
    }
    Snapshot snapshot()
    {
        Snapshot state = new Snapshot();
        state.enabled = settings.isEnabled();
        state.bluetoothPermission = hasPermission(Manifest.permission.BLUETOOTH_CONNECT);
        state.finePermission = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION);
        state.backgroundPermission = hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        try
        {
            AssociationInfo association = selectedAssociation();
            state.associated = association != null;
            if (association != null)
            {
                state.carName = String.valueOf(association.getDisplayName());
                BluetoothManager manager = context.getSystemService(BluetoothManager.class);
                BluetoothAdapter adapter = manager == null ? null : manager.getAdapter();
                if (state.bluetoothPermission && adapter != null && adapter.isEnabled())
                {
                    BluetoothDevice device = adapter.getRemoteDevice(
                        association.getDeviceMacAddress().toByteArray());
                    state.connected = device.isConnected(BluetoothDevice.TRANSPORT_BREDR);
                }
            }
            Intent battery = context.registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (battery != null)
            {
                state.plugged = battery.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
                state.batteryStatus = battery.getIntExtra(BatteryManager.EXTRA_STATUS,
                    BatteryManager.BATTERY_STATUS_UNKNOWN);
                state.wirelessPowered = TrackingGate.isWirelessPowered(
                    state.plugged, state.batteryStatus);
            }
            LocationManager location = context.getSystemService(LocationManager.class);
            state.locationEnabled = location != null && location.isLocationEnabled()
                && location.isProviderEnabled(LocationManager.GPS_PROVIDER);
        }
        catch (SecurityException exception)
        {
            state.connected = false;
            reportError("System permission revoked", exception);
        }
        catch (RuntimeException exception)
        {
            // Fail closed even if a permission is revoked between check and binder call.
            state.connected = false;
            reportError("System state unavailable", exception);
        }
        state.reason = TrackingGate.evaluate(state.enabled, state.associated,
            state.bluetoothPermission, state.connected, state.wirelessPowered,
            state.finePermission, state.backgroundPermission, state.locationEnabled);
        return state;
    }
    void refresh(String trigger)
    {
        Snapshot state = snapshot();
        String description = "enabled=" + state.enabled + " associated=" + state.associated
            + " bt=" + state.connected + " wireless=" + state.wirelessPowered
            + " plugged=" + state.plugged + " batteryStatus=" + state.batteryStatus
            + " fine=" + state.finePermission + " background=" + state.backgroundPermission
            + " gpsEnabled=" + state.locationEnabled + " gate=" + state.reason;
        if (!description.equals(lastState))
        {
            lastState = description;
            Log.i(TAG, "STATE trigger=" + trigger + " " + description);
            publishStatus();
        }
        Intent service = new Intent(context, GpsTrackingService.class);
        if (state.reason == TrackingGate.Reason.READY)
        {
            if (!running && !startPending)
            {
                try
                {
                    startPending = true;
                    context.startForegroundService(service);
                    Log.i(TAG, "GPS_START requested trigger=" + trigger);
                }
                catch (ForegroundServiceStartNotAllowedException | SecurityException exception)
                {
                    startPending = false;
                    reportError("Location FGS start denied", exception);
                }
                catch (IllegalStateException exception)
                {
                    startPending = false;
                    reportError("Location FGS start failed", exception);
                }
            }
        }
        else
        {
            startPending = false;
            context.stopService(service);
        }
    }
    void serviceRunning(boolean value)
    {
        running = value;
        startPending = false;
        if (value)
        {
            diagnostic = "";
        }
        publishStatus();
    }
    boolean isRunning()
    {
        return running;
    }
    String getDiagnostic()
    {
        return diagnostic;
    }
    void reportError(String message, RuntimeException exception)
    {
        String next = message + ": " + exception.getClass().getSimpleName();
        if (!next.equals(diagnostic))
        {
            diagnostic = next;
            Log.e(TAG, diagnostic, exception);
            publishStatus();
        }
    }
    private void publishStatus()
    {
        context.sendBroadcast(new Intent(ACTION_STATUS).setPackage(context.getPackageName()));
    }
    static final class Snapshot
    {
        boolean enabled;
        boolean associated;
        boolean bluetoothPermission;
        boolean connected;
        boolean wirelessPowered;
        boolean finePermission;
        boolean backgroundPermission;
        boolean locationEnabled;
        int plugged;
        int batteryStatus;
        String carName;
        TrackingGate.Reason reason;
    }
}

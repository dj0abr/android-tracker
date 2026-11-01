package de.kurt.aprstracker;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.companion.AssociationInfo;
import android.companion.AssociationRequest;
import android.companion.BluetoothDeviceFilter;
import android.companion.CompanionDeviceManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.WindowInsets;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

public final class MainActivity extends Activity
{
    private TrackerController controller;
    private TextView status;
    private Switch enabled;
    private final BroadcastReceiver statusReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            renderStatus();
        }
    };
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        controller = TrackerApplication.controller(this);
        setContentView(R.layout.activity_main);
        findViewById(R.id.root).setOnApplyWindowInsetsListener((view, insets) ->
        {
            android.graphics.Insets bars = insets.getInsets(
                WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        status = findViewById(R.id.status);
        enabled = findViewById(R.id.enabled);
        enabled.setChecked(controller.settings.isEnabled());
        enabled.setOnCheckedChangeListener((button, checked) ->
        {
            controller.settings.setEnabled(checked);
            controller.configureObservation();
            renderStatus();
        });
        findViewById(R.id.permissions).setOnClickListener(view -> requestSetupPermissions());
        findViewById(R.id.background).setOnClickListener(view -> explainBackgroundLocation());
        findViewById(R.id.choose_car).setOnClickListener(view -> chooseCar());
        findViewById(R.id.refresh).setOnClickListener(view ->
        {
            controller.configureObservation();
            renderStatus();
        });
        findViewById(R.id.app_settings).setOnClickListener(view -> openAppSettings());
    }
    @Override
    protected void onStart()
    {
        super.onStart();
        registerReceiver(statusReceiver, new IntentFilter(TrackerController.ACTION_STATUS),
            Context.RECEIVER_NOT_EXPORTED);
    }
    @Override
    protected void onResume()
    {
        super.onResume();
        enabled.setChecked(controller.settings.isEnabled());
        controller.configureObservation();
        renderStatus();
    }
    @Override
    protected void onStop()
    {
        unregisterReceiver(statusReceiver);
        super.onStop();
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results)
    {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        controller.configureObservation();
        renderStatus();
    }
    private void requestSetupPermissions()
    {
        List<String> missing = new ArrayList<>();
        String[] permissions = {Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.POST_NOTIFICATIONS};
        for (String permission : permissions)
        {
            if (!controller.hasPermission(permission))
            {
                missing.add(permission);
            }
        }
        if (!missing.isEmpty())
        {
            requestPermissions(missing.toArray(new String[0]), 1);
        }
        else
        {
            renderStatus();
        }
    }
    private void explainBackgroundLocation()
    {
        if (!controller.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION))
        {
            Toast.makeText(this, R.string.need_permissions, Toast.LENGTH_LONG).show();
            return;
        }
        new AlertDialog.Builder(this).setTitle(R.string.background_title)
            .setMessage(getString(R.string.background_explanation,
                getPackageManager().getBackgroundPermissionOptionLabel()))
            .setPositiveButton(R.string.open_settings, (dialog, which) -> openAppSettings())
            .setNegativeButton(R.string.cancel, null).show();
    }
    private void openAppSettings()
    {
        startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:" + getPackageName())));
    }
    private void chooseCar()
    {
        if (!controller.hasPermission(Manifest.permission.BLUETOOTH_CONNECT))
        {
            Toast.makeText(this, R.string.need_permissions, Toast.LENGTH_LONG).show();
            return;
        }
        try
        {
            List<BluetoothDevice> devices = new ArrayList<>(
                getSystemService(BluetoothManager.class).getAdapter().getBondedDevices());
            devices.sort((first, second) -> deviceLabel(first).compareTo(deviceLabel(second)));
            if (devices.isEmpty())
            {
                Toast.makeText(this, R.string.no_bonded_devices, Toast.LENGTH_LONG).show();
                return;
            }
            String[] labels = new String[devices.size()];
            for (int index = 0; index < devices.size(); index++)
            {
                labels[index] = deviceLabel(devices.get(index));
            }
            new AlertDialog.Builder(this).setTitle(R.string.select_car_title)
                .setItems(labels, (dialog, index) -> associate(devices.get(index)))
                .setNegativeButton(R.string.cancel, null).show();
        }
        catch (SecurityException exception)
        {
            controller.reportError("Bluetooth permission revoked", exception);
            renderStatus();
        }
        catch (RuntimeException exception)
        {
            controller.reportError("Bluetooth selection failed", exception);
            renderStatus();
        }
    }
    private String deviceLabel(BluetoothDevice device)
    {
        String name = null;
        try
        {
            name = device.getName();
        }
        catch (SecurityException exception)
        {
            controller.reportError("Bluetooth name permission revoked", exception);
        }
        return (name == null ? getString(R.string.unknown_device) : name) + " · " + device.getAddress();
    }
    private void associate(BluetoothDevice device)
    {
        CompanionDeviceManager manager = getSystemService(CompanionDeviceManager.class);
        for (AssociationInfo association : manager.getMyAssociations())
        {
            if (association.getDeviceMacAddress() != null
                && association.getDeviceMacAddress().toString().equalsIgnoreCase(device.getAddress()))
            {
                selectAssociation(association);
                return;
            }
        }
        AssociationRequest request = new AssociationRequest.Builder()
            .addDeviceFilter(new BluetoothDeviceFilter.Builder().setAddress(device.getAddress()).build())
            .setSingleDevice(true).build();
        try
        {
            manager.associate(request, getMainExecutor(), new CompanionDeviceManager.Callback()
            {
                @Override
                public void onAssociationPending(IntentSender sender)
                {
                    try
                    {
                        startIntentSenderForResult(sender, 2, null, 0, 0, 0);
                    }
                    catch (IntentSender.SendIntentException exception)
                    {
                        Toast.makeText(MainActivity.this,
                            getString(R.string.association_failed, exception.getMessage()),
                            Toast.LENGTH_LONG).show();
                    }
                }
                @Override
                public void onAssociationCreated(AssociationInfo association)
                {
                    selectAssociation(association);
                }
                @Override
                public void onFailure(CharSequence error)
                {
                    Toast.makeText(MainActivity.this, getString(R.string.association_failed, error),
                        Toast.LENGTH_LONG).show();
                }
            });
        }
        catch (RuntimeException exception)
        {
            controller.reportError("Association request failed", exception);
            renderStatus();
        }
    }
    private void selectAssociation(AssociationInfo association)
    {
        int previous = controller.settings.getAssociationId();
        controller.settings.setAssociationId(association.getId());
        if (previous != -1 && previous != association.getId())
        {
            try
            {
                getSystemService(CompanionDeviceManager.class).disassociate(previous);
            }
            catch (RuntimeException exception)
            {
                controller.reportError("Old association removal failed", exception);
            }
        }
        controller.configureObservation();
        renderStatus();
    }
    private String yesNo(boolean value)
    {
        return getString(value ? R.string.yes : R.string.no);
    }
    private void renderStatus()
    {
        TrackerController.Snapshot state = controller.snapshot();
        int reason = switch (state.reason)
        {
            case DISABLED -> R.string.gate_disabled;
            case NO_ASSOCIATION -> R.string.gate_no_association;
            case BLUETOOTH_PERMISSION -> R.string.gate_bluetooth_permission;
            case DISCONNECTED -> R.string.gate_disconnected;
            case NOT_WIRELESS -> R.string.gate_not_wireless;
            case FINE_PERMISSION -> R.string.gate_fine_permission;
            case BACKGROUND_PERMISSION -> R.string.gate_background_permission;
            case LOCATION_DISABLED -> R.string.gate_location_disabled;
            case READY -> R.string.gate_ready;
        };
        String diagnostic = controller.getDiagnostic();
        status.setText(getString(R.string.status_format,
            state.carName == null ? getString(R.string.no_car) : state.carName,
            yesNo(state.associated), yesNo(state.connected), yesNo(state.wirelessPowered),
            yesNo(state.finePermission), yesNo(state.backgroundPermission), yesNo(state.locationEnabled),
            yesNo(controller.hasPermission(Manifest.permission.POST_NOTIFICATIONS)),
            getString(controller.isRunning() ? R.string.running : R.string.stopped),
            getString(reason), diagnostic.isEmpty() ? getString(R.string.none) : diagnostic));
    }
}

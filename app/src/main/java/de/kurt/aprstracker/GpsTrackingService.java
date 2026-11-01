package de.kurt.aprstracker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import java.util.Locale;

public final class GpsTrackingService extends Service implements LocationListener
{
    private static final String CHANNEL = "drive_location";
    private static final int NOTIFICATION_ID = 1;
    private LocationManager locationManager;
    private TrackerController controller;
    private boolean listening;
    private boolean foreground;
    @Override
    public void onCreate()
    {
        super.onCreate();
        controller = TrackerApplication.controller(this);
        locationManager = getSystemService(LocationManager.class);
        NotificationManager notifications = getSystemService(NotificationManager.class);
        NotificationChannel channel = new NotificationChannel(CHANNEL,
            getString(R.string.channel_name), NotificationManager.IMPORTANCE_LOW);
        channel.setSound(null, null);
        channel.enableVibration(false);
        notifications.createNotificationChannel(channel);
        PendingIntent open = PendingIntent.getActivity(this, 0,
            new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_tracker)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE).build();
        try
        {
            startForeground(NOTIFICATION_ID, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
            foreground = true;
        }
        catch (RuntimeException exception)
        {
            controller.reportError("Location FGS promotion failed", exception);
            stopSelf();
        }
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        if (!foreground || controller.snapshot().reason != TrackingGate.Reason.READY)
        {
            Log.i(TrackerController.TAG, "GPS_START cancelled: conditions no longer met");
            stopSelf();
            return START_NOT_STICKY;
        }
        try
        {
            if (!listening)
            {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    5000L, 0.0f, this, Looper.getMainLooper());
                listening = true;
                controller.serviceRunning(true);
                Log.i(TrackerController.TAG, "GPS_STARTED intervalMs=5000 stickyRestart="
                    + (intent == null));
            }
            return START_STICKY;
        }
        catch (SecurityException exception)
        {
            controller.reportError("GPS permission revoked", exception);
            stopSelf();
            return START_NOT_STICKY;
        }
        catch (RuntimeException exception)
        {
            controller.reportError("GPS subscription failed", exception);
            stopSelf();
            return START_NOT_STICKY;
        }
    }
    @Override
    public void onLocationChanged(Location location)
    {
        controller.refresh("gps_guard");
        if (controller.snapshot().reason != TrackingGate.Reason.READY)
        {
            return;
        }
        if (BuildConfig.DEBUG)
        {
            Log.i(TrackerController.TAG, String.format(Locale.ROOT,
                "GPS lat=%.6f lon=%.6f accuracyM=%.1f speedMps=%.2f bearing=%.1f timeMs=%d mock=%s",
                location.getLatitude(), location.getLongitude(), location.getAccuracy(),
                location.hasSpeed() ? location.getSpeed() : Float.NaN,
                location.hasBearing() ? location.getBearing() : Float.NaN,
                location.getTime(), location.isMock()));
        }
    }
    @Override
    public void onProviderDisabled(String provider)
    {
        controller.refresh("gps_provider_disabled");
    }
    @Override
    public void onProviderEnabled(String provider)
    {
        controller.refresh("gps_provider_enabled");
    }
    @Override
    public void onDestroy()
    {
        if (listening)
        {
            try
            {
                locationManager.removeUpdates(this);
            }
            catch (SecurityException exception)
            {
                controller.reportError("GPS cleanup after permission revocation", exception);
            }
            listening = false;
        }
        stopForeground(STOP_FOREGROUND_REMOVE);
        controller.serviceRunning(false);
        Log.i(TrackerController.TAG, "GPS_STOPPED listener removed");
        super.onDestroy();
    }
    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }
}

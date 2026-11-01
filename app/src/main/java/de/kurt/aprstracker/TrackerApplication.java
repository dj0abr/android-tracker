package de.kurt.aprstracker;

import android.app.Application;
import android.content.Context;

public final class TrackerApplication extends Application
{
    private TrackerController controller;
    @Override
    public void onCreate()
    {
        super.onCreate();
        controller = new TrackerController(this);
        controller.initialize();
    }
    static TrackerController controller(Context context)
    {
        return ((TrackerApplication) context.getApplicationContext()).controller;
    }
}

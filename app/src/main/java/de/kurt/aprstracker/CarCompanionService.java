package de.kurt.aprstracker;

import android.companion.CompanionDeviceService;
import android.companion.DevicePresenceEvent;
import android.util.Log;

public final class CarCompanionService extends CompanionDeviceService
{
    @Override
    public void onDevicePresenceEvent(DevicePresenceEvent event)
    {
        Log.i(TrackerController.TAG, "PRESENCE association=" + event.getAssociationId()
            + " event=" + event.getEvent());
        TrackerController controller = TrackerApplication.controller(this);
        if (event.getAssociationId() == controller.settings.getAssociationId())
        {
            controller.refresh("companion_event_" + event.getEvent());
        }
    }
    @Override
    public void onDestroy()
    {
        TrackerApplication.controller(this).refresh("companion_destroyed");
        super.onDestroy();
    }
}

package de.kurt.aprstracker;

import android.content.Context;
import android.content.SharedPreferences;

final class TrackerSettings
{
    private final SharedPreferences preferences;
    TrackerSettings(Context context)
    {
        preferences = context.getSharedPreferences("tracker", Context.MODE_PRIVATE);
    }
    boolean isEnabled()
    {
        return preferences.getBoolean("enabled", false);
    }
    void setEnabled(boolean enabled)
    {
        preferences.edit().putBoolean("enabled", enabled).apply();
    }
    int getAssociationId()
    {
        return preferences.getInt("association_id", -1);
    }
    void setAssociationId(int id)
    {
        preferences.edit().putInt("association_id", id).apply();
    }
}

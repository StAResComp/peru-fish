package uk.ac.masts.sifids;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import java.util.TimeZone;

import uk.ac.masts.sifids.activities.SettingsActivity;
import uk.ac.masts.sifids.receivers.AlarmReceiver;
import uk.ac.masts.sifids.services.CatchLocationService;

/**
 * This class is extends android.app.Application to provide a flag which can be used to indicate
 * whether or not the user is currently fishing and which can be set and inspected from anywhere in
 * the app.
 */
public class CatchApplication extends Application {

    public final static String VERSION = "0.6.5";

    public final static TimeZone TIME_ZONE = TimeZone.getTimeZone("TIME_ZONE");

    private boolean fishing = false;

    private boolean trackingLocation = false;

    /**
     * Indicates whether or not the user is currently fishing.
     *
     * @return whether or not the user is currently fishing
     */
    public boolean isFishing() {
        return fishing;
    }

    /**
     * Set whether or not the user is currently fishing.
     *
     * @param fishing whether or not the user is currently fishing
     */
    public void setFishing(boolean fishing) {
        this.fishing = fishing;
        if (this.isTrackingLocation()) {
            ServiceConnection connection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    CatchLocationService locationService = ((CatchLocationService.CatchLocationBinder) service).getService();
                    locationService.forceWriteLocation();
                    unbindService(this);
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {

                }
            };
            bindService(new Intent(this, CatchLocationService.class), connection, Context.BIND_AUTO_CREATE);
        }
    }

    public boolean isTrackingLocation() {
        return trackingLocation;
    }

    public void setTrackingLocation(boolean trackingLocation) {
        if (trackingLocation) {
            startService(new Intent(this, CatchLocationService.class));
        }
        else {
            stopService(new Intent(this, CatchLocationService.class));
            Toast.makeText(getBaseContext(), getString(R.string.stopped_tracking_location),
                    Toast.LENGTH_LONG).show();
        }
        this.trackingLocation = trackingLocation;
    }

    public void redirectIfNecessary() {
        if (!hasSetMinimumPreferences()) {
            Intent intent = new Intent(this, SettingsActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    private boolean hasSetMinimumPreferences() {
        return true;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Context context = this.getApplicationContext();
        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent alarmIntent =
                PendingIntent.getBroadcast(context, 0, intent, 0);
        alarmManager.setInexactRepeating(
                AlarmManager.RTC,
                SystemClock.elapsedRealtime() + 5000,
                AlarmManager.INTERVAL_HALF_HOUR, alarmIntent);
    }
}

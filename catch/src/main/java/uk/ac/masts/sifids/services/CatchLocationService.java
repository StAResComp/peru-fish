package uk.ac.masts.sifids.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.Calendar;

import uk.ac.masts.sifids.CatchApplication;
import uk.ac.masts.sifids.R;
import uk.ac.masts.sifids.activities.Fish1FormsActivity;
import uk.ac.masts.sifids.database.CatchDatabase;
import uk.ac.masts.sifids.entities.CatchLocation;

/**
 * Service for logging location to database
 *
 * Based largely on https://github.com/codepath/android_guides/issues/220#issuecomment-250756857
 */
public class CatchLocationService extends Service {

	private static final String TAG = "CatchLocationService";
	private LocationManager mLocationManager = null;
	private static final int LOCATION_INTERVAL = 30000;
	private static final float LOCATION_DISTANCE = 5f;
    private final int TRACKING_NOTIFICATION_ID = 204;
    private final String SIFIDS_LOCATION_TRACKING_CHANNEL_ID = "sifids_location_tracking_channel";
    NotificationManager notificationManager;
    private final IBinder locationBinder = new CatchLocationBinder();

    private class LocationListener implements android.location.LocationListener {

	    Location mLastLocation;

        //Attempt to persist location immediately on creation
        public LocationListener(String provider) {
            mLastLocation = new Location(provider);
        }

        //Update location and persist
        @Override
        public void onLocationChanged(Location location) {
            if(location.getAccuracy() > 0.01) {
                mLastLocation.set(location);
                this.writeLocation();
            }
        }

        //Attempt to persist location when provider is disabled
        @Override
        public void onProviderDisabled(String provider) {
            this.writeLocation();
        }

        //Attempt to persist location immediately when provider is enabled
        @Override
        public void onProviderEnabled(String provider) {
            this.writeLocation();
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        // Writes the current (technically, last known) location to the database
        protected void writeLocation() {
            if (mLastLocation != null) {
                final CatchDatabase db = CatchDatabase.getInstance(getApplicationContext());
                //Database queries need their own thread
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        CatchLocation location = new CatchLocation();
                        location.setLatitude(mLastLocation.getLatitude());
                        location.setLongitude(mLastLocation.getLongitude());
                        location.setAccuracy(mLastLocation.getAccuracy());
                        Calendar cal = Calendar.getInstance(CatchApplication.TIME_ZONE);
                        location.setTimestamp(cal.getTime());
                        location.setFishing(((CatchApplication) getApplication()).isFishing());
                        db.catchDao().insertLocation(location);
                    }
                };
                Thread newThread = new Thread(r);
                newThread.start();
            }
        }
    }

    private LocationListener mLocationListener;

	@Override
	public IBinder onBind(Intent arg0) {
		return locationBinder;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		return START_STICKY;
	}

	@Override
	public void onCreate() {

		initializeLocationManager();

        notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		/*
		App is intended for use at sea, where mobile signal will be unreliable and location derived
		from network triangulation even less so. Need to always use GPS for location, if available.
		 */

		String provider = LocationManager.GPS_PROVIDER;

        if (!mLocationManager.isProviderEnabled(provider)) {
            provider = LocationManager.NETWORK_PROVIDER;
        }

        mLocationListener = new LocationListener(provider);

		try {
			mLocationManager.requestLocationUpdates(
				provider,
				LOCATION_INTERVAL,
				LOCATION_DISTANCE,
				mLocationListener
			);
		} catch (java.lang.SecurityException ex) {
		} catch (IllegalArgumentException ex) {
		}

		startForeground(TRACKING_NOTIFICATION_ID, this.getNotification());
	}

	private Notification getNotification() {

        //Need to notify user that app is using location
        Intent notificationIntent = new Intent(this, Fish1FormsActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0,
                        notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        //Newer versions of Android need a notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelName = getString(R.string.location_tracking_notification_channel_name);
            NotificationChannel channel = new NotificationChannel(
                    SIFIDS_LOCATION_TRACKING_CHANNEL_ID, channelName,
                    NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
            return new Notification.Builder(this, SIFIDS_LOCATION_TRACKING_CHANNEL_ID)
                            .setContentTitle(
                                    getString(R.string.location_tracking_notification_title))
                            .setContentText(getString(R.string.location_tracking_notification_text))
                            .setSmallIcon(R.drawable.ic_menu_mylocation)
                            .setContentIntent(pendingIntent)
                            .setTicker(getString(R.string.location_tracking_notification_text))
                            .setVisibility(Notification.VISIBILITY_PRIVATE)
                            .build();
        }
        else {
            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(this, null)
                            .setContentTitle(
                                    getString(R.string.location_tracking_notification_title))
                            .setContentText(
                                    getString(R.string.location_tracking_notification_text))
                            .setSmallIcon(R.drawable.ic_menu_mylocation)
                            .setContentIntent(pendingIntent)
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                            .setDefaults(Notification.DEFAULT_SOUND)
                            .setTicker(getString(R.string.location_tracking_notification_text));
            return builder.build();
        }
    }

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mLocationManager != null) {
            try {
                if (
                        ActivityCompat.checkSelfPermission(
                                this,
                                android.Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                mLocationManager.removeUpdates(mLocationListener);
            } catch (Exception ex) {
            }
		}
	}

	private void initializeLocationManager() {
		if (mLocationManager == null) {
			mLocationManager =
                    (LocationManager) getApplicationContext()
                            .getSystemService(Context.LOCATION_SERVICE);
		}
	}

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class CatchLocationBinder extends Binder {
        public CatchLocationService getService() {
            // Return this instance of LocalService so clients can call public methods
            return CatchLocationService.this;
        }
    }

    public void forceWriteLocation() {
        this.mLocationListener.writeLocation();
    }

}


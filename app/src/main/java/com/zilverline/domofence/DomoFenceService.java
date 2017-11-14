package com.zilverline.domofence;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.TextUtils;
import android.util.Log;

import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;
import com.evernote.android.job.util.support.PersistableBundleCompat;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingEvent;
import com.zilverline.domofence.scheduler.JobScheduler;
import com.zilverline.domofence.scheduler.NetworkJobCreator;

import java.util.ArrayList;
import java.util.List;

public class DomoFenceService extends IntentService {

    private static final String TAG = "DomoFenceService";
    private static final String PACKAGENAME = "com.zilverline.domofence";


    public DomoFenceService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        JobManager.create(getApplicationContext()).addJobCreator(new NetworkJobCreator());
        Log.v(TAG, "Service started");
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "Service stopped");
        super.onDestroy();
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        JobManager.create(getApplicationContext()).addJobCreator(new NetworkJobCreator());

        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        String username, password;

        if (geofencingEvent.hasError()) {
            String errorMessage;

            switch (geofencingEvent.getErrorCode()) {
                case GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE:
                    errorMessage = "Geofence service is not available now"; break;
                case GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES:
                    errorMessage = "Your app has registered too many geofences"; break;
                case GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS:
                    errorMessage = "You have provided too many PendingIntents to the addGeofences() call"; break;
                default:
                    errorMessage = "Unknown error: the Geofence service is not available now";
            }
            Log.e(TAG, errorMessage);
            return;
        } else {
            username = intent.getStringExtra("username");
            password = intent.getStringExtra("password");
        }
        int geofenceTransition = geofencingEvent.getGeofenceTransition();
        Log.v(TAG, "Geofence Transition: " + geofenceTransition);

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
                geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {

            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

            String optionalPort = (intent.getStringExtra("server_port").matches("\\d{1,5}") ? ":" + intent.getStringExtra("server_port") : "");

            String url = intent.getStringExtra("protocol")+"://" + intent.getStringExtra("server_address") + optionalPort +
                    "/json.htm?type=command&param=switchlight&idx=" +
                    intent.getStringExtra("switchIdx")+"&switchcmd=";

            boolean inGeofence;
            if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                inGeofence = true;
                scheduleRequest(url.replaceAll("\\s", "") + "On", username, password);
                Log.v(TAG, "Switch ON");
            } else {
                inGeofence = false;
                scheduleRequest(url.replaceAll("\\s", "") + "Off", username, password);
                Log.v(TAG, "Switch OFF");
            }

            SharedPreferences mSharedPreferences = getApplicationContext().getSharedPreferences(PACKAGENAME + ".SHARED_PREFERENCES_NAME",
                    Context.MODE_PRIVATE);

            SharedPreferences.Editor editor = mSharedPreferences.edit();
            editor.putBoolean(PACKAGENAME + ".inGeofence", inGeofence);
            editor.apply();

            String geofenceTransitionDetails = getGeofenceTransitionDetails(
                    this,
                    geofenceTransition,
                    triggeringGeofences
            );

            boolean toggleNotifications = mSharedPreferences.getBoolean(PACKAGENAME + ".notifications", true);
            Log.v(TAG, "Notify: " + toggleNotifications);

            if (toggleNotifications) {
                sendNotification(geofenceTransitionDetails);
            }

            Log.i(TAG, geofenceTransitionDetails);
        } else {
            Log.e(TAG, "Geofence transition error: invalid transition type: " + geofenceTransition);
        }

    }

    private void scheduleRequest(String url, String username, String password) {
        Log.d(TAG, "Scheduling a request: " + url);
        PersistableBundleCompat extras = new PersistableBundleCompat();
        extras.putString("url", url);
        extras.putString("username", username);
        extras.putString("password", password);

        new JobRequest.Builder(JobScheduler.TAG)
                .setExecutionWindow(500L, 5000L)
                .setExtras(extras)
                .setRequirementsEnforced(true)
                .setUpdateCurrent(true)
                .setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
                .build()
                .schedule();

    }

    private String getGeofenceTransitionDetails(
            Context context,
            int geofenceTransition,
            List<Geofence> triggeringGeofences) {

        String geofenceTransitionString = getTransitionString(geofenceTransition);

        ArrayList<String> triggeringGeofencesIdsList = new ArrayList<>();
        for (Geofence geofence : triggeringGeofences) {
            triggeringGeofencesIdsList.add(geofence.getRequestId());
        }
        String triggeringGeofencesIdsString = TextUtils.join(", ", triggeringGeofencesIdsList);

        return geofenceTransitionString + ": " + triggeringGeofencesIdsString;
    }

    private void sendNotification(String notificationDetails) {
        Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(notificationIntent);
        PendingIntent notificationPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.drawable.geofence_notification)
                .setContentTitle(notificationDetails)
                .setContentText("Tap here to return to DomoFence")
                .setContentIntent(notificationPendingIntent);
        builder.setAutoCancel(true);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(0, builder.build());
    }

    private String getTransitionString(int transitionType) {
        switch (transitionType) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                return "Entered";
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                return "Exited";
            default:
                return "Unknown transition";
        }
    }
}

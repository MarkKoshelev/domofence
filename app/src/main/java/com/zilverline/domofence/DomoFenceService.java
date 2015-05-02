package com.zilverline.domofence;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingEvent;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class DomoFenceService extends IntentService {

    private static final String TAG = "DomoFenceService";
    private String username, password;

    public DomoFenceService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(TAG, "Service started");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
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
        Log.v(TAG, "Geofence Transition: "+geofenceTransition);

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
                geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {

            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

            String url = "http://" + intent.getStringExtra("server_address") + ":" +
                    intent.getStringExtra("server_port") +
                    "/json.htm?type=command&param=switchlight&idx=" +
                    intent.getStringExtra("switchIdx")+"&switchcmd=";

            if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                getToServer(url.replaceAll("\\s","") + "On");
                Log.v(TAG, "Switch ON");
            } else {
                getToServer(url.replaceAll("\\s","") + "Off");
                Log.v(TAG, "Switch OFF");
            }

            String geofenceTransitionDetails = getGeofenceTransitionDetails(
                    this,
                    geofenceTransition,
                    triggeringGeofences
            );

            sendNotification(geofenceTransitionDetails);
            Log.i(TAG, geofenceTransitionDetails);
        } else {
            Log.e(TAG, "Geofence transition error: invalid transition type: "+geofenceTransition);
        }

    }

    private void getToServer(String requestUrl) {
        Authenticator.setDefault(new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password.toCharArray());
            }
        });

        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(requestUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setReadTimeout(2500);
            urlConnection.setConnectTimeout(3000);

            InputStream in = urlConnection.getInputStream();
            InputStreamReader isw = new InputStreamReader(in);
            String status = "";

            int data = isw.read();
            while (data != -1) {
                char current = (char) data;
                data = isw.read();
                status += current;
            }
            Log.v(TAG, "Response: "+status);

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            urlConnection.disconnect();
        }
    }

    private String getGeofenceTransitionDetails(
            Context context,
            int geofenceTransition,
            List<Geofence> triggeringGeofences) {

        String geofenceTransitionString = getTransitionString(geofenceTransition);

        ArrayList triggeringGeofencesIdsList = new ArrayList();
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
        builder.setSmallIcon(R.drawable.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                        R.drawable.ic_launcher))
                .setColor(Color.RED)
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

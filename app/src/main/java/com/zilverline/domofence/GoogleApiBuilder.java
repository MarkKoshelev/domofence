package com.zilverline.domofence;

import java.util.ArrayList;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.RuntimeExecutionException;
import com.google.android.gms.tasks.Task;

public class GoogleApiBuilder extends BroadcastReceiver implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "GoogleApiBuilder";
    private static final String PACKAGENAME = "com.zilverline.domofence";

    private GoogleApiClient mGoogleApiClient;
    private ArrayList<Geofence> mGeofenceList;
    private boolean mGeofencesAdded, start_after_boot;

    private PendingIntent mGeofencePendingIntent;
    private SharedPreferences mSharedPreferences;
    private String server_address, server_port, username, password, latitude, longitude, geofence_radius, idx_of_switch, protocol, overridden_server_address;
    private Context baseContext;


    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.v(TAG, "onReceive after reboot");
            initialize(context);

            if (mGeofencesAdded) {
                Log.v(TAG, "Geofence was added, re-adding");

                server_address = mSharedPreferences.getString(PACKAGENAME + ".server_address", "not_found");
                server_port = mSharedPreferences.getString(PACKAGENAME + ".server_port", "not_found");
                username = mSharedPreferences.getString(PACKAGENAME + ".username", "not_found");
                password = mSharedPreferences.getString(PACKAGENAME + ".password", "not_found");
                latitude = mSharedPreferences.getString(PACKAGENAME + ".latitude", "not_found");
                longitude = mSharedPreferences.getString(PACKAGENAME + ".longitude", "not_found");
                geofence_radius = mSharedPreferences.getString(PACKAGENAME + ".geofence_radius", "not_found");
                idx_of_switch = mSharedPreferences.getString(PACKAGENAME + ".idx_of_switch", "not_found");
                protocol = mSharedPreferences.getString(PACKAGENAME + ".protocol", "not_found");
                overridden_server_address = mSharedPreferences.getString(PACKAGENAME + ".overridden_server_address", "not_found");

                start_after_boot = true;
                mGoogleApiClient.connect();

            } else {
                start_after_boot = false;
                Log.v(TAG, "No data found, not initializing geofence");
            }
        } else {
            Toast.makeText(baseContext, "Got unknown message: " + intent.getAction(), Toast.LENGTH_SHORT).show();
        }
    }

    protected void initialize(Context context) {

        Log.v(TAG, "Initializing GoogleApiBuilder");

        baseContext = context;
        mGeofenceList = new ArrayList<>();
        mSharedPreferences = baseContext.getSharedPreferences(PACKAGENAME + ".SHARED_PREFERENCES_NAME",
                Context.MODE_PRIVATE);
        mGeofencesAdded = mSharedPreferences.getBoolean(PACKAGENAME + ".GEOFENCES_ADDED_KEY", false);

        if (ActivityCompat.checkSelfPermission(baseContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(baseContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            throw new LocationServiceNoPermission();
        }

        mGoogleApiClient = new GoogleApiClient.Builder(baseContext)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        Log.v(TAG, "Build the Google API Client");
    }

    protected void addGeofence(String server_address, String server_port, String password, String username, String latitude, String longitude, String geofence_radius, String idx_of_switch, String protocol) {
        Log.v(TAG, "Adding a geofence: " + mGeofencesAdded +".  server_address: " + server_address +
                " server_port: " + server_port +
                " password: " + password +
                " username: " + username +
                " latitude: " + latitude +
                " longitude: " + longitude +
                " geofence_radius: " + geofence_radius +
                " idx_of_switch: " + idx_of_switch +
                " protocol: " + protocol);

        this.server_address = server_address;
        this.server_port = server_port;
        this.password = password;
        this.username = username;
        this.latitude = latitude;
        this.longitude = longitude;
        this.geofence_radius = geofence_radius;
        this.idx_of_switch = idx_of_switch;
        this.protocol = protocol;

        populateGeofenceList();

        try {
            LocationServices.getGeofencingClient(baseContext).addGeofences(
                    getGeofencingRequest(),
                    getGeofencePendingIntent()
            ).addOnCompleteListener(new GeofenceCompletedListener());
        } catch (SecurityException securityException) {
            logSecurityException(securityException);
        }
    }

    protected void removeGeofence(String server_address, String server_port, String password, String username, String latitude, String longitude, String geofence_radius, String idx_of_switch, String protocol) {
        this.server_address = server_address;
        this.server_port = server_port;
        this.password = password;
        this.username = username;
        this.latitude = latitude;
        this.longitude = longitude;
        this.geofence_radius = geofence_radius;
        this.idx_of_switch = idx_of_switch;
        this.protocol = protocol;

        if (this.server_address.isEmpty()) {
            populateGeofenceList();
        }
        try {
            LocationServices.getGeofencingClient(baseContext).removeGeofences(
                    getGeofencePendingIntent()
            ).addOnCompleteListener(new GeofenceCompletedListener());
        } catch (SecurityException securityException) {
            logSecurityException(securityException);
        }
    }

    private void logSecurityException(SecurityException securityException) {
        Log.e(TAG, "Invalid location permission. " +
                "You need to use ACCESS_FINE_LOCATION with geofences", securityException);
    }

    private PendingIntent getGeofencePendingIntent() {

        if (mGeofencePendingIntent != null){
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(baseContext, DomoFenceBroadcastReceiver.class);
        intent.putExtra("server_address", server_address);
        intent.putExtra("server_port", server_port);
        intent.putExtra("username", username);
        intent.putExtra("password", password);
        intent.putExtra("switchIdx", idx_of_switch);
        intent.putExtra("protocol", protocol);
        intent.putExtra("overridden_server_address", overridden_server_address);

        Log.v(TAG, "Sending the Geofence Intent");

        return PendingIntent.getBroadcast(baseContext, 1977, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public void populateGeofenceList() {
        Log.v(TAG, "Populating the Geofences: " + protocol + "://" + server_address + ":" + server_port);

        Log.v(TAG, "Values: " + Double.valueOf(latitude) + " " +
                Double.valueOf(longitude) + " " +
                Float.valueOf(geofence_radius));

        mGeofenceList.add(new Geofence.Builder()
                .setRequestId(server_address)
                .setCircularRegion(
                        Double.valueOf(latitude),
                        Double.valueOf(longitude),
                        Float.valueOf(geofence_radius)
                )
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                        Geofence.GEOFENCE_TRANSITION_EXIT)
                .build());
    }

    protected void getCurrentLocation(final EditText lat, final EditText lon) {

        if (mGoogleApiClient != null) {
            if (ActivityCompat.checkSelfPermission(baseContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(baseContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                throw new LocationServiceNoPermission();
            }
            LocationServices.getFusedLocationProviderClient(baseContext).getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
                @Override
                public void onComplete(Task<Location> task) {
                    if (task.isSuccessful() && task.getResult() != null) {
                        lat.setText(String.valueOf(task.getResult().getLatitude()));
                        lon.setText((String.valueOf(task.getResult().getLongitude())));
                        Toast.makeText(baseContext, "Found and set current location!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(baseContext, "Could not find current location!", Toast.LENGTH_SHORT).show();
                    }

                }
            });
        } else {
            Log.e(TAG, "Cant get currentlocation, GoogleApiClient not ready");
            Toast.makeText(baseContext, "Google client not (yet) ready, can't get current location.", Toast.LENGTH_SHORT).show();
        }

    }

    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT);
        builder.addGeofences(mGeofenceList);
        return builder.build();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "Connected to GoogleApiClient");
        if(start_after_boot){
            addGeofence(server_address, server_port, password, username, latitude, longitude, geofence_radius, idx_of_switch, protocol);
        }
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.i(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + connectionResult.getErrorCode());
    }

    private void sendStatus(String message) {
        Intent intent = new Intent(".GoogleApiBuilder");
        intent.putExtra("status", message);
        LocalBroadcastManager mgr = LocalBroadcastManager.getInstance(baseContext);
        mgr.sendBroadcast(intent);
    }

    public GoogleApiClient getGoogleApiClient() {
        return mGoogleApiClient;
    }

    public class LocationServiceNoPermission extends RuntimeException {
    }

    public class GeofenceCompletedListener implements OnCompleteListener {
        @Override
        public void onComplete(Task task) {

            try{
                if (task.isSuccessful() & !start_after_boot) {
                    mGeofencesAdded = !mGeofencesAdded;
                    SharedPreferences.Editor editor = mSharedPreferences.edit();
                    editor.putBoolean(PACKAGENAME + ".GEOFENCES_ADDED_KEY", mGeofencesAdded);
                    editor.putString(PACKAGENAME + ".server_address", server_address);
                    editor.putString(PACKAGENAME + ".server_port", server_port);
                    editor.putString(PACKAGENAME + ".username", username);
                    editor.putString(PACKAGENAME + ".password", password);
                    editor.putString(PACKAGENAME + ".longitude", longitude);
                    editor.putString(PACKAGENAME + ".latitude", latitude);
                    editor.putString(PACKAGENAME + ".geofence_radius", geofence_radius);
                    editor.putString(PACKAGENAME + ".idx_of_switch", idx_of_switch);
                    editor.putString(PACKAGENAME + ".protocol", protocol);

                    editor.apply();
                } else if (!task.isSuccessful()){
                    String errorMessage = "Error while registering a geofence: " + task.getResult();
                    Toast.makeText(baseContext, errorMessage, Toast.LENGTH_SHORT).show();
                    Log.e(TAG, errorMessage);
                }
            } catch (RuntimeExecutionException exception){
                String errorMessage = "Error while registering a geofence: " + exception.getMessage();
                Toast.makeText(baseContext, errorMessage, Toast.LENGTH_SHORT).show();
                Log.e(TAG, errorMessage);
            }

            if(mGeofencesAdded) {
                sendStatus("Added");
            } else {
                sendStatus("Removed");
            }

            Toast.makeText(
                    baseContext,
                    mGeofencesAdded ? "The Geofence was added" : "All Geofences are removed",
                    Toast.LENGTH_SHORT
            ).show();

            start_after_boot = false;
        }
    }
}

package com.zilverline.domofence;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;

public class MainActivity extends ActionBarActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ResultCallback<Status> {

    private static final String TAG = "DomoFence";
    private static final String PACKAGENAME = "com.zilverline.domofence";
    private GoogleApiClient mGoogleApiClient;
    private ArrayList<Geofence> mGeofenceList;
    private boolean mGeofencesAdded;
    private PendingIntent mGeofencePendingIntent;
    private SharedPreferences mSharedPreferences;

    private FloatingActionButton mAddGeofencesButton;
    private FloatingActionButton mRemoveGeofencesButton;
    private EditText mServerAddress, mServerPort, mUsername, mPassword, mLatitude, mLongitude, mGeofenceRadius, mIdxOfSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAddGeofencesButton = (FloatingActionButton) findViewById(R.id.start);
        mRemoveGeofencesButton = (FloatingActionButton) findViewById(R.id.stop);
        mServerAddress = (EditText) findViewById(R.id.server_address);
        mServerPort = (EditText) findViewById(R.id.server_port);
        mUsername = (EditText) findViewById(R.id.username);
        mPassword = (EditText) findViewById(R.id.password);
        mLatitude = (EditText) findViewById(R.id.latitude);
        mLongitude = (EditText) findViewById(R.id.longitude);
        mGeofenceRadius = (EditText) findViewById(R.id.fence_radius);
        mIdxOfSwitch = (EditText) findViewById(R.id.switch_idx);

        mGeofenceList = new ArrayList<Geofence>();
        mGeofencePendingIntent = null;
        mSharedPreferences = getSharedPreferences(PACKAGENAME + ".SHARED_PREFERENCES_NAME",
                MODE_PRIVATE);
        mGeofencesAdded = mSharedPreferences.getBoolean(PACKAGENAME + ".GEOFENCES_ADDED_KEY", false);
        setButtonsEnabledState();

        String server_address = mSharedPreferences.getString(PACKAGENAME + ".server_address","not_found");
        if(!server_address.equals("not_found")){
            mServerAddress.setText(server_address);
        }
        String server_port = mSharedPreferences.getString(PACKAGENAME + ".server_port","not_found");
        if(!server_port.equals("not_found")){
            mServerPort.setText(server_port);
        }
        String username = mSharedPreferences.getString(PACKAGENAME + ".username","not_found");
        if(!username.equals("not_found")){
            mUsername.setText(username);
        }
        String password = mSharedPreferences.getString(PACKAGENAME + ".password","not_found");
        if(!password.equals("not_found")){
            mPassword.setText(password);
        }
        String latitude = mSharedPreferences.getString(PACKAGENAME + ".latitude","not_found");
        if(!latitude.equals("not_found")){
            mLatitude.setText(latitude);
        }
        String longitude = mSharedPreferences.getString(PACKAGENAME + ".longitude","not_found");
        if(!longitude.equals("not_found")){
            mLongitude.setText(longitude);
        }
        String geofence_radius = mSharedPreferences.getString(PACKAGENAME + ".geofence_radius","not_found");
        if(!geofence_radius.equals("not_found")){
            mGeofenceRadius.setText(geofence_radius);
        }
        String idx_of_switch = mSharedPreferences.getString(PACKAGENAME + ".idx_of_switch","not_found");
        if(!idx_of_switch.equals("not_found")){
            mIdxOfSwitch.setText(idx_of_switch);
        }

        buildGoogleApiClient();
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "Connected to GoogleApiClient");
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

    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(mGeofenceList);
        return builder.build();
    }

    public void addGeofencesButtonHandler(View view) {
        if (!mGoogleApiClient.isConnected()) {
            Toast.makeText(this, "GoogleApiClient no yet connected. Try again.", Toast.LENGTH_SHORT).show();
            return;
        } else if(  mServerAddress.getText().length()==0 ||
                    mServerPort.getText().length()==0 ||
                    mPassword.getText().length()==0 ||
                    mUsername.getText().length()==0 ||
                    mLatitude.getText().length()==0 ||
                    mLongitude.getText().length()==0 ||
                    mGeofenceRadius.getText().length()==0 ||
                    mIdxOfSwitch.getText().length()==0) {
            Toast.makeText(this, "Please enter ALL fields before starting a geofence.", Toast.LENGTH_SHORT).show();
            return;
        }


        populateGeofenceList();

        try {
            LocationServices.GeofencingApi.addGeofences(
                    mGoogleApiClient,
                    getGeofencingRequest(),
                    getGeofencePendingIntent()
            ).setResultCallback(this); // Result processed in onResult().
        } catch (SecurityException securityException) {
            logSecurityException(securityException);
        }
    }

    public void removeGeofencesButtonHandler(View view) {
        if (!mGoogleApiClient.isConnected()) {
            Toast.makeText(this, "GoogleApiClient no yet connected. Try again.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            LocationServices.GeofencingApi.removeGeofences(
                    mGoogleApiClient,
                    getGeofencePendingIntent()
            ).setResultCallback(this); // Result processed in onResult().
        } catch (SecurityException securityException) {
            logSecurityException(securityException);
        }
    }

    private void logSecurityException(SecurityException securityException) {
        Log.e(TAG, "Invalid location permission. " +
                "You need to use ACCESS_FINE_LOCATION with geofences", securityException);
    }

    @Override
    public void onResult(Status status) {
        if (status.isSuccess()) {
            mGeofencesAdded = !mGeofencesAdded;
            SharedPreferences.Editor editor = mSharedPreferences.edit();
            editor.putBoolean(PACKAGENAME + ".GEOFENCES_ADDED_KEY", mGeofencesAdded);
            editor.putString(PACKAGENAME + ".server_address", mServerAddress.getText().toString());
            editor.putString(PACKAGENAME + ".server_port", mServerPort.getText().toString());
            editor.putString(PACKAGENAME + ".username", mUsername.getText().toString());
            editor.putString(PACKAGENAME + ".password", mPassword.getText().toString());
            editor.putString(PACKAGENAME + ".longitude", mLongitude.getText().toString());
            editor.putString(PACKAGENAME + ".latitude", mLatitude.getText().toString());
            editor.putString(PACKAGENAME + ".geofence_radius", mGeofenceRadius.getText().toString());
            editor.putString(PACKAGENAME + ".idx_of_switch", mIdxOfSwitch.getText().toString());

            editor.commit();

            setButtonsEnabledState();

            Toast.makeText(
                    this,
                    mGeofencesAdded ? "The Geofence was added" : "All Geofences are removed",
                    Toast.LENGTH_SHORT
            ).show();
        } else {
            String errorMessage;

            switch (status.getStatusCode()) {
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
        }
    }

    private PendingIntent getGeofencePendingIntent() {
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(this, DomoFenceService.class);
        intent.putExtra("server_address", mServerAddress.getText().toString());
        intent.putExtra("server_port", mServerPort.getText().toString());
        intent.putExtra("username", mUsername.getText().toString());
        intent.putExtra("password", mPassword.getText().toString());
        intent.putExtra("switchIdx", mIdxOfSwitch.getText().toString());

        Log.v(TAG, "Sending the Intent");

        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public void populateGeofenceList() {
        Log.v(TAG, "Populating the Geofences: "+mServerAddress.getText().toString()+ ":"+mServerPort.getText().toString());

        Log.v(TAG, "Values: "+Double.valueOf(mLatitude.getText().toString()) + " " +
                Double.valueOf(mLongitude.getText().toString()) + " " +
                Float.valueOf(mGeofenceRadius.getText().toString()) );
        mGeofenceList.add(new Geofence.Builder()
                .setRequestId(mServerAddress.getText().toString())
                .setCircularRegion(
                        Double.valueOf(mLatitude.getText().toString()),
                        Double.valueOf(mLongitude.getText().toString()),
                        Float.valueOf(mGeofenceRadius.getText().toString())
                )
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                        Geofence.GEOFENCE_TRANSITION_EXIT)
                .build());
    }

    private void setButtonsEnabledState() {
        if (mGeofencesAdded) {
            mAddGeofencesButton.setVisibility(View.GONE);
            mRemoveGeofencesButton.setVisibility(View.VISIBLE);
        } else {
            mAddGeofencesButton.setVisibility(View.VISIBLE);
            mRemoveGeofencesButton.setVisibility(View.GONE);
        }
    }
}
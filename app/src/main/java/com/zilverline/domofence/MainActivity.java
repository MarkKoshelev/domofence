package com.zilverline.domofence;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.*;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.getbase.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.SocketTimeoutException;
import java.net.URL;

public class MainActivity extends Activity {

    private static final String TAG = "DomoFence";
    private SharedPreferences mSharedPreferences;
    private boolean mGeofencesAdded;
    private static final String PACKAGENAME = "com.zilverline.domofence";
    private GoogleApiBuilder googleApiBuilder;
    private BroadcastReceiver mBroadcastReceiver;
    private LocalBroadcastManager mgr;

    private FloatingActionButton mAddGeofencesButton;
    private FloatingActionButton mRemoveGeofencesButton;
    private EditText mServerAddress, mServerPort, mUsername, mPassword, mLatitude, mLongitude, mGeofenceRadius, mIdxOfSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        googleApiBuilder = new GoogleApiBuilder();
        googleApiBuilder.initialize(this);

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

        mSharedPreferences = getSharedPreferences(PACKAGENAME + ".SHARED_PREFERENCES_NAME",
                MODE_PRIVATE);
        mGeofencesAdded = mSharedPreferences.getBoolean(PACKAGENAME + ".GEOFENCES_ADDED_KEY", false);

        String server_address = mSharedPreferences.getString(PACKAGENAME + ".server_address", "not_found");
        if(!server_address.equals("not_found")){
            mServerAddress.setText(server_address);
        }
        String server_port = mSharedPreferences.getString(PACKAGENAME + ".server_port", "not_found");
        if(!server_port.equals("not_found")){
            mServerPort.setText(server_port);
        }
        String username = mSharedPreferences.getString(PACKAGENAME + ".username", "not_found");
        if(!username.equals("not_found")){
            mUsername.setText(username);
        }
        String password = mSharedPreferences.getString(PACKAGENAME + ".password", "not_found");
        if(!password.equals("not_found")){
            mPassword.setText(password);
        }
        String latitude = mSharedPreferences.getString(PACKAGENAME + ".latitude", "not_found");
        if(!latitude.equals("not_found")){
            mLatitude.setText(latitude);
        }
        String longitude = mSharedPreferences.getString(PACKAGENAME + ".longitude", "not_found");
        if(!longitude.equals("not_found")){
            mLongitude.setText(longitude);
        }
        String geofence_radius = mSharedPreferences.getString(PACKAGENAME + ".geofence_radius", "not_found");
        if(!geofence_radius.equals("not_found")){
            mGeofenceRadius.setText(geofence_radius);
        }
        String idx_of_switch = mSharedPreferences.getString(PACKAGENAME + ".idx_of_switch", "not_found");
        if(!idx_of_switch.equals("not_found")){
            mIdxOfSwitch.setText(idx_of_switch);
        }

        setButtonsEnabledState();
    }

    @Override
    protected void onStart() {
        Log.v(TAG, "onStart()");
        super.onStart();
        googleApiBuilder.getGoogleApiClient().connect();

        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getStringExtra("status").equals("Added")) {
                    mAddGeofencesButton.setVisibility(View.GONE);
                    mRemoveGeofencesButton.setVisibility(View.VISIBLE);
                } else {
                    mAddGeofencesButton.setVisibility(View.VISIBLE);
                    mRemoveGeofencesButton.setVisibility(View.GONE);
                }
            }
        };

        mgr = LocalBroadcastManager.getInstance(this);
        mgr.registerReceiver(mBroadcastReceiver, new IntentFilter(".GoogleApiBuilder"));
    }

    @Override
    protected void onStop() {
        Log.v(TAG, "onStop()");
        super.onStop();
        googleApiBuilder.getGoogleApiClient().disconnect();
        mgr.unregisterReceiver(mBroadcastReceiver);
    }

    public void addGeofencesButtonHandler(View view) {
        if (!googleApiBuilder.getGoogleApiClient().isConnected()) {
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

        googleApiBuilder.addGeofence(   mServerAddress.getText().toString(),
                                        mServerPort.getText().toString(),
                                        mPassword.getText().toString(),
                                        mUsername.getText().toString(),
                                        mLatitude.getText().toString(),
                                        mLongitude.getText().toString(),
                                        mGeofenceRadius.getText().toString(),
                                        mIdxOfSwitch.getText().toString());
        Log.v(TAG, "Adding geofence");
    }

    public void removeGeofencesButtonHandler(View view) {
        if (!googleApiBuilder.getGoogleApiClient().isConnected()) {
            Toast.makeText(this, "GoogleApiClient no yet connected. Try again.", Toast.LENGTH_SHORT).show();
            return;
        }
        googleApiBuilder.removeGeofence(mServerAddress.getText().toString(),
                                        mServerPort.getText().toString(),
                                        mPassword.getText().toString(),
                                        mUsername.getText().toString(),
                                        mLatitude.getText().toString(),
                                        mLongitude.getText().toString(),
                                        mGeofenceRadius.getText().toString(),
                                        mIdxOfSwitch.getText().toString());
        Log.v(TAG, "Removing geofence");
    }

    private void setButtonsEnabledState() {
        Log.v(TAG, "Reading SET geofence from storage: "+mSharedPreferences.getBoolean(PACKAGENAME + ".GEOFENCES_ADDED_KEY", false));
        if (mGeofencesAdded) {
            mAddGeofencesButton.setVisibility(View.GONE);
            mRemoveGeofencesButton.setVisibility(View.VISIBLE);
        } else {
            mAddGeofencesButton.setVisibility(View.VISIBLE);
            mRemoveGeofencesButton.setVisibility(View.GONE);
        }
    }


    public void testServerConnection(View view) {

        if(  mServerAddress.getText().length()==0 ||
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

        new TestURL().execute("");

    }

    private class TestURL extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            String status = "";
            String requestUrl =    "http://" + mServerAddress.getText().toString() + ":" +
                            mServerPort.getText().toString() +
                            "/json.htm?type=command&param=switchlight&idx=" +
                            mIdxOfSwitch.getText().toString() + "&switchcmd=On";
            Authenticator.setDefault(new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(mUsername.getText().toString(), mPassword.getText().toString().toCharArray());
                }
            });

            HttpURLConnection urlConnection = null;
            try {
                URL url = new URL(requestUrl.replaceAll("\\s",""));
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setReadTimeout(2500);
                urlConnection.setConnectTimeout(3000);

                InputStream in = urlConnection.getInputStream();
                InputStreamReader isw = new InputStreamReader(in);

                int data = isw.read();
                while (data != -1) {
                    char current = (char) data;
                    data = isw.read();
                    status += current;
                }
            } catch (MalformedURLException e) {
                status = "Error when contacting server: "+e.getMessage();
            } catch (SocketTimeoutException e){
                status = "Server did not respond in 3 seconds: "+e.getMessage();
            } catch (IOException e) {
                status = "Error when contacting server: "+e.getMessage();
            } finally {
                urlConnection.disconnect();
            }
            return status;
        }

        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(getApplicationContext(), result, Toast.LENGTH_SHORT).show();
        }

        @Override
        protected void onPreExecute() {}

        @Override
        protected void onProgressUpdate(Void... values) {}
    }


}
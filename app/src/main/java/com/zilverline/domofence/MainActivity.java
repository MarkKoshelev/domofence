package com.zilverline.domofence;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.google.android.gms.common.api.GoogleApiClient;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class MainActivity extends Activity {

    private static final String TAG = "DomoFence";
    private static final int REQUEST_LOCATION_ACCESS = 1977;
    private SharedPreferences mSharedPreferences;
    private boolean mGeofencesAdded, mNotifications, inGeofence;
    private static final String PACKAGENAME = "com.zilverline.domofence";
    private GoogleApiBuilder googleApiBuilder;
    private BroadcastReceiver mBroadcastReceiver;
    private LocalBroadcastManager mgr;

    private FloatingActionButton addGeofencesButton, removeGeofencesButton, toggleNotification, testConnection;
    private EditText mServerAddress, mServerPort, mUsername, mPassword, mLatitude, mLongitude, mGeofenceRadius, mIdxOfSwitch;
    private Spinner mSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        googleApiBuilder = new GoogleApiBuilder();
        try {
            googleApiBuilder.initialize(this);
        } catch (GoogleApiBuilder.LocationServiceNoPermission e) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_ACCESS);
        }

        addGeofencesButton = findViewById(R.id.start);
        removeGeofencesButton = findViewById(R.id.stop);
        toggleNotification = findViewById(R.id.toggleNotifications);
        testConnection = findViewById(R.id.test_url);
        mServerAddress = findViewById(R.id.server_address);
        mServerPort = findViewById(R.id.server_port);
        mUsername = findViewById(R.id.username);
        mPassword = findViewById(R.id.password);
        mLatitude = findViewById(R.id.latitude);
        mLongitude = findViewById(R.id.longitude);
        mGeofenceRadius = findViewById(R.id.fence_radius);
        mIdxOfSwitch = findViewById(R.id.switch_idx);
        mSpinner = findViewById(R.id.spinner);


        FloatingActionsMenu actionsMenu = findViewById(R.id.extra_buttons);
        actionsMenu.setOnFloatingActionsMenuUpdateListener(new FloatingActionsMenu.OnFloatingActionsMenuUpdateListener() {
            @Override
            public void onMenuExpanded() {
                mServerAddress.setEnabled(false);
                mServerPort.setEnabled(false);
                mSpinner.setEnabled(false);
                mUsername.setEnabled(false);
                mPassword.setEnabled(false);
                mLatitude.setEnabled(false);
                mLongitude.setEnabled(false);
                mGeofenceRadius.setEnabled(false);
                mIdxOfSwitch.setEnabled(false);
            }

            @Override
            public void onMenuCollapsed() {
                mServerAddress.setEnabled(true);
                mServerPort.setEnabled(true);
                mSpinner.setEnabled(true);
                mUsername.setEnabled(true);
                mPassword.setEnabled(true);
                mLatitude.setEnabled(true);
                mLongitude.setEnabled(true);
                mGeofenceRadius.setEnabled(true);
                mIdxOfSwitch.setEnabled(true);
            }
        });

        String[] items = new String[]{"http", "https"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, items);
        mSpinner.setAdapter(adapter);

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
        String protocol = mSharedPreferences.getString(PACKAGENAME + ".protocol", "not_found");
        if(protocol.equals("http") || protocol.equals("https")){
            if(protocol.equals("http")){
                mSpinner.setSelection(0);
            } else if (protocol.equals("https")){
                mSpinner.setSelection(1);
            }
        } else {
            SharedPreferences.Editor editor = mSharedPreferences.edit();
            editor.putString(PACKAGENAME + ".protocol", "http");
            editor.apply();
        }

        mNotifications = mSharedPreferences.getBoolean(PACKAGENAME + ".notifications", true);
        Log.v(TAG, "toggleNotifications " + mNotifications);

        if (mNotifications) {
            toggleNotification.setIcon(R.drawable.ic_notifications_off_black_48dp);
            toggleNotification.setTitle("Disable notifications");
        } else {
            toggleNotification.setTitle("Enable notifications");
            toggleNotification.setIcon(R.drawable.ic_notifications_black_48dp);
        }

        inGeofence = mSharedPreferences.getBoolean(PACKAGENAME + ".inGeofence", true);

        if(inGeofence){
            testConnection.setTitle("Test connection (ON)");
        } else {
            testConnection.setTitle("Test connection (OFF)");
        }

        setButtonsEnabledState();
    }

    @Override
    protected void onStart() {
        Log.v(TAG, "onStart()");
        super.onStart();
        GoogleApiClient apiClient = googleApiBuilder.getGoogleApiClient();

        if (apiClient != null) {
            apiClient.connect();
        }

        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getStringExtra("status").equals("Added")) {
                    addGeofencesButton.setVisibility(View.GONE);
                    removeGeofencesButton.setVisibility(View.VISIBLE);
                } else {
                    addGeofencesButton.setVisibility(View.VISIBLE);
                    removeGeofencesButton.setVisibility(View.GONE);
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
        if(googleApiBuilder != null && googleApiBuilder.getGoogleApiClient() != null){
            googleApiBuilder.getGoogleApiClient().disconnect();
        }
        if(mgr != null){
            mgr.unregisterReceiver(mBroadcastReceiver);
        }
    }

    public void addGeofencesButtonHandler(View view) {
        if (!googleApiBuilder.getGoogleApiClient().isConnected()) {
            Toast.makeText(this, "GoogleApiClient no yet connected. Try again.", Toast.LENGTH_SHORT).show();
            return;
        } else if(  mServerAddress.getText().length()==0 ||
                    //mServerPort.getText().length()==0 ||
                    mPassword.getText().length()==0 ||
                    mUsername.getText().length()==0 ||
                    mLatitude.getText().length()==0 ||
                    mLongitude.getText().length()==0 ||
                    mGeofenceRadius.getText().length()==0 ||
                    mIdxOfSwitch.getText().length()==0) {
                Toast.makeText(this, "Please enter ALL fields before starting a geofence.", Toast.LENGTH_SHORT).show();
                return;
        }

        googleApiBuilder.addGeofence(mServerAddress.getText().toString(),
                mServerPort.getText().toString(),
                mPassword.getText().toString(),
                mUsername.getText().toString(),
                mLatitude.getText().toString(),
                mLongitude.getText().toString(),
                mGeofenceRadius.getText().toString(),
                mIdxOfSwitch.getText().toString(),
                mSpinner.getSelectedItem().toString());
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
                mIdxOfSwitch.getText().toString(),
                mSpinner.getSelectedItem().toString());
        Log.v(TAG, "Removing geofence");
    }

    private void setButtonsEnabledState() {
        Log.v(TAG, "Reading SET geofence from storage: " + mSharedPreferences.getBoolean(PACKAGENAME + ".GEOFENCES_ADDED_KEY", false));
        if (mGeofencesAdded) {
            addGeofencesButton.setVisibility(View.GONE);
            removeGeofencesButton.setVisibility(View.VISIBLE);
        } else {
            addGeofencesButton.setVisibility(View.VISIBLE);
            removeGeofencesButton.setVisibility(View.GONE);
        }
    }


    public void testServerConnection(View view) {

        if(  mServerAddress.getText().length()==0 ||
                //mServerPort.getText().length()==0 ||
                mPassword.getText().length()==0 ||
                mUsername.getText().length()==0 ||
                mLatitude.getText().length()==0 ||
                mLongitude.getText().length()==0 ||
                mGeofenceRadius.getText().length()==0 ||
                mIdxOfSwitch.getText().length()==0) {
            Toast.makeText(this, "Please enter ALL fields before testing the connection.", Toast.LENGTH_SHORT).show();
            return;
        }

        String optionalPort = (mServerPort.getText().toString().matches("\\d{1,5}")) ? ":" + mServerPort.getText().toString() : "";

        boolean inGeofence = mSharedPreferences.getBoolean(PACKAGENAME + ".inGeofence", true);
        String state = "Off";
        if(inGeofence){
            state = "On";
        }

        new TestURL().execute(  mSpinner.getSelectedItem().toString() + "://" +
                                mServerAddress.getText().toString() + optionalPort +
                                "/json.htm?type=command&param=switchlight&idx=" +
                                mIdxOfSwitch.getText().toString() + "&switchcmd=" + state);
    }

    public void getCurrentLocation(View view) {

        if(googleApiBuilder!= null){

            Location currentLocation = googleApiBuilder.getCurrentLocation();

            if(currentLocation!=null){
                mLatitude.setText(String.valueOf(currentLocation.getLatitude()));
                mLongitude.setText((String.valueOf(currentLocation.getLongitude())));

                Toast.makeText(this, "Found and set current location!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Could not find your current location.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Could not find your current location.", Toast.LENGTH_SHORT).show();
        }
    }


    public void toggleNotifications(View view) {
        boolean notifications = mSharedPreferences.getBoolean(PACKAGENAME + ".notifications", true);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        Log.v(TAG, "Stored Notifications: " + notifications);

        if (notifications) {
            editor.putBoolean(PACKAGENAME + ".notifications", false);
            toggleNotification.setTitle("Enable notifications");
            toggleNotification.setIcon(R.drawable.ic_notifications_black_48dp);
        } else {
            editor.putBoolean(PACKAGENAME + ".notifications", true);
            toggleNotification.setIcon(R.drawable.ic_notifications_off_black_48dp);
            toggleNotification.setTitle("Disable notifications");
        }

        editor.apply();

        boolean notifications1 = mSharedPreferences.getBoolean(PACKAGENAME + ".notifications", true);
        Log.v(TAG, "Stored Notifications after: " + notifications1);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if (requestCode == REQUEST_LOCATION_ACCESS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                googleApiBuilder.initialize(this);
                googleApiBuilder.getGoogleApiClient().connect();
            } else {
                Toast.makeText(this, "We can not work without access to your location", Toast.LENGTH_LONG).show();
            }
        }
    }



    private class TestURL extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            StringBuilder status = new StringBuilder();
            String requestUrl = params[0];

            Authenticator.setDefault(new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(mUsername.getText().toString(), mPassword.getText().toString().toCharArray());
                }
            });


            URLConnection urlConnection;

            try {
                URL url = new URL(requestUrl.replaceAll("\\s",""));

                if (url.getProtocol().toLowerCase().equals("https")) {
                    Log.v(TAG, "Found https");
                    urlConnection = url.openConnection();
                    HttpsURLConnection httpsUrlConnection = (HttpsURLConnection) urlConnection;
                    SSLSocketFactory sslSocketFactory = createSslSocketFactory();

                    httpsUrlConnection.setSSLSocketFactory(sslSocketFactory);
                    httpsUrlConnection.setHostnameVerifier(new HostnameVerifier() {
                        @Override
                        public boolean verify(String hostname, SSLSession session) {
                            return true;
                        }
                    });
                    urlConnection = httpsUrlConnection;
                } else {
                    urlConnection = url.openConnection();
                }

                urlConnection.setReadTimeout(2500);
                urlConnection.setConnectTimeout(3000);

                InputStream in = urlConnection.getInputStream();
                InputStreamReader isw = new InputStreamReader(in);

                int data = isw.read();
                while (data != -1) {
                    char current = (char) data;
                    data = isw.read();
                    status.append(current);
                }
            } catch (MalformedURLException e) {
                status = new StringBuilder("Error in URL: " + e.getMessage());
                e.printStackTrace();
            } catch (Exception e) {
                status = new StringBuilder("Error: " + e.getMessage());
                e.printStackTrace();
            }
            return status.toString();
        }

        private SSLSocketFactory createSslSocketFactory() throws Exception {
            TrustManager[] byPassTrustManagers = new TrustManager[] { new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
                public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                public void checkServerTrusted(X509Certificate[] chain, String authType) {}
            } };
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, byPassTrustManagers, new SecureRandom());

            return sslContext.getSocketFactory();
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
package com.zilverline.domofence;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class NetworkChangeReceiver extends BroadcastReceiver {

    private static final String PACKAGENAME = "com.zilverline.domofence";

    private String TAG = "NetworkChangeReceiver";
    private Context baseContext;
    private SharedPreferences mSharedPreferences;
    private String url, username, password;

    @Override
    public void onReceive(final Context context, final Intent intent) {
        baseContext = context;
        final ConnectivityManager connMgr = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        mSharedPreferences = baseContext.getSharedPreferences(PACKAGENAME + ".SHARED_PREFERENCES_NAME",
                Context.MODE_PRIVATE);

        NetworkInfo netInfo = connMgr.getActiveNetworkInfo();

        if(intent.getAction().equals("com.zilverline.domofence.DomoFenceService")){

            url = intent.getStringExtra("url");
            username = intent.getStringExtra("username");
            password = intent.getStringExtra("password");


            Log.d("NCR", "DomoFenceService Action Received");
            if(isOnline()){
                new SendHttpRequest().execute("");
            } else {
                storeState(url);
            }
        }

        if (netInfo != null && netInfo.isConnected()) {
            Log.d("NCR", "Interwebs available. Retrieve state?: "+retrieveState());

            if(retrieveState()) {
                new SendHttpRequest().execute("");
            }
        }

    }

    private boolean retrieveState() {

        boolean postponed = mSharedPreferences.getBoolean(PACKAGENAME + ".POSTPONED", false);

        if(postponed){
            url = mSharedPreferences.getString(PACKAGENAME + ".url","not_found");
            username = mSharedPreferences.getString(PACKAGENAME + ".username","not_found");
            password = mSharedPreferences.getString(PACKAGENAME + ".password","not_found");
        }

        return postponed;
    }

    private void storeState(String url) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(PACKAGENAME + ".url", url);
        editor.putBoolean(PACKAGENAME + ".POSTPONED", true);

        editor.apply();
    }

    private void resetState(){
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(PACKAGENAME + ".url", "");
        editor.putBoolean(PACKAGENAME + ".POSTPONED", false);

        editor.apply();

    }


    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) baseContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        Log.v(TAG, "Online?: " + (netInfo != null && netInfo.isConnected()));
        return netInfo != null && netInfo.isConnected();
    }

    private class SendHttpRequest extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... strings) {

            Log.v(TAG, "getToServer: " + url);
            resetState();

            Authenticator.setDefault(new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password.toCharArray());
                }
            });

            URLConnection urlConnection;
            try {
                URL realUrl = new URL(url);

                if (realUrl.getProtocol().toLowerCase().equals("https")) {
                    Log.v(TAG, "Found https");
                    urlConnection = realUrl.openConnection();
                    HttpsURLConnection httpsUrlConnection = (HttpsURLConnection) urlConnection;
                    SSLSocketFactory sslSocketFactory = createSslSocketFactory();

                    httpsUrlConnection.setSSLSocketFactory(sslSocketFactory);

                    urlConnection = httpsUrlConnection;
                } else {
                    urlConnection = realUrl.openConnection();
                }

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
                Log.v(TAG, "Response: " + status);

            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
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
    }
}

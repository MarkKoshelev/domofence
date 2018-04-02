package com.zilverline.domofence.scheduler;

import android.support.annotation.NonNull;
import android.util.Log;

import com.evernote.android.job.Job;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Authenticator;
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

public class JobScheduler extends Job {

    public static final String TAG = "JobScheduler";

    @NonNull
    @Override
    protected Result onRunJob(@NonNull Params params) {

        final String url = params.getExtras().getString("url", "not_found");
        final String username = params.getExtras().getString("username", "not_found");
        final String password = params.getExtras().getString("password", "not_found");

        Log.i(TAG, "Scheduling a call: " + url);

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
                httpsUrlConnection.setHostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, SSLSession session) {
                        return true;
                    }
                });

                urlConnection = httpsUrlConnection;
            } else {
                urlConnection = realUrl.openConnection();
            }

            urlConnection.setReadTimeout(5000);
            urlConnection.setConnectTimeout(5000);

            InputStream in = urlConnection.getInputStream();
            InputStreamReader isw = new InputStreamReader(in);
            StringBuilder status = new StringBuilder();

            int data = isw.read();
            while (data != -1) {
                char current = (char) data;
                data = isw.read();
                status.append(current);
            }
            Log.v(TAG, "Response: " + status);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return Result.SUCCESS;
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

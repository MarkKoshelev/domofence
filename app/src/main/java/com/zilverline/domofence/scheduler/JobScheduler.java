package com.zilverline.domofence.scheduler;

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
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import com.evernote.android.job.Job;

public class JobScheduler extends Job {

  public static final String TAG = "JobScheduler";
  private static final String PACKAGENAME = "com.zilverline.domofence";

  @Override
  protected Result onRunJob(Params params) {

    final String url = params.getExtras().getString("url", "not_found");
    final String username = params.getExtras().getString("username", "not_found");
    final String password = params.getExtras().getString("password", "not_found");
    final String overridden_server_address = params.getExtras().getString("overridden_server_address", "not_found");

    Log.i(TAG, "Scheduling a call: " + url);

    Authenticator.setDefault(new Authenticator() {
      protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(username, password.toCharArray());
      }
    });

    URLConnection urlConnection;
    try {
      URL realUrl = new URL(url);
      urlConnection = realUrl.openConnection();
      urlConnection.setReadTimeout(5000);
      urlConnection.setConnectTimeout(5000);

      if (realUrl.getProtocol().toLowerCase().equals("https")) {
        Log.v(TAG, "Found https");
        HttpsURLConnection httpsUrlConnection = (HttpsURLConnection) urlConnection;
        SSLSocketFactory sslSocketFactory = createSslSocketFactory();

        httpsUrlConnection.setSSLSocketFactory(sslSocketFactory);
        httpsUrlConnection.setHostnameVerifier(new HostnameVerifier() {
          @Override
          public boolean verify(String hostname, SSLSession session) {
            HostnameVerifier hv = HttpsURLConnection.getDefaultHostnameVerifier();
            if (hv.verify(hostname, session) || hostname.equals(overridden_server_address)) {
              return true;
            } else if (overridden_server_address.equals(session.getPeerHost())){
              Log.d(TAG, "Overridden server address verification succeeded");
              return true;
            } else {
              Log.d(TAG, "Hostname verification failed");
              Toast.makeText(getContext(), "Hostname verification failed. Using: " + session.getPeerHost() + " as future check"  , Toast.LENGTH_SHORT).show();
              overrideHostnameVerification(session.getPeerHost());

              return true;
            }
          }
        });

        urlConnection = httpsUrlConnection;
      }

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

  private void overrideHostnameVerification(String overridden_server_address){
    SharedPreferences sharedPreferences = getContext().getSharedPreferences(PACKAGENAME + ".SHARED_PREFERENCES_NAME",
        Context.MODE_PRIVATE);
    SharedPreferences.Editor editor = sharedPreferences.edit();
    editor.putString(PACKAGENAME + ".overridden_server_address", overridden_server_address);
    editor.apply();
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

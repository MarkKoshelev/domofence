package com.zilverline.domofence;

import android.app.Application;

import com.evernote.android.job.JobManager;
import com.zilverline.domofence.scheduler.NetworkJobCreator;

public class DomoFenceApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        JobManager.create(this).addJobCreator(new NetworkJobCreator());
    }
}

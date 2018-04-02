package com.zilverline.domofence.scheduler;

import android.support.annotation.NonNull;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobCreator;

public class NetworkJobCreator implements JobCreator {

    @Override
    public Job create(@NonNull String tag) {
        switch (tag) {
            case JobScheduler.TAG:
                return new JobScheduler();
            default:
                return null;
        }
    }

}

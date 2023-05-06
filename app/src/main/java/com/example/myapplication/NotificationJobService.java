package com.example.myapplication;

import android.app.job.JobParameters;
import android.app.job.JobService;

public class NotificationJobService extends JobService {
    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        new NotificationHandler(getApplicationContext())
                .send("Vásároljunk valamit!");

        return false;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return true;
    }
}

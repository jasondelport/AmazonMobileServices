package com.jasondelport.nobelprize.nobelprizelaureates;

import android.app.Application;

import timber.log.Timber;

/**
 * Created by jasondelport on 12/05/16.
 */
public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Timber.plant(new Timber.DebugTree());
    }
}

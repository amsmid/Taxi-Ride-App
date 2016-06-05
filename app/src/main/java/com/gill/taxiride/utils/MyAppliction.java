package com.gill.taxiride.utils;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;

import com.gill.taxiride.R;

import uk.co.chrisjenx.calligraphy.CalligraphyConfig;


public class MyAppliction extends Application{

    @Override
    public void onCreate() {
        super.onCreate();

        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath(GeneralValues.FONT_FILE)
                .setFontAttrId(R.attr.fontPath)
                .build()
        );
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }
}

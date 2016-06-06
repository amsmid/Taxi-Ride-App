package com.gill.taxiride;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.gill.taxiride.utils.GeneralValues;
import com.gill.taxiride.utils.TinyDB;
import com.gill.taxiride.utils.Utils;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class SplashActivity extends AppCompatActivity {

    ImageView logo;
    TextView title;
    TinyDB tinyDB;
    Context mContext;
    private LocationListener locationListener;
    private LocationManager locationManager;
    final int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 45;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        /*Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);*/

        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.flags |= WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;

        mContext=SplashActivity.this;
        tinyDB=new TinyDB(mContext);

        logo=(ImageView)findViewById(R.id.logo);
        title=(TextView)findViewById(R.id.title);

        //animation on logo
        logo.setDrawingCacheEnabled(true);
        Animation anim_zoom = AnimationUtils.loadAnimation(this, R.anim.zoom_in);
        AnimationSet growShrink = new AnimationSet(true);
        growShrink.addAnimation(anim_zoom);
        logo.startAnimation(growShrink);

        final Animation animationFadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        title.setDrawingCacheEnabled(true);

        anim_zoom.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                title.setVisibility(View.VISIBLE);
                title.startAnimation(animationFadeIn);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        if(check_GPS()){
            if(Utils.isNetworkConnected(getApplicationContext())){
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    askPermissions();
                } else {
                    fetch_location();
                }
            }else{
                Utils.showToast(getApplicationContext(),getString(R.string.no_internet_connection));
            }
        }

    }

    //stay 1500 milliseconds here before goto next screen
    public void start_timer(){
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(getApplicationContext(), MapsActivity.class));
                overridePendingTransition(R.anim.to_leftin, R.anim.to_leftout);
                SplashActivity.this.finish();
            }
        }, 100);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            if (locationManager != null) {
                locationManager.removeUpdates(locationListener);
            }
        } catch (SecurityException e) {

        }
    }

    public boolean check_GPS(){
        if (Utils.isGpsEnabled(mContext)) {
            return true;
        } else {
            Utils.showDialog(mContext, getString(R.string.enable_gps), getString(R.string.check_location_services), getString(R.string.settings), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));

                }
            }, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Utils.showToast(mContext,getString(R.string.gps_required));
                }
            }, getString(R.string.back), null, null);

            return false;
        }

    }

    private void fetch_location() {
        locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                String latitide=String.valueOf(location.getLatitude());
                String longitude=String.valueOf(location.getLongitude());
                if(latitide.equalsIgnoreCase("")||longitude.equalsIgnoreCase("")){
                    Utils.showToast(mContext, getString(R.string.can_not_able_find_location));
                }else{
                    tinyDB.putDouble(GeneralValues.CURRENT_LATITUDE,location.getLatitude());
                    tinyDB.putDouble(GeneralValues.CURRENT_LONGITUDE,location.getLongitude());
                    Utils.show_log("Location : Lat = "+tinyDB.getDouble(GeneralValues.CURRENT_LATITUDE,0)+" Lng = "+tinyDB.getDouble(GeneralValues.CURRENT_LONGITUDE,0));
                    start_timer();
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };
        try{
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        }catch (SecurityException e){

        }
    }

    public void askPermissions() {
        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(SplashActivity.this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
        } else {
            fetch_location();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    fetch_location();
                }else{
                    Utils.showToast(mContext,getString(R.string.permission_required));
                }
                break;
        }
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
    }
}

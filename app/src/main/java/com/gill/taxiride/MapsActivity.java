package com.gill.taxiride;

import android.app.Dialog;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gill.taxiride.utils.GeneralValues;
import com.gill.taxiride.utils.TinyDB;
import com.gill.taxiride.utils.Utils;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.uber.sdk.android.core.UberSdk;
import com.uber.sdk.android.rides.RideParameters;
import com.uber.sdk.android.rides.RideRequestButton;
import com.uber.sdk.core.auth.Scope;
import com.uber.sdk.rides.client.SessionConfiguration;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,AdapterView.OnItemClickListener {

    private GoogleMap mMap;
    Context mContext;
    TinyDB tinyDB;
    AutoCompleteTextView et_pickup,et_destination;
    TextView tv_pickup_time,tv_set_pick_up,tv_estimate_fare,tv_estimate_time;
    LinearLayout ll_pickup,ll_book;
    private RideRequestButton uberButton;
    Dialog dialog;
    double pick_lat=0,pick_long=0,des_lat=0,des_long=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.flags |= WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;

        mContext=MapsActivity.this;
        tinyDB=new TinyDB(mContext);
        dialog=Utils.get_progressDialog(mContext);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        et_pickup=(AutoCompleteTextView)findViewById(R.id.et_pickup);
        et_destination=(AutoCompleteTextView)findViewById(R.id.et_destination);
        tv_pickup_time=(TextView)findViewById(R.id.tv_pickup_time);
        tv_set_pick_up=(TextView)findViewById(R.id.tv_set_pick_up);
        ll_pickup=(LinearLayout)findViewById(R.id.ll_pickup);
        ll_book=(LinearLayout)findViewById(R.id.ll_book);
        tv_estimate_fare=(TextView)findViewById(R.id.tv_estimate_fare);
        tv_estimate_time=(TextView)findViewById(R.id.tv_estimate_time);
        uberButton = (RideRequestButton) findViewById(R.id.tv_book_uber_default);

        SessionConfiguration config = new SessionConfiguration.Builder()
                .setClientId(GeneralValues.UBER_CLIENT_ID) //This is necessary
                .setEnvironment(SessionConfiguration.Environment.SANDBOX) //Useful for testing your app in the sandbox environment
                .setScopes(Arrays.asList(Scope.PROFILE, Scope.RIDE_WIDGETS)) //Your scopes for authentication here
                .build();

        UberSdk.initialize(config);


        et_pickup.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String str = s.toString();
                if (str.length() > 0 && str.startsWith(" ")) {
                    et_pickup.setText(str.trim());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        et_destination.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String str = s.toString();
                if (str.length() > 0 && str.startsWith(" ")) {
                    et_destination.setText(str.trim());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        tv_set_pick_up.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(et_pickup.getText().toString().length()>0){
                    et_destination.setVisibility(View.VISIBLE);
                    ll_pickup.setVisibility(View.GONE);
                }else{
                    Utils.showToast(mContext,getString(R.string.enter_pickup_location));
                }
            }
        });

        et_pickup.setAdapter(new GooglePlacesAutocompleteAdapter(this, R.layout.search_list_item));
        et_pickup.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Utils.hideKeyboard(mContext,getCurrentFocus());
                get_latlong(et_pickup.getText().toString(),"pick");
            }
        });

        et_destination.setAdapter(new GooglePlacesAutocompleteAdapter(this, R.layout.search_list_item));
        et_destination.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Utils.hideKeyboard(mContext,getCurrentFocus());
                get_latlong(et_destination.getText().toString(),"des");
            }
        });

        api_get_uber_time("current");
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }
    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        LatLng sydney = new LatLng(tinyDB.getDouble(GeneralValues.CURRENT_LATITUDE,0),
                tinyDB.getDouble(GeneralValues.CURRENT_LONGITUDE,0));
        mMap.addMarker(new MarkerOptions().position(sydney).title(getString(R.string.your_current_location))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(12));
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Utils.show_log(""+adapterView.getItemAtPosition(position)+" id = "+view.getId());
    }

    public static ArrayList<String> autocomplete(String input) {
        ArrayList<String> resultList = null;

        HttpURLConnection conn = null;
        StringBuilder jsonResults = new StringBuilder();
        try {
            StringBuilder sb = new StringBuilder(GeneralValues.PLACES_API_BASE);
            sb.append("?key=" + GeneralValues.GOOGLE_SERVER_KEY);
            sb.append("&input=" + URLEncoder.encode(input, "utf8"));

            URL url = new URL(sb.toString());

            System.out.println("URL: " + url);
            conn = (HttpURLConnection) url.openConnection();
            InputStreamReader in = new InputStreamReader(conn.getInputStream());

            // Load the results into a StringBuilder
            int read;
            char[] buff = new char[1024];
            while ((read = in.read(buff)) != -1) {
                jsonResults.append(buff, 0, read);
            }
        } catch (MalformedURLException e) {
            Log.e("Error", "Error processing Places API URL", e);
            return resultList;
        } catch (IOException e) {
            Log.e("IO Exp", "Error connecting to Places API", e);
            return resultList;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        try {
            // Create a JSON object hierarchy from the results
            JSONObject jsonObj = new JSONObject(jsonResults.toString());
            JSONArray predsJsonArray = jsonObj.getJSONArray("predictions");
            // Extract the Place descriptions from the results
            resultList = new ArrayList<String>(predsJsonArray.length());
            for (int i = 0; i < predsJsonArray.length(); i++) {
                resultList.add(predsJsonArray.getJSONObject(i).getString("description"));
            }
        } catch (JSONException e) {
            Log.e("JSON EXP", "Cannot process JSON results", e);
        }
        return resultList;
    }

    class GooglePlacesAutocompleteAdapter extends ArrayAdapter<String> implements Filterable {
        private ArrayList<String> resultList;

        public GooglePlacesAutocompleteAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);
        }

        @Override
        public int getCount() {
            return resultList.size();
        }

        @Override
        public String getItem(int index) {
            return resultList.get(index);
        }

        @Override
        public Filter getFilter() {
            Filter filter = new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults filterResults = new FilterResults();
                    if (constraint != null) {
                        // Retrieve the autocomplete results.
                        resultList = autocomplete(constraint.toString());
                        // Assign the data to the FilterResults
                        filterResults.values = resultList;
                        filterResults.count = resultList.size();
                    }
                    return filterResults;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    if (results != null && results.count > 0) {
                        notifyDataSetChanged();
                    } else {
                        notifyDataSetInvalidated();
                    }
                }
            };
            return filter;
        }
    }

    public void get_latlong(final String place, final String check){
        dialog.show();
        if(check.equals("pick")){
            pick_lat=0;
            pick_long=0;
        }else{
            des_lat=0;
            des_long=0;
        }
        new AsyncTask<String, String, String>() {
            @Override
            protected String doInBackground(String... params) {
                if(Geocoder.isPresent()){
                    try {
                        Geocoder gc = new Geocoder(mContext);
                        List<Address> addresses= gc.getFromLocationName(place, 5); // get the found Address Objects
                        for(Address a : addresses){
                            if(a.hasLatitude() && a.hasLongitude()){
                                if(check.equals("pick")){
                                    pick_lat=a.getLatitude();
                                    pick_long=a.getLongitude();
                                }else{
                                    des_lat=a.getLatitude();
                                    des_long=a.getLongitude();
                                }

                                set_rideParams();
                            }
                        }
                    } catch (IOException e) {
                        // handle the exception
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                if(check.equals("pick")){
                    if(pick_lat==0||pick_long==0){
                        Utils.showToast(mContext,getString(R.string.can_not_find_coordinates));
                    }else{
                        Utils.show_log("Location : "+pick_lat+","+pick_long);
                        mMap.clear();
                        LatLng pick_loc = new LatLng(pick_lat,pick_long);
                        mMap.addMarker(new MarkerOptions().position(pick_loc).title(place).
                                icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                        mMap.moveCamera(CameraUpdateFactory.newLatLng(pick_loc));
                        mMap.animateCamera(CameraUpdateFactory.zoomTo(12));

                        api_get_uber_time("selected");
                    }
                }else{
                    if(des_lat==0||des_long==0){
                        Utils.showToast(mContext,getString(R.string.can_not_find_coordinates));
                    }else{
                        Utils.show_log("Location : "+des_lat+","+des_long);
                        api_get_uber_price();
                    }
                }
            }
        }.execute();
    }

    public void api_get_uber_time(final String check){
        tv_pickup_time.setText("");
        ll_book.setVisibility(View.GONE);
        ll_pickup.setVisibility(View.VISIBLE);
        et_destination.setText("");
        et_destination.setVisibility(View.GONE);
        des_lat=0;
        des_long=0;

        String complete_url="";
        if(check.equals("current")){
            dialog.show();
            complete_url="&start_latitude="+tinyDB.getDouble(GeneralValues.CURRENT_LATITUDE,0)+
                    "&start_longitude="+tinyDB.getDouble(GeneralValues.CURRENT_LONGITUDE,0);
        }else{
            complete_url="&start_latitude="+pick_lat+"&start_longitude="+pick_long;
        }
        Call<ResponseBody> call = Utils.requestApi_Default().requestJson_simple(GeneralValues.UBER_TIME_URL+complete_url);
        Utils.show_log("url = "+call.request().url());

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    String jsonResponse = response.body().string();
                    Log.e("res = ",jsonResponse);

                    JSONObject result = new JSONObject(jsonResponse);
                    JSONArray list_arr=new JSONArray(result.getString("times"));
                    String time="";
                    if(list_arr!=null&&list_arr.length()>0){
                        for(int i=0;i<list_arr.length();i++){
                            JSONObject full_data = list_arr.getJSONObject(i);
                            if(full_data.getString("display_name").equals("uberX")){
                                time=full_data.getString("estimate");
                            }
                        }
                        if(time.equals("")){
                            Utils.showToast(mContext,getString(R.string.not_found_time));
                        }else{
                            if(check.equals("current")){
                                tv_pickup_time.setText(Integer.parseInt(time)/60+" Minutes");
                            }else{
                                ll_book.setVisibility(View.GONE);
                                ll_pickup.setVisibility(View.VISIBLE);
                                et_destination.setText("");
                                et_destination.setVisibility(View.GONE);
                                des_lat=0;
                                des_long=0;
                                tv_set_pick_up.setBackgroundResource(R.drawable.custom_green_selector);
                                tv_pickup_time.setText(Integer.parseInt(time)/60+" Minutes");
                            }
                        }
                        try{
                            dialog.dismiss();
                        }catch (Exception e){

                        }
                    }else{
                        Utils.showToast(mContext,getString(R.string.not_found_time));
                        try{
                            dialog.dismiss();
                        }catch (Exception e){

                        }
                    }
                } catch (Exception e) {
                    Utils.showToast(mContext,getString(R.string.server_error));
                    Log.e("exception", "" + e.getMessage());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Utils.showToast(mContext, getString(R.string.server_not_responding));
                try{
                    dialog.dismiss();
                }catch (Exception e){

                }
            }
        });
    }

    public void set_rideParams() {

        RideParameters rideParameters = new RideParameters.Builder()
                .setProductId(GeneralValues.UBER_PRODUCT_ID)
                .setPickupLocation(pick_lat, pick_long, et_pickup.getText().toString(),
                        et_pickup.getText().toString())
                .setDropoffLocation(des_lat, des_long, et_destination.getText().toString(),
                        et_destination.getText().toString())
                .build();

        // This button demonstrates deep-linking to the Uber app (default button behavior).
        uberButton.setRideParameters(rideParameters);
    }

    public void api_get_uber_price(){
        tv_estimate_fare.setText("");
        tv_estimate_time.setText("");
        Call<ResponseBody> call = Utils.requestApi_Default().requestJson_simple(
                GeneralValues.UBER_PRICE_URL+"&start_latitude="
                        +pick_lat+"&start_longitude="+pick_long+
                        "&end_latitude="+des_lat+
                        "&end_longitude="+des_long);
        Utils.show_log("url = "+call.request().url());

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    try{
                        dialog.dismiss();
                    }catch (Exception e){

                    }
                    String jsonResponse = response.body().string();
                    Log.e("res = ",jsonResponse);

                    JSONObject result = new JSONObject(jsonResponse);
                    JSONArray list_arr=new JSONArray(result.getString("prices"));
                    String time="",price="";
                    if(list_arr!=null&&list_arr.length()>0){
                        for(int i=0;i<list_arr.length();i++){
                            JSONObject full_data = list_arr.getJSONObject(i);
                            if(full_data.getString("display_name").equals("uberX")){
                                time=full_data.getString("duration");
                                price=full_data.getString("estimate");
                            }
                        }
                        if(time.equals("")&&price.equals("")){
                            Utils.showToast(mContext,getString(R.string.not_found_fare));
                        }else{
                            ll_book.setVisibility(View.VISIBLE);
                            tv_estimate_fare.setText(price);
                            tv_estimate_time.setText(Integer.parseInt(time)/60+" Minutes");
                        }
                        try{
                            dialog.dismiss();
                        }catch (Exception e){

                        }
                    }else{
                        Utils.showToast(mContext,getString(R.string.not_found_fare));
                        try{
                            dialog.dismiss();
                        }catch (Exception e){

                        }
                    }
                } catch (Exception e) {
                    Utils.showToast(getApplicationContext(),getString(R.string.server_error));
                    Log.e("exception", "" + e.getMessage());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Utils.showToast(getApplicationContext(), getString(R.string.server_not_responding));
                try{
                    dialog.dismiss();
                }catch (Exception e){

                }
            }
        });
    }
}

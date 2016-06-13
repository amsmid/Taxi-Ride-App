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
    AutoCompleteTextView pickUpLocationView, destinationView;
    TextView pickUpTimeTextView, setPickUpTimeTextView, fareEstimateTextView, timeEstimateTextView;
    LinearLayout pickUpLayout, requestUberLayout;
    private RideRequestButton uberButton;
    Dialog dialog;
    double pickUpLatitude = 0, pickUpLongitude = 0, destinationLatitude = 0, destinationLongitude = 0;

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

        pickUpLocationView =(AutoCompleteTextView)findViewById(R.id.et_pickup);
        destinationView =(AutoCompleteTextView)findViewById(R.id.et_destination);
        pickUpTimeTextView =(TextView)findViewById(R.id.tv_pickup_time);
        setPickUpTimeTextView =(TextView)findViewById(R.id.tv_set_pick_up);
        pickUpLayout =(LinearLayout)findViewById(R.id.ll_pickup);
        requestUberLayout =(LinearLayout)findViewById(R.id.ll_book);
        fareEstimateTextView =(TextView)findViewById(R.id.tv_estimate_fare);
        timeEstimateTextView =(TextView)findViewById(R.id.tv_estimate_time);
        uberButton = (RideRequestButton) findViewById(R.id.tv_book_uber_default);

        SessionConfiguration config = new SessionConfiguration.Builder()
                .setClientId(GeneralValues.UBER_CLIENT_ID) //This is necessary
                .setEnvironment(SessionConfiguration.Environment.SANDBOX) //Useful for testing your app in the sandbox environment
                .setScopes(Arrays.asList(Scope.PROFILE, Scope.RIDE_WIDGETS)) //Your scopes for authentication here
                .build();

        UberSdk.initialize(config);


        pickUpLocationView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String str = s.toString();
                if (str.length() > 0 && str.startsWith(" ")) {
                    pickUpLocationView.setText(str.trim());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        destinationView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String str = s.toString();
                if (str.length() > 0 && str.startsWith(" ")) {
                    destinationView.setText(str.trim());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        setPickUpTimeTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(pickUpLocationView.getText().toString().length()>0){
                    destinationView.setVisibility(View.VISIBLE);
                    pickUpLayout.setVisibility(View.GONE);
                }else{
                    Utils.showToast(mContext,getString(R.string.enter_pickup_location));
                }
            }
        });

        pickUpLocationView.setAdapter(new GooglePlacesAutocompleteAdapter(this, R.layout.search_list_item));
        pickUpLocationView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Utils.hideKeyboard(mContext,getCurrentFocus());
                get_latlong(pickUpLocationView.getText().toString(),"pick");
            }
        });

        destinationView.setAdapter(new GooglePlacesAutocompleteAdapter(this, R.layout.search_list_item));
        destinationView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Utils.hideKeyboard(mContext,getCurrentFocus());
                get_latlong(destinationView.getText().toString(),"des");
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
            pickUpLatitude =0;
            pickUpLongitude =0;
        }else{
            destinationLatitude =0;
            destinationLongitude =0;
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
                                    pickUpLatitude =a.getLatitude();
                                    pickUpLongitude =a.getLongitude();
                                }else{
                                    destinationLatitude =a.getLatitude();
                                    destinationLongitude =a.getLongitude();
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
                    if(pickUpLatitude ==0|| pickUpLongitude ==0){
                        Utils.showToast(mContext,getString(R.string.can_not_find_coordinates));
                    }else{
                        Utils.show_log("Location : "+ pickUpLatitude +","+ pickUpLongitude);
                        mMap.clear();
                        LatLng pick_loc = new LatLng(pickUpLatitude, pickUpLongitude);
                        mMap.addMarker(new MarkerOptions().position(pick_loc).title(place).
                                icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                        mMap.moveCamera(CameraUpdateFactory.newLatLng(pick_loc));
                        mMap.animateCamera(CameraUpdateFactory.zoomTo(12));

                        api_get_uber_time("selected");
                    }
                }else{
                    if(destinationLatitude ==0|| destinationLongitude ==0){
                        Utils.showToast(mContext,getString(R.string.can_not_find_coordinates));
                    }else{
                        Utils.show_log("Location : "+ destinationLatitude +","+ destinationLongitude);
                        api_get_uber_price();
                    }
                }
            }
        }.execute();
    }

    public void api_get_uber_time(final String check){
        pickUpTimeTextView.setText("");
        requestUberLayout.setVisibility(View.GONE);
        pickUpLayout.setVisibility(View.VISIBLE);
        destinationView.setText("");
        destinationView.setVisibility(View.GONE);
        destinationLatitude =0;
        destinationLongitude =0;

        String complete_url="";
        if(check.equals("current")){
            dialog.show();
            complete_url="&start_latitude="+tinyDB.getDouble(GeneralValues.CURRENT_LATITUDE,0)+
                    "&start_longitude="+tinyDB.getDouble(GeneralValues.CURRENT_LONGITUDE,0);
        }else{
            complete_url="&start_latitude="+ pickUpLatitude +"&start_longitude="+ pickUpLongitude;
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
                                pickUpTimeTextView.setText(Integer.parseInt(time)/60+" Minutes");
                            }else{
                                requestUberLayout.setVisibility(View.GONE);
                                pickUpLayout.setVisibility(View.VISIBLE);
                                destinationView.setText("");
                                destinationView.setVisibility(View.GONE);
                                destinationLatitude =0;
                                destinationLongitude =0;
                                setPickUpTimeTextView.setBackgroundResource(R.drawable.custom_green_selector);
                                pickUpTimeTextView.setText(Integer.parseInt(time)/60+" Minutes");
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
                .setPickupLocation(pickUpLatitude, pickUpLongitude, pickUpLocationView.getText().toString(),
                        pickUpLocationView.getText().toString())
                .setDropoffLocation(destinationLatitude, destinationLongitude, destinationView.getText().toString(),
                        destinationView.getText().toString())
                .build();

        // This button demonstrates deep-linking to the Uber app (default button behavior).
        uberButton.setRideParameters(rideParameters);
    }

    public void api_get_uber_price(){
        fareEstimateTextView.setText("");
        timeEstimateTextView.setText("");
        Call<ResponseBody> call = Utils.requestApi_Default().requestJson_simple(
                GeneralValues.UBER_PRICE_URL+"&start_latitude="
                        + pickUpLatitude +"&start_longitude="+ pickUpLongitude +
                        "&end_latitude="+ destinationLatitude +
                        "&end_longitude="+ destinationLongitude);
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
                            requestUberLayout.setVisibility(View.VISIBLE);
                            fareEstimateTextView.setText(price);
                            timeEstimateTextView.setText(Integer.parseInt(time)/60+" Minutes");
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

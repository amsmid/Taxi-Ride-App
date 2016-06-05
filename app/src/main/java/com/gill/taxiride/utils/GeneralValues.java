package com.gill.taxiride.utils;


public class GeneralValues {

    public static String EMAIL_PATTERN = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@" + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
    public static String FONT_FILE="bauhaus.ttf";

    public static String SERVER_TOKEN="";

    public static String BASE_URL="https://api.uber.com/";
    public static String UBER_TIME_URL=BASE_URL+"v1/estimates/time?server_token="+SERVER_TOKEN;
    public static String UBER_PRICE_URL=BASE_URL+"v1/estimates/price?server_token="+SERVER_TOKEN;

    public static String CURRENT_LATITUDE="current_latitude";
    public static String CURRENT_LONGITUDE="current_longitude";

    public static String GOOGLE_SERVER_KEY="AIzaSyDF77lVqylHcEsAX7aarelABmga1V-Nd_U";
    public static String PLACES_API_BASE = "https://maps.googleapis.com/maps/api/place/autocomplete/json";
}

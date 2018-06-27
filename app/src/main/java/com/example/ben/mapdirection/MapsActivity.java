package com.example.ben.mapdirection;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_REQUEST = 500;
    private static final int COLOR_ORANGE_ARGB = 0xffF57F17;

    private static final int NOTIFY_ID = 100;
    private static final String YES_ACTION = "YES_ACTION";
    private static final String NO_ACTION = "NO_ACTION";
    private NotificationManager notificationManager;

    private GoogleMap mMap;
    LocationManager locationManager;
    Boolean isGPSEnabled;
    Location location;
    double latitude;
    double longitude;
    LatLng currentLatLng;
    LatLng selectedLatLng;
    String latlog;
    Boolean changeSelectedLocation = true;
    SharedPreferences preferences;
    String selectedLatlog;
    String selectedLat;
    String selectedLog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        preferences = getSharedPreferences("MYPREFS", MODE_PRIVATE);
        selectedLatlog = preferences.getString("latlog", null);

        if (selectedLatlog != null) {
            changeSelectedLocation = false;
            String latlog = selectedLatlog.split("Test ")[1];
            selectedLat = (latlog.split(",")[0] + "." + latlog.split(",")[1]).split(" ")[0];
            selectedLog = (latlog.split(",")[2] + "." + latlog.split(",")[3]).split(" ")[0];
            selectedLatLng = new LatLng(Double.parseDouble(selectedLat), Double.parseDouble(selectedLog));
            currentLatLng = new LatLng(getCurrentLocation().getLatitude(), getCurrentLocation().getLongitude());
            String url = getRequestUrl(currentLatLng, selectedLatLng);
            TaskRequestDirections taskRequestDirections = new TaskRequestDirections();
            taskRequestDirections.execute(url);
        }
        else {
            Toast.makeText(this, "Select any location", Toast.LENGTH_LONG).show();
        }

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        processIntentAction(getIntent());
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST);
            Toast.makeText(this, "Close and restart application!", Toast.LENGTH_LONG).show();
            return;
        }
        mMap.setMyLocationEnabled(true);

        currentLatLng = new LatLng(getCurrentLocation().getLatitude(), getCurrentLocation().getLongitude());

        if (currentLatLng != null) {
            MarkerOptions currentMarkerOptions = new MarkerOptions();
            currentMarkerOptions.position(currentLatLng);
            currentMarkerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
            currentMarkerOptions.title("Your current location");
            mMap.addMarker(currentMarkerOptions);
        }

        if (selectedLatLng != null) {
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(selectedLatLng);
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
            markerOptions.title(getAddress(Double.parseDouble(selectedLat), Double.parseDouble(selectedLog)));
            mMap.addMarker(markerOptions);
        }

        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                if (changeSelectedLocation){

                    mMap.clear();

                    Location location = new Location("Test");
                    location.setLatitude(latLng.latitude);
                    location.setLongitude(latLng.longitude);
                    latlog = location.toString();

                    SharedPreferences.Editor editor = getSharedPreferences("MYPREFS", MODE_PRIVATE).edit();
                    editor.putString("latlog", latlog);
                    editor.commit();
                    //Create marker
                    currentLatLng = new LatLng(getCurrentLocation().getLatitude(), getCurrentLocation().getLongitude());

                    MarkerOptions currentMarkerOptions = new MarkerOptions();
                    currentMarkerOptions.position(currentLatLng);
                    currentMarkerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
                    currentMarkerOptions.title("Your current location");
                    mMap.addMarker(currentMarkerOptions);

                    preferences = getSharedPreferences("MYPREFS", MODE_PRIVATE);
                    selectedLatlog = preferences.getString("latlog", null);
                    String latlog = selectedLatlog.split("Test ")[1];
                    selectedLat = (latlog.split(",")[0] + "." +  latlog.split(",")[1]).split(" ")[0];
                    selectedLog = (latlog.split(",")[2] + "." +  latlog.split(",")[3]).split(" ")[0];

                    MarkerOptions markerOptions = new MarkerOptions();
                    markerOptions.position(latLng);
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                    markerOptions.title(getAddress(Double.parseDouble(selectedLat), Double.parseDouble(selectedLog)));
                    mMap.addMarker(markerOptions);

                    selectedLatLng = new LatLng(Double.parseDouble(selectedLat), Double.parseDouble(selectedLog));
                    currentLatLng = new LatLng(getCurrentLocation().getLatitude(), getCurrentLocation().getLongitude());
                    String url = getRequestUrl(currentLatLng, selectedLatLng);
                    TaskRequestDirections taskRequestDirections = new TaskRequestDirections();
                    taskRequestDirections.execute(url);

                    changeSelectedLocation = false;
                }
            }
        });
    }

    public void onResume(){
        super.onResume();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST);
            return;
        }
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 0, new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (selectedLat != null) {
                    double distance = getDistanceFromLatLonInKm(Double.parseDouble(selectedLat), Double.parseDouble(selectedLog), location.getLatitude(), location.getLongitude());
                    if (distance <= 0.2) {
                        showActionButtonsNotification();
                    }
                    else {
                        notificationManager.cancel(NOTIFY_ID);
                    }
                }
            }
            @Override
            public void onProviderDisabled(String provider) {
            }
            @Override
            public void onProviderEnabled(String provider) {
            }
            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }
        });
    }

    private Intent getNotificationIntent() {
        Intent intent = new Intent(this, MapsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return intent;
    }

    private void showActionButtonsNotification() {
        Intent yesIntent = getNotificationIntent();
        yesIntent.setAction(YES_ACTION);

        Intent noIntent = getNotificationIntent();
        noIntent.setAction(NO_ACTION);

        Notification notification = new android.support.v7.app.NotificationCompat.Builder(this)
                .setContentIntent(PendingIntent.getActivity(this, 0, getNotificationIntent(), PendingIntent.FLAG_UPDATE_CURRENT))
                .setSmallIcon(R.mipmap.location_icon)
                .setContentTitle("Are you around?")
                .setContentText("Do not forget to visit us!")
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true)
                .addAction(new NotificationCompat.Action(
                        R.mipmap.location_app,
                        getString(R.string.yes),
                        PendingIntent.getActivity(this, 0, yesIntent, PendingIntent.FLAG_UPDATE_CURRENT)))
                .addAction(new NotificationCompat.Action(
                        R.mipmap.exit,
                        getString(R.string.no),
                        PendingIntent.getActivity(this, 0, noIntent, PendingIntent.FLAG_UPDATE_CURRENT)))
                .build();

        notificationManager.notify(NOTIFY_ID, notification);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        processIntentAction(intent);
        super.onNewIntent(intent);
    }

    private void processIntentAction(Intent intent) {
        if (intent.getAction() != null) {
            switch (intent.getAction()) {
                case YES_ACTION:
                    break;
                case NO_ACTION:
                    finish();
                    notificationManager.cancel(NOTIFY_ID);
                    break;
            }
        }
    }

    public Location getCurrentLocation() {
        try {
            locationManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
            isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            if (isGPSEnabled) {
                if (location == null) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST);
                        return null;
                    }
                    if (locationManager != null) {
                        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST);
                            return null;
                        }
                        location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        if (location != null) {
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();
                        }
                    }
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return location;
    }

    private String getAddress(double latitude, double longitude) {
        StringBuilder result = new StringBuilder();
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses.size() > 0) {
                Address address = addresses.get(0);
                result.append(address.getFeatureName());
            }
        } catch (IOException e) {
            Log.e("tag", e.getMessage());
        }

        return result.toString();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch(id)
        {
            case R.id.action_settings:
                changeSelectedLocation = true;
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public double getDistanceFromLatLonInKm(double slat,double slon,double clat,double clon) {
        double R = 6371;
        double dLat = deg2rad(clat-slat);
        double dLon = deg2rad(clon-slon);
        double a =
                Math.sin(dLat/2) * Math.sin(dLat/2) +
                        Math.cos(deg2rad(slat)) * Math.cos(deg2rad(clat)) *
                                Math.sin(dLon/2) * Math.sin(dLon/2)
                ;
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double d = R * c;
        return d;
    }

    public double deg2rad(double deg) {
        return deg * (Math.PI/180);
    }

    private String getRequestUrl(LatLng origin, LatLng dest) {
        String str_org = "origin=" + origin.latitude +","+origin.longitude;
        String str_dest = "destination=" + dest.latitude+","+dest.longitude;
        String sensor = "sensor=false";
        String mode = "mode=driving";
        String param = str_org +"&" + str_dest + "&" +sensor+"&" +mode;
        String output = "json";
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + param;
        return url;
    }

    private String requestDirection(String reqUrl) throws IOException {
        String responseString = "";
        InputStream inputStream = null;
        HttpURLConnection httpURLConnection = null;
        try{
            URL url = new URL(reqUrl);
            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.connect();

            inputStream = httpURLConnection.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            StringBuffer stringBuffer = new StringBuffer();
            String line = "";
            while ((line = bufferedReader.readLine()) != null) {
                stringBuffer.append(line);
            }

            responseString = stringBuffer.toString();
            bufferedReader.close();
            inputStreamReader.close();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
            httpURLConnection.disconnect();
        }
        return responseString;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case LOCATION_REQUEST:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                }
                break;
        }
    }

    public class TaskRequestDirections extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... strings) {
            String responseString = "";
            try {
                responseString = requestDirection(strings[0]);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return  responseString;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            //Parse json here
            TaskParser taskParser = new TaskParser();
            taskParser.execute(s);
        }
    }

    public class TaskParser extends AsyncTask<String, Void, List<List<HashMap<String, String>>> > {

        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... strings) {
            JSONObject jsonObject = null;
            List<List<HashMap<String, String>>> routes = null;
            try {
                jsonObject = new JSONObject(strings[0]);
                DirectionsParser directionsParser = new DirectionsParser();
                routes = directionsParser.parse(jsonObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> lists) {

            ArrayList points = null;

            PolylineOptions polylineOptions = null;

            for (List<HashMap<String, String>> path : lists) {
                points = new ArrayList();
                polylineOptions = new PolylineOptions();

                for (HashMap<String, String> point : path) {
                    double lat = Double.parseDouble(point.get("lat"));
                    double lon = Double.parseDouble(point.get("lon"));

                    points.add(new LatLng(lat,lon));
                }

                polylineOptions.addAll(points);
                polylineOptions.width(15);
                polylineOptions.color(COLOR_ORANGE_ARGB);
                polylineOptions.geodesic(true);
            }

            if (polylineOptions!=null) {
                mMap.addPolyline(polylineOptions);
            } else {
                Toast.makeText(getApplicationContext(), "Direction not found!", Toast.LENGTH_SHORT).show();
            }

        }
    }
}

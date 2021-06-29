package com.example.locationtracker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final String url = "http://192.168.0.105/location.php";
    private final static int REQUEST_CODE_LOCATION_PERMISSION = 1;
    public Button startButton, stopButton;
    public TextView distance;
    public double lati1, longi1, lati2, longi2, latitude, longitude;
    public boolean checkLoc = false;
    public boolean running = false;
    public String locString = "loc1";
    final Handler handler = new Handler();
    int interval = 10000;
    String unit = "A";
    String unitText = " miles";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startButton = findViewById(R.id.startButton);
        stopButton = findViewById(R.id.stopButton);
        distance = findViewById(R.id.distView);

        stopButton.setEnabled(false);

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startButton.setEnabled(false);
                stopButton.setEnabled(true);
                getCurrentLocation();
                latitude = lati1;
                longitude = longi2;
                running = true;
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startButton.setEnabled(true);
                stopButton.setEnabled(false);
                running = false;
                distance.setText("");
            }
        });

        if (running = true){
            //Using runnable to run this every 10 seconds
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (ContextCompat.checkSelfPermission(getApplicationContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                REQUEST_CODE_LOCATION_PERMISSION);
                    } else {
                        getCurrentLocation();
                    }

                    System.out.println(locString);
                    if (locString.equals("loc1")){
                        lati1 = latitude;
                        longi1 = longitude;
                        locString = "loc2";
                    } else if (locString.equals("loc2")){
                        lati2 = latitude;
                        longi2 = longitude;
                        locString = "loc1";
                    }

                    if (lati1 != 0 && lati2 != 0){
                        String lat1 = String.valueOf(lati1);
                        String lat2 = String.valueOf(lati2);
                        String lon1 = String.valueOf(longi1);
                        String lon2 = String.valueOf(longi2);
                        String dist;

                        RequestQueue queue = Volley.newRequestQueue(getApplicationContext());

                        //Volley sends post method to PHP file in localhost
                        StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                                new Response.Listener<String>()
                                {
                                    @Override
                                    public void onResponse(String response) {
                                        Log.d("Response", response);
                                        //Response gets set into the view
                                        distance.setText(response + unitText);
                                    }
                                },
                                new Response.ErrorListener()
                                {
                                    @Override
                                    public void onErrorResponse(VolleyError error) {
                                        // error
                                        String response = "An error has occurred";
                                        Log.d("Error.Response", response);
                                    }
                                }
                        ) {
                            @Override
                            protected Map<String, String> getParams()
                            {
                                //params for the php function
                                Map<String, String> params = new HashMap<>();
                                params.put("lat1", lat1.trim());
                                params.put("lat2", lat2.trim());
                                params.put("lon1", lon1.trim());
                                params.put("lon2", lon2.trim());
                                params.put("unit", unit.trim());

                                return params;
                            }
                        };
                        queue.add(postRequest);

                    }
                    System.out.println("Lat1: " + lati1 + "\nLat2: " + lati2);
                    handler.postDelayed(this, interval);
                }
            }, interval);
        } else {
            distance.setText("");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_LOCATION_PERMISSION && grantResults.length > 0) {
            getCurrentLocation();
        } else {
            Toast.makeText(this, "Permission denied!", Toast.LENGTH_SHORT).show();
        }
    }

    private void getCurrentLocation() {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        //Lat and Longitude located using GPS
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.getFusedLocationProviderClient(MainActivity.this)
                .requestLocationUpdates(locationRequest, new LocationCallback(){
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        super.onLocationResult(locationResult);
                        LocationServices.getFusedLocationProviderClient(MainActivity.this)
                                .removeLocationUpdates(this);
                        if(locationResult != null && locationResult.getLocations().size() > 0){
                            int latestLocationIndex = locationResult.getLocations().size() - 1;
                            //Lat and Longitude set
                            latitude = locationResult.getLocations().get(latestLocationIndex).getLatitude();
                            longitude = locationResult.getLocations().get(latestLocationIndex).getLongitude();
                            }
                    }
                }, Looper.getMainLooper());
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.dist_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            //Depending on the letter, the unit of measurement will change in PHP
            case R.id.meter:
                unit = "M";
                unitText = " meters";
                return true;
            case R.id.km:
                unit = "K";
                unitText = " km";
                return true;
            case R.id.miles:
                unit = "A";
                unitText = " miles";
                return true;

        }
        return true;
    }
}
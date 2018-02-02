package com.example.mapsstuff;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.icu.text.DecimalFormat;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.CountDownTimer;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import android.os.Vibrator;
import android.widget.ToggleButton;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;


public class MainActivity extends AppCompatActivity {

    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;

    private boolean bOn = true;
    private double latitude = 0;
    private double longitude = 0;

    // latitude and longitude of newmarket GO train station

    private double desiredLat = 44.060558;
    private double desiredLong = -79.459834;
    private CountDownTimer updateDestionationTimer;
    // how close in kilometers before alarm goes off
    private double alarmValue = 2.5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermission();
        Spinner locations = (Spinner) findViewById(R.id.locations);


        locations.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                EditText inputAddress = (EditText) findViewById(R.id.destAddr);
                Spinner locations = (Spinner) findViewById(R.id.locations);
                String selectedValue = locations.getSelectedItem().toString();

                //first selection is always current text
                if(locations.getCount() != 1 && locations.getSelectedItemPosition() != 0) {
                    inputAddress.setText(selectedValue);
                }

            }
            public void onNothingSelected(AdapterView<?> parent)
            {

            }
        });

        updateDestionationTimer = new CountDownTimer(1000, 20) {

            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                try {
                    updateDestination();
                }
                catch(Exception e) {
                    System.out.println(e.getStackTrace());
                    updateDestionationTimer.start();
                }
            }
        }.start();

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {

                    //set current long and latitude
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();

                    // get editable text elements
                    EditText alarmVal = (EditText) findViewById(R.id.alarmVal);

                    //try to load their values into their set variables
                    try {
                        alarmValue = Double.parseDouble(alarmVal.getText().toString());
                    }
                    catch (NumberFormatException e ) {
                        System.out.println("Not a Double");
                    }

                    //set the location in the UI
                    SetApproxAddress();
                    setLocation();
                }
            }


        };

    }



    public void checkPermission(){
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                ){//Can add more as per requirement

            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION,android.Manifest.permission.ACCESS_COARSE_LOCATION},
                    123);
        }
    }


    public void toggleActive (View view) {


        ToggleButton button = (ToggleButton) findViewById(R.id.toggleButton);

        bOn = button.isChecked();

        if(bOn)
            startUpdates();
        else
            stopUpdates();

    }


    protected void setLocation() {
        //get all the UI elements that need to be changed

        TextView remainingVal = (TextView) findViewById(R.id.remainingVal);

        /// get remaining distance in kilometers
        double remainingDist = Math.sqrt(Math.pow((longitude - desiredLong),2) + Math.pow((latitude - desiredLat),2))*111;

        //if below set alarm distance, vibrate
        if(remainingDist <= alarmValue) {

            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

            v.vibrate(5000);

        }

        // set UI elements text
        remainingVal.setText(String.format("%.2f km",remainingDist));

    }

    // make sure that the maps element is updating upon resuming the app
    @Override
    protected void onResume() {
        super.onResume();
        if(bOn)
            startUpdates();
    }

    public void updateDestination() {


        Geocoder geocoder = new Geocoder(this);

        List<Address> addresses;
        EditText destination = (EditText) findViewById(R.id.destAddr);
        Spinner locations = (Spinner) findViewById(R.id.locations);

        try {

            addresses = geocoder.getFromLocationName(destination.getText().toString(),10);
            List<String> addressList = new ArrayList<String>();
            addressList.add(destination.getText().toString()); //add current address string to simulate no selection

            // dont display if only one result (our current result)
            String p = destination.getText().toString();
            String pp = addresses.get(0).getAddressLine(0).toString();
            boolean isIt = p == pp;
            if(!(addresses.size() == 1 && destination.getText().toString().equals(addresses.get(0).getAddressLine(0).toString()))) {
                for (android.location.Address x : addresses) {
                    addressList.add(x.getAddressLine(0));
                }
            }

            ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, addressList);
            dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            locations.setAdapter(dataAdapter);

            if (addresses.size() > 0) {
                desiredLat = addresses.get(0).getLatitude();
                desiredLong = addresses.get(0).getLongitude();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

    //restart timer
        updateDestionationTimer.start();


    }

    public void stopUpdates() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }
    public void startUpdates () {

        //make sure we have permissions to use maps
        checkPermission();

        // make location requests
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // make request updates (also loop)
        mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                mLocationCallback,
                null /* Looper */);
    }

    // set the text view that contains the approx address at our location
    public void SetApproxAddress() {

        Geocoder myLocation = new Geocoder(getApplicationContext(),
                Locale.getDefault());

        try {
            List<Address> myList = myLocation.getFromLocation(latitude,
                    longitude, 1);
            TextView addrView = (TextView) findViewById(R.id.approxAddr);
            if (myList != null && myList.size() > 0) {
                Address address = myList.get(0);
                String result = address.getAddressLine(0);

                addrView.setText(result);
                // Toast.makeText(getApplicationContext(), ""+result,
                // Toast.LENGTH_LONG).show();
            }
            else {
                addrView.setText("Can't Find Address");
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

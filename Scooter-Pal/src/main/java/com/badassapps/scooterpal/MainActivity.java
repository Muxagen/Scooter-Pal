package com.badassapps.scooterpal;

import android.content.Context;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends ActionBarActivity {

    private LocationManager locationManager;
    private LocationListener locationListener;

    private Location currentGPSLocation;
    private Location currentNetworkLocation;
    private Location currentBestLocation;

    private int nullCounter;
    private int nextSpeedLevel;
    private long currentTime;

    private boolean measuringAcceleration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
        measuringAcceleration = false;
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
        locationManager.removeUpdates(locationListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        nullCounter = 0;
        registerLocationListener();
    }

    private void registerLocationListener() {
        // Acquire a reference to the system Location Manager
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // Define a listener that responds to location updates
        locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {

                if(location != null) {
                    // Called when a new location is found by the network location provider.
                    makeUseOfNewLocation(location);
                }
                else {
                    nullCounter++;
                    if (nullCounter > 3) {
                        locationManager.removeUpdates(locationListener);
                        Toast.makeText(getApplicationContext(),
                                "Something terrible has happened with your GPS",
                                Toast.LENGTH_LONG).show();
                    }
                }
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {}
            public void onProviderEnabled(String provider) {}
            public void onProviderDisabled(String provider) {}
        };

        // Register the listener with the Location Manager to receive location updates
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
    }

    private void makeUseOfNewLocation(Location newLocation) {

        TextView locationTV;
        Long speed = 0L;

        if (newLocation.getProvider().equals(LocationManager.GPS_PROVIDER)) {
            locationTV = (TextView)findViewById(R.id.gps_coordinates);

            if (currentGPSLocation == null) currentGPSLocation = newLocation;
            else speed = calculateSpeed(currentGPSLocation, newLocation);

            locationTV.setText("GPS:\n" + newLocation.getLatitude() + " " + newLocation.getLongitude()
                    + "\n speed = " + speed + "m\\s");

            TextView accelerationTV = (TextView)findViewById(R.id.acceleration);

            if (measuringAcceleration == true) {
                if (speed >= nextSpeedLevel) {
                    if (nextSpeedLevel == 5) {
                        currentTime = System.currentTimeMillis();
                    } else {
                        long accelerationTime = calculateAcceleration();
                        accelerationTV.append("" + nextSpeedLevel + " - " + nextSpeedLevel + 10 + ": " + accelerationTime);
                    }
                    nextSpeedLevel += 10;
                }
            }
        }
        else if (newLocation.getProvider().equals(LocationManager.NETWORK_PROVIDER)) {
            locationTV = (TextView)findViewById(R.id.network_coordinates);

            if (currentNetworkLocation == null) currentNetworkLocation = newLocation;
            else speed = calculateSpeed(currentNetworkLocation, newLocation);

            locationTV.setText("NETWORK:\n" + newLocation.getLatitude() + " " + newLocation.getLongitude()
                    + "\n speed = " + speed + "m\\s");
        } else {
            locationTV = (TextView)findViewById(R.id.network_coordinates);
            locationTV.setText("Location update from unknown Provider: " + newLocation.getProvider());
        }
    }

    private long calculateAcceleration() {

        long newCurrentTime = System.currentTimeMillis();
        long levelTime = newCurrentTime - currentTime;
        currentTime = newCurrentTime;
        return levelTime;
    }


    //not using location.getSpeed() because it compares the distance to previous
    //gps location that might have been inaccurate
    private Long calculateSpeed(Location oldLocation, Location newLocation) {

        TextView locationTV = (TextView)findViewById(R.id.gps_coordinates);

        Double speed;
        double distance = newLocation.distanceTo(oldLocation); //m
        newLocation.getLongitude()
        //locationTV.append("distance = " + distance);
        if (distance < 0.001) {
            speed = 0D;
        } else {
            double time = (newLocation.getTime() - oldLocation.getTime()); /// (1000*60*60); //h
            speed = (time == 0D) ? 0 : distance / time;
            speed = speed/3600;
        }
        return  speed.longValue(); //km/h
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return item.getItemId() == R.id.action_settings || super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_main, container, false);
        }
    }

    public void onClickReset(View v) {
        TextView acceleration = (TextView)findViewById(R.id.acceleration);
        acceleration.setText("");
    }

    public void onClickStart(View v) {

        Button startBTN = (Button)findViewById(R.id.start);

        if (!measuringAcceleration) {
            nextSpeedLevel = 5;
            currentTime = 0;
            measuringAcceleration = true;
            startBTN.setText("Stop");
        } else {
            measuringAcceleration = false;
            startBTN.setText("Start");
        }

    }

}

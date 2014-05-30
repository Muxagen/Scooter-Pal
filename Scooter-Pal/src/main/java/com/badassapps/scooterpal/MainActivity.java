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
import android.widget.TextView;


public class MainActivity extends ActionBarActivity {

    private LocationManager locationManager;
    private LocationListener locationListener;

    private Location currentGPSLocation;
    private Location currentNetworkLocation;
    private Location currentBestLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
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
        registerLocationListener();
    }

    private void registerLocationListener() {
        // Acquire a reference to the system Location Manager
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // Define a listener that responds to location updates
        locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                // Called when a new location is found by the network location provider.
                makeUseOfNewLocation(location);
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

        TextView tv;
        Long speed = 0L;

        if (newLocation.getProvider().equals(LocationManager.GPS_PROVIDER)) {
            tv = (TextView)findViewById(R.id.gps_coordinates);

            if (currentGPSLocation == null) currentGPSLocation = newLocation;
            else speed = calculateSpeed(currentGPSLocation, newLocation);

            tv.setText("GPS:\n" + newLocation.getLatitude() + " " + newLocation.getLongitude()
                        + "\n speed = " + speed + "m\\s");
        }
        else if (newLocation.getProvider().equals(LocationManager.NETWORK_PROVIDER)) {
            tv = (TextView)findViewById(R.id.network_coordinates);

            if (currentNetworkLocation == null) currentNetworkLocation = newLocation;
            else speed = calculateSpeed(currentNetworkLocation, newLocation);

            tv.setText("NETWORK:\n" + newLocation.getLatitude() + " " + newLocation.getLongitude()
                        + "\n speed = " + speed + "m\\s");
        } else {
            tv = (TextView)findViewById(R.id.network_coordinates);
            tv.setText("Location update from unknown Provider: " + newLocation.getProvider());
        }
    }


    //not using location.getSpeed() because it compares the distance to previous
    //gps location that might have been inaccurate
    private Long calculateSpeed(Location oldLocation, Location newLocation) {
        Long speed;
        float distance = newLocation.distanceTo(oldLocation);
        if (distance < 1) {
            speed = 0L;
        } else {
            long time = (newLocation.getTime() - oldLocation.getTime()) / 1000;
            speed = (long) distance / time;
        }
        return  speed;
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
        acceleration.setText("a=0.0 m/s²");
    }
}

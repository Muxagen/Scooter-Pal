package com.badassapps.scooterpal;

import android.content.Context;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
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

	private static final int FIRST_LEVEL_SPEED = 5;
	private static final int SPEED_LEVEL_STEP = 10;

	private LocationManager locationManager;
	private LocationListener locationListener;

	private Location currentGPSLocation;
	private Location currentNetworkLocation;
	private Location currentBestLocation; //TODO

	private final Handler handler = new Handler();

	private int nullCounter;
	private int nextSpeedLevel;
	private long currentTime;
	private int maxSpeed;

	private boolean measuringAcceleration;

	private static final String LOG_TAG = "MainActivity";

	/**
	 * **************************Activity Lifecycle*********************************************
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment())
					.commit();
		}
		nullCounter = 0;
		measuringAcceleration = false;
		registerLocationListener();
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		locationManager.removeUpdates(locationListener);
	}
	/*********************************************************************************************/

	/**
	 * register location listeners and handle location provider problems
	 */
	private void registerLocationListener() {

		// Acquire a reference to the system Location Manager
		locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

		// Define a listener that responds to location updates
		locationListener = new LocationListener() {

			public void onLocationChanged(Location location) {

				if (location != null) {
					// Called when a new location is found by the network location provider.
					makeUseOfNewLocation(location);
				} else {
					nullCounter++;
					if (nullCounter > 3) {
						locationManager.removeUpdates(locationListener);
						nullCounter = 0;

						Toast.makeText(getApplicationContext(),	R.string.gps_provider_lost,	Toast.LENGTH_LONG).show();
						Log.d(LOG_TAG, "GPS location provider not available");

						// Try to register again in 5 seconds
						handler.postDelayed(new Runnable() {
							@Override
							public void run() {
								registerLocationListener();
							}
						}, 5000);
					}
				}
			}
			public void onStatusChanged(String provider, int status, Bundle extras) {}
			public void onProviderEnabled(String provider) {}
			public void onProviderDisabled(String provider) {}
		};

		// Register the listener with the Location Manager to receive location updates
		try {
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
		} catch (IllegalArgumentException e) {
			Toast.makeText(this, "GPS location provider not available", Toast.LENGTH_LONG).show();
			Log.d(LOG_TAG, "GPS location provider not available");
		}
		try {
			locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
		} catch (IllegalArgumentException e) {
			Toast.makeText(this, "Network location provider not available", Toast.LENGTH_LONG).show();
			Log.d(LOG_TAG, "Network location provider not available");
		}
	}

	/**
	 * @param newLocation
	 */
	private void makeUseOfNewLocation(Location newLocation) {

		TextView locationTV;
		Long speed = 0L;

		if (newLocation.getProvider().equals(LocationManager.GPS_PROVIDER)) {
			locationTV = (TextView) findViewById(R.id.gps_coordinates);

			if (currentGPSLocation == null) currentGPSLocation = newLocation;
			else speed = calculateSpeed(currentGPSLocation, newLocation);

			locationTV.setText("GPS:\n  " + newLocation.getLatitude() + " " + newLocation.getLongitude()
					+ "\n  speed = " + speed + "km/h"
					+ "\n  accuracy = " + newLocation.getAccuracy());

			TextView accelerationTV = (TextView) findViewById(R.id.acceleration);

			// if the measurements are running
			if (measuringAcceleration == true) {
				// if the next speed 'level' has been reached
				if (speed >= nextSpeedLevel) {
					// for the first 'level' only init the currentTime
					if (nextSpeedLevel == FIRST_LEVEL_SPEED) {
						currentTime = System.currentTimeMillis();
					} else {
						long accelerationTime = calculateAcceleration();
						accelerationTV.append("" + nextSpeedLevel + " - " + (nextSpeedLevel + SPEED_LEVEL_STEP)
								+ ": " + accelerationTime + "\n");
					}
					nextSpeedLevel += SPEED_LEVEL_STEP;
				}
			}
		} else if (newLocation.getProvider().equals(LocationManager.NETWORK_PROVIDER)) {
			locationTV = (TextView) findViewById(R.id.network_coordinates);

			if (currentNetworkLocation == null) currentNetworkLocation = newLocation;
			else speed = calculateSpeed(currentNetworkLocation, newLocation);

			locationTV.setText("NETWORK:\n  " + newLocation.getLatitude() + " " + newLocation.getLongitude()
					+ "\n  speed = " + speed + "km/h");
		} else {
			locationTV = (TextView) findViewById(R.id.network_coordinates);
			locationTV.setText("Location update from unknown Provider: " + newLocation.getProvider());
		}
	}

	/**
	 * Calculate acceleration as time spent on accelerating to the speed of the next 'level'
	 * and set the new currentTime value for next calculation
	 *
	 * @return time spent on accelerating to next 'level' in seconds
	 */
	private long calculateAcceleration() {

		long newCurrentTime = System.currentTimeMillis();
		long levelTime = newCurrentTime - this.currentTime;
		this.currentTime = newCurrentTime;
		return levelTime / 1000;
	}

	/**
	 * not using location.getSpeed() because it compares the distance to previous
	 * gps location that might have been inaccurate
	 *
	 * @param oldLocation
	 * @param newLocation
	 * @return
	 */
	private Long calculateSpeed(Location oldLocation, Location newLocation) {

		TextView locationTV = (TextView) findViewById(R.id.gps_coordinates);

		Double speed;
		double distance = distance(oldLocation, newLocation) / 1000;//km //newLocation.distanceTo(oldLocation); //m
		//locationTV.append("distance = " + distance);
		if (distance < 0.001) {
			speed = 0d;
		} else {
			//TODO: getElapsedRealtimeNanos is API Level 17, so change back to getTime or invent something else
			//double time_in_millis = (newLocation.getTime() - oldLocation.getTime());
			double time_in_millis = (newLocation.getElapsedRealtimeNanos() - oldLocation.getElapsedRealtimeNanos());
			double time_in_hours = time_in_millis / 3600000000000d;
			speed = (time_in_hours == 0d) ? 0 : distance / time_in_hours;
		}
		if (measuringAcceleration) {
			if (speed > maxSpeed) {
				maxSpeed = speed.intValue();
				TextView maxSpeedTV = (TextView) findViewById(R.id.maxSpeed);
				maxSpeedTV.setText(getString(R.string.best_speed) + maxSpeed);
			}
		}
		return speed.longValue(); //km/h
	}


	/**
	 * Calculates Pythagorean distance using approximation of location coordinates
	 */
	private double distance(Location location1, Location location2) {
		double a = (location1.getLatitude() - location2.getLatitude()) * distPerLat(location1.getLatitude());
		double b = (location1.getLongitude() - location2.getLongitude()) * distPerLng(location1.getLongitude());
		return Math.sqrt(a * a + b * b);
	}

	private double distPerLng(double lat) {
		return	  0.0003121092 * Math.pow(lat, 4)
				+ 0.0101182384 * Math.pow(lat, 3)
				- 17.2385140059 * lat * lat
				+ 5.5485277537 * lat + 111301.967182595;
	}

	private double distPerLat(double lat) {
		return  - 0.000000487305676 * Math.pow(lat, 4)
				- 0.0033668574 * Math.pow(lat, 3)
				+ 0.4601181791 * lat * lat
				- 1.4558127346 * lat + 110579.25662316;
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
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			return inflater.inflate(R.layout.fragment_main, container, false);
		}
	}

	public void onClickReset(View v) {
		TextView acceleration = (TextView) findViewById(R.id.acceleration);
		TextView maxSpeed = (TextView) findViewById(R.id.maxSpeed);
		acceleration.setText("");
		maxSpeed.setText(getText(R.string.best_speed));
	}

	public void onClickStart(View v) {

		Button startBTN = (Button) findViewById(R.id.start);
		Button resetBTN = (Button) findViewById(R.id.reset);

		if (!measuringAcceleration) {
			nextSpeedLevel = 5;
			currentTime = 0;
			maxSpeed = 0;
			measuringAcceleration = true;
			startBTN.setText(R.string.btn_stop);
			resetBTN.setEnabled(false);
		} else {
			measuringAcceleration = false;
			startBTN.setText(R.string.btn_start);
			resetBTN.setEnabled(true);
		}
	}

}

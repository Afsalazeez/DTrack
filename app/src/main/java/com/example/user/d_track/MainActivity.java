package com.example.user.d_track;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.PersistableBundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApi;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private static final int MY_PERMISSION_TO_GET_LOCATION = 1001;

    private static final int REQUEST_CHECK_SETTINGS = 101;

    private static final String REQUESTING_LOCATION_UPDATES_KEY = "location_request_key";

    private static final String KEY_LOCATION = "location";

    private static final String KEY_LAST_UPDATED_TIME_STRING = "last_updated_time_string";

    private GoogleApiClient mGoogleApiClient;

    private FusedLocationProviderClient mFusedLocationProviderClient;

    TextView locationTextView;

    TextView totalDistanceTravelledTextView;

    TextView timeTextView;

    Button getLocationButton;

    Button startButton;

    Button stopButton;

    private LocationCallback mLocationCallback;

    private boolean mRequestingLocationUpdates;

    private LocationRequest mLocationRequest;

    private Location mCurrentLocation;

    private Location mLastLocation;

    private float totalDistanceTravelled = 0;

    private String mLastUpdatedTime;

    private long timeInMilliSeconds = 0L;

    private long startTime = 0L;

    private Handler customTimeUpdationHandler = new Handler();

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        outState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY, mRequestingLocationUpdates);
        super.onSaveInstanceState(outState, outPersistentState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initGoogleClientAPI();

        locationTextView = (TextView) findViewById(R.id.location_display_text_view);

        totalDistanceTravelledTextView = (TextView) findViewById(R.id.total_distance_in_meters);

        timeTextView = (TextView) findViewById(R.id.time_text_view);

        getLocationButton = (Button) findViewById(R.id.get_location_button);

        startButton = (Button) findViewById(R.id.start_button);

        stopButton = (Button) findViewById(R.id.stop_button);

        getLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mRequestingLocationUpdates = true;
                getLastKnownLocation();
            }
        });

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startTime = SystemClock.uptimeMillis();
                customTimeUpdationHandler.postDelayed(updateTimerThread, 0);
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                customTimeUpdationHandler.removeCallbacks(updateTimerThread);
            }
        });


        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        mLocationCallback = new LocationCallback() {

            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    // Update UI with location data
                    updateUI(location);
                }
            }
        };


        /**
         * From API level 23 users can revoke any permissions at any time
         * So we must check if we have that permission every time we perform an
         * operation that requires that permission
         *
         * The codes below checks if the activity have the permission to get the
         * user location
         */
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Permission not granted
            // Should we show and explanation
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission
            } else {
                // No explanation needed, request the permission
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSION_TO_GET_LOCATION);

                // MY_PERMISSION_TO_GET_LOCATION is an
                // app-defined constant. The callback method gets the
                // result of the request.
            }


        } else {
            // Permission has already been granted
            createLocationRequest();
        }

        updateValuesFromBundle(savedInstanceState);


    }

    private void updateValuesFromBundle(Bundle savedInstanceState) {

        if (savedInstanceState == null) {
            return;
        }
        // Update the value of mRequestingLocationUpdates from the Bundle.
        if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
            mRequestingLocationUpdates = savedInstanceState.getBoolean(
                    REQUESTING_LOCATION_UPDATES_KEY);
        }
        //...

        // Update the value of mCurrentLocation from the Bundle and update the UI to show the
        // correct longitude and latitude
        if (savedInstanceState.keySet().contains(KEY_LOCATION)) {
            // Since KEY_LOCATION was found in the Bundle, we can be sure that the mCurrentLocation
            // is not null
            mCurrentLocation = savedInstanceState.getParcelable(KEY_LOCATION);
        }

        // Update the value of mLastUpdatedTime from the Bundle and update the UI
        if (savedInstanceState.keySet().contains(KEY_LAST_UPDATED_TIME_STRING)) {
            mLastUpdatedTime = savedInstanceState.getString(KEY_LAST_UPDATED_TIME_STRING);
        }
        // We update the UI here...
    }

    /**
     * Callback received when a permission request has been completed.
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSION_TO_GET_LOCATION:
                // if the request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] ==
                        PackageManager.PERMISSION_GRANTED) {
                    // Permission is granted move to the next location related task we need to
                    createLocationRequest();

                } else {
                    // Permission is denied, Disable the
                    // functionality that depends on the permission
                }
        }
        return;
    }

    /**
     * This function checks if the location services are turned on or not
     * If not, this method show a dialogue to
     * turn on the location services, unless we cant get location services
     * to display
     */
    protected void createLocationRequest() {

        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(2000);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);
        builder.setAlwaysShow(true);

        SettingsClient client = LocationServices.getSettingsClient(this);

        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());
        task.addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                // All location settings are satisfied. The client can initialize
                // location requests here...
                startLocationUpdates();
            }
        });

        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    // Location settings are not satisfied. But could be fixed by showing the
                    // user dialog
                    try {
                        // Cast to a resolvable exception.
                        ResolvableApiException apiException = (ResolvableApiException) e;
                        // Show the dialogue by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        apiException.startResolutionForResult(MainActivity.this,
                                REQUEST_CHECK_SETTINGS);
                    } catch (IntentSender.SendIntentException e1) {
                        // Ignore the error
                        e1.printStackTrace();
                    } catch (ClassCastException e2) {
                        // Ignore, should be an impossible error
                    }
                }
            }
        });


    }

    /**
     * This function initialize GoogleAPIClient,
     * Without GoogleAPIClient Auto location Client will not work
     */
    private void initGoogleClientAPI() {

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    /**
     * Callback received when a settings changes has been completed.
     *
     * @param requestCode The code for changing location settings
     * @param resultCode  Result code received by user choice
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        final LocationSettingsStates states = LocationSettingsStates.fromIntent(data);
        switch (requestCode) {

            case REQUEST_CHECK_SETTINGS:

                switch (resultCode) {

                    case Activity.RESULT_OK:
                        // All required changes were successfully made
                        startLocationUpdates();
                        break;
                    case Activity.RESULT_CANCELED:
                        // The user we asked to change settings, but chose not to
                        break;
                    default:
                        break;
                }
                break;
        }
    }

    // This method fetches last updated location using {@link mFusedLocationProviderClient}
    private void getLastKnownLocation() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        } else {
            mFusedLocationProviderClient.getLastLocation()
                    .addOnSuccessListener(new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            // Got last know location. In some situations this can be null
                            if (location != null) {
                                // Save the location as last location for distance calculation
                                updateUI(location);
                            }
                        }
                    });
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }


    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mFusedLocationProviderClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);
    }

    // Stop receiving updates when activity is paused
    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    // This method stops receiving location updates from {@link mFusedLocationProviderClient}
    private void stopLocationUpdates() {
        mFusedLocationProviderClient.removeLocationUpdates(mLocationCallback);
    }

    /**
     * This method updates the UI with Longitude and Latitude values from
     * the location updated
     *
     * @param currentLocation Location updated from (@link mFusedLocationProviderClient}
     */

    public void updateUI(Location currentLocation) {

        Double latitude = currentLocation.getLatitude();
        Double longitude = currentLocation.getLongitude();

        String locationString = "Longitude : " + String.valueOf(longitude)
                + " Latitude : " + String.valueOf(latitude);

        locationTextView.append(new StringBuilder().append("\n").append(locationString).toString());

        if (mLastLocation == null) {

            saveLastLocation(currentLocation);
        } else {

            calculateDistanceInMeters(mLastLocation, currentLocation);
        }

        saveLastLocation(currentLocation);
    }

    public void saveLastLocation(Location location) {
        mLastLocation = location;
    }

    /**
     * This method calculates distance between two locations in meters and updates the UI
     *
     * @param lastLocation    Location previously stored
     * @param currentLocation Location recently updated
     */

    public void calculateDistanceInMeters(Location lastLocation, Location currentLocation) {

        // Earth radius in meters
        double earthRadius = 6371000;

        double deltaLatitude = Math.toRadians(currentLocation.getLatitude() - lastLocation.getLatitude());
        double deltaLongitude = Math.toRadians(currentLocation.getLongitude() - lastLocation.getLongitude());

        double val = Math.sin(deltaLatitude / 2) * Math.sin(deltaLatitude / 2) +
                Math.cos(Math.toRadians(currentLocation.getLatitude())) * Math.cos(Math.toRadians(currentLocation.getLatitude()))
                        * Math.sin(deltaLongitude / 2) * Math.sin(deltaLongitude / 2);
        double val2 = 2 * Math.atan2(Math.sqrt(val), Math.sqrt(1 - val));

        float distanceInMeters = (float) (earthRadius * val2);

        totalDistanceTravelled = totalDistanceTravelled + distanceInMeters;

        updateDistanceUI(distanceInMeters);

    }

    /**
     * This method append location distance String value to the locationTextView
     *
     * @param distanceInMeters float value which was calculated using calculateDistanceInMeters
     */
    public void updateDistanceUI(float distanceInMeters) {
        locationTextView.append(new StringBuilder().append("\nDistance : ").append(String.valueOf(distanceInMeters)).toString());

        totalDistanceTravelledTextView.setText(new StringBuilder().append("Total Distance : ").append(totalDistanceTravelled)
                .append(" meters").toString());
    }

    /**
     * Runnable thread for calculating time taken
     */
    private Runnable updateTimerThread = new Runnable() {
        @Override
        public void run() {

            timeInMilliSeconds = SystemClock.uptimeMillis() - startTime;

            int seconds = (int) (timeInMilliSeconds / 1000);

            int minutes = seconds / 60;

            if (seconds > 60) {
                seconds = seconds % 60;
            }


            String timeValue = String.format("%02d", minutes) + ":" + String.format("%02d", seconds);

            timeTextView.setText(timeValue);

            customTimeUpdationHandler.postDelayed(this, 0);
        }
    };

}

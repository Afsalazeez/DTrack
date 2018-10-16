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

    // static final integer value for checking permission to get location
    private static final int MY_PERMISSION_TO_GET_LOCATION = 1001;

    // static final integer value for turning on location services settings
    private static final int REQUEST_CHECK_SETTINGS = 101;

    // static final String value key for location updates
    private static final String REQUESTING_LOCATION_UPDATES_KEY = "location_request_key";

    // GoogleApiClient object
    private GoogleApiClient mGoogleApiClient;

    // FusedLocationProviderClient object
    private FusedLocationProviderClient mFusedLocationProviderClient;

    // TextView  to display location logs
    TextView locationTextView;

    // TextView which displays total distance travelled by user
    TextView totalDistanceTravelledTextView;

    // TextView which displays time for every second change
    TextView timeTextView;

    // TextView which displays speed of the user travelling
    TextView speedTextView;

    // Button which makes display the current or last known
    // location of the user in the location logs
    Button getLocationButton;

    // Button to start tracking
    Button startButton;

    // Button to stop tracking
    Button stopButton;

    // Callbacks which receives location updates
    private LocationCallback mLocationCallback;

    // Boolean value for storing if updates are received
    // or not
    private boolean mRequestingLocationUpdates;

    // LocationRequest object
    private LocationRequest mLocationRequest;

    // Location object for storing last know location
    private Location mLastLocation;

    // float value which stores the distance
    // travelled by the user
    private float totalDistanceTravelled = 0;

    // float value which stores the time in
    // milliseconds passed after the user starts
    // navigation
    private long timeInMilliSeconds = 0L;

    // time of the system when user starts navigation
    // is recorded as the start time.
    private long startTime = 0L;

    // Handler is used as a timer..
    private Handler customTimeUpdationHandler = new Handler();

    public static final String DISTANCE_IN_METERS = "distance_in_meters";

    public static final String TIME_IN_MILLISECONDS = "time_in_milliseconds";

    public static final String AVERAGE_SPEED = "average_speed";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initializing GoogleApiClient for requesting location updates.
        // We can't have LocationAPI requests if GoogleAPIClient is not connected.
        initGoogleClientAPI();

        // binding all the views to the MainActivity
        locationTextView = (TextView) findViewById(R.id.location_display_text_view);

        totalDistanceTravelledTextView = (TextView) findViewById(R.id.total_distance_in_meters);

        timeTextView = (TextView) findViewById(R.id.time_text_view);

        speedTextView = (TextView) findViewById(R.id.speed_text_view);

        getLocationButton = (Button) findViewById(R.id.get_location_button);

        startButton = (Button) findViewById(R.id.start_button);

        stopButton = (Button) findViewById(R.id.stop_button);


        // Adding an onClickListener to the getLocationButton
        getLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                getLastKnownLocation();
            }
        });

        // Adding an onClickListener to the startButton
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                createLocationRequest();
                startButton.setActivated(false);
            }
        });

        // Adding an onClickListener to the stopButton
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Removing callbacks from the Timer thread so
                // it won't update time
                customTimeUpdationHandler.removeCallbacks(updateTimerThread);

                // stopping location updates
                stopLocationUpdates();

                Long time = timeInMilliSeconds / 1000;

                float averageSpeed = totalDistanceTravelled / time;

                // Creates an intent to the resultActivity to print results
                Intent resultIntent = new Intent(MainActivity.this, ResultActivity.class);
                resultIntent.putExtra(DISTANCE_IN_METERS, totalDistanceTravelled);
                resultIntent.putExtra(TIME_IN_MILLISECONDS, timeInMilliSeconds);
                resultIntent.putExtra(AVERAGE_SPEED, averageSpeed);
                startActivity(resultIntent);

                // Changing all the UI , integer and String values to
                // first values
                timeTextView.setText(getString(R.string.time));
                totalDistanceTravelled = 0;
                timeInMilliSeconds = 0;
                totalDistanceTravelledTextView.setText(getString(R.string.distance));


                locationTextView.setText("Location Logs : ");
                stopButton.setActivated(false);

            }
        });

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
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSION_TO_GET_LOCATION);

            // MY_PERMISSION_TO_GET_LOCATION is an
            // app-defined constant. The callback method gets the
            // result of the request.s
        }

        /**
         * Initializing {@link FusedLocationProviderClient} object
         * This object uses GPS and internet connection to fetch accurate
         * location data of the user
         */
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        /**
         * This callback receives location updates from the
         * {@link FusedLocationProviderClient} object
         */
        mLocationCallback = new LocationCallback() {

            @Override
            public void onLocationResult(LocationResult locationResult) {

                // Sometimes, locationResults can be null,
                if (locationResult == null) {
                    return;
                }

                for (Location location : locationResult.getLocations()) {
                    // Update UI with location data
                    updateUI(location);
                }
            }
        };

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
            // Ask user for permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSION_TO_GET_LOCATION);
        } else {

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

        startTime = SystemClock.uptimeMillis();
        customTimeUpdationHandler.postDelayed(updateTimerThread, 0);
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

        float timeInSeconds = timeInMilliSeconds / 1000;

        float speed = distanceInMeters / timeInSeconds;

        updateSpeedUI(speed);

        updateDistanceUI(distanceInMeters);

    }

    /**
     * This method updates speed value in the UI
     *
     * @param speed Current speed of the user calculated
     */
    public void updateSpeedUI(float speed) {

        speedTextView.setText(new StringBuilder().append(String.valueOf(speed)).append(" m/s").toString());
    }

    /**
     * This method append location distance String value to the locationTextView
     *
     * @param distanceInMeters float value which was calculated using calculateDistanceInMeters
     */
    public void updateDistanceUI(float distanceInMeters) {
        locationTextView.append(new StringBuilder().append("\nDistance : ").append(String.valueOf(distanceInMeters)).toString());

        totalDistanceTravelledTextView.setText(new StringBuilder().append(totalDistanceTravelled)
                .append(" mtr(s)").toString());
    }

    /**
     * Runnable thread for calculating time taken.
     * This thread is used by the Handler to udpate time
     * in the Activity
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

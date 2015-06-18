package com.lastedit.towerfinder;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Matrix;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.RotateAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.widget.TextView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends ActionBarActivity implements SensorEventListener, LocationListener {

    private ImageView compassView;
    private TextView statusText;
    private TextView stationNameText;
    private TextView compassDegreeView;
    private TextView headingText;
    private TextView distanceText;

    private SensorManager mSensorManager;
    private LocationManager locationManager;
    Sensor accelerometer;
    Sensor magnetometer;

    public Location towerLoc = new Location("");

    double headingToTower = 0;
    double distanceToTower = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        compassView = (ImageView) findViewById(R.id.compassView);
        statusText = (TextView) findViewById(R.id.gpsStatusTextView);
        compassDegreeView = (TextView) findViewById(R.id.compassDegTextView);
        headingText = (TextView) findViewById(R.id.headingTextView);
        distanceText = (TextView) findViewById(R.id.distanceTextView);
        stationNameText = (TextView) findViewById(R.id.towerNameTextView);

        towerLoc.setLatitude(39.33472);
        towerLoc.setLongitude(-76.65083);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                showSettings();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected void onResume() {
        super.onResume();
        PreferenceSerializer ps = new PreferenceSerializer();
        SharedPreferences sharedPreferences = getSharedPreferences("com.lastedit.tower",Context.MODE_PRIVATE);
        HashMap<String, Object> selectedTower = (HashMap<String, Object>) ps.getObject(sharedPreferences, "selectedTower");
        if (selectedTower != null) {
            Log.d("tower",String.format("Selected tower: %s",selectedTower.toString()));
            stationNameText.setText((String) selectedTower.get("stationName"));
            towerLoc.setLatitude((Double) selectedTower.get("latitude"));
            towerLoc.setLongitude((Double) selectedTower.get("longitude"));
        }
        Log.d("tower",String.format("Tower Location: %f / %f",towerLoc.getLatitude(),towerLoc.getLongitude()));
        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 10, this);
        getCurrentLocation();
    }

    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
        locationManager.removeUpdates(this);
    }

    private void showSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
        //overridePendingTransition(android.R.anim.slide_in_left,android.R.anim.slide_out_right);
    }

    void getCurrentLocation() {
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_MEDIUM);
        statusText.setText("No location available.");

        List<String> providers = locationManager.getProviders(criteria, true);
        Log.d("Tower",String.format("%s",providers));
        int x = 1;
        if (providers.size() > 0) {
            Location newestLocation = null;
            for (String provider : providers) {
                Location location = locationManager.getLastKnownLocation(provider);
                if (location != null) {
                    if (newestLocation == null) {
                        newestLocation = location;
                    } else {
                        if (location.getElapsedRealtimeNanos() > newestLocation.getElapsedRealtimeNanos()) {
                            newestLocation = location;
                        }
                    }
                    locationManager.requestLocationUpdates(provider, 0, 0, this);
                }
                locationManager.requestLocationUpdates(provider, 0, 0, this);
            }

            if (newestLocation != null) {
                assert newestLocation != null;
                Date date = new Date(newestLocation.getTime());

                String provider;
                if (newestLocation.getProvider().equals("network"))
                    provider = "Network";
                else if (newestLocation.getProvider().equals("gps"))
                    provider = "GPS";
                else
                    provider = newestLocation.getProvider();

                statusText.setText(String.format("Using last known location. (%s)\nLast Seen: %s\n%f / %f",provider,date.toString(),newestLocation.getLatitude(), newestLocation.getLongitude()));
                headingToTower = normalizeDegree(newestLocation.bearingTo(towerLoc));
                distanceToTower = (newestLocation.distanceTo(towerLoc) / 1000) * 0.621371;

                headingText.setText(String.format("%d\u00B0",Math.round(headingToTower)));
                distanceText.setText(String.format("%d",Math.round(distanceToTower)));
            }
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Show location settings?");
            builder.setTitle("GPS is Disabled");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Intent settingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(settingsIntent);
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.cancel();
                }
            });
            builder.show();
        }
    }

    public void turn(double degrees)
    {
        RotateAnimation anim = new RotateAnimation((float) degrees, (float) degrees,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,0.5f);
        //currentRotation = (currentRotation + 30) % 360;

        anim.setInterpolator(new LinearInterpolator());
        anim.setDuration(0);
        anim.setFillEnabled(true);

        anim.setFillAfter(true);
        compassView.startAnimation(anim);
    }

    private double normalizeDegree(double value){
        if(value >= 0.0f && value <= 180.0f){
            return value;
        }else{
            return 180 + (180 + value);
        }
    };

    protected float[] lowPass( float[] input, float[] output ) {
        if ( output == null ) return input;

        for ( int i=0; i<input.length; i++ ) {
            output[i] = output[i] + 0.15f * (input[i] - output[i]);
        }
        return output;
    }

    protected double[] lowPass( double[] input, double[] output ) {
        if ( output == null ) return input;

        for ( int i=0; i<input.length; i++ ) {
            output[i] = output[i] + 0.10f * (input[i] - output[i]);
        }
        return output;
    }


    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d("Tower", String.format("onAccuracyChanged accuracy: %d", accuracy));
    }

    float[] mGravity;
    float[] mGeomagnetic;
    float azimuth;
    double[] azimuthDeg = new double[1];
    double[] azimuthDegSmooth = new double[1];
    long compassDeg;

    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            //mGravity = event.values.clone();
            mGravity = lowPass( event.values.clone(), mGravity );
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            //mGeomagnetic = event.values.clone();
            mGeomagnetic = lowPass( event.values.clone(), mGeomagnetic );
        if (mGravity != null && mGeomagnetic != null) {
            float R[] = new float[9];
            float I[] = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
            if (success) {
                float orientation[] = new float[3];
                //float orientationSmooth[] = new float[3];
                SensorManager.getOrientation(R, orientation);
                //lowPass(orientation, orientationSmooth);
                azimuth = orientation[0]; // orientation contains: azimuth, pitch and roll
                azimuthDeg[0] = Math.toDegrees(azimuth);
                azimuthDeg[0] = normalizeDegree(azimuthDeg[0]);
                //azimuthDegSmooth = lowPass(azimuthDeg,azimuthDegSmooth);
                //compassDeg = Math.round(normalizeDegree(azimuthDeg));
                //statusText.setText(String.format("%f\n%f\n%d",orientation[0],azimuthDeg[0],compassDeg));
                compassDegreeView.setText(String.format("%d\u00B0",Math.round(azimuthDeg[0])));
                turn(headingToTower - azimuthDeg[0]);
            }
        }
    }

    public void onLocationChanged(Location location) {
        // Called when a new location is found by the network location provider.

        String provider;
        if (location.getProvider().equals("network"))
            provider = "Network";
        else if (location.getProvider().equals("gps"))
            provider = "GPS";
        else
            provider = location.getProvider();

        statusText.setText(String.format("%s Location Acquired - %dft Accuracy\n%f / %f",provider,Math.round(location.getAccuracy() * 3.28084),location.getLatitude(), location.getLongitude()));

        headingToTower = normalizeDegree(location.bearingTo(towerLoc));
        distanceToTower = (location.distanceTo(towerLoc) / 1000) * 0.621371;

        headingText.setText(String.format("%d\u00B0",Math.round(headingToTower)));
        distanceText.setText(String.format("%d",Math.round(distanceToTower)));
        PreferenceSerializer ps = new PreferenceSerializer();
        SharedPreferences sharedPreferences = getSharedPreferences("com.lastedit.tower",Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putFloat("lastLongitude", (float) location.getLongitude());
        editor.putFloat("lastLatitude", (float) location.getLatitude());
        editor.commit();
        float lastLongitude,lastLatitude;
        lastLongitude = sharedPreferences.getFloat("lastLongitude",-1);
        lastLatitude = sharedPreferences.getFloat("lastLatitude",-1);

        Log.d("tower",String.format("Recorded location: %f / %f, Last Location: %f / %f",(float) location.getLongitude(), (float) location.getLatitude(),lastLongitude,lastLatitude));
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {}

    public void onProviderEnabled(String provider) {}

    public void onProviderDisabled(String provider) {}
    
}
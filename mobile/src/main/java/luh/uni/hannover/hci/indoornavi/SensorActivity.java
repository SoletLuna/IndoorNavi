package luh.uni.hannover.hci.indoornavi;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class SensorActivity extends AppCompatActivity implements SensorEventListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, MessageApi.MessageListener {

    private Sensor accelSensor;
    private Sensor magneticSensor;
    private Sensor linAccelSensor;
    private Sensor pressureSensor;
    private Sensor stepCountSensor;
    private Sensor stepDetectSensor;

    private SensorManager mSensorManager;

    private  float[] mAccelerometerReading = new float[3];
    private  float[] mMagnetometerReading = new float[3];
    private  float[] mMLinAcccelReading = new float[3];
    private float mPressureReading;
    private int stepDetectCount = 0;
    private int stepCount = 0;

    private  float[] mRotationMatrix = new float[9];
    private  float[] mOrientationAngles = new float[3];

    private List<float[]> accelSensorList = new ArrayList<>();
    private List<Float> accelMagSensorList = new ArrayList<>();
    private List<float[]> linAccelSensorList = new ArrayList<>();
    private List<Float> linMagSensorList = new ArrayList<>();
    private List<float[]> orientSensorList = new ArrayList<>();
    private List<Float> pressureSensorList = new ArrayList<>();
    private List<Integer> stepCountSensorList = new ArrayList<>();
    private List<Integer> stepDetectSensorList = new ArrayList<>();

    private Long startTime;
    private boolean tracking = false;
    private int trackCount = 0;

    private Handler mHandler;

    private String TAGAPI = "Google Api";
    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        initializeApi();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setVisibility(View.GONE);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!tracking) {
                    tracking = true;
                    startSensoring();
                    startTime = System.currentTimeMillis();
                }
                else {
                    tracking = false;
                    trackCount++;
                }
            }
        });
    }

    private void initializeApi() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    private void startSensoring() {
        // register Sensors and Listeners
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        accelSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        linAccelSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        magneticSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        pressureSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        stepCountSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        stepDetectSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);

        mSensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, linAccelSensor, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, magneticSensor, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, pressureSensor, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, stepCountSensor, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, stepDetectSensor, SensorManager.SENSOR_DELAY_UI);
        mHandler = new Handler();
        mHandler.post(collectData);
    }

    private void stopSensoring() {
        mSensorManager.unregisterListener(this, accelSensor);
        mSensorManager.unregisterListener(this, linAccelSensor);
        mSensorManager.unregisterListener(this, magneticSensor);
        mSensorManager.unregisterListener(this, pressureSensor);
        mSensorManager.unregisterListener(this, stepDetectSensor);
        mSensorManager.unregisterListener(this, stepCountSensor);

        saveSensorData();
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        if (sensorEvent.sensor == accelSensor) {
            System.arraycopy(sensorEvent.values, 0, mAccelerometerReading,
                    0, mAccelerometerReading.length);
        }

        else if (sensorEvent.sensor == linAccelSensor) {
            System.arraycopy(sensorEvent.values, 0, mMLinAcccelReading,
                    0, mMLinAcccelReading.length);
        }

        else if (sensorEvent.sensor == magneticSensor) {
            System.arraycopy(sensorEvent.values, 0, mMagnetometerReading,
                    0, mMagnetometerReading.length);
        }

        else if (sensorEvent.sensor == pressureSensor) {
            mPressureReading = sensorEvent.values[0];
        }

        else  if (sensorEvent.sensor == stepCountSensor) {
            stepCount = (int) sensorEvent.values[0];
        }

        else  if (sensorEvent.sensor == stepDetectSensor) {
            stepDetectCount++;
        }

        updateOrientationAngles();
    }

    // Compute the three orientation angles based on the most recent readings from
    // the device's accelerometer and magnetometer.
    public void updateOrientationAngles() {
        // Update rotation matrix, which is needed to update orientation angles.
        mSensorManager.getRotationMatrix(mRotationMatrix, null,
                mAccelerometerReading, mMagnetometerReading);

        // "mRotationMatrix" now has up-to-date information.

        mSensorManager.getOrientation(mRotationMatrix, mOrientationAngles);

        // "mOrientationAngles" now has up-to-date information.
    }

    private void saveSensorData() {
        saveDataArray(accelSensorList, "accel" + trackCount + ".txt");
        saveDataArray(linAccelSensorList, "linAccel" + trackCount + ".txt");
        saveDataArray(orientSensorList, "ori" + trackCount + ".txt");
        saveDataPoints(accelMagSensorList, "accelMag" + trackCount + ".txt");
        saveDataPoints(linMagSensorList, "linAccelMag" + trackCount + ".txt");
        saveDataPoints(stepCountSensorList, "stepCount" + trackCount + ".txt");
        saveDataPoints(stepDetectSensorList, "stepDetect" + trackCount + ".txt");
        saveDataPoints(pressureSensorList, "pressure" + trackCount + ".txt");

        clearSensorData();
    }

    private void clearSensorData() {
        accelSensorList.clear();
        linAccelSensorList.clear();
        accelMagSensorList.clear();
        linMagSensorList.clear();
        pressureSensorList.clear();
        stepCountSensorList.clear();
        stepDetectSensorList.clear();
        orientSensorList.clear();
    }

    private void saveDataArray(List<float[]> data, String filename) {
        File root = new File(Environment.getExternalStorageDirectory(),"WiFiApp/SensorData");

        StringBuilder sb = new StringBuilder();
        Log.d("LENGTH", data.size() +"");
        for (int i=0; i < data.size(); i++) {
            sb.append(Arrays.toString(data.get(i)));
            sb.append(System.getProperty("line.separator"));
        }

        try {
            File myFile = new File(root, filename);
            FileOutputStream fos = new FileOutputStream(myFile);
            fos.write(sb.toString().getBytes());
            fos.getFD().sync();
            fos.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveDataPoints(List<?> data, String filename) {
        File root = new File(Environment.getExternalStorageDirectory(),"WiFiApp/SensorData");

        StringBuilder sb = new StringBuilder();

        for (int i=0; i < data.size(); i++) {
            sb.append(data.get(i));
            sb.append(System.getProperty("line.separator"));
        }

        try {
            File myFile = new File(root, filename);
            FileOutputStream fos = new FileOutputStream(myFile);
            fos.write(sb.toString().getBytes());
            fos.getFD().sync();
            fos.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private Runnable collectData = new Runnable() {
        @Override
        public void run() {
            float[] tmpAccel = new float[]{mAccelerometerReading[0], mAccelerometerReading[1], mAccelerometerReading[2]};
            accelSensorList.add(tmpAccel);
            float mag = (float) Math.sqrt(mAccelerometerReading[0] * mAccelerometerReading[0] + mAccelerometerReading[1] * mAccelerometerReading[1] + mAccelerometerReading[2] * mAccelerometerReading[2]);
            accelMagSensorList.add(mag);
            float[] tmpLinAccel = new float[]{mMLinAcccelReading[0], mMLinAcccelReading[1], mMLinAcccelReading[2]};
            linAccelSensorList.add(tmpLinAccel);
            float linMag = (float) Math.sqrt(mMLinAcccelReading[0] * mMLinAcccelReading[0] + mMLinAcccelReading[1] * mMLinAcccelReading[1] + mMLinAcccelReading[2] * mMLinAcccelReading[2]);
            linMagSensorList.add(linMag);
            float[] tmpOri = new float[]{mOrientationAngles[0], mOrientationAngles[1], mOrientationAngles[2]};
            orientSensorList.add(tmpOri);
            pressureSensorList.add(mPressureReading);
            stepDetectSensorList.add(stepCount);
            stepCountSensorList.add(stepDetectCount);
            if (tracking)
                mHandler.postDelayed(collectData, 100);
            else {
                stopSensoring();
                mHandler.removeCallbacks(collectData);
            }
        }
    };

    @Override
    protected void onStart() {
        super.onStart();

        mGoogleApiClient.connect();
        Wearable.MessageApi.addListener(mGoogleApiClient, this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d("MESSAGE", "received");
        if (!tracking) {
            tracking = true;
            startSensoring();
            startTime = System.currentTimeMillis();
        }
        else {
            tracking = false;
            trackCount++;
        }
    }
}

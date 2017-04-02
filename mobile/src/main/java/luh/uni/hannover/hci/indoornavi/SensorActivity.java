package luh.uni.hannover.hci.indoornavi;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

public class SensorActivity extends AppCompatActivity implements SensorEventListener {

    private Sensor accelSensor;
    private Sensor magneticSensor;
    private Sensor linAccelSensor;
    private Sensor pressureSensor;
    private Sensor stepCountSensor;
    private Sensor stepDetectSensor;

    private SensorManager mSensorManager;

    private final float[] mAccelerometerReading = new float[3];
    private final float[] mMagnetometerReading = new float[3];

    private final float[] mRotationMatrix = new float[9];
    private final float[] mOrientationAngles = new float[3];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startSensoring();
                view.setVisibility(View.INVISIBLE);
            }
        });
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
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        if (sensorEvent.sensor == accelSensor) {
            System.arraycopy(sensorEvent.values, 0, mAccelerometerReading,
                    0, mAccelerometerReading.length);
        }

        else if (sensorEvent.sensor == linAccelSensor) {

        }

        else if (sensorEvent.sensor == magneticSensor) {
            System.arraycopy(sensorEvent.values, 0, mMagnetometerReading,
                    0, mMagnetometerReading.length);
        }

        else if (sensorEvent.sensor == pressureSensor) {

        }

        else  if (sensorEvent.sensor == stepCountSensor) {

        }

        else  if (sensorEvent.sensor == stepDetectSensor) {

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

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}

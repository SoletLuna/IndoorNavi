package luh.uni.hannover.hci.indoornavi.Services;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import luh.uni.hannover.hci.indoornavi.Utilities.SensorCoordinator;

/**
 * Created by solet on 10/04/2017.
 * <p>
 * Service that will be used for the dead reckoning approach, thus it handle step detection.
 * It also should provide the current orientation (step length?) and the direction the user is moving.
 */

public class SensorService extends Service implements SensorEventListener {

    private SensorManager mSensorManager;
    private Sensor accelSensor;
    private Sensor magSensor;
    private String TAG = "SensorService";
    private SensorCoordinator mSensorCoordinator;

    private Handler mHandler;

    private static long PEAK_WINDOW_SIZE = 450;
    private static long MOVING_WINDOW_SIZE = 390;
    private static float ALPHA = 0.25f;
    private static float PEAK_THRESHOLD = 11.0f;

    private float[] mAccelerometerReading = new float[3];
    private List<SensorData> magnitudeAccelReadingList = new ArrayList<>();
    private List<SensorData> smoothedMagnitudeList = new ArrayList<>();
    private int lastStepIndex = 0;

    @Override
    public void onCreate() {
        mSensorManager = (SensorManager) getApplicationContext().getSystemService(SENSOR_SERVICE);
        accelSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mSensorCoordinator = new SensorCoordinator();
        mHandler = new Handler();
    }

    @Override
    public int onStartCommand(Intent i, int flags, int startId) {

        mSensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, magSensor, SensorManager.SENSOR_DELAY_FASTEST);
        Log.d(TAG, "Registered");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        mSensorManager.unregisterListener(this, accelSensor);
        mSensorManager.unregisterListener(this, magSensor);
        Log.d(TAG, "Unregistered");
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(sensorEvent.values, 0, mAccelerometerReading,
                    0, mAccelerometerReading.length);
            float magnitude = (float) Math.sqrt(mAccelerometerReading[0] * mAccelerometerReading[0] + mAccelerometerReading[1] * mAccelerometerReading[1] + mAccelerometerReading[2] * mAccelerometerReading[2]);
            magnitudeAccelReadingList.add(new SensorData(magnitude, sensorEvent.timestamp));
           // addSmoothedValue();
           // boolean step = detectStep(smoothedMagnitudeList);
           // onStep();
        }
    }

    private void addSmoothedValue() {
        SensorData first = magnitudeAccelReadingList.get(0);
        SensorData last = magnitudeAccelReadingList.get(magnitudeAccelReadingList.size()-1);
        if ( (last.time - first.time) < 310000000) {
            return;
        } else {
            double average = 0.0f;
            int amount = 0;
            for (int i=magnitudeAccelReadingList.size()-1; i >= 0; i++) {
                if ( (last.time - magnitudeAccelReadingList.get(i).time) < 310000000) {
                    double value = magnitudeAccelReadingList.get(i).value;
                    average += value;
                    amount++;
                } else {
                    average /= amount;
                    SensorData data = new SensorData(average, last.time);
                    smoothedMagnitudeList.add(data);
                    return;
                }
            }
        }
    }

    // uses a peak detection using thresholds for peak and valley
    private boolean detectStep(List<SensorData> list) {
        long currentTime = list.get(list.size()-1).time;
        double max = 0.0;
        double min = 50;
        boolean peak = false;
        boolean valley = false;
        boolean flag = false;
        for (int i=0; i < list.size()-1; i++) {
            double current = list.get(i).value;
            if ((currentTime - list.get(i).time) > 500000000) {
                continue;
            } else {
                if (!peak && !valley) {
                    max = current;
                    peak = true;
                } else if (peak) {
                    if (current > max) {
                        max = current;
                    } else if (max > 11) {
                        if (flag) {
                            return true;
                        } else {
                            flag = true;
                            peak = false;
                            valley = true;
                            min = current;
                        }
                    }
                } else if (valley) {
                    if (current < min) {
                        min = current;
                    } else if (min < 10) {
                        if( flag) {
                            return true;
                        } else {
                            flag = true;
                            valley = false;
                            peak = true;
                            max = current;
                        }
                    }
                }
            }
        }
        return false;
    }

    public class SensorData {

        double value;
        long time;

        public SensorData(double value, long time) {
            this.value = value;
            this.time = time;
        }
    }

    private void onStep() {
        Intent i = new Intent("Step");
        LocalBroadcastManager.getInstance(this).sendBroadcast(i);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

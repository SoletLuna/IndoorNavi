package luh.uni.hannover.hci.indoornavi.Services;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

/**
 * Created by solet on 10/04/2017.
 *
 * Service that will be used for the dead reckoning approach, thus it handle step detection.
 * It also should provide the current orientation (step length?) and the direction the user is moving.
 */

public class SensorService extends Service implements SensorEventListener{

    private SensorManager mSensorManager;
    private Sensor accel;
    private Sensor mag;
    private String TAG = "SensorService";

    private static long PEAK_WINDOW_SIZE = 500;
    private static long MOVING_WINDOW_SIZE = 390;

    @Override
    public void onCreate() {
        mSensorManager = (SensorManager) getApplicationContext().getSystemService(SENSOR_SERVICE);
        accel = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mag = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    @Override
    public int onStartCommand(Intent i, int flags, int startId) {

        mSensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mag, SensorManager.SENSOR_DELAY_FASTEST);
        Log.d(TAG, "Registered");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        mSensorManager.unregisterListener(this, accel);
        mSensorManager.unregisterListener(this, mag);
        Log.d(TAG, "Unregistered");
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            //onStep();
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

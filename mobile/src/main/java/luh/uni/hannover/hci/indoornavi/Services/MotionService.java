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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by solet on 10/04/2017.
 * <p>
 * Service that will be used for the dead reckoning approach, thus it handle step detection.
 * It also should provide the current orientation (step length?) and the direction the user is moving.
 */

public class MotionService extends Service implements SensorEventListener {


    SensorManager sManager;
    Sensor stepCountSensor;
    Sensor stepDetectSensor;
    Sensor accelSensor;

    double peakThreshold = 11.0;
    double valleyThreshold = 8.0;
    long timeThreshold = 500000000;
    boolean peakDetected = false;
    boolean valleyDetected = false;
    boolean simpleDetector = true;
    List<Double> peakList = new ArrayList<>();
    List<Double> valleyList = new ArrayList<>();
    List<Double> stepList = new ArrayList<>();
    List<Long> timeList = new ArrayList<>();
    long lastStepTime = 0;
    List<Double> magnitudeList = new ArrayList<>();
    List<Double> lowPassList = new ArrayList<>();

    double adaptiveStepAverage = 9.8;
    double adaptiveTimeThreshold = 500000000;
    double adaptiveStepDeviation = 0;
    double adaptiveTimeDeviation = 0;
    double lastPeak = 0;

    String TAG = "StepThings";

    @Override
    public void onCreate() {
        sManager = (SensorManager) getApplicationContext().getSystemService(SENSOR_SERVICE);
        stepCountSensor = sManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        stepDetectSensor = sManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        accelSensor = sManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    @Override
    public int onStartCommand(Intent i, int flags, int startId) {

        sManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_UI);
        /*sManager.registerListener(this, stepCountSensor, SensorManager.SENSOR_DELAY_UI);
        sManager.registerListener(this, stepDetectSensor, SensorManager.SENSOR_DELAY_UI);*/
        Log.d(TAG, "Registered");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        sManager.unregisterListener(this, accelSensor);
        //  mSensorManager.unregisterListener(this, magSensor);
        Log.d(TAG, "Unregistered");
    }

    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor == accelSensor) {
            float sum = 0;
            for (float f: sensorEvent.values) {
                sum += f*f;
            }
            double mag = Math.sqrt(sum);

            if (magnitudeList.size() < 3) {
                magnitudeList.add(mag);
            }
            else {
                magnitudeList.remove(0);
                magnitudeList.add(mag);
                double low = 0;
                if (lowPassList.isEmpty()) {
                    low = lowPass(mag, magnitudeList.get(1), 0.5);
                }
                else {
                    low = lowPass(mag, lowPassList.get(lowPassList.size()-1), 0.8);
                }
                lowPassList.add(low);
            }

            if (magnitudeList.size() == 3) {
                if (simpleDetector)
                    detectPV(sensorEvent.timestamp);
                else
                    detectDynamicPV(sensorEvent.timestamp);
            }
        }
    }

    private void detectPV(long time) {
        double previous = magnitudeList.get(0);
        double middle = magnitudeList.get(1);
        double last = magnitudeList.get(2);
        String type = "";
        if (middle > Math.max(previous, last)) {
            type = "Peak";
        }
        if (middle < Math.min(previous, last)) {
            type = "Valley";
        }

        if (type != "") {
            detectStep(type, middle, time);
        }
    }

    private void detectDynamicPV(long time) {
        double previous = magnitudeList.get(0);
        double middle = magnitudeList.get(1);
        double last = magnitudeList.get(2);
        String type = "";
        if (middle > Math.max(Math.max(previous, last), adaptiveStepAverage + adaptiveStepDeviation)) {
            type = "Peak";
        }
        if (middle < Math.min(Math.min(previous, last), adaptiveStepAverage - adaptiveStepDeviation)) {
            type = "Valley";
        }

        if (type != "") {
            detectDynamicStep(type, middle, time);
        }
    }

    private void detectStep(String type, double value, long time) {
        if (type == "Peak") {
            if (value >= peakThreshold) {
                long timeDiff = time - lastStepTime;
                if (timeDiff >= timeThreshold) {
                    peakDetected = true;
                }
            }
        }
        else if (type == "Valley") {
            if (peakDetected) {
                if (value < valleyThreshold) {
                    valleyList.add(value);
                    lastStepTime = time;
                    peakDetected = false;
                    reportStep();
                }
            }
        }
    }

    private void detectDynamicStep(String type, double value, long time) {
        //Log.d(TAG, type + "," + value + "," + time);
        if (type == "Peak") {
            long timeDiff = time - lastStepTime;
            //if (timeDiff >= adaptiveTimeThreshold - adaptiveTimeThreshold) {
            peakDetected = true;
            lastPeak = value;
            valleyDetected = false;
            // }
        }
        else if (type == "Valley") {
            if (peakDetected) {
                long timeDiff = time - lastStepTime;
                lastStepTime = time;
                adaptiveTimeThreshold = timeDiff;
                timeList.add(timeDiff);
                adaptiveStepAverage = (lastPeak + value) / 2;
                Log.d(TAG, adaptiveStepAverage + "");
                stepList.add(adaptiveStepAverage);
                peakDetected = false;
                valleyDetected = true;
                reportStep();
                updateThresholds();
            }
        }
    }

    private void updateThresholds() {
        int peek = 5;

        double sumStep = 0;
        double sumTime = 0;
        if (stepList.size() < 5 || timeList.size() < 5) {
            peek = stepList.size();
        }
        for (int i=stepList.size() - 1; i >= stepList.size() - peek; i--) {
            sumStep += stepList.get(i);
        }
        for (int i=timeList.size() - 1; i >= timeList.size() - peek; i--) {
            sumTime += timeList.get(i);
        }

        double averageStepAverage = sumStep/peek;
        double averageTimeThreshold = sumTime/peek;

        //calculate deviation
        double sumStepDev = 0;
        double sumTimeDev = 0;
        for (int i=stepList.size() -1; i > stepList.size() - peek; i--) {
            sumStepDev += Math.pow(stepList.get(i) - averageStepAverage, 2);
        }
        for (int i=timeList.size() -1; i > timeList.size() - peek; i--) {
            sumTimeDev += Math.pow(timeList.get(i) - averageTimeThreshold, 2);
        }

        adaptiveStepDeviation = Math.sqrt(sumStepDev);
        adaptiveTimeDeviation = Math.sqrt(sumTimeDev);

    }

    private double lowPass(double current, double previous, double alpha) {
        double value = (current * alpha) + (previous * (1 - alpha));
        return value;
    }


    private void reportStep() {
        onStep();
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

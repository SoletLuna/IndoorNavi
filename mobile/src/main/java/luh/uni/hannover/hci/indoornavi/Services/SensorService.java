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
import java.util.Arrays;
import java.util.LinkedList;
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

    private float[] mAccelerometerReading = new float[3];

    // Step Detection Robust Dynamics
    private LinkedList<Double> magnitudeList = new LinkedList<>();
    private LinkedList<TypeData> typeList = new LinkedList<>();
    private int magnitudeIndex = 0;
    private int stepsDetected = 0;

    private double stepAverage = 9.8;
    private double peakMag = 0;
    private double valleyMag = 0;
    private double stepDeviation = 0;
    // Last K samples taken into account
    private int K = 25;
    // Last M peak/valley pairs taken into account
    private int M = 10;
    // magnitude constant
    private double alpha = 4;
    // time scale constant
    private double beta = 1 / 3;
    private double averagePeakTime = 0;
    private double averageValleyTime = 0;
    private double peakTimeDeviation = 0;
    private double valleyTimeDeviation = 0;
    private double lastPeakTime = 0;
    private double lastValleyTime = 0;
    private double adaptivePeakThreshold = 0;
    private double adaptiveValleyThreshold = 0;
    private double lastPeakMagnitude = 0;
    private double lastValleyMagnitude = 0;

    // From here to the old step detector
    private ArrayList<SensorData> accelMagnitudeList = new ArrayList<>();
    private ArrayList<SensorData> accelMovWindow = new ArrayList<>();

    // threshold for walk detection
    private static double MAGNITUDE_THRESHOLD = 10.5;
    // in nanoseconds
    private static double WALKING_THRESHOLD_SIZE = 500000000;
    private static double MOVING_AVERAGE_SIZE = 310000000;
    private static double WINDOW_PEAK_SIZE = 590000000;

    long curWalkTimeStamp;
    long curMovTimeStamp;
    double stepTime = 0;
    int count = 0;

    @Override
    public void onCreate() {
        mSensorManager = (SensorManager) getApplicationContext().getSystemService(SENSOR_SERVICE);
        accelSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        typeList.add(new TypeData("init", 0));
    }

    @Override
    public int onStartCommand(Intent i, int flags, int startId) {

        mSensorManager.registerListener(this, accelSensor, 20000);
        // mSensorManager.registerListener(this, magSensor, SensorManager.SENSOR_DELAY_FASTEST);
        Log.d(TAG, "Registered");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        mSensorManager.unregisterListener(this, accelSensor);
        //  mSensorManager.unregisterListener(this, magSensor);
        Log.d(TAG, "Unregistered");
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(sensorEvent.values, 0, mAccelerometerReading,
                    0, mAccelerometerReading.length);
            double magnitude = Math.sqrt(mAccelerometerReading[0] * mAccelerometerReading[0] + mAccelerometerReading[1] * mAccelerometerReading[1] + mAccelerometerReading[2] * mAccelerometerReading[2]);
            magnitudeList.add(magnitude);
            magnitudeIndex++;
            // 0 = an+1, 1 = an, 2 = an-1
            if (!(magnitudeList.size() < 3)) {
                if (magnitudeList.size() >= K) {
                    magnitudeList.removeFirst();
                }
                int magSize = magnitudeList.size();
                Double[] mags = new Double[3];
                mags[0] = magnitudeList.getLast();
                mags[1] = magnitudeList.get(magnitudeList.size()-2);
                mags[2] = magnitudeList.get(magnitudeList.size()-3);
                // oldStepDetector(sensorEvent);
                adaptiveStepDetector(mags);
            }
        }
    }

    /**
     * Step deviation fehlt noch
     * @param mags
     */
    private void adaptiveStepDetector(Double[] mags) {
        String candidate = detectCandidate(mags);
        Log.d(TAG, candidate);
        String type = "inter";
        String lastType = typeList.getLast().type;
        if (candidate.equals("peak")) {
            if (lastType.equals("init")) {
                type = "peak";
                typeList.add(new TypeData(type, magnitudeIndex-1));
                updatePeak(mags[1], magnitudeIndex - 1);
            } else if (lastType.equals("valley") && (magnitudeIndex - 1 - lastPeakTime) > adaptivePeakThreshold) {
                type = "peak";
                typeList.add(new TypeData(type, magnitudeIndex-1));
                updatePeak(mags[1], magnitudeIndex - 1);
                //stepAverage = (lastPeakMagnitude - lastValleyMagnitude) / 2;
            } else if (lastType.equals("peak") && (magnitudeIndex - 1 - lastPeakTime) < adaptiveValleyThreshold
                    && mags[1] > lastPeakMagnitude) {
                typeList.add(new TypeData(type, magnitudeIndex-1));
                updateValley(mags[1], magnitudeIndex - 1);

            }
        } else if (candidate.equals("valley")) {
            if (lastType.equals("peak") && (magnitudeIndex - 1 - lastValleyTime) > adaptiveValleyThreshold) {
                type = "valley";
                typeList.add(new TypeData(type, magnitudeIndex-1));
                updateValley(mags[1], magnitudeIndex-1);
                stepsDetected++;
                onStep();
                //stepAverage = (lastPeakMagnitude - lastValleyMagnitude) / 2;
            } else if (lastType.equals("valley") && (magnitudeIndex - 1 - lastValleyTime) < adaptiveValleyThreshold
                    && mags[1] < lastValleyMagnitude) {
                typeList.add(new TypeData(type, magnitudeIndex-1));
                updateValley(mags[1], magnitudeIndex - 1);
            }
        }
    }

    private String detectCandidate(Double[] mags) {
        if (magnitudeList.size() < 3) {
            return "inter";
        }

        double previous = mags[2];
        double current = mags[1];
        double next = mags[0];
        double stepThresholdPeak = stepAverage + (stepDeviation / alpha);
        if (current > Math.max(Math.max(previous, next), stepThresholdPeak)) {
            return "peak";
        }
        double stepThresholdValley = stepAverage - (stepDeviation / alpha);
        if (current < Math.min(Math.min(previous, next), stepThresholdValley)) {
            return "valley";
        }

        return "inter";
    }

    private void updatePeak(double mag, int n) {
        lastPeakTime = n;
        lastPeakMagnitude = mag;
        int peakCount = 0;
        List<Integer> timeList = new ArrayList<>();

        //find all peaks with respective time
        for (int i=typeList.size()-1; i >= 0; i--) {
            if (peakCount < 10) {
                String type = typeList.get(i).type;
                if (type.equals("peak")) {
                    timeList.add(typeList.get(i).n);
                    peakCount++;
                }
            } else {
                break;
            }
        }
        int sum = 0;
        for (int i=0; i < timeList.size()-1; i++) {
            sum += timeList.get(i);
        }
        averagePeakTime = sum / timeList.size();

        int sumSD = 0;
        for (int i=0; i < timeList.size()-1; i++) {
            sumSD += Math.pow(timeList.get(i) - averagePeakTime,2);
        }
        peakTimeDeviation = Math.sqrt(sumSD/timeList.size());
    }

    private void updateValley(double mag, int n) {
        lastPeakTime = n;
        lastPeakMagnitude = mag;
        int valleyCount = 0;
        List<Integer> timeList = new ArrayList<>();

        //find all peaks with respective time
        for (int i=typeList.size()-1; i >= 0; i--) {
            if (valleyCount < M) {
                String type = typeList.get(i).type;
                if (type.equals("peak")) {
                    timeList.add(typeList.get(i).n);
                    valleyCount++;
                }
            } else {
                break;
            }
        }
        int sum = 0;
        for (int i=0; i < timeList.size()-1; i++) {
            sum += timeList.get(i);
        }
        averageValleyTime = sum / timeList.size();

        int sumSD = 0;
        for (int i=0; i < timeList.size()-1; i++) {
            sumSD += Math.pow(timeList.get(i) - averageValleyTime,2);
        }
        valleyTimeDeviation = Math.sqrt(sumSD/timeList.size());

    }

    private void oldStepDetector(SensorEvent sensorEvent) {
        float x = sensorEvent.values[0];
        float y = sensorEvent.values[1];
        float z = sensorEvent.values[2];
        double magnitude = Math.sqrt(x * x + y * y + z * z);
        accelMagnitudeList.add(new SensorData(magnitude, sensorEvent.timestamp));
        //Log.e("ACCEL", x + "-" + y + "-" + z);
        // walking detection using magnitude threshold
        if (magnitude > MAGNITUDE_THRESHOLD) {
            //walking = true;
            curWalkTimeStamp = sensorEvent.timestamp;
        } else {
            if ((sensorEvent.timestamp - curWalkTimeStamp) > WALKING_THRESHOLD_SIZE) {
                // walking = false;
            }
        }

        // moving average, counting downwards
        curMovTimeStamp = sensorEvent.timestamp;
        int winCount = 0;
        double avgMag = 0.0;
        for (int i = accelMagnitudeList.size() - 1; i >= 0; i--) {
            long time = accelMagnitudeList.get(i).time;
            double mag = accelMagnitudeList.get(i).value;
            if ((curMovTimeStamp - time) < MOVING_AVERAGE_SIZE) {
                winCount++;
                avgMag += mag;
                // Log.e("BLA", winCount +"");
            } else {
                break;
            }
        }
        if (winCount > 0) {
            avgMag = avgMag / winCount;
            accelMovWindow.add(new SensorData(avgMag, curMovTimeStamp));
            //Log.e("Window", avgMag +"");
        }

        //detect peak in moving average
        double max = 0;
        double min = 100;
        boolean isPeak = false;
        boolean isStep = false;
        for (int i = accelMovWindow.size() - 1; i >= 0; i--) {
            if (curMovTimeStamp - stepTime < WINDOW_PEAK_SIZE) {
                break;
            }
            long time = accelMovWindow.get(i).time;
            double mag = accelMovWindow.get(i).value;
            if ((curMovTimeStamp - time) < WINDOW_PEAK_SIZE) {
                if (!isPeak) {
                    if (mag > max) {
                        max = mag;
                    } else if (mag < max) {
                        if (max > MAGNITUDE_THRESHOLD)
                            isPeak = true;
                    }
                } else {
                    if (mag < min) {
                        min = mag;
                    } else if (mag > min) {
                        if (min < MAGNITUDE_THRESHOLD)
                            isStep = true;
                    }
                }
            } else {
                break;
            }
        }
        if (isStep && curMovTimeStamp - stepTime > WINDOW_PEAK_SIZE) {
            count++;
            stepTime = sensorEvent.timestamp;
            onStep();
        }

    }

    public class SensorData {

        double value;
        long time;

        public SensorData(double value, long time) {
            this.value = value;
            this.time = time;
        }
    }

    public class TypeData {
        String type;
        int n;

        public TypeData(String type, int n){
            this.type = type;
            this.n = n;
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

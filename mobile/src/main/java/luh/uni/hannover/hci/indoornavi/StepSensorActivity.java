package luh.uni.hannover.hci.indoornavi;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StepSensorActivity extends AppCompatActivity implements SensorEventListener {

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
    private StringBuilder sb = new StringBuilder();
    Handler mHandler;
    int stepCounterValue = 0;
    int stepDetectorValue = 0;
    int simpleStepValue = 0;
    boolean logging = false;
    boolean starting = false;
    int counter = 0;
    TextView tv;

    String TAG = "StepThings";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_step_sensor);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mHandler = new Handler();
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        tv = (TextView) findViewById(R.id.stepsensortextview);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!logging) {
                    logging = true;
                    mHandler.postDelayed(start, 5000);
                }
                else {
                    saveLogFile();
                    logging = false;
                    mHandler.removeCallbacks(start);
                    starting = false;
                }
            }
        });
    }

    private Runnable start = new Runnable() {
        @Override
        public void run() {
            if (!starting) {
                registerSensors();
                starting = true;
                sb.append("Simple - Detector");
                sb.append(System.lineSeparator());
            }

            if (starting == true) {
                sb.append(simpleStepValue + "," + stepDetectorValue);
                sb.append(System.lineSeparator());
            }
            mHandler.postDelayed(start, 1000);
        }
    };

    protected void onDestroy() {
        super.onDestroy();
        unregisterSensors();
    }

    private void registerSensors() {
        sManager = (SensorManager) getApplicationContext().getSystemService(SENSOR_SERVICE);
        stepCountSensor = sManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        stepDetectSensor = sManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        accelSensor = sManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        sManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_UI);
        sManager.registerListener(this, stepDetectSensor, SensorManager.SENSOR_DELAY_UI);
    }

    private void unregisterSensors() {
        sManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor == stepDetectSensor) {
            stepDetectorValue++;
        }
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
        simpleStepValue++;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public void saveLogFile() {
        File root = new File(Environment.getExternalStorageDirectory(), "IndoorNavigation");
        String name ="stepcounting.txt";
        File[] fileArr = root.listFiles();
        List<String> fileNames = new ArrayList<>();
        for (File f : fileArr) {
            fileNames.add(f.getName());
        }

        int counter = 1;
        while (fileNames.contains(name)) {
            counter++;
            name = "stepcounting" + counter + ".txt";
        }

        if (!root.mkdirs()) {
            Log.e(TAG, "Directory not created");
        }

        try {
            File myFile = new File(root, name);
            FileOutputStream fos = new FileOutputStream(myFile);
            fos.write(sb.toString().getBytes());
            fos.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}

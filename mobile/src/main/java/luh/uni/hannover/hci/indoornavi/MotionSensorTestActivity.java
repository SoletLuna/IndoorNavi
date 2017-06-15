package luh.uni.hannover.hci.indoornavi;

import android.content.DialogInterface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MotionSensorTestActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager mSensorManager;
    private Sensor accSensor;
    private Sensor gyroSensor;
    private List<Double> accList = new ArrayList<>();
    private List<Double> avgAccList = new ArrayList<>();
    private List<Float> gyroList = new ArrayList<>();
    private List<Float> avgGyroList = new ArrayList<>();
    private boolean active = false;
    private int windowSize = 10;

    private double stepTime;
    private double stepThreshold;
    private double minStepTime = 250000000; // in seconds for now
    private double minStepThreshold = 5; // for now
    private String previousCandidate = "";
    private double lastPeakValue = 0;
    private double lastValleyValue = 0;
    private double lastPeakTime;
    private double lastValleyTime;
    private boolean skipValue = false;
    private boolean skipTime = false;
    private boolean setup = false;

    private int samples = 25;
    private int timeSamples = 5;
    private int thresholdSamples = 5;

    //calibration
    private boolean calibrated = false;
    private List<Double> toCalibrateList = new ArrayList<>();
    private List<Double> calibratedList = new ArrayList<>();

    private String TAG = "MotionTest";
    private StringBuilder sb = new StringBuilder();
    private StringBuilder sb2 = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_step_test);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (active) {
                    stopSensors();
                    active = false;
                    saveLogFile();
                } else {
                    setup = true;
                    startSensors();
                    active = true;
                }
            }
        });

        mSensorManager = (SensorManager) getApplicationContext().getSystemService(SENSOR_SERVICE);
        accSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
    }

    private void startSensors() {
        mSensorManager.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_UI);
       // mSensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

    private void stopSensors() {
        mSensorManager.unregisterListener(this);
    }

    private void stepDetected() {
        Log.d(TAG, "step");
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        // small scale first time setup
        if (setup) {
            lastValleyTime = sensorEvent.timestamp;
            lastPeakTime = sensorEvent.timestamp;
            stepTime = minStepTime;
            stepThreshold = minStepThreshold;
            setup = false;
        }
        if (sensorEvent.sensor == accSensor) {
            float x = sensorEvent.values[0];
            float y = sensorEvent.values[1];
            float z = sensorEvent.values[2];
            double value = Math.sqrt(x * x + y * y + z * z);
            if (calibrated) {
                addValue(accList, value, "live");
                checkStep(sensorEvent.timestamp);
            } else {
                addValue(toCalibrateList, value, "calibrate");
            }
        } else if (sensorEvent.sensor == gyroSensor) {
            //addValue(value, gyroSensor);
        }
    }

    private void addValue(List<Double> list, double value, String mode) {
        list.add(value);
        if (mode == "calibrate") {

        } else {
            if (list.size() > windowSize) {
                list.remove(0);
            }
        }
    }

    private String getCandidate() {
        String state = "inter";
        double prev = avgAccList.get(0);
        double current = avgAccList.get(1);
        double next = avgAccList.get(2);

        double max = Math.max(prev, next);
        double min = Math.min(prev, next);

        if (current > max) {
            state = "peak";
        } else if (current < min) {
            state = "valley";
        }

        return state;
    }

    private void checkStep(long time) {
        averageWindow(windowSize);
        if (avgAccList.size() < 3) {
            return;
        }
        String candidate = getCandidate();
        switch (candidate) {
            case "peak":
                updatePeak(time);
                break;
            case "valley":
                updateValley(time);
                break;
            default:
                return;
        }

        previousCandidate = candidate;
    }

    public void updatePeak(long time) {
        double current = avgAccList.get(1);
        switch (previousCandidate) {
            case "valley":
                double timeDiff = time - lastValleyTime;
                if (timeDiff < stepTime) {
                    skipTime = true;
                } else {
                    skipTime = false;
                }
                break;
            default:
                break;
        }

        lastPeakValue = current;
        lastPeakTime = time;
    }

    public void updateValley(long time) {
        double current = avgAccList.get(1);
        switch (previousCandidate) {
            case "peak":
                double diff = Math.abs(lastPeakValue - current);
                if (diff > stepThreshold) {
                    if (!skipTime) {
                        stepDetected();
                        double timeDiff = time - lastPeakTime;
                        updateStepTime(timeDiff);
                    }
                    updateStepThreshold(diff);
                } else if (diff > minStepThreshold) {
                    if (!skipTime) {
                        double timeDiff = time - lastPeakTime;
                        updateStepTime(timeDiff);
                    }
                    updateStepThreshold(diff);
                }
                break;
            default:
                break;
        }

        lastValleyValue = current;
        lastValleyTime = time;
    }

    private void updateStepTime(double time) {

    }

    private void updateStepThreshold(double diff) {

    }

    public void addValueAccel(double value, Sensor type) {
        accList.add(value);
        if (accList.size() > windowSize) {
            accList.remove(0);
        }
        Log.d(TAG, "Value: " + value);
        sb.append(value);
        sb.append(System.lineSeparator());
    }

    public void averageWindow(int size) {
        double avg = 0;
        for (double value : accList) {
            avg += value;
        }
        avg /= accList.size();
        avgAccList.add(avg);
        if (avgAccList.size() > 3) {
            avgAccList.remove(0);
        }
        Log.d(TAG, "Window: " + avg);
        sb2.append(avg);
        sb2.append(System.lineSeparator());
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public void saveLogFile() {
        File root = new File(Environment.getExternalStorageDirectory(), "IndoorNavigation");
        String name ="step.txt";
        String name2 ="window.txt";
        File[] fileArr = root.listFiles();
        List<String> fileNames = new ArrayList<>();
        for (File f : fileArr) {
            fileNames.add(f.getName());
        }

        int counter = 1;
        while (fileNames.contains(name)) {
            counter++;
            name = "step" + counter + ".txt";
        }
        while (fileNames.contains(name2)) {
            counter++;
            name2 = "window" + counter + ".txt";
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

        try {
            File myFile = new File(root, name2);
            FileOutputStream fos = new FileOutputStream(myFile);
            fos.write(sb2.toString().getBytes());
            fos.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_motion, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();


        switch (id) {
            case R.id.start_calibrate:
                if (calibrated) {
                    stopCalibration();
                } else {
                    startCalibration();
                }
                return true;
            case R.id.test_calibrate:
        }

        return super.onOptionsItemSelected(item);
    }

    private void startCalibration() {
        calibrated = false;
        startSensors();
    }

    private void addValueCalibrate(double value) {
        toCalibrateList.add(value);
    }

    private void stopCalibration() {
        filterCalibration(15);
        analyzeData();
        calibrated = true;
    }

    private void filterCalibration(int window) {
        double sum = 0.0;

        for (int i=0; i < window; i++) {
            sum = sum + toCalibrateList.get(i);
            double calValue = sum / (double) window;
            calibratedList.add(calValue);
        }

        for (int i=window; i < toCalibrateList.size(); i++) {
            sum = sum - toCalibrateList.get(i-window) + toCalibrateList.get(i);
            double calValue = sum / (double) window;
            calibratedList.add(calValue);
        }
    }

    private void analyzeData() {

    }

}

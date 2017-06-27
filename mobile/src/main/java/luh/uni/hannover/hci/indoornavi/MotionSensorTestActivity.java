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
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import luh.uni.hannover.hci.indoornavi.Utilities.FileChooser;

public class MotionSensorTestActivity extends AppCompatActivity implements SensorEventListener {

    FileChooser fileChooser;
    private SensorManager mSensorManager;
    private Sensor accSensor;
    private List<Double> accList = new ArrayList<>();
    private List<Double> avgAccList = new ArrayList<>();
    private boolean active = false;
    private int windowSize = 10;

    private double stepTime;
    private double stepThreshold;
    private double minStepTime = 3; //seconds
    private double minStepThreshold = 1; // for now
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
    private List<StepData> toCalibrateList = new ArrayList<>();
    private List<StepData> calibratedList = new ArrayList<>();
    private List<StepData> calibrateCandidateList = new ArrayList<>();
    private StepData lastPeak;
    private StepData lastValley;
    private String lastCandidate = "";
    private int stepCounter = 0;

    // turning
    private Sensor rotSensor;
    final float[] rotationMatrix = new float[9];
    final float[] orientation = new float[3];
    float[] adjustedRotationMatrix = new float[9];
    private WindowManager mWindowManager;

    private String TAG = "MotionTest";
    private StringBuilder sb = new StringBuilder();
    private StringBuilder sb2 = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_step_test);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        fileChooser = new FileChooser(this);
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
        rotSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        mWindowManager = getWindowManager();
        accSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    private void startSensors() {
        //mSensorManager.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, rotSensor, SensorManager.SENSOR_DELAY_UI);

    }

    private void stopSensors() {
        mSensorManager.unregisterListener(this);
    }

    private void stepDetected() {
        Log.d(TAG, "step");
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor == rotSensor) {
            updateOrientation(sensorEvent.values);
        }
        // small scale first time setup
        if (!setup) {
            lastValleyTime = sensorEvent.timestamp;
            lastPeakTime = sensorEvent.timestamp;
            stepTime = minStepTime;
            stepThreshold = minStepThreshold;
            setup = true;
        }
        if (sensorEvent.sensor == accSensor) {
            float x = sensorEvent.values[0];
            float y = sensorEvent.values[1];
            float z = sensorEvent.values[2];
            double value = Math.sqrt(x * x + y * y + z * z);
            if (calibrated) {
                addValue(accList, value);
                checkStep(sensorEvent.timestamp);
            } else {
                addValueCalibrate(value, sensorEvent.timestamp);
            }
        }
    }

    private void updateOrientation(float[] rotationVector) {
        mSensorManager.getRotationMatrixFromVector(rotationMatrix, rotationVector);

        final int worldAxisForDeviceAxisX;
        final int worldAxisForDeviceAxisY;

        // Remap the axes as if the device screen was the instrument panel,
        // and adjust the rotation matrix for the device orientation.
        switch (mWindowManager.getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_0:
            default:
                worldAxisForDeviceAxisX = SensorManager.AXIS_X;
                worldAxisForDeviceAxisY = SensorManager.AXIS_Z;
                break;
            case Surface.ROTATION_90:
                worldAxisForDeviceAxisX = SensorManager.AXIS_Z;
                worldAxisForDeviceAxisY = SensorManager.AXIS_MINUS_X;
                break;
            case Surface.ROTATION_180:
                worldAxisForDeviceAxisX = SensorManager.AXIS_MINUS_X;
                worldAxisForDeviceAxisY = SensorManager.AXIS_MINUS_Z;
                break;
            case Surface.ROTATION_270:
                worldAxisForDeviceAxisX = SensorManager.AXIS_MINUS_Z;
                worldAxisForDeviceAxisY = SensorManager.AXIS_X;
                break;
        }

        SensorManager.remapCoordinateSystem(rotationMatrix, worldAxisForDeviceAxisX,
                worldAxisForDeviceAxisY, adjustedRotationMatrix);

        mSensorManager.getOrientation(adjustedRotationMatrix, orientation);
        float x = orientation[0] * -57 + 180;
        float y = orientation[1] * -57 + 180;
        float z = orientation[2] * -57 + 180;
        Log.d(TAG, "RotationVector: " + x + ", " + y + ", " + z);
        sb.append(x);
        sb.append(System.lineSeparator());
    }

    private void addValue(List<Double> list, double value) {
        list.add(value);
        if (list.size() > windowSize) {
            list.remove(0);
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
        int counter2 = 1;
        while (fileNames.contains(name)) {
            counter++;
            name = "step" + counter + ".txt";
        }
        while (fileNames.contains(name2)) {
            counter2++;
            name2 = "window" + counter2 + ".txt";
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

       /* try {
            File myFile = new File(root, name2);
            FileOutputStream fos = new FileOutputStream(myFile);
            fos.write(sb2.toString().getBytes());
            fos.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
*/
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
                if (!calibrated)
                    startCalibration();
                return true;
            case R.id.stop_calibrate:
                if (!calibrated) {
                    stopCalibration();
                    return true;
                }
            case R.id.test_calibrate:
                loadTestData();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void startCalibration() {
        calibrated = false;
        sb.setLength(0);
        sb2.setLength(0);
        startSensors();
    }

    private void stopCalibration() {
        filterCalibration(15);
        analyzeData();
        Log.d(TAG, "Steps: " + stepCounter);
        for (int i=0; i < calibratedList.size(); i++) {
            sb2.append(calibratedList.get(i).value);
            sb2.append(System.lineSeparator());
        }
        saveLogFile();
        sb.setLength(0);
        sb2.setLength(0);
        toCalibrateList.clear();
        calibratedList.clear();
        calibrateCandidateList.clear();
        calibrated = false;
    }

    private void filterCalibration(int window) {
        double sum = 0.0;
        int count = 0;
        for (int i=0; i < window; i++) {
            sum = sum + toCalibrateList.get(i).value;
            double calValue = sum / (double) (i + 1);
            StepData data = new StepData(calValue, toCalibrateList.get(i).time);
            calibratedList.add(data);
            count++;
        }

        for (int i=window; i < toCalibrateList.size(); i++) {
            double rem = toCalibrateList.get(i - window).value;
            double add = toCalibrateList.get(i).value;
            sum = sum - rem + add;
            double calValue = sum / (double) window;
            StepData data = new StepData(calValue, toCalibrateList.get(i).time);
            calibratedList.add(data);
            count++;
        }
    }

    private void analyzeData() {
        for (int i=0; i < calibratedList.size(); i++) {
            calibrateSteps(calibratedList.get(i));
        }
    }

    private void calibrateSteps(StepData data) {
        calibrateCandidateList.add(data);
        if (calibrateCandidateList.size() < 3) {
            return;
        }
        if (calibrateCandidateList.size() > 3) {
            calibrateCandidateList.remove(0);
        }
        String candidate =  findCalibrateCandidate();
        //Log.d(TAG, "Cand: " + candidate);
        StepData current = calibrateCandidateList.get(1);

        if (candidate == "peak") {
            lastPeak = current;
            lastCandidate = "peak";
        }
        else if (candidate == "valley") {
            if (lastCandidate == "peak") {
                double diff = lastPeak.value - current.value;
                //long tDiff = lastValley.time - current.time;
                //  double tDiffSec = tDiff /1000000000.0;
                if (diff > minStepThreshold) {
                    Log.d(TAG, "Step: " + diff + " - " + lastPeak.value + ", " + current.value);
                    stepCounter++;
                }
            }
            lastValley = current;
            lastCandidate = "valley";
        }
    }

    private String findCalibrateCandidate() {
        double previous = calibrateCandidateList.get(0).value;
        double current = calibrateCandidateList.get(1).value;
        double next = calibrateCandidateList.get(2).value;
        String candidate = "inter";
        //Log.d(TAG, "List: " + previous + ", " + current + ", " + next);
        double max = Math.max(previous, next);
        double min = Math.min(previous, next);

        if (current > max) {
            candidate = "peak";
        } else if (current < min) {
            candidate = "valley";
        }

        return candidate;
    }

    private void addValueCalibrate(double value, long time) {
        StepData data = new StepData(value, time);
        toCalibrateList.add(data);
        sb.append(value);
        sb.append(System.lineSeparator());
    }

    private class StepData {
        long time;
        double value;

        public StepData(double val, long time) {
            this.time = time;
            this.value = val;
        }
    }

    public void loadTestData() {
        fileChooser.setFileListener(new FileChooser.FileSelectedListener() {
            @Override
            public void fileSelected(File file) {
                String filePath = file.getAbsolutePath();
                Log.d(TAG, filePath);
                try {
                    FileInputStream fis = new FileInputStream(new File(filePath));
                    if (fis != null) {
                        InputStreamReader isReader = new InputStreamReader(fis);
                        BufferedReader bufferedReader = new BufferedReader(isReader);
                        String receive = "";
                        try {
                            while ((receive = bufferedReader.readLine()) != null) {
                                parseData(receive);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                testTestData();
            }
        }).showDialog();
    }

    private void parseData(String receive) {
        double value = Double.parseDouble(receive);
        StepData data = new StepData(value, 0);
        toCalibrateList.add(data);
    }

    private void testTestData() {
        stopCalibration();
    }
}

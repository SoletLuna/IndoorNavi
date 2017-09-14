package luh.uni.hannover.hci.indoornavi;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.StringBuilderPrinter;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BarometerSensorActivity extends AppCompatActivity implements SensorEventListener {

    private Sensor pressureSensor;
    private SensorManager mSensorManager;
    private TextView tv;
    private ArrayList<Float> sensorList = new ArrayList<>();
    private int windowSize = 10;
    private String state = "start";
    private double oldLevel = 0;
    private int transitionCounter = 0;
    private int limit = 30;
    private double minChange = 0.15;
    private double changeLevel = 0;
    private double dLevel = 0;
    private double avgDLevel = 0;
    private String TAG = "BaroStuff";
    private StringBuilder sb = new StringBuilder();
    private Boolean logging = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barometer_sensor);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (logging) {
                    saveLogFile();
                    logging = false;
                    sb.setLength(0);
                } else {
                    logging = true;
                }
            }
        });

        EditText win = (EditText) findViewById(R.id.windowText);
        win.setText(String.valueOf(windowSize));
        EditText min = (EditText) findViewById(R.id.changeText);
        min.setText(String.valueOf(minChange));
        Button but = (Button) findViewById(R.id.baroStart);
        but.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText win2 = (EditText) findViewById(R.id.windowText);
                EditText min2 = (EditText) findViewById(R.id.changeText);
                windowSize = Integer.parseInt(win2.getText().toString());
                minChange = Double.parseDouble(min2.getText().toString());
                sensorList.clear();
                state = "start";
            }
        });
    }

    public void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this, pressureSensor);
    }

    public void onResume() {
        super.onResume();
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        pressureSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        mSensorManager.registerListener(this, pressureSensor, SensorManager.SENSOR_DELAY_UI);
    }

    public void saveLogFile() {
        File root = new File(Environment.getExternalStorageDirectory(), "IndoorNavigation");
        String name ="bar1.txt";
        File[] fileArr = root.listFiles();
        List<String> fileNames = new ArrayList<>();
        for (File f : fileArr) {
            fileNames.add(f.getName());
        }

        int counter = 1;
        while (fileNames.contains(name)) {
            counter++;
            name = "bar" + counter + ".txt";
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

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor == pressureSensor) {
            addToWindows(sensorEvent.values[0]);
            if (state == "start") {
                oldLevel = (double) sensorEvent.values[0];
                state = "same";
            }
        }
    }

    private void addToWindows(Float value) {
        sensorList.add(value);
        if (sensorList.size() > windowSize) {
            sensorList.remove(0);
        }
        double avg = avgWindow();
        tv = (TextView) findViewById(R.id.baroText);
        tv.setText(Double.toString(avg) + " - " + value + " - " + sensorList.size() + " - " + transitionCounter + " - " + logging);

        if (state == "same") {
            if (avg > oldLevel) {
                state = "down";
            } else {
                state = "up";
            }
            changeLevel = avg;
        } else if (state == "down") {
            if (!(avg > oldLevel)) {
                state = "same";
                if (transitionCounter >= limit && (avg > changeLevel + minChange)) {
                    String str = "Went Down a Level";
                    Toast.makeText(this, str,
                            Toast.LENGTH_SHORT).show();
                }
                transitionCounter = 0;
            } else {
                transitionCounter++;
            }
        } else if (state == "up") {
            if (!(avg < oldLevel)) {
                state = "same";
                if (transitionCounter >= limit && (avg < changeLevel - minChange)) {
                    String str = "Went Up a Level";
                    Toast.makeText(this, str,
                            Toast.LENGTH_SHORT).show();
                }
                transitionCounter = 0;
            } else {
                transitionCounter++;
            }
        }
        dLevel = Math.abs(avg - oldLevel);
        if (logging) {
            sb.append(avg + " - " + dLevel);
            sb.append(System.lineSeparator());
        }
        oldLevel = avg;
    }

    private double avgWindow() {
        double avg = 0;
        for (int i=0; i < sensorList.size(); i++) {
            avg += sensorList.get(i);
        }
        avg /= sensorList.size();

        return avg;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}

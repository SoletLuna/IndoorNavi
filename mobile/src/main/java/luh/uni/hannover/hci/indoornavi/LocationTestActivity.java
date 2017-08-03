package luh.uni.hannover.hci.indoornavi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import luh.uni.hannover.hci.indoornavi.DataModels.WifiFingerprint;
import luh.uni.hannover.hci.indoornavi.Localisation.DistanceLocalisation;
import luh.uni.hannover.hci.indoornavi.Localisation.Localisation;
import luh.uni.hannover.hci.indoornavi.Localisation.StochasticLocalisation;
import luh.uni.hannover.hci.indoornavi.Services.WifiService;
import luh.uni.hannover.hci.indoornavi.Utilities.FileChooser;
import luh.uni.hannover.hci.indoornavi.Utilities.WifiFingerprintFilter;

public class LocationTestActivity extends AppCompatActivity {

    private FileChooser fileChooser;
    private String TAG = "LocTest";
    private List<WifiFingerprint> navigationPath = new ArrayList<>();
    private WifiFingerprintFilter wFilter;
    private boolean started = false;
    private Localisation localisator;

    RadioGroup locGroup;
    RadioGroup sensorGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_test);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        fileChooser = new FileChooser(this);
        wFilter = new WifiFingerprintFilter();

        locGroup = (RadioGroup) findViewById(R.id.LocationGroup);
        sensorGroup = (RadioGroup) findViewById(R.id.SensorGroup);


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigationPath.clear();;
                int locFilter = locGroup.getCheckedRadioButtonId();
                int sensor = sensorGroup.getCheckedRadioButtonId();
                setUpFilter(locFilter, sensor);
                loadNavigationPath();
            }
        });
    }

    protected void onPause() {
        super.onPause();
        stopScan();
    }

    private void startScan() {
        Intent i = new Intent(getApplicationContext(), WifiService.class);
        startService(i);
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("Scan"));
        started = true;
    }

    private void stopScan() {
        Intent i = new Intent(getApplicationContext(), WifiService.class);
        stopService(i);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
    }



    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case "Scan":
                    if (started) {
                        Log.d(TAG, "Scan received");
                        getMeasurement(intent);
                    }
                    return;
                default: Log.d(TAG, intent.getAction());
            }
        }
    };

    private void getMeasurement(Intent intent) {
        Bundle data = intent.getExtras();

        ArrayList<String> bssidList = data.getStringArrayList("BSSID");
        ArrayList<Double> rssList = (ArrayList<Double>) intent.getSerializableExtra("RSS");
        WifiFingerprint fp = new WifiFingerprint("measure");

        for (int i = 0; i < bssidList.size(); i++) {
            fp.addRSS(bssidList.get(i), rssList.get(i));
        }
        Log.d(TAG, navigationPath.size() +"");
        //Log.d(TAG, navigationPathPDF.get(0).toString());
        Log.d(TAG, fp.toString());
        String bla = localisator.measure(fp);

        TextView tv = (TextView) findViewById(R.id.locationText);
        tv.setText("Current Location: " + bla);

       /* Toast.makeText(this, bla,
                Toast.LENGTH_LONG).show();*/
    }

    private void setUp() {
        startScan();
        //testFingerprints();
    }

    private void setUpFilter(int loc, int sensor) {
        switch (loc) {
            case R.id.DistanceRadioButton:
                localisator = new DistanceLocalisation(navigationPath);
                return;
            case R.id.StochasticRadioBUtton:
                localisator = new StochasticLocalisation(navigationPath);
                return;

        }

        switch (sensor) {
            case R.id.SensorOnRadioButton:
                return;
            case R.id.SensorOffRadioButton:
                return;
        }
    }

    private void testFingerprints() {
        List<WifiFingerprint> avgFp = wFilter.filterAverageRSS(navigationPath);
        for (int j=0; j < avgFp.size(); j++) {
            WifiFingerprint fp = avgFp.get(j);
            for (String key : fp.getWifiMap().keySet()) {
                List<Double> lst = new ArrayList<>();
                lst.add(fp.getWifiMap().get(key).get(0));
                for (int i = j + 1; i < avgFp.size(); i++) {
                    WifiFingerprint fp2 = avgFp.get(i);
                    if (fp2.getWifiMap().containsKey(key)) {
                        lst.add(fp2.getWifiMap().get(key).get(0));
                    }
                }
                StringBuilder sb = new StringBuilder();
                sb.append(key + ": ");
                for (int i = 0; i < lst.size(); i++) {
                    sb.append(lst.get(i) + ", ");
                }
                Log.d(TAG, sb.toString());
            }
        }
    }

    public void loadNavigationPath() {
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
                                parseFingerprints(receive);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                setUp();
            }
        }).showDialog();
    }

    public void parseFingerprints(String json) throws JSONException {
        Gson gson = new Gson();
        WifiFingerprint fp = gson.fromJson(json, WifiFingerprint.class);
        navigationPath.add(fp);
    }

}

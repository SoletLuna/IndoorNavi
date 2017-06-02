package luh.uni.hannover.hci.indoornavi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import luh.uni.hannover.hci.indoornavi.DataModels.WifiFingerprint;
import luh.uni.hannover.hci.indoornavi.Services.WifiService;
import luh.uni.hannover.hci.indoornavi.Utilities.FileChooser;
import luh.uni.hannover.hci.indoornavi.Utilities.ParticleFilter;
import luh.uni.hannover.hci.indoornavi.Utilities.WifiFingerprintFilter;

public class ParticleTestActivity extends AppCompatActivity {

    private FileChooser fileChooser;
    private String TAG = "ParticleDebug";
    private ArrayList<WifiFingerprint> navigationPath = new ArrayList<>();
    private ParticleFilter pf;
    private WifiFingerprintFilter wFilter;
    private boolean started = false;
    private int IAP = 5;
    private int steps = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_particle_test);
        double x = 10.0;
        int xy = 10;

        fileChooser = new FileChooser(ParticleTestActivity.this);
        wFilter = new WifiFingerprintFilter();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_particle);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadNavigationPath();
            }
        });

        Button step = (Button) findViewById(R.id.step);
        Button stepBack = (Button) findViewById(R.id.stepBack);
        step.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                steps++;
                TextView tv = (TextView) findViewById(R.id.stepText);
                tv.setText(Integer.toString(steps));
                pf.stepParticles();
            }
        });
        stepBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                steps--;
                TextView tv = (TextView) findViewById(R.id.stepText);
                tv.setText(Integer.toString(steps));
                pf.stepBackParticles();
            }
        });

        Button reset = (Button) findViewById(R.id.reset);
        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pf.reset();
            }
        });
    }

    protected void onPause() {
        super.onPause();
        Intent i = new Intent(getApplicationContext(), WifiService.class);
        stopService(i);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
    }

    protected void onResume() {
        super.onResume();
        //registerScanning();
    }

    private void registerScanning() {
        Intent i = new Intent(getApplicationContext(), WifiService.class);
        startService(i);
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("Scan"));
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

        for (int i=0; i < bssidList.size(); i++) {
            fp.addRSS(bssidList.get(i), rssList.get(i));
        }
        //Log.d(TAG, fp.toString());
        WifiFingerprint filteredFP = wFilter.filterBestInFingerprint(fp, IAP);
        //Log.d(TAG, filteredFP.toString());

        //pf.measureParticles(fp);
        pf.measure(fp);
        double pos = pf.estimatePosition();
        String str = pf.getBestParticle();
        Toast.makeText(this, Double.toString(pos) + " - " + str,
                Toast.LENGTH_LONG).show();
        pf.sample();
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
                initiateParticleFilter();
            }
        }).showDialog();
    }

    public void initiateParticleFilter() {
        List<WifiFingerprint> filtered = wFilter.filterAverageRSS(navigationPath);
        for (WifiFingerprint fp : filtered) {
            Log.d(TAG, fp.toString());
        }
        pf = new ParticleFilter(50, filtered);
        pf.start();
        started = true;
        registerScanning();
    }

    public void parseFingerprints(String json) throws JSONException {
        Gson gson = new Gson();
        WifiFingerprint fp = gson.fromJson(json, WifiFingerprint.class);
        navigationPath.add(fp);
    }
}

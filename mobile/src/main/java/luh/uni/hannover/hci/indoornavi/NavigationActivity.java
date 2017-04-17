package luh.uni.hannover.hci.indoornavi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import luh.uni.hannover.hci.indoornavi.Services.SensorService;
import luh.uni.hannover.hci.indoornavi.Services.WatchService;
import luh.uni.hannover.hci.indoornavi.Services.WifiService;
import luh.uni.hannover.hci.indoornavi.WifiUtilities.WifiCoordinator;
import luh.uni.hannover.hci.indoornavi.DataModels.WifiFingerprint;

public class NavigationActivity extends AppCompatActivity {

    private WifiCoordinator wifiCoord;
    private List<WifiFingerprint> navigationPath;
    private String TAG = "Navigation";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        wifiCoord = new WifiCoordinator();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                startNavigation();
            }
        });

        registerServices();
    }


    @Override
    protected void onPause() {
        super.onPause();
        Intent i = new Intent(getApplicationContext(), SensorService.class);
        Intent i2 = new Intent(getApplicationContext(), WifiService.class);
        Intent i3 = new Intent(getApplicationContext(), WatchService.class);
        stopService(i);
        stopService(i2);
        stopService(i3);

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerServices();
    }

    private void registerServices() {
        Intent i = new Intent(getApplicationContext(), SensorService.class);
        Intent i2 = new Intent(getApplicationContext(), WifiService.class);
        Intent i3 = new Intent(getApplicationContext(), WatchService.class);
        startService(i);
        startService(i2);
        startService(i3);

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("Step"));
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("Scan"));
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("Watch"));
    }

    /**
     * Gets called whenever a step is detected, checks if the step results in an image change or
     * something else
     */
    private void update() {

    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            update();
            switch (intent.getAction()) {
                case "Step":
                    Log.d(TAG, "Step received");
                    return;
                case "Scan":
                    Log.d(TAG, "Scan received");
                    wifiCoord.setUnknown(getUnknownFP(intent));
                    return;
                case "Watch":
                    Log.d(TAG, "Watch received");
                    return;
                default: Log.d(TAG, intent.getAction());
            }
        }
    };

    private WifiFingerprint getUnknownFP(Intent intent) {
        Bundle data = intent.getExtras();
        ArrayList<String> bssidList = data.getStringArrayList("BSSID");
        ArrayList<Integer> rssList = data.getIntegerArrayList("RSS");
        WifiFingerprint currentFP = new WifiFingerprint("current");

        for (int i=0; i < bssidList.size(); i++) {
            currentFP.addRSS(bssidList.get(i), rssList.get(i));
        }
        return currentFP;
    }

    private void startNavigation() {
        navigationPath = wifiCoord.selectNavigationPath();
    }



}

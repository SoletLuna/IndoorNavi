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
import luh.uni.hannover.hci.indoornavi.Services.NavigationService;
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
        setUpWifiCoordinator();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                startNavigation();
            }
        });

        //registerServices();
    }


    @Override
    protected void onPause() {
        super.onPause();
        Intent i = new Intent(getApplicationContext(), SensorService.class);
        Intent i2 = new Intent(getApplicationContext(), WifiService.class);
        Intent i3 = new Intent(getApplicationContext(), NavigationService.class);
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
        Intent i3 = new Intent(getApplicationContext(), NavigationService.class);
        startService(i);
        startService(i2);
        startService(i3);

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("Step"));
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("Scan"));
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("Watch"));
    }

    private void setUpWifiCoordinator() {
        wifiCoord = new WifiCoordinator();
    }

    /**
     * Gets called whenever a step is detected, checks if the step results in an image change or
     * something else
     */
    private void updateFromStep() {
        setNextImage();
    }

    /**
     * Orders service to update the next image to sync with wearable
     */
    private void setNextImage() {
        Intent i = new Intent("sendImage");
        LocalBroadcastManager.getInstance(this).sendBroadcast(i);
    }

    /**
     * Gets called whenever a scan has been received, used to correct the current displayed image to
     * appropriate fingerprint.
     */
    private void updateFromScan(Intent intent) {
        wifiCoord.setUnknown(getUnknownFP(intent));

        double dist = wifiCoord.getDistanceToNextFP(2); // replace this and the condition by whatever method we will use
        if (dist < 20) {
            int imageIndex = wifiCoord.reachedCheckpoint();
            Intent i = new Intent("sendImage");
            i.putExtra("stepCount", imageIndex);
            LocalBroadcastManager.getInstance(this).sendBroadcast(i);
        }
    }


    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case "Step":
                    Log.d(TAG, "Step received");
                    //updateFromStep();
                    return;
                case "Scan":
                    //Log.d(TAG, "Scan received");
                    //updateFromScan(intent);
                    updateFromStep(); //for debugging purposes only
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

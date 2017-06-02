package luh.uni.hannover.hci.indoornavi.Services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import luh.uni.hannover.hci.indoornavi.DataModels.WifiFingerprint;

/**
 * Created by solet on 11/04/2017.
 */

public class WifiService extends Service {

    private WifiManager mWifiManager;
    private ScanResultReceiver scanReceiver;
    private String TAG = "WifiService";

    @Override
    public void onCreate() {
        mWifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        scanReceiver = new ScanResultReceiver();
    }

    @Override
    public int onStartCommand(Intent i, int flags, int startId) {
        if (mWifiManager.isWifiEnabled() == false) {
            mWifiManager.setWifiEnabled(true);
        }
        registerReceiver(scanReceiver, new IntentFilter(mWifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        Log.d(TAG, "Registered");
        mWifiManager.startScan();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(scanReceiver);
        Log.d(TAG, "Unregistered");
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public class ScanResultReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            List<ScanResult> scans = mWifiManager.getScanResults();
            ArrayList<String> bssidList = new ArrayList<>();
            ArrayList<Double> rssList = new ArrayList<>();
            for(ScanResult scan : scans) {
                String bssid = scan.BSSID;
                double level = scan.level;
                bssidList.add(bssid);
                rssList.add(level);
            }
            Intent i = new Intent("Scan");
            Bundle data = new Bundle();
            data.putStringArrayList("BSSID", bssidList);
            i.putExtras(data);
            i.putExtra("RSS", rssList);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(i);
            mWifiManager.startScan();
        }
    }
}

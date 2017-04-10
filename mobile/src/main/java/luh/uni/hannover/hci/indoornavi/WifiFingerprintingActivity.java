package luh.uni.hannover.hci.indoornavi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import luh.uni.hannover.hci.indoornavi.Utilities.ExpandableListAdapter;
import luh.uni.hannover.hci.indoornavi.WifiUtilities.*;

/**
 * Activity for registering fingerprints. Multiple ways of filtering/Saving scanned fingerprints are
 * proposed in here (number of scans, footsteps/location, filter methods: low RSS, etc. Shows scanned fingerprints
 * in an expandable list view with more information
 */

public class WifiFingerprintingActivity extends AppCompatActivity {

    private ExpandableListView mExpListView;
    private ExpandableListAdapter mExpListAdapter;
    private WifiCoordinator wifiCoord;
    private List<String> listDataHeader;
    private HashMap<String, List<String>> listDataChild;
    private int tmp = 0;

    private WifiManager mWifiManager;
    private ScanResultReceiver scanReceiver = new ScanResultReceiver();
    private List<WifiFingerprint> currentScanList = new ArrayList<>();
    private int numberOfScansCompleted = 1;
    private int numberOfScansStarted = 1;
    private int scansToDo;
    private Handler mHandler;
    private String currentLocation;
    private final long SCAN_DELAY = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fingerprinting);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        wifiCoord = new WifiCoordinator();
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(WifiFingerprintingActivity.this);
                builder.setTitle("Enter Location Name");
                final EditText input = new EditText(WifiFingerprintingActivity.this);
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                builder.setView(input);
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        currentLocation = input.getText().toString();
                        startScan();
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });
                builder.show();
                /*mHandler = new Handler();
                mHandler.post(startScanning);*/
            }
        });

        mExpListView = (ExpandableListView) findViewById(R.id.expListView);
        fillListView();
        mExpListAdapter = new ExpandableListAdapter(this, listDataHeader, listDataChild);
        mExpListView.setAdapter(mExpListAdapter);

        mExpListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView expandableListView, View view, int i, int i1, long l) {
                final int group = i;
                String nameFP = listDataHeader.get(group);
                AlertDialog.Builder builder = new AlertDialog.Builder(WifiFingerprintingActivity.this);
                builder.setMessage("Delete Fingerprint " + nameFP  + "?");
                builder.setCancelable(false);
                builder.setPositiveButton("Yes",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                removeFingerprintElement(group);
                            }
                        });
                builder.setNegativeButton("No",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
                return true;
            }
        });

        setUpWifi();

    }

    private void setUpWifi() {
        mWifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        if (mWifiManager.isWifiEnabled() == false) {
            mWifiManager.setWifiEnabled(true);
        }
        registerReceiver(scanReceiver, new IntentFilter(mWifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    private void startScan() {
        AlertDialog.Builder builder = new AlertDialog.Builder(WifiFingerprintingActivity.this);
        builder.setTitle("Enter Number of Scans");
        final EditText input = new EditText(WifiFingerprintingActivity.this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                scansToDo = Integer.parseInt(input.getText().toString());
                mWifiManager.startScan();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });
        builder.show();
    }

    // Scans x times specified by user input
    private class ScanResultReceiver extends BroadcastReceiver {
        public void onReceive(Context c, Intent i) {
            Log.d("WIFI", "RECEIVED");
            WifiFingerprint createdFP = new WifiFingerprint(currentLocation);
            List<ScanResult> scans = mWifiManager.getScanResults();
            if (scans.isEmpty()) {
                return;
            } else {
                for (ScanResult scan : scans) {
                    String bssid = scan.BSSID;
                    int level = scan.level;
                    createdFP.addRSS(bssid, level);
                }
                currentScanList.add(createdFP); // accumulates scans for one fp
                Log.d("WIFI", numberOfScansCompleted + "" +  "number");
                if (numberOfScansCompleted == scansToDo) {
                    wifiCoord.addScansToFingerprint(currentScanList, currentLocation);
                    currentScanList.clear();
                    numberOfScansCompleted = 1;
                    WifiFingerprint fp = wifiCoord.getLastAddedFingerprint();
                    addFingerprintElement(fp);
                    Log.d("FP", fp.toString());
                    return;
                }
                numberOfScansCompleted++;
                mWifiManager.startScan();
            }
        }
    }

/*    private Runnable startScanning = new Runnable() {
        @Override
        public void run() {
            mWifiManager.startScan();
            Log.d("WIFI", "started scan");
            if (numberOfScansStarted == scansToDo) {
                mHandler.removeCallbacks(startScanning);
            } else {
                mHandler.postDelayed(startScanning, SCAN_DELAY);
                numberOfScansStarted++;
            }
        }
    };*/

    private void fillListView() {
        listDataHeader = new ArrayList<>();
        listDataChild = new HashMap<>();
    }

    private void addFingerprintElement(WifiFingerprint fp) {
        listDataHeader.add(fp.getLocation());
        List<String> dataList = new ArrayList<>();
        String data = "AP: " + fp.getNumberOfAPs() + "Steps: " + fp.getStepCount();
        dataList.add(data);
        listDataChild.put(fp.getLocation(), dataList);

        mExpListAdapter.notifyDataSetChanged();
    }

    private void addFingerprintElementMock() {
        WifiFingerprint fp = new WifiFingerprint("Start");
        String mockAP = "H1X" + tmp;
        int mockLevel = -10;
        fp.addRSS(mockAP, mockLevel);
        listDataHeader.add(mockAP);

        Random rnd = new Random();
        int mockCount = rnd.nextInt(20);
        fp.setStepCount(mockCount);
        String mockData = "AP: " + fp.getNumberOfAPs() + " Steps: " + fp.getStepCount();
        List<String> mockList = new ArrayList<>();
        mockList.add(mockData);
        listDataChild.put(listDataHeader.get(tmp), mockList);
        tmp = listDataHeader.size();
        mExpListAdapter.notifyDataSetChanged();
    }

    private void removeFingerprintElement(int group) {
        listDataHeader.remove(group);
        tmp = listDataHeader.size();
        mExpListAdapter.notifyDataSetChanged();
    }


}

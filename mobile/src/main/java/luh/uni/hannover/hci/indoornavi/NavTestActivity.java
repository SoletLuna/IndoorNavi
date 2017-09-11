package luh.uni.hannover.hci.indoornavi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.RadioGroup;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.gson.Gson;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import luh.uni.hannover.hci.indoornavi.DataModels.WifiFingerprint;
import luh.uni.hannover.hci.indoornavi.Localisation.DistanceLocalisation;
import luh.uni.hannover.hci.indoornavi.Localisation.ParticleFilter;
import luh.uni.hannover.hci.indoornavi.Localisation.ParticleFilterStatic;
import luh.uni.hannover.hci.indoornavi.Services.MotionService;
import luh.uni.hannover.hci.indoornavi.Services.WifiService;
import luh.uni.hannover.hci.indoornavi.Utilities.FileChooser;
import luh.uni.hannover.hci.indoornavi.Utilities.WifiFingerprintFilter;

public class NavTestActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    GoogleApiClient mGoogleApiClient;
    String TAG = "NaviTest";
    int stepCount = 1;
    double estimateLocation = 0;
    //String navMode = "Step"; // "Particle", "Step", "Checkpoint"
    private FileChooser fileChooser;
    private List<WifiFingerprint> navRoute = new ArrayList<>();
    private List<WifiFingerprint> parsedRoute = new ArrayList<>();
    private ParticleFilter partFilter;
    private ParticleFilterStatic altPartFilter;
    private DistanceLocalisation checkpoint;
    private WifiFingerprintFilter wFilter;
    private int totalCount = 0;
    private String imgPath = "/storage/emulated/0/IndoorNavigation/imageHCI/";
    Handler mHandler;
    RadioGroup methodGroup;
    int navMode = 0;
    int checkCount = 0;
    int failedScans = 0;

    String finalDestination;
    String nextTarget;
    List<Integer> checkPointCountList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nav_test);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        fileChooser = new FileChooser(this);
        wFilter = new WifiFingerprintFilter();
        methodGroup = (RadioGroup) findViewById(R.id.MethodGroup);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                parsedRoute.clear();
                navRoute.clear();
                stepCount = 1;
                loadNavigationPath();
            }
        });
    }

    protected void onPause() {
        super.onPause();
        stopServices();
    }

    private void connectServices() {
        Intent i = new Intent(getApplicationContext(), MotionService.class);
        Intent i2 = new Intent(getApplicationContext(), WifiService.class);
        //Intent i3 = new Intent(getApplicationContext(), DataLayerPhoneService.class);

        startService(i);
        startService(i2);

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("Step"));
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("Scan"));
        //LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("Watch"));
    }

    private void stopServices() {
        Intent i = new Intent(getApplicationContext(), MotionService.class);
        Intent i2 = new Intent(getApplicationContext(), WifiService.class);
        //Intent i3 = new Intent(getApplicationContext(), DataLayerPhoneService.class);

        stopService(i);
        stopService(i2);
    }

    private void connectAPI() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();
    }

    private void startNavigation() {
        navMode = methodGroup.getCheckedRadioButtonId();
        connectAPI();
        connectServices();
        navRoute = wFilter.filterAverageRSS(parsedRoute);
        totalCount = 0;
        for (WifiFingerprint fp : navRoute) {
            totalCount += fp.getStepCount();
        }
        switch (navMode) {
            case R.id.ParticleFilter:
                partFilter = new ParticleFilter(totalCount + 1, navRoute);
                break;
            case R.id.AltParticleFilter:
                altPartFilter = new ParticleFilterStatic(totalCount + 1, navRoute);
                break;
            case R.id.Step:
                Log.d(TAG, "Stepster");
                break;
            case R.id.Checkpoint:
                nextTarget = navRoute.get(1).getLocation();
                finalDestination = navRoute.get(navRoute.size()-1).getLocation();
                checkpoint = new DistanceLocalisation(navRoute);
                int tmp = 0;
                for (int i=0; i < navRoute.size(); i++) {
                    tmp += navRoute.get(i).getStepCount();
                    checkPointCountList.add(tmp);
                }
                break;

        }
        updateImage(stepCount);
        mHandler = new Handler();
        mHandler.postDelayed(periodicSend, 1000);
    }

    private void finishNavigation() {
        celebration();
        stopServices();
        mHandler.removeCallbacks(periodicSend);
    }

    private void celebration() {
        // do something to show we are done
        Log.d(TAG, "HELLO");
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case "Step":
                    updateStep();
                    Log.d(TAG, "Step received");
                    return;
                case "Scan":
                    updateScan(intent);
                    Log.d(TAG, "Scan received");
                    return;
/*                case "Watch":
                    Log.d(TAG, "Watch received");
                    return;*/
            }
        }
    };

    private void updateStep() {
        switch (navMode) {
            case R.id.ParticleFilter:
                stepCount++;
                partFilter.stepParticles();
                return;
            case R.id.AltParticleFilter:
                stepCount++;
                return;
            case R.id.Step:
                if (stepCount < (totalCount * 0.95)) {
                    stepCount++;
                }
                else {
                    finishNavigation();
                }
                return;
            case R.id.Checkpoint:
                stepCount++;
                checkCount++;
                //if the recent scans have not located the next fingerprint, then either you are lost or further ahead, locate nexttarget equal to curent stepcount;
                for (int i=0; i < checkPointCountList.size(); i++) {
                    int compare = checkPointCountList.get(i);
                    if (checkCount == compare && failedScans > 5) {
                        nextTarget = navRoute.get(i+1).getLocation();
                    }
                }
                return;
        }
    }

    private WifiFingerprint getUnknownFP(Intent intent) {
        Bundle data = intent.getExtras();
        ArrayList<String> bssidList = data.getStringArrayList("BSSID");
        ArrayList<Double> rssList = (ArrayList<Double>) intent.getSerializableExtra("RSS");
        WifiFingerprint currentFP = new WifiFingerprint("current");

        for (int i=0; i < bssidList.size(); i++) {
            currentFP.addRSS(bssidList.get(i), rssList.get(i));
        }
        return currentFP;
    }

    private void updateScan(Intent intent) {
        WifiFingerprint fp = getUnknownFP(intent);
        switch (navMode) {
            case R.id.ParticleFilter:
                partFilter.measure(fp);
                estimateLocation = partFilter.estimatePosition();
                if (estimateLocation > (totalCount * .95)) {
                    finishNavigation();
                }
                return;
            case R.id.AltParticleFilter:
                altPartFilter.measure(fp);
                estimateLocation = altPartFilter.estimatePosition();
                if (estimateLocation > (totalCount * .95)) {
                    finishNavigation();
                }
                return;
            case R.id.Step:
                return;
            case R.id.Checkpoint:
                if (nextTarget == checkpoint.measure(fp)) {
                    for (int i=0; i < navRoute.size(); i++) {
                        if (nextTarget == navRoute.get(i).getLocation()) {
                            checkCount = checkPointCountList.get(i);
                            break;
                        }
                    }
                }
                // count failed scans
                else {
                    failedScans++;
                }
                return;
        }
    }

    private Runnable periodicSend = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "Sending");
            int estimate = (int) Math.ceil(estimateLocation);
            switch (navMode) {
                case R.id.ParticleFilter:
                    updateImage(estimate);
                    break;
                case R.id.AltParticleFilter:
                    updateImage(estimate);
                    break;
                case R.id.Step:
                    updateImage(stepCount);
                    break;
                case R.id.Checkpoint:
                    updateImage(checkCount);
                    break;
        }
            mHandler.postDelayed(periodicSend, 1000);
        }
    };

    private static Asset createAssetFromBitmap(Bitmap bitmap) {
        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteStream);
        return Asset.createFromBytes(byteStream.toByteArray());
    }

    private void updateImage(int imageCount) {
        String formatted = String.format("%03d", imageCount);
        String filePath = imgPath + "hci" + formatted + ".jpg";
        Log.d(TAG, filePath);
        Bitmap myBitmap = BitmapFactory.decodeFile(filePath);
        Asset img = createAssetFromBitmap(myBitmap);

        PutDataMapRequest dataMap = PutDataMapRequest.create("/img");
        dataMap.getDataMap().putAsset("navImage", img);
        //dataMap.getDataMap().putLong("timestamp", System.currentTimeMillis());
        dataMap.setUrgent();
        PutDataRequest request = dataMap.asPutDataRequest();
        PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi
                .putDataItem(mGoogleApiClient, request);

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
                startNavigation();
            }
        }).showDialog();
    }

    public void parseFingerprints(String json) throws JSONException {
        Gson gson = new Gson();
        WifiFingerprint fp = gson.fromJson(json, WifiFingerprint.class);
        parsedRoute.add(fp);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "Connected");
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}

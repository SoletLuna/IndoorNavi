package luh.uni.hannover.hci.indoornavi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import luh.uni.hannover.hci.indoornavi.Services.DataLayerPhoneService;
import luh.uni.hannover.hci.indoornavi.Services.SensorService;
import luh.uni.hannover.hci.indoornavi.Services.WifiService;
import luh.uni.hannover.hci.indoornavi.Utilities.WifiCoordinator;
import luh.uni.hannover.hci.indoornavi.DataModels.WifiFingerprint;

public class NavigationActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private WifiCoordinator wifiCoord;
    private List<WifiFingerprint> navigationPath;
    private String TAG = "Navigation";
    private boolean isNavRunning = false;

    private String imagePath = "/storage/emulated/0/WiFiApp/ImagePaths/";
    private int imageIndex = 1;
    GoogleApiClient mGoogleApiClient;

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

        setUpGoogleApi();
    }


    @Override
    protected void onPause() {
        super.onPause();
        Intent i = new Intent(getApplicationContext(), SensorService.class);
        Intent i2 = new Intent(getApplicationContext(), WifiService.class);
        Intent i3 = new Intent(getApplicationContext(), DataLayerPhoneService.class);
        stopService(i);
        stopService(i2);
        stopService(i3);

        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        Log.d(TAG,"GoogleApi unregistered");

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerServices();
        if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }
        Log.d(TAG,"GoogleApi Registered");
    }

    private void setUpGoogleApi() {
        if (null == mGoogleApiClient) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
            Log.d(TAG, "GoogleApiClient created");
        }

        if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }
    }

    private void registerServices() {
        Intent i = new Intent(getApplicationContext(), SensorService.class);
        Intent i2 = new Intent(getApplicationContext(), WifiService.class);
        Intent i3 = new Intent(getApplicationContext(), DataLayerPhoneService.class);
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
    private void setNextImage() {/*
        Intent i = new Intent("sendImage");
        LocalBroadcastManager.getInstance(this).sendBroadcast(i);*/
        sendImage();
    }

    /**
     * Gets called whenever a scan has been received, used to correct the current displayed image to
     * appropriate fingerprint.
     */
    private void updateFromScan(Intent intent) {
        wifiCoord.setUnknown(getUnknownFP(intent));
        double dist = wifiCoord.getDistanceToNextFP(2); // replace this and the condition by whatever method we will use
        if (dist < 20) {
            imageIndex = wifiCoord.reachedCheckpoint();/*
            Intent i = new Intent("sendImage");
            i.putExtra("stepCount", imageIndex);
            LocalBroadcastManager.getInstance(this).sendBroadcast(i);*/
        }
    }


    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case "Step":
                    Log.d(TAG, "Step received");
                    updateFromStep();
                    return;
                case "Scan":
                        Log.d(TAG, "Scan received");
                    //updateFromScan(intent);
                    //updateFromStep(); //for debugging purposes only
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
        ArrayList<Double> rssList = (ArrayList<Double>) intent.getSerializableExtra("RSS");
        WifiFingerprint currentFP = new WifiFingerprint("current");

        for (int i=0; i < bssidList.size(); i++) {
            currentFP.addRSS(bssidList.get(i), rssList.get(i));
        }
        return currentFP;
    }

    private void sendImage() {
        String filePath = imagePath + "img" + imageIndex + ".jpg";
        Bitmap myBitmap = BitmapFactory.decodeFile(filePath);
        Asset img = createAssetFromBitmap(myBitmap);

        PutDataMapRequest dataMap = PutDataMapRequest.create("/img");
        dataMap.getDataMap().putAsset("navImage", img);
        dataMap.setUrgent();
        PutDataRequest request = dataMap.asPutDataRequest();
        PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi
                .putDataItem(mGoogleApiClient, request);

        imageIndex = (imageIndex + 1) % 30 + 1;
    }

    private static Asset createAssetFromBitmap(Bitmap bitmap) {
        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteStream);
        return Asset.createFromBytes(byteStream.toByteArray());
    }

    private void startNavigation() {
        //registerServices();
        //isNavRunning = true;
        navigationPath = wifiCoord.selectNavigationPath();
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "GoogleApiClient connected");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "GoogleApiClient connection suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "GoogleApiClient connection failed");
    }
}

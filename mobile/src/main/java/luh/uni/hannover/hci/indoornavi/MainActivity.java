package luh.uni.hannover.hci.indoornavi;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import luh.uni.hannover.hci.indoornavi.Services.ActivityRecognitionService;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private String imgPath = "/storage/emulated/0/WiFiApp/ImagePaths/";
    private int imgCount = 1;

    private ImageView mImageView;

    private String TAGAPI = "Google Api";
    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addApi(ActivityRecognition.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();

        GraphView graph = (GraphView) findViewById(R.id.graph);
        LineGraphSeries<DataPoint> series = new LineGraphSeries<>(new DataPoint[] {
                new DataPoint(0, 1),
                new DataPoint(1, 5),
                new DataPoint(2, 3),
                new DataPoint(3, 2),
                new DataPoint(4, 6)
        });
        graph.addSeries(series);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendImage();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        mGoogleApiClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();

        mGoogleApiClient.disconnect();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        switch (id) {
            case R.id.action_settings:
                return true;
            case R.id.open_sensors:
                startActivity(new Intent(this, SensorActivity.class));
                return true;
            case R.id.open_fingerprinting:
                startActivity(new Intent(this, WifiFingerprintingActivity.class));
                return true;
            case R.id.open_navigation:
                startActivity(new Intent(this, NavigationActivity.class));
                return true;
            case R.id.open_particle:
                startActivity(new Intent(this, ParticleTestActivity.class));
                return true;
            case R.id.open_baro:
                startActivity(new Intent(this, BarometerSensorActivity.class));
                return true;
            case R.id.open_motion:
                startActivity(new Intent(this, MotionSensorTestActivity.class));
            case R.id.open_loc:
                startActivity(new Intent(this, LocationTestActivity.class));
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
      //  Intent intent =  new Intent(this, ActivityRecognitionService.class);
      //  PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
      //  ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mGoogleApiClient, 2000, pendingIntent);
        Log.e(TAGAPI, "Connected");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e(TAGAPI, "Suspended: " + i);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(TAGAPI, "Failed: " + connectionResult);
    }

    private static Asset createAssetFromBitmap(Bitmap bitmap) {
        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteStream);
        return Asset.createFromBytes(byteStream.toByteArray());
    }

    private void sendImage() {
        String filePath = imgPath + "img" + imgCount + ".jpg";
        Bitmap myBitmap = BitmapFactory.decodeFile(filePath);
        Asset img = createAssetFromBitmap(myBitmap);

        PutDataMapRequest dataMap = PutDataMapRequest.create("/img");
        dataMap.getDataMap().putAsset("navImage", img);
        dataMap.getDataMap().putLong("timestamp", System.currentTimeMillis());
        dataMap.setUrgent();
        PutDataRequest request = dataMap.asPutDataRequest();
        PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi
                .putDataItem(mGoogleApiClient, request);

        imgCount = (imgCount % 160) +1;

    }
}

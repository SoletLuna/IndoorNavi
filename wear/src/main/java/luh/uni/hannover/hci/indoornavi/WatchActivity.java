package luh.uni.hannover.hci.indoornavi;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.images.ImageManager;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class WatchActivity extends Activity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, DataApi.DataListener {

    private TextView mTextView;
    private ImageView mImageView;

    private String TAGAPI = "Google Api";
    private GoogleApiClient mGoogleApiClient;
    private String nodeId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watch);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.text);
                mImageView = (ImageView) stub.findViewById(R.id.imgView);
            }
        });

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();
        retrieveDeviceNode();
    }

    private void retrieveDeviceNode() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                mGoogleApiClient.blockingConnect(500, TimeUnit.MILLISECONDS);
                NodeApi.GetConnectedNodesResult result =
                        Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
                List<Node> nodes = result.getNodes();
                if (nodes.size() > 0) {
                    nodeId = nodes.get(0).getId();
                }
                mGoogleApiClient.disconnect();
            }
        }).start();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.e(TAGAPI, "Connected");
        Wearable.DataApi.addListener(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e(TAGAPI, "Suspended: " + i);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(TAGAPI, "Failed: " + connectionResult);
    }

    public Bitmap loadBitmapFromAsset(Asset asset) {

        if (asset == null) {
            throw new IllegalArgumentException("Asset must be non-null");
        }
        ConnectionResult result =
                mGoogleApiClient.blockingConnect(500, TimeUnit.MILLISECONDS);
        if (!result.isSuccess()) {
            return null;
        }
        // convert asset into a file descriptor and block until it's ready
        InputStream assetInputStream = Wearable.DataApi.getFdForAsset(
                mGoogleApiClient, asset).await().getInputStream();
        mGoogleApiClient.disconnect();

        if (assetInputStream == null) {
            Log.w(TAGAPI, "Requested an unknown Asset.");
            return null;
        }
        // decode the stream into a bitmap
        return BitmapFactory.decodeStream(assetInputStream);
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {

        Log.d(TAGAPI, "DataChanged");
        for (DataEvent event : dataEvents) {
            Log.d(TAGAPI, event.getDataItem().getUri().getPath() + "");
            if (event.getType() == DataEvent.TYPE_CHANGED &&
                    event.getDataItem().getUri().getPath().equals("/img")) {
                DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                Asset profileAsset = dataMapItem.getDataMap().getAsset("navImage");

                Bitmap bitmap = loadBitmapFromAsset(profileAsset);
                // Do something with the bitmap

                mImageView.setImageBitmap(bitmap);
                Log.d(TAGAPI, "Image");
            }
        }
    }


}

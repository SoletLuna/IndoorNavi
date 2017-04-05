package luh.uni.hannover.hci.indoornavi;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class SensorCompanionActivity extends Activity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, MessageApi.MessageListener {

    private TextView mTextView;
    private ToggleButton mToggleButton;

    private String TAGAPI = "Google Api";
    private GoogleApiClient mGoogleApiClient;
    private String nodeId = "";
    private final String START_MESSAGE = "/start";
    private final String STOP_MESSAGE ="/stop";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_companion);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.text);
                mToggleButton = (ToggleButton) findViewById(R.id.toggleButton);
                mToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        if (b) {
                            retrieveDeviceNode();
                            Log.d("IDSend", nodeId);
                            sendStartMessage();
                        }
                        else {
                            Log.d("Toggle", "Off");
                            sendStopMessage();
                        }
                    }
                });
            }
        });

        initializeApi();
    }

    private void initializeApi() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();
    }

    private void sendStartMessage() {
        if (nodeId != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    mGoogleApiClient.blockingConnect(500, TimeUnit.MILLISECONDS);
                    Wearable.MessageApi.sendMessage(mGoogleApiClient, nodeId, START_MESSAGE, null);
                    mGoogleApiClient.disconnect();
                }
            }).start();
        }
    }

    private void sendStopMessage() {
        if (nodeId != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    mGoogleApiClient.blockingConnect(500, TimeUnit.MILLISECONDS);
                    Wearable.MessageApi.sendMessage(mGoogleApiClient, nodeId, STOP_MESSAGE, null);
                    mGoogleApiClient.disconnect();
                }
            }).start();
        }
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

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {

    }
}
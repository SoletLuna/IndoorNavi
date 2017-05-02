package luh.uni.hannover.hci.indoornavi;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.Image;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import luh.uni.hannover.hci.indoornavi.Services.DataLayerWatchService;

public class WatchStartActivity extends Activity {

    private TextView mTextView;

    private final String TAG = "WatchStart";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watch_start);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.text);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerServices();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterServices();
    }

    private void registerServices() {
        Intent i = new Intent(getApplicationContext(), DataLayerWatchService.class);
        startService(i);
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mMessageReceiver, new IntentFilter("Image"));
        Log.d(TAG, "registered");
    }

    private void unregisterServices() {
        Intent i = new Intent(getApplicationContext(), DataLayerWatchService.class);
        stopService(i);
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mMessageReceiver);
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, intent.getAction());
            switch (intent.getAction()) {
                case "Image":
                    ImageView imgView = (ImageView) findViewById(R.id.image_View);
                    Bitmap image = intent.getParcelableExtra("img");
                    imgView.setImageBitmap(image);
                    return;
                default: Log.d(TAG, intent.getAction());
            }
        }
    };
}

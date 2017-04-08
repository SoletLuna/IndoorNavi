package luh.uni.hannover.hci.indoornavi;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ExpandableListView;

/**
 * Activity for registering fingerprints. Multiple ways of filtering/Saving scanned fingerprints are
 * proposed in here (number of scans, footsteps/location, filter methods: low RSS, etc. Shows scanned fingerprints
 * in an expandable list view with more information
 */

public class WifiFingerprintingActivity extends AppCompatActivity {

    private ExpandableListView mExpListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fingerprinting);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        mExpListView = (ExpandableListView) findViewById(R.id.expListView);
    }

}

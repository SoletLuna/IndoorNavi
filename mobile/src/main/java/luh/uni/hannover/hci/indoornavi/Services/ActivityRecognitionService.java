package luh.uni.hannover.hci.indoornavi.Services;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.util.List;

/**
 * Created by solet on 28/06/2017.
 */

public class ActivityRecognitionService extends IntentService {

    public ActivityRecognitionService() {
        super("ActivityRecognizedService");
    }

    public ActivityRecognitionService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (ActivityRecognitionResult.hasResult(intent)) {
            ActivityRecognitionResult result =  ActivityRecognitionResult.extractResult(intent);
            handleDetectedActivities(result.getMostProbableActivity());
        }
    }

    private void handleDetectedActivities(DetectedActivity probableActivity) {
            switch (probableActivity.getType()) {
                case DetectedActivity.ON_FOOT: {
                    Log.e("ActivityRecogition", "On Foot: " + probableActivity.getConfidence());
                    break;
                }
                case DetectedActivity.STILL: {
                    Log.e("ActivityRecogition", "Still: " + probableActivity.getConfidence());
                    break;
                }
            }
        }
}

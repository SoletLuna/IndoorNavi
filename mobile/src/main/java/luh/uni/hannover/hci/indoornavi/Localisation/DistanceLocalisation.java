package luh.uni.hannover.hci.indoornavi.Localisation;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import luh.uni.hannover.hci.indoornavi.DataModels.WifiFingerprint;

/**
 * Created by solet on 14/07/2017.
 */

public class DistanceLocalisation {

    List<WifiFingerprint> navPath;
    int mode = 1; // 1 or 2 for manhattan or euclidean distance
    String TAG = "DistLoc";

    public DistanceLocalisation(List<WifiFingerprint> navPath) {
        this.navPath = navPath;
    }

    public void measure(WifiFingerprint fp) {
        List<Double> distances = new ArrayList<>();
        for (int i=0; i < navPath.size(); i++) {
            WifiFingerprint current = navPath.get(i);
            distances.add(getDistance(current, fp));
        }
        WifiFingerprint bestFp = findBestDistance(distances);
    }

    private WifiFingerprint findBestDistance(List<Double> distances) {
        double distance = 10000;
        int index = 0;
        for (int i=0; i < distances.size(); i++) {
            if (distance > distances.get(i)) {
                distance = distances.get(i);
                index = i;
            }
        }
        Log.d(TAG, "FP: " + navPath.get(index).getLocation() + ", distance: " + distances.get(index));
        return navPath.get(index);
    }

    private double getDistance(WifiFingerprint fp1, WifiFingerprint fp2) {
        double dist = 0.0;
        for (String key : fp1.getWifiMap().keySet()) {
            double level1 = fp1.getWifiMap().get(key).get(0);
            if (fp2.getWifiMap().containsKey(key)) {
                double level2 = fp2.getWifiMap().get(key).get(0);
                dist += Math.pow(level1 - level2, mode);
            } else {
                // optional error, add -100 if it helps
            }
        }
        Math.pow(dist, 1/mode);
        return dist;
    }
}

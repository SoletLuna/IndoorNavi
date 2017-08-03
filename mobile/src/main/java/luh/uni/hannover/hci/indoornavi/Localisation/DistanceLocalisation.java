package luh.uni.hannover.hci.indoornavi.Localisation;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import luh.uni.hannover.hci.indoornavi.DataModels.WifiFingerprint;
import luh.uni.hannover.hci.indoornavi.DataModels.WifiFingerprintPDF;

/**
 * Created by solet on 14/07/2017.
 */

public class DistanceLocalisation extends Localisation {

    List<WifiFingerprint> navPath;
    int mode = 2; // 1 or 2 for manhattan or euclidean distance
    String TAG = "DistLoc";
    String bla;

    public DistanceLocalisation(List<WifiFingerprint> navPath) {
        this.navPath = navPath;
    }

    public String measure(WifiFingerprint fp) {
        List<Double> distances = new ArrayList<>();
        List<Double> similarities = new ArrayList<>();
        List<Double> completeness = new ArrayList<>();
        for (int i=0; i < navPath.size(); i++) {
            WifiFingerprint current = navPath.get(i);
            distances.add(getDistance(current, fp));
            similarities.add(getSimilarity(fp, current));
            completeness.add(getCompleteness(fp, current));
        }
        WifiFingerprint bestFp = findBestDistance(distances, similarities);
        double sim = getSimilarity(fp, bestFp);
        double compl = getCompleteness(fp, bestFp);
        //return bla + ", " + sim + ", " + compl;
        return bestFp.getLocation();
    }

    private WifiFingerprint findBestDistance(List<Double> distances, List<Double> similarities) {
        double distance = 10000;
        int index = 0;
        for (int i=0; i < distances.size(); i++) {
            if (distance > distances.get(i) && similarities.get(i) >= 0.5) {
                distance = distances.get(i);
                index = i;
            }
        }
        Log.d(TAG, "FP: " + navPath.get(index).getLocation() + ", distance: " + distances.get(index));
        bla = "FP: " + navPath.get(index).getLocation() + ", distance: " + distances.get(index);
        return navPath.get(index);
    }

    private double getDistance(WifiFingerprint fp1, WifiFingerprint fp2) {
        double dist = 0.0;
        boolean atLeastOne = false;
        for (String key : fp1.getWifiMap().keySet()) {
            double level1 = fp1.getWifiMap().get(key).get(0);
            if (fp2.getWifiMap().containsKey(key)) {
                atLeastOne = true;
                double level2 = fp2.getWifiMap().get(key).get(0);
                dist += Math.pow(Math.abs(level1 - level2), mode);
            } else {
                // optional error, add -100 if it helps
            }
        }
        Math.pow(dist, 1/mode);
        if (!atLeastOne) {
            dist = 100000;
        }
        return dist;
    }

    private double getSimilarity(WifiFingerprint fp, WifiFingerprint best) {
        double similarity = 0.0;
        double amount = 0;
        for (String key : fp.getWifiMap().keySet()) {
            if (best.getWifiMap().containsKey(key)) {
                amount++;
            }
        }
        similarity =  amount/best.getWifiMap().size();
        return similarity;
    }

    private double getCompleteness(WifiFingerprint fp, WifiFingerprint fp2) {
        double complete = 0.0;
        double amount = 0;
        for (String key : fp.getWifiMap().keySet()) {
            if (fp2.getWifiMap().containsKey(key)) {
                amount++;
            }
        }
        complete = amount/fp.getWifiMap().size();
        return complete;
    }
}

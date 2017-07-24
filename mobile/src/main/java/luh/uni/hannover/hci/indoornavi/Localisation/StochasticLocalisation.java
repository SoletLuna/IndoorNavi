package luh.uni.hannover.hci.indoornavi.Localisation;

import android.util.Log;

import org.apache.commons.math3.distribution.NormalDistribution;

import java.util.ArrayList;
import java.util.List;

import luh.uni.hannover.hci.indoornavi.DataModels.WifiFingerprint;
import luh.uni.hannover.hci.indoornavi.DataModels.WifiFingerprintPDF;

/**
 * Created by solet on 14/07/2017.
 * Locates using gaussian normal distribution, the score is calculated by the cumulative distribution
 * of the difference between the means
 */

public class StochasticLocalisation {

    private List<WifiFingerprintPDF> navPath;
    private String TAG = "StoLoc";

    public StochasticLocalisation(List<WifiFingerprintPDF> navPath) {
        this.navPath = navPath;
    }

    public void measure(WifiFingerprintPDF fp) {
        List<Double> scores = new ArrayList<>();

        for (int i=0; i < navPath.size(); i++) {
            WifiFingerprintPDF current = navPath.get(i);
            scores.add(getScore(current, fp));
        }
        WifiFingerprintPDF bestFp = findBest(scores);
    }

    private WifiFingerprintPDF findBest(List<Double> scores) {
        double score = 0.0;
        int index = 0;
        for (int i=0; i < scores.size(); i++) {
            if (score < scores.get(i)) {
                score = scores.get(i);
                index = i;
            }
        }
        Log.d(TAG, "FP: " + navPath.get(index).getLocation() + ", score: " + scores.get(index));
        return navPath.get(index);
    }

    private double getScore (WifiFingerprintPDF fp1, WifiFingerprintPDF fp2) {
        double score = 1;
        for (String key: fp2.getWifiMapPDF().keySet()) {
            if (fp1.getWifiMapPDF().containsKey(key)) {
                NormalDistribution norm1 = fp1.getWifiMapPDF().get(key);
                NormalDistribution norm2 = fp2.getWifiMapPDF().get(key);
                double mean1 = norm1.getMean();
                double mean2 = norm2.getMean();
                double diff = Math.abs(mean1 - mean2);
                score *= norm1.cumulativeProbability(mean1 + diff);
            }
        }
        return score;
    }

}

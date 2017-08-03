package luh.uni.hannover.hci.indoornavi.Localisation;

import android.util.Log;

import org.apache.commons.math3.distribution.NormalDistribution;

import java.util.ArrayList;
import java.util.List;

import luh.uni.hannover.hci.indoornavi.DataModels.WifiFingerprint;
import luh.uni.hannover.hci.indoornavi.DataModels.WifiFingerprintPDF;
import luh.uni.hannover.hci.indoornavi.Utilities.WifiFingerprintFilter;

/**
 * Created by solet on 14/07/2017.
 * Locates using gaussian normal distribution, the score is calculated by the cumulative distribution
 * of the difference between the means
 */

public class StochasticLocalisation extends Localisation {

    private List<WifiFingerprintPDF> navPath;
    private List<WifiFingerprint> tmp = new ArrayList<>();
    private String TAG = "StoLoc";
    private WifiFingerprintFilter wFilter = new WifiFingerprintFilter();
    private String bla = "";
    private boolean sto = true;
    public StochasticLocalisation(List<WifiFingerprint> navPath) {
        this.navPath = wFilter.addPDFtoFingerprints(navPath);
        tmp = navPath;
    }

    private void listUpdate(List<WifiFingerprint> update) {
        this.navPath = wFilter.addPDFtoFingerprints(update);
    }
    public String measure(WifiFingerprint fp) {
        if (sto) {
            listUpdate(tmp);
            sto = false;
        }
        List<Double> scores = new ArrayList<>();
        List<Double> similarities = new ArrayList<>();
        List<Double> completeness = new ArrayList<>();

        for (int i=0; i < navPath.size(); i++) {
            WifiFingerprintPDF current = navPath.get(i);
            scores.add(getScore(current, fp));
            similarities.add(getSimilarity(fp, current));
            completeness.add(getCompleteness(fp, current));

        }

        WifiFingerprintPDF bestFp = findBest(scores, similarities);
        double sim = getSimilarity(fp, bestFp);
        double compl = getSimilarity(fp, bestFp);
        return bla + ", " + sim + ", " + compl;
    }

    private WifiFingerprintPDF findBest(List<Double> scores, List<Double> similarities) {
        double score = 0.0;
        int index = 0;
        for (int i=0; i < scores.size(); i++) {
            if (score < scores.get(i) && similarities.get(i) >= 0.5) {
                score = scores.get(i);
                index = i;
            }
        }
        Log.d(TAG, "FP: " + navPath.get(index).getLocation() + ", score: " + scores.get(index));
        bla = "FP: " + navPath.get(index).getLocation() + ", score: " + scores.get(index);
        return navPath.get(index);
    }

    private double getScore (WifiFingerprintPDF fp1, WifiFingerprint fp2) {
        double score = 1;
        boolean atLeastOne = false;
        for (String key: fp2.getWifiMap().keySet()) {
            if (fp1.getWifiMapPDF().containsKey(key)) {
                NormalDistribution norm1 = fp1.getWifiMapPDF().get(key);
                if (norm1.getStandardDeviation() <= 1.0) {
                    continue;
                }
                double mean1 = norm1.getMean();
                double value = fp2.getWifiMap().get(key).get(0);
                double diff = Math.abs(mean1 - value);
                //Log.d(TAG, norm1.cumulativeProbability(mean1 - diff) + "");
                score *= norm1.cumulativeProbability(mean1 - diff)*2;
                atLeastOne = true;
            }
        }
        if (!atLeastOne) {
            score = 0.0;
        }
        return score;
    }

    private double getSimilarity(WifiFingerprint fp, WifiFingerprintPDF fp2) {
        double similarity = 0.0;
        double amount = 0;
        for (String key : fp.getWifiMap().keySet()) {
            if (fp2.getWifiMapPDF().containsKey(key)) {
                amount++;
            }
        }
        similarity = amount/fp2.getWifiMapPDF().size();
        return similarity;
    }

    private double getCompleteness(WifiFingerprint fp, WifiFingerprintPDF fp2) {
        double complete = 0.0;
        double amount = 0;
        for (String key : fp.getWifiMap().keySet()) {
            if (fp2.getWifiMapPDF().containsKey(key)) {
                amount++;
            }
        }
        complete = amount/fp.getWifiMap().size();
        return complete;
    }

}

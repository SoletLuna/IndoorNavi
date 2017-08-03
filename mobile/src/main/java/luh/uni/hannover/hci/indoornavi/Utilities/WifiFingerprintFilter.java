package luh.uni.hannover.hci.indoornavi.Utilities;

import android.util.Log;

import org.apache.commons.math3.distribution.NormalDistribution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import luh.uni.hannover.hci.indoornavi.DataModels.WifiFingerprint;
import luh.uni.hannover.hci.indoornavi.DataModels.WifiFingerprintPDF;

/**
 * Created by solet on 16/05/2017.
 * Class that takes a list of fingerprints and returns a filtered list according to certain criteria.
 */

public class WifiFingerprintFilter {

    private String TAG = "WFilter";
    /**
     * for a given fingerprint, return the n best RSS values
     * @param inFp
     * @param n
     * @return
     */
    public WifiFingerprint filterBestInFingerprint(WifiFingerprint inFp, int n) {
        WifiFingerprint outFP = new WifiFingerprint(inFp.getLocation());
        outFP.setStepCount(inFp.getStepCount());
        List<FilterData> list = new ArrayList<>();

        // easier to parse data
        for (String key : inFp.getWifiMap().keySet()) {
            FilterData data = new FilterData(key, inFp.getWifiMap().get(key).get(0));
            list.add(data);
        }
        List<String> processed = new ArrayList<>();
        while (processed.size() < n) {
            double max = -1000;
            String loc = "";
            int index = -1;
            for (int i=0; i < list.size(); i++) {
                String bssid = list.get(i).loc;
                double rss = list.get(i).val;

                for (String str : processed) {
                    if (str.trim().contains(bssid))
                        continue;
                }
                if (rss > max) {
                    max = rss;
                    loc = bssid;
                    index = i;
                }
            }
            processed.add(loc);
            outFP.addRSS(loc, max);
            list.remove(index);
        }
        return outFP;
    }

    /**
     * for a list of fps, returns the n best RSS values for the list
     * @param inList
     * @param n
     * @return
     */
    public List<WifiFingerprint> filterBestInMultipleFingerprints(List<WifiFingerprint> inList, int n) {
        ArrayList<WifiFingerprint> outList = new ArrayList<>();
        for (int i=0; i < inList.size(); i++) {
            outList.add(filterBestInFingerprint(inList.get(i), n));
        }
        return outList;
    }

    /**
     * If there are multiple scans for a wifingerprint, returns the average for the list
     */
    public List<WifiFingerprint> filterAverageRSS(List<WifiFingerprint> inList) {
        ArrayList<WifiFingerprint> outList = new ArrayList<>();

        for(int i=0; i < inList.size(); i++) {
            WifiFingerprint fp = inList.get(i);
            WifiFingerprint newFP = new WifiFingerprint(fp.getLocation());
            newFP.setStepCount(fp.getStepCount());
            for (String key : fp.getWifiMap().keySet()) {
                List<Double> values = fp.getWifiMap().get(key);
                double sum = 0.0;
                for (Double val : values) {
                    sum += val;
                }
                double avg = sum / values.size();
                newFP.addRSS(key, avg);
            }
            outList.add(newFP);
        }
        return outList;
    }

    /**
     * If there are multiple scans for a fingerprint, return the best rss value for each fp
     * @param inList
     * @return
     */
    public List<WifiFingerprint> filterBestRSS(List<WifiFingerprint> inList) {
        ArrayList<WifiFingerprint> outList = new ArrayList<>();

        for(int i=0; i < inList.size(); i++) {
            WifiFingerprint fp = inList.get(i);
            WifiFingerprint newFP = new WifiFingerprint(fp.getLocation());
            newFP.setStepCount(fp.getStepCount());
            for (String key : fp.getWifiMap().keySet()) {
                List<Double> values = fp.getWifiMap().get(key);
                double max = Collections.max(values);
                newFP.addRSS(key, max);
            }
            outList.add(newFP);
        }

        return outList;
    }

    public WifiFingerprint filterBadSignals(WifiFingerprint fp, double threshold) {
        WifiFingerprint filtered = new WifiFingerprint(fp.getLocation());
        Set<String> keys = fp.getWifiMap().keySet();
        for (String key : keys) {
            double value = fp.getWifiMap().get(key).get(0);
            if (value >= threshold) {
                filtered.addRSS(key, value);
            }
        }

        filtered.setStepCount(fp.getStepCount());
        return filtered;
    }

    /**
     * Calculates standard deviation and mean value for PDF and returns FP as a PDF
     * @param fp
     */
    public WifiFingerprintPDF addPDF(WifiFingerprint fp) {
        WifiFingerprintPDF pdf = new WifiFingerprintPDF(fp.getLocation());
        pdf.setDirection(fp.getDirection());
        pdf.setStepCount(fp.getStepCount());
        Set<String> keys = fp.getWifiMap().keySet();
        for (String key : keys) {
            List<Double> list = fp.getWifiMap().get(key);
            double mean = calculateMean(list);
            double sigma = calculateSDeviation(list, mean);
            NormalDistribution norm = new NormalDistribution(mean, sigma);
            pdf.addPDF(key, norm);
        }

        return pdf;
    }

    public List<WifiFingerprintPDF> addPDFtoFingerprints(List<WifiFingerprint> list) {
        List<WifiFingerprintPDF> listPDF = new ArrayList<>();
        for (int i=0; i < list.size(); i++) {
            WifiFingerprint fp = list.get(i);
            listPDF.add(addPDF(fp));
        }
        return listPDF;
    }

    private double calculateMean(List<Double> list) {
        double sum = 0.0;
        for (Double value : list) {
            sum += value;
        }
        sum = sum / list.size();

        return sum;
    }

    private double calculateSDeviation(List<Double> list, double mean) {
        double squaredSum = 0.0;
        for (Double value : list) {
            double dev = Math.pow(value - mean, 2);
            squaredSum += dev;
        }
        squaredSum = squaredSum / list.size();

        double sigma = Math.sqrt(squaredSum);

        if (sigma <= 0) {
            sigma = 0.01;
        }
        return Math.ceil(sigma*2);
    }


    private class FilterData {

        String loc;
        double val;

        public FilterData(String loc, double val) {
            this.loc = loc;
            this.val = val;
        }
    }
}

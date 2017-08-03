package luh.uni.hannover.hci.indoornavi.DataModels;

import android.text.TextUtils;

import org.apache.commons.math3.distribution.NormalDistribution;

import java.util.HashMap;

/**
 * Created by solet on 04/07/2017.
 */

public class WifiFingerprintPDF extends WifiFingerprint {

    private HashMap<String, NormalDistribution> wifiMapPDF = new HashMap<>();

    public WifiFingerprintPDF(String loc) {
        super(loc);
    }

    public void addPDF(String bssid, NormalDistribution norm) {
        wifiMapPDF.put(bssid, norm);
    }

    public HashMap<String, NormalDistribution> getWifiMapPDF() {
        return wifiMapPDF;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.getLocation());
        sb.append(System.lineSeparator());
        for (String key : getWifiMapPDF().keySet()) {
            sb.append(key + ": ");
            double mean = getWifiMapPDF().get(key).getMean();
            double sd = getWifiMapPDF().get(key).getStandardDeviation();
            sb.append(mean + ", " + sd);
            sb.append(System.lineSeparator());
        }
        return sb.toString();
    }
}

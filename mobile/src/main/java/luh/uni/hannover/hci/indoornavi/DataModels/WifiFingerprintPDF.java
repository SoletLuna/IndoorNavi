package luh.uni.hannover.hci.indoornavi.DataModels;

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
}

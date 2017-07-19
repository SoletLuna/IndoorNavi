package luh.uni.hannover.hci.indoornavi.Utilities;

import java.util.List;

import luh.uni.hannover.hci.indoornavi.DataModels.WifiFingerprint;
import luh.uni.hannover.hci.indoornavi.DataModels.WifiFingerprintPDF;

/**
 * Created by solet on 14/07/2017.
 */

public class StochasticLocalisation {

    List<WifiFingerprintPDF> navPath;

    public StochasticLocalisation(List<WifiFingerprintPDF> navPath) {
        this.navPath = navPath;
    }

    public void measure(WifiFingerprintPDF fp) {
        int index = 0;
        double score = 0.0;

        for (int i=0; i < navPath.size(); i++) {

        }
    }


    public void getMatchings(WifiFingerprintPDF fp) {}

}

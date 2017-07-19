package luh.uni.hannover.hci.indoornavi.Utilities;

import java.util.List;

import luh.uni.hannover.hci.indoornavi.DataModels.WifiFingerprint;

/**
 * Created by solet on 14/07/2017.
 */

public class DistanceLocalisation {

    List<WifiFingerprint> navPath;

    public DistanceLocalisation(List<WifiFingerprint> navPath) {
        this.navPath = navPath;
    }

    public void measure(WifiFingerprint fp) {

    }
}

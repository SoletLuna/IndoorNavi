package luh.uni.hannover.hci.indoornavi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by solet on 05/04/2017.
 * Class that holds all information regarding the fingerprints and navigationpath for the navigation
 * and scanning. It also presents all operations between fingerprints
 */

public class WifiCoordinator {

    private List<WifiFingerprint> fpList = new ArrayList<>();

    public WifiCoordinator() {

    }

    public List<WifiFingerprint> getFingerprints() {
        return fpList;
    }

    public void addFingerprint(WifiFingerprint fp) {
        fpList.add(fp);
    }

    public void addScansToFingerprint(List<WifiFingerprint> fpList, String location) {
        WifiFingerprint createdFP = new WifiFingerprint(location);

        for (WifiFingerprint fp : fpList) {
            for (String key : fp.getWifiMap().keySet()) {
                createdFP.addRSS(key, fp.getWifiMap().get(key).get(0));
            }
        }
        addFingerprint(createdFP);
    }

    public void loadNavigationPath() {

    }

    public double getDistanceBetweenFP(WifiFingerprint fp1, WifiFingerprint fp2) {
        double dist = 0.0;
        return dist;
    }
}

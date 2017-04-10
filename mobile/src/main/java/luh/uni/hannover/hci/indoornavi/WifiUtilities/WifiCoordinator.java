package luh.uni.hannover.hci.indoornavi.WifiUtilities;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by solet on 05/04/2017.
 * Class that holds all information regarding the fingerprints and navigationpath for the navigation
 * and scanning. It also presents all operations between fingerprints. It can save the NavPath to file
 * and load it from a selection from file. It will ever only hold one NavPath at the same time.
 * It also chooses the method for operating with the received RSS vector: distance, similarity, NN, KWNN
 */

public class WifiCoordinator {

    private List<WifiFingerprint> navigationPath = new ArrayList<>();
    private WifiFingerprintFilter wifiFilter;

    public WifiCoordinator() {

    }

    public List<WifiFingerprint> getFingerprints() {
        return navigationPath;
    }

    public void addFingerprint(WifiFingerprint fp) {
        navigationPath.add(fp);
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

    public WifiFingerprint getLastAddedFingerprint() {
        return navigationPath.get(navigationPath.size()-1);
    }

    /**
     *
     * @param fp1
     * @param fp2
     * @param mode MD = 1, ED = 2
     * @return distance between fp1 and fp2
     */
    public double getDistanceBetweenFP(WifiFingerprint fp1, WifiFingerprint fp2, int mode) {
        double dist = 0.0;
        return dist;
    }

    public double getSimilarityBetweenFP(WifiFingerprint fp1, WifiFingerprint fp2) {
        double sim = 0.0;
        return sim;
    }

    public WifiFingerprint getNearestNeighbour(WifiFingerprint fp) {
        WifiFingerprint nearestFP = new WifiFingerprint("");
        return nearestFP;
    }

    public List<WifiFingerprint> selectNavigationPath() {
        //Display possible NavigationPaths and select one
        return navigationPath;
    }

    private void loadNavigationPathFromFile() {

    }

    private void saveNavigationPathToFile() {

    }
}

package luh.uni.hannover.hci.indoornavi.WifiUtilities;

import java.util.ArrayList;
import java.util.List;

import luh.uni.hannover.hci.indoornavi.DataModels.WifiFingerprint;

/**
 * Created by solet on 05/04/2017.
 * Class that holds all information regarding the fingerprints and navigationpath for the navigation
 * and scanning. It also presents all operations between fingerprints. It can save the NavPath to file
 * and load it from a selection from file. It will ever only hold one NavPath at the same time.
 * It also chooses the method for operating with the received RSS vector: distance, similarity, NN, KWNN
 */

public class WifiCoordinator {

    private List<WifiFingerprint> navigationPath = new ArrayList<>();
    private List<WifiFingerprint> filteredPath = new ArrayList<>();
    private WifiFingerprintFilter wifiFilter;
    private WifiFingerprint targetLoc;
    private WifiFingerprint nextLoc;
    private WifiFingerprint unknownLoc;
    private WifiFingerprint lastLoc;
    private int locationIndex = 1;
    private int stepCount = 0; // internal step count, supplied by step count between all fingerprints

    public WifiCoordinator() {
        loadNavigationPathFromFile();
//        lastLoc = navigationPath.get(0);
//        nextLoc = navigationPath.get(1);
//       targetLoc = navigationPath.get(navigationPath.size()-1);
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

    public void setTargetLoc(WifiFingerprint loc) {
        targetLoc = loc;
    }

    public void setNextLoc(WifiFingerprint loc) {
        nextLoc = loc;
    }

    public void setLastLoc(WifiFingerprint loc) {
        lastLoc = loc;
    }

    /** Only use this when a fingerprint's RSSI vector only contains one value per bssid
     *
     * @param fp1 unknown fp from scan
     * @param fp2 known fp from database
     * @param mode Manhattan distance = 1, Euclidean distance = 2
     * @return distance between fp1 and fp2
     */
    public double getDistanceBetweenFP(WifiFingerprint fp1, WifiFingerprint fp2, int mode) {
        double dist = 0.0;
        for (String key : fp1.getWifiMap().keySet()) {
            int level1 = fp1.getWifiMap().get(key).get(0);
            if (fp2.getWifiMap().containsKey(key)) {
                int level2 = fp2.getWifiMap().get(key).get(0);
                dist += Math.pow(level1 - level2, mode);
            } else {
                // optional error, add -100 if it helps
            }
        }
        Math.pow(dist, 1/mode);
        return dist;
    }

    public double getDistanceToNextFP(int mode) {
        WifiFingerprint fp1 = unknownLoc;
        WifiFingerprint fp2 = nextLoc;
        double dist = 0.0;
        for (String key : fp1.getWifiMap().keySet()) {
            int level1 = fp1.getWifiMap().get(key).get(0);
            if (fp2.getWifiMap().containsKey(key)) {
                int level2 = fp2.getWifiMap().get(key).get(0);
                dist += Math.pow(level1 - level2, mode);
            } else {
                // optional error, add -100 if it helps
            }
        }
        Math.pow(dist, 1/mode);
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

    public void setUnknown(WifiFingerprint fp) {
        unknownLoc = fp;
    }

    public List<WifiFingerprint> selectNavigationPath() {
        //Display possible NavigationPaths and select one
        return navigationPath;
    }

    // whenever the next fingerprint is reached, updates last and next location.
    public int reachedCheckpoint() {
        if (nextLoc.getLocation().equals(targetLoc.getLocation())) {
            // goal reached
        }
        locationIndex++;
        stepCount = stepCount + nextLoc.getStepCount();
        lastLoc = nextLoc;
        nextLoc = navigationPath.get(locationIndex);

        return stepCount;
    }

    private void loadNavigationPathFromFile() {

    }

    private void saveNavigationPathToFile() {

    }
}

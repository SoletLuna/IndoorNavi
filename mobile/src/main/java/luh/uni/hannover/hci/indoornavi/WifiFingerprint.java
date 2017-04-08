package luh.uni.hannover.hci.indoornavi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by solet on 05/04/2017.
 */

public class WifiFingerprint {

    private HashMap<String, List<Integer>> wifiMap = new HashMap<>();
    private String loc = "";
    private int stepToFP;

    public WifiFingerprint(String ID) {
        loc = ID;
    }

    public HashMap<String, List<Integer>> getWifiMap() {
        return wifiMap;
    }

    public void addRSS(String bssid, int level) {
        if (wifiMap.containsKey(bssid)) {
            wifiMap.get(bssid).add(level);
        } else {
            List<Integer> list = new ArrayList<>();
            list.add(level);
            wifiMap.put(bssid, list);
        }
    }

    public String getLocation() {
        return loc;
    }

    public int getNumberOfAPs() {
        return wifiMap.size();
    }

    public void setStepCount(int step) {
        this.stepToFP = step;
    }

    public int getStepCount() {
        return stepToFP;
    }

}

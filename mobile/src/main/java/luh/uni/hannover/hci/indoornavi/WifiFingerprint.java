package luh.uni.hannover.hci.indoornavi;

import java.util.HashMap;
import java.util.List;

/**
 * Created by solet on 05/04/2017.
 */

public class WifiFingerprint {

    private HashMap<String, List<Double>> wifiMap = new HashMap<>();
    private String ID = "";

    public WifiFingerprint(String ID) {
        ID = ID;
    }
}

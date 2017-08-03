package luh.uni.hannover.hci.indoornavi.Utilities;

import java.util.List;

import luh.uni.hannover.hci.indoornavi.DataModels.WifiFingerprint;
import luh.uni.hannover.hci.indoornavi.Localisation.DistanceLocalisation;
import luh.uni.hannover.hci.indoornavi.Localisation.Localisation;
import luh.uni.hannover.hci.indoornavi.Localisation.StochasticLocalisation;

/**
 * Created by solet on 31/07/2017.
 */

public class Navigator {

    private int currentIndex;
    private String nextFp;
    private String lastFp;
    private Localisation localisator;
    private List<WifiFingerprint> navPath;

    public Navigator(int locId, List<WifiFingerprint> navPath) {
        this.navPath = navPath;
        switch (locId) {
            case 1:
                localisator = new DistanceLocalisation(navPath);
                return;
            case 2:
                localisator = new StochasticLocalisation(navPath);
                return;
        }
    }

    public void startNavigation() {
        currentIndex = 0;
        nextFp = navPath.get(currentIndex).getLocation();
        lastFp = navPath.get(navPath.size()-1).getLocation();
    }

    public int resolveMeasure(WifiFingerprint fp) {
        String str = localisator.measure(fp);
        if (str == nextFp) {
            currentIndex++;
            nextFp = navPath.get(currentIndex).getLocation();
        }

        return currentIndex;
    }



}

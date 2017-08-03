package luh.uni.hannover.hci.indoornavi.Utilities;

import java.util.List;

import luh.uni.hannover.hci.indoornavi.DataModels.WifiFingerprint;
import luh.uni.hannover.hci.indoornavi.Localisation.LocalisationParticle;
import luh.uni.hannover.hci.indoornavi.Localisation.ParticleFilter;
import luh.uni.hannover.hci.indoornavi.Localisation.ParticleFilterStatic;

/**
 * Created by solet on 31/07/2017.
 */

public class NavigatorParticle {

    int stripeStart;
    int stripeEnd;
    int currentFP;
    LocalisationParticle localisator;
    List<WifiFingerprint> navPath;

    public NavigatorParticle(int locId, List<WifiFingerprint> navPath, int numberOfParticles) {
        navPath = navPath;
        switch (locId) {
            case 1: localisator = new ParticleFilter(numberOfParticles, navPath);
                return;
            case 2: localisator = new ParticleFilterStatic(numberOfParticles, navPath);
        }
    }

    public void startNavigation() {
        createStripe();
    }

    public void resolveMeasure(WifiFingerprint fp) {
        //localisator.measure(fp, stripeStart, stripeEnd);
    }

    private void createStripe() {
        stripeStart = 0;
        for (int i=0; i <navPath.size(); i++) {
            WifiFingerprint fp = navPath.get(i);
            if (fp.getDirection() != "") {
                stripeEnd = i;
                break;
            }
        }
    }
}

package luh.uni.hannover.hci.indoornavi.Localisation;

import luh.uni.hannover.hci.indoornavi.DataModels.WifiFingerprint;

/**
 * Created by solet on 31/07/2017.
 */

public abstract class LocalisationParticle {

    abstract public String measure(WifiFingerprint fp);
}

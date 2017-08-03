package luh.uni.hannover.hci.indoornavi.Localisation;

import luh.uni.hannover.hci.indoornavi.DataModels.WifiFingerprint;

/**
 * Created by solet on 28/07/2017.
 */

abstract public class Localisation {

    abstract public String measure(WifiFingerprint fp);
}

package luh.uni.hannover.hci.indoornavi.Localisation;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;

import luh.uni.hannover.hci.indoornavi.DataModels.Particle;
import luh.uni.hannover.hci.indoornavi.DataModels.WifiFingerprint;

/**
 * Created by solet on 31/07/2017.
 */

public class ParticleFilterStatic extends LocalisationParticle{

    private int numberOfParticles;
    private List<Particle> listOfParticles = new ArrayList<>();
    private Random rnd = new Random();
    private List<WifiFingerprint> navPath = new ArrayList<>();
    private List<Integer> navPoints = new ArrayList<>();
    private int xMax;
    private double stepLength = 1;
    private String TAG = "ParticleFilter";
    private int pD = 1;

    public ParticleFilterStatic(int particles, List<WifiFingerprint> path) {
        numberOfParticles = particles;
        navPath = path;
    }


    /**
     * interpolate between the fingerprints, so particles can measure
     * only interpolate when the neighbouring fingerprints actually have similar bssids in its vector
     */
    private double linearInterpolate(double x1, double x2, double mu) {
        return (x1 * (1-mu) + x2*mu);
    }

    private double cosineInterpolate(double x1, double x2, double mu) {
        double mu2 = (1- Math.cos(mu*Math.PI)/2);
        return (x1 *(1-mu2) + x2*mu2);
    }

    public void reset(int particles) {
        listOfParticles.clear();
        numberOfParticles = particles;
        start();
    }

    public void start() {
        xMax = 0;
        double dMax = 0;
        for (WifiFingerprint fp : navPath) {
            xMax += fp.getStepCount();
            dMax += fp.getStepCount();
            navPoints.add(xMax);
        }
        for (int i=0; i < numberOfParticles; i++) {
            double number = numberOfParticles;
            Particle p = new Particle(i* dMax/(numberOfParticles-1), 1/number);
            //Particle p = new Particle(0.0, 1/number);
            listOfParticles.add(p);
        }
    }

    public String getBestParticle() {
        double x = 0;
        double w = 0;
        double similarity = 0;
        for (Particle p : listOfParticles) {
            if (p.weight > w) {
                w = p.weight;
                x = p.x;
                similarity = p.similarity;
            }
        }
        String best = x + ", " + w + ", " + similarity;
        return best;
    }

    public String getBestParticles(int n) {
        double x = 0;
        double w = 0;
        double similarity = 0;
        String best = "";
        List<Particle> tmpList = new ArrayList<>();
        Particle tmpParticle = new Particle(0,0);
        for (Particle p : listOfParticles) {
            tmpList.add(p);
        }
        for (int i=0; i < n; i++) {
            for (Particle p : tmpList) {
                if (p.weight > w) {
                    w = p.weight;
                    x = p.x;
                    similarity = p.similarity;
                    tmpParticle = p;
                }
            }
            w = 0;
            best += System.lineSeparator();
            best += tmpParticle.x + ", " + tmpParticle.weight + " - ";
            tmpList.remove(tmpParticle);
        }
        return best;
    }

    public void stepParticles() {
        for (Particle p : listOfParticles) {
            double x = p.x;
            x += stepLength * (rnd.nextGaussian() *0.15 + 1);
            if (x > xMax)
                p.x = xMax;
            else
                p.x = x;
        }
    }

    public void stepBackParticles() {
        for (Particle p : listOfParticles) {
            double x = p.x;
            x -= stepLength * (rnd.nextGaussian() *0.15 + 1);
            if (x < 0)
                p.x = 0;
            else
                p.x = x;
        }
    }

    public List<Particle> getListOfParticles() {
        return listOfParticles;
    }

    public String measure(WifiFingerprint fp) {
        HashMap<String, List<Double>> measurement = fp.getWifiMap();
        Set<String> keys = measurement.keySet();
        int pID = 1;

        for (Particle p : listOfParticles) {
            List<Double> interpolatedList = new ArrayList<>();
            List<Double> measuredList = new ArrayList<>();
            for (String key : keys) {
                int l = -1;
                int r = -1;
                for (int i=0; i < navPath.size(); i++) {
                    //Log.d(TAG, "Key: " + key + ", " + navPath.get(i).getWifiMap().keySet().toString());
                    if (navPath.get(i).getWifiMap().containsKey(key)) {
                        if (navPoints.get(i) <= p.x) {
                            l = i;
                        }
                        if (navPoints.get(i) >= p.x) {
                            if (i > r && r >= 0) {

                            } else {
                                r = i;
                            }
                        }
                    }
                }
                if ((r >= 0 && l >= 0)) {
                    double valueM = measurement.get(key).get(0);
                    double valueL = navPath.get(l).getWifiMap().get(key).get(0);
                    double valueR = navPath.get(r).getWifiMap().get(key).get(0);
                    double mu;
                    double posL = navPoints.get(l);
                    double posR = navPoints.get(r);
                    if ((l - r) == 0) {
                        mu = 0;
                    } else {
                        mu = (p.x - posL) / (posR - posL);
                    }
                    double valueI = linearInterpolate(valueL, valueR, mu);
                    //Log.d(TAG, pID + ": " + p.x +" - "+l+","+r+" - " + mu +" - " +valueL +", "+valueR+", "+valueI + " - " + key);
                    interpolatedList.add(valueI);
                    measuredList.add(valueM);
                }

            }
            p.similarity = ((double)measuredList.size())/ ((double) fp.getWifiMap().size());
            p.weight = calculateWeight(interpolatedList, measuredList, p.similarity);
            Log.d(TAG, pID + ": " + p.x + " - " + p.weight + " - " + p.similarity);
            pID++;
        }
        normalizeWeights();
        return "";
    }

    public String measure(WifiFingerprint fp, List<WifiFingerprint> stripes) {
        HashMap<String, List<Double>> measurement = fp.getWifiMap();
        Set<String> keys = measurement.keySet();
        int pID = 1;

        for (Particle p : listOfParticles) {
            List<Double> interpolatedList = new ArrayList<>();
            List<Double> measuredList = new ArrayList<>();
            for (String key : keys) {
                int l = -1;
                int r = -1;
                for (int i=0; i < navPath.size(); i++) {
                    //Log.d(TAG, "Key: " + key + ", " + navPath.get(i).getWifiMap().keySet().toString());
                    if (navPath.get(i).getWifiMap().containsKey(key)) {
                        if (navPoints.get(i) <= p.x) {
                            l = i;
                        }
                        if (navPoints.get(i) >= p.x) {
                            if (i > r && r >= 0) {

                            } else {
                                r = i;
                            }
                        }
                    }
                }
                if ((r >= 0 && l >= 0)) {
                    double valueM = measurement.get(key).get(0);
                    double valueL = navPath.get(l).getWifiMap().get(key).get(0);
                    double valueR = navPath.get(r).getWifiMap().get(key).get(0);
                    double mu;
                    double posL = navPoints.get(l);
                    double posR = navPoints.get(r);
                    if ((l - r) == 0) {
                        mu = 0;
                    } else {
                        mu = (p.x - posL) / (posR - posL);
                    }
                    double valueI = linearInterpolate(valueL, valueR, mu);
                    //Log.d(TAG, pID + ": " + p.x +" - "+l+","+r+" - " + mu +" - " +valueL +", "+valueR+", "+valueI + " - " + key);
                    interpolatedList.add(valueI);
                    measuredList.add(valueM);
                }

            }
            p.similarity = ((double)measuredList.size())/ ((double) fp.getWifiMap().size());
            p.weight = calculateWeight(interpolatedList, measuredList, p.similarity);
            Log.d(TAG, pID + ": " + p.x + " - " + p.weight + " - " + p.similarity);
            pID++;
        }
        normalizeWeights();
        return "";
    }

    private double calculateScore(List<Double> list) {
        double score = 1;
        for (Double value : list) {
            score *= value;
        }

        return score;
    }

    private double calculateWeight(List<Double> values, List<Double> measuredValues, double similarity) {
        double weight;
        double distance = 0;

        //calculate euclidian distance as preliminary weight
        for (int i=0; i < values.size(); i++) {
            distance += Math.pow(Math.abs(values.get(i) - measuredValues.get(i)),2);
        }
        distance = Math.sqrt(distance);
        weight = distance; // * 1/similarity;
        //Log.d(TAG, values.toString() + " - " + measuredValues.toString() + " - " + weight);
        return weight;
    }

    private void normalizeWeights() {
        double weight = 0;
        for (Particle p : listOfParticles) {
            weight += p.weight;
        }
        for (Particle p : listOfParticles) {
            p.weight = weight/p.weight;
        }
        weight = 0;
        for (Particle p : listOfParticles) {
            weight += p.weight;
        }
        for (Particle p : listOfParticles) {
            p.weight = p.weight/weight;
        }

    }


    /**
     * calculates the accuracy for the given measurement
     * accuracy is calculated by checking the distance between the APs appearing in the measurement
     * e.g if AP1 appears in the measurement and AP1 is in both FP next to the estimation, then for AP1 the accuracy is high
     * do this for every AP in measurement
     * @param keys
     * @return
     */
    public double estimateAccuracy(Set<String>  keys) {
        double acc = 0;
        List<Integer> leftList = new ArrayList<>();
        List<Integer> rightList = new ArrayList<>();
        double x = 0.5; //lastEstimate;

        // find all fingerprints left and right of estimate
        for (int i=0; i < navPoints.size(); i++) {
            if (navPoints.get(i) <= x) {
                leftList.add(navPoints.get(i));
            }
            else if (navPoints.get(i) >= x) {
                rightList.add(navPoints.get(i));
            }
        }

        // find the first left and the first right fp, in which AP of measurement appears
        for (String ap : keys) {
            int l = -1;
            int r = -1;
            for (int i=0; i < navPath.size(); i++) {
                if (i < leftList.size()) {
                    if (navPath.get(i).getWifiMap().containsKey(ap)) {
                        l = navPoints.get(i);
                    }
                } else {
                    if (navPath.get(i).getWifiMap().containsKey(ap)) {
                        r = navPoints.get(i);
                        break;
                    }
                }
            }
            // if there is no AP left or right of estimate
            if (l < 0 || r < 0) {
                acc += 0;
            } else {
                int fpDistance = r - l;

            }
        }

        return acc;
    }

    /**
     * Returns if fingerprint data is on the same 1D-stripe.  Data that is on another 1D-stripe should
     * count less for weighting. (Imagine a path along a rectangle
     * @return
     */
    private boolean onPath(double x, double left, double right) {
        boolean isOnPath = false;

        for (int i=0; i < navPoints.size(); i++) {
            if (navPoints.get(i) > x) {

            }
        }
        return isOnPath;
    }
}

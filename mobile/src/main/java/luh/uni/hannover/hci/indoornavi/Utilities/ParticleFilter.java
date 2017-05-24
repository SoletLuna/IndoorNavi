package luh.uni.hannover.hci.indoornavi.Utilities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import luh.uni.hannover.hci.indoornavi.DataModels.Particle;
import luh.uni.hannover.hci.indoornavi.DataModels.WifiFingerprint;

/**
 * Created by solet on 23/05/2017.
 */

public class ParticleFilter {

    private int numberOfParticles;
    private List<Particle> listOfParticles = new ArrayList<>();
    private Random rnd = new Random();
    private List<WifiFingerprint> navPath = new ArrayList<>();
    private List<Integer> navPoints = new ArrayList<>();
    private int xMax;
    private double stepLength = 1;

    public ParticleFilter(int particles, List<WifiFingerprint> path) {
        numberOfParticles = particles;
        navPath = path;
        for (int i=0; i < numberOfParticles; i++) {
            Particle p = new Particle(i, 1/numberOfParticles);
            listOfParticles.add(p);
        }
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

    public void start() {
        xMax = 0;
        for (WifiFingerprint fp : navPath) {
            navPoints.add(xMax);
            xMax += fp.getStepCount();
        }
    }

    private Particle sampleParticle() {
        if (listOfParticles.isEmpty()) {
            return null;
        }

        double x = rnd.nextDouble();
        double w = 0.0;

        for (Particle p : listOfParticles) {
            if (x < w + p.weight) {
                return p;
            }
            w += p.weight;
        }
        return listOfParticles.get(listOfParticles.size()-1);
    }

    public double estimatePosition() {
        double pos = 0.0;

        for (Particle p : listOfParticles) {
            pos += p.x * p.weight;
        }
        return pos;
    }

    public void stepParticles() {
        for (Particle p : listOfParticles) {
            p.x += stepLength * (rnd.nextGaussian() *0.15 + 1);
        }
    }

    /**
     * uses the incoming measurement that has already been filtered
     *
     * @param fp
     */
    public void measureParticles(WifiFingerprint fp) {
        HashMap<String, List<Double>> map = fp.getWifiMap();

        for (Particle p : listOfParticles) {
            double x = p.x;
            int l = -1, r = -1;
            double valueLeft = 0;
            double valueRight = 0;
            double valueInterpolated = 0;
            List<Integer> leftList = new ArrayList<>();
            List<Integer> rightList = new ArrayList<>();
            List<Double> interpolatedList = new ArrayList<>();
            List<Double> measuredList = new ArrayList<>();

            // find all fingerprints left and right of particle
            for (int i=0; i < navPoints.size(); i++) {
                if (navPoints.get(i) <= x) {
                    leftList.add(navPoints.get(i));
                }
                else if (navPoints.get(i) >= x) {
                    rightList.add(navPoints.get(i));
                }
            }
            //find l and r, and their values in path for every AP in measurement, interpolate and calculate weight
            for (String keys : map.keySet()) {
                double measuredValue = map.get(keys).get(0);
                measuredList.add(measuredValue);
                // find l, r and values
                for (int i=0; i < navPath.size(); i++) {
                    if (i < leftList.size()) {
                        if (navPath.get(i).getWifiMap().containsKey(keys)) {
                            l = navPoints.get(i);
                            valueLeft = navPath.get(i).getWifiMap().get(keys).get(0);
                        }
                    } else {
                        if (navPath.get(i).getWifiMap().containsKey(keys)) {
                            r = navPoints.get(i);
                            valueRight = navPath.get(i).getWifiMap().get(keys).get(0);
                            break;
                        }
                    }
                }
                // if r or l not found
                if (r < 0 || l < 0) {
                    valueInterpolated = -90;
                    interpolatedList.add(valueInterpolated);
                } else {
                    //interpolate
                    double mu = (x - l) / (r - l);
                    valueInterpolated = cosineInterpolate(valueLeft, valueRight, mu);
                    interpolatedList.add(valueInterpolated);
                }
            }
            p.weight = calculateWeight(interpolatedList, measuredList);
        }

        normalizeWeights();
    }
    private double calculateWeight(List<Double> values, List<Double> measuredValues) {
        double weight = 0.0;
        double distance = 0;
        //calculate euclidian distance
        for (int i=0; i < values.size(); i++) {
            distance += Math.pow(Math.abs(values.get(i) - measuredValues.get(i)),2);
        }
        distance = Math.sqrt(distance);
        weight = distance / Math.sqrt(values.size() * (90*90));
        return weight;
    }

    private void normalizeWeights() {
        double weight = 0;
        for (Particle p : listOfParticles) {
            weight += p.weight;
        }

        for (Particle p : listOfParticles) {
            p.weight += p.weight/weight;
        }
    }
}

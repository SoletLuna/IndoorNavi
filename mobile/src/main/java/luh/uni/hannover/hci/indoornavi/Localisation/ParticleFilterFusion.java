package luh.uni.hannover.hci.indoornavi.Localisation;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;

import luh.uni.hannover.hci.indoornavi.DataModels.Particle;
import luh.uni.hannover.hci.indoornavi.DataModels.WifiFingerprint;
import luh.uni.hannover.hci.indoornavi.DataModels.WifiFingerprintPDF;

/**
 * Created by solet on 03/08/2017.
 */

public class ParticleFilterFusion extends LocalisationParticle {

    private int numberOfParticles;
    private List<Particle> listOfParticles = new ArrayList<>();
    private List<Particle> staticParticles = new ArrayList<>();
    private Random rnd = new Random();
    private List<WifiFingerprint> navPath = new ArrayList<>();
    private List<Integer> navPoints = new ArrayList<>();
    private List<WifiFingerprintPDF> navPDF = new ArrayList<>();
    private int xMax;
    private double sigmaPos = 0.1;
    private double stepLength = 1;
    private double resampleRandom = 0.000000001;
    private double lastEstimate;
    private String TAG = "ParticleFilter";
    private int pD = 1;
    private double similarity;

    public ParticleFilterFusion(int particles, List<WifiFingerprint> path) {
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
            Particle p2 = new Particle(i* dMax/(numberOfParticles-1), 1/number);
            //Particle p = new Particle(0.0, 1/number);
            listOfParticles.add(p);
            staticParticles.add(p2);
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

    public void sample() {
        // sample
        List<Particle> sampledList = new ArrayList<>();
        for (int i = 0; i < numberOfParticles ; i++) {
            Particle sample = sampleParticle();
            //double x = sample.x + ((rnd.nextDouble()*2) - 1)/10;
            double randX = rnd.nextInt(1000) * 0.01 - 5.0;
            double x = sample.x + randX;
            if (x < 0) {
                x = 0;
            }
            if (x > xMax) {
                x = xMax;
            }
            Particle p = new Particle(x, sample.weight);
            sampledList.add(p);
        }
        listOfParticles.clear();
        listOfParticles = sampledList;
    }

    public double estimatePosition() {
        double pos = 0.0;
        for (Particle p : listOfParticles) {
            pos += p.x * p.weight;
        }
        Log.d(TAG, pos +"");
        return pos;
    }

    public String getBestParticle() {
        double x = 0;
        double w = 0;
        for (Particle p : listOfParticles) {
            if (p.weight > w) {
                w = p.weight;
                x = p.x;
            }
        }
        String best = x + ", " + w;
        return best;
    }

    public List<Particle> getListOfParticles() {
        return listOfParticles;
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
            similarity = ((double)measuredList.size())/ ((double) fp.getWifiMap().size());
            p.weight = calculateWeight(interpolatedList, measuredList);
            Log.d(TAG, pID + ": " + p.x + " - " + p.weight + " - " + similarity);
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

    private double calculateWeight(List<Double> values, List<Double> measuredValues) {
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

}

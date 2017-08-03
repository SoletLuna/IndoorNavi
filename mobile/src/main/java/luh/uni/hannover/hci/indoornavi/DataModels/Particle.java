package luh.uni.hannover.hci.indoornavi.DataModels;

/**
 * Created by solet on 04/05/2017.
 */

public class Particle {

    public double x;
    public double weight;
    public double similarity;

    public Particle(double x, double weight) {
        this.x = x;
        this.weight = weight;
    }

    public String toString() {
        return "Particle(" + x + " ," + weight +")";
    }
}

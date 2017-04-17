package luh.uni.hannover.hci.indoornavi.DataModels;

/**
 * Created by solet on 11/04/2017.
 */

public class Position {

    public double x;
    public double y;
    public double z;

    public Position(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public String toString() {
        return "(" + x + "," + y + "," + z +")";
    }
}

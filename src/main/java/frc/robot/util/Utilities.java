package frc.robot.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;

import edu.wpi.first.wpilibj.Encoder;
import edu.wpi.first.wpilibj.Timer;
import frc.robot.Constants;

public class Utilities {
    public static Timer time = new Timer();
    public static Hashtable<Encoder, ArrayList<Double>> velocityDict = new Hashtable<Encoder, ArrayList<Double>>();

    /**
     * Indexes for the ArrayList values in velocityDict.
     */
    private static enum Data {
        DISTANCE(0),
        TIME(1);

        private final int index;
        private Data(int index) {
            this.index = index;
        }
    }

    /**
     * Returns number of rotations requires to go a certain distance.
     * @param inches Distance to travel in inches.
     * @return The number of rotations needed to travel the specified distance.
     */
    public static double inchesToRotations(double inches) {
        return (inches / (Math.PI * Constants.WHEEL_DIAMETER));
    }

    /**
     * Returns the linear distance traveled of the drivetrain.
     * @param rotations Number of wheel motor rotations
     * @return The linear distance traveled by a wheel motor.
     */
    public static double rotationsToInches(double rotations) {
        return rotations * (Math.PI * Constants.WHEEL_DIAMETER);
    }

    /**
     * Returns linear velocity of an encoder.
     * @param encoder Encoder to get linear velocity from.
     * @return The linear velocity of encoder in inches per second.
     */
    public static double linearVelocity(Encoder encoder) {
        double currTime = time.get();
        double currDist = rotationsToInches(encoder.get() / Constants.THROUGH_BORE_COUNTS_PER_REVOLUTION);
        ArrayList<Double> oldData = velocityDict.getOrDefault(encoder, new ArrayList<Double>(Arrays.asList(0.0, 0.0)));

        double linVelocity = (currDist - oldData.get(Data.DISTANCE.index)) / (currTime - oldData.get(Data.TIME.index));

        oldData.set(Data.DISTANCE.index, currDist);
        oldData.set(Data.TIME.index, currTime);

        velocityDict.putIfAbsent(encoder, oldData);

        return linVelocity;
    }

    private Utilities() {}
}

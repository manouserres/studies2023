package org.team100.lib.system.examples;

import edu.wpi.first.math.Matrix;
import edu.wpi.first.math.VecBuilder;
import edu.wpi.first.math.numbers.N1;
import edu.wpi.first.math.numbers.N2;

/**
 * One-dimensional pendulum with gravity. Angle is measured from horizontal.
 * State includes velocity and position, input is acceleration, output is
 * position.
 */
public class Pendulum1D extends RotaryPlant1D {

    public Pendulum1D(double positionMeasurementStdev, double velocityMeasurementStdev, double positionStateStdev,
            double velocityStateStdev) {
        super(positionMeasurementStdev, velocityMeasurementStdev, positionStateStdev,
                velocityStateStdev);
    }

    /**
     * xdot = f(x,u)
     * pdot = v
     * vdot = u - cos(p)
     * 
     * so vdot itself depends on p but it is still linear in u.
     */
    @Override
    public Matrix<N2, N1> f(Matrix<N2, N1> xmat, Matrix<N1, N1> umat) {
        double p = xmat.get(0, 0);
        double v = xmat.get(1, 0);
        double u = umat.get(0, 0);
        double pdot = v;
        double vdot = u - Math.cos(p);
        return VecBuilder.fill(pdot, vdot);
    }
}

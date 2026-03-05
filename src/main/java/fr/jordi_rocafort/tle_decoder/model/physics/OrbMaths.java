package fr.jordi_rocafort.tle_decoder.model.physics;

public class OrbMaths {
	public static final double MEAN_SOLAR_DAY = 86400.0;
	public static final double MU_EARTH = 3.986004415e14;
	private static final double MEAN_MOTION_CONVERSION_FACTOR = (2.0 * Math.PI) / MEAN_SOLAR_DAY;

	// region Static Phase

	/**
	 * @param meanMotion in revolutions per day
	 * @return meanMotion in radians per second
	 */
	static double convertMeanMotion(double meanMotion) {
		return meanMotion * MEAN_MOTION_CONVERSION_FACTOR;
	}

	/**
	 * Finds the orbital period by using the Mean Motion (rev / day)
	 *
	 * @param meanMotion in revolutions per day
	 * @return orbital period in seconds
	 */
	static double orbPeriod(double meanMotion) {
		return MEAN_SOLAR_DAY / meanMotion;
	}

	/**
	 * Finds the semi major axis using the mean motion in radians per second
	 *
	 * @param meanMotion in radians per second
	 * @return semi major axis in meters
	 */
	static double semiMajorAxis(double meanMotion) {
		return Math.cbrt(MU_EARTH / (meanMotion * meanMotion));
	}

	// endregion

	// region Dynamic Phase

	double EccentricAnomaly(double MA, double e) {
		double E_Approx = MA + e * Math.sin(MA);
		double E = Algos.NewtonRaphson(
			MA, e, Kepler::KeplerEquation, Kepler::KeplerPrime, E_Approx, 1E-6, 1000
		);

		return E;
	}

	double TrueAnomaly(double E, double e) {
		double beta = e / (1.0 + Math.sqrt(1.0 - e * e));
		double nu = E + 2.0 * Math.atan(
				beta * Math.sin(E) /
						(1 - beta * Math.cos(E)));

		return nu;
	}

	double AltFromTA(double a, double e, double nu) {
		return a * (1 - e * e) / (1.0 + e * Math.cos(nu));
	}

	double orbSpeed(double a, double r) {
		return Math.sqrt(MU_EARTH * ((2.0 / r) - (1.0 / a)));
	}

	// endregion
}

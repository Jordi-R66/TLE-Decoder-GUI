package fr.jordi_rocafort.tle_decoder.model.physics;

import fr.jordi_rocafort.tle_decoder.util.Algos;

public class OrbMaths {

	// region Static Phase

	/**
	 * @param meanMotion in revolutions per day
	 * @return meanMotion in radians per second
	 */
	static double convertMeanMotion(double meanMotion) {
		return meanMotion * Constants.MEAN_MOTION_CONVERSION_FACTOR;
	}

	/**
	 * Finds the orbital period by using the Mean Motion (rev / day)
	 *
	 * @param meanMotion in revolutions per day
	 * @return orbital period in seconds
	 */
	static double orbPeriod(double meanMotion) {
		return Constants.MEAN_SOLAR_DAY / meanMotion;
	}

	/**
	 * Finds the semi major axis using the mean motion in radians per second
	 *
	 * @param meanMotion in radians per second
	 * @return semi major axis in meters
	 */
	static double semiMajorAxis(double meanMotion) {
		return Math.cbrt(Constants.MU_EARTH / (meanMotion * meanMotion));
	}

	// endregion

	// region Dynamic Phase

	static double EccentricAnomaly(double MA, double e) {
		double E_Approx = MA + e * Math.sin(MA);
		double E = Algos.NewtonRaphson(
			MA, e, Kepler::KeplerEquation, Kepler::KeplerPrime, E_Approx, 1E-6, 1000
		);

		return E;
	}

	static double TrueAnomaly(double E, double e) {
		double beta = e / (1.0 + Math.sqrt(1.0 - e * e));
		double nu = E + 2.0 * Math.atan(
				beta * Math.sin(E) /
						(1 - beta * Math.cos(E)));

		return nu;
	}

	static double AltFromTA(double a, double e, double nu) {
		return a * (1 - e * e) / (1.0 + e * Math.cos(nu));
	}

	static double orbSpeed(double a, double r) {
		return Math.sqrt(Constants.MU_EARTH * ((2.0 / r) - (1.0 / a)));
	}

	// endregion
}

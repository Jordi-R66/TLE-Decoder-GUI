package fr.jordi_rocafort.tle_decoder.model.physics;

public class Kepler {

	public static double KeplerEquation(double eccentricAnomaly, double eccentricity) {
		return eccentricAnomaly - eccentricity * Math.sin(eccentricAnomaly);
	}

	public static double KeplerPrime(double eccentricAnomaly, double eccentricity) {
		return 1.0 - eccentricity * Math.cos(eccentricAnomaly);
	}
}
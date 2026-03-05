package fr.jordi_rocafort.tle_decoder.model.physics;

public class Constants {
	public static final double DEGS2RADS = Math.PI / 180.0;
	public static final double RADS2DEGS = 180.0 / Math.PI;

	public static final double J2_EARTH = 0.00108262668; // Constante d'aplatissement
	public static final double WGS84_A = 6378137.0; // Rayon équatorial de la Terre (mètres)
	public static final double WGS84_B = 6356752.314245; // Rayon polaire de la Terre (mètres)
}
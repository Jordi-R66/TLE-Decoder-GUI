package fr.jordi_rocafort.tle_decoder.model.physics;

public class Constants {
	public static final double MEAN_SOLAR_DAY = 86400.0;
	public static final double MU_EARTH = 3.986004415e14;
	public static final double MEAN_MOTION_CONVERSION_FACTOR = (2.0 * Math.PI) / MEAN_SOLAR_DAY;
	public static final double DEGS2RADS = Math.PI / 180.0;
	public static final double RADS2DEGS = 180.0 / Math.PI;

	public static final double J2_EARTH = 0.0010826359; // Constante d'aplatissement
	public static final double WGS84_A = 6378137.0; // Rayon équatorial de la Terre (mètres)
	public static final double WGS84_B = 6356752.314245; // Rayon polaire de la Terre (mètres)
}
package fr.jordi_rocafort.keplertrack.model.physics;

import java.util.ArrayList;
import java.util.List;

import fr.jordi_rocafort.keplertrack.model.data.Coords3D;
import fr.jordi_rocafort.keplertrack.model.data.GeoCoords;

public class GeographyPhysics {

	/**
	 * Convertit des coordonnées ECI en coordonnées géographiques Terrestres
	 * (WGS84).
	 */
	public static GeoCoords getGeoCoords(Coords3D eciCoords, long timestamp) {
		// 1. Calcul du GMST (Greenwich Mean Sidereal Time)
		// Conversion du timestamp UNIX en Jours Juliens depuis l'époque J2000.0
		double daysSinceJ2000 = (timestamp / 86400.0) + 2440587.5 - 2451545.0;

		// Formule de l'USNO pour le GMST en degrés (avec sécurisation modulo positif)
		double gmstDeg = (280.46061837 + 360.98564736629 * daysSinceJ2000) % 360.0;
		gmstDeg = (gmstDeg + 360.0) % 360.0;
		double gmstRad = gmstDeg * Constants.DEGS2RADS;

		// 2. LONGITUDE
		// Ascension droite du satellite dans le plan équatorial ECI
		double ra = Math.atan2(eciCoords.y(), eciCoords.x());

		// Longitude = Ascension Droite - Rotation de la Terre (GMST)
		double lon = ra - gmstRad;

		// Normalisation de la longitude entre -PI et +PI (-180° à +180°)
		double twoPi = 2.0 * Math.PI;
		lon = ((lon + Math.PI) % twoPi + twoPi) % twoPi;
		lon -= Math.PI;

		double longitudeDeg = lon * Constants.RADS2DEGS;

		// 3. LATITUDE GÉODÉSIQUE (Méthode de Bowring itérative pour WGS84)
		double p = Math.sqrt((eciCoords.x() * eciCoords.x()) + (eciCoords.y() * eciCoords.y()));

		// e² = 1 - (b²/a²) pour l'ellipsoïde WGS84
		double e2 = 1.0 - ((Constants.WGS84_B * Constants.WGS84_B) / (Constants.WGS84_A * Constants.WGS84_A));

		double lat = Math.atan2(eciCoords.z(), p * (1.0 - e2));
		double n = 0.0;

		for (int i = 0; i < 5; i++) {
			n = Constants.WGS84_A / Math.sqrt(1.0 - e2 * Math.sin(lat) * Math.sin(lat));
			lat = Math.atan2(eciCoords.z() + n * e2 * Math.sin(lat), p);
		}

		double latitudeDeg = lat * Constants.RADS2DEGS;

		// 4. ALTITUDE GÉODÉSIQUE WGS84
		double altitude = (p / Math.cos(lat)) - n;

		return new GeoCoords(latitudeDeg, longitudeDeg, altitude);
	}

	public static List<GeoCoords> computeFootprint(GeoCoords satCoords, int numPoints) {
		List<GeoCoords> footprint = new ArrayList<>();

		double latRad = satCoords.lat() * Constants.DEGS2RADS;
		double lonRad = satCoords.lng() * Constants.DEGS2RADS;
		double alt = satCoords.altitude();

		// 1. Rayon géocentrique local (prise en compte de l'ellipsoïde WGS84)
		double a = Constants.WGS84_A;
		double b = Constants.WGS84_B;
		double cosLat = Math.cos(latRad);
		double sinLat = Math.sin(latRad);

		// Formule exacte du rayon depuis le centre de la Terre jusqu'à la surface pour
		// une latitude donnée
		double num = Math.pow(a * a * cosLat, 2) + Math.pow(b * b * sinLat, 2);
		double den = Math.pow(a * cosLat, 2) + Math.pow(b * sinLat, 2);
		double rLocal = Math.sqrt(num / den);

		// 2. Demi-angle au centre (Earth Central Angle) pour une élévation de 0°
		// C'est l'angle entre le centre de la Terre, le satellite et l'horizon.
		double alpha = Math.acos(rLocal / (rLocal + alt));

		// 3. Génération des points du cercle de visibilité
		for (int i = 0; i <= numPoints; i++) {
			// Balayage azimutal de 0 à 360 degrés
			double azimuth = 2.0 * Math.PI * i / numPoints;

			// Trigonométrie pour trouver les coordonnées du point d'horizon
			double ptLatRad = Math.asin(Math.sin(latRad) * Math.cos(alpha) +
					Math.cos(latRad) * Math.sin(alpha) * Math.cos(azimuth));

			double ptLonRad = lonRad + Math.atan2(Math.sin(azimuth) * Math.sin(alpha) * Math.cos(latRad),
					Math.cos(alpha) - Math.sin(latRad) * Math.sin(ptLatRad));

			// Normalisation de la longitude pour qu'elle reste entre -180° et +180° (-PI à
			// PI)
			ptLonRad = ((ptLonRad + Math.PI) % (2.0 * Math.PI) + (2.0 * Math.PI)) % (2.0 * Math.PI) - Math.PI;

			footprint.add(new GeoCoords(ptLatRad * Constants.RADS2DEGS, ptLonRad * Constants.RADS2DEGS, 0));
		}

		return footprint;
	}
}
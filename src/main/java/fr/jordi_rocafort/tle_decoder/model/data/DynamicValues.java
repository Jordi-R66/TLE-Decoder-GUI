package fr.jordi_rocafort.tle_decoder.model.data;

public record DynamicValues(
		long deltaTime, // Temps écoulé depuis l'époque (secondes)
		double trueAno, // Anomalie vraie (rad)
		double meanAno, // Anomalie moyenne (rad)
		double eccAno, // Anomalie excentrique (rad)
		double distanceToFocal, // Distance au centre de la Terre (m)
		double speed, // Vitesse instantanée (m/s)

		Coords2D coords2d, // Coordonnées dans le plan orbital
		Coords3D coords3d, // Coordonnées géocentriques équatoriales (ECI)
		GeoCoords geoCoords // Coordonnées géographiques (Lat/Lon/Alt)
) {
}
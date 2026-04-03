package fr.jordi_rocafort.keplertrack.model.data;

public record StaticValues(
		double n, // Mouvement moyen (rad/s)
		double a, // Demi-grand axe (m)
		double T, // Période orbitale (s)
		double M, // Anomalie moyenne à l'époque (rad)
		double p, // Demi-latus rectum (m)

		double apoAlt, // Altitude à l'apogée (m)
		double periAlt, // Altitude au périgée (m)
		double epochAlt, // Altitude à l'époque (m)

		double apoSpd, // Vitesse à l'apogée (m/s)
		double periSpd, // Vitesse au périgée (m/s)
		double epochSpd, // Vitesse à l'époque (m/s)

		long epochTimestamp // Instant de référence de l'orbite (Unix timestamp)
) {
}
package fr.jordi_rocafort.keplertrack.model.data;

public record TopocentricCoords(
	double azimuth,		// Degrés (0° = Nord, 90° = Est)
	double elevation,	// Degrés (> 0 = au-dessus de l'horizon)
	double range		// Kilomètres (distance entre l'observateur et le satellite)
) {}

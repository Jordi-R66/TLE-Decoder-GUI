package fr.jordi_rocafort.keplertrack.model.data;

public record PassPoint(
	long timestamp,
	TopocentricCoords coords
) {}

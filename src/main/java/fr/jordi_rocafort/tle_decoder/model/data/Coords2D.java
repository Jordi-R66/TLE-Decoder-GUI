package fr.jordi_rocafort.tle_decoder.model.data;

public record Coords2D(double x, double y) {
	public double getDistanceToOrigin() {
		return Math.sqrt(x * x + y * y);
	}
}

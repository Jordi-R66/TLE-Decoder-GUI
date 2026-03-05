package fr.jordi_rocafort.tle_decoder.model.data;

public record Coords3D(double x, double y, double z) {
	public double getDistanceToOrigin() {
		return Math.sqrt(x * x + y * y + z * z);
	}
}

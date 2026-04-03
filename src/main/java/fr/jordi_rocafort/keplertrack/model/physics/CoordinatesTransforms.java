package fr.jordi_rocafort.keplertrack.model.physics;

import fr.jordi_rocafort.keplertrack.model.data.Coords2D;
import fr.jordi_rocafort.keplertrack.model.data.Coords3D;
import fr.jordi_rocafort.keplertrack.model.data.TLE;

public class CoordinatesTransforms {

	public static Coords2D getPlaneCoords(double nu, double r) {
		return new Coords2D(r * Math.cos(nu), r * Math.sin(nu));
	}

	public static Coords3D getECICoords(Coords2D planeCoords, double inclination, double ascNodeLong, double periArg) {
		double an = ascNodeLong * Constants.DEGS2RADS;
		double arg = periArg * Constants.DEGS2RADS;
		double inc = inclination * Constants.DEGS2RADS;

		double cosAn = Math.cos(an);
		double sinAn = Math.sin(an);
		double cosArg = Math.cos(arg);
		double sinArg = Math.sin(arg);
		double cosInc = Math.cos(inc);
		double sinInc = Math.sin(inc);

		double x = planeCoords.x() * (cosAn * cosArg - sinAn * sinArg * cosInc)
				+ planeCoords.y() * (-cosAn * sinArg - sinAn * cosArg * cosInc);

		double y = planeCoords.x() * (sinAn * cosArg + cosAn * sinArg * cosInc)
				+ planeCoords.y() * (-sinAn * sinArg + cosAn * cosArg * cosInc);

		double z = planeCoords.x() * (sinArg * sinInc)
				+ planeCoords.y() * (cosArg * sinInc);

		return new Coords3D(x, y, z);
	}

	public static double getWGS84Altitude(Coords3D coords) {
		double r = coords.getDistanceToOrigin();

		if (r == 0.0)
			return 0.0; // Sécurité division par zéro

		double a = Constants.WGS84_A;
		double b = Constants.WGS84_B;

		double xySqr = (coords.x() * coords.x()) + (coords.y() * coords.y());
		double zSqr = (coords.z() * coords.z());

		double cos2Phi = xySqr / (r * r);
		double sin2Phi = zSqr / (r * r);

		double a2 = a * a;
		double b2 = b * b;

		double num = (a2 * a2 * cos2Phi) + (b2 * b2 * sin2Phi);
		double den = (a2 * cos2Phi) + (b2 * sin2Phi);

		double earthRadius = Math.sqrt(num / den);

		return r - earthRadius;
	}

	public static double getWGS84AltitudeFromTA(double nu, double a, TLE tle) {
		double r = OrbMaths.AltFromTA(a, tle.eccentricity(), nu);
		Coords2D planeCoords = getPlaneCoords(nu, r);

		// En phase statique, on utilise les angles non-perturbés du TLE original
		Coords3D eciCoords = getECICoords(planeCoords, tle.inclination(), tle.rightAscension(),
				tle.argumentOfPerigee());

		return getWGS84Altitude(eciCoords);
	}

}

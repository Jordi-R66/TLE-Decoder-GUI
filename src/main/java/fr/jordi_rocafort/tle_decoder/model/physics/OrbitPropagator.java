package fr.jordi_rocafort.tle_decoder.model.physics;

import fr.jordi_rocafort.tle_decoder.model.data.*;
import fr.jordi_rocafort.tle_decoder.util.TimeUtils;

public class OrbitPropagator {
	// region STATIC PHASE

	public static StaticValues computeStaticPhase(TLE tle) {
		StaticValues output = null;

		double n, a, T, M, p, E, epochNu, apoR, periR, epochR, apoAlt, periAlt, epochAlt, apoSpd, periSpd, epochSpd;
		long epochTimestamp;

		n = OrbMaths.convertMeanMotion(tle.meanMotion());
		a = OrbMaths.semiMajorAxis(n);
		T = OrbMaths.orbPeriod(tle.meanMotion());
		M = tle.meanAnomaly() * Constants.DEGS2RADS;
		p = a * (1.0 - tle.eccentricity() * tle.eccentricity());

		E = OrbMaths.EccentricAnomaly(M, tle.eccentricity());
		epochNu = OrbMaths.TrueAnomaly(E, tle.eccentricity());

		apoR = OrbMaths.AltFromTA(a, tle.eccentricity(), Math.PI);
		periR = OrbMaths.AltFromTA(a, tle.eccentricity(), 0.0);
		epochR = OrbMaths.AltFromTA(a, tle.eccentricity(), epochNu);

		apoAlt = CoordinatesTransforms.getWGS84AltitudeFromTA(Math.PI, a, tle);
		periAlt = CoordinatesTransforms.getWGS84AltitudeFromTA(0.0, a, tle);
		epochAlt = CoordinatesTransforms.getWGS84AltitudeFromTA(epochNu, a, tle);

		apoSpd = OrbMaths.orbSpeed(a, apoR);
		periSpd = OrbMaths.orbSpeed(a, periR);
		epochSpd = OrbMaths.orbSpeed(a, epochR);

		epochTimestamp = TimeUtils.getEpochTimestampFromTLE(tle);

		output = new StaticValues(n, a, T, M, p, apoAlt, periAlt, epochAlt, apoSpd, periSpd, epochSpd,
				epochTimestamp);

		return output;
	}

	// endregion

	// region STATIC PHASE

	public static DynamicValues computeDynamicPhase(TLE tle, StaticValues init, long currentTimestamp) {
		DynamicValues output = null;

		long deltaTime = currentTimestamp - init.epochTimestamp();

		// 1. Perturbations séculaires J2 (Rotation du plan orbital dû à l'aplatissement
		// de la Terre)
		double iRad = tle.inclination() * Constants.DEGS2RADS;
		double j2Factor = 1.5 * Constants.J2_EARTH * Math.pow(Constants.WGS84_A / init.p(), 2.0) * init.n();

		double dotOmegaNode = -j2Factor * Math.cos(iRad);
		double dotOmegaPeri = j2Factor * 0.5 * (5.0 * Math.pow(Math.cos(iRad), 2.0) - 1.0);

		double currentAscNodeLong = (tle.rightAscension() + (dotOmegaNode * deltaTime) * Constants.RADS2DEGS);
		currentAscNodeLong = ((currentAscNodeLong % 360.0) + 360.0) % 360.0;

		double currentPeriArg = (tle.argumentOfPerigee() + (dotOmegaPeri * deltaTime) * Constants.RADS2DEGS);
		currentPeriArg = ((currentPeriArg % 360.0) + 360.0) % 360.0;

		// 2. Traînée atmosphérique (Dégradation de l'orbite)
		double ndotRads2 = (tle.firstDerivMeanMotion() * 2.0 * Math.PI) / (86400.0 * 86400.0);
		double currentN = init.n() + (ndotRads2 * deltaTime);
		double currentA = Math.cbrt(Constants.MU_EARTH / (currentN * currentN));

		// 3. Propagation de l'Anomalie Moyenne
		double deltaM = (init.n() * deltaTime) + (0.5 * ndotRads2 * deltaTime * deltaTime);
		double currentM = ((deltaM + init.M()) % (2.0 * Math.PI) + (2.0 * Math.PI)) % (2.0 * Math.PI);

		// 4. Calculs des anomalies et positions
		double E = OrbMaths.EccentricAnomaly(currentM, tle.eccentricity());
		double trueAno = OrbMaths.TrueAnomaly(E, tle.eccentricity());
		double r = OrbMaths.AltFromTA(currentA, tle.eccentricity(), trueAno);
		double spd = OrbMaths.orbSpeed(currentA, r);

		// 5. Appels aux transformées de coordonnées (Que nous traduirons juste après)
		Coords2D coords2d = CoordinatesTransforms.getPlaneCoords(trueAno, r);

		// Au lieu de modifier le TLE original, on passe les paramètres perturbés
		Coords3D coords3d = CoordinatesTransforms.getECICoords(coords2d, tle.inclination(), currentAscNodeLong,
				currentPeriArg);
		GeoCoords geoCoords = GeographyPhysics.getGeoCoords(coords3d, currentTimestamp);

		output = new DynamicValues(
				deltaTime, trueAno, currentM, E, r, spd,
				coords2d, coords3d, geoCoords);

		return output;
	}

	// endregion
}

package fr.jordi_rocafort.keplertrack.model.physics;

import fr.jordi_rocafort.keplertrack.model.data.*;
import java.util.ArrayList;
import java.util.List;

public class PassPredictor {
	private static final boolean ASCENDING = true;
	private static final boolean DESCENDING = false;
	private static final long GEO_TIMEOUT_MILLIS = 3L * 3600L; // 3 heures max

	public static List<SatellitePass> predictPasses(TLE tle, GeoCoords station, long startSecs, long endSecs,
			long stepSecs, double minElevation) {

		List<SatellitePass> passes = new ArrayList<>();

		if (tle.meanMotion() < 1.1) {
			return passes;
		}

		StaticValues staticVals = OrbitPropagator.computeStaticPhase(tle);

		boolean inPass = false;
		long aosTime = 0;
		long tcaTime = 0;
		double maxElev = 0.0;
		ArrayList<PassPoint> currentTrajectory = new ArrayList<>();

		for (long t = startSecs; t <= endSecs; t += stepSecs) {
			DynamicValues dynVals = OrbitPropagator.computeDynamicPhase(tle, staticVals, t);
			Coords3D satEci = dynVals.coords3d();

			TopocentricCoords aer = CoordinatesTransforms.calculateAER(station, satEci, t);

			if (!inPass) {
				if (aer.elevation() >= minElevation) {
					inPass = true;
					aosTime = refineBoundary(tle, staticVals, station, t - stepSecs, t, minElevation, ASCENDING);

					maxElev = aer.elevation();
					tcaTime = t;
					currentTrajectory = new ArrayList<>();
					currentTrajectory.add(new PassPoint(t, aer));
				}
			} else {
				currentTrajectory.add(new PassPoint(t, aer));

				if (aer.elevation() > maxElev) {
					maxElev = aer.elevation();
					tcaTime = t;
				} else if (aer.elevation() < minElevation) {
					inPass = false;
					long losTime = refineBoundary(tle, staticVals, station, t - stepSecs, t, minElevation, DESCENDING);

					long exactTcaTime = refineTCA(tle, staticVals, station, tcaTime, stepSecs);

					DynamicValues peakDyn = OrbitPropagator.computeDynamicPhase(tle, staticVals, exactTcaTime);
					double exactMaxElev = CoordinatesTransforms.calculateAER(station, peakDyn.coords3d(), exactTcaTime)
							.elevation();

					passes.add(new SatellitePass(aosTime, exactTcaTime, losTime, exactMaxElev, currentTrajectory));
				}

				if (t - aosTime > GEO_TIMEOUT_MILLIS) {
					inPass = false;
				}
			}
		}

		return passes;
	}

	private static long refineBoundary(TLE tle, StaticValues staticVals, GeoCoords station, long t1, long t2,
			double targetElevation, boolean isAscending) {
		while ((t2 - t1) > 1) {
			long tMid = t1 + (t2 - t1) / 2;

			DynamicValues dynVals = OrbitPropagator.computeDynamicPhase(tle, staticVals, tMid);
			double elevMid = CoordinatesTransforms.calculateAER(station, dynVals.coords3d(), tMid).elevation();

			boolean isTargetPassed = (isAscending && (elevMid > targetElevation))
					|| (!isAscending && (elevMid < targetElevation));

			if (isTargetPassed) {
				t2 = tMid;
			} else {
				t1 = tMid;
			}
		}

		return t1 + (t2 - t1) / 2;
	}

	private static long refineTCA(TLE tle, StaticValues staticVals, GeoCoords station, long coarseTca, long stepSecs) {
		long bestTime = coarseTca;
		double absoluteMaxElev = -90.0;

		// On scanne de [TCA grossier - 60s] à [TCA grossier + 60s] par pas de 1 seconde
		for (long t = coarseTca - stepSecs; t <= coarseTca + stepSecs; t++) {
			DynamicValues dynVals = OrbitPropagator.computeDynamicPhase(tle, staticVals, t);
			double currentElev = CoordinatesTransforms.calculateAER(station, dynVals.coords3d(), t).elevation();

			if (currentElev > absoluteMaxElev) {
				absoluteMaxElev = currentElev;
				bestTime = t;
			}
		}
		return bestTime;
	}
}
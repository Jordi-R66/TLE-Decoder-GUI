package fr.jordi_rocafort.keplertrack.model.physics;

import fr.jordi_rocafort.keplertrack.model.data.*;
import java.util.ArrayList;

public class PassPredictor {
	private static final boolean ASCENDING = true;
	private static final boolean DESCENDING = false;
	private static final long MAX_PASS_DURATION_SECS = 24L * 3600L;

	public static ArrayList<SatellitePass> predictPasses(TLE tle, GeoCoords station, long startSecs, long endSecs,
			long stepSecs, double minElevation) {
		ArrayList<SatellitePass> passes = new ArrayList<>();
		StaticValues staticVals = OrbitPropagator.computeStaticPhase(tle);

		boolean inPass = false;
		long aosTime = 0;
		long tcaTime = 0;
		double maxElevation = -90.0;

		long actualStartSecs = startSecs;

		DynamicValues initialDyn = OrbitPropagator.computeDynamicPhase(tle, staticVals, startSecs);
		double initialElev = CoordinatesTransforms.calculateAER(station, initialDyn.coords3d(), startSecs).elevation();

		if (initialElev >= minElevation) {
			long backT = startSecs;

			long maxBacktrack = startSecs - (24L * 3600L);

			while (backT > maxBacktrack) {
				backT -= stepSecs;
				DynamicValues backDyn = OrbitPropagator.computeDynamicPhase(tle, staticVals, backT);
				double backElev = CoordinatesTransforms.calculateAER(station, backDyn.coords3d(), backT).elevation();

				if (backElev < minElevation) {
					actualStartSecs = backT;
					break;
				}
			}
		}

		for (long t = actualStartSecs; t <= endSecs; t += stepSecs) {
			DynamicValues dynVals = OrbitPropagator.computeDynamicPhase(tle, staticVals, t);
			TopocentricCoords aer = CoordinatesTransforms.calculateAER(station, dynVals.coords3d(), t);

			if (!inPass && aer.elevation() >= minElevation) {
				inPass = true;
				aosTime = refineBoundary(tle, staticVals, station, t - stepSecs, t, minElevation, ASCENDING);
				maxElevation = aer.elevation();
				tcaTime = t;

			} else if (inPass) {
				if (aer.elevation() > maxElevation) {
					maxElevation = aer.elevation();
					tcaTime = t;
				}

				if (aer.elevation() < minElevation) {
					inPass = false;
					long losTime = refineBoundary(tle, staticVals, station, t - stepSecs, t, minElevation, DESCENDING);
					long exactTcaTime = refineTCA(tle, staticVals, station, tcaTime, stepSecs);

					DynamicValues peakDyn = OrbitPropagator.computeDynamicPhase(tle, staticVals, exactTcaTime);
					double exactMaxElev = CoordinatesTransforms.calculateAER(station, peakDyn.coords3d(), exactTcaTime)
							.elevation();

					ArrayList<PassPoint> smoothTrajectory = generateSmoothTrajectory(tle, staticVals, station, aosTime,
							losTime, 2L);
					passes.add(new SatellitePass(aosTime, exactTcaTime, losTime, exactMaxElev, smoothTrajectory));
				}

				if (t - aosTime > MAX_PASS_DURATION_SECS) {
					inPass = false;
				}
			}
		}

		return passes;
	}

	private static ArrayList<PassPoint> generateSmoothTrajectory(TLE tle, StaticValues staticVals, GeoCoords station,
			long aos, long los, long stepSecs) {
		ArrayList<PassPoint> smoothPath = new ArrayList<>();

		for (long t = aos; t <= los; t += stepSecs) {
			DynamicValues dynVals = OrbitPropagator.computeDynamicPhase(tle, staticVals, t);
			TopocentricCoords aer = CoordinatesTransforms.calculateAER(station, dynVals.coords3d(), t);
			smoothPath.add(new PassPoint(t, aer));
		}

		DynamicValues lastDyn = OrbitPropagator.computeDynamicPhase(tle, staticVals, los);
		TopocentricCoords lastAer = CoordinatesTransforms.calculateAER(station, lastDyn.coords3d(), los);
		smoothPath.add(new PassPoint(los, lastAer));

		return smoothPath;
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
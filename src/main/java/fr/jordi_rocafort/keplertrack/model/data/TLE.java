package fr.jordi_rocafort.keplertrack.model.data;

public record TLE(
		String name,
		int noradId,
		char classification,

		int cosparYear,
		int cosparLaunchNum,
		String cosparPiece,

		DateAndTime epoch,

		double firstDerivMeanMotion,
		double secondDerivMeanMotion,
		double bStar,

		double inclination,
		double rightAscension,
		double eccentricity,
		double argumentOfPerigee,
		double meanAnomaly,
		double meanMotion,

		int revolutionNumber
	) {

	public int epochYear() { return epoch.year(); }
	public double epochDay() { return epoch.day(); }
}

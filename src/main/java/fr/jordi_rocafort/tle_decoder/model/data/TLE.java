package fr.jordi_rocafort.tle_decoder.model.data;

public record TLE(
		String name,
		int noradId,
		char classification,

		int cosparYear,
		int cosparLaunchNum,
		String cosparPiece,

		int epochYear,
		double epochDay,

		double firstDerivMeanMotion,
		double secondDerivMeanMotion,
		double bStar,

		double inclination,
		double rightAscension,
		double eccentrivity,
		double argumentOfPerigee,
		double meanAnomaly,
		double meanMotion,

		int revolutionNumber
	) {
}

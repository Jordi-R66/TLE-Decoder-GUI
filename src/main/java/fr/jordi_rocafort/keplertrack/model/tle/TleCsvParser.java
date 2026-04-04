package fr.jordi_rocafort.keplertrack.model.tle;

import fr.jordi_rocafort.keplertrack.model.data.DateAndTime;
import fr.jordi_rocafort.keplertrack.model.data.TLE;
import fr.jordi_rocafort.keplertrack.model.data.TleCsvBlock;
import fr.jordi_rocafort.keplertrack.util.TimeUtils;

public class TleCsvParser implements ITleParser<TleCsvBlock> {
	/**
	 * Parse un bloc de 3 lignes de texte pour générer un objet TLE immuable.
	 */
	public TLE parseLines(TleCsvBlock block) {
		String line = block.line();
		String[] fields = line.split(",");

		String name = fields[0];
		String cosparWhole = fields[1];

		String[] splitCospar = cosparWhole.split("-");
		String cosparYrStr = splitCospar[0], cosparLaunchNumStr = splitCospar[1].substring(0, 2),
				cosparPiece = splitCospar[1].substring(3);

		// --------------------------- 1-ST LINE PARSING ---------------------------

		int noradId = Integer.parseInt(fields[11]);
		char classification = fields[10].charAt(0);

		int cosparYear = Integer.parseInt(cosparYrStr);
		int cosparLaunchNum = Integer.parseInt(cosparLaunchNumStr);

		DateAndTime epoch = TimeUtils.isoTimeToObject(fields[2]);

		// Multiplié par 2.0 comme dans le code C
		double firstDeriv = Double.parseDouble(fields[15]) * 2.0;

		// Format scientifique spécifique aux TLE (multiplié par 6.0 pour la dérivée
		// seconde)
		double secondDeriv = Double.parseDouble(fields[16]) * 6.0;
		double bStar = Double.parseDouble(fields[14]);

		// --------------------------- 2-ND LINE PARSING ---------------------------

		double inclination = Double.parseDouble(fields[5]);

		// Application des conditions ternaires du C (si inclinaison != 0)
		double ascNodeLong = (inclination != 0) ? Double.parseDouble(fields[6]) : 0.0;

		// L'excentricité n'a pas de point décimal dans le TLE (Remplace le ECC[0] =
		// '.')
		double eccentricity = Double.parseDouble(fields[4]);

		double periArg = (inclination != 0) ? Double.parseDouble(fields[7]) : 0.0;
		double meanAnomaly = Double.parseDouble(fields[8]);
		double meanMotion = Double.parseDouble(fields[3]);

		int revolutions = Integer.parseInt(fields[13]);

		// Création et retour du Record
		return new TLE(
				name,
				noradId,
				classification,
				cosparYear,
				cosparLaunchNum,
				cosparPiece,
				epoch,
				firstDeriv,
				secondDeriv,
				bStar,
				inclination,
				ascNodeLong,
				eccentricity,
				periArg,
				meanAnomaly,
				meanMotion,
				revolutions);
	}
}

package fr.jordi_rocafort.keplertrack.model.tle;

import fr.jordi_rocafort.keplertrack.model.data.DateAndTime;
import fr.jordi_rocafort.keplertrack.model.data.TLE;
import fr.jordi_rocafort.keplertrack.model.data.TleLegacyBlock;

public class TleLegacyParser implements ITleParser<TleLegacyBlock> {
	/**
	 * Parse un bloc de 3 lignes de texte pour générer un objet TLE immuable.
	 */
	public TLE parseLines(TleLegacyBlock block) {
		String nameLine = block.firstLine().trim();
		String line1 = block.secondLine();
		String line2 = block.thirdLine();

		// --------------------------- 1-ST LINE PARSING ---------------------------

		int noradIdLine1 = Integer.parseInt(line1.substring(2, 7).trim());
		char classification = line1.charAt(7);

		int cosparYear = Integer.parseInt(line1.substring(9, 11).trim());
		int cosparLaunchNum = Integer.parseInt(line1.substring(11, 14).trim());
		String cosparPiece = line1.substring(14, 17).trim();

		int epochYear = Integer.parseInt(line1.substring(18, 20).trim());
		double epochDay = Double.parseDouble(line1.substring(20, 32).trim());

		DateAndTime epoch = new DateAndTime(epochYear, epochDay);

		// Multiplié par 2.0 comme dans le code C
		double firstDeriv = Double.parseDouble(line1.substring(33, 43).trim()) * 2.0;

		// Format scientifique spécifique aux TLE (multiplié par 6.0 pour la dérivée
		// seconde)
		double secondDeriv = parseTleScientificNotation(line1.substring(44, 52)) * 6.0;
		double bStar = parseTleScientificNotation(line1.substring(53, 61));

		// --------------------------- 2-ND LINE PARSING ---------------------------

		int noradIdLine2 = Integer.parseInt(line2.substring(2, 7).trim());

		// Vérification sécurisée (Remplace le exit(0) qui ferait crasher l'interface)
		if (noradIdLine1 != noradIdLine2) {
			throw new IllegalArgumentException("Erreur de parsing : Le NORAD ID ne correspond pas entre la ligne 1 ("
					+ noradIdLine1 + ") et la ligne 2 (" + noradIdLine2 + ")");
		}

		double inclination = Double.parseDouble(line2.substring(8, 16).trim());

		// Application des conditions ternaires du C (si inclinaison != 0)
		double ascNodeLong = (inclination != 0) ? Double.parseDouble(line2.substring(17, 25).trim()) : 0.0;

		// L'excentricité n'a pas de point décimal dans le TLE (Remplace le ECC[0] =
		// '.')
		double eccentricity = Double.parseDouble("0." + line2.substring(26, 33).trim());

		double periArg = (inclination != 0) ? Double.parseDouble(line2.substring(34, 42).trim()) : 0.0;
		double meanAnomaly = Double.parseDouble(line2.substring(43, 51).trim());
		double meanMotion = Double.parseDouble(line2.substring(52, 63).trim());

		int revolutions = Integer.parseInt(line2.substring(63, 68).trim());

		// Création et retour du Record
		return new TLE(
				nameLine,
				noradIdLine1,
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

	/**
	 * Remplace la fonction C "strtoscinotd"
	 * Convertit le format des TLE (ex: " 11606-4" devient 0.11606 * 10^-4)
	 */
	private static double parseTleScientificNotation(String str) {
		str = str.trim();
		if (str.isEmpty() || str.equals("00000-0") || str.equals("00000+0")) {
			return 0.0;
		}

		// Le dernier signe (+ ou -) indique l'exposant
		int signIndex = Math.max(str.lastIndexOf('+'), str.lastIndexOf('-'));

		if (signIndex <= 0) { // S'il n'y a pas d'exposant
			return Double.parseDouble(str);
		}

		String mantissaStr = str.substring(0, signIndex).trim();
		String exponentStr = str.substring(signIndex).trim();

		double mantissa = Double.parseDouble(mantissaStr);
		double exponent = Double.parseDouble(exponentStr);

		// On divise la mantisse pour recréer la virgule implicite devant les chiffres
		int digits = mantissaStr.replace("-", "").replace("+", "").length();
		mantissa = mantissa / Math.pow(10, digits);

		return mantissa * Math.pow(10, exponent);
	}
}

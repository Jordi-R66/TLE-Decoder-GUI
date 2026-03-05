package fr.jordi_rocafort.tle_decoder.model.physics;

import java.util.function.DoubleBinaryOperator;

public class Algos {

	/**
	 * Algorithme de calcul du Checksum d'une ligne TLE.
	 * 
	 * @param line    La ligne sous forme de String
	 * @param modulus Le modulo à appliquer (souvent 10 pour un TLE)
	 * @return Le checksum calculé, ou -1 si la ligne est invalide
	 */
	public static int checksumAlgorithm(String line, int modulus) {
		if (line == null || line.length() < 68) {
			return -1;
		}

		int checksum = 0;

		// On boucle sur les 68 premiers caractères (le 69ème étant le checksum lu)
		int lengthToScan = Math.min(line.length(), 68);
		for (int i = 0; i < lengthToScan; i++) {
			char c = line.charAt(i);

			if (c >= '0' && c <= '9') {
				checksum += (c - '0'); // Convertit le caractère en chiffre
			} else if (c == '-') {
				checksum += 1;
			}
			// Tout autre caractère vaut 0, donc on ne fait rien
		}

		return checksum % modulus;
	}

	public static double antecedentDroite(double a, double b, double y) {
		return (y - b) / a;
	}

	/**
	 * Implémentation générique de la méthode de Newton-Raphson.
	 * Utilise DoubleBinaryOperator en remplacement des pointeurs de fonction C.
	 */
	public static double NewtonRaphson(
			double target,
			double funcParam,
			DoubleBinaryOperator func,
			DoubleBinaryOperator funcPrime,
			double xStart,
			double tolerance,
			long maxIter) {

		double xGuess = xStart;
		double lowLimit = target - tolerance;
		double highLimit = target + tolerance;

		for (long n = 0; n < maxIter; n++) {
			// Équivalent Java de func(x_guess, func_param)
			double fVal = func.applyAsDouble(xGuess, funcParam);

			// Condition de sortie de boucle (si la valeur est dans les limites)
			if (fVal >= lowLimit && fVal <= highLimit) {
				break;
			}

			// Équivalent Java de func_prime(x_guess, func_param)
			double a = funcPrime.applyAsDouble(xGuess, funcParam);
			double b = -a * xGuess + fVal;
			xGuess = antecedentDroite(a, b, target);
		}

		return xGuess;
	}
}
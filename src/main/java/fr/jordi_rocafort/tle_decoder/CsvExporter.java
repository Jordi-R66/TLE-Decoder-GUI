package fr.jordi_rocafort.tle_decoder;

import fr.jordi_rocafort.tle_decoder.model.data.DynamicValues;
import fr.jordi_rocafort.tle_decoder.model.data.StaticValues;
import fr.jordi_rocafort.tle_decoder.model.data.TLE;
import fr.jordi_rocafort.tle_decoder.model.physics.Constants;
import fr.jordi_rocafort.tle_decoder.model.physics.OrbitPropagator;
import fr.jordi_rocafort.tle_decoder.model.tle.TleFileManager;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class CsvExporter {

	public static void main(String[] args) {
		// Obligatoire pour avoir un point "." comme séparateur décimal (comme le %.4lf
		// du C)
		Locale.setDefault(Locale.US);

		long start = 1772724600L;
		long end = start + 86400L;
		long step = 1L;

		try {
			// 1. Charger le TLE depuis ton dossier existant
			TLE tle = TleFileManager.getSingleTLE("TLEs/merged.tle", 25544);

			// 2. Calculer la phase statique une seule fois
			StaticValues init = OrbitPropagator.computeStaticPhase(tle);

			System.out.println("Début de l'export CSV. Calcul de " + ((end - start) / step) + " positions...");

			// 3. Ouvrir le fichier en écriture
			// PrintWriter est l'équivalent le plus rapide de fprintf en Java
			try (PrintWriter writer = new PrintWriter("output_java.csv", StandardCharsets.UTF_8)) {

				// Header
				writer.println(
						"timestamp,MeanAnomaly,EccentricAnomaly,TrueAnomaly,R,Altitude,X,Y,Z,Latitude,Longitude");

				// Boucle de balayage (remplace sweepTimestamps)
				for (long timestamp = start; timestamp <= end; timestamp += step) {
					DynamicValues dyn = OrbitPropagator.computeDynamicPhase(tle, init, timestamp);
					writeAsCSV(writer, timestamp, dyn);
				}
			}

			System.out.println("Export terminé avec succès : output_java.csv");

		} catch (Exception e) {
			System.err.println("Erreur lors de l'export : " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Traduction exacte de la fonction writeAsCSV
	 */
	private static void writeAsCSV(PrintWriter writer, long timestamp, DynamicValues dyn) {
		// Conversion en degrés pour l'affichage CSV
		double m = dyn.meanAno() * Constants.RADS2DEGS;
		double e = dyn.eccAno() * Constants.RADS2DEGS;
		double nu = dyn.trueAno() * Constants.RADS2DEGS;

		double r = dyn.distanceToFocal();

		double lat = dyn.geoCoords().lat();
		double lng = dyn.geoCoords().lng();
		double alt = dyn.geoCoords().altitude();

		double x = dyn.coords3d().x();
		double y = dyn.coords3d().y();
		double z = dyn.coords3d().z();

		// En Java, %d s'utilise pour les entiers/long, et %f pour les flottants/doubles
		// On reprend les précisions exactes de ton code C : %.4lf, %.3lf, %.5lf
		writer.printf("%d,%.4f,%.4f,%.4f,%.3f,%.3f,%.4f,%.4f,%.4f,%.5f,%.5f\n",
				timestamp, m, e, nu, r, alt, x, y, z, lat, lng);
	}
}
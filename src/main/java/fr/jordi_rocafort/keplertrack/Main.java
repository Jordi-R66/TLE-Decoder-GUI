package fr.jordi_rocafort.keplertrack;

import java.time.Instant;
import java.util.Locale;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.formdev.flatlaf.*;

import fr.jordi_rocafort.keplertrack.model.data.*;
import fr.jordi_rocafort.keplertrack.model.physics.OrbitPropagator;
import fr.jordi_rocafort.keplertrack.model.tle.*;
import fr.jordi_rocafort.keplertrack.util.TimeUtils;
import fr.jordi_rocafort.keplertrack.view.frames.KeplerTrack;

public class Main {

	public static void cliMode(String filename, int noradId) {
		try {
			TLE tle = TleFileManager.getSingleTLE(filename, noradId);
			StaticValues init = OrbitPropagator.computeStaticPhase(tle);

			// Boucle temps réel
			while (true) {
				long currentTimestamp = Instant.now().getEpochSecond();
				DynamicValues instant = OrbitPropagator.computeDynamicPhase(tle, init, currentTimestamp);

				// Efface la console (équivalent du clear_screen en C sur Linux)
				System.out.print("\033[H\033[2J");
				System.out.flush();

				// Appel de la méthode d'affichage formaté
				printValues(tle, init, instant, currentTimestamp);

				Thread.sleep(16);
			}
		} catch (Exception e) {
			System.err.println("Erreur fatale de la simulation :");
			e.printStackTrace();
		}
	}

	public static void guiMode() {
		try {
			UIManager.setLookAndFeel(new FlatDarkLaf());
		} catch (Exception e) {}

		SwingUtilities.invokeLater(() -> {
			KeplerTrack tleDecoder = new KeplerTrack();

			tleDecoder.setVisible(true);
		});
	}

	public static void main(String[] args) {
		// Force l'affichage des nombres avec un point "." au lieu d'une virgule "," (format anglo-saxon)
		Locale.setDefault(Locale.US);
		//TleDownloader.downloadAndMergeAllTles(4, TleFormat.CSV);
		guiMode();
	}

	/**
	 * Traduction exacte de la fonction printValues du projet C original.
	 */
	private static void printValues(TLE tle, StaticValues init, DynamicValues instant, long currentTimestamp) {

		// Strings pré-formatées pour les dates et périodes
		String epochString = TimeUtils.timestampToDateString(init.epochTimestamp());
		String periodString = TimeUtils.periodToString(init.T());
		String instantString = TimeUtils.timestampToDateString(currentTimestamp);
		String ageString = TimeUtils.deltaTimeToString(instant.deltaTime());

		// Note: En Java %u n'existe pas, on utilise %d car les int/long couvrent les
		// valeurs.
		// %lf devient %f en Java.

		System.out.printf("Object name : %s\n", tle.name());

		System.out.println("----------------------------------- TLE -----------------------------------");

		System.out.printf("NORAD ID : %d%c\n", tle.noradId(), tle.classification());
		System.out.printf("COSPAR : %d %03d %s\n", tle.cosparYear(), tle.cosparLaunchNum(), tle.cosparPiece());
		System.out.printf("EPOCH : %s\n", epochString);
		System.out.printf("TLE AGE : %s\n", ageString);
		System.out.printf("(MEAN MOTION)' = %.4e\n", tle.firstDerivMeanMotion());
		System.out.printf("(MEAN MOTION)'' = %.4e\n", tle.secondDerivMeanMotion());
		System.out.printf("B* = %.4e\n", tle.bStar());

		System.out.println();

		System.out.printf("INCLINATION : %.4f degs\n", tle.inclination());
		System.out.printf("LONGITUDE OF ASC. NODE : %.4f degs\n", tle.rightAscension());
		System.out.printf("LONGITUDE OF PERIAPSIS : %.4f degs\n", tle.rightAscension() + tle.argumentOfPerigee());
		System.out.printf("ECCENTRICITY : %.7f\n", tle.eccentricity());
		System.out.printf("ARG. OF PERIAPSIS : %.4f degs\n", tle.argumentOfPerigee());
		System.out.printf("MEAN ANOMALY : %.4f degs\n", tle.meanAnomaly());
		System.out.printf("MEAN MOTION : %.8f rev/day\n", tle.meanMotion());

		System.out.println("--------------------------------- RESULTS ---------------------------------");

		System.out.printf("ORBITAL PERIOD : %.4f secs (%s)\n", init.T(), periodString);
		System.out.printf("SEMI MAJOR AXIS : %.0f m\n", init.a());
		System.out.printf("APOAPSIS : %.0f m | PERIAPSIS : %.0f m | EPOCH : %.0f m\n", init.apoAlt(), init.periAlt(),
				init.epochAlt());
		System.out.printf("SPEED @ AP : %.4f m/s | PE : %.4f m/s | EP : %.4f m/s\n", init.apoSpd(), init.periSpd(),
				init.epochSpd());

		System.out.println("-------------------------------- CURRENTLY --------------------------------");

		System.out.printf("DATE : %s\n", instantString);
		System.out.printf("X Coord : %.2f m\n", instant.coords3d().x());
		System.out.printf("Y Coord : %.2f m\n", instant.coords3d().y());
		System.out.printf("Z Coord : %.2f m\n", instant.coords3d().z());
		System.out.println();
		System.out.printf("LATITUDE : %.5f\n", instant.geoCoords().lat());
		System.out.printf("LONGITUDE : %.5f\n", instant.geoCoords().lng());
		System.out.printf("ALTITUDE : %.0f m\n", instant.geoCoords().altitude());
		System.out.println();
		System.out.printf("VELOCITY : %.2f m/s\n", instant.speed());

		System.out.println("---------------------------------------------------------------------------");
	}
}
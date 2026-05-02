package fr.jordi_rocafort.keplertrack.util;

import fr.jordi_rocafort.keplertrack.model.data.GeoCoords;
import fr.jordi_rocafort.keplertrack.model.data.Station;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class StationManager {

	private static final String FILE_PATH = "stations.csv"; // Ou mets-le dans un dossier "config"
	private static final String CSV_HEADER = "Place Name,Lat,Lng,Alt ASL (meters)";

	// Méthode appelée au démarrage pour s'assurer que le fichier existe
	public static void ensureCsvExists() {
		Path path = Paths.get(FILE_PATH);
		if (!Files.exists(path)) {
			System.out
					.println("Fichier " + FILE_PATH + " introuvable. Création en cours avec des valeurs par défaut...");
			try (PrintWriter writer = new PrintWriter(new FileWriter(FILE_PATH))) {
				// Écriture de l'en-tête
				writer.println(CSV_HEADER);
				// Ajout de quelques stations par défaut pour l'utilisateur
				writer.println(new Station("Paris (France)", new GeoCoords(48.8566, 2.3522, 35)).toCsvRow());
				writer.println(new Station("Montpellier (France)", new GeoCoords(43.6107, 3.8767, 27)).toCsvRow());
				writer.println(new Station("Kourou (Guyane)", new GeoCoords(5.1597, -52.6503, 4)).toCsvRow());
			} catch (IOException e) {
				System.err.println("Impossible de créer le fichier " + FILE_PATH);
				e.printStackTrace();
			}
		}
	}

	// Lire toutes les stations depuis le CSV
	public static List<Station> loadStations() {
		List<Station> stations = new ArrayList<>();
		ensureCsvExists(); // Sécurité supplémentaire

		try (BufferedReader reader = new BufferedReader(new FileReader(FILE_PATH))) {
			String line;
			boolean isFirstLine = true;
			while ((line = reader.readLine()) != null) {
				// Ignorer l'en-tête
				if (isFirstLine) {
					isFirstLine = false;
					continue;
				}

				// Ignorer les lignes vides
				if (line.trim().isEmpty())
					continue;

				Station station = Station.fromCsvRow(line);
				if (station != null) {
					stations.add(station);
				}
			}
		} catch (IOException e) {
			System.err.println("Erreur lors de la lecture du fichier " + FILE_PATH);
			e.printStackTrace();
		}
		return stations;
	}

	// Ajouter une nouvelle station à la fin du fichier CSV
	public static void saveStation(Station newStation) {
		ensureCsvExists();

		// Le paramètre "true" dans FileWriter active le mode "Append" (ajout à la fin
		// du fichier)
		try (PrintWriter writer = new PrintWriter(new FileWriter(FILE_PATH, true))) {
			writer.println(newStation.toCsvRow());
		} catch (IOException e) {
			System.err.println("Erreur lors de la sauvegarde de la station : " + newStation.name());
			e.printStackTrace();
		}
	}
}
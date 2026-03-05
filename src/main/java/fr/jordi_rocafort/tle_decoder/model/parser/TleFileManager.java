package fr.jordi_rocafort.tle_decoder.model.parser;

import fr.jordi_rocafort.tle_decoder.model.data.TLE;
import fr.jordi_rocafort.tle_decoder.model.data.TleBlock;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class TleFileManager {

	/**
	 * Scanne un fichier TLE (comme merged.tle) et renvoie l'objet TLE correspondant
	 * à l'ID NORAD.
	 */
	public static TLE getTleFromNoradId(String filePath, int targetNoradId) throws Exception {
		List<String> lines = Files.readAllLines(Path.of(filePath));

		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i).trim();

			// Si la ligne commence par "1 ", c'est la première ligne d'un TLE
			if (line.startsWith("1 ")) {
				// On extrait l'ID (caractères index 2 à 6)
				int noradId = Integer.parseInt(line.substring(2, 7).trim());

				if (noradId == targetNoradId) {
					// Récupération du nom du satellite (ligne précédente)
					String name = "UNKNOWN";
					if (i > 0) {
						String previousLine = lines.get(i - 1).trim();
						// On vérifie que ce n'est pas juste un fichier sans nom (ex: suite de TLE nus)
						if (!previousLine.startsWith("1 ") && !previousLine.startsWith("2 ")) {
							name = previousLine;
						}
					}

					String line2 = lines.get(i + 1).trim();

					TleBlock block = new TleBlock(name, line, line2);
					return TleParser.parseLines(block);
				}
			}
		}
		throw new IllegalArgumentException("Satellite " + targetNoradId + " introuvable dans " + filePath);
	}
}
package fr.jordi_rocafort.keplertrack.model.data;

/**
 * Représente un lieu d'observation (Station au sol).
 */
public record Station(String name, GeoCoords coords) {

	// Méthode utilitaire pour générer la ligne CSV correspondante
	public String toCsvRow() {
		// Format: Place Name, Lat, Lng, Alt ASL (meters)
		return String.format("%s,%f,%f,%f",
				name.replace(",", " "), // Sécurité : on retire les virgules du nom pour ne pas casser le CSV
				coords.lat(),
				coords.lng(),
				coords.altitude());
	}

	// Méthode de parsing depuis une ligne CSV
	public static Station fromCsvRow(String csvLine) {
		String[] parts = csvLine.split(",");
		if (parts.length >= 4) {
			try {
				String name = parts[0].trim();
				double lat = Double.parseDouble(parts[1].trim());
				double lng = Double.parseDouble(parts[2].trim());
				double alt = Double.parseDouble(parts[3].trim());
				return new Station(name, new GeoCoords(lat, lng, alt));
			} catch (NumberFormatException e) {
				System.err.println("Erreur de parsing des coordonnées pour la ligne : " + csvLine);
			}
		}
		return null; // Retourne null si la ligne est mal formatée
	}
}
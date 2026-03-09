package fr.jordi_rocafort.tle_decoder.util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;

public class ResourceManager {
	// Variable statique pour conserver l'image en RAM une fois chargée
	private static BufferedImage worldMapImage = null;

	private static final String WORLD_MAP = "/blue_marble1.jpg";

	/**
	 * Retourne l'image de la carte du monde.
	 * Si elle n'est pas encore chargée, elle est lue depuis le disque.
	 */
	public static BufferedImage getWorldMap() {
		if (worldMapImage == null) {
			try {
				InputStream is = ResourceManager.class.getResourceAsStream(WORLD_MAP);
				if (is != null) {
					worldMapImage = ImageIO.read(is);
					System.out.printf("Image '%s' chargée en mémoire avec succès (Une seule fois).\n", WORLD_MAP);
				} else {
					System.err.printf("Avertissement : Image '%s' introuvable.\n", WORLD_MAP);
				}
			} catch (Exception e) {
				System.err.printf("Erreur lors du chargement de la carte du monde : %s\n", e.getMessage());
				e.printStackTrace();
			}
		}
		return worldMapImage;
	}
}
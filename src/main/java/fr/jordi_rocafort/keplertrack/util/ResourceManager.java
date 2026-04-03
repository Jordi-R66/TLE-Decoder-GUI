package fr.jordi_rocafort.keplertrack.util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;

public class ResourceManager {
	// Variable statique pour conserver l'image en RAM une fois chargée
	private static BufferedImage worldMapImage_highres = null;
	private static BufferedImage worldMapImage_lowres = null;

	private static final String WORLD_MAP_LOWRES = "/blue_marble1.jpg";
	private static final String WORLD_MAP_HIGHRES = "/blue_marble2.jpg";

	/**
	 * Retourne l'image haute résolution de la carte du monde.
	 * Si elle n'est pas encore chargée, elle est lue depuis le disque.
	 */
	public static BufferedImage getLowResWorldMap() {
		if (worldMapImage_lowres == null) {
			try {
				InputStream is = ResourceManager.class.getResourceAsStream(WORLD_MAP_LOWRES);
				if (is != null) {
					worldMapImage_lowres = ImageIO.read(is);
					System.out.printf("Image '%s' chargée en mémoire avec succès (Une seule fois).\n",
							WORLD_MAP_LOWRES);
				} else {
					System.err.printf("Avertissement : Image '%s' introuvable.\n", WORLD_MAP_LOWRES);
				}
			} catch (Exception e) {
				System.err.printf("Erreur lors du chargement de la carte du monde : %s\n", e.getMessage());
				e.printStackTrace();
			}
		}

		return worldMapImage_lowres;
	}

	/**
	 * Retourne l'image haute résolution de la carte du monde.
	 * Si elle n'est pas encore chargée, elle est lue depuis le disque.
	 */
	public static BufferedImage getHighResWorldMap() {
		if (worldMapImage_highres == null) {
			try {
				InputStream is = ResourceManager.class.getResourceAsStream(WORLD_MAP_HIGHRES);
				if (is != null) {
					worldMapImage_highres = ImageIO.read(is);
					System.out.printf("Image '%s' chargée en mémoire avec succès (Une seule fois).\n",
							WORLD_MAP_HIGHRES);
				} else {
					System.err.printf("Avertissement : Image '%s' introuvable.\n", WORLD_MAP_HIGHRES);
				}
			} catch (Exception e) {
				System.err.printf("Erreur lors du chargement de la carte du monde : %s\n", e.getMessage());
				e.printStackTrace();
			}
		}

		return worldMapImage_highres;
	}
}
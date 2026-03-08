package fr.jordi_rocafort.tle_decoder.view;

import fr.jordi_rocafort.tle_decoder.model.data.GeoCoords;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.util.LinkedList;

public class GroundTrackMapPanel extends JPanel {

	private static GroundTrackMapPanel instance;
	private GeoCoords currentPosition;

	// Historique pour dessiner la trace du satellite (limité à N points)
	private final LinkedList<GeoCoords> trackHistory = new LinkedList<>();
	private static final int MAX_HISTORY = 300;

	public static GroundTrackMapPanel getInstance() {
		if (instance == null) {
			instance = new GroundTrackMapPanel();
		}
		return instance;
	}

	private GroundTrackMapPanel() {
		this.setBackground(new Color(20, 24, 32)); // Fond sombre "espace/radar"
		this.setBorder(BorderFactory.createTitledBorder("Trace au sol 2D (Projection Équirectangulaire)"));
	}

	/**
	 * Met à jour la position et redessine le panneau
	 */
	public void updatePosition(GeoCoords geoCoords) {
		this.currentPosition = geoCoords;

		trackHistory.add(geoCoords);
		if (trackHistory.size() > MAX_HISTORY) {
			trackHistory.removeFirst();
		}

		// Demande à Swing de redessiner ce composant
		this.repaint();
	}

	/**
	 * Réinitialise la trace (utile quand on change de satellite)
	 */
	public void clearTrack() {
		trackHistory.clear();
		currentPosition = null;
		this.repaint();
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2d = (Graphics2D) g;

		// Antialiasing pour un rendu plus lisse
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		int width = getWidth();
		int height = getHeight();

		// 1. Dessin d'une grille de fond (Méridiens et Parallèles)
		g2d.setColor(new Color(40, 50, 65));
		for (int lon = -180; lon <= 180; lon += 30) {
			int x = lngToX(lon, width);
			g2d.drawLine(x, 0, x, height);
		}
		for (int lat = -90; lat <= 90; lat += 30) {
			int y = latToY(lat, height);
			g2d.drawLine(0, y, width, y);
		}

		// Équateur et Méridien de Greenwich en plus clair
		g2d.setColor(new Color(80, 90, 110));
		g2d.drawLine(0, height / 2, width, height / 2); // Équateur
		g2d.drawLine(width / 2, 0, width / 2, height); // Greenwich

		// 2. Dessin de la trace historique (ligne continue)
		if (trackHistory.size() > 1) {
			g2d.setColor(new Color(100, 200, 255, 150)); // Bleu clair semi-transparent
			g2d.setStroke(new BasicStroke(2.0f));

			Path2D path = new Path2D.Double();
			boolean first = true;
			GeoCoords prev = null;

			for (GeoCoords pos : trackHistory) {
				int x = lngToX(pos.lng(), width);
				int y = latToY(pos.lat(), height);

				if (first) {
					path.moveTo(x, y);
					first = false;
				} else {
					// Gestion du passage "pacifique" (quand on passe de +180 à -180 de longitude)
					// Si le saut est trop grand (ex: > 180 degrés), on coupe la ligne pour éviter
					// un trait horizontal
					if (Math.abs(pos.lng() - prev.lng()) > 180) {
						path.moveTo(x, y);
					} else {
						path.lineTo(x, y);
					}
				}
				prev = pos;
			}
			g2d.draw(path);
		}

		// 3. Dessin du satellite (point actuel)
		if (currentPosition != null) {
			int currentX = lngToX(currentPosition.lng(), width);
			int currentY = latToY(currentPosition.lat(), height);

			// Halo rouge
			g2d.setColor(new Color(255, 50, 50, 100));
			g2d.fillOval(currentX - 10, currentY - 10, 20, 20);

			// Centre rouge vif
			g2d.setColor(Color.RED);
			g2d.fillOval(currentX - 4, currentY - 4, 8, 8);
		}
	}

	// --- Méthodes de conversion Mathématiques ---

	private int lngToX(double lng, int width) {
		// La longitude va de -180 à 180
		return (int) ((lng + 180.0) / 360.0 * width);
	}

	private int latToY(double lat, int height) {
		// La latitude va de -90 (Sud) à 90 (Nord).
		// L'axe Y des écrans va vers le bas, on doit donc inverser.
		return (int) ((90.0 - lat) / 180.0 * height);
	}
}
package fr.jordi_rocafort.keplertrack.view;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.util.List;

import fr.jordi_rocafort.keplertrack.model.data.PassPoint;
import fr.jordi_rocafort.keplertrack.model.data.SatellitePass;
import fr.jordi_rocafort.keplertrack.model.data.TopocentricCoords;
import fr.jordi_rocafort.keplertrack.model.physics.Constants;

public class PolarViewPanel extends JPanel {
	private static PolarViewPanel instance;

	// Le passage actuellement sélectionné à afficher
	private SatellitePass currentPass;

	// Position instantanée (si on simule le passage en temps réel)
	private TopocentricCoords currentInstantCoords;

	public static PolarViewPanel getInstance() {
		if (instance == null) {
			instance = new PolarViewPanel();
		}
		return instance;
	}

	private PolarViewPanel() {
		this.setBackground(new Color(20, 24, 32));
	}

	/**
	 * Affiche l'historique complet d'un passage.
	 */
	public void displayPass(SatellitePass pass) {
		this.currentPass = pass;
		this.currentInstantCoords = null;
		this.repaint();
	}

	/**
	 * Met à jour la position du satellite en temps réel sur le graphique.
	 */
	public void updateInstantPosition(TopocentricCoords coords) {
		this.currentInstantCoords = coords;
		this.repaint();
	}

	public void clear() {
		this.currentPass = null;
		this.currentInstantCoords = null;
		this.repaint();
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);

		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		int width = getWidth();
		int height = getHeight();
		int centerX = width / 2;
		int centerY = height / 2;

		// Le rayon de l'horizon (0° d'élévation)
		// On laisse une petite marge (10%) pour dessiner les lettres N, S, E, W
		int maxRadius = (int) (Math.min(centerX, centerY) * 0.85);

		// --- 1. Dessin de la grille polaire (Boussole et Élévations) ---
		drawPolarGrid(g2d, centerX, centerY, maxRadius);

		// --- 2. Dessin de la trajectoire du passage ---
		if (currentPass != null && currentPass.trajectory() != null && !currentPass.trajectory().isEmpty()) {
			drawTrajectory(g2d, centerX, centerY, maxRadius);
		}

		// --- 3. Dessin de la position instantanée (Point rouge) ---
		if (currentInstantCoords != null) {
			drawSatellite(g2d, centerX, centerY, maxRadius, currentInstantCoords);
		}
	}

	/**
	 * Dessine la grille de fond : les cercles concentriques (élévations) et la
	 * croix (Nord/Sud, Est/Ouest).
	 */
	private void drawPolarGrid(Graphics2D g2d, int centerX, int centerY, int maxRadius) {
		g2d.setColor(new Color(40, 50, 65)); // Couleur de la grille
		g2d.setStroke(new BasicStroke(1.0f));

		// Les axes (Croix principale)
		g2d.drawLine(centerX, centerY - maxRadius, centerX, centerY + maxRadius); // N-S
		g2d.drawLine(centerX - maxRadius, centerY, centerX + maxRadius, centerY); // E-W

		// Les cercles d'élévation (0°, 30°, 60°)
		int[] elevations = { 0, 30, 60 };

		for (int elev : elevations) {
			// Le rayon est proportionnel à l'angle depuis le Zénith (90 - élévation)
			int r = (int) (maxRadius * ((90.0 - elev) / 90.0));
			Ellipse2D.Double circle = new Ellipse2D.Double(centerX - r, centerY - r, r * 2, r * 2);
			g2d.draw(circle);

			// Affichage du texte de l'élévation sur l'axe Sud
			if (elev > 0) { // On ne met pas 0° pour ne pas surcharger
				g2d.setColor(new Color(100, 120, 140));
				g2d.setFont(new Font("Arial", Font.PLAIN, 10));
				g2d.drawString(elev + "°", centerX + 2, centerY + r - 2);
				g2d.setColor(new Color(40, 50, 65)); // Remise de la couleur de grille
			}
		}

		// --- Ajout des points cardinaux ---
		g2d.setColor(new Color(150, 180, 200));
		g2d.setFont(new Font("Arial", Font.BOLD, 14));
		FontMetrics fm = g2d.getFontMetrics();

		// Nord (En Haut)
		String n = "N";
		g2d.drawString(n, centerX - fm.stringWidth(n) / 2, centerY - maxRadius - 5);
		// Sud (En Bas)
		String s = "S";
		g2d.drawString(s, centerX - fm.stringWidth(s) / 2, centerY + maxRadius + fm.getAscent() + 2);
		// Est (À Droite)
		String e = "E";
		g2d.drawString(e, centerX + maxRadius + 5, centerY + fm.getAscent() / 2 - 2);
		// Ouest (À Gauche)
		String w = "W";
		g2d.drawString(w, centerX - maxRadius - fm.stringWidth(w) - 5, centerY + fm.getAscent() / 2 - 2);
	}

	/**
	 * Dessine la ligne de la trajectoire (AOS -> LOS).
	 */
	private void drawTrajectory(Graphics2D g2d, int centerX, int centerY, int maxRadius) {
		g2d.setColor(new Color(100, 200, 255, 180));
		g2d.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

		Path2D.Double path = new Path2D.Double();
		boolean isFirst = true;

		for (PassPoint pt : currentPass.trajectory()) {
			TopocentricCoords coords = pt.coords();

			if (coords.elevation() < 0)
				continue;

			Point p = polarToScreen(coords, centerX, centerY, maxRadius);

			if (isFirst) {
				path.moveTo(p.x, p.y);
				isFirst = false;
			} else {
				path.lineTo(p.x, p.y);
			}
		}

		g2d.draw(path);

		// --- Optionnel : Marquer l'AOS et le LOS ---
		if (!currentPass.trajectory().isEmpty()) {
			List<PassPoint> traj = currentPass.trajectory();
			// Point AOS (Début)
			Point pAos = polarToScreen(traj.get(0).coords(), centerX, centerY, maxRadius);
			g2d.setColor(Color.GREEN);
			g2d.fillOval(pAos.x - 4, pAos.y - 4, 8, 8);

			// Point LOS (Fin)
			Point pLos = polarToScreen(traj.get(traj.size() - 1).coords(), centerX, centerY, maxRadius);
			g2d.setColor(Color.ORANGE);
			g2d.fillOval(pLos.x - 4, pLos.y - 4, 8, 8);
		}
	}

	/**
	 * Dessine la position d'un point (le satellite en temps réel).
	 */
	private void drawSatellite(Graphics2D g2d, int centerX, int centerY, int maxRadius, TopocentricCoords coords) {
		if (coords.elevation() < 0)
			return; // Caché sous l'horizon

		Point p = polarToScreen(coords, centerX, centerY, maxRadius);

		// Point du satellite (Aura + Centre)
		g2d.setColor(new Color(255, 50, 50, 100));
		g2d.fillOval(p.x - 8, p.y - 8, 16, 16);
		g2d.setColor(Color.RED);
		g2d.fillOval(p.x - 3, p.y - 3, 6, 6);
	}

	/**
	 * Convertit des coordonnées (Azimut, Elévation) en pixels X,Y sur l'écran.
	 */
	private Point polarToScreen(TopocentricCoords coords, int centerX, int centerY, int maxRadius) {
		// 1. Calcul du rayon sur le graphique.
		double r = maxRadius * ((90.0 - coords.elevation()) / 90.0);

		double azRads = coords.azimuth() * Constants.DEGS2RADS;

		// Pour avoir Nord en Haut(y négatif), Est à Droite(x positif) :
		double x = centerX + r * Math.sin(azRads);
		double y = centerY - r * Math.cos(azRads);

		return new Point((int) Math.round(x), (int) Math.round(y));
	}
}
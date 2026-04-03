package fr.jordi_rocafort.keplertrack.view;

import javax.swing.*;

import fr.jordi_rocafort.keplertrack.model.data.Coords2D;
import fr.jordi_rocafort.keplertrack.model.data.StaticValues;
import fr.jordi_rocafort.keplertrack.model.data.TLE;
import fr.jordi_rocafort.keplertrack.model.physics.Constants;

import java.awt.*;
import java.awt.geom.Ellipse2D;

public class Orbit2DPanel extends JPanel {
	private static Orbit2DPanel instance;

	private Coords2D currentCoords;
	private StaticValues initValues;
	private TLE currentTle;

	public static Orbit2DPanel getInstance() {
		if (instance == null) {
			instance = new Orbit2DPanel();
		}
		return instance;
	}

	private Orbit2DPanel() {
		this.setBackground(new Color(20, 24, 32));
		//this.setBorder(BorderFactory.createTitledBorder("Orbite 2D (Vue perpendiculaire au plan orbital)"));
	}

	public void updateData(TLE tle, StaticValues init, Coords2D coords) {
		this.currentTle = tle;
		this.initValues = init;
		this.currentCoords = coords;
		this.repaint();
	}

	public void clear() {
		this.currentTle = null;
		this.initValues = null;
		this.currentCoords = null;
		this.repaint();
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (initValues == null || currentCoords == null)
			return;

		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		int width = getWidth();
		int height = getHeight();
		int centerX = width / 2;
		int centerY = height / 2;

		// 1. Calcul de l'échelle (pour que l'apogée rentre dans le panneau avec 10% de
		// marge)
		double maxDist = initValues.a() * (1.0 + currentTle.eccentricity());
		double scale = Math.min(width, height) / (2.0 * maxDist * 1.1);

		// 2. Dessin des axes (L'axe X positif pointe vers le périgée)
		g2d.setColor(new Color(40, 50, 65));
		g2d.drawLine(0, centerY, width, centerY); // Axe X (Ligne des apsides)
		g2d.drawLine(centerX, 0, centerX, height); // Axe Y

		// 3. Dessin de la Terre au foyer de l'orbite (0,0)
		double earthRadiusScaled = Constants.WGS84_A * scale;
		g2d.setColor(new Color(50, 100, 200, 180)); // Bleu Terre
		g2d.fillOval((int) (centerX - earthRadiusScaled), (int) (centerY - earthRadiusScaled),
				(int) (earthRadiusScaled * 2), (int) (earthRadiusScaled * 2));

		// 4. Dessin de l'orbite (Ellipse mathématique parfaite)
		double a = initValues.a() * scale;
		double e = currentTle.eccentricity();
		double b = a * Math.sqrt(1 - e * e);
		double c = a * e; // Distance entre le centre de l'ellipse et la Terre (le foyer)

		// Dans notre repère, la Terre est à (0,0) et le périgée est sur X positif.
		// Le centre géométrique de l'ellipse est donc décalé de -c sur l'axe X.
		double ellipseCenterX = centerX - c;
		double ellipseCenterY = centerY;

		g2d.setColor(new Color(100, 200, 255, 150));
		g2d.setStroke(new BasicStroke(1.5f));
		Ellipse2D.Double orbitShape = new Ellipse2D.Double(
				ellipseCenterX - a, ellipseCenterY - b,
				a * 2, b * 2);
		g2d.draw(orbitShape);

		// 5. Dessin du Satellite
		// L'axe Y de Swing va vers le bas, on doit l'inverser pour respecter les maths
		// (Y vers le haut)
		double satX = centerX + currentCoords.x() * scale;
		double satY = centerY - currentCoords.y() * scale;

		// Ligne pointillée reliant la Terre au satellite (Rayon vecteur)
		g2d.setColor(new Color(255, 255, 255, 50));
		g2d.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[] { 5 }, 0));
		g2d.drawLine(centerX, centerY, (int) satX, (int) satY);

		// Point du satellite
		g2d.setColor(new Color(255, 50, 50, 100));
		g2d.fillOval((int) satX - 8, (int) satY - 8, 16, 16);
		g2d.setColor(Color.RED);
		g2d.fillOval((int) satX - 3, (int) satY - 3, 6, 6);
	}
}
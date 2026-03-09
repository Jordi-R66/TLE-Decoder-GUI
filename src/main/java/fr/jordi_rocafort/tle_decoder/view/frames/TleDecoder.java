package fr.jordi_rocafort.tle_decoder.view.frames;

import fr.jordi_rocafort.tle_decoder.view.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class TleDecoder extends JFrame {
	private static TleDecoder instance;
	private static DataPanel dataPanel;

	public TleDecoder() {
		super("TLE-Decoder");

		instance = this;
		dataPanel = DataPanel.getInstance();

		this.setSize(1100, 620);
		this.setLocationRelativeTo(null);
		this.setLayout(new BorderLayout());

		/*// Panneau temporaire de droite (C, D, E)
		JPanel rightPanelPlaceholder = new JPanel();
		rightPanelPlaceholder.setBackground(new Color(40, 44, 52));*/

		// Placement : DataPanel fixe à gauche, le reste prend tout l'espace central
		this.add(dataPanel, BorderLayout.WEST);

		// Création des onglets pour les vues orbitales (2D vs 3D)
		JTabbedPane orbitTabs = new JTabbedPane();
		orbitTabs.addTab("Orbite 2D", Orbit2DPanel.getInstance());
		orbitTabs.addTab("Globe 3D", Orbit3DPanel.getInstance()); // NOUVEAU

		// Création d'un séparateur pour diviser la partie droite en haut/bas
		JSplitPane rightSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		rightSplitPane.setTopComponent(GroundTrackMapPanel.getInstance());
		rightSplitPane.setBottomComponent(orbitTabs); // On place les onglets en bas

		rightSplitPane.setResizeWeight(0.55);
		rightSplitPane.setContinuousLayout(true);
		rightSplitPane.setDividerSize(6);

		// On place ce panneau divisé au centre (qui prend tout l'espace restant à
		// droite du DataPanel)
		this.add(rightSplitPane, BorderLayout.CENTER);

		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				close();
			}
		});
	}

	public static TleDecoder getInstance() {
		return instance;
	}

	public void close() {
		System.exit(0);
	}
}
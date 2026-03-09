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

		JSplitPane lowerPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		lowerPane.setLeftComponent(Orbit2DPanel.getInstance());
		lowerPane.setRightComponent(GroundTrackMapPanel.getInstance());

		lowerPane.setResizeWeight(0.5);
		lowerPane.setContinuousLayout(true);
		lowerPane.setDividerSize(6);

		// Création d'un séparateur pour diviser la partie droite en haut/bas
		JSplitPane mainPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		mainPane.setTopComponent(Orbit3DPanel.getInstance());
		mainPane.setBottomComponent(lowerPane);

		mainPane.setResizeWeight(0.55);
		mainPane.setContinuousLayout(true);
		mainPane.setDividerSize(6);

		// On place ce panneau divisé au centre (qui prend tout l'espace restant à
		// droite du DataPanel)
		this.add(mainPane, BorderLayout.CENTER);

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
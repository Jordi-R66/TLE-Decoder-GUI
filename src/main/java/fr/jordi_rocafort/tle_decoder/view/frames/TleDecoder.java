package fr.jordi_rocafort.tle_decoder.view.frames;

import fr.jordi_rocafort.tle_decoder.view.*;

import java.util.ArrayList;

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

		ArrayList<Component> vues = new ArrayList<>(3);

		this.setSize(1100, 700);
		this.setLocationRelativeTo(null);
		this.setLayout(new BorderLayout());

		/*// Panneau temporaire de droite (C, D, E)
		JPanel rightPanelPlaceholder = new JPanel();
		rightPanelPlaceholder.setBackground(new Color(40, 44, 52));*/

		// Placement : DataPanel fixe à gauche, le reste prend tout l'espace central
		this.add(dataPanel, BorderLayout.WEST);

		JSplitPane lowerPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

		lowerPane.setResizeWeight(0.5);
		lowerPane.setContinuousLayout(true);
		lowerPane.setDividerSize(6);

		// Création d'un séparateur pour diviser la partie droite en haut/bas
		JSplitPane mainPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

		mainPane.setResizeWeight(0.55);
		mainPane.setContinuousLayout(true);
		mainPane.setDividerSize(6);

		vues.add(Orbit2DPanel.getInstance());
		vues.add(GroundTrackMapPanel.getInstance());
		vues.add(Orbit3DPanel.getInstance());

		lowerPane.setLeftComponent(vues.get(0));
		lowerPane.setRightComponent(vues.get(1));
		mainPane.setTopComponent(vues.get(2));

		mainPane.setBottomComponent(lowerPane);

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
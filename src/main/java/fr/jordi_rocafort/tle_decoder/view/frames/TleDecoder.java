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
		this.add(GroundTrackMapPanel.getInstance(), BorderLayout.CENTER);

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
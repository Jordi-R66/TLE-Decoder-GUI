package fr.jordi_rocafort.tle_decoder.view.frames;

import fr.jordi_rocafort.tle_decoder.view.*;

import java.util.ArrayList;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class TleDecoder extends JFrame {
	private static TleDecoder instance;
	private static DataPanel dataPanel;

	// Variables pour stocker l'état précédent avant le plein écran
	private boolean isFullScreen = false;
	private Rectangle normalBounds;
	private int normalState;

	public TleDecoder() {
		super("TLE-Decoder");

		instance = this;
		dataPanel = DataPanel.getInstance();

		ArrayList<Component> vues = new ArrayList<>(3);

		this.setSize(1100, 700);
		this.setLocationRelativeTo(null);
		this.setLayout(new BorderLayout());

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

		// On place ce panneau divisé au centre (qui prend tout l'espace restant à droite du DataPanel)
		this.add(mainPane, BorderLayout.CENTER);

		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				close();
			}
		});

		// --- CONFIGURATION DU RACCOURCI F11 ---
		setupFullScreenBinding();
	}

	/**
	 * Configure le Key Binding pour la touche F11 (fonctionne peu importe le
	 * composant qui a le focus).
	 */
	private void setupFullScreenBinding() {
		JRootPane rootPane = this.getRootPane();

		// On associe la touche F11 à une action nommée "toggleFullScreen"
		rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
				.put(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0), "toggleFullScreen");

		// On définit ce que fait cette action
		rootPane.getActionMap().put("toggleFullScreen", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				toggleFullScreen();
			}
		});
	}

	/**
	 * Bascule entre le mode fenêtré normal et le plein écran sans bordures.
	 */
	private void toggleFullScreen() {
		if (!isFullScreen) {
			// Sauvegarde de l'état actuel (taille, position, maximisation)
			normalBounds = this.getBounds();
			normalState = this.getExtendedState();

			// Passage en plein écran
			this.dispose();
			this.setUndecorated(true);
			this.setExtendedState(JFrame.MAXIMIZED_BOTH);
			this.setVisible(true);

			isFullScreen = true;
		} else {
			// Retour au mode fenêtré
			this.dispose();
			this.setUndecorated(false);
			this.setExtendedState(normalState);
			this.setBounds(normalBounds);
			this.setVisible(true);

			isFullScreen = false;
		}
	}

	public static TleDecoder getInstance() {
		return instance;
	}

	public void close() {
		System.exit(0);
	}
}
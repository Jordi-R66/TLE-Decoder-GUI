package fr.jordi_rocafort.tle_decoder.view.frames;

import fr.jordi_rocafort.tle_decoder.view.*;

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
		super("KeplerTrack");

		instance = this;
		dataPanel = DataPanel.getInstance();

		this.setSize(1100, 700);
		this.setLocationRelativeTo(null);
		this.setLayout(new BorderLayout());

		// Placement : DataPanel fixe à gauche
		this.add(dataPanel, BorderLayout.WEST);

		// Panneau inférieur contenant la vue 2D et la carte (50-50 grâce au GridLayout)
		JPanel bottomPanel = new JPanel(new GridLayout(1, 2));
		bottomPanel.add(Orbit2DPanel.getInstance());
		bottomPanel.add(GroundTrackMapPanel.getInstance());

		// Panneau droit contenant la vue 3D (Haut) et le panneau inférieur (Bas)
		JPanel rightPanel = new JPanel(new BorderLayout());
		rightPanel.add(Orbit3DPanel.getInstance(), BorderLayout.CENTER);
		rightPanel.add(bottomPanel, BorderLayout.SOUTH);

		// On place ce panneau droit au centre (qui prend tout l'espace restant à droite
		// du DataPanel)
		this.add(rightPanel, BorderLayout.CENTER);

		// Listener pour forcer le ratio dynamique du panneau inférieur sans autoriser
		// l'utilisateur à le modifier
		rightPanel.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				int width = rightPanel.getWidth();

				// La carte occupe 50% de la largeur totale (width / 2)
				// Sa hauteur doit être égale à 50% de sa propre largeur, soit (width / 2) / 2 =
				// width / 4
				int requiredHeight = width / 4;

				// On applique cette hauteur uniquement si elle a changé pour éviter les boucles
				// de redimensionnement
				if (bottomPanel.getPreferredSize().height != requiredHeight) {
					bottomPanel.setPreferredSize(new Dimension(width, requiredHeight));
					rightPanel.revalidate();
				}
			}
		});

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
	 * Bascule entre le mode fenêtré normal et le plein écran exclusif (vrai
	 * fullscreen).
	 */
	private void toggleFullScreen() {
		GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();

		if (!isFullScreen) {
			// Sauvegarde de l'état actuel (taille, position, maximisation)
			normalBounds = this.getBounds();
			normalState = this.getExtendedState();

			// Passage en vrai plein écran
			this.dispose(); // Nécessaire avant de changer l'état 'undecorated'
			this.setUndecorated(true);

			// Demande au système graphique de passer cette fenêtre en plein écran absolu
			device.setFullScreenWindow(this);

			this.setVisible(true);
			isFullScreen = true;
		} else {
			// Retour au mode fenêtré
			this.dispose();

			// Libère le mode plein écran exclusif
			device.setFullScreenWindow(null);

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
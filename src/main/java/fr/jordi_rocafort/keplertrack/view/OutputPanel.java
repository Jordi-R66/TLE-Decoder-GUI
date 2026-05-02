package fr.jordi_rocafort.keplertrack.view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;

import fr.jordi_rocafort.keplertrack.model.data.DynamicValues;
import fr.jordi_rocafort.keplertrack.model.data.StaticValues;
import fr.jordi_rocafort.keplertrack.model.data.Station;
import fr.jordi_rocafort.keplertrack.model.data.TLE;
import fr.jordi_rocafort.keplertrack.util.TimeUtils;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

public class OutputPanel extends JPanel {

	private static OutputPanel instance = null;

	// UI Components pour la navigation (CardLayout)
	private CardLayout cardLayout;
	private JPanel cardsContainer;
	private JRadioButton realTimeRadio;
	private JRadioButton predictionRadio;

	// UI Components pour le Temps Réel
	private Map<String, JLabel> valueLabels;

	// UI Components pour la Prédiction
	private JComboBox<Station> stationComboBox;
	/*
	 * private JTextField latField;
	 * private JTextField lngField;
	 * private JTextField altField;
	 */
	private JTextField maskField;
	private JTextField daysField;
	private JButton predictBtn;
	private DefaultTableModel passesTableModel;
	private JTable passesTable;

	public static OutputPanel getInstance() {
		if (instance == null) {
			instance = new OutputPanel();
		}
		return instance;
	}

	public OutputPanel() {
		this.setLayout(new BorderLayout());
		this.setBorder(BorderFactory.createTitledBorder("Output"));

		JPanel topMenu = new JPanel(new FlowLayout(FlowLayout.LEFT));
		realTimeRadio = new JRadioButton("Real Time", true);
		predictionRadio = new JRadioButton("Pass Prediction");

		ButtonGroup bg = new ButtonGroup();
		bg.add(realTimeRadio);
		bg.add(predictionRadio);
		topMenu.add(realTimeRadio);
		topMenu.add(predictionRadio);

		cardLayout = new CardLayout();
		cardsContainer = new JPanel(cardLayout);

		cardsContainer.add(buildRealTimeCard(), "REALTIME");
		cardsContainer.add(buildPredictionCard(), "PREDICTION");

		realTimeRadio.addActionListener(e -> cardLayout.show(cardsContainer, "REALTIME"));
		predictionRadio.addActionListener(e -> cardLayout.show(cardsContainer, "PREDICTION"));

		this.add(topMenu, BorderLayout.NORTH);
		this.add(cardsContainer, BorderLayout.CENTER);
	}

	private JPanel buildRealTimeCard() {
		valueLabels = new HashMap<>();

		JPanel contentPanel = new JPanel();
		contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

		contentPanel.add(createCategoryPanel("Identification", new String[] {
				"Object name", "NORAD ID", "COSPAR ID", "EPOCH", "TLE AGE"
		}));
		contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));

		contentPanel.add(createCategoryPanel("Orbital Parameters", new String[] {
				"(MEAN MOTION)'", "(MEAN MOTION)''", "INCLINATION", "LONGITUDE OF ASC. NODE",
				"LONGITUDE OF PERIAPSIS", "ECCENTRICITY", "ARG. OF PERIAPSIS", "MEAN ANOMALY", "MEAN MOTION"
		}));
		contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));

		contentPanel.add(createCategoryPanel("Initial Results", new String[] {
				"ORBITAL PERIOD", "SEMI MAJOR AXIS", "ALTITUDE AT APOGEE",
				"ALTITUDE AT PERIGEE", "ALTITUDE AT EPOCH", "SPEED @ AP", "SPEED @ PE", "SPEED @ EPOCH"
		}));
		contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));

		contentPanel.add(createCategoryPanel("Real Time", new String[] {
				"DATE", "X Coord", "Y Coord", "Z Coord", "GROUND COORDINATES", "ALTITUDE", "VELOCITY", "LOCAL GRAVITY"
		}));

		JScrollPane scrollPane = new JScrollPane(contentPanel);
		scrollPane.setBorder(null);
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);

		JPanel card = new JPanel(new BorderLayout());
		card.add(scrollPane, BorderLayout.CENTER);
		return card;
	}

	private JPanel createCategoryPanel(String title, String[] keys) {
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(BorderFactory.createTitledBorder(
				BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor"), 1),
				title, TitledBorder.LEFT, TitledBorder.TOP,
				new Font(Font.SANS_SERIF, Font.BOLD, 12), UIManager.getColor("Label.foreground")));

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(4, 5, 4, 15);

		for (int i = 0; i < keys.length; i++) {
			String key = keys[i];

			JLabel keyLabel = new JLabel(key);
			keyLabel.setForeground(Color.GRAY);
			keyLabel.setFont(keyLabel.getFont().deriveFont(11f));
			gbc.gridx = 0;
			gbc.gridy = i;
			gbc.weightx = 0.0;
			panel.add(keyLabel, gbc);

			JLabel valueLabel = new JLabel("-");
			valueLabel.setFont(valueLabel.getFont().deriveFont(Font.BOLD, 12f));
			valueLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
			valueLabel.setToolTipText("Double-cliquez pour copier");

			valueLabel.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					if (e.getClickCount() == 2 && !valueLabel.getText().equals("-")) {
						StringSelection stringSelection = new StringSelection(valueLabel.getText());
						Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
						clipboard.setContents(stringSelection, null);
					}
				}
			});

			valueLabels.put(key, valueLabel);
			gbc.gridx = 1;
			gbc.gridy = i;
			gbc.weightx = 1.0;
			panel.add(valueLabel, gbc);
		}
		return panel;
	}

	// =========================================================
	// VUE 2 : PRÉDICTION DE PASSAGES
	// =========================================================
	private JPanel buildPredictionCard() {
		JPanel predictionPanel = new JPanel(new BorderLayout());

		// --- Zone de formulaire (Nord) ---
		JPanel formPanel = new JPanel(new GridBagLayout());
		formPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(5, 5, 5, 5);
		gbc.fill = GridBagConstraints.HORIZONTAL;

		/*
		 * // Ligne 1 : Observateur
		 * gbc.gridy = 0;
		 * gbc.gridx = 0;
		 * formPanel.add(new JLabel("Lat (°):"), gbc);
		 * gbc.gridx = 1;
		 * latField = new JTextField("48.8566", 6);
		 * formPanel.add(latField, gbc); // Paris par défaut
		 * 
		 * gbc.gridx = 2;
		 * formPanel.add(new JLabel("Lng (°):"), gbc);
		 * gbc.gridx = 3;
		 * lngField = new JTextField("2.3522", 6);
		 * formPanel.add(lngField, gbc);
		 * 
		 * gbc.gridx = 4;
		 * formPanel.add(new JLabel("Alt (m):"), gbc);
		 * gbc.gridx = 5;
		 * altField = new JTextField("35", 6);
		 * formPanel.add(altField, gbc);
		 */

		// Ligne 1 : Observateur (Liste déroulante)
		gbc.gridy = 0;
		gbc.gridx = 0;
		formPanel.add(new JLabel("Observateur:"), gbc);

		// Initialisation de la ComboBox avec un renderer personnalisé pour n'afficher
		// que le nom
		stationComboBox = new JComboBox<>();
		stationComboBox.setRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
					boolean cellHasFocus) {
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				if (value instanceof Station station) {
					setText(station.name());
				}
				return this;
			}
		});

		gbc.gridx = 1;
		gbc.gridwidth = 5; // Prend toute la largeur restante de la ligne
		formPanel.add(stationComboBox, gbc);

		// Rétablir gridwidth pour la ligne suivante
		gbc.gridwidth = 1;

		gbc.gridy = 1;
		gbc.gridx = 0;
		formPanel.add(new JLabel("Masque (°):"), gbc);
		gbc.gridx = 1;
		maskField = new JTextField("0.0", 6);
		formPanel.add(maskField, gbc);

		gbc.gridx = 2;
		formPanel.add(new JLabel("Jours:"), gbc);
		gbc.gridx = 3;
		daysField = new JTextField("3", 6);
		formPanel.add(daysField, gbc);

		gbc.gridx = 4;
		gbc.gridwidth = 2;
		predictBtn = new JButton("Calculate Passes");
		formPanel.add(predictBtn, gbc);

		String[] columns = { "AOS", "TCA", "LOS", "Duration", "Max Elevation" };
		passesTableModel = new DefaultTableModel(columns, 0) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};
		passesTable = new JTable(passesTableModel);
		JScrollPane tableScroll = new JScrollPane(passesTable);

		predictionPanel.add(formPanel, BorderLayout.NORTH);
		predictionPanel.add(tableScroll, BorderLayout.CENTER);

		return predictionPanel;
	}

	// =========================================================
	// MÉTHODES PUBLIQUES (Mise à jour et Getters)
	// =========================================================

	private void updateLabel(String key, String value) {
		JLabel label = valueLabels.get(key);
		if (label != null) {
			label.setText(value);
		}
	}

	public void showData(TLE tle, StaticValues init, DynamicValues instant, long currentTimestamp) {
		String epochString = TimeUtils.timestampToDateString(init.epochTimestamp());
		String periodString = TimeUtils.periodToString(init.T());
		String instantString = TimeUtils.timestampToDateString(currentTimestamp);
		String ageString = TimeUtils.deltaTimeToString(instant.deltaTime());

		// Identification
		updateLabel("Object name", tle.name());
		updateLabel("NORAD ID", String.format("%d%c", tle.noradId(), tle.classification()));
		updateLabel("COSPAR ID",
				String.format("%d %03d %s", tle.cosparYear(), tle.cosparLaunchNum(), tle.cosparPiece()));
		updateLabel("EPOCH", epochString);
		updateLabel("TLE AGE", ageString);

		// Paramètres Orbitaux
		updateLabel("(MEAN MOTION)'", String.format("%.4e", tle.firstDerivMeanMotion()));
		updateLabel("(MEAN MOTION)''", String.format("%.4e", tle.secondDerivMeanMotion()));
		updateLabel("INCLINATION", String.format("%.4f degs", tle.inclination()));
		updateLabel("LONGITUDE OF ASC. NODE", String.format("%.4f degs", tle.rightAscension()));
		updateLabel("LONGITUDE OF PERIAPSIS",
				String.format("%.4f degs", (tle.rightAscension() + tle.argumentOfPerigee()) % 360.0));
		updateLabel("ECCENTRICITY", String.format("%.7f", tle.eccentricity()));
		updateLabel("ARG. OF PERIAPSIS", String.format("%.4f degs", tle.argumentOfPerigee()));
		updateLabel("MEAN ANOMALY", String.format("%.4f degs", tle.meanAnomaly()));
		updateLabel("MEAN MOTION", String.format("%.8f rev/day", tle.meanMotion()));

		// Résultats
		updateLabel("ORBITAL PERIOD", String.format("%.4f secs (%s)", init.T(), periodString));
		updateLabel("SEMI MAJOR AXIS", String.format("%.0f m", init.a()));
		updateLabel("ALTITUDE AT APOGEE", String.format("%.4f m", init.apoAlt()));
		updateLabel("ALTITUDE AT PERIGEE", String.format("%.4f m", init.periAlt()));
		updateLabel("ALTITUDE AT EPOCH", String.format("%.4f m", init.epochAlt()));
		updateLabel("SPEED @ AP", String.format("%.4f m/s", init.apoSpd()));
		updateLabel("SPEED @ PE", String.format("%.4f m/s", init.periSpd()));
		updateLabel("SPEED @ EPOCH", String.format("%.4f m/s", init.epochSpd()));

		// Temps Réel
		updateLabel("DATE", instantString);
		updateLabel("X Coord", String.format("%.2f m", instant.coords3d().x()));
		updateLabel("Y Coord", String.format("%.2f m", instant.coords3d().y()));
		updateLabel("Z Coord", String.format("%.2f m", instant.coords3d().z()));
		updateLabel("GROUND COORDINATES",
				String.format("%.5f, %.5f", instant.geoCoords().lat(), instant.geoCoords().lng()));
		updateLabel("ALTITUDE", String.format("%.0f m", instant.geoCoords().altitude()));
		updateLabel("VELOCITY", String.format("%.2f m/s", instant.speed()));
		updateLabel("LOCAL GRAVITY", String.format("%.2f m/s²", instant.localGravity()));
	}

	// Getters pour que le contrôleur puisse interagir avec la vue Prédiction
	/*
	 * public JTextField getLatField() {
	 * return latField;
	 * }
	 * 
	 * public JTextField getLngField() {
	 * return lngField;
	 * }
	 * 
	 * public JTextField getAltField() {
	 * return altField;
	 * }
	 */

	public Station getSelectedStation() {
		return (Station) stationComboBox.getSelectedItem();
	}

	public JComboBox<Station> getStationComboBox() {
		return stationComboBox;
	}

	public JTextField getMaskField() {
		return maskField;
	}

	public JTextField getDaysField() {
		return daysField;
	}

	public JButton getPredictBtn() {
		return predictBtn;
	}

	public DefaultTableModel getPassesTableModel() {
		return passesTableModel;
	}

	public JTable getPassesTable() {
		return passesTable;
	}
}
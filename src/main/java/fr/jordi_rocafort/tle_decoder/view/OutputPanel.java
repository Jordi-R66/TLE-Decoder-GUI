package fr.jordi_rocafort.tle_decoder.view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import fr.jordi_rocafort.tle_decoder.model.data.DynamicValues;
import fr.jordi_rocafort.tle_decoder.model.data.StaticValues;
import fr.jordi_rocafort.tle_decoder.model.data.TLE;
import fr.jordi_rocafort.tle_decoder.util.TimeUtils;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

public class OutputPanel extends JPanel {

	private static OutputPanel instance = null;

	// Dictionnaire pour stocker et mettre à jour rapidement les labels sans recréer
	// l'UI
	private Map<String, JLabel> valueLabels;

	public static OutputPanel getInstance() {
		if (instance == null) {
			instance = new OutputPanel();
		}
		return instance;
	}

	public OutputPanel() {
		this.setLayout(new BorderLayout());
		this.setBorder(BorderFactory.createTitledBorder("Output"));

		valueLabels = new HashMap<>();

		// Le panneau principal qui contiendra les catégories
		JPanel contentPanel = new JPanel();
		contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

		// Création de l'arborescence UI une seule fois
		contentPanel.add(createCategoryPanel("Identification", new String[] {
				"Object name", "NORAD ID", "COSPAR ID", "EPOCH", "TLE AGE"
		}));
		contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));

		contentPanel.add(createCategoryPanel("Paramètres Orbitaux (TLE)", new String[] {
				"(MEAN MOTION)'", "(MEAN MOTION)''", "INCLINATION", "LONGITUDE OF ASC. NODE",
				"LONGITUDE OF PERIAPSIS", "ECCENTRICITY", "ARG. OF PERIAPSIS", "MEAN ANOMALY", "MEAN MOTION"
		}));
		contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));

		contentPanel.add(createCategoryPanel("Résultats Initiaux", new String[] {
				"ORBITAL PERIOD", "SEMI MAJOR AXIS", "ALTITUDE AT APOGEE",
				"ALTITUDE AT PERIGEE", "ALTITUDE AT EPOCH", "SPEED @ AP", "SPEED @ PE", "SPEED @ EPOCH"
		}));
		contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));

		contentPanel.add(createCategoryPanel("Actuellement (Temps Réel)", new String[] {
				"DATE", "X Coord", "Y Coord", "Z Coord", "GROUND COORDINATES", "ALTITUDE", "VELOCITY"
		}));

		JScrollPane scrollPane = new JScrollPane(contentPanel);
		scrollPane.setBorder(null);
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);

		this.add(scrollPane, BorderLayout.CENTER);
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

			// Label de la propriété (gauche, gris)
			JLabel keyLabel = new JLabel(key);
			keyLabel.setForeground(Color.GRAY);
			keyLabel.setFont(keyLabel.getFont().deriveFont(11f));
			gbc.gridx = 0;
			gbc.gridy = i;
			gbc.weightx = 0.0;
			panel.add(keyLabel, gbc);

			// Label de la valeur (droite, blanc/clair)
			JLabel valueLabel = new JLabel("-"); // Valeur par défaut avant décodage
			valueLabel.setFont(valueLabel.getFont().deriveFont(Font.BOLD, 12f));
			valueLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
			valueLabel.setToolTipText("Double-cliquez pour copier");

			// Événement de copie au double-clic
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

			// On stocke la référence du label pour le mettre à jour plus tard très
			// rapidement
			valueLabels.put(key, valueLabel);

			gbc.gridx = 1;
			gbc.gridy = i;
			gbc.weightx = 1.0;
			panel.add(valueLabel, gbc);
		}
		return panel;
	}

	// Met à jour le texte du label s'il existe
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
				String.format("%.4f degs", tle.rightAscension() + tle.argumentOfPerigee()));
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
	}
}
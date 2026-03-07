package fr.jordi_rocafort.tle_decoder.view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import fr.jordi_rocafort.tle_decoder.model.data.DynamicValues;
import fr.jordi_rocafort.tle_decoder.model.data.StaticValues;
import fr.jordi_rocafort.tle_decoder.model.data.TLE;
import fr.jordi_rocafort.tle_decoder.util.TimeUtils;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class OutputPanel extends JPanel {

	private DefaultListModel<PropertyItem> model;
	private static OutputPanel instance = null;

	public static OutputPanel getInstance() {
		if (instance == null) {
			instance = new OutputPanel();
		}

		return instance;
	}

	public void showData(TLE tle, StaticValues init, DynamicValues instant, long currentTimestamp) {
		model.clear();
		String epochString = TimeUtils.timestampToDateString(init.epochTimestamp());
		String periodString = TimeUtils.periodToString(init.T());
		String instantString = TimeUtils.timestampToDateString(currentTimestamp);
		String ageString = TimeUtils.deltaTimeToString(instant.deltaTime());

		model.addElement(new PropertyItem("Object name", tle.name()));
		model.addElement(new PropertyItem("NORAD ID", String.format("%d%c", tle.noradId(), tle.classification())));
		model.addElement(new PropertyItem("COSPAR ID", String.format("%d %03d %s", tle.cosparYear(), tle.cosparLaunchNum(), tle.cosparPiece())));
		model.addElement(new PropertyItem("EPOCH", epochString));
		model.addElement(new PropertyItem("TLE AGE", ageString));
		model.addElement(new PropertyItem("(MEAN MOTION)'", String.format("%.4e", tle.firstDerivMeanMotion())));
		model.addElement(new PropertyItem("(MEAN MOTION)''", String.format("%.4e", tle.secondDerivMeanMotion())));

		model.addElement(new PropertyItem("INCLINATION", String.format("%.4f degs", tle.inclination())));
		model.addElement(new PropertyItem("LONGITUDE OF ASC. NODE", String.format("%.4f degs", tle.rightAscension())));
		model.addElement(new PropertyItem("LONGITUDE OF PERIAPSIS", String.format("%.4f degs", tle.rightAscension() + tle.argumentOfPerigee())));
		model.addElement(new PropertyItem("ECCENTRICITY", String.format("%.7f", tle.eccentricity())));
		model.addElement(new PropertyItem("ARG. OF PERIAPSIS", String.format("%.4f degs", tle.argumentOfPerigee())));
		model.addElement(new PropertyItem("MEAN ANOMALY", String.format("%.4f degs", tle.meanAnomaly())));
		model.addElement(new PropertyItem("MEAN MOTION", String.format("%.8f rev/day", tle.meanMotion())));

		model.addElement(new PropertyItem("ORBITAL PERIOD", String.format("%.4f secs (%s)", init.T(), periodString)));
		model.addElement(new PropertyItem("SEMI MAJOR AXIS", String.format("%.0f m", init.a())));

		model.addElement(new PropertyItem("ALTITUDE AT APOGEE", String.format("%.4f m", init.apoAlt())));
		model.addElement(new PropertyItem("ALTITUDE AT PERIGEE", String.format("%.4f m", init.periAlt())));
		model.addElement(new PropertyItem("ALTITUDE AT EPOCH", String.format("%.4f m", init.epochAlt())));

		model.addElement(new PropertyItem("SPEED @ AP", String.format("%.4f m/s", init.apoSpd())));
		model.addElement(new PropertyItem("SPEED @ PE", String.format("%.4f m/s", init.periSpd())));
		model.addElement(new PropertyItem("SPEED @ EPOCH", String.format("%.4f m/s", init.epochSpd())));

		model.addElement(new PropertyItem("DATE", instantString));
		model.addElement(new PropertyItem("X Coord", String.format("%.2f m", instant.coords3d().x())));
		model.addElement(new PropertyItem("Y Coord", String.format("%.2f m", instant.coords3d().y())));
		model.addElement(new PropertyItem("Z Coord", String.format("%.2f m", instant.coords3d().z())));

		model.addElement(new PropertyItem("GROUND COORDINATES", String.format("%.5f, %.5f", instant.geoCoords().lat(), instant.geoCoords().lng())));
		model.addElement(new PropertyItem("ALTITUDE", String.format("%.0f m", instant.geoCoords().altitude())));

		model.addElement(new PropertyItem("VELOCITY", String.format("%.2f m/s", instant.speed())));
		return;
	}

	public OutputPanel() {
		this.setLayout(new BorderLayout());
		this.setBorder(BorderFactory.createTitledBorder("Output"));

		// Modèle de données pour la liste
		model = new DefaultListModel<>();

		// Création de la liste
		JList<PropertyItem> list = new JList<>(model);
		list.setCellRenderer(new PropertyListRenderer());
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setBackground(UIManager.getColor("Panel.background"));

		// Ajout de l'événement double-clic pour copier la valeur
		list.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					int index = list.locationToIndex(e.getPoint());
					if (index >= 0) {
						PropertyItem item = model.getElementAt(index);

						// Copie dans le presse-papiers du système
						StringSelection stringSelection = new StringSelection(item.value);
						Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
						clipboard.setContents(stringSelection, null);
					}
				}
			}
		});

		JScrollPane scrollPane = new JScrollPane(list);
		scrollPane.setBorder(null);
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);

		this.add(scrollPane, BorderLayout.CENTER);
	}

	// --- CLASSE DE DONNÉES ---
	private static class PropertyItem {
		String key;
		String value;

		public PropertyItem(String key, String value) {
			this.key = key;
			this.value = value;
		}

		public void setValue(String val) { value = val; }
	}

	// --- LE RENDU PERSONNALISÉ POUR CHAQUE CELLULE ---
	private static class PropertyListRenderer extends JPanel implements ListCellRenderer<PropertyItem> {
		private JLabel keyLabel;
		private JLabel valueLabel;

		public PropertyListRenderer() {
			this.setLayout(new GridLayout(2, 1, 0, 2));
			this.setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("Component.borderColor")),
					new EmptyBorder(8, 10, 8, 10)));

			keyLabel = new JLabel();
			keyLabel.setFont(keyLabel.getFont().deriveFont(10f));
			keyLabel.setForeground(Color.GRAY);

			valueLabel = new JLabel();
			valueLabel.setFont(valueLabel.getFont().deriveFont(Font.BOLD, 13f));

			this.add(keyLabel);
			this.add(valueLabel);
		}

		@Override
		public Component getListCellRendererComponent(JList<? extends PropertyItem> list, PropertyItem item, int index,
				boolean isSelected, boolean cellHasFocus) {
			keyLabel.setText(item.key.toUpperCase());
			valueLabel.setText(item.value);

			if (isSelected) {
				this.setBackground(UIManager.getColor("List.selectionBackground"));
				valueLabel.setForeground(UIManager.getColor("List.selectionForeground"));
			} else {
				this.setBackground(list.getBackground());
				valueLabel.setForeground(list.getForeground());
			}
			return this;
		}
	}
}
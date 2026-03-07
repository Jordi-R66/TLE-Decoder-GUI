package fr.jordi_rocafort.tle_decoder.view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import fr.jordi_rocafort.tle_decoder.model.data.DynamicValues;
import fr.jordi_rocafort.tle_decoder.model.data.StaticValues;
import fr.jordi_rocafort.tle_decoder.model.data.TLE;

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
		return;
	}

	public OutputPanel() {
		this.setLayout(new BorderLayout());
		this.setBorder(BorderFactory.createTitledBorder("Output"));

		// Modèle de données pour la liste
		model = new DefaultListModel<>();
		model.addElement(new PropertyItem("Object name", "ROBUSTA-3A"));
		model.addElement(new PropertyItem("NORAD ID", "60243U"));
		model.addElement(new PropertyItem("EPOCH", "2026-03-05 13:24:53 UTC"));
		model.addElement(new PropertyItem("INCLINATION", "61.9866 degs"));
		model.addElement(new PropertyItem("ECCENTRICITY", "0.0052320"));
		model.addElement(new PropertyItem("SEMI MAJOR AXIS", "6914449 m"));
		model.addElement(new PropertyItem("VELOCITY", "7626.79 m/s"));

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
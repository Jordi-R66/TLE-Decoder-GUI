package fr.jordi_rocafort.tle_decoder.view;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

public class InputPanel extends JPanel {
	private CardLayout cardLayout;
	private JPanel cards;
	private JButton loadBtn;
	private JComboBox<String> satCombo;
	private JTextArea tleTextArea;
	private JButton confirmBtn;

	private JRadioButton fileRadio, manualRadio;

	public InputPanel() {
		this.setLayout(new BorderLayout());

		// Largeur augmentée à 580px pour garantir la place de 70 caractères monospaced
		this.setBorder(BorderFactory.createTitledBorder("Input"));

		JPanel topMenu = new JPanel(new FlowLayout(FlowLayout.LEFT));
		fileRadio = new JRadioButton("From File", true);
		manualRadio = new JRadioButton("3LE Block");

		ButtonGroup bg = new ButtonGroup();
		bg.add(fileRadio);
		bg.add(manualRadio);
		topMenu.add(fileRadio);
		topMenu.add(manualRadio);

		cardLayout = new CardLayout();
		cards = new JPanel(cardLayout);

		// --- Carte 1 : Fichier ---
		JPanel filePanel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(10, 10, 10, 10);
		gbc.weightx = 1.0;

		loadBtn = new JButton("Open File");
		satCombo = new JComboBox<>(new String[] { "Select a NORAD ID" });
		satCombo.setEnabled(false);

		gbc.gridy = 0;
		filePanel.add(loadBtn, gbc);
		gbc.gridy = 1;
		filePanel.add(satCombo, gbc);
		cards.add(filePanel, "FILE");

		// --- Carte 2 : Saisie manuelle ---
		tleTextArea = new JTextArea(3, 70);
		tleTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		tleTextArea.setLineWrap(false);

		// Application du filtre strict (3 lignes, 70 char max)
		AbstractDocument doc = (AbstractDocument) tleTextArea.getDocument();
		doc.setDocumentFilter(new TleDocumentFilter());

		// On masque les scrollbars puisqu'elles ne devraient jamais servir avec le
		// filtre
		JScrollPane scrollPane = new JScrollPane(tleTextArea);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
		cards.add(scrollPane, "MANUAL");

		fileRadio.addActionListener(e -> cardLayout.show(cards, "FILE"));
		manualRadio.addActionListener(e -> cardLayout.show(cards, "MANUAL"));
		cardLayout.show(cards, "FILE");

		confirmBtn = new JButton("Confirm");

		this.add(topMenu, BorderLayout.NORTH);
		this.add(cards, BorderLayout.CENTER);
		this.add(confirmBtn, BorderLayout.SOUTH);
	}

	/**
	 * Filtre interne pour limiter strictement à 3 lignes de 70 caractères.
	 */
	private class TleDocumentFilter extends DocumentFilter {
		private static final int MAX_LINES = 3;
		private static final int MAX_CHARS_PER_LINE = 70;

		@Override
		public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
				throws BadLocationException {
			if (isValid(fb, offset, 0, string)) {
				super.insertString(fb, offset, string, attr);
			}
		}

		@Override
		public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
				throws BadLocationException {
			if (isValid(fb, offset, length, text)) {
				super.replace(fb, offset, length, text, attrs);
			}
		}

		private boolean isValid(FilterBypass fb, int offset, int length, String text) throws BadLocationException {
			if (text == null)
				return true;

			// Reconstruire le texte tel qu'il serait après l'insertion/remplacement
			String currentContent = fb.getDocument().getText(0, fb.getDocument().getLength());
			String beforeInsert = currentContent.substring(0, offset);
			String afterInsert = currentContent.substring(offset + length);
			String proposedContent = beforeInsert + text + afterInsert;

			// Normaliser les retours à la ligne (utile en cas de copier-coller depuis
			// Windows)
			proposedContent = proposedContent.replace("\r\n", "\n").replace("\r", "\n");

			String[] lines = proposedContent.split("\n", -1);

			// Vérification 1 : Pas plus de 3 lignes
			if (lines.length > MAX_LINES) {
				return false;
			}

			// Vérification 2 : Pas plus de 70 caractères par ligne
			for (String line : lines) {
				if (line.length() > MAX_CHARS_PER_LINE) {
					return false;
				}
			}

			return true;
		}
	}

	public JButton getLoadBtn() {
		return loadBtn;
	}

	public JComboBox<String> getSatCombo() {
		return satCombo;
	}

	public JTextArea getTleTextArea() {
		return tleTextArea;
	}

	public JButton getConfirmBtn() {
		return confirmBtn;
	}

	public JRadioButton getFileRadio() {
		return fileRadio;
	}

	public JRadioButton getManualRadio() {
		return manualRadio;
	}
}
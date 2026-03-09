package fr.jordi_rocafort.tle_decoder.controller;

import fr.jordi_rocafort.tle_decoder.view.InputPanel;
import fr.jordi_rocafort.tle_decoder.model.parser.*;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FileSelectionController implements ActionListener {
	private InputPanel inputPanel;
	private File selectedFile;
	private static FileSelectionController instance = null;

	// Associe le texte affiché directement au NORAD ID
	private HashMap<String, Integer> blockStrings;

	// Garde la liste complète en mémoire pour le filtrage
	private List<BlockInformation> currentLoadedBlocks;

	public FileSelectionController() {
		this.inputPanel = InputPanel.getInstance();
		this.inputPanel.getLoadBtn().addActionListener(this);

		blockStrings = new HashMap<>();
		currentLoadedBlocks = new ArrayList<>();
		FileSelectionController.instance = this;

		// On écoute le champ de recherche
		this.inputPanel.getSearchField().getDocument().addDocumentListener(new DocumentListener() {
			public void insertUpdate(DocumentEvent e) {
				filter();
			}

			public void removeUpdate(DocumentEvent e) {
				filter();
			}

			public void changedUpdate(DocumentEvent e) {
				filter();
			}

			private void filter() {
				updateComboFilter(inputPanel.getSearchField().getText());
			}
		});
	}

	public static FileSelectionController getInstance() {
		if (instance == null) {
			new FileSelectionController();
		}
		return instance;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Sélectionner un fichier TLE");
		fileChooser.setFileFilter(new FileNameExtensionFilter("Fichiers texte/TLE (*.txt, *.tle)", "txt", "tle"));

		int userSelection = fileChooser.showOpenDialog(inputPanel);

		if (userSelection == JFileChooser.APPROVE_OPTION) {
			selectedFile = fileChooser.getSelectedFile();
			inputPanel.getLoadBtn().setText(selectedFile.getName());

			try {
				// On charge tout une seule fois
				currentLoadedBlocks = TleFileManager.getAllNoradIDs(selectedFile.getPath());

				// On active la recherche et on la vide (ce qui déclenche l'affichage complet)
				inputPanel.getSearchField().setEnabled(true);
				inputPanel.getSearchField().setText("");

				System.out.printf("%d TLEs trouvées dans %s\n", currentLoadedBlocks.size(),
						selectedFile.getAbsolutePath());
			} catch (Exception exception) {
				exception.printStackTrace();
			}
		}
	}

	/**
	 * Filtre dynamiquement les éléments de la ComboBox
	 */
	private void updateComboFilter(String searchText) {
		if (currentLoadedBlocks == null || currentLoadedBlocks.isEmpty())
			return;

		JComboBox<String> combo = inputPanel.getSatCombo();

		combo.setEnabled(false);
		combo.removeAllItems();
		blockStrings.clear();

		String lowerSearch = searchText.toLowerCase();
		combo.addItem("Select a satellite");

		for (BlockInformation blockInfo : currentLoadedBlocks) {
			// Si le champ de recherche est vide OU que le nom/ID correspond
			if (searchText.isEmpty() || blockInfo.toString().toLowerCase().contains(lowerSearch)) {
				combo.addItem(blockInfo.toString());
				// On sauvegarde le noradId exact, pas l'index !
				blockStrings.put(blockInfo.toString(), blockInfo.noradId());
			}
		}

		combo.setEnabled(true);

		// Si on a filtré, on sélectionne le premier résultat valide pour gagner du
		// temps
		if (!searchText.isEmpty() && combo.getItemCount() > 1) {
			combo.setSelectedIndex(1);
		}
	}

	public HashMap<String, Integer> getAssociations() {
		return blockStrings;
	}

	public InputPanel getInputPanel() {
		return inputPanel;
	}

	public String getSelectedFilePath() {
		return (selectedFile != null) ? selectedFile.getAbsolutePath() : null;
	}
}
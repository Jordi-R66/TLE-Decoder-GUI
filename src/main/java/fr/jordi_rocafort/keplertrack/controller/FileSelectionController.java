package fr.jordi_rocafort.keplertrack.controller;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import fr.jordi_rocafort.keplertrack.model.tle.*;
import fr.jordi_rocafort.keplertrack.view.InputPanel;

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

		this.inputPanel.getDownloadBtn().addActionListener(e -> startDownloadTask());
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

		File defaultDir = new File("TLEs");

		if (!defaultDir.exists()) {
			defaultDir.mkdirs(); // Crée le dossier s'il n'existe pas encore
		}

		fileChooser.setCurrentDirectory(defaultDir);
		fileChooser.setDialogTitle("Sélectionner un fichier TLE");
		fileChooser.setFileFilter(new FileNameExtensionFilter("Fichiers texte/TLE (*.txt, *.tle)", "txt", "tle"));

		int userSelection = fileChooser.showOpenDialog(inputPanel);

		if (userSelection == JFileChooser.APPROVE_OPTION) {
			selectedFile = fileChooser.getSelectedFile();
			inputPanel.getLoadBtn().setText(selectedFile.getName());

			try {
				// On charge tout une seule fois
				currentLoadedBlocks = TleFileManager.getAllNoradIDs(selectedFile.getPath());

				// On active la recherche et on la vide
				inputPanel.getSearchField().setEnabled(true);
				inputPanel.getSearchField().setText("");
				updateComboFilter("");

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

	/**
	 * Lance le téléchargement en arrière-plan pour ne pas figer l'interface
	 * (SwingWorker).
	 */
	private void startDownloadTask() {
		JButton btn = inputPanel.getDownloadBtn();
		btn.setEnabled(false);
		btn.setText("Downloading...");

		SwingWorker<Void, Void> worker = new SwingWorker<>() {
			@Override
			protected Void doInBackground() throws Exception {
				TleDownloader.downloadAndMergeAllTles(4);
				return null;
			}

			@Override
			protected void done() {
				btn.setEnabled(true);
				btn.setText("Download TLEs");

				TleFileManager.getAssociations().clear();

				reloadCurrentFile();

				DecodeController.getInstance().refreshActiveSatellite();

				JOptionPane.showMessageDialog(inputPanel, "Mise à jour des TLE terminée !", "Succès",
						JOptionPane.INFORMATION_MESSAGE);
			}
		};
		worker.execute();
	}

	/**
	 * Recharge les TLEs du fichier actuellement sélectionné.
	 */
	public void reloadCurrentFile() {
		if (selectedFile != null) {
			try {
				currentLoadedBlocks = TleFileManager.getAllNoradIDs(selectedFile.getAbsolutePath());
				// On applique à nouveau la recherche actuelle pour rafraîchir la liste
				updateComboFilter(inputPanel.getSearchField().getText());
			} catch (Exception exception) {
				exception.printStackTrace();
			}
		}
	}
}
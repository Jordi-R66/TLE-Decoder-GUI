package fr.jordi_rocafort.tle_decoder.controller;

import fr.jordi_rocafort.tle_decoder.view.InputPanel;
import fr.jordi_rocafort.tle_decoder.model.parser.*;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.HashMap;

public class FileSelectionController implements ActionListener {
	private InputPanel inputPanel;
	private File selectedFile;

	private static FileSelectionController instance = null;

	private HashMap<String, Integer> blockStrings;

	public FileSelectionController() {
		this.inputPanel = InputPanel.getInstance();
		// On attache ce contrôleur au bouton
		this.inputPanel.getLoadBtn().addActionListener(this);
		blockStrings = new HashMap<>();
		FileSelectionController.instance = this;
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
			inputPanel.getLoadBtn().setText(selectedFile.getName()); // Affiche le nom du fichier sur le bouton

			JComboBox<String> combo = inputPanel.getSatCombo();
			combo.removeAllItems();
			combo.addItem("Select a satellite");
			blockStrings.clear();

			int compteur = 0;

			try {
				for (BlockInformation blockInfo : TleFileManager.getAllNoradIDs(selectedFile.getPath())) {
					combo.addItem(blockInfo.toString());
					blockStrings.put(blockInfo.toString(), compteur++);
				}

					System.out.printf("%d TLEs trouvées dans %s\n", compteur, selectedFile.getAbsolutePath());
			} catch (Exception exception) {
				exception.printStackTrace();
			}

			combo.setEnabled(true);
		}
	}

	public HashMap<String, Integer> getAssociations() { return blockStrings; }
	public InputPanel getInputPanel() { return inputPanel; }

	public String getSelectedFilePath() {
		String output = null;

		if (selectedFile != null) {
			output = selectedFile.getAbsolutePath();
		}

		return output;
	}
}
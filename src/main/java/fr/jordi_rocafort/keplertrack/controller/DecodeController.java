package fr.jordi_rocafort.keplertrack.controller;

import javax.swing.*;

import fr.jordi_rocafort.keplertrack.model.data.*;
import fr.jordi_rocafort.keplertrack.model.physics.OrbitPropagator;
import fr.jordi_rocafort.keplertrack.model.tle.TleFileManager;
import fr.jordi_rocafort.keplertrack.model.tle.ITleParser;
import fr.jordi_rocafort.keplertrack.view.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class DecodeController implements ActionListener {
	private InputPanel inputPanel;
	private FileSelectionController fileController;
	private TLE tle;
	private StaticValues init;
	private SimulationEngine simulationEngine;
	private int currentNoradId = -1;
	private String currentFilePath = null;
	private boolean isTrackingFromFile = false;

	private static DecodeController instance = null;

	public static DecodeController getInstance() {
		if (instance == null) {
			instance = new DecodeController();
		}

		return instance;
	}

	public DecodeController() {
		this.fileController = FileSelectionController.getInstance();
		this.inputPanel = InputPanel.getInstance();
		this.simulationEngine = new SimulationEngine();

		for (ActionListener al : this.inputPanel.getConfirmBtn().getActionListeners()) {
			this.inputPanel.getConfirmBtn().removeActionListener(al);
		}

		// On attache ce contrôleur au bouton Confirmer
		this.inputPanel.getConfirmBtn().addActionListener(this);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// 1. Vérifier quel mode d'entrée est sélectionné
		if (inputPanel.getFileRadio().isSelected()) {
			// Mode Fichier
			if (fileController.getSelectedFilePath() == null) {
				JOptionPane.showMessageDialog(inputPanel, "Veuillez d'abord sélectionner un fichier.", "Erreur",
						JOptionPane.WARNING_MESSAGE);
				return;
			}

			if (inputPanel.getSatCombo().getSelectedIndex() <= 0) {
				JOptionPane.showMessageDialog(inputPanel, "Veuillez sélectionner un satellite dans la liste.", "Erreur",
						JOptionPane.WARNING_MESSAGE);
				return;
			}

			try {
				String filePath = fileController.getSelectedFilePath();
				String selectedSat = (String) inputPanel.getSatCombo().getSelectedItem();
				System.out.println("Lancement du décodage pour le fichier : " + filePath + " | Sat: " + selectedSat);

				int noradId = FileSelectionController.getInstance().getAssociations().get(selectedSat);

				this.currentNoradId = noradId;
				this.currentFilePath = filePath;
				this.isTrackingFromFile = true;

				tle = TleFileManager.getSingleTLE(filePath, noradId);
				init = OrbitPropagator.computeStaticPhase(tle);
			} catch (Exception exception) {
				exception.printStackTrace();
			}

		} else {
			// Mode Manuel
			String tleText = inputPanel.getTleTextArea().getText().trim();

			this.isTrackingFromFile = false;

			if (tleText.isEmpty()) {
				JOptionPane.showMessageDialog(inputPanel, "Le bloc TLE est vide.", "Erreur",
						JOptionPane.WARNING_MESSAGE);
				return;
			}

			System.out.println("Lancement du décodage pour le texte manuel :\n" + tleText);

			TleLegacyBlock tempBlock = TleLegacyBlock.fromRawBlock(tleText);

			if (tempBlock != null) {
				tle = ITleParser.parseLines(tempBlock);
				init = OrbitPropagator.computeStaticPhase(tle);
			} else {
				JOptionPane.showMessageDialog(inputPanel, "Vérifiez avoir saisie une 3LE", "Erreur",
						JOptionPane.WARNING_MESSAGE);
				return;
			}
		}

		// 2. Lancement en temps réel
		if (this.tle != null && this.init != null) {
			simulationEngine.startSimulation(tle, init);
		}
	}

	/**
	 * Récupère la TLE mise à jour et relance la simulation sur le satellite en
	 * cours.
	 */
	public void refreshActiveSatellite() {
		if (isTrackingFromFile && currentFilePath != null && currentNoradId != -1) {
			try {
				System.out.println("Rafraîchissement de la trajectoire pour le satellite : " + currentNoradId);
				this.tle = TleFileManager.getSingleTLE(currentFilePath, currentNoradId);
				this.init = OrbitPropagator.computeStaticPhase(tle);

				this.simulationEngine.startSimulation(tle, init);
			} catch (Exception e) {
				System.err.println("Impossible de rafraîchir le satellite : " + e.getMessage());
			}
		}
	}
}
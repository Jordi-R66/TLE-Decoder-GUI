package fr.jordi_rocafort.keplertrack.controller;

import fr.jordi_rocafort.keplertrack.model.data.GeoCoords;
import fr.jordi_rocafort.keplertrack.model.data.SatellitePass;
import fr.jordi_rocafort.keplertrack.model.data.Station;
import fr.jordi_rocafort.keplertrack.model.data.TLE;
import fr.jordi_rocafort.keplertrack.model.physics.PassPredictor;
import fr.jordi_rocafort.keplertrack.util.StationManager;
import fr.jordi_rocafort.keplertrack.view.OutputPanel;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionEvent;
import java.util.List;

public class PredictionController {

	private final OutputPanel outputPanel;
	private TLE currentTle; // Le satellite actuellement suivi

	public PredictionController(OutputPanel outputPanel) {
		this.outputPanel = outputPanel;
		initController();
	}

	// Méthode pour mettre à jour le satellite cible quand l'utilisateur en change
	public void setCurrentTle(TLE tle) {
		this.currentTle = tle;
	}

	private void initController() {
		// 1. Assurer que le fichier existe et charger les stations
		List<Station> stations = StationManager.loadStations();

		// 2. Remplir la ComboBox de l'OutputPanel
		JComboBox<Station> combo = outputPanel.getStationComboBox();
		combo.removeAllItems();
		for (Station s : stations) {
			combo.addItem(s);
		}

		// 3. Listener du bouton
		outputPanel.getPredictBtn().addActionListener(this::handlePredictionRequest);
	}

	private void handlePredictionRequest(ActionEvent e) {
		if (currentTle == null) {
			JOptionPane.showMessageDialog(outputPanel, "Please first select a TLE", "Erreur",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		try {
			// 1. Récupération et parsing des paramètres de l'UI
			/*
			 * double lat =
			 * Double.parseDouble(outputPanel.getLatField().getText().replace(",", "."));
			 * double lng =
			 * Double.parseDouble(outputPanel.getLngField().getText().replace(",", "."));
			 * double alt =
			 * Double.parseDouble(outputPanel.getAltField().getText().replace(",", "."));
			 */
			Station selectedStation = outputPanel.getSelectedStation();
			if (selectedStation == null) {
				JOptionPane.showMessageDialog(outputPanel, "Veuillez sélectionner un lieu d'observation.");
				return;
			}
			GeoCoords stationCoords = selectedStation.coords();
			double mask = Double.parseDouble(outputPanel.getMaskField().getText().replace(",", "."));
			int days = Integer.parseInt(outputPanel.getDaysField().getText());

			GeoCoords station = new GeoCoords(stationCoords.lat(), stationCoords.lng(), stationCoords.altitude());

			// 2. Paramètres temporels
			long startSecs = System.currentTimeMillis() / 1000L;
			long endSecs = startSecs + (days * 24L * 60L * 60L); // Jours en secondes
			long stepSecs = 60L; // Pas de 1 minute pour la recherche grossière

			// 3. Préparation de l'UI pour l'attente
			outputPanel.getPredictBtn().setEnabled(false);
			outputPanel.getPredictBtn().setText("Computing...");

			// Vider le tableau
			DefaultTableModel tableModel = outputPanel.getPassesTableModel();
			tableModel.setRowCount(0);

			// 4. Lancement du calcul en arrière-plan via SwingWorker
			SwingWorker<List<SatellitePass>, Void> worker = new SwingWorker<>() {
				@Override
				protected List<SatellitePass> doInBackground() throws Exception {
					// C'EST ICI QUE LE CPU TRAVAILLE (Hors de l'UI)
					return PassPredictor.predictPasses(currentTle, station, startSecs, endSecs, stepSecs, mask);
				}

				@Override
				protected void done() {
					try {
						// Le calcul est fini, on récupère le résultat
						List<SatellitePass> passes = get();
						populateTable(passes);
					} catch (Exception ex) {
						JOptionPane.showMessageDialog(outputPanel, "Failure on computation : " + ex.getMessage(),
								"Failure", JOptionPane.ERROR_MESSAGE);
						ex.printStackTrace();
					} finally {
						// Rétablir le bouton
						outputPanel.getPredictBtn().setEnabled(true);
						outputPanel.getPredictBtn().setText("Compute Passes");
					}
				}
			};

			worker.execute(); // Démarre le thread

		} catch (NumberFormatException ex) {
			JOptionPane.showMessageDialog(outputPanel, "Veuillez vérifier le format des nombres saisis.",
					"Erreur de saisie", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void populateTable(List<SatellitePass> passes) {
		DefaultTableModel tableModel = outputPanel.getPassesTableModel();
		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.systemDefault());
		DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

		LocalDate previousDate = null;

		for (SatellitePass pass : passes) {
			Instant aosInstant = Instant.ofEpochSecond(pass.aosTime());
			LocalDate currentDate = LocalDate.ofInstant(aosInstant, ZoneId.systemDefault());

			String dateStr = "";
			if (previousDate == null || !currentDate.equals(previousDate)) {
				dateStr = dateFormatter.format(aosInstant);
				previousDate = currentDate;

				if (tableModel.getRowCount() > 0) {
					tableModel.addRow(new Object[] { dateStr, "", "", "", "" });
				}
			}

			String aosTimeStr = timeFormatter.format(aosInstant);
			String tcaTimeStr = timeFormatter.format(Instant.ofEpochSecond(pass.tcaTime()));
			String losTimeStr = timeFormatter.format(Instant.ofEpochSecond(pass.losTime()));

			long durationSecs = pass.losTime() - pass.aosTime();

			//String aosDisplay = dateStr.isEmpty() ? aosTimeStr : dateStr + "  " + aosTimeStr;
			String durationStr = String.format("%02d:%02d", durationSecs / 60, durationSecs % 60);
			String elevStr = String.format("%.1f°", pass.maxElevation());

			tableModel.addRow(new Object[] { aosTimeStr, tcaTimeStr, losTimeStr, durationStr, elevStr });
		}
	}
}

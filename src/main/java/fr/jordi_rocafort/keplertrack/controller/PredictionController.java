package fr.jordi_rocafort.keplertrack.controller;

import fr.jordi_rocafort.keplertrack.model.data.GeoCoords;
import fr.jordi_rocafort.keplertrack.model.data.SatellitePass;
import fr.jordi_rocafort.keplertrack.model.data.Station;
import fr.jordi_rocafort.keplertrack.model.data.TLE;
import fr.jordi_rocafort.keplertrack.model.physics.PassPredictor;
import fr.jordi_rocafort.keplertrack.util.StationManager;
import fr.jordi_rocafort.keplertrack.view.OutputPanel;
import fr.jordi_rocafort.keplertrack.view.PolarViewPanel; // <-- Ajout de l'import

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PredictionController {

	private final OutputPanel outputPanel;
	private TLE currentTle;

	// Dictionnaire pour lier un numéro de ligne du tableau à un objet SatellitePass
	private final Map<Integer, SatellitePass> rowToPassMap;

	public PredictionController(OutputPanel outputPanel) {
		this.outputPanel = outputPanel;
		this.rowToPassMap = new HashMap<>();
		initController();
	}

	public void setCurrentTle(TLE tle) {
		this.currentTle = tle;
	}

	private void initController() {
		List<Station> stations = StationManager.loadStations();

		JComboBox<Station> combo = outputPanel.getStationComboBox();
		combo.removeAllItems();
		for (Station s : stations) {
			combo.addItem(s);
		}

		// Listener du bouton de calcul
		outputPanel.getPredictBtn().addActionListener(this::handlePredictionRequest);

		// --- NOUVEAU : Listener de sélection du tableau ---
		outputPanel.getPassesTable().getSelectionModel().addListSelectionListener(e -> {
			// e.getValueIsAdjusting() permet de ne traiter le clic qu'une seule fois (quand
			// la souris est relâchée)
			if (!e.getValueIsAdjusting()) {
				int selectedRow = outputPanel.getPassesTable().getSelectedRow();

				// Si une ligne valide est cliquée et qu'elle correspond bien à un passage (pas
				// à une ligne de date)
				if (selectedRow >= 0 && rowToPassMap.containsKey(selectedRow)) {
					SatellitePass selectedPass = rowToPassMap.get(selectedRow);
					// On envoie le passage à la vue polaire !
					PolarViewPanel.getInstance().displayPass(selectedPass);
				}
			}
		});
	}

	private void handlePredictionRequest(ActionEvent e) {
		if (currentTle == null) {
			JOptionPane.showMessageDialog(outputPanel, "Please first select a TLE", "Erreur",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		try {
			Station selectedStation = outputPanel.getSelectedStation();
			if (selectedStation == null) {
				JOptionPane.showMessageDialog(outputPanel, "Veuillez sélectionner un lieu d'observation.");
				return;
			}
			GeoCoords stationCoords = selectedStation.coords();
			double mask = Double.parseDouble(outputPanel.getMaskField().getText().replace(",", "."));
			int days = Integer.parseInt(outputPanel.getDaysField().getText());

			GeoCoords station = new GeoCoords(stationCoords.lat(), stationCoords.lng(), stationCoords.altitude());

			long startSecs = System.currentTimeMillis() / 1000L;
			long endSecs = startSecs + (days * 24L * 60L * 60L);
			long stepSecs = 60L;

			outputPanel.getPredictBtn().setEnabled(false);
			outputPanel.getPredictBtn().setText("Computing...");

			DefaultTableModel tableModel = outputPanel.getPassesTableModel();
			tableModel.setRowCount(0);
			rowToPassMap.clear(); // On vide le cache des clics précédents

			// Si on relance un calcul, on efface aussi l'affichage de la vue polaire
			PolarViewPanel.getInstance().clear();

			SwingWorker<List<SatellitePass>, Void> worker = new SwingWorker<>() {
				@Override
				protected List<SatellitePass> doInBackground() throws Exception {
					return PassPredictor.predictPasses(currentTle, station, startSecs, endSecs, stepSecs, mask);
				}

				@Override
				protected void done() {
					try {
						List<SatellitePass> passes = get();
						populateTable(passes);
					} catch (Exception ex) {
						JOptionPane.showMessageDialog(outputPanel, "Failure on computation : " + ex.getMessage(),
								"Failure", JOptionPane.ERROR_MESSAGE);
						ex.printStackTrace();
					} finally {
						outputPanel.getPredictBtn().setEnabled(true);
						outputPanel.getPredictBtn().setText("Compute Passes");
					}
				}
			};

			worker.execute();

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
					// Note: On N'AJOUTE PAS cette ligne d'en-tête dans rowToPassMap.
					// Comme ça, cliquer sur la date ne fera rien.
				} else if (tableModel.getRowCount() == 0) {
					// Ajout visuel de la date pour le tout premier élément aussi
					tableModel.addRow(new Object[] { dateStr, "", "", "", "" });
				}
			}

			String aosTimeStr = timeFormatter.format(aosInstant);
			String tcaTimeStr = timeFormatter.format(Instant.ofEpochSecond(pass.tcaTime()));
			String losTimeStr = timeFormatter.format(Instant.ofEpochSecond(pass.losTime()));

			long durationSecs = pass.losTime() - pass.aosTime();

			String durationStr = String.format("%02d:%02d", durationSecs / 60, durationSecs % 60);
			String elevStr = String.format("%.1f°", pass.maxElevation());

			// On récupère l'index que la ligne va avoir (qui est égal au RowCount actuel
			// juste avant l'ajout)
			int rowIndex = tableModel.getRowCount();

			tableModel.addRow(new Object[] { aosTimeStr, tcaTimeStr, losTimeStr, durationStr, elevStr });

			// On lie cette ligne exacte au passage
			rowToPassMap.put(rowIndex, pass);
		}
	}
}
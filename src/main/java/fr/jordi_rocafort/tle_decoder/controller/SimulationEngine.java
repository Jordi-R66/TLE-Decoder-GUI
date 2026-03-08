package fr.jordi_rocafort.tle_decoder.controller;

import javax.swing.SwingUtilities;

import fr.jordi_rocafort.tle_decoder.model.data.DynamicValues;
import fr.jordi_rocafort.tle_decoder.model.data.StaticValues;
import fr.jordi_rocafort.tle_decoder.model.data.TLE;
import fr.jordi_rocafort.tle_decoder.model.physics.OrbitPropagator;
import fr.jordi_rocafort.tle_decoder.view.GroundTrackMapPanel;
import fr.jordi_rocafort.tle_decoder.view.OutputPanel;

import java.time.Instant;

public class SimulationEngine {
	private Thread simulationThread;
	private boolean isRunning = false;

	private TLE currentTle;
	private StaticValues currentInit;

	public void startSimulation(TLE tle, StaticValues init) {
		this.currentTle = tle;
		this.currentInit = init;

		// Arrêter le thread précédent s'il y en a un
		stopSimulation();

		isRunning = true;
		simulationThread = new Thread(() -> {
			while (isRunning) {
				long currentTimestamp = Instant.now().getEpochSecond();

				// 1. Calcul de la physique (Lourd, fait en arrière plan)
				DynamicValues instant = OrbitPropagator.computeDynamicPhase(currentTle, currentInit, currentTimestamp);

				// 2. Mise à jour de l'UI (Délégué au thread Swing graphique)
				SwingUtilities.invokeLater(() -> {
					OutputPanel.getInstance().showData(currentTle, currentInit, instant, currentTimestamp);
					GroundTrackMapPanel.getInstance().updatePosition(instant.geoCoords());
				});

				// 3. Pause (~60 FPS)
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		});

		// Démarrer le calcul en arrière plan
		simulationThread.start();
	}

	public void stopSimulation() {
		isRunning = false;
		if (simulationThread != null) {
			simulationThread.interrupt();
		}
	}
}
package fr.jordi_rocafort.tle_decoder.controller;

import fr.jordi_rocafort.tle_decoder.model.data.GeoCoords;
import fr.jordi_rocafort.tle_decoder.model.data.DynamicValues;
import fr.jordi_rocafort.tle_decoder.model.data.StaticValues;
import fr.jordi_rocafort.tle_decoder.model.data.TLE;
import fr.jordi_rocafort.tle_decoder.model.physics.OrbitPropagator;
import fr.jordi_rocafort.tle_decoder.view.GroundTrackMapPanel;
import fr.jordi_rocafort.tle_decoder.view.OutputPanel;

import javax.swing.SwingUtilities;

import java.util.ArrayList;
import java.util.List;
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
		SwingUtilities.invokeLater(() -> GroundTrackMapPanel.getInstance().clearTrack());

		long startTs = Instant.now().getEpochSecond();
		long duration = (long) (init.T() * 1.5);
		long endTs = startTs + duration;
		long step = 60L;

		List<GeoCoords> futureTrack = new ArrayList<>();
		for (long ts = startTs; ts <= endTs; ts += step) {
			DynamicValues dyn = OrbitPropagator.computeDynamicPhase(tle, init, ts);
			futureTrack.add(dyn.geoCoords());
		}

		SwingUtilities.invokeLater(() -> {
			GroundTrackMapPanel.getInstance().setFutureTrack(futureTrack);
		});

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

				// 3. Pause (~10 FPS)
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
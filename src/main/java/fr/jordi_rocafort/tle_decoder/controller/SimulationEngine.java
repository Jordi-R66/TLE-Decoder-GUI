package fr.jordi_rocafort.tle_decoder.controller;

import javax.swing.SwingUtilities;

import fr.jordi_rocafort.tle_decoder.model.data.DynamicValues;
import fr.jordi_rocafort.tle_decoder.model.data.GeoCoords;
import fr.jordi_rocafort.tle_decoder.model.data.StaticValues;
import fr.jordi_rocafort.tle_decoder.model.data.TLE;
import fr.jordi_rocafort.tle_decoder.model.physics.OrbitPropagator;
import fr.jordi_rocafort.tle_decoder.view.OutputPanel;
import fr.jordi_rocafort.tle_decoder.view.GroundTrackMapPanel;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class SimulationEngine {
	private Thread simulationThread;
	private boolean isRunning = false;

	private TLE currentTle;
	private StaticValues currentInit;

	public void startSimulation(TLE tle, StaticValues init) {
		this.currentTle = tle;
		this.currentInit = init;

		stopSimulation();

		// Nettoyage de l'ancienne carte
		SwingUtilities.invokeLater(() -> GroundTrackMapPanel.getInstance().clearTrack());

		isRunning = true;
		simulationThread = new Thread(() -> {
			long lastFutureTrackUpdate = 0; // Permet de ne pas recalculer la trace future à chaque frame

			while (isRunning) {
				long currentTimestamp = Instant.now().getEpochSecond();

				// 1. Calcul de la physique actuelle
				DynamicValues instant = OrbitPropagator.computeDynamicPhase(currentTle, currentInit, currentTimestamp);

				// 2. Mise à jour de la TRACE FUTURE (Toutes les 10 secondes)
				if (currentTimestamp - lastFutureTrackUpdate >= 10) {
					long duration = (long) (currentInit.T() * 1.5); // 1.5 orbite dans le futur
					long endTs = currentTimestamp + duration;
					long step = 60L;

					List<GeoCoords> futureTrack = new ArrayList<>();
					for (long ts = currentTimestamp; ts <= endTs; ts += step) {
						DynamicValues dyn = OrbitPropagator.computeDynamicPhase(currentTle, currentInit, ts);
						futureTrack.add(dyn.geoCoords());
					}

					SwingUtilities.invokeLater(() -> {
						GroundTrackMapPanel.getInstance().setFutureTrack(futureTrack);
					});

					lastFutureTrackUpdate = currentTimestamp;
				}

				// 3. Mise à jour de l'UI temps réel
				SwingUtilities.invokeLater(() -> {
					OutputPanel.getInstance().showData(currentTle, currentInit, instant, currentTimestamp);
					GroundTrackMapPanel.getInstance().updatePosition(instant.geoCoords());
				});

				// 4. Pause (~60 FPS)
				try {
					Thread.sleep(16);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		});

		simulationThread.start();
	}

	public void stopSimulation() {
		isRunning = false;
		if (simulationThread != null) {
			simulationThread.interrupt();
		}
	}
}
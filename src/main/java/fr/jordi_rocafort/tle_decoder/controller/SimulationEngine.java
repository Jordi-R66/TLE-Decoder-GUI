package fr.jordi_rocafort.tle_decoder.controller;

import javax.swing.SwingUtilities;

import fr.jordi_rocafort.tle_decoder.model.data.Coords3D;
import fr.jordi_rocafort.tle_decoder.model.data.DynamicValues;
import fr.jordi_rocafort.tle_decoder.model.data.GeoCoords;
import fr.jordi_rocafort.tle_decoder.model.data.StaticValues;
import fr.jordi_rocafort.tle_decoder.model.data.TLE;
import fr.jordi_rocafort.tle_decoder.model.physics.OrbitPropagator;
import fr.jordi_rocafort.tle_decoder.view.OutputPanel;
import fr.jordi_rocafort.tle_decoder.view.GroundTrackMapPanel;
import fr.jordi_rocafort.tle_decoder.view.Orbit2DPanel;
import fr.jordi_rocafort.tle_decoder.view.Orbit3DPanel;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class SimulationEngine {
	private Thread simulationThread;

	// Le mot-clé "volatile" garantit que le Thread lira toujours la vraie valeur
	// et ne la mettra pas en cache de son côté.
	private volatile boolean isRunning = false;

	private TLE currentTle;
	private StaticValues currentInit;

	public void startSimulation(TLE tle, StaticValues init) {
		this.currentTle = tle;
		this.currentInit = init;

		// Arrête proprement et tue l'ancien calcul s'il existe
		stopSimulation();

		// Nettoyage des anciennes vues
		SwingUtilities.invokeLater(() -> {
			GroundTrackMapPanel.getInstance().clearTrack();
			Orbit2DPanel.getInstance().clear();
		});

		isRunning = true;
		simulationThread = new Thread(() -> {
			long lastFutureTrackUpdate = 0;

			while (isRunning) {
				long currentTimestamp = Instant.now().getEpochSecond();

				// 1. Calcul de la physique actuelle
				DynamicValues instant = OrbitPropagator.computeDynamicPhase(currentTle, currentInit, currentTimestamp);

				// 2. Mise à jour de la TRACE FUTURE (Toutes les 10 secondes)
				if (currentTimestamp - lastFutureTrackUpdate >= 10) {
					long duration = (long) 24 * 3600;
					long endTs = currentTimestamp + duration;
					long step = 60L;

					List<GeoCoords> futureTrack2D = new ArrayList<>();
					for (long ts = currentTimestamp; ts <= endTs; ts += step) {
						DynamicValues dyn = OrbitPropagator.computeDynamicPhase(currentTle, currentInit, ts);
						futureTrack2D.add(dyn.geoCoords());
					}

					long duration3D = (long) currentInit.T();
					List<Coords3D> futureTrack3D = new ArrayList<>();
					for (long ts = currentTimestamp; ts <= currentTimestamp + duration3D; ts += 60L) {
						futureTrack3D.add(OrbitPropagator.computeDynamicPhase(currentTle, currentInit, ts).coords3d());
					}

					SwingUtilities.invokeLater(() -> {
						GroundTrackMapPanel.getInstance().setFutureTrack(futureTrack2D);
						Orbit3DPanel.getInstance().setFutureTrack(futureTrack3D);
					});

					lastFutureTrackUpdate = currentTimestamp;
				}

				// 3. Mise à jour de l'UI temps réel
				SwingUtilities.invokeLater(() -> {
					OutputPanel.getInstance().showData(currentTle, currentInit, instant, currentTimestamp);
					GroundTrackMapPanel.getInstance().updatePosition(instant.geoCoords());
					Orbit2DPanel.getInstance().updateData(currentTle, currentInit, instant.coords2d());
					Orbit3DPanel.getInstance().updatePosition(instant.coords3d(), currentTimestamp);
				});

				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					break;
				}
			}
		});

		simulationThread.start();
	}

	public void stopSimulation() {
		isRunning = false; // Avec "volatile", le thread le verra instantanément
		if (simulationThread != null && simulationThread.isAlive()) {
			simulationThread.interrupt(); // Réveille le thread s'il est en train de dormir
		}
	}
}
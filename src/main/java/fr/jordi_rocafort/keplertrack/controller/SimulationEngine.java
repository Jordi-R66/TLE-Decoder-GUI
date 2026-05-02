package fr.jordi_rocafort.keplertrack.controller;

import javax.swing.SwingUtilities;

import fr.jordi_rocafort.keplertrack.model.data.Coords3D;
import fr.jordi_rocafort.keplertrack.model.data.DynamicValues;
import fr.jordi_rocafort.keplertrack.model.data.GeoCoords;
import fr.jordi_rocafort.keplertrack.model.data.StaticValues;
import fr.jordi_rocafort.keplertrack.model.data.TLE;
// --- NOUVEAUX IMPORTS POUR LA VUE POLAIRE ---
import fr.jordi_rocafort.keplertrack.model.data.Station;
import fr.jordi_rocafort.keplertrack.model.data.TopocentricCoords;
import fr.jordi_rocafort.keplertrack.model.physics.CoordinatesTransforms;
import fr.jordi_rocafort.keplertrack.view.PolarViewPanel;
// --------------------------------------------
import fr.jordi_rocafort.keplertrack.model.physics.OrbitPropagator;
import fr.jordi_rocafort.keplertrack.view.GroundTrackMapPanel;
import fr.jordi_rocafort.keplertrack.view.Orbit2DPanel;
import fr.jordi_rocafort.keplertrack.view.Orbit3DPanel;
import fr.jordi_rocafort.keplertrack.view.OutputPanel;

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
			// --- NOUVEAU : Nettoyage du Skyplot quand on change de satellite ---
			PolarViewPanel.getInstance().clear();
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
					long duration = (long) 6 * 3600;
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

					// --- NOUVEAU : MISE À JOUR DU SKYPLOT POLAIRE TEMPS RÉEL ---
					// On récupère la station sélectionnée depuis l'UI (on le fait ici dans
					// l'invokeLater pour être "Thread-Safe" avec Swing)
					Station selectedStation = OutputPanel.getInstance().getSelectedStation();

					if (selectedStation != null) {
						// On calcule l'Azimut et l'Élévation instantanés
						TopocentricCoords currentAER = CoordinatesTransforms.calculateAER(
								selectedStation.coords(),
								instant.coords3d(),
								currentTimestamp);
						// On envoie à la vue polaire
						PolarViewPanel.getInstance().updateInstantPosition(currentAER);
					} else {
						// Pas de station, pas de point rouge
						PolarViewPanel.getInstance().updateInstantPosition(null);
					}
					// -----------------------------------------------------------
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
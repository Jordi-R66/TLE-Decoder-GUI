package fr.jordi_rocafort.keplertrack.model.data;

import java.util.ArrayList;

public record SatellitePass(
    long aosTime,               // Acquisition Of Signal
    long tcaTime,               // Time of Closest Approach
    long losTime,               // Loss Of Signal
    double maxElevation,        // En degrés
    ArrayList<PassPoint> trajectory  // L'historique pour dessiner le Skyplot plus tard
) {}

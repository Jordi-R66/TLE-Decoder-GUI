package fr.jordi_rocafort.keplertrack.model.tle;

import fr.jordi_rocafort.keplertrack.model.data.TLE;
import fr.jordi_rocafort.keplertrack.model.data.TleRawData;

public interface ITleParser<T extends TleRawData> {
	/**
	 * Parse un bloc de 3 lignes de texte pour générer un objet TLE immuable.
	 */
	public TLE parseLines(T rawData);
}
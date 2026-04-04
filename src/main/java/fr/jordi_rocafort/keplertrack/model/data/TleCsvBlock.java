package fr.jordi_rocafort.keplertrack.model.data;

public record TleCsvBlock(String line) implements TleRawData {
	public static TleCsvBlock fromRawBlock(String rawLine) {
		TleCsvBlock output = null;

		rawLine = rawLine.replace("\r", "").replace("\n", "");
		output = new TleCsvBlock(rawLine);

		return output;
	}
}
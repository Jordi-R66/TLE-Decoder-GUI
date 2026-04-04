package fr.jordi_rocafort.keplertrack.model.data;

public record TleLegacyBlock(String firstLine, String secondLine, String thirdLine) {
	public static TleLegacyBlock fromRawBlock(String rawBlock) {
		TleLegacyBlock output = null;

		rawBlock = rawBlock.replace("\r", "");
		String[] lines = rawBlock.split("\n");

		if (lines.length == 3) {
			output = new TleLegacyBlock(lines[0], lines[1], lines[2]);
		}

		return output;
	}
}
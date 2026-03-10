package fr.jordi_rocafort.tle_decoder.model.data;

public record TleBlock(String firstLine, String secondLine, String thirdLine) {
	public static TleBlock fromRawBlock(String rawBlock) {
		TleBlock output = null;

		rawBlock = rawBlock.replace("\r", "");
		String[] lines = rawBlock.split("\n");

		if (lines.length == 3) {
			output = new TleBlock(lines[0], lines[1], lines[2]);
		}

		return output;
	}
}
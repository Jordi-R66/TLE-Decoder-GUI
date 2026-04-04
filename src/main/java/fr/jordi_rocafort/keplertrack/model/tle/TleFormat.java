package fr.jordi_rocafort.keplertrack.model.tle;

public enum TleFormat {
	LEGACY("tle"),
	CSV("csv");

	private final String format;

	private TleFormat(String format) {
		this.format = format;
	}

	public String getFormatString() {
		return format;
	}
}

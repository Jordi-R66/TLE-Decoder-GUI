package fr.jordi_rocafort.tle_decoder.model.tle;

public record BlockInformation(String objName, int noradId, long blockIndex) implements Comparable<BlockInformation> {
	@Override
	public String toString() {
		return String.format("%d - %s", noradId, objName);
	}

	@Override
	public int compareTo(BlockInformation other) {
		return noradId - other.noradId;
	}
}

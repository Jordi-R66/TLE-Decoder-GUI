package fr.jordi_rocafort.keplertrack.model.tle;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;

import fr.jordi_rocafort.keplertrack.model.data.TLE;
import fr.jordi_rocafort.keplertrack.model.data.TleCsvBlock;
import fr.jordi_rocafort.keplertrack.model.data.TleLegacyBlock;

public class TleFileManager {
	// Instances statiques des deux parseurs
	private static final TleLegacyParser legacyParser = new TleLegacyParser();
	private static final TleCsvParser csvParser = new TleCsvParser();

	private static final int TLE_BLOCK_SIZE = 165;
	private static HashMap<String, ArrayList<BlockInformation>> associations = new HashMap<>();

	public static HashMap<String, ArrayList<BlockInformation>> getAssociations() {
		return associations;
	}

	// =====================================================================
	// ROUTAGE PRINCIPAL (Ce que DecodeController et FileSelection appellent)
	// =====================================================================

	public static ArrayList<BlockInformation> getAllNoradIDs(String filePath) throws Exception {
		if (!associations.containsKey(filePath)) {
			ArrayList<BlockInformation> outputList;

			// Routage selon l'extension du fichier
			if (filePath.toLowerCase().endsWith(".csv")) {
				outputList = parseCsvForIds(filePath);
			} else {
				outputList = parseLegacyForIds(filePath);
			}

			if (outputList.size() > 1) {
				outputList.sort(null);
			}

			associations.put(filePath, outputList);
		}

		return associations.get(filePath);
	}

	public static TLE getSingleTLE(String filePath, int targetNoradId) throws Exception {
		// Routage selon l'extension du fichier
		if (filePath.toLowerCase().endsWith(".csv")) {
			return getSingleTleFromCsv(filePath, targetNoradId);
		} else {
			return getSingleTleFromLegacy(filePath, targetNoradId);
		}
	}

	// =====================================================================
	// IMPLEMENTATION CSV
	// =====================================================================

	private static ArrayList<BlockInformation> parseCsvForIds(String filePath) throws Exception {
		ArrayList<BlockInformation> outputList = new ArrayList<>();
		long index = 0;

		try (BufferedReader br = new BufferedReader(new FileReader(filePath, StandardCharsets.UTF_8))) {
			String line;

			while ((line = br.readLine()) != null) {
				String[] fields = line.split(",");
				if (fields.length > 11) {
					try {
						// fields[0] = Nom, fields[11] = NORAD ID
						int noradId = Integer.parseInt(fields[11]);
						String objName = fields[0];
						outputList.add(new BlockInformation(objName, noradId, index));
						index++;
					} catch (NumberFormatException e) {
						// Ignore l'en-tête du CSV (NORAD_CAT_ID n'est pas un nombre)
					}
				}
			}
		}

		return outputList;
	}

	private static TLE getSingleTleFromCsv(String filePath, int targetNoradId) throws Exception {
		try (BufferedReader br = new BufferedReader(new FileReader(filePath, StandardCharsets.UTF_8))) {
			String line;

			while ((line = br.readLine()) != null) {
				String[] fields = line.split(",");

				if (fields.length > 11) {
					try {
						int tempNoradId = Integer.parseInt(fields[11]);
						if (tempNoradId == targetNoradId) {
							System.out.println("Ligne CSV trouvée : " + line);
							TleCsvBlock block = TleCsvBlock.fromRawBlock(line);
							return csvParser.parseLines(block);
						}
					} catch (NumberFormatException e) {
						// Ignore l'en-tête
					}
				}
			}
		}

		throw new IllegalArgumentException("Satellite " + targetNoradId + " introuvable dans " + filePath);
	}

	// =====================================================================
	// IMPLEMENTATION LEGACY (Ancien code conservé intact)
	// =====================================================================

	private static ArrayList<BlockInformation> parseLegacyForIds(String filePath) throws Exception {
		ArrayList<BlockInformation> outputList = new ArrayList<>();

		try (RandomAccessFile fp = new RandomAccessFile(filePath, "r")) {
			long tleCount = fp.length() / TLE_BLOCK_SIZE;
			outputList.ensureCapacity(tleCount < Integer.MAX_VALUE ? (int) tleCount : Integer.MAX_VALUE);

			for (long i = 0; i < tleCount; i++) {
				TleLegacyBlock block = getBlockByIndex(fp, i);
				int noradId = readNoradIdFromBlock(block);
				String objName = readObjectNameFromBlock(block);
				outputList.add(new BlockInformation(objName, noradId, i));
			}
		}

		return outputList;
	}

	private static TLE getSingleTleFromLegacy(String filePath, int targetNoradId) throws Exception {
		try (RandomAccessFile fp = new RandomAccessFile(filePath, "r")) {
			long tleCount = fp.length() / TLE_BLOCK_SIZE;
			System.out.println("Counted the TLEs (" + tleCount + ")");

			for (long i = 0; i < tleCount; i++) {
				TleLegacyBlock tempBlock = getBlockByIndex(fp, i);
				int tempNoradId = readNoradIdFromBlock(tempBlock);

				if (targetNoradId == tempNoradId) {
					System.out.printf("%s\n%s\n%s\n\n", tempBlock.firstLine(), tempBlock.secondLine(),
							tempBlock.thirdLine());
					return legacyParser.parseLines(tempBlock);
				}
			}
		}

		throw new IllegalArgumentException("Satellite " + targetNoradId + " introuvable dans " + filePath);
	}

	private static TleLegacyBlock getBlockByIndex(RandomAccessFile fp, long index) throws Exception {
		long newPos = TLE_BLOCK_SIZE * index;
		fp.seek(newPos);

		byte[] firstLineBytes = new byte[25];
		byte[] secondLineBytes = new byte[70];
		byte[] thirdLineBytes = new byte[70];

		fp.readFully(firstLineBytes);
		fp.readFully(secondLineBytes);
		fp.readFully(thirdLineBytes);

		String firstLine = new String(firstLineBytes, StandardCharsets.UTF_8).trim();
		String secondLine = new String(secondLineBytes, StandardCharsets.UTF_8).trim();
		String thirdLine = new String(thirdLineBytes, StandardCharsets.UTF_8).trim();

		return new TleLegacyBlock(firstLine, secondLine, thirdLine);
	}

	private static String readObjectNameFromBlock(TleLegacyBlock block) {
		return block.firstLine().trim();
	}

	private static int readNoradIdFromBlock(TleLegacyBlock block) {
		String noradCat = block.secondLine().substring(2, 7).trim();
		return Integer.parseInt(noradCat);
	}
}

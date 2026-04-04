package fr.jordi_rocafort.keplertrack.model.tle;

import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;

import fr.jordi_rocafort.keplertrack.model.data.TLE;
import fr.jordi_rocafort.keplertrack.model.data.TleLegacyBlock; // Modification ici

public class TleFileManager {
	// Création d'une instance statique de parser
	private static final TleLegacyParser parser = new TleLegacyParser();

	// Rendu statique
	private static final int TLE_BLOCK_SIZE = 165;

	// Rendu statique
	private static HashMap<String, ArrayList<BlockInformation>> associations = new HashMap<>();

	public static HashMap<String, ArrayList<BlockInformation>> getAssociations() {
		return associations;
	}

	public static long getTleNumber(RandomAccessFile fp) throws Exception {
		return fp.length() / TLE_BLOCK_SIZE;
	}

	public static TleLegacyBlock getBlockByIndex(RandomAccessFile fp, long index) throws Exception {
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

		// TleLegacyBlock prend bien 3 arguments
		return new TleLegacyBlock(firstLine, secondLine, thirdLine);
	}

	public static String readObjectNameFromBlock(TleLegacyBlock block) {
		return block.firstLine().trim();
	}

	public static int readNoradIdFromBlock(TleLegacyBlock block) {
		String noradCat = block.secondLine().substring(2, 7).trim();
		return Integer.parseInt(noradCat);
	}

	public static ArrayList<BlockInformation> getAllNoradIDs(String filePath) throws Exception {
		ArrayList<BlockInformation> outputList = new ArrayList<>();

		if (!associations.containsKey(filePath)) {
			try (RandomAccessFile fp = new RandomAccessFile(filePath, "r")) {
				long tleCount = (long) getTleNumber(fp);
				outputList.ensureCapacity(tleCount < Integer.MAX_VALUE ? (int) tleCount : Integer.MAX_VALUE);

				for (long i = 0; i < tleCount; i++) {
					TleLegacyBlock block = getBlockByIndex(fp, i);
					int noradId = readNoradIdFromBlock(block);
					String objName = readObjectNameFromBlock(block);

					outputList.add(new BlockInformation(objName, noradId, i));
				}
			}

			if (outputList.size() > 1) {
				outputList.sort(null);
			}

			associations.put(filePath, outputList);
		} else {
			outputList = associations.get(filePath);
		}

		return outputList;
	}

	public static TLE getSingleTLE(String filePath, int targetNoradId) throws Exception {
		try (RandomAccessFile fp = new RandomAccessFile(filePath, "r")) {
			long tleCount = getTleNumber(fp);
			System.out.println("Counted the TLEs (" + tleCount + ")");

			boolean found = false;
			long i = 0;
			TleLegacyBlock tempBlock = null;

			while (!found && i < tleCount) {
				tempBlock = getBlockByIndex(fp, i);
				int tempNoradId = readNoradIdFromBlock(tempBlock);

				found = (targetNoradId == tempNoradId);

				if (found) {
					System.out.printf("%s\n%s\n%s\n\n",
							tempBlock.firstLine(),
							tempBlock.secondLine(),
							tempBlock.thirdLine());
				}

				i++;
			}

			if (found && tempBlock != null) {
				return parser.parseLines(tempBlock);
			}
		}

		throw new IllegalArgumentException("Satellite " + targetNoradId + " introuvable dans " + filePath);
	}
}
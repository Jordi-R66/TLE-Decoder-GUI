package fr.jordi_rocafort.keplertrack.model.tle;

import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;

import fr.jordi_rocafort.keplertrack.model.data.TLE;
import fr.jordi_rocafort.keplertrack.model.data.TleBlock;

public class TleFileManager {

	// Équivalent de #define TLE_BLOCK_SIZE sizeof(tle_block) (25 + 70 + 70 = 165)
	private static final int TLE_BLOCK_SIZE = 165;

	private static HashMap<String, ArrayList<BlockInformation>> associations = new HashMap<>();

	public static HashMap<String, ArrayList<BlockInformation>> getAssociations() { return associations; }

	/**
	 * Équivalent de GetTLENumber(FILE* fp)
	 */
	public static long getTleNumber(RandomAccessFile fp) throws Exception {
		return fp.length() / TLE_BLOCK_SIZE;
	}

	/**
	 * Équivalent de getBlockByIndex(FILE* fp, long index)
	 */
	public static TleBlock getBlockByIndex(RandomAccessFile fp, long index) throws Exception {
		// fseek(fp, new_pos, SEEK_SET);
		long newPos = TLE_BLOCK_SIZE * index;
		fp.seek(newPos);

		byte[] firstLineBytes = new byte[25];
		byte[] secondLineBytes = new byte[70];
		byte[] thirdLineBytes = new byte[70];

		// fread(&output, TLE_BLOCK_SIZE, 1, fp);
		fp.readFully(firstLineBytes);
		fp.readFully(secondLineBytes);
		fp.readFully(thirdLineBytes);

		// Enlève les caractères de fin de chaîne (\0) ou retours à la ligne avec trim()
		String firstLine = new String(firstLineBytes, StandardCharsets.UTF_8).trim();
		String secondLine = new String(secondLineBytes, StandardCharsets.UTF_8).trim();
		String thirdLine = new String(thirdLineBytes, StandardCharsets.UTF_8).trim();

		return new TleBlock(firstLine, secondLine, thirdLine);
	}

	public static String readObjectNameFromBlock(TleBlock block) {
		return block.firstLine().trim();
	}

	/**
	 * Équivalent de readNoradIdFromBlock(tle_block* block)
	 */
	public static int readNoradIdFromBlock(TleBlock block) {
		// L'équivalent de la boucle for (i < 5) qui récupère SECOND_LINE[i + 2]
		String noradCat = block.secondLine().substring(2, 7).trim();
		return Integer.parseInt(noradCat);
	}

	public static ArrayList<BlockInformation> getAllNoradIDs(String filePath) throws Exception {
		ArrayList<BlockInformation> outputList = new ArrayList<>();

		if (!associations.containsKey(filePath)) {
			try (RandomAccessFile fp = new RandomAccessFile(filePath, "r")) {
				long tleCount = (long)getTleNumber(fp);
				outputList.ensureCapacity(tleCount < Integer.MAX_VALUE ? (int)tleCount : Integer.MAX_VALUE);

				for (long i = 0; i < tleCount; i++) {
					TleBlock block = getBlockByIndex(fp, i);
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

	/**
	 * Équivalent de GetSingleTLE(FILE* fp, uint32_t noradId)
	 */
	public static TLE getSingleTLE(String filePath, int targetNoradId) throws Exception {
		// RandomAccessFile "r" est l'équivalent de fopen(..., "r")
		try (RandomAccessFile fp = new RandomAccessFile(filePath, "r")) {
			long tleCount = getTleNumber(fp);
			System.out.println("Counted the TLEs (" + tleCount + ")");

			boolean found = false;
			long i = 0;
			TleBlock tempBlock = null;

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
				return TleParser.parseLines(tempBlock); // Équivalent de parse_block(&tempBlock)
			}
		}

		// Si on sort de la boucle sans rien trouver
		throw new IllegalArgumentException("Satellite " + targetNoradId + " introuvable dans " + filePath);
	}
}
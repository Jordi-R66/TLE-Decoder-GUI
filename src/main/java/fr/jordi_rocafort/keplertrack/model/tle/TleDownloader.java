package fr.jordi_rocafort.keplertrack.model.tle;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.net.URI;

public class TleDownloader {
	private static final String USER_AGENT = "TLE-Decoder/1.0 (See https://github.com/Jordi-R66/TLE-Decoder-GUI)";
	private static final String BASE_URL = "https://celestrak.org/NORAD/elements/gp.php?GROUP=SET_NAME&FORMAT=FORMAT_NAME";

	public static final ArrayList<String> datasets = new ArrayList<String>(
			Arrays.asList(new String[] {
					"active", "stations", "last-30-days", "visual", "analyst", "cosmos-1408-debris",
					"fengyun-1c-debris", "iridium-33-debris", "cosmos-2251-debris", "weather", "noaa", "goes",
					"resource", "sarsat", "dmc", "tdrss", "argos", "planet", "spire", "geo", "gpz", "gpz-plus",
					"intelsat", "ses", "starlink", "iridium", "iridium-NEXT", "oneweb", "orbcomm", "globalstar",
					"swarm", "amateur", "x-comm", "other-comm", "satnogs", "gorizont", "raduga", "molniya", "gnss",
					"gps-ops", "glo-ops", "galileo", "beidou", "sbas", "nnss", "musson", "science", "geodetic",
					"engineering", "education", "military", "radar", "cubesat", "other"
			}));

	private static int downloadTleSet(HttpClient client, String datasetName, TleFormat format) {
		int statusCode = 0;
		String formatString = format.getFormatString();
		URI uri = URI.create(BASE_URL.replace("SET_NAME", datasetName).replace("FORMAT_NAME", formatString));

		HttpRequest req;
		HttpResponse<String> response;

		if (datasets.contains(datasetName)) {
			try {
				req = HttpRequest.newBuilder().uri(uri).header("User-Agent", USER_AGENT).GET().build();
				response = client.send(req, HttpResponse.BodyHandlers.ofString());

				statusCode = response.statusCode();

				if (statusCode == 200) {
					boolean remakeRequest = response.body().contains("not found");

					if (remakeRequest) {
						uri = URI.create(BASE_URL.replace("SET_NAME", datasetName).replace("GROUP", "SPECIAL").replace("FORMAT_NAME", formatString));
						req = HttpRequest.newBuilder().uri(uri).header("User-Agent", USER_AGENT).GET().build();
						response = client.send(req, HttpResponse.BodyHandlers.ofString());
						statusCode = response.statusCode();
					}

					if (!remakeRequest || (remakeRequest && statusCode == 200)) {
						String content = response.body();
						Path chemin = Paths.get(String.format("TLEs/%s.%s", datasetName, formatString));

						System.out.println(chemin);

						Files.createDirectories(chemin.getParent());
						Files.writeString(chemin, content.replace("\r", ""));
					}
				}
			} catch (Exception e) {
				statusCode = -1;
			}
		}

		return statusCode;
	}

	public static int downloadTleSet(String datasetName) {
		int statusCode = 0;

		try (HttpClient client = HttpClient.newHttpClient()) {
			statusCode = downloadTleSet(client, datasetName, TleFormat.LEGACY);
		} catch (Exception e) {
			statusCode = -1;
		}

		return statusCode;
	}

	/**
	 * Downloads all the sets given in a specified list
	 * 
	 * @param datasets
	 * @return The list of sets that couldn't be downloaded
	 */
	public static ArrayList<String> downloadTleSets(List<String> datasets) {
		ArrayList<String> output = new ArrayList<>();

		if (TleDownloader.datasets.containsAll(datasets)) {
			try (HttpClient client = HttpClient.newHttpClient()) {
				int statusCode = 0;

				for (String datasetName : datasets) {
					statusCode = downloadTleSet(client, datasetName, TleFormat.LEGACY);

					if (statusCode != 200) {
						output.add(datasetName);
					}
				}
			} catch (Exception e) {
				output.addAll(datasets);
			}
		}

		return output;
	}

	/**
	 * Downloads all the sets given in a specified list with a given number of
	 * threads
	 * 
	 * @param datasets
	 * @return The list of sets that couldn't be downloaded
	 */
	public static ArrayList<String> downloadTleSets(List<String> datasets, int threads) {
		ArrayList<String> output = new ArrayList<>();

		if (TleDownloader.datasets.containsAll(datasets)) {
			ExecutorService executor = Executors.newFixedThreadPool(threads);
			List<Callable<ArrayList<String>>> tasks = new ArrayList<>();

			int totalSize = datasets.size();

			int chunkSize = (int) Math.ceil((double) totalSize / threads);
			if (chunkSize == 0)
				chunkSize = 1;

			for (int i = 0; i < totalSize; i += chunkSize) {
				int end = Math.min(totalSize, i + chunkSize);
				List<String> subList = datasets.subList(i, end);

				tasks.add(() -> downloadTleSets(subList));
			}

			try {
				List<Future<ArrayList<String>>> futures = executor.invokeAll(tasks);

				for (Future<ArrayList<String>> future : futures) {
					output.addAll(future.get());
				}
			} catch (InterruptedException | ExecutionException e) {
				System.err.println("Une erreur est survenue lors de l'exécution multithread : " + e.getMessage());
			} finally {
				executor.shutdown();
			}
		}

		return output;
	}

	public static ArrayList<String> downloadAllTles(int threads) {
		return downloadTleSets(datasets, threads);
	}

	public static ArrayList<String> downloadAllTles() {
		return downloadTleSets(datasets, 1);
	}


	/**
	 * Fusionne tous les fichiers TLE téléchargés en un seul fichier "merged.tle",
	 * en excluant les doublons (via le NORAD ID) et les satellites "UNKNOWN".
	 */
	public static void mergeAllTles() {
		Path mergedFilePath = Paths.get("TLEs/merged.tle");

		// Un HashSet est parfait ici : il empêche les doublons et la vérification
		// "contains" est quasi instantanée
		HashSet<Integer> mergedIds = new HashSet<>();

		long totalTime = 0;

		try {
			Files.createDirectories(mergedFilePath.getParent());

			try (BufferedWriter writer = Files.newBufferedWriter(mergedFilePath, StandardCharsets.UTF_8)) {

				// On boucle sur la liste déjà existante dans TleDownloader
				for (String setName : TleDownloader.datasets) {
					System.out.println("Merging " + setName + ".tle");
					long start = System.nanoTime();

					Path currentFilePath = Paths.get("TLEs/" + setName + ".tle");

					if (!Files.exists(currentFilePath)) {
						System.out.println("Fichier " + setName + ".tle introuvable, ignoré.");
						continue;
					}

					try (BufferedReader reader = Files.newBufferedReader(currentFilePath, StandardCharsets.UTF_8)) {
						String line0, line1, line2;

						while ((line0 = reader.readLine()) != null &&
								(line1 = reader.readLine()) != null &&
								(line2 = reader.readLine()) != null) {

							if (line2.length() < 7)
								continue;

							int noradId;
							try {
								noradId = Integer.parseInt(line2.substring(2, 7).trim());
							} catch (NumberFormatException e) {
								continue;
							}

							// Si l'ID n'est pas déjà dans le Set ET que le nom n'est pas "UNKNOWN"
							if (!mergedIds.contains(noradId) && !line0.contains("UNKNOWN")) {
								mergedIds.add(noradId);

								// Écriture du bloc en forçant le line separator LF (\n)
								writer.write(line0 + "\n");
								writer.write(line1 + "\n");
								writer.write(line2 + "\n");
							}
						}
					}

					long end = System.nanoTime();
					long execTime = end - start;
					totalTime += execTime;

					System.out.printf("%s.tle merged in %.3fs\n", setName, execTime / 1_000_000_000.0);
				}
			}

			System.out.printf("All files have been merged in %.3fs\n", totalTime / 1_000_000_000.0);

		} catch (Exception e) {
			System.err.println("Erreur lors de la fusion des fichiers TLE : " + e.getMessage());
			e.printStackTrace();
		}
	}

	public static ArrayList<String> downloadAndMergeAllTles(int threads) {
		ArrayList<String> output = downloadAllTles(threads);
		mergeAllTles();

		return output;
	}

	public static ArrayList<String> downloadAndMergeAllTles() {
		return downloadAllTles(1);
	}
}

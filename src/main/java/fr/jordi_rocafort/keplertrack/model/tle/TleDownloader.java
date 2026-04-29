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

					if (remakeRequest && format == TleFormat.LEGACY) {
						uri = URI.create(BASE_URL.replace("SET_NAME", datasetName).replace("GROUP", "SPECIAL")
								.replace("FORMAT_NAME", formatString));
						req = HttpRequest.newBuilder().uri(uri).header("User-Agent", USER_AGENT).GET().build();
						response = client.send(req, HttpResponse.BodyHandlers.ofString());
						statusCode = response.statusCode();
					} else if (remakeRequest && (format != TleFormat.LEGACY)) {
						statusCode = 0;
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
			statusCode = downloadTleSet(client, datasetName, TleFormat.CSV);
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
	public static ArrayList<String> downloadTleSets(List<String> datasets, TleFormat format) {
		ArrayList<String> output = new ArrayList<>();

		if (TleDownloader.datasets.containsAll(datasets)) {
			try (HttpClient client = HttpClient.newHttpClient()) {
				int statusCode = 0;

				for (String datasetName : datasets) {
					statusCode = downloadTleSet(client, datasetName, format);

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
	public static ArrayList<String> downloadTleSets(List<String> datasets, TleFormat format, int threads) {
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

				tasks.add(() -> downloadTleSets(subList, format));
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

	public static ArrayList<String> downloadAllTles(int threads, TleFormat format) {
		return downloadTleSets(datasets, format, threads);
	}

	public static ArrayList<String> downloadAllTles(TleFormat format) {
		return downloadTleSets(datasets, format, 1);
	}

	/**
	 * Fusionne tous les fichiers téléchargés du format spécifié en un seul fichier
	 * "merged.tle" ou "merged.csv", en excluant les doublons (via le NORAD ID)
	 * et les satellites "UNKNOWN".
	 * * @param format Le format de fichiers à fusionner (TleFormat.LEGACY ou TleFormat.CSV)
	 */
	public static void mergeAllTles(TleFormat format) {
		boolean isCsv = (format == TleFormat.CSV);
		String extension = format.getFormatString(); // Récupère "tle" ou "csv" depuis l'Enum
		Path mergedPath = Paths.get("TLEs/merged." + extension);

		// Un seul HashSet pour stocker les IDs traités
		HashSet<Integer> mergedIds = new HashSet<>();

		long totalTime = 0;

		try {
			Files.createDirectories(mergedPath.getParent());

			// On n'ouvre qu'un seul writer pour le format cible
			try (BufferedWriter writer = Files.newBufferedWriter(mergedPath, StandardCharsets.UTF_8)) {

				// Si c'est un CSV, on commence par écrire l'en-tête
				if (isCsv) {
					String csvHeader = "OBJECT_NAME,OBJECT_ID,EPOCH,MEAN_MOTION,ECCENTRICITY,INCLINATION,RA_OF_ASC_NODE,ARG_OF_PERICENTER,MEAN_ANOMALY,EPHEMERIS_TYPE,CLASSIFICATION_TYPE,NORAD_CAT_ID,ELEMENT_SET_NO,REV_AT_EPOCH,BSTAR,MEAN_MOTION_DOT,MEAN_MOTION_DDOT";
					writer.write(csvHeader + "\n");
				}

				for (String setName : TleDownloader.datasets) {
					long start = System.nanoTime();
					boolean processedAnything = false;

					// Le chemin cible utilise l'extension dynamique
					Path filePath = Paths.get("TLEs/" + setName + "." + extension);

					if (Files.exists(filePath)) {
						try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
							
							if (isCsv) {
								// === LOGIQUE DE PARSING CSV ===
								String line;
								boolean isFirstLine = true;

								while ((line = reader.readLine()) != null) {
									// On saute l'en-tête de chaque fichier individuel
									if (isFirstLine) {
										isFirstLine = false;
										continue;
									}

									String[] fields = line.split(",");
									if (fields.length > 11) {
										try {
											// Ajout du .trim() pour corriger les éventuels espaces
											int noradId = Integer.parseInt(fields[11].trim());
											String objName = fields[0];

											if (!mergedIds.contains(noradId) && !objName.contains("UNKNOWN")) {
												mergedIds.add(noradId);
												writer.write(line + "\n");
											}
										} catch (NumberFormatException e) {
											continue;
										}
									}
								}
							} else {
								// === LOGIQUE DE PARSING LEGACY (TLE) ===
								String line0, line1, line2;

								while ((line0 = reader.readLine()) != null &&
										(line1 = reader.readLine()) != null &&
										(line2 = reader.readLine()) != null) {

									if (line2.length() < 7)
										continue;

									try {
										int noradId = Integer.parseInt(line2.substring(2, 7).trim());
										if (!mergedIds.contains(noradId) && !line0.contains("UNKNOWN")) {
											mergedIds.add(noradId);
											writer.write(line0 + "\n");
											writer.write(line1 + "\n");
											writer.write(line2 + "\n");
										}
									} catch (NumberFormatException e) {
										continue;
									}
								}
							}
						}
						processedAnything = true;
					}

					long end = System.nanoTime();
					long execTime = end - start;
					totalTime += execTime;

					if (processedAnything) {
						System.out.printf("%s.%s datasets merged in %.3fs\n", setName, extension, execTime / 1_000_000_000.0);
					}
				}
			}

			System.out.printf("All %s files have been merged in %.3fs\n", extension, totalTime / 1_000_000_000.0);

		} catch (Exception e) {
			System.err.println("Erreur lors de la fusion des fichiers : " + e.getMessage());
			e.printStackTrace();
		}
	}

	public static ArrayList<String> downloadAndMergeAllTles(int threads, TleFormat format) {
		ArrayList<String> output = downloadAllTles(threads, format);
		mergeAllTles(format);

		return output;
	}

	public static ArrayList<String> downloadAndMergeAllTles(TleFormat format) {
		return downloadAllTles(1, format);
	}
}

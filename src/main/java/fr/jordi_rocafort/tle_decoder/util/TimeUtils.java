package fr.jordi_rocafort.tle_decoder.util;

import fr.jordi_rocafort.tle_decoder.model.data.TLE;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class TimeUtils {

	// Formateur réutilisable pour l'affichage des dates en UTC
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'")
			.withZone(ZoneOffset.UTC);

	/**
	 * Remplace la mécanique complexe du format "NORAD_TIME" de ton code C.
	 * Convertit l'année sur 2 chiffres et le jour fractionnel du TLE en Timestamp
	 * UNIX (secondes).
	 */
	public static long getEpochTimestampFromTLE(TLE tle) {
		// Règle officielle des TLE NORAD :
		// Si l'année est >= 57, on est dans les années 1900. Sinon, on est dans les
		// années 2000.
		int fullYear = (tle.epochYear() >= 57) ? 1900 + tle.epochYear() : 2000 + tle.epochYear();

		int dayOfYear = (int) tle.epochDay();
		double fractionalDay = tle.epochDay() - dayOfYear;

		// Calcul du nombre de secondes que représente la fraction de jour
		long secondsOfDay = Math.round(fractionalDay * 24.0 * 3600.0);

		// On crée la date au 1er Janvier de l'année, on ajoute le nombre de jours et de
		// secondes
		ZonedDateTime epochZdt = ZonedDateTime.of(fullYear, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
				.plusDays(dayOfYear - 1) // -1 car le 1er Janvier est déjà le jour 1
				.plusSeconds(secondsOfDay);

		// Retourne le timestamp UNIX (type long au lieu du time_t du C)
		return epochZdt.toEpochSecond();
	}

	/**
	 * @brief Génère une string représentant la date et l'heure ("YYYY-MM-DD
	 *        HH:mm:SS UTC")
	 */
	public static String timestampToDateString(long timestamp) {
		// Pas besoin de sprintf ni d'allouer de la mémoire !
		return DATE_FORMATTER.format(Instant.ofEpochSecond(timestamp));
	}

	/**
	 * @brief Formate une période orbitale (en secondes) en format: "Xd Yh Zm
	 *        W.AAAs"
	 */
	public static String periodToString(double periodSeconds) {
		long totalMillis = (long) (periodSeconds * 1000.0);

		long days = totalMillis / 86400000L;
		totalMillis %= 86400000L;

		long hours = totalMillis / 3600000L;
		totalMillis %= 3600000L;

		long minutes = totalMillis / 60000L;
		totalMillis %= 60000L;

		long seconds = totalMillis / 1000L;
		long millis = totalMillis % 1000L;

		return String.format("%dd %dh %dm %d.%03ds", days, hours, minutes, seconds, millis);
	}

	/**
	 * @brief Formate un DeltaTime (en secondes) en format: "Xd YYh ZZm WWs"
	 */
	public static String deltaTimeToString(long deltaTimeSeconds) {
		long days = deltaTimeSeconds / 86400L;
		long rem = deltaTimeSeconds % 86400L;

		long hours = rem / 3600L;
		rem %= 3600L;

		long minutes = rem / 60L;
		long seconds = rem % 60L;

		return String.format("%dd %02dh %02dm %02ds", days, hours, minutes, seconds);
	}
}
package com.bingmerfest.wordle.nyt;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Fetches and caches the daily NYT puzzle. Answers never change once
 * published, so each date is fetched at most once per JVM; don't hammer the
 * endpoint. Supported date range is launch (2021-06-19) to ~23 days ahead —
 * out-of-range dates error.
 *
 * <p>Used by the daily AUTO mode only. Assist mode must never call this —
 * that's the honest, no-peeking flow.
 */
@Component
public class NytWordleClient {

	private static final String URL_TEMPLATE = "https://www.nytimes.com/svc/wordle/v2/%s.json";

	private final RestClient http;
	private final Map<LocalDate, NytPuzzle> cache = new ConcurrentHashMap<>();

	public NytWordleClient() {
		this(RestClient.builder()
				.defaultHeader(HttpHeaders.USER_AGENT,
						"Mozilla/5.0 (Windows NT 10.0; Win64; x64) wordle-solver/1.0")
				.build());
	}

	NytWordleClient(RestClient http) {
		this.http = http;
	}

	/** Today's puzzle (server-local date), cached. */
	public NytPuzzle fetchToday() {
		return fetch(LocalDate.now());
	}

	public NytPuzzle fetch(LocalDate date) {
		return cache.computeIfAbsent(date, this::load);
	}

	private NytPuzzle load(LocalDate date) {
		NytResponse r = http.get()
				.uri(URL_TEMPLATE.formatted(date))
				.retrieve()
				.body(NytResponse.class);
		if (r == null || r.solution() == null || r.solution().length() != 5) {
			throw new IllegalStateException("Unexpected NYT response for " + date);
		}
		return new NytPuzzle(LocalDate.parse(r.printDate()), r.id(), r.daysSinceLaunch(),
				r.solution().toLowerCase(Locale.ROOT), r.editor());
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record NytResponse(
			@JsonProperty("id") int id,
			@JsonProperty("solution") String solution,
			@JsonProperty("print_date") String printDate,
			@JsonProperty("days_since_launch") int daysSinceLaunch,
			@JsonProperty("editor") String editor) {
	}
}

package com.bingmerfest.wordle.web;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import com.bingmerfest.wordle.engine.Benchmark;
import com.bingmerfest.wordle.engine.EntropySolver;
import com.bingmerfest.wordle.engine.PatternMatrix;
import com.bingmerfest.wordle.engine.SolverConfig;
import com.bingmerfest.wordle.engine.WordLists;
import com.bingmerfest.wordle.persistence.BenchmarkRunEntity;
import com.bingmerfest.wordle.persistence.BenchmarkRunRepository;
import com.bingmerfest.wordle.persistence.SolverConfigEntity;
import com.bingmerfest.wordle.persistence.SolverConfigRepository;

/**
 * Benchmark harness UI (M7): trigger runs over the full answer list (or a
 * seeded sample), persist them linked to their solver config, and compare
 * configs over time. Runs are synchronous — the engine clears the full list
 * in well under a second on this hardware (hard mode takes a few).
 *
 * <p>Individual benchmark games are deliberately NOT persisted to the game
 * table — 2,309 rows per run would drown it; the aggregated benchmark_run
 * row is the record.
 */
@Controller
public class BenchmarkController {

	private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
	private static final long DEFAULT_SAMPLE_SEED = 42L;
	private static final int MIN_SAMPLE = 10;

	private final WordLists words;
	private final PatternMatrix matrix;
	private final BenchmarkRunRepository runs;
	private final SolverConfigRepository configs;

	public BenchmarkController(WordLists words, PatternMatrix matrix,
			BenchmarkRunRepository runs, SolverConfigRepository configs) {
		this.words = words;
		this.matrix = matrix;
		this.runs = runs;
		this.configs = configs;
	}

	@GetMapping("/benchmark")
	public String page() {
		return "benchmark";
	}

	public record RunRequest(String opener, Boolean hardMode, String guessPool, String tiebreak,
			Integer sampleSize, Long seed) {
	}

	public record RunDto(long id, String configName, String opener, boolean hardMode,
			String guessPool, String tiebreak, int answerCount, double winRate, double avgGuesses,
			int[] dist, int fails, long durationMs, String createdAt) {

		static RunDto of(BenchmarkRunEntity e) {
			SolverConfigEntity c = e.getConfig();
			return new RunDto(e.getId(), c.getName(), c.getOpener(), c.isHardMode(),
					c.getGuessPool().name(), c.getTiebreak().name(), e.getAnswerCount(),
					e.getWinRate(), e.getAvgGuesses(),
					new int[] { e.getDist1(), e.getDist2(), e.getDist3(), e.getDist4(), e.getDist5(), e.getDist6() },
					e.getFails(), e.getDurationMs(),
					WHEN.format(LocalDateTime.ofInstant(e.getCreatedAt(), ZoneId.systemDefault())));
		}
	}

	public record RunResponse(RunDto run, String error) {
	}

	@PostMapping(value = "/benchmark/run", produces = "application/json")
	@ResponseBody
	public RunResponse run(@RequestBody RunRequest request) {
		SolverConfig config;
		EntropySolver solver;
		try {
			config = toConfig(request);
			solver = new EntropySolver(words, matrix, config); // validates opener
		} catch (IllegalArgumentException e) {
			return new RunResponse(null, e.getMessage());
		}

		List<String> answerSet = words.answers();
		if (request.sampleSize() != null && request.sampleSize() < words.answerCount()) {
			int size = Math.max(MIN_SAMPLE, request.sampleSize());
			long seed = request.seed() != null ? request.seed() : DEFAULT_SAMPLE_SEED;
			answerSet = Benchmark.sample(words, size, seed);
		}

		Benchmark.Result result = Benchmark.run(solver, answerSet);
		SolverConfigEntity configRow = configs.getOrCreate(canonicalName(config), config);
		BenchmarkRunEntity saved = runs.save(BenchmarkRunEntity.of(result, configRow));
		return new RunResponse(RunDto.of(saved), null);
	}

	@GetMapping(value = "/benchmark/runs", produces = "application/json")
	@ResponseBody
	public List<RunDto> list() {
		return runs.findAllWithConfig().stream().map(RunDto::of).toList();
	}

	private static SolverConfig toConfig(RunRequest request) {
		String opener = request.opener() == null || request.opener().isBlank()
				? SolverConfig.DEFAULT_OPENER : request.opener().trim();
		SolverConfig.GuessPool pool = parseEnum(SolverConfig.GuessPool.class,
				request.guessPool(), SolverConfig.GuessPool.FULL);
		SolverConfig.Tiebreak tiebreak = parseEnum(SolverConfig.Tiebreak.class,
				request.tiebreak(), SolverConfig.Tiebreak.PREFER_CANDIDATE);
		return new SolverConfig(opener, Boolean.TRUE.equals(request.hardMode()), pool, tiebreak);
	}

	private static <E extends Enum<E>> E parseEnum(Class<E> type, String value, E fallback) {
		if (value == null || value.isBlank()) {
			return fallback;
		}
		try {
			return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Bad " + type.getSimpleName() + ": " + value);
		}
	}

	/** Stable, human-readable name so identical configs share one row. */
	static String canonicalName(SolverConfig config) {
		return config.opener()
				+ (config.hardMode() ? "-hard" : "-easy")
				+ (config.guessPool() == SolverConfig.GuessPool.ANSWERS_ONLY ? "-answers" : "-full")
				+ (config.tiebreak() == SolverConfig.Tiebreak.FIRST_BY_INDEX ? "-byindex" : "-prefcand");
	}
}

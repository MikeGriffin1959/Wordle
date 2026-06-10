package com.bingmerfest.wordle.engine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Engine-level benchmark harness (M3): runs the solver across the full answer
 * list (or a seedable sample) and aggregates win-rate, average guesses, the
 * 1–6 guess-count distribution, failures, and worst-case answers.
 *
 * <p>Expected with defaults (SALET, easy mode, full pool): average ≈ 3.4–3.6
 * guesses, win-rate ≈ 100%. This is the regression check for solver changes.
 *
 * <p>Also runnable standalone: {@code main(opener...)} compares openers.
 */
public final class Benchmark {

	private static final int HARDEST_LIMIT = 10;

	private Benchmark() {
	}

	public record Result(SolverConfig config, int answerCount, int solved, int failed,
			double avgGuesses, int[] distribution, List<String> failures,
			List<GameResult> hardestGames, long durationMs) {

		public double winRate() {
			return answerCount == 0 ? 0 : solved / (double) answerCount;
		}

		public String summary() {
			StringBuilder sb = new StringBuilder();
			sb.append(String.format("config: opener=%s hardMode=%s pool=%s tiebreak=%s%n",
					config.opener(), config.hardMode(), config.guessPool(), config.tiebreak()));
			sb.append(String.format("answers: %d  solved: %d (%.2f%%)  failed: %d%n",
					answerCount, solved, winRate() * 100, failed));
			sb.append(String.format("avg guesses (solved): %.4f%n", avgGuesses));
			sb.append("distribution:");
			for (int k = 1; k <= SolverConfig.MAX_GUESSES; k++) {
				sb.append(String.format(" %d:%d", k, distribution[k - 1]));
			}
			sb.append(String.format("%n"));
			if (!failures.isEmpty()) {
				sb.append("failures: ").append(failures).append(String.format("%n"));
			}
			sb.append("hardest: ");
			for (GameResult g : hardestGames) {
				sb.append(String.format("%n  %s", g));
			}
			sb.append(String.format("%ntime: %d ms%n", durationMs));
			return sb.toString();
		}
	}

	/** Runs the full answer list. */
	public static Result runAll(EntropySolver solver) {
		return run(solver, solver.words().answers());
	}

	/** Runs an arbitrary subset of answers (e.g. a seeded sample). */
	public static Result run(EntropySolver solver, List<String> answers) {
		long start = System.nanoTime();
		List<GameResult> results = answers.parallelStream().map(solver::solve).toList();
		long durationMs = (System.nanoTime() - start) / 1_000_000;

		int[] distribution = new int[SolverConfig.MAX_GUESSES];
		int solved = 0;
		long totalGuesses = 0;
		List<String> failures = new ArrayList<>();
		for (GameResult r : results) {
			if (r.solved()) {
				solved++;
				totalGuesses += r.numGuesses();
				distribution[r.numGuesses() - 1]++;
			} else {
				failures.add(r.answer());
			}
		}
		double avg = solved == 0 ? 0 : totalGuesses / (double) solved;

		List<GameResult> hardest = results.stream()
				.sorted(Comparator.comparing(GameResult::solved) // failures first
						.thenComparing(Comparator.comparingInt(GameResult::numGuesses).reversed())
						.thenComparing(GameResult::answer))
				.limit(HARDEST_LIMIT)
				.toList();

		return new Result(solver.config(), answers.size(), solved, answers.size() - solved,
				avg, distribution, failures, hardest, durationMs);
	}

	/** Deterministic random sample of the answer list. */
	public static List<String> sample(WordLists words, int size, long seed) {
		List<String> all = new ArrayList<>(words.answers());
		java.util.Collections.shuffle(all, new Random(seed));
		return List.copyOf(all.subList(0, Math.min(size, all.size())));
	}

	/**
	 * Standalone harness: {@code java ...Benchmark [opener ...]} runs the
	 * full answer list once per opener (default: salet) and prints summaries.
	 */
	public static void main(String[] args) {
		WordLists words = WordLists.load();
		System.out.printf("loaded %d answers, %d guesses%n", words.answerCount(), words.guessCount());

		long t0 = System.nanoTime();
		PatternMatrix matrix = PatternMatrix.build(words);
		System.out.printf("pattern matrix built in %d ms%n%n", (System.nanoTime() - t0) / 1_000_000);

		String[] openers = args.length > 0 ? args : new String[] { SolverConfig.DEFAULT_OPENER };
		for (String opener : openers) {
			EntropySolver solver = new EntropySolver(words, matrix, SolverConfig.defaults().withOpener(opener));
			System.out.println(runAll(solver).summary());
		}
	}
}

package com.bingmerfest.wordle.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * M3 regression check (CLAUDE.md §4.5): with the default config
 * (SALET, easy mode, full guess pool) the solver should average ≈3.4–3.6
 * guesses with ≈100% win rate over the full classic answer list.
 *
 * Runs the full 2,309-answer benchmark; expect several seconds.
 */
class BenchmarkRegressionTest {

	@Test
	void fullBenchmarkMeetsExpectedPerformance() {
		EntropySolver solver = TestFixtures.solver(SolverConfig.defaults());
		Benchmark.Result result = Benchmark.runAll(solver);
		System.out.println(result.summary());

		assertEquals(2309, result.answerCount());
		assertTrue(result.winRate() >= 0.995,
				"win rate too low: " + result.winRate() + "\n" + result.summary());
		assertTrue(result.avgGuesses() >= 3.0 && result.avgGuesses() <= 3.7,
				"avg guesses out of expected band: " + result.avgGuesses() + "\n" + result.summary());
	}

	@Test
	void seededSampleIsDeterministic() {
		WordLists words = TestFixtures.words();
		assertEquals(Benchmark.sample(words, 25, 42L), Benchmark.sample(words, 25, 42L));
		assertEquals(25, Benchmark.sample(words, 25, 42L).size());
	}
}

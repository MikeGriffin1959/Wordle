package com.bingmerfest.wordle.engine;

/**
 * Shares the loaded word lists and the ~30 MB pattern matrix across test
 * classes so they're built once per JVM, not once per class.
 */
final class TestFixtures {

	private static WordLists words;
	private static PatternMatrix matrix;

	private TestFixtures() {
	}

	static synchronized WordLists words() {
		if (words == null) {
			words = WordLists.load();
		}
		return words;
	}

	static synchronized PatternMatrix matrix() {
		if (matrix == null) {
			matrix = PatternMatrix.build(words());
		}
		return matrix;
	}

	static EntropySolver solver(SolverConfig config) {
		return new EntropySolver(words(), matrix(), config);
	}
}

package com.bingmerfest.wordle.engine;

import java.util.Locale;

/**
 * Solver configuration. Immutable; use the {@code withX} methods to derive
 * variants (e.g. for opener comparisons in the benchmark harness).
 *
 * @param opener    fixed first guess; precomputed/hardcoded so games skip the
 *                  expensive first full scan. SALET is the standard strong
 *                  opener (~5.8 bits); SOARE/CRANE/TRACE are close.
 * @param hardMode  when true, every guess must be consistent with all clues
 *                  received so far (it could itself be the answer). Default
 *                  false: any legal guess is allowed, maximizing information.
 * @param guessPool which words the solver may guess from turn 2 on
 * @param tiebreak  how equal-entropy guesses are broken
 */
public record SolverConfig(String opener, boolean hardMode, GuessPool guessPool, Tiebreak tiebreak) {

	public static final int MAX_GUESSES = 6;
	public static final String DEFAULT_OPENER = "salet";

	public enum GuessPool {
		/** All ~12,947 accepted guess words. */
		FULL,
		/** Only the ~2,309 curated answer words. */
		ANSWERS_ONLY
	}

	public enum Tiebreak {
		/**
		 * Among max-entropy guesses, prefer one that is itself a remaining
		 * candidate (it might be the answer and end the game a turn early);
		 * then first in lexicographic order. Deterministic.
		 */
		PREFER_CANDIDATE,
		/** Plain first-in-lexicographic-order. Deterministic. */
		FIRST_BY_INDEX
	}

	public SolverConfig {
		opener = opener.toLowerCase(Locale.ROOT);
		if (opener.length() != PatternService.WORD_LENGTH) {
			throw new IllegalArgumentException("Opener must be a 5-letter word: " + opener);
		}
		if (guessPool == null || tiebreak == null) {
			throw new IllegalArgumentException("guessPool and tiebreak are required");
		}
	}

	public static SolverConfig defaults() {
		return new SolverConfig(DEFAULT_OPENER, false, GuessPool.FULL, Tiebreak.PREFER_CANDIDATE);
	}

	public SolverConfig withOpener(String newOpener) {
		return new SolverConfig(newOpener, hardMode, guessPool, tiebreak);
	}

	public SolverConfig withHardMode(boolean newHardMode) {
		return new SolverConfig(opener, newHardMode, guessPool, tiebreak);
	}

	public SolverConfig withGuessPool(GuessPool newGuessPool) {
		return new SolverConfig(opener, hardMode, newGuessPool, tiebreak);
	}

	public SolverConfig withTiebreak(Tiebreak newTiebreak) {
		return new SolverConfig(opener, hardMode, guessPool, newTiebreak);
	}
}

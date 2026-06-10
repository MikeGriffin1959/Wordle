package com.bingmerfest.wordle.engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Entropy-maximizing Wordle solver — the brain. Pure information theory; no
 * Spring, no LLM, no external calls.
 *
 * <p>Given the remaining candidate set R, each potential guess g partitions R
 * by feedback pattern; the solver picks argmax of
 * {@code H(g) = -Σ p·log2(p)} over the partition, with a deterministic
 * tiebreak (see {@link SolverConfig.Tiebreak}). When |R| == 1 it guesses the
 * answer; on turn 1 it plays the configured precomputed opener.
 *
 * <p>The solver itself is immutable and thread-safe; per-game state lives in
 * {@link Game} instances (one per concurrent game).
 */
public final class EntropySolver {

	private static final double EPS = 1e-9;
	private static final double LN2 = Math.log(2);

	private final WordLists words;
	private final PatternMatrix matrix;
	private final SolverConfig config;
	private final int openerGuessIdx;
	private final int[] basePool;   // guess indices the solver may pick from (per config.guessPool)
	private final double[] cLog2C;  // cLog2C[c] = c * log2(c), c up to answerCount

	public EntropySolver(WordLists words, PatternMatrix matrix, SolverConfig config) {
		this.words = words;
		this.matrix = matrix;
		this.config = config;

		this.openerGuessIdx = words.guessIndexOf(config.opener());
		if (openerGuessIdx < 0) {
			throw new IllegalArgumentException("Opener is not a valid guess word: " + config.opener());
		}

		if (config.guessPool() == SolverConfig.GuessPool.ANSWERS_ONLY) {
			this.basePool = new int[words.answerCount()];
			for (int a = 0; a < words.answerCount(); a++) {
				basePool[a] = words.guessIndexOfAnswer(a);
			}
		} else {
			this.basePool = new int[words.guessCount()];
			for (int g = 0; g < words.guessCount(); g++) {
				basePool[g] = g;
			}
		}

		this.cLog2C = new double[words.answerCount() + 1];
		for (int c = 2; c < cLog2C.length; c++) {
			cLog2C[c] = c * (Math.log(c) / LN2);
		}
	}

	public WordLists words() {
		return words;
	}

	public SolverConfig config() {
		return config;
	}

	/** Starts a fresh game with all answers as candidates. */
	public Game newGame() {
		return new Game();
	}

	/**
	 * Self-plays the given answer to completion (max 6 guesses). Feedback is
	 * computed directly from the answer word, so this also behaves sensibly
	 * for an answer outside the bundled answer list (e.g. a future
	 * NYT-curated word): candidates may empty out and the game is recorded
	 * as a failure instead of blowing up.
	 */
	public GameResult solve(String answer) {
		String target = answer.trim().toLowerCase(Locale.ROOT);
		if (target.length() != PatternService.WORD_LENGTH) {
			throw new IllegalArgumentException("Answer must be a 5-letter word: " + answer);
		}
		Game game = newGame();
		for (int t = 0; t < SolverConfig.MAX_GUESSES; t++) {
			Suggestion suggestion = game.suggest();
			int pattern = PatternService.compute(suggestion.word(), target);
			Turn turn = game.apply(suggestion.word(), pattern);
			if (turn.isWin()) {
				return new GameResult(target, true, List.copyOf(game.history()));
			}
			if (game.candidateCount() == 0) {
				break; // answer is not in the candidate list — unwinnable
			}
		}
		return new GameResult(target, false, List.copyOf(game.history()));
	}

	/**
	 * Full scan for the best opening guess (no opener shortcut). Expensive —
	 * intended for offline comparison, not the request path.
	 */
	public Suggestion computeBestOpener() {
		Game game = new Game();
		return game.scanPool(basePool);
	}

	/**
	 * One game in progress. Tracks the remaining candidate answers and (in
	 * hard mode) the clue-consistent guess pool. Not thread-safe; use one
	 * instance per game.
	 */
	public final class Game {

		private int[] candidates;      // answer indices, ascending
		private int[] hardPool;        // hard mode only: clue-consistent guess indices
		private final List<Turn> history = new ArrayList<>();

		private Game() {
			candidates = new int[words.answerCount()];
			for (int a = 0; a < candidates.length; a++) {
				candidates[a] = a;
			}
			hardPool = config.hardMode() ? basePool.clone() : null;
		}

		public int candidateCount() {
			return candidates.length;
		}

		public List<Turn> history() {
			return List.copyOf(history);
		}

		/** Up to {@code limit} of the remaining candidate words, in order. */
		public List<String> remainingWords(int limit) {
			List<String> out = new ArrayList<>(Math.min(limit, candidates.length));
			for (int i = 0; i < candidates.length && i < limit; i++) {
				out.add(words.answerWord(candidates[i]));
			}
			return out;
		}

		/** Proposes the next guess for the current candidate set. */
		public Suggestion suggest() {
			if (candidates.length == 0) {
				throw new IllegalStateException(
						"No candidates remain — the feedback entered is inconsistent with the word list");
			}
			if (candidates.length == 1) {
				return new Suggestion(words.answerWord(candidates[0]), 0.0, 1);
			}
			if (history.isEmpty()) {
				// Precomputed opener: skip the expensive first full scan.
				return new Suggestion(words.guessWord(openerGuessIdx),
						entropyOf(openerGuessIdx, new int[PatternService.PATTERN_COUNT]),
						candidates.length);
			}
			return scanPool(hardPool != null ? hardPool : basePool);
		}

		/**
		 * Applies observed feedback for a played guess, narrowing the
		 * candidate set (and, in hard mode, the guess pool).
		 */
		public Turn apply(String guess, int pattern) {
			String word = guess.trim().toLowerCase(Locale.ROOT);
			int g = words.guessIndexOf(word);
			if (g < 0) {
				throw new IllegalArgumentException("Not a valid guess word: " + guess);
			}
			if (pattern < 0 || pattern >= PatternService.PATTERN_COUNT) {
				throw new IllegalArgumentException("Pattern out of range 0..242: " + pattern);
			}
			int before = candidates.length;
			int[] kept = new int[before];
			int k = 0;
			for (int a : candidates) {
				if (matrix.pattern(g, a) == pattern) {
					kept[k++] = a;
				}
			}
			candidates = Arrays.copyOf(kept, k);

			if (hardPool != null) {
				// Hard mode: future guesses must be clue-consistent, i.e. the
				// guess word, played as a hypothetical answer, would have
				// produced exactly this feedback. Guess words outside the
				// answer list aren't in the matrix, so compute directly.
				int[] keptPool = new int[hardPool.length];
				int m = 0;
				for (int gi : hardPool) {
					if (PatternService.compute(word, words.guessWord(gi)) == pattern) {
						keptPool[m++] = gi;
					}
				}
				hardPool = Arrays.copyOf(keptPool, m);
			}

			Turn turn = new Turn(history.size() + 1, word, pattern, before, candidates.length);
			history.add(turn);
			return turn;
		}

		private Suggestion scanPool(int[] pool) {
			boolean[] isCandidate = new boolean[words.guessCount()];
			for (int a : candidates) {
				isCandidate[words.guessIndexOfAnswer(a)] = true;
			}
			boolean preferCandidate = config.tiebreak() == SolverConfig.Tiebreak.PREFER_CANDIDATE;

			int[] counts = new int[PatternService.PATTERN_COUNT];
			double bestH = -1;
			int bestG = -1;
			boolean bestIsCandidate = false;
			for (int g : pool) { // ascending index == lexicographic order
				double h = entropyOf(g, counts);
				if (bestG < 0 || h > bestH + EPS) {
					bestG = g;
					bestH = h;
					bestIsCandidate = isCandidate[g];
				} else if (preferCandidate && h >= bestH - EPS && isCandidate[g] && !bestIsCandidate) {
					bestG = g;
					bestH = Math.max(bestH, h);
					bestIsCandidate = true;
				}
			}
			return new Suggestion(words.guessWord(bestG), bestH, candidates.length);
		}

		/**
		 * H(g) over the current candidates. {@code counts} is caller-provided
		 * scratch space (all zeros on entry) and is left re-zeroed on exit so
		 * it can be reused across the pool scan without a 243-slot clear.
		 */
		private double entropyOf(int g, int[] counts) {
			int n = candidates.length;
			for (int a : candidates) {
				counts[matrix.pattern(g, a)]++;
			}
			double sumCLog2C = 0;
			for (int a : candidates) {
				int p = matrix.pattern(g, a);
				int c = counts[p];
				if (c != 0) {
					sumCLog2C += cLog2C[c];
					counts[p] = 0;
				}
			}
			// H = log2(n) - (1/n)·Σ c·log2(c)
			return (Math.log(n) / LN2) - (sumCLog2C / n);
		}
	}
}

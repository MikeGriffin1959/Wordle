package com.bingmerfest.wordle.engine;

/**
 * Computes Wordle tile feedback for a (guess, answer) pair and encodes it as a
 * base-3 integer: gray=0, yellow=1, green=2, leftmost tile most significant.
 * Range 0..242 (243 possible patterns); 242 is all-green.
 *
 * <p>Duplicate letters are handled with the required two-pass algorithm:
 * pass 1 marks greens and consumes those letters from the answer's multiset;
 * pass 2 marks a non-green tile yellow only if the guessed letter still has
 * remaining (unconsumed) count in the answer, consuming one per yellow.
 *
 * <p>Pure static utility; no Spring, no state. Words must be 5 lowercase
 * letters a-z (guaranteed by {@link WordLists}).
 */
public final class PatternService {

	public static final int WORD_LENGTH = 5;
	public static final int PATTERN_COUNT = 243; // 3^5
	public static final int ALL_GREEN = 242;     // base-3 "22222"
	public static final int ALL_GRAY = 0;

	public static final int GRAY = 0;
	public static final int YELLOW = 1;
	public static final int GREEN = 2;

	private PatternService() {
	}

	/**
	 * Two-pass feedback computation. Both words must be 5 lowercase letters.
	 */
	public static int compute(String guess, String answer) {
		if (guess.length() != WORD_LENGTH || answer.length() != WORD_LENGTH) {
			throw new IllegalArgumentException("Words must be 5 letters: " + guess + " / " + answer);
		}
		int[] remaining = new int[26];
		int[] codes = new int[WORD_LENGTH];

		// Pass 1: greens; build the multiset of unmatched answer letters.
		for (int i = 0; i < WORD_LENGTH; i++) {
			char g = guess.charAt(i);
			char a = answer.charAt(i);
			if (g == a) {
				codes[i] = GREEN;
			} else {
				remaining[a - 'a']++;
			}
		}

		// Pass 2: yellows consume from the multiset; otherwise gray.
		for (int i = 0; i < WORD_LENGTH; i++) {
			if (codes[i] == GREEN) {
				continue;
			}
			int c = guess.charAt(i) - 'a';
			if (remaining[c] > 0) {
				codes[i] = YELLOW;
				remaining[c]--;
			}
		}

		int pattern = 0;
		for (int i = 0; i < WORD_LENGTH; i++) {
			pattern = pattern * 3 + codes[i];
		}
		return pattern;
	}

	/**
	 * Renders a pattern as five letters, leftmost tile first: G=green,
	 * Y=yellow, B=gray. E.g. {@code format(242)} is {@code "GGGGG"}.
	 */
	public static String format(int pattern) {
		checkRange(pattern);
		char[] out = new char[WORD_LENGTH];
		for (int i = WORD_LENGTH - 1; i >= 0; i--) {
			out[i] = switch (pattern % 3) {
				case GREEN -> 'G';
				case YELLOW -> 'Y';
				default -> 'B';
			};
			pattern /= 3;
		}
		return new String(out);
	}

	/**
	 * Parses user-entered feedback. Accepts G/Y/B (any case) or digits 2/1/0,
	 * e.g. "GYBBG", "gybbg", "21002".
	 */
	public static int parse(String tiles) {
		if (tiles == null || tiles.length() != WORD_LENGTH) {
			throw new IllegalArgumentException("Feedback must be 5 tiles, e.g. GYBBG: " + tiles);
		}
		int pattern = 0;
		for (int i = 0; i < WORD_LENGTH; i++) {
			int code = switch (Character.toUpperCase(tiles.charAt(i))) {
				case 'G', '2' -> GREEN;
				case 'Y', '1' -> YELLOW;
				case 'B', '0' -> GRAY;
				default -> throw new IllegalArgumentException(
						"Bad tile '" + tiles.charAt(i) + "' in: " + tiles + " (use G/Y/B)");
			};
			pattern = pattern * 3 + code;
		}
		return pattern;
	}

	private static void checkRange(int pattern) {
		if (pattern < 0 || pattern >= PATTERN_COUNT) {
			throw new IllegalArgumentException("Pattern out of range 0..242: " + pattern);
		}
	}
}

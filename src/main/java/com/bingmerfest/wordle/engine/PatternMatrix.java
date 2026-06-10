package com.bingmerfest.wordle.engine;

import java.util.stream.IntStream;

/**
 * Precomputed feedback table over the full guess list × full answer list:
 * {@code pattern(guessIdx, answerIdx)} in 0..242. Stored as bytes
 * (~12,947 × ~2,309 ≈ 30 MB) and read back unsigned.
 *
 * <p>Built once at startup in parallel — takes on the order of a second; no
 * disk cache needed. Immutable and thread-safe after construction.
 */
public final class PatternMatrix {

	private final byte[][] table; // [guessIdx][answerIdx]

	private PatternMatrix(byte[][] table) {
		this.table = table;
	}

	public static PatternMatrix build(WordLists words) {
		byte[][] table = new byte[words.guessCount()][];
		IntStream.range(0, words.guessCount()).parallel().forEach(g -> {
			String guess = words.guessWord(g);
			byte[] row = new byte[words.answerCount()];
			for (int a = 0; a < row.length; a++) {
				row[a] = (byte) PatternService.compute(guess, words.answerWord(a));
			}
			table[g] = row;
		});
		return new PatternMatrix(table);
	}

	/** Feedback pattern (0..242) for guess {@code guessIdx} against answer {@code answerIdx}. */
	public int pattern(int guessIdx, int answerIdx) {
		return table[guessIdx][answerIdx] & 0xFF;
	}
}

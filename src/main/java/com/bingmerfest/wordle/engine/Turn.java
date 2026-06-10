package com.bingmerfest.wordle.engine;

/**
 * One completed turn: a guess, the feedback pattern it received, and the
 * candidate-set collapse it caused.
 */
public record Turn(int turnNumber, String guess, int pattern, int candidatesBefore, int candidatesAfter) {

	/** Actual information gained: log2(before/after). NaN if the feedback eliminated everything. */
	public double bitsGained() {
		if (candidatesAfter <= 0) {
			return Double.NaN;
		}
		return Math.log((double) candidatesBefore / candidatesAfter) / Math.log(2);
	}

	public boolean isWin() {
		return pattern == PatternService.ALL_GREEN;
	}

	public String patternString() {
		return PatternService.format(pattern);
	}
}

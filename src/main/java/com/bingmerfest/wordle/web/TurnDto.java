package com.bingmerfest.wordle.web;

import java.util.List;

import com.bingmerfest.wordle.engine.Turn;

/**
 * One turn as sent to the browser: pattern as a "GYBBG" string, expected vs
 * actual bits, and a small sample of the surviving candidates.
 */
public record TurnDto(int turnNumber, String word, String pattern, double expectedBits,
		double bitsGained, int candidatesBefore, int candidatesAfter, List<String> remainingSample) {

	public static TurnDto of(Turn turn, List<String> remainingSample) {
		return new TurnDto(turn.turnNumber(), turn.guess(), turn.patternString(),
				turn.expectedBits(),
				turn.candidatesAfter() == 0 ? 0 : turn.bitsGained(),
				turn.candidatesBefore(), turn.candidatesAfter(), remainingSample);
	}
}

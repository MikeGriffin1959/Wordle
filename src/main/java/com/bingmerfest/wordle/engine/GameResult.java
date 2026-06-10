package com.bingmerfest.wordle.engine;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Outcome of one self-played game.
 */
public record GameResult(String answer, boolean solved, List<Turn> turns) {

	public int numGuesses() {
		return turns.size();
	}

	public List<String> guessWords() {
		return turns.stream().map(Turn::guess).toList();
	}

	@Override
	public String toString() {
		return answer + (solved ? " solved in " + numGuesses() : " FAILED after " + numGuesses())
				+ " [" + turns.stream().map(Turn::guess).collect(Collectors.joining(" → ")) + "]";
	}
}

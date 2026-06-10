package com.bingmerfest.wordle.engine;

/**
 * A proposed guess: the word, the expected information gain in bits
 * (entropy of the partition it induces on the remaining candidates), and the
 * candidate count it was computed against.
 */
public record Suggestion(String word, double expectedBits, int candidatesBefore) {
}

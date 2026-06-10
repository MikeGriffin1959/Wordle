package com.bingmerfest.wordle.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class WordListsTest {

	@Test
	void loadsBundledLists() {
		WordLists words = TestFixtures.words();
		assertEquals(2309, words.answerCount());
		assertEquals(12947, words.guessCount());
	}

	@Test
	void everyAnswerIsAValidGuess() {
		WordLists words = TestFixtures.words();
		for (int a = 0; a < words.answerCount(); a++) {
			String answer = words.answerWord(a);
			assertTrue(words.isValidGuess(answer), answer);
			assertEquals(answer, words.guessWord(words.guessIndexOfAnswer(a)));
		}
	}

	@Test
	void listsAreSortedLowercaseFiveLetters() {
		WordLists words = TestFixtures.words();
		String prev = "";
		for (String g : words.guesses()) {
			assertEquals(5, g.length());
			assertEquals(g.toLowerCase(), g);
			assertTrue(prev.compareTo(g) < 0, "not sorted/deduped at: " + g);
			prev = g;
		}
	}

	@Test
	void saletIsAValidGuessButNotAnAnswer() {
		WordLists words = TestFixtures.words();
		assertTrue(words.isValidGuess("salet"));
		assertTrue(!words.isAnswer("salet"));
	}

	@Test
	void lookupsAreCaseInsensitiveAndMissingWordsReturnMinusOne() {
		WordLists words = TestFixtures.words();
		assertTrue(words.guessIndexOf("SALET") >= 0);
		assertEquals(-1, words.guessIndexOf("zzzzz"));
		assertEquals(-1, words.answerIndexOf("salet"));
	}

	@Test
	void rejectsMalformedWords() {
		assertThrows(IllegalArgumentException.class,
				() -> new WordLists(List.of("toolong"), List.of()));
		assertThrows(IllegalArgumentException.class,
				() -> new WordLists(List.of("ab1de"), List.of()));
		assertThrows(IllegalArgumentException.class,
				() -> new WordLists(List.of(), List.of("salet")));
	}
}

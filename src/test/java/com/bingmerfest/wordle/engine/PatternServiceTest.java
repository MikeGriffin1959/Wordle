package com.bingmerfest.wordle.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Known-pattern tests, especially the duplicate-letter cases that break naive
 * per-letter feedback (CLAUDE.md §4.1). Patterns are written as G/Y/B,
 * leftmost tile first.
 */
class PatternServiceTest {

	@ParameterizedTest(name = "{0} vs {1} -> {2}")
	@CsvSource({
			// guess, answer, expected
			"speed, erase, YBYYB", // duplicate E in guess, duplicate E in answer
			"erase, speed, YBBYY", // reversed
			"sissy, essay, YBGBG", // S twice in guess, S twice in answer, one green
			"stats, toast, YYGYB", // T twice in guess vs T twice in answer
			"mamma, maxim, GGYBB", // M three times in guess: one yellow, then exhausted
			"allee, eagle, YYBYG", // L twice in guess, once in answer; E green consumes one
			"geese, those, BBBGG", // trailing greens; extra E's all gray
			"crane, crane, GGGGG", // all green
			"fight, mambo, BBBBB", // fully disjoint
	})
	void knownPatterns(String guess, String answer, String expected) {
		assertEquals(expected, PatternService.format(PatternService.compute(guess, answer)));
	}

	@Test
	void allGreenIs242AllGrayIs0() {
		assertEquals(PatternService.ALL_GREEN, PatternService.compute("crane", "crane"));
		assertEquals(242, PatternService.ALL_GREEN);
		assertEquals(0, PatternService.compute("fight", "mambo"));
	}

	@Test
	void base3EncodingIsLeftmostMostSignificant() {
		// YBYYB = 1,0,1,1,0 -> 1*81 + 0*27 + 1*9 + 1*3 + 0 = 93
		assertEquals(93, PatternService.compute("speed", "erase"));
		// GGYBB = 2,2,1,0,0 -> 2*81 + 2*27 + 1*9 = 225
		assertEquals(225, PatternService.compute("mamma", "maxim"));
	}

	@Test
	void formatParseRoundTripsAllPatterns() {
		for (int p = 0; p < PatternService.PATTERN_COUNT; p++) {
			assertEquals(p, PatternService.parse(PatternService.format(p)));
		}
	}

	@Test
	void parseAcceptsCaseAndDigits() {
		assertEquals(242, PatternService.parse("GGGGG"));
		assertEquals(242, PatternService.parse("ggggg"));
		assertEquals(242, PatternService.parse("22222"));
		assertEquals(93, PatternService.parse("YbyYb"));
		assertEquals(93, PatternService.parse("10110"));
	}

	@Test
	void parseRejectsBadInput() {
		assertThrows(IllegalArgumentException.class, () -> PatternService.parse(null));
		assertThrows(IllegalArgumentException.class, () -> PatternService.parse("GGGG"));
		assertThrows(IllegalArgumentException.class, () -> PatternService.parse("GGGGGG"));
		assertThrows(IllegalArgumentException.class, () -> PatternService.parse("GXGGG"));
	}

	@Test
	void computeRejectsWrongLength() {
		assertThrows(IllegalArgumentException.class, () -> PatternService.compute("ab", "crane"));
		assertThrows(IllegalArgumentException.class, () -> PatternService.compute("crane", "abcdef"));
	}

	@Test
	void yellowsConsumeAnswerLettersLeftToRight() {
		// "eeeee" vs erase: greens at 0 and 4 consume both E's; middle E's gray.
		assertEquals("GBBBG", PatternService.format(PatternService.compute("eeeee", "erase")));
		// alaap vs salad: green A at pos 3; first A yellow, second A exhausted.
		assertEquals("YYBGB", PatternService.format(PatternService.compute("alaap", "salad")));
	}
}

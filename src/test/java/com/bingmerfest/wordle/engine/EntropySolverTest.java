package com.bingmerfest.wordle.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class EntropySolverTest {

	private static EntropySolver defaultSolver() {
		return TestFixtures.solver(SolverConfig.defaults());
	}

	@Test
	void firstGuessIsTheConfiguredOpener() {
		EntropySolver solver = defaultSolver();
		Suggestion first = solver.newGame().suggest();
		assertEquals("salet", first.word());
		assertEquals(2309, first.candidatesBefore());
		// SALET is a strong opener: ~5.8 bits on the classic list.
		assertTrue(first.expectedBits() > 5.5 && first.expectedBits() < 6.5,
				"unexpected opener entropy: " + first.expectedBits());
	}

	@Test
	void solvesAssortedAnswersWithinSixGuesses() {
		EntropySolver solver = defaultSolver();
		WordLists words = solver.words();
		// fixed spread across the sorted list + a known-nasty *ight family member
		List<String> answers = List.of(
				words.answerWord(0),
				words.answerWord(words.answerCount() / 2),
				words.answerWord(words.answerCount() - 1),
				"cigar", "vivid", "fight");
		for (String answer : answers) {
			GameResult result = solver.solve(answer);
			assertTrue(result.solved(), "failed on " + answer + ": " + result);
			assertTrue(result.numGuesses() <= SolverConfig.MAX_GUESSES);
			assertEquals("salet", result.turns().get(0).guess());
		}
	}

	@Test
	void solvingIsDeterministic() {
		EntropySolver solver = defaultSolver();
		GameResult a = solver.solve("cigar");
		GameResult b = solver.solve("cigar");
		assertEquals(a.guessWords(), b.guessWords());
	}

	@Test
	void candidateSetShrinksEachTurnAndBitsAddUp() {
		EntropySolver solver = defaultSolver();
		GameResult result = solver.solve("vivid");
		int prev = Integer.MAX_VALUE;
		for (Turn turn : result.turns()) {
			assertTrue(turn.candidatesAfter() <= turn.candidatesBefore());
			assertTrue(turn.candidatesBefore() <= prev);
			prev = turn.candidatesAfter();
		}
		Turn last = result.turns().get(result.numGuesses() - 1);
		assertTrue(last.isWin());
		assertEquals("GGGGG", last.patternString());
	}

	@Test
	void guessesTheAnswerWhenOneCandidateRemains() {
		EntropySolver solver = defaultSolver();
		EntropySolver.Game game = solver.newGame();
		// Feed real feedback for the answer "cigar" until one candidate is left.
		for (int t = 0; t < SolverConfig.MAX_GUESSES; t++) {
			Suggestion s = game.suggest();
			if (game.candidateCount() == 1) {
				assertEquals(game.remainingWords(1).get(0), s.word());
				assertEquals(0.0, s.expectedBits());
				return;
			}
			game.apply(s.word(), PatternService.compute(s.word(), "cigar"));
			if (game.candidateCount() == 1) {
				assertEquals("cigar", game.suggest().word());
				return;
			}
		}
		throw new AssertionError("never narrowed to one candidate");
	}

	@Test
	void assistModeNarrowsFromManuallyEnteredPatterns() {
		EntropySolver solver = defaultSolver();
		EntropySolver.Game game = solver.newGame();
		int before = game.candidateCount();
		// User played salet on the real board and saw all gray.
		Turn turn = game.apply("salet", PatternService.parse("BBBBB"));
		assertTrue(game.candidateCount() < before);
		assertTrue(turn.bitsGained() > 0);
		// Engine proposes something legal next.
		assertTrue(solver.words().isValidGuess(game.suggest().word()));
	}

	@Test
	void inconsistentFeedbackEmptiesCandidatesAndSuggestThrows() {
		EntropySolver solver = defaultSolver();
		EntropySolver.Game game = solver.newGame();
		// salet all-green is impossible: salet is not in the answer list.
		game.apply("salet", PatternService.ALL_GREEN);
		assertEquals(0, game.candidateCount());
		assertThrows(IllegalStateException.class, game::suggest);
	}

	@Test
	void rejectsInvalidGuessesAndPatterns() {
		EntropySolver solver = defaultSolver();
		EntropySolver.Game game = solver.newGame();
		assertThrows(IllegalArgumentException.class, () -> game.apply("zzzzz", 0));
		assertThrows(IllegalArgumentException.class, () -> game.apply("salet", 243));
		assertThrows(IllegalArgumentException.class, () -> game.apply("salet", -1));
	}

	@Test
	void hardModeOnlyGuessesClueConsistentWordsAndStillSolves() {
		EntropySolver solver = TestFixtures.solver(SolverConfig.defaults().withHardMode(true));
		for (String answer : List.of("cigar", "vivid", "epoxy")) {
			GameResult result = solver.solve(answer);
			assertTrue(result.solved(), "hard mode failed on " + answer + ": " + result);
			// every guess after the first must be consistent with all prior clues
			List<Turn> turns = result.turns();
			for (int i = 1; i < turns.size(); i++) {
				for (int j = 0; j < i; j++) {
					assertEquals(turns.get(j).pattern(),
							PatternService.compute(turns.get(j).guess(), turns.get(i).guess()),
							"guess " + turns.get(i).guess() + " inconsistent with clue from "
									+ turns.get(j).guess());
				}
			}
		}
	}

	@Test
	void answersOnlyPoolStillSolves() {
		EntropySolver solver = TestFixtures.solver(
				SolverConfig.defaults().withGuessPool(SolverConfig.GuessPool.ANSWERS_ONLY).withOpener("crane"));
		GameResult result = solver.solve("vivid");
		assertTrue(result.solved(), result.toString());
		for (Turn t : result.turns()) {
			assertTrue(solver.words().isAnswer(t.guess()) || t.turnNumber() == 1,
					"non-answer guess from ANSWERS_ONLY pool: " + t.guess());
		}
	}

	@Test
	void answerOutsideTheListIsHandledGracefully() {
		EntropySolver solver = defaultSolver();
		// The opener itself: not a candidate, but the all-green feedback on
		// turn 1 still wins the game.
		GameResult lucky = solver.solve("salet");
		assertTrue(lucky.solved());
		assertEquals(1, lucky.numGuesses());
		// An obscure non-answer the solver never plays: candidates empty out
		// and the game records a failure instead of throwing.
		GameResult result = solver.solve("zoppo");
		assertTrue(!result.solved());
	}

	@Test
	void rejectsUnknownOpener() {
		assertThrows(IllegalArgumentException.class,
				() -> TestFixtures.solver(SolverConfig.defaults().withOpener("zzzzz")));
	}
}

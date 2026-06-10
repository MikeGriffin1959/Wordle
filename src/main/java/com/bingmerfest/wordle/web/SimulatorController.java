package com.bingmerfest.wordle.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.bingmerfest.wordle.engine.EntropySolver;
import com.bingmerfest.wordle.engine.GameResult;
import com.bingmerfest.wordle.engine.PatternService;
import com.bingmerfest.wordle.engine.SolverConfig;
import com.bingmerfest.wordle.engine.Suggestion;
import com.bingmerfest.wordle.engine.Turn;
import com.bingmerfest.wordle.persistence.GameEntity;
import com.bingmerfest.wordle.persistence.GameMode;
import com.bingmerfest.wordle.persistence.GameRepository;
import com.bingmerfest.wordle.persistence.SolverConfigEntity;
import com.bingmerfest.wordle.persistence.SolverConfigRepository;

/**
 * Self-play simulator (M5). The board page animates games produced by the
 * JSON endpoint; each game picks a (seedable) random answer, is solved
 * server-side in one shot, and is persisted as a SIMULATOR game.
 */
@Controller
public class SimulatorController {

	private static final int REMAINING_SAMPLE_LIMIT = 8;

	private final EntropySolver solver;
	private final GameRepository games;
	private final SolverConfigRepository configs;

	public SimulatorController(EntropySolver solver, GameRepository games, SolverConfigRepository configs) {
		this.solver = solver;
		this.games = games;
		this.configs = configs;
	}

	@GetMapping("/simulator")
	public String page() {
		return "simulator";
	}

	public record TurnDto(int turnNumber, String word, String pattern, double expectedBits,
			double bitsGained, int candidatesBefore, int candidatesAfter, List<String> remainingSample) {
	}

	public record SimGameDto(long gameId, String answer, boolean solved, int numGuesses, List<TurnDto> turns) {
	}

	@GetMapping(value = "/simulator/game", produces = "application/json")
	@ResponseBody
	public SimGameDto playGame(@RequestParam(name = "seed", required = false) Long seed) {
		List<String> answers = solver.words().answers();
		int pick = seed != null
				? new Random(seed).nextInt(answers.size())
				: ThreadLocalRandom.current().nextInt(answers.size());
		String answer = answers.get(pick);

		EntropySolver.Game game = solver.newGame();
		List<TurnDto> turns = new ArrayList<>();
		boolean solved = false;
		for (int t = 0; t < SolverConfig.MAX_GUESSES && !solved; t++) {
			Suggestion suggestion = game.suggest();
			int pattern = PatternService.compute(suggestion.word(), answer);
			Turn turn = game.apply(suggestion.word(), pattern);
			turns.add(new TurnDto(turn.turnNumber(), turn.guess(), turn.patternString(),
					turn.expectedBits(),
					turn.candidatesAfter() == 0 ? 0 : turn.bitsGained(),
					turn.candidatesBefore(), turn.candidatesAfter(),
					game.remainingWords(REMAINING_SAMPLE_LIMIT)));
			solved = turn.isWin();
			if (game.candidateCount() == 0) {
				break;
			}
		}

		GameResult result = new GameResult(answer, solved, game.history());
		GameEntity saved = games.save(GameEntity.of(GameMode.SIMULATOR, result, defaultConfig()));
		return new SimGameDto(saved.getId(), answer, solved, turns.size(), turns);
	}

	private SolverConfigEntity defaultConfig() {
		return configs.findByName("default")
				.orElseGet(() -> configs.save(SolverConfigEntity.of("default", solver.config())));
	}
}

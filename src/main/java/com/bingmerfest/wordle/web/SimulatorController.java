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

		List<TurnDto> turns = new ArrayList<>();
		GameResult result = selfPlay(solver, answer, turns, REMAINING_SAMPLE_LIMIT);

		GameEntity saved = games.save(GameEntity.of(GameMode.SIMULATOR, result,
				configs.getOrCreate("default", solver.config())));
		return new SimGameDto(saved.getId(), answer, result.solved(), turns.size(), turns);
	}

	/**
	 * Plays one full game against {@code answer}, filling {@code turnsOut}
	 * with UI-ready turn DTOs. Shared with the daily AUTO mode.
	 */
	static GameResult selfPlay(EntropySolver solver, String answer, List<TurnDto> turnsOut, int sampleLimit) {
		EntropySolver.Game game = solver.newGame();
		boolean solved = false;
		for (int t = 0; t < SolverConfig.MAX_GUESSES && !solved; t++) {
			Suggestion suggestion = game.suggest();
			int pattern = PatternService.compute(suggestion.word(), answer);
			Turn turn = game.apply(suggestion.word(), pattern);
			turnsOut.add(TurnDto.of(turn, game.remainingWords(sampleLimit)));
			solved = turn.isWin();
			if (game.candidateCount() == 0) {
				break;
			}
		}
		return new GameResult(answer, solved, game.history());
	}
}

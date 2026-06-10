package com.bingmerfest.wordle.web;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import com.bingmerfest.wordle.engine.EntropySolver;
import com.bingmerfest.wordle.engine.GameResult;
import com.bingmerfest.wordle.engine.PatternService;
import com.bingmerfest.wordle.engine.SolverConfig;
import com.bingmerfest.wordle.engine.Suggestion;
import com.bingmerfest.wordle.engine.Turn;
import com.bingmerfest.wordle.nyt.NytPuzzle;
import com.bingmerfest.wordle.nyt.NytWordleClient;
import com.bingmerfest.wordle.persistence.DailyMode;
import com.bingmerfest.wordle.persistence.DailySolveEntity;
import com.bingmerfest.wordle.persistence.DailySolveRepository;
import com.bingmerfest.wordle.persistence.GameEntity;
import com.bingmerfest.wordle.persistence.GameMode;
import com.bingmerfest.wordle.persistence.GameRepository;
import com.bingmerfest.wordle.persistence.SolverConfigRepository;

import jakarta.servlet.http.HttpSession;

/**
 * Daily NYT solver (M6), two sub-modes:
 *
 * <ul>
 * <li><b>AUTO</b> — fetches today's solution from the NYT endpoint and
 * self-plays it (spoilers, obviously).</li>
 * <li><b>ASSIST</b> — honest advisor: the engine proposes a guess, the user
 * reports the pattern they observed on the real NYT board, the engine
 * narrows. The solution is never fetched. State is the per-session list of
 * (guess, pattern) pairs, replayed through a fresh game on every request —
 * which makes undo free.</li>
 * </ul>
 */
@Controller
public class DailyController {

	private static final int REMAINING_SAMPLE_LIMIT = 8;
	private static final String ASSIST_SESSION_KEY = "wordle.assistTurns";

	private final EntropySolver solver;
	private final NytWordleClient nyt;
	private final GameRepository games;
	private final SolverConfigRepository configs;
	private final DailySolveRepository dailySolves;

	public DailyController(EntropySolver solver, NytWordleClient nyt, GameRepository games,
			SolverConfigRepository configs, DailySolveRepository dailySolves) {
		this.solver = solver;
		this.nyt = nyt;
		this.games = games;
		this.configs = configs;
		this.dailySolves = dailySolves;
	}

	@GetMapping("/daily")
	public String page() {
		return "daily";
	}

	// ------------------------------------------------------------ AUTO mode

	public record DailyAutoDto(String printDate, int puzzleId, int daysSinceLaunch, String editor,
			String answer, boolean solved, int numGuesses, List<TurnDto> turns,
			boolean firstSolveToday, String error) {

		static DailyAutoDto error(String message) {
			return new DailyAutoDto(null, 0, 0, null, null, false, 0, List.of(), false, message);
		}
	}

	@GetMapping(value = "/daily/auto/game", produces = "application/json")
	@ResponseBody
	public DailyAutoDto autoGame() {
		NytPuzzle puzzle;
		try {
			puzzle = nyt.fetchToday();
		} catch (Exception e) {
			return DailyAutoDto.error("Could not fetch today's puzzle from NYT: " + e.getMessage());
		}

		List<TurnDto> turns = new ArrayList<>();
		GameResult result = SimulatorController.selfPlay(solver, puzzle.solution(), turns, REMAINING_SAMPLE_LIMIT);

		// Persist once per puzzle date; replays just re-animate.
		boolean first = dailySolves.findByPuzzleDateAndMode(puzzle.printDate(), DailyMode.AUTO).isEmpty();
		if (first) {
			games.save(GameEntity.of(GameMode.DAILY, result, configs.getOrCreate("default", solver.config())));
			dailySolves.save(DailySolveEntity.of(puzzle.printDate(), puzzle.id(), result.answer(),
					result.solved(), result.numGuesses(), DailyMode.AUTO));
		}
		return new DailyAutoDto(puzzle.printDate().toString(), puzzle.id(), puzzle.daysSinceLaunch(),
				puzzle.editor(), result.answer(), result.solved(), result.numGuesses(), turns, first, null);
	}

	// ---------------------------------------------------------- ASSIST mode

	record AppliedTurn(String guess, int pattern) implements Serializable {
	}

	public record AssistRequest(String guess, String pattern) {
	}

	public record AssistStateDto(List<TurnDto> turns, String suggestion, Double expectedBits,
			int candidates, List<String> remainingSample, boolean solved, boolean failed,
			boolean inconsistent, String error) {
	}

	@PostMapping(value = "/daily/assist/start", produces = "application/json")
	@ResponseBody
	public AssistStateDto assistStart(HttpSession session) {
		session.setAttribute(ASSIST_SESSION_KEY, new ArrayList<AppliedTurn>());
		return replay(List.of(), null);
	}

	@PostMapping(value = "/daily/assist/feedback", produces = "application/json")
	@ResponseBody
	public AssistStateDto assistFeedback(@RequestBody AssistRequest request, HttpSession session) {
		List<AppliedTurn> applied = sessionTurns(session);

		AssistStateDto current = replay(applied, null);
		if (current.solved() || current.failed()) {
			return replay(applied, "Game is already over — restart to begin a new one.");
		}

		int pattern;
		try {
			pattern = PatternService.parse(request.pattern());
		} catch (IllegalArgumentException e) {
			return replay(applied, e.getMessage());
		}
		String guess = request.guess() == null ? "" : request.guess().trim().toLowerCase();
		if (!solver.words().isValidGuess(guess)) {
			return replay(applied, "Not a valid guess word: " + guess);
		}

		applied.add(new AppliedTurn(guess, pattern));
		session.setAttribute(ASSIST_SESSION_KEY, applied);

		AssistStateDto state = replay(applied, null);
		if (state.solved() || state.failed()) {
			persistAssistResult(state, guess);
		}
		return state;
	}

	@PostMapping(value = "/daily/assist/undo", produces = "application/json")
	@ResponseBody
	public AssistStateDto assistUndo(HttpSession session) {
		List<AppliedTurn> applied = sessionTurns(session);
		if (!applied.isEmpty()) {
			applied.remove(applied.size() - 1);
			session.setAttribute(ASSIST_SESSION_KEY, applied);
		}
		return replay(applied, null);
	}

	@SuppressWarnings("unchecked")
	private List<AppliedTurn> sessionTurns(HttpSession session) {
		Object turns = session.getAttribute(ASSIST_SESSION_KEY);
		return turns instanceof List ? (List<AppliedTurn>) turns : new ArrayList<>();
	}

	/** Rebuilds the game from the applied turns and describes the resulting state. */
	private AssistStateDto replay(List<AppliedTurn> applied, String error) {
		EntropySolver.Game game = solver.newGame();
		List<TurnDto> turns = new ArrayList<>();
		boolean solved = false;
		for (AppliedTurn at : applied) {
			Turn turn = game.apply(at.guess(), at.pattern());
			turns.add(TurnDto.of(turn, game.remainingWords(REMAINING_SAMPLE_LIMIT)));
			solved = turn.isWin();
		}
		boolean failed = !solved && turns.size() >= SolverConfig.MAX_GUESSES;
		boolean inconsistent = !solved && game.candidateCount() == 0;

		String suggestion = null;
		Double expectedBits = null;
		if (!solved && !failed && !inconsistent) {
			Suggestion s = game.suggest();
			suggestion = s.word();
			expectedBits = s.expectedBits();
		}
		return new AssistStateDto(turns, suggestion, expectedBits, game.candidateCount(),
				game.remainingWords(REMAINING_SAMPLE_LIMIT), solved, failed, inconsistent, error);
	}

	/** Records the concluded assist game, once per day. */
	private void persistAssistResult(AssistStateDto state, String lastGuess) {
		LocalDate today = LocalDate.now();
		if (dailySolves.findByPuzzleDateAndMode(today, DailyMode.ASSIST).isEmpty()) {
			dailySolves.save(DailySolveEntity.of(today, null, state.solved() ? lastGuess : null,
					state.solved(), state.turns().size(), DailyMode.ASSIST));
		}
	}
}

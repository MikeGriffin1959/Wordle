package com.bingmerfest.wordle.persistence;

import java.time.Instant;
import java.util.stream.Collectors;

import com.bingmerfest.wordle.engine.GameResult;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * One played game, in any mode. Guesses are stored as a JSON array of
 * {@code {"word":...,"pattern":...}} objects in play order.
 */
@Entity
@Table(name = "game")
public class GameEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private GameMode mode;

	@Column(nullable = false, length = 5)
	private String answer;

	@Column(nullable = false)
	private boolean solved;

	@Column(name = "num_guesses", nullable = false)
	private int numGuesses;

	@Column(nullable = false, columnDefinition = "json")
	private String guesses;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "config_id")
	private SolverConfigEntity config;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected GameEntity() {
	}

	public static GameEntity of(GameMode mode, GameResult result, SolverConfigEntity config) {
		GameEntity e = new GameEntity();
		e.mode = mode;
		e.answer = result.answer();
		e.solved = result.solved();
		e.numGuesses = result.numGuesses();
		e.guesses = toJson(result);
		e.config = config;
		return e;
	}

	static String toJson(GameResult result) {
		return result.turns().stream()
				.map(t -> "{\"word\":\"" + t.guess() + "\",\"pattern\":" + t.pattern() + "}")
				.collect(Collectors.joining(",", "[", "]"));
	}

	@PrePersist
	void onCreate() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}

	public Long getId() {
		return id;
	}

	public GameMode getMode() {
		return mode;
	}

	public String getAnswer() {
		return answer;
	}

	public boolean isSolved() {
		return solved;
	}

	public int getNumGuesses() {
		return numGuesses;
	}

	public String getGuesses() {
		return guesses;
	}

	public SolverConfigEntity getConfig() {
		return config;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}

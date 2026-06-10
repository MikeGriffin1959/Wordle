package com.bingmerfest.wordle.persistence;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * One day's NYT puzzle solve (AUTO or ASSIST); at most one row per
 * (puzzle_date, mode).
 */
@Entity
@Table(name = "daily_solve")
public class DailySolveEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "puzzle_date", nullable = false)
	private LocalDate puzzleDate;

	@Column(name = "puzzle_id")
	private Integer puzzleId;

	@Column(length = 5)
	private String answer;

	@Column(nullable = false)
	private boolean solved;

	@Column(name = "num_guesses", nullable = false)
	private int numGuesses;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 8)
	private DailyMode mode;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected DailySolveEntity() {
	}

	public static DailySolveEntity of(LocalDate puzzleDate, Integer puzzleId, String answer,
			boolean solved, int numGuesses, DailyMode mode) {
		DailySolveEntity e = new DailySolveEntity();
		e.puzzleDate = puzzleDate;
		e.puzzleId = puzzleId;
		e.answer = answer;
		e.solved = solved;
		e.numGuesses = numGuesses;
		e.mode = mode;
		return e;
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

	public LocalDate getPuzzleDate() {
		return puzzleDate;
	}

	public Integer getPuzzleId() {
		return puzzleId;
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

	public DailyMode getMode() {
		return mode;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}

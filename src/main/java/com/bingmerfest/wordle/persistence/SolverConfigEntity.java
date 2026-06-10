package com.bingmerfest.wordle.persistence;

import java.time.Instant;

import com.bingmerfest.wordle.engine.SolverConfig;

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
 * A named, persisted solver configuration so benchmark runs can be compared
 * across openers/strategies over time.
 */
@Entity
@Table(name = "solver_config")
public class SolverConfigEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 64, unique = true)
	private String name;

	@Column(nullable = false, length = 5)
	private String opener;

	@Column(name = "hard_mode", nullable = false)
	private boolean hardMode;

	@Enumerated(EnumType.STRING)
	@Column(name = "guess_pool", nullable = false, length = 16)
	private SolverConfig.GuessPool guessPool;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private SolverConfig.Tiebreak tiebreak;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected SolverConfigEntity() {
	}

	public static SolverConfigEntity of(String name, SolverConfig config) {
		SolverConfigEntity e = new SolverConfigEntity();
		e.name = name;
		e.opener = config.opener();
		e.hardMode = config.hardMode();
		e.guessPool = config.guessPool();
		e.tiebreak = config.tiebreak();
		return e;
	}

	/** The engine-side value object this row describes. */
	public SolverConfig toSolverConfig() {
		return new SolverConfig(opener, hardMode, guessPool, tiebreak);
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

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getOpener() {
		return opener;
	}

	public boolean isHardMode() {
		return hardMode;
	}

	public SolverConfig.GuessPool getGuessPool() {
		return guessPool;
	}

	public SolverConfig.Tiebreak getTiebreak() {
		return tiebreak;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}

package com.bingmerfest.wordle.persistence;

import java.time.Instant;

import com.bingmerfest.wordle.engine.Benchmark;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Aggregated result of one benchmark run, linked to the config it tested so
 * openers/strategies can be compared over time.
 */
@Entity
@Table(name = "benchmark_run")
public class BenchmarkRunEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "config_id", nullable = false)
	private SolverConfigEntity config;

	@Column(name = "answer_count", nullable = false)
	private int answerCount;

	@Column(name = "win_rate", nullable = false)
	private double winRate;

	@Column(name = "avg_guesses", nullable = false)
	private double avgGuesses;

	@Column(name = "dist_1", nullable = false)
	private int dist1;

	@Column(name = "dist_2", nullable = false)
	private int dist2;

	@Column(name = "dist_3", nullable = false)
	private int dist3;

	@Column(name = "dist_4", nullable = false)
	private int dist4;

	@Column(name = "dist_5", nullable = false)
	private int dist5;

	@Column(name = "dist_6", nullable = false)
	private int dist6;

	@Column(nullable = false)
	private int fails;

	@Column(name = "duration_ms", nullable = false)
	private long durationMs;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected BenchmarkRunEntity() {
	}

	public static BenchmarkRunEntity of(Benchmark.Result result, SolverConfigEntity config) {
		BenchmarkRunEntity e = new BenchmarkRunEntity();
		e.config = config;
		e.answerCount = result.answerCount();
		e.winRate = result.winRate();
		e.avgGuesses = result.avgGuesses();
		int[] d = result.distribution();
		e.dist1 = d[0];
		e.dist2 = d[1];
		e.dist3 = d[2];
		e.dist4 = d[3];
		e.dist5 = d[4];
		e.dist6 = d[5];
		e.fails = result.failed();
		e.durationMs = result.durationMs();
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

	public SolverConfigEntity getConfig() {
		return config;
	}

	public int getAnswerCount() {
		return answerCount;
	}

	public double getWinRate() {
		return winRate;
	}

	public double getAvgGuesses() {
		return avgGuesses;
	}

	public int getDist1() {
		return dist1;
	}

	public int getDist2() {
		return dist2;
	}

	public int getDist3() {
		return dist3;
	}

	public int getDist4() {
		return dist4;
	}

	public int getDist5() {
		return dist5;
	}

	public int getDist6() {
		return dist6;
	}

	public int getFails() {
		return fails;
	}

	public long getDurationMs() {
		return durationMs;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}

package com.bingmerfest.wordle.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bingmerfest.wordle.engine.SolverConfig;

public interface SolverConfigRepository extends JpaRepository<SolverConfigEntity, Long> {

	Optional<SolverConfigEntity> findByName(String name);

	/** The persisted row for a named config, created on first use. */
	default SolverConfigEntity getOrCreate(String name, SolverConfig config) {
		return findByName(name).orElseGet(() -> save(SolverConfigEntity.of(name, config)));
	}
}

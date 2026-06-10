package com.bingmerfest.wordle.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SolverConfigRepository extends JpaRepository<SolverConfigEntity, Long> {

	Optional<SolverConfigEntity> findByName(String name);
}

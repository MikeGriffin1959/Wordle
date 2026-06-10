package com.bingmerfest.wordle.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRepository extends JpaRepository<GameEntity, Long> {

	List<GameEntity> findTop20ByModeOrderByCreatedAtDesc(GameMode mode);
}

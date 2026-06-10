package com.bingmerfest.wordle.persistence;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DailySolveRepository extends JpaRepository<DailySolveEntity, Long> {

	Optional<DailySolveEntity> findByPuzzleDateAndMode(LocalDate puzzleDate, DailyMode mode);
}

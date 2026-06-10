package com.bingmerfest.wordle.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BenchmarkRunRepository extends JpaRepository<BenchmarkRunEntity, Long> {

	List<BenchmarkRunEntity> findAllByOrderByCreatedAtDesc();
}

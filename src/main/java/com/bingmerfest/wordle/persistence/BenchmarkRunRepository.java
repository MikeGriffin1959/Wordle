package com.bingmerfest.wordle.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BenchmarkRunRepository extends JpaRepository<BenchmarkRunEntity, Long> {

	List<BenchmarkRunEntity> findAllByOrderByCreatedAtDesc();

	/** All runs with their config eagerly fetched (open-in-view is off). */
	@Query("select r from BenchmarkRunEntity r join fetch r.config order by r.createdAt desc")
	List<BenchmarkRunEntity> findAllWithConfig();
}

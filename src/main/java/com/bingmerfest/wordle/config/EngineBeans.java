package com.bingmerfest.wordle.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.bingmerfest.wordle.engine.EntropySolver;
import com.bingmerfest.wordle.engine.PatternMatrix;
import com.bingmerfest.wordle.engine.SolverConfig;
import com.bingmerfest.wordle.engine.WordLists;

/**
 * Wires the Spring-free engine into the application context. The word lists
 * and pattern matrix are built once at startup (~30 MB, a second or so); the
 * default solver is shared, and per-request solvers with other configs can be
 * built cheaply on top of the same matrix.
 */
@Configuration
public class EngineBeans {

	@Bean
	public WordLists wordLists() {
		return WordLists.load();
	}

	@Bean
	public PatternMatrix patternMatrix(WordLists wordLists) {
		return PatternMatrix.build(wordLists);
	}

	/** Default solver: SALET opener, easy mode, full guess pool. */
	@Bean
	public EntropySolver entropySolver(WordLists wordLists, PatternMatrix patternMatrix) {
		return new EntropySolver(wordLists, patternMatrix, SolverConfig.defaults());
	}
}

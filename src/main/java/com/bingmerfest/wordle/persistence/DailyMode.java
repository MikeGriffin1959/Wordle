package com.bingmerfest.wordle.persistence;

public enum DailyMode {
	/** Fetches the solution from the NYT endpoint and self-plays it. */
	AUTO,
	/** Honest advisor: engine proposes, user enters the observed pattern. */
	ASSIST
}

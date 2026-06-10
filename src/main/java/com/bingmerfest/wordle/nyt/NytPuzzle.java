package com.bingmerfest.wordle.nyt;

import java.time.LocalDate;

/**
 * One day's NYT Wordle puzzle as served by
 * {@code https://www.nytimes.com/svc/wordle/v2/{date}.json}.
 */
public record NytPuzzle(LocalDate printDate, int id, int daysSinceLaunch, String solution, String editor) {
}

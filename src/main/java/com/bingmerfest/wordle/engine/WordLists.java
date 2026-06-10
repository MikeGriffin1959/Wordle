package com.bingmerfest.wordle.engine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

/**
 * Loads and indexes the two bundled word lists:
 * <ul>
 * <li>{@code answers.txt} — the curated solution words (~2,309)</li>
 * <li>{@code allowed.txt} — accepted non-answer guesses (~10,638)</li>
 * </ul>
 * The guess list is the sorted union of both (~12,947 words). All words are
 * 5 lowercase letters; both lists are kept sorted so word indexes are stable
 * and lexicographic tiebreaks fall out of plain index order.
 *
 * <p>Immutable and thread-safe. No Spring.
 */
public final class WordLists {

	private static final String ANSWERS_RESOURCE = "/words/answers.txt";
	private static final String ALLOWED_RESOURCE = "/words/allowed.txt";

	private final List<String> answers;
	private final List<String> guesses;
	private final Map<String, Integer> answerIndex;
	private final Map<String, Integer> guessIndex;
	private final int[] answerToGuess; // answer index -> index of same word in the guess list

	/** Loads the bundled classpath word lists. */
	public static WordLists load() {
		return new WordLists(readResource(ANSWERS_RESOURCE), readResource(ALLOWED_RESOURCE));
	}

	/**
	 * Builds lists from arbitrary collections (mainly for tests). Words are
	 * lowercased, validated, deduplicated, and sorted.
	 */
	public WordLists(Collection<String> answerWords, Collection<String> allowedWords) {
		TreeSet<String> answerSet = new TreeSet<>();
		for (String w : answerWords) {
			answerSet.add(validate(w));
		}
		if (answerSet.isEmpty()) {
			throw new IllegalArgumentException("Answer list is empty");
		}
		TreeSet<String> guessSet = new TreeSet<>(answerSet);
		for (String w : allowedWords) {
			guessSet.add(validate(w));
		}

		this.answers = List.copyOf(answerSet);
		this.guesses = List.copyOf(guessSet);
		this.answerIndex = indexOf(answers);
		this.guessIndex = indexOf(guesses);
		this.answerToGuess = new int[answers.size()];
		for (int a = 0; a < answers.size(); a++) {
			answerToGuess[a] = guessIndex.get(answers.get(a));
		}
	}

	public int answerCount() {
		return answers.size();
	}

	public int guessCount() {
		return guesses.size();
	}

	/** Sorted, immutable list of possible solutions. */
	public List<String> answers() {
		return answers;
	}

	/** Sorted, immutable list of all valid guesses (answers ∪ allowed). */
	public List<String> guesses() {
		return guesses;
	}

	public String answerWord(int answerIdx) {
		return answers.get(answerIdx);
	}

	public String guessWord(int guessIdx) {
		return guesses.get(guessIdx);
	}

	/** Index in the guess list, or -1 if not a valid guess. */
	public int guessIndexOf(String word) {
		Integer i = guessIndex.get(word.toLowerCase(Locale.ROOT));
		return i == null ? -1 : i;
	}

	/** Index in the answer list, or -1 if not a possible answer. */
	public int answerIndexOf(String word) {
		Integer i = answerIndex.get(word.toLowerCase(Locale.ROOT));
		return i == null ? -1 : i;
	}

	/** Guess-list index of the answer at {@code answerIdx}. */
	public int guessIndexOfAnswer(int answerIdx) {
		return answerToGuess[answerIdx];
	}

	public boolean isValidGuess(String word) {
		return guessIndexOf(word) >= 0;
	}

	public boolean isAnswer(String word) {
		return answerIndexOf(word) >= 0;
	}

	private static String validate(String word) {
		String w = word.trim().toLowerCase(Locale.ROOT);
		if (w.length() != PatternService.WORD_LENGTH) {
			throw new IllegalArgumentException("Not a 5-letter word: '" + word + "'");
		}
		for (int i = 0; i < w.length(); i++) {
			char c = w.charAt(i);
			if (c < 'a' || c > 'z') {
				throw new IllegalArgumentException("Non a-z letter in word: '" + word + "'");
			}
		}
		return w;
	}

	private static Map<String, Integer> indexOf(List<String> words) {
		Map<String, Integer> map = HashMap.newHashMap(words.size());
		for (int i = 0; i < words.size(); i++) {
			map.put(words.get(i), i);
		}
		return Map.copyOf(map);
	}

	private static List<String> readResource(String path) {
		InputStream in = WordLists.class.getResourceAsStream(path);
		if (in == null) {
			throw new IllegalStateException("Missing classpath resource: " + path);
		}
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
			List<String> words = new ArrayList<>();
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (!line.isEmpty()) {
					words.add(line);
				}
			}
			return words;
		} catch (IOException e) {
			throw new UncheckedIOException("Failed reading " + path, e);
		}
	}
}

package com.bingmerfest.wordle.nyt;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Verifies the JSON shape documented in CLAUDE.md §6 maps onto NytResponse
 * (no network involved; the live endpoint shape was verified manually).
 */
class NytWordleClientTest {

	@Test
	void parsesNytResponseJson() throws Exception {
		String json = """
				{"id":1114,"solution":"crane","print_date":"2026-06-10",
				 "days_since_launch":1817,"editor":"Tracy Bennett","some_new_field":true}
				""";
		NytWordleClient.NytResponse r = new ObjectMapper().readValue(json, NytWordleClient.NytResponse.class);
		assertEquals(1114, r.id());
		assertEquals("crane", r.solution());
		assertEquals("2026-06-10", r.printDate());
		assertEquals(1817, r.daysSinceLaunch());
		assertEquals("Tracy Bennett", r.editor());
	}
}

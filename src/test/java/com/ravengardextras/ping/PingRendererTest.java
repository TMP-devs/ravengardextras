package com.ravengardextras.ping;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PingRendererTest {
	@Test
	void namesEndingInSGetBareApostrophe() {
		assertEquals("Scrolls'", PingRenderer.possessive("Scrolls"));
		assertEquals("CHAOS'", PingRenderer.possessive("CHAOS"));
	}

	@Test
	void otherNamesGetApostropheS() {
		assertEquals("Bob's", PingRenderer.possessive("Bob"));
	}
}

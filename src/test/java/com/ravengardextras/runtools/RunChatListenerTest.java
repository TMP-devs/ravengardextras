package com.ravengardextras.runtools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RunChatListenerTest {
	@Test
	void parsesCrownGlyphAmount() {
		assertEquals(136, RunChatListener.parseEscapedCrowns("ScrollsAFK2 has escaped with ♛136!"));
	}

	@Test
	void parsesPlainAmount() {
		assertEquals(136, RunChatListener.parseEscapedCrowns("ScrollsAFK2 has escaped with 136!"));
	}

	@Test
	void parsesCommaThousands() {
		assertEquals(12500, RunChatListener.parseEscapedCrowns("Name has escaped with ♛12,500!"));
	}

	@Test
	void unparseableReturnsMinusOne() {
		assertEquals(-1, RunChatListener.parseEscapedCrowns("Name has escaped with the loot!"));
		assertEquals(-1, RunChatListener.parseEscapedCrowns("An escape portal has spawned!"));
		assertEquals(-1, RunChatListener.parseEscapedCrowns(null));
	}
}

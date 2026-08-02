package com.ravengardextras.ping;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class PingMessageTest {
	@Test
	void alertFormatsAsPlainCoordinates() {
		assertEquals("RGE-ALERT @ 12, 64, -900", PingMessage.formatAlert(12, 64, -900));
	}

	@Test
	void alertRoundTripsThroughServerWrappedChat() {
		PingMessage.Parsed parsed =
				PingMessage.parse("Party > [MVP+] Scrolls: " + PingMessage.formatAlert(12, 64, -900));
		assertNotNull(parsed);
		assertEquals(PingMessage.Type.ALERT, parsed.type());
		assertEquals("Scrolls", parsed.sender());
		assertEquals(12, parsed.x());
		assertEquals(64, parsed.y());
		assertEquals(-900, parsed.z());
		assertNull(parsed.label());
	}

	@Test
	void alertWithoutRecognizableSenderIsIgnored() {
		assertNull(PingMessage.parse("RGE-ALERT @ 1, 2, 3"));
	}

	@Test
	void alertWithLabelSuffixDiscardsTheLabel() {
		// The COORDS regex accepts a trailing "(label)" on any type; only MARK should keep it.
		PingMessage.Parsed parsed = PingMessage.parse("Scrolls: RGE-ALERT @ 1, 2, 3 (SomeLabel)");
		assertNotNull(parsed);
		assertEquals(PingMessage.Type.ALERT, parsed.type());
		assertNull(parsed.label());
	}

	@Test
	void pingAndMarkStillParse() {
		PingMessage.Parsed ping = PingMessage.parse("Scrolls: RGE-PING @ 1, 2, 3");
		assertNotNull(ping);
		assertEquals(PingMessage.Type.PING, ping.type());

		PingMessage.Parsed mark = PingMessage.parse("Scrolls: RGE-MARK @ 1, 2, 3 (Bandage)");
		assertNotNull(mark);
		assertEquals(PingMessage.Type.MARK, mark.type());
		assertEquals("Bandage", mark.label());
	}
}

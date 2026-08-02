package com.ravengardextras.ping;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PingManagerTest {
	private static final long DURATION = 10_000;

	@BeforeEach
	void clear() {
		PingManager.clear();
	}

	@Test
	void alertReplacesThePingInTheTemporarySlot() {
		PingManager.addPing("Scrolls", new BlockPos(1, 2, 3));
		PingManager.addAlert("Scrolls", new BlockPos(4, 5, 6));

		List<PingManager.Ping> active = PingManager.active(0, DURATION);
		assertEquals(1, active.size());
		assertEquals(PingManager.Kind.ALERT, active.get(0).kind());
		assertEquals(new BlockPos(4, 5, 6), active.get(0).pos());
	}

	@Test
	void newPingReplacesAnAlertToo() {
		PingManager.addAlert("Scrolls", new BlockPos(4, 5, 6));
		PingManager.addPing("Scrolls", new BlockPos(1, 2, 3));

		List<PingManager.Ping> active = PingManager.active(0, DURATION);
		assertEquals(1, active.size());
		assertEquals(PingManager.Kind.PING, active.get(0).kind());
	}

	@Test
	void alertExpiresLikeATemporaryPing() {
		PingManager.addAlert("Scrolls", new BlockPos(1, 2, 3));
		long created = PingManager.pingOf("Scrolls").createdAtMillis();
		assertTrue(PingManager.active(created + DURATION + 1, DURATION).isEmpty());
	}

	@Test
	void marksAreNotAlerts() {
		PingManager.addMark("Scrolls", new BlockPos(1, 2, 3), "Bandage");
		List<PingManager.Ping> active = PingManager.active(0, DURATION);
		assertEquals(PingManager.Kind.MARK, active.get(0).kind());
		assertTrue(active.get(0).permanent());
	}

	@Test
	void pingOfReturnsTemporarySlotOrNull() {
		assertNull(PingManager.pingOf("Scrolls"));
		PingManager.addPing("Scrolls", new BlockPos(1, 2, 3));
		assertEquals(new BlockPos(1, 2, 3), PingManager.pingOf("Scrolls").pos());
	}
}

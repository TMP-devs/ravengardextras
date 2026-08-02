# Double-tap Alert Ping Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Double-tapping the ping key (Z) upgrades the just-placed temporary ping into a red flashing circle alert labeled `<Name>'s Alert`, replacing the ping locally and on teammates' clients.

**Architecture:** The alert is a third "kind" of the existing `PingManager.Ping` (PING / MARK / ALERT) that shares the one-per-player temporary ping slot and expiry timer. A new `RGE-ALERT @ x, y, z` chat message rides the existing party-chat wire format. `PingRenderer` draws a flashing red ring instead of the colored diamond for ALERT kind. Double-tap detection is a tiny pure class driven from `PingKeyHandler.onPingKey`.

**Tech Stack:** Java 25, Fabric/Loom Minecraft client mod, Gradle, JUnit 5 (added by Task 1 — no test infrastructure exists yet).

## Global Constraints

- Spec: `docs/superpowers/specs/2026-08-01-double-tap-alert-ping-design.md`
- Every Gradle command needs `JAVA_HOME=/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home` (machine default is Java 8; build requires 25).
- Commits: author is the repo-configured `ScrollsDev <scrollsmc@gmail.com>`. Do NOT add a `Co-Authored-By` line or any Claude attribution. Match existing style: plain imperative subject, no `feat:` prefix (e.g. "Add 2s ping cooldown so key spam can't flood party chat").
- Double-tap window: **400ms**. Flash cycle: **~500ms**. Alert color: red `0xFFFF4040`.
- No new keybind, no new config option, no sound.
- Wire format line: `RGE-ALERT @ x, y, z` (no label suffix).
- Possessive rule: names ending in `s`/`S` get `'` (`Scrolls' Alert`), otherwise `'s` (`Bob's Alert`).
- Cooldown refinement (agreed during planning): the upgrade tap is cooldown-exempt **only when the first tap actually placed a ping**; it refreshes the cooldown timestamp. A double-tap whose first tap was blocked (cooldown / no block) falls through to the normal ping path. This keeps "double-tap always completes" true in the normal case while still bounding chat traffic to ~1 broadcast per 2s.

---

### Task 1: `RGE-ALERT` wire format + JUnit test infrastructure

**Files:**
- Modify: `build.gradle` (add JUnit; wire `test` source set to the client source set)
- Modify: `src/client/java/com/ravengardextras/ping/PingMessage.java`
- Test: `src/test/java/com/ravengardextras/ping/PingMessageTest.java` (new; first test file in the repo)

**Interfaces:**
- Consumes: existing `PingMessage.parse(String)`, `Parsed(String sender, Type type, int x, int y, int z, String label)`.
- Produces: `PingMessage.Type.ALERT` enum constant; `public static String formatAlert(int x, int y, int z)` returning `"RGE-ALERT @ x, y, z"`; `parse` returns `Parsed` with `type() == Type.ALERT`, `label() == null` for alert lines. Task 3 (listener) and Task 4 (key handler) rely on these exact names.

- [ ] **Step 1: Add test infrastructure to `build.gradle`**

The project uses Loom's `splitEnvironmentSourceSets()`; the classes under test live in the `client` source set, so the default `test` source set must see the client output and its compile classpath. Append to `build.gradle` (after the existing `dependencies` block):

```gradle
sourceSets {
	test {
		compileClasspath += sourceSets.client.output + sourceSets.client.compileClasspath
		runtimeClasspath += sourceSets.client.output + sourceSets.client.runtimeClasspath
	}
}

dependencies {
	testImplementation platform("org.junit:junit-bom:5.11.3")
	testImplementation "org.junit.jupiter:junit-jupiter"
	testRuntimeOnly "org.junit.platform:junit-platform-launcher"
}

tasks.named("test") {
	useJUnitPlatform()
}
```

- [ ] **Step 2: Write the failing tests**

Create `src/test/java/com/ravengardextras/ping/PingMessageTest.java`:

```java
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
```

- [ ] **Step 3: Run tests to verify they fail**

Run: `JAVA_HOME=/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home ./gradlew test --tests "com.ravengardextras.ping.PingMessageTest"`
Expected: COMPILE FAILURE — `Type.ALERT` and `formatAlert` don't exist. (`pingAndMarkStillParse` would pass; the new symbols break compilation first, which is the failure signal here.)

- [ ] **Step 4: Implement in `PingMessage`**

Three edits:

1. `COORDS` pattern — add `ALERT` to the marker alternation:

```java
	private static final Pattern COORDS =
			Pattern.compile("RGE-(PING|MARK|ALERT) @ (-?\\d{1,8}), (-?\\d{1,8}), (-?\\d{1,8})( \\(([^()]{1,48})\\))?");
```

2. `Type` enum and formatter:

```java
	public enum Type {
		PING, MARK, ALERT, CLEAR_MARK, CLEAR_ALL_MARKS
	}
```

```java
	public static String formatAlert(int x, int y, int z) {
		return "RGE-ALERT @ " + x + ", " + y + ", " + z;
	}
```

3. In `parse`, replace the type/label lines inside the `coords.find()` branch:

```java
				Type type = switch (coords.group(1)) {
					case "MARK" -> Type.MARK;
					case "ALERT" -> Type.ALERT;
					default -> Type.PING;
				};
				String label = type == Type.MARK ? coords.group(6) : null;
```

Also update the class javadoc's wire-format list with a line: `{@code RGE-ALERT @ x, y, z} - alert ping (double-tap), red flashing circle`.

- [ ] **Step 5: Run tests to verify they pass**

Run: `JAVA_HOME=/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home ./gradlew test --tests "com.ravengardextras.ping.PingMessageTest"`
Expected: PASS (4 tests)

- [ ] **Step 6: Commit**

```bash
git add build.gradle src/client/java/com/ravengardextras/ping/PingMessage.java src/test/java/com/ravengardextras/ping/PingMessageTest.java
git commit -m "Add RGE-ALERT wire message and JUnit test setup"
```

---

### Task 2: `DoubleTap` detector

**Files:**
- Create: `src/client/java/com/ravengardextras/ping/DoubleTap.java`
- Test: `src/test/java/com/ravengardextras/ping/DoubleTapTest.java`

**Interfaces:**
- Consumes: nothing (pure, no Minecraft types).
- Produces: `public DoubleTap(long windowMillis)`; `public boolean tap(long nowMillis)` — records a tap, returns true iff it completed a double-tap. Task 4 constructs `new DoubleTap(400)` and calls `tap(System.currentTimeMillis())` once per ping-key press.

- [ ] **Step 1: Write the failing tests**

Create `src/test/java/com/ravengardextras/ping/DoubleTapTest.java`:

```java
package com.ravengardextras.ping;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DoubleTapTest {
	@Test
	void secondTapWithinWindowIsDoubleTap() {
		DoubleTap tap = new DoubleTap(400);
		assertFalse(tap.tap(1_000));
		assertTrue(tap.tap(1_300));
	}

	@Test
	void secondTapOutsideWindowIsNot() {
		DoubleTap tap = new DoubleTap(400);
		assertFalse(tap.tap(1_000));
		assertFalse(tap.tap(1_500));
	}

	@Test
	void tapExactlyAtWindowEdgeCounts() {
		DoubleTap tap = new DoubleTap(400);
		assertFalse(tap.tap(1_000));
		assertTrue(tap.tap(1_400));
	}

	@Test
	void completingADoubleTapResetsTheSequence() {
		DoubleTap tap = new DoubleTap(400);
		assertFalse(tap.tap(1_000));
		assertTrue(tap.tap(1_300));
		// A third quick tap starts a NEW sequence - it must not chain another double-tap.
		assertFalse(tap.tap(1_600));
		assertTrue(tap.tap(1_900));
	}
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `JAVA_HOME=/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home ./gradlew test --tests "com.ravengardextras.ping.DoubleTapTest"`
Expected: COMPILE FAILURE — `DoubleTap` does not exist.

- [ ] **Step 3: Implement `DoubleTap`**

Create `src/client/java/com/ravengardextras/ping/DoubleTap.java`:

```java
package com.ravengardextras.ping;

/**
 * Detects a double-tap: two taps at most a window apart. The tap that completes
 * a double-tap resets the detector, so a third quick tap starts a new sequence
 * instead of chaining.
 */
public final class DoubleTap {
	private final long windowMillis;
	private long lastTapMillis;

	public DoubleTap(long windowMillis) {
		this.windowMillis = windowMillis;
	}

	/** Records a tap at nowMillis; true iff it completed a double-tap. */
	public boolean tap(long nowMillis) {
		boolean second = lastTapMillis != 0 && nowMillis - lastTapMillis <= windowMillis;
		lastTapMillis = second ? 0 : nowMillis;
		return second;
	}
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `JAVA_HOME=/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home ./gradlew test --tests "com.ravengardextras.ping.DoubleTapTest"`
Expected: PASS (4 tests)

- [ ] **Step 5: Commit**

```bash
git add src/client/java/com/ravengardextras/ping/DoubleTap.java src/test/java/com/ravengardextras/ping/DoubleTapTest.java
git commit -m "Add pure double-tap detector for the ping key"
```

---

### Task 3: `PingManager` Kind + `addAlert` + chat listener

**Files:**
- Modify: `src/client/java/com/ravengardextras/ping/PingManager.java`
- Modify: `src/client/java/com/ravengardextras/ping/PingChatListener.java` (add `ALERT` switch case, ~line 57-68)
- Test: `src/test/java/com/ravengardextras/ping/PingManagerTest.java`

**Interfaces:**
- Consumes: `PingMessage.Type.ALERT` (Task 1).
- Produces: `PingManager.Kind` enum (`PING`, `MARK`, `ALERT`); `Ping` record becomes `Ping(String sender, BlockPos pos, long createdAtMillis, Kind kind, String label)` with a derived helper `public boolean permanent()` (== `kind == Kind.MARK`) so existing `.permanent()` callers keep compiling; `public static void addAlert(String sender, BlockPos pos)`; `public static Ping pingOf(String sender)` (the sender's current temporary-slot entry — ping or alert — or null). Task 4 uses `addAlert`/`pingOf`; Task 5 uses `kind()`.

- [ ] **Step 1: Write the failing tests**

Create `src/test/java/com/ravengardextras/ping/PingManagerTest.java` (BlockPos is fine here — the test classpath includes the client compile classpath):

```java
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
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `JAVA_HOME=/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home ./gradlew test --tests "com.ravengardextras.ping.PingManagerTest"`
Expected: COMPILE FAILURE — `Kind`, `addAlert`, `pingOf` don't exist.

- [ ] **Step 3: Implement in `PingManager`**

Replace the `Ping` record and add the enum (the record keeps a `permanent()` helper so `PingRenderer`/other callers compile unchanged):

```java
	public enum Kind {
		PING, MARK, ALERT
	}

	/** label is the item name shown instead of the sender, or null for a plain mark/ping. */
	public record Ping(String sender, BlockPos pos, long createdAtMillis, Kind kind, String label) {
		/** Marks persist until cleared; pings and alerts expire on the temp timer. */
		public boolean permanent() {
			return kind == Kind.MARK;
		}
	}
```

Update the two existing constructor calls and add the new methods:

```java
	public static void addPing(String sender, BlockPos pos) {
		PINGS.put(pingKey(sender), new Ping(sender, pos.immutable(), System.currentTimeMillis(), Kind.PING, null));
	}

	/** An alert shares the sender's single temporary-ping slot, replacing any ping there. */
	public static void addAlert(String sender, BlockPos pos) {
		PINGS.put(pingKey(sender), new Ping(sender, pos.immutable(), System.currentTimeMillis(), Kind.ALERT, null));
	}

	/** The sender's current temporary-slot entry (ping or alert), or null. */
	public static Ping pingOf(String sender) {
		return PINGS.get(pingKey(sender));
	}
```

In `addMark`, change `true` to `Kind.MARK` in the `new Ping(...)` call. `active()`'s `!ping.permanent()` check stays as-is (alerts are non-permanent, so they expire correctly).

- [ ] **Step 4: Handle incoming alerts in `PingChatListener`**

In the `switch (parsed.type())` block, add a case mirroring `PING` (echo reconciliation included, so nickname-server echoes of your own alert don't duplicate):

```java
				case ALERT -> {
					reconcileOwnEcho(sender, pos, false);
					PingManager.addAlert(sender, pos);
				}
```

- [ ] **Step 5: Run tests + full build to verify everything passes and compiles**

Run: `JAVA_HOME=/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home ./gradlew test build`
Expected: PingManagerTest PASS (5 tests), all prior tests PASS, `build` SUCCESS (proves `PingRenderer`/`PingKeyHandler` still compile against the changed record).

- [ ] **Step 6: Commit**

```bash
git add src/client/java/com/ravengardextras/ping/PingManager.java src/client/java/com/ravengardextras/ping/PingChatListener.java src/test/java/com/ravengardextras/ping/PingManagerTest.java
git commit -m "Model alerts as a third ping kind sharing the temporary slot"
```

---

### Task 4: Double-tap upgrade in `PingKeyHandler`

**Files:**
- Modify: `src/client/java/com/ravengardextras/ping/PingKeyHandler.java` (state fields ~line 39, `onPingKey` ~line 46-66)

**Interfaces:**
- Consumes: `new DoubleTap(400)` / `tap(long)` (Task 2); `PingManager.addAlert`, `PingManager.pingOf` (Task 3); `PingMessage.formatAlert` (Task 1); existing `broadcast`, `onCooldown`, `lastPingMillis`.
- Produces: no new public API — behavior only. This logic is exercised manually in-game (Task 5 Step 6); the timing decision itself is unit-tested via `DoubleTap`.

- [ ] **Step 1: Add state fields**

Below the existing `lastPingMillis` field:

```java
	/** Two ping-key taps at most this far apart upgrade the ping into an alert. */
	private static final long DOUBLE_TAP_WINDOW_MILLIS = 400;

	private static final DoubleTap PING_DOUBLE_TAP = new DoubleTap(DOUBLE_TAP_WINDOW_MILLIS);
	/** Whether the previous ping-key tap actually placed a ping (not cooldown/range blocked). */
	private static boolean lastTapPinged;
```

- [ ] **Step 2: Rewrite `onPingKey` and add `upgradeToAlert`**

Replace `onPingKey` with:

```java
	public static void onPingKey(Minecraft client) {
		PingConfig config = RavengardExtrasClient.PING_CONFIG;
		if (!config.enabled) {
			return;
		}
		LocalPlayer player = client.player;
		if (player == null || client.level == null) {
			return;
		}
		long now = System.currentTimeMillis();
		// Only a tap that actually placed a ping can be upgraded; a blocked first
		// tap falls through to the normal (cooldown-checked) path, so double-tap
		// spam can't flood party chat.
		if (PING_DOUBLE_TAP.tap(now) && lastTapPinged) {
			upgradeToAlert(client, config, player, now);
			return;
		}
		HitResult hit = player.pick(config.maxPingDistance, 1.0F, false);
		if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
			client.gui.hud.setOverlayMessage(Component.literal("No block in ping range"), false);
			lastTapPinged = false;
			return;
		}
		if (onCooldown(client)) {
			lastTapPinged = false;
			return;
		}
		lastTapPinged = true;
		BlockPos pos = blockHit.getBlockPos();
		PingManager.addPing(player.getName().getString(), pos);
		broadcast(client, config, PingMessage.formatPing(pos.getX(), pos.getY(), pos.getZ()));
	}

	/**
	 * Second tap of a double-tap: replaces the just-placed ping with an alert at
	 * the block now under the crosshair (or the ping's own spot if that pick
	 * fails). Exempt from the cooldown check - the first tap just passed it -
	 * but refreshes the timestamp so a double-tap still counts as one use.
	 */
	private static void upgradeToAlert(Minecraft client, PingConfig config, LocalPlayer player, long now) {
		String name = player.getName().getString();
		HitResult hit = player.pick(config.maxPingDistance, 1.0F, false);
		BlockPos pos;
		if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
			pos = blockHit.getBlockPos();
		} else {
			PingManager.Ping existing = PingManager.pingOf(name);
			if (existing == null) {
				client.gui.hud.setOverlayMessage(Component.literal("No block in ping range"), false);
				return;
			}
			pos = existing.pos();
		}
		lastPingMillis = now;
		PingManager.addAlert(name, pos);
		broadcast(client, config, PingMessage.formatAlert(pos.getX(), pos.getY(), pos.getZ()));
	}
```

Also update the class javadoc's first paragraph: `The ping key places a temporary ping; double-tapping it upgrades that ping into a red flashing alert.`

- [ ] **Step 3: Build to verify it compiles and nothing regressed**

Run: `JAVA_HOME=/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home ./gradlew test build`
Expected: all tests PASS, BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add src/client/java/com/ravengardextras/ping/PingKeyHandler.java
git commit -m "Upgrade a ping to an alert on ping-key double-tap"
```

---

### Task 5: Alert rendering — flashing red circle + possessive label

**Files:**
- Modify: `src/client/java/com/ravengardextras/ping/PingRenderer.java`
- Test: `src/test/java/com/ravengardextras/ping/PingRendererTest.java` (possessive helper only; geometry is verified in-game)

**Interfaces:**
- Consumes: `ping.kind()` / `PingManager.Kind.ALERT` (Task 3).
- Produces: `static String possessive(String name)` in `PingRenderer` (package-private, pure string). Rendering behavior: ALERT kind draws a flashing red ring + `<Name>'s Alert (Nm)` label instead of diamond + name.

- [ ] **Step 1: Write the failing possessive test**

Create `src/test/java/com/ravengardextras/ping/PingRendererTest.java`:

```java
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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `JAVA_HOME=/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home ./gradlew test --tests "com.ravengardextras.ping.PingRendererTest"`
Expected: COMPILE FAILURE — `possessive` does not exist.

- [ ] **Step 3: Implement alert rendering in `PingRenderer`**

Add constants next to the existing ones:

```java
	private static final int ALERT_COLOR = 0xFFFF4040;
	private static final long FLASH_CYCLE_MILLIS = 500;
	private static final int CIRCLE_SEGMENTS = 24;
	private static final float CIRCLE_RADIUS = 0.5F;
	private static final float CIRCLE_EDGE = 0.09F;
```

In `collectSubmits`, inside the per-ping loop, replace the color lines:

```java
			boolean alert = ping.kind() == PingManager.Kind.ALERT;
			int color = alert ? ALERT_COLOR : PingColors.colorFor(ping.sender());
			// Marks are a darker shade of the player's color so the two kinds read
			// apart at a glance; alerts flash between full and dim red.
			int shapeColor = ping.permanent() ? scaleBrightness(color, 0.55F) : color;
			if (alert) {
				float phase = (now % FLASH_CYCLE_MILLIS) / (float) FLASH_CYCLE_MILLIS;
				shapeColor = scaleBrightness(color, 0.4F + 0.6F * (0.5F + 0.5F * Mth.sin(phase * Mth.TWO_PI)));
			}
```

(`diamondColor` is renamed to `shapeColor`; the label keeps using `color`.) Replace the label-text line:

```java
				String labelText = alert
						? possessive(ping.sender()) + " Alert"
						: ping.label() != null ? ping.label() : ping.sender();
```

Replace the geometry submission so alerts draw the circle:

```java
				context.submitNodeCollector().submitCustomGeometry(
						poseStack, RenderTypes.textBackgroundSeeThrough(),
						alert
								? (pose, buffer) -> drawCircle(pose, buffer, finalShapeColor)
								: (pose, buffer) -> drawDiamond(pose, buffer, finalShapeColor));
```

(`shapeColor` must be effectively final for the lambda — assign it once into `final int finalShapeColor = shapeColor;` before the submit call.)

Generalize `darken` into `scaleBrightness` (replacing it — `darken(x)` becomes `scaleBrightness(x, 0.55F)` at its call site, already shown above):

```java
	/** Scales an ARGB color's RGB channels by factor (0..1), keeping alpha. */
	private static int scaleBrightness(int argb, float factor) {
		int r = (int) (((argb >> 16) & 0xFF) * factor);
		int g = (int) (((argb >> 8) & 0xFF) * factor);
		int b = (int) ((argb & 0xFF) * factor);
		return (argb & 0xFF000000) | (r << 16) | (g << 8) | b;
	}
```

Add the circle geometry and possessive helper:

```java
	/** A ring with a translucent fill, same visual language as the diamond. */
	private static void drawCircle(PoseStack.Pose pose, VertexConsumer buffer, int color) {
		int fill = (color & 0x00FFFFFF) | 0xB0000000;
		float outer = CIRCLE_RADIUS + CIRCLE_EDGE;
		for (int i = 0; i < CIRCLE_SEGMENTS; i++) {
			float a1 = (float) (i * Math.TAU / CIRCLE_SEGMENTS);
			float a2 = (float) ((i + 1) * Math.TAU / CIRCLE_SEGMENTS);
			float ix1 = Mth.cos(a1) * CIRCLE_RADIUS;
			float iy1 = Mth.sin(a1) * CIRCLE_RADIUS;
			float ix2 = Mth.cos(a2) * CIRCLE_RADIUS;
			float iy2 = Mth.sin(a2) * CIRCLE_RADIUS;
			// Fill wedge (quad degenerate at the center), both windings so it can't be culled.
			quad(pose, buffer, fill, 0.0F, 0.0F, ix1, iy1, ix2, iy2, 0.0F, 0.0F);
			quad(pose, buffer, fill, 0.0F, 0.0F, ix2, iy2, ix1, iy1, 0.0F, 0.0F);
			// Solid rim segment, double-sided via edge().
			edge(pose, buffer, color, ix1, iy1, ix2, iy2,
					Mth.cos(a1) * outer, Mth.sin(a1) * outer, Mth.cos(a2) * outer, Mth.sin(a2) * outer);
		}
	}

	/** "Scrolls" -> "Scrolls'", "Bob" -> "Bob's". */
	static String possessive(String name) {
		return name.endsWith("s") || name.endsWith("S") ? name + "'" : name + "'s";
	}
```

Update the class javadoc first sentence to mention alerts: diamonds for pings/marks, a flashing red circle for alerts.

- [ ] **Step 4: Run tests + build**

Run: `JAVA_HOME=/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home ./gradlew test build`
Expected: all tests PASS (PingRendererTest 2 tests), BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add src/client/java/com/ravengardextras/ping/PingRenderer.java src/test/java/com/ravengardextras/ping/PingRendererTest.java
git commit -m "Render alerts as a flashing red circle with a possessive label"
```

- [ ] **Step 6: Manual in-game verification (requires the user)**

Deploy the build to the test client:

```bash
cp build/libs/ravengardextras-1.0.0.jar "$HOME/Library/Application Support/ModrinthApp/profiles/Fabric 26.2/mods/"
```

Ask the user to restart the Modrinth "Fabric 26.2" profile and check:
1. Single Z → normal colored diamond ping (unchanged).
2. Quick Z-Z → the diamond is replaced by ONE red flashing circle labeled `<Name>'s Alert (Nm)`, visible through walls, expiring like a normal ping.
3. Z-Z aimed at the sky after the first ping → alert lands on the first ping's block.
4. In a party: teammates see the ping replaced by the alert; non-modded members see the `RGE-ALERT @ x, y, z` chat line.

---

### Task 6: Document the alert in README

**Files:**
- Modify: `README.md` (Party Ping section, ~lines 19-30)

**Interfaces:**
- Consumes: final behavior from Tasks 1-5.
- Produces: user-facing docs only.

- [ ] **Step 1: Add the alert bullet**

In the Party Ping bullet list, after the `**\`Z\` — temporary ping**` bullet, insert:

```markdown
- **`Z` `Z` — alert**: double-tap the ping key to turn your ping into a **red flashing circle** labeled with your name (e.g. "Scrolls' Alert") — for "danger here, look NOW" moments. It replaces your ping (only one thing shows), lasts as long as a normal ping, and broadcasts as `RGE-ALERT @ x, y, z`
```

Also extend the broadcast-format bullet on line 26 to mention the alert message: change `(\`RGE-PING @ x, y, z\` / \`RGE-MARK @ x, y, z\`)` to `(\`RGE-PING @ x, y, z\` / \`RGE-MARK @ x, y, z\` / \`RGE-ALERT @ x, y, z\`)`.

- [ ] **Step 2: Commit**

```bash
git add README.md
git commit -m "Document double-tap alert pings in README"
```

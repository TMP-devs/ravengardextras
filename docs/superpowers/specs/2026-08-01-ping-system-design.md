# Party Ping System — Design

Date: 2026-08-01
Status: approved for implementation (user authorized autonomous execution)

## Goal

A Valorant-style ping for parties on servers where only some players run the mod.
Press a key (default `Z`, rebindable in Controls) and a diamond-shaped waypoint
appears on the block you are looking at. Everyone in your party who runs the
mod sees the diamond **through walls**, colored per player, with a distance
label. A player has at most one active ping; pinging again moves it.

## Constraints

- The mod is client-side only (`"environment": "client"`); there is no server
  component and never will be. Party members can therefore only communicate
  through the server's chat.
- Minecraft 26.2 / Fabric API 0.155.2, Mojang mappings, Java 25.

## Approach (chosen)

**Chat-relay pings** — the approach Skyblock mods use, which the user
suggested. On keypress the client raycasts to the targeted block and sends a
human-readable message to party chat:

```
RGE-PING @ 123, 64, -456
```

The server relays it to the party like any chat line (e.g.
`Party > Scrolls: RGE-PING @ 123, 64, -456`). Every client with the mod parses
incoming chat for that marker, extracts the sender name and coordinates, and
renders the waypoint. Players without the mod still see usable coordinates in
chat — a feature, not a bug.

Alternatives considered and rejected:
- *Custom plugin channels / packets*: needs a server-side mod. Not possible.
- *Local-only pings*: doesn't fulfill the party use case; still available by
  clearing the party command (see Config).

## Components

### 1. `ping/PingConfig` (new file `ravengardextras-ping.json`)

Same Gson pattern as `GearHighlighterConfig`, separate file so the existing
config format is untouched. Fields:

- `enabled` (default `true`)
- `partyCommand` (default `"pc"`) — the chat command used to broadcast,
  without the slash; sent as `/pc RGE-PING @ x, y, z`. Blank = don't send to
  chat at all (ping is local-only).
- `pingDurationSeconds` (default `60`) — pings expire after this long.
- `maxPingDistance` (default `160`) — raycast reach in blocks.

### 2. `ping/PingMessage` — protocol encode/parse (pure logic)

- `format(x, y, z)` → `"RGE-PING @ x, y, z"`.
- `parse(String plainChatLine)` → `sender + coords` or `null`. Works on the
  *plain text* of any incoming chat line. Coordinate regex
  `RGE-PING @ (-?\d+), (-?\d+), (-?\d+)`; sender is the last
  `[A-Za-z0-9_]{1,16}` token followed by `:`, `>` or `»` before the marker,
  so it tolerates `Party > [MVP+] Scrolls: ...`, `Scrolls » ...`, etc.
  No sender found → line ignored.

### 3. `ping/PingManager` — client state

- `Map<String lowercased sender, Ping>` where `Ping = (sender, BlockPos,
  createdAtMillis)`. Adding a ping for an existing sender replaces the old one
  (the "re-ping moves it" rule falls out for free).
- Expiry: pings older than `pingDurationSeconds` are dropped (checked on tick).
- Cleared on disconnect/world change (`ClientPlayConnectionEvents.DISCONNECT`).

### 4. `ping/PingColors` — per-player color (pure logic)

Sticky and collision-free: the first time a name needs a color it starts at
`palette[abs(hash(lowercase name)) % 8]` (8 distinct ARGB colors: blue, yellow,
green, red, purple, orange, cyan, pink) and probes forward to the next color
not yet assigned this session. Assignments persist until disconnect, so a
player's color never changes mid-session (recomputing from the live ping set
caused visible color flicker when the party-chat echo arrived). No two players
on one screen share a color up to 8 players; a 9th falls back to its base hash
color. Party chat reaches every client in the same order, so clients observing
the same pings resolve clashes identically with zero coordination.

### 5. Keybind + broadcast (in `RavengardExtrasClient` + `ping/PingKeyHandler`)

- `KeyMappingHelper.registerKeyMapping(new KeyMapping("key.ravengardextras.ping",
  InputConstants.KEY_Z, KeyMapping.Category.MISC))` — appears in vanilla
  Controls screen, rebindable.
- On press (END_CLIENT_TICK, `consumeClick()`): `player.pick(maxPingDistance,
  1.0F, false)`. If a `BlockHitResult` → block hit:
  - register own ping locally right away (instant feedback, works even if
    the server eats the message),
  - if `partyCommand` non-blank →
    `connection.sendCommand(partyCommand + " " + PingMessage.format(...))`.
  - On a miss: HUD overlay message ("No block in range"), via
    `minecraft.gui.hud.setOverlayMessage(...)`.

### 6. Chat listener (`ping/PingChatListener`)

Registers `ClientReceiveMessageEvents.CHAT` **and** `.GAME` (server plugins
usually send party chat as system/game messages, not signed player chat).
`component.getString()` → `PingMessage.parse` → `PingManager.add`. Own name
dedupes naturally (same key). Ignores messages when the feature is disabled.

### 7. World rendering (`ping/PingRenderer`)

Hook: `LevelRenderEvents.COLLECT_SUBMITS` (fabric-rendering-v1). Verified
against 26.2: the context provides `poseStack()` (identity, camera-relative
space), `submitNodeCollector()`, and `levelState().cameraRenderState`
(`pos`, `orientation`).

Per ping:
- translate to `blockCenter - cameraPos` (diamond floats ~1.2 blocks above the
  block so it reads as a marker on that block),
- `mulPose(camera.orientation)` to billboard (always faces you),
- scale: `clamp(0.5 + distance * 0.05, 0.6, 6.0)` — roughly constant on
  screen, slightly *growing* with distance, small when close so it never
  blocks your view,
- `submitCustomGeometry(poseStack, RenderTypes.textBackgroundSeeThrough(),
  ...)` — POSITION_COLOR quads, translucent, **no depth test** → visible
  through walls. Geometry: a 4-sided diamond (rotated square): filled quad in
  the player's ARGB color + a brighter outline (thin quads along the edges).
- Label: `submitNameTag(...)` with `seeThrough=true`, text `"Name (37m)"`,
  distance recomputed from the *viewer's* camera each frame, full-bright
  light coords. Rendered exactly like a nametag, so it shows through walls.
- A ping in the fade-out final second shrinks to nothing (subtle exit).

### 8. Menu integration

- New `FeatureButton` row "Party Ping" in `RavengardExtrasMenuScreen`
  (icon: ender eye), status dot bound to `PingConfig.enabled`, opens:
- `ping/PingScreen` (mirrors `GearHighlighterScreen` style): on/off
  `CycleButton`, `EditBox` for party command (with explainer that blank =
  local only), `EditBox` for duration seconds, Save/Back. Also shows your
  currently assigned ping color as a swatch.
- Panel height of the main menu grows to fit the second row.

## Error handling

- Bad config JSON → defaults (existing pattern).
- Unparseable chat lines → ignored silently (they're just chat).
- Raycast miss → overlay message, no chat spam.
- Not connected / no player → keybind no-ops.

## Testing

Pure-logic classes (`PingMessage`, `PingColors`) have no Minecraft imports so
they can be unit-tested with plain JUnit if a test source set is wired up.
Given the repo has no test infrastructure and the risk concentrates in
rendering (untestable headlessly), verification is: `./gradlew build` +
in-game testing by the user. If test wiring proves cheap it will be added,
LO priority.

## Out of scope (YAGNI)

- Multiple simultaneous pings per player, ping types (danger/loot/etc.),
  sounds, chat-format auto-detection per server, coordinate obfuscation.

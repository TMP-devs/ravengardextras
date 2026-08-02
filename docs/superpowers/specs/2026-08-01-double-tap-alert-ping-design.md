# Double-tap Alert Ping — Design

Date: 2026-08-01
Branch: feature/ping-system

## Summary

Double-tapping the ping key (default Z) upgrades the just-placed temporary ping
into an **alert**: a red flashing circle rendered through walls with the label
`<Name>'s Alert (<distance>m)`. Only one visual is ever shown per player — the
alert replaces the ping, locally and on teammates' clients.

## Trigger behavior

- Each Z press behaves exactly as today: pick block under crosshair, place local
  temporary ping, broadcast `RGE-PING @ x, y, z` (subject to the 2s cooldown).
- If a second Z press arrives within **400ms** of the previous one, the ping is
  upgraded to an alert:
  - Position: block under crosshair at the second press; if that pick fails
    (no block in range), reuse the first ping's position.
  - The alert overwrites the player's one temporary-ping slot in
    `PingManager`, so only the alert is visible.
  - Broadcast `RGE-ALERT @ x, y, z`.
- Cooldown: the upgrade tap is **exempt** from the 2s cooldown check (so the
  double-tap always completes), but it refreshes the cooldown timestamp, so a
  double-tap counts as one use.
- A second press more than 400ms after the first is just a new normal ping
  (subject to cooldown as usual).

## Wire format

New message, same style as existing ones and parsed by `PingMessage`:

```
RGE-ALERT @ x, y, z
```

Human-readable for partymates without the mod. On a double-tap, party chat
carries two lines (`RGE-PING …` then `RGE-ALERT …`); modded clients still
render only the alert because it replaces the sender's ping slot.

`PingChatListener` handles the new `ALERT` parse type by storing an
alert-kind ping for the sender (same replace-slot semantics as PING).

## Data model

- `PingManager.Ping` gains `Kind kind` (`PING`, `MARK`, `ALERT`), replacing the
  `permanent` boolean. `permanent` semantics = `kind == MARK`.
- Alerts share the temporary slot (`sender#ping` key) and expire on the same
  `tempPingSeconds` timer as normal pings. No new config.

## Rendering

In `PingRenderer`, for `ALERT` kind:

- Draw a **red circle** (ring built from quads, e.g. 24 segments) instead of
  the diamond. Same see-through render type, same camera billboard, same
  distance-based scale and end-of-life fade as temporary pings.
- **Flashing**: pulse the ring's alpha/brightness on a ~500ms cycle
  (sine or on/off — implementer's choice, must read clearly as flashing).
- Label: `<Name>'s Alert (<distance>m)` in red (0xFF5555-style), replacing the
  usual sender-name label. Possessive: names ending in `s`/`S` get `'`
  (e.g. `Scrolls' Alert`), otherwise `'s`.

## Out of scope

- No sound cue.
- No new keybind, config option, or GUI change.
- Marks are unaffected.

## Testing

- `PingMessage` unit coverage: format/parse round-trip for `RGE-ALERT`,
  including sender extraction and server-wrapped lines.
- Double-tap timing logic (window accept/reject, cooldown exemption) covered
  at whatever seam the implementation exposes; manual in-game verification via
  the Modrinth "Fabric 26.2" profile for rendering and flash behavior.

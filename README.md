# Ravengard Extras

A lightweight QOL mod for Fabric 26.2.

Open the menu with `/ravengardextras` or `/rge`.

## Features

### Gear Highlighter

Outlines item slots in any inventory GUI based on their "Crowns" value.

- Three configurable tiers, each with its own threshold and color
- Thresholds are entered as a single "starts at" number per tier, with a live preview showing the exact range it covers (e.g. `8 to 11`, `12 to 17`, `18+`)
- Tiers automatically stay in order (no overlapping ranges to worry about)
- Ten preset colors plus an animated rainbow option
- Full in-game GUI, no config file editing required

### Party Ping

Valorant-style pings for your party (both keys rebindable in Controls):

- **`Z` — temporary ping**: for pointing out people or directions; disappears after 10 seconds (configurable)
- **`X` — permanent mark**: for marking loot like heals; stays until you move it, clear it (crouch+`X`, or `X` the same block again), or the world changes (e.g. dungeon ends). Marks have a white core so they read differently from pings
- A diamond appears on the block, visible **through walls**, with the pinger's name and live distance
- Broadcast through party chat as plain coordinates (`RGE-PING @ x, y, z` / `RGE-MARK @ x, y, z`), so it works on any server — party members with the mod see the diamond, everyone else still gets the coords
- Each player gets a color that's unique within the party and stays fixed for the whole session
- One ping and one mark per player; pinging/marking again moves it
- The party chat command is configurable (default `/pc`); leave it blank to keep pings to yourself
- 2-second cooldown between broadcasts so you can't flood party chat

## Requirements

- Minecraft 26.2
- Fabric Loader 0.19.3 or newer
- Fabric API (26.2 build)
- Java 25 or newer

## Credits

Made by chrrisk and scrolls.

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

Valorant-style pings for your party. Press `Z` (rebindable in Controls) to ping the block you're looking at.

- A diamond marker appears on the block, visible **through walls**, with the pinger's name and live distance
- Broadcast through party chat as plain coordinates (`RGE-PING @ x, y, z`), so it works on any server — party members with the mod see the diamond, everyone else still gets the coords
- Each player gets a stable color derived from their name, the same on every teammate's screen
- One ping per player: ping again to move it; pings expire after a configurable duration (default 60s)
- The party chat command is configurable (default `/pc`); leave it blank to keep pings to yourself

## Requirements

- Minecraft 26.2
- Fabric Loader 0.19.3 or newer
- Fabric API (26.2 build)
- Java 25 or newer

## Credits

Made by chrrisk and scrolls.

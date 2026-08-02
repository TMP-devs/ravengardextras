# Ravengard Extras

A lightweight QOL mod for Fabric 26.2.

Open the menu with `/ravengardextras` or `/rge` - a tabbed dashboard covering Dungeon Utils, Party, and Inventory.

## Features

### 🛡️ Gear Highlighter

Outlines item slots based on their "Crowns" value.

- 3 configurable tiers, each with its own threshold and color
- Live range preview, tiers auto-sort so they never overlap
- 10 preset colors plus animated rainbow

### 💚 Heal Highlighter

Tints healing items with a colored wash.

- Checklist to pick which items count
- Custom tint color
- Overrides Gear Highlighter's outline on the same slot

### 📍 Party Ping

Valorant-style pings for your party.

- `Z` - temporary ping, fades after 10s (configurable)
- Double-tap `Z` - turns it into a flashing red alert
- `X` - permanent mark, up to 5 at a time; tap to clear one, hold to clear all
- Visible through walls, shown with name and distance
- Broadcasts over party chat so it works on any server
- Each player gets a bright, fixed color for the session

### 🔒 Slot Locking

Lock slots so you can't accidentally move or drop what's in them.

- `L` on a hovered slot to lock/unlock it
- Locked slots block clicks, drags, throws, and hotbar swaps
- Small lock icon + chime, persists across sessions

### 👑 Run Tools

Per-run Crown and XP tallies on the HUD, summarised in chat when you escape.

- **Crown Calculator** - net Crowns gained this run. A snapshot of your inventory is taken
  as you enter, so the gear you brought never counts and swapping a 5-Crown chestplate for
  a 10-Crown one reads `+5`, not `+10`. Worn armor and offhand are included
- **XP Calculator** - totals XP gains from chat and the action bar
- Consumables you brought in count against the total when used, since the figure is net
  inventory value rather than loot collected

## Requirements

- Minecraft 26.2
- Fabric Loader 0.19.3 or newer
- Fabric API (26.2 build)
- Java 25 or newer

## Credits

Made by chrrisk and Scrolls.

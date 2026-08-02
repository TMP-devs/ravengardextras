# Changelog

## 0.3

### 👑 Run Tools
A new **Run** tab with per-run tallies, shown on the HUD and summarised in chat when you escape.

- **Crown Calculator** - net Crowns gained, measured against a snapshot taken as you enter, so the gear you walked in with never counts and swapping an item up is worth the difference
- **XP Calculator** - totals XP gains from chat and the action bar, reset each run

### 🐛 Fixes
- Items worth exactly one Crown were valued at zero, because their lore reads "1 Crown" and the parser required the plural. They are now counted and highlighted correctly. The same fix makes Crown matching case-insensitive and stops lore like "2 Crownsmiths" being read as a value

## 0.2

### 🖥️ New Dashboard
Redesigned the menu into a tabbed control panel with expandable feature cards, styled to match the Ravengard aesthetic.

### 💚 Heal Highlighter
Highlights healing items in your inventory with a colored tint. Choose the color and pick exactly which items count.

### 📍 Party Ping
Ping locations and mark loot for your party to see, with configurable pings and clear visuals for pings vs. marks.

### 🔒 Slot Locking
Lock inventory slots to stop yourself from accidentally moving or dropping important items.

### ✨ Polish
Assorted visual tweaks and bug fixes across the board.

## 1.0.0

- Initial release: Gear Highlighter (outline gear by Crowns value, 3 tiers, 10 preset colors + rainbow)
- `/ravengardextras` and `/rge` open a menu GUI
- Configurable thresholds with live range preview, auto-sorted so tiers never overlap
- Settings persist to `config/ravengardextras.json`

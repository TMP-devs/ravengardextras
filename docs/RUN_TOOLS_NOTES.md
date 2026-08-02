# Run Tools — overnight build notes (validate in the morning)

Branch: `feature/run-tools`. Built + unit-tested + jar deployed to the Modrinth
"Fabric 26.2" test profile. Two features, all client-side only.

(A stronghold sound alert was also built but removed — that already exists.)

Open the mod menu (`/rge` or `/ravengardextras`) — two new rows sit under Gear
Highlighter: **Crown Calculator** and **XP Calculator**.

## 1. Crown Calculator  (net Crowns per run)
- On entering a run it snapshots the total Crown value of your **whole inventory**
  (the baseline). The HUD then shows `current − baseline`, so gear you brought in
  never counts. Enter with 300 Crowns of gear, loot a 75-Crown sword → **+75**.
  Drop it / stash it in a chest → back toward **0**. Swap for something worse → goes
  **negative**. Exactly the behaviour you described.
- A Crown lore value is multiplied by stack size, so five 10-Crown potions = 50.
- Shows top-left while in a run; when you leave, the final tally is printed once in
  the action bar ("Run complete: +N Crowns  +M XP") — that's your exfil number.

**Assumption to check:** "in a run" = the sidebar scoreboard objective name starts
with `dungeon` (hub is `hub_sb`). This is the same signal the Room Navigator uses,
so it should be right. If the calculator never appears in a dungeon, the server
renamed that objective — tell me the name and I'll adjust
`RavengardRunDetector.isDungeonObjective`.

## 2. XP Calculator  (total XP per run)
- Reads chat **and** the action bar, sums every XP gain during the run, resets each
  run. Displayed under the Crowns line.
- Parser (`XpParser`) matches number-before-keyword gains: `+15 XP`, `You gained
  1,240 EXP`, `+2.5k Experience`. It ignores progress-bar totals (anything with a
  `1240 / 5000` ratio) so a persistent XP bar can't inflate the count.

**Assumption to check:** the real format of Ravengard's XP-gain line. If XP isn't
counting, paste me one exact gain line (e.g. how a mob kill reads) and I'll tune the
regex. If it counts *too much*, the server is probably showing a running total I
didn't recognise as a bar — again, send a sample.

## What I could and couldn't verify
- ✅ Compiles against the real 26.2 mappings; `./gradlew build test` green.
- ✅ Pure parser logic (XP parsing) is unit-tested.
- ❌ I could not launch the game and play a run, so the "Assumption to check"
  items above — chat/action-bar wording and the dungeon scoreboard name — are my best
  guesses from the existing code. Everything is written to be easy to retune once you
  confirm the real strings.

## Files
- `runtools/RunToolsConfig.java` — settings (`config/ravengardextras-runtools.json`)
- `runtools/RavengardRunDetector.java` — dungeon detection (scoreboard)
- `runtools/RunTracker.java` — baseline snapshot + net Crowns + XP tally + exit summary
- `runtools/XpParser.java` — pure, unit-tested
- `runtools/RunChatListener.java` — reads chat/action bar
- `runtools/RunHudRenderer.java` — the top-left HUD
- menu rows + client wiring in `RavengardExtrasMenuScreen` / `RavengardExtrasClient`

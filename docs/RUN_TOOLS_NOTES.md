# Run Tools — overnight build notes (validate in the morning)

Branch: `feature/run-tools`. Built + unit-tested + jar deployed to the Modrinth
"Fabric 26.2" test profile. Three features, all client-side only.

Open the mod menu (`/rge` or `/ravengardextras`) — three new rows sit under Gear
Highlighter: **Crown Calculator**, **XP Calculator**, **Stronghold Alert**.

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

## 3. Stronghold Alert  (dragon growl)
- Plays `ENTITY_ENDER_DRAGON_GROWL` at medium volume when chat announces the
  stronghold opened. 5-second cooldown so repeated server lines don't stack it.
- Settings row → on/off, a **Volume** slider (default 60%), and a **Test growl**
  button so you can hear it without waiting for a stronghold.

**Assumption to check:** the trigger is any line containing "stronghold" + an
open/unlock/unseal/breach word (case-insensitive). Sealed/closed/"entering" lines are
ignored. If the real announcement uses different wording and the growl doesn't fire,
send me the exact line and I'll widen `StrongholdAlertMatcher`.

## What I could and couldn't verify
- ✅ Compiles against the real 26.2 mappings; `./gradlew build test` green.
- ✅ Pure parser logic (XP + stronghold matching) is unit-tested.
- ❌ I could not launch the game and play a run, so the three "Assumption to check"
  items above — chat/action-bar wording and the dungeon scoreboard name — are my best
  guesses from the existing code. Everything is written to be easy to retune once you
  confirm the real strings.

## Files
- `runtools/RunToolsConfig.java` — settings (`config/ravengardextras-runtools.json`)
- `runtools/RavengardRunDetector.java` — dungeon detection (scoreboard)
- `runtools/RunTracker.java` — baseline snapshot + net Crowns + XP tally + exit summary
- `runtools/XpParser.java`, `StrongholdAlertMatcher.java` — pure, unit-tested
- `runtools/StrongholdAlert.java` — sound + cooldown
- `runtools/RunChatListener.java` — reads chat/action bar
- `runtools/RunHudRenderer.java` — the top-left HUD
- `runtools/StrongholdAlertScreen.java` — the alert settings sub-screen
- menu row + client wiring in `RavengardExtrasMenuScreen` / `RavengardExtrasClient`

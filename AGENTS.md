# Pillars of Fortune — Grok Build spec

Rewrite `pof_fixed.sk` (Skript 2.2dev36, Spigot/Paper 1.8.8) into a Java plugin.
This is a replacement, not a Skript runner. Improve storage, leaderboards, worlds, and arena reset.
Do not invent extra minigames.

## Identity

- Plugin name: `PillarsOfFortune`
- Package: `com.slop.pof`
- Main class: `com.slop.pof.PoFPlugin`
- Commands: `/pof`, aliases `/pillars`, `/fortune`
- Author/site string default: `example.com` (from config)

## Platform

- API: Spigot / Bukkit **1.8.8 only**
- JVM on the server: **Java 17**
- Compile target: **Java 17** (`maven.compiler.source/target` or `release` = 17)
- Build: **Maven**. Produce a shaded jar in `target/`
- Command to verify: `mvn -q package`
- Color: legacy `&` codes / `ChatColor` (1.8). Do not require Adventure.
- Do **not** use Paper 1.21 APIs, Folia, `paper-plugin.yml`, or Brigadier.
- Do **not** depend on Skript, skRayFall, or skQuery at runtime.
- ProtocolLib is **optional**. If present, use it for tab header/footer on 1.8.8. If absent, skip header/footer and still set `player.setPlayerListName`.
- Scoreboards: Bukkit `Scoreboard` API per player (or per mode). Do not emulate skRayFall “id based score” APIs.

## Permissions

Prefix: `slop.pof.`

| Node | Default | Use |
|---|---|---|
| `slop.pof.play` | true | `/pof join`, `leave`, `stats` (self), `top`, lobby queue items |
| `slop.pof.vip` | op | `/pof start`, “Start Match” item (skip queue timer to 5s) |
| `slop.pof.stats.others` | true | `/pof stats <player>` |
| `slop.pof.admin` | op | `stop`, `setlobby`, `regen`, `arenas`, `debug`, `reload`, lobby build, in-game command bypass |

`slop.pof.admin` children: `play`, `vip`, `stats.others`.

Do **not** register `pof.vip` or `pof.admin`.

## Commands

`/pof [join|leave|stats|top|start|stop|setlobby|regen|arenas|debug|reload]`

Behavior matches `pof_fixed.sk` except where this file says otherwise.

- `join` / `leave` — queue or forfeit
- `stats [player]`
- `top` — top wins from **SQL**, not online-player scans
- `start` — VIP/admin: if queue >= minPlayers, set queue timer to 5 if it was higher
- `stop [id]` — stop one arena or all + clear queue
- `setlobby` — save executor location to disk (survives restart)
- `regen` — rebuild WAITING arenas only
- `arenas` — list id, state, alive count
- `debug` — toggle verbose admin messages
- `reload` — see below
- no args — help; extra admin lines only if `slop.pof.admin`

Console may run `stop`, `regen`, `arenas`, `reload`.

### `/pof reload` (`slop.pof.admin`)

Reload `config.yml`, messages, item pools, sounds, timings.

- Do **not** interrupt `STARTING` / `INGAME` / `ENDING` arenas.
- Do **not** delete worlds or wipe the database.
- If `storage.*` changed, reconnect HikariCP; do not drop tables.
- If `arenas` / `pillars` counts changed, apply only to `WAITING` arenas; tell the admin if a full restart is required.
- Success message from config (default `&aConfig reloaded.`).

## Worlds

Two different worlds.

1. **Lobby world** — existing main/lobby world. Name from config `worlds.lobby-name` (default `world`). Queue, lobby items, `/pof setlobby` live here. Do **not** generate pillars here.
2. **Game world** — separate void world. Name from config `worlds.game-name` (default `pof_void`).

On enable:

- Load/create `worlds.game-name` with a custom `ChunkGenerator` that returns empty chunks (true void). No vanilla terrain, villages, or default spawn island.
- Never call `WorldCreator` on the lobby world name.
- Keep the **game** world weather clear. Do not spam `weather clear` on the lobby every 10 seconds unless `worlds.clear-lobby-weather` is true (default false).

Arenas are laid out **only** in the void world, same math as the Skript:

- arena `id` center X = `id * 2000`, Z = 0, Y = `pillars.y` (`pillarY`)
- 12 pillar offsets from the Skript (`pof.tmp.ox/oz`)

## Arena reset (required)

After **each** match, that arena must be empty and pillars rebuilt before reuse.

**Do not** delete or regenerate the shared void world after a match while other arenas may still be running.

Config:

```yaml
arenas:
  reset-mode: region          # region | world-per-arena
  reset-blocks-per-tick: 8000
```

Default `region`:

When `ENDING` timer hits 0:

1. Send that arena’s players to lobby (`pofToLobby` behavior).
2. Hard-reset **only that arena’s AABB** in the void world:
   - bounds from `clear-radius` / `clear-height` around that arena center
   - set blocks to air with the Bukkit API — **no** `/minecraft:fill` and no console commands
   - slice work across ticks using `reset-blocks-per-tick` so the server does not freeze
   - remove non-player entities in range
   - rebuild 12 bedrock pillars and refresh stored pillar locations
3. Reset that arena’s state to `WAITING` only.

If `reset-mode` is `world-per-arena`: one void world per arena id; on reset, unload, delete that world folder, create a new void world. Never delete a world that still has players.

`/pof regen` uses the same reset+rebuild for idle (`WAITING`) arenas.

## Storage

**Forbidden:** `variables.csv`, Skript variables, YAML/CSV for wins/kills/leaderboards/playtime.

Use JDBC + HikariCP.

Engines:

- **H2** embedded file in the plugin data folder (default; zero setup)
- **MySQL / MariaDB**

Shade into the jar: HikariCP, H2 driver, MySQL connector.

```yaml
storage:
  type: h2                    # h2 | mysql
  h2:
    file: data
  mysql:
    host: 127.0.0.1
    port: 3306
    database: minecraft
    user: root
    password: ""
```

Schema (same for both engines):

```text
players (
  uuid         CHAR(36) PRIMARY KEY,
  name         VARCHAR(16) NOT NULL,
  wins         INT NOT NULL DEFAULT 0,
  kills        INT NOT NULL DEFAULT 0,
  deaths       INT NOT NULL DEFAULT 0,
  games        INT NOT NULL DEFAULT 0,
  streak       INT NOT NULL DEFAULT 0,
  best_streak  INT NOT NULL DEFAULT 0,
  items        INT NOT NULL DEFAULT 0,
  playtime_min INT NOT NULL DEFAULT 0,
  updated_at   TIMESTAMP
)
```

- Upsert on join (`pofEnsureStats`).
- Increment on the same events as the Skript (win, kill, death, games started, items rolled, playtime).
- Streak resets to 0 on death/eliminate; winner streak +1; update `best_streak` if higher.
- `/pof top`: `SELECT name, wins FROM players WHERE wins > 0 ORDER BY wins DESC, name ASC LIMIT :size`
- Offline players **must** appear. Do not port the Skript’s nested `{pof.players::*}`, `{pof.index::*}`, `{pof.wins::*}` merge. That logic is the bug.
- Create tables on enable. Never drop tables on reload.
- Growing data does not go in YAML. Lobby location may live in `data.yml` or a small `meta` table.

## Leaderboards / sidebar / tab

- `/pof top` = SQL only (config `leaderboards.size`, default 10).
- Optional in-memory cache refreshed every `leaderboards.refresh-ticks` (default 200 = 10s). Invalidate on win.
- Sidebar modes from the Skript: `lobby`, `queued`, `starting`, `grace`, `ingame`, `ending`. Titles match (`PILLARS`, `STARTING`, `FINISHED`, `FORTUNE`).
- Tab list name colors: queued gold, lobby gray, alive green, dead dark gray, winner gold — as in the Skript.

## Game rules (keep Skript behavior)

Port flow from `pof_fixed.sk`:

- Queue, `min-players` / `max-players`, queue timer, VIP skip to 5s
- Assign free `WAITING` arena, cages during `STARTING`, open cages → `INGAME`
- Grace period: no PvP
- Item drip every `item-seconds`; after `max-game-seconds`, interval 2s + occasional lightning on pillars (sudden death)
- Eliminate on Y < `void-y` or distance from arena center > `border-kill`
- Fire charge → fireball (`fireball-*` config), cooldown, knockback, self/enemy damage; cancel vanilla explosion damage when using this system
- Fishing rod consumed on hit as in the Skript
- Illegal items (bedrock, barrier, command block, spawner, portal frames, etc.) stripped on a timer; cancel pickup/drop/craft of illegal items
- Lobby items: slot 0 join, slot 8 leave, slot 4 VIP start if `slop.pof.vip`
- Cancel place/break outside a live `INGAME` unless `slop.pof.admin`; cancel bedrock break; `max-build-y` and `build-radius`
- Hunger cancelled; portals cancelled while in an arena
- Per-arena chat; spectators prefixed
- Block `/give`, `/gamemode`, `/tp`, `/teleport`, `/enchant`, `/effect` while in an arena unless `slop.pof.admin`
- Join → lobby + stats upsert + queue item; quit while alive → eliminate forfeit
- Playtime +1 minute per 60s while online (Skript default). Config `stats.playtime-online-only` default true. If false, only count time in a match.

Item pools and weighted roll (`rate-weapons` + `rate-armor` + `rate-blocks` + `rate-chaos` = 100), block amounts, pearl chance, knockback-stick rarity — same as `pofGiveRandom` / `pofInitPools`, but **lists live in config.yml** so they can be edited.

Do not use console `effect` / `fill` / `weather` if the Bukkit API can do it.

## config.yml

Every `options:` key from `pof_fixed.sk` becomes a config key with the same meaning (prefix, server IP, arena counts, radii, timings, fireball, item rates, lobby item materials/names, gamemode, etc.).

Also required:

- `storage.*` as above
- `worlds.lobby-name`, `worlds.game-name`, `worlds.clear-lobby-weather`
- `arenas.count`, `arenas.reset-mode`, `arenas.reset-blocks-per-tick`
- `leaderboards.size`, `leaderboards.refresh-ticks`
- `messages.*` for every player-facing string (prefix applied)
- `permissions` documented in plugin.yml only; don’t hardcode node strings in scattered literals if a constants class is cleaner
- `items.weapons`, `items.armor`, `items.blocks`, `items.chaos` lists
- `items.illegal` list
- `sounds.*` as 1.8 sound names

No magic numbers left in code that the Skript already exposed as `options:`.

## What not to do

- Do not port `variables.csv` or Skript variable names into production logic
- Do not port the broken `/pof top` loops
- Do not put arenas in the lobby world
- Do not `/minecraft:fill`
- Do not delete the shared void world after every win
- Do not bump the Minecraft version
- Do not add features not in the Skript unless this file asked for them (H2/MySQL, void world, SQL top, region reset, `/pof reload`, `slop.pof.*` permissions, full config)

## Done when

`mvn -q package` succeeds and `target/PillarsOfFortune-*.jar` exists.
`plugin.yml` has the command, aliases, and permissions above.
Default config is written on first enable.
H2 works with no extra setup.
`/pof top` can show a player who is offline.
After a match, that arena’s placed blocks are gone and pillars exist again in `pof_void`, not in the lobby world.

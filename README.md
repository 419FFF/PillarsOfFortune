# Pillars of Fortune

A Pillars of Fortune minigame plugin for **Spigot / Paper 1.8.8**, built for servers running **Java 17**.

Used Grok Build 4.7 (high), Deepseek 4.1 Flash (medium), along with some prompt assistance from Grok 4.6 (Fast).

Players are placed onto bedrock pillars in a void world, gear up over the course of a round, and fight
until one winner remains. Almost everything — arena layout, item pools, timings, leveling, messages and
leaderboards — is configurable in `config.yml`.

## Requirements

- Spigot or Paper **1.8.8**
- **Java 17** on the server (the plugin is compiled for Java 17)
- Optional integrations:
  - **PlaceholderAPI** — extra placeholders such as `%pof_wins%`
  - **LuckPerms** — prefix/suffix tokens inside messages
  - **ProtocolLib** — not required; the plugin runs without it

## Installation

1. Drop `PillarsOfFortune-<version>.jar` into your `plugins/` folder and start the server once.
2. A fully commented `config.yml` is created in `plugins/PillarsOfFortune/`.
3. Set `worlds.lobby-name` to your lobby world, stand there, and run `/pof setlobby`.
4. Change whatever you like, then run `/pof reload`.

A separate void world (default `pof_void`) is created automatically for matches. Arenas are only ever
built there, never in the lobby world.

## Gameplay

### Modes
Modes live under `gamemodes:` in `config.yml`. One enabled mode is used directly by `/pof join`; two or
more open a "Choose a mode" menu where each mode is an icon you can click.

Each mode can set its own name, GUI icon, description, player limits, cage countdown, queue time, item
delay, whether random item drops happen, and a fixed starting kit.

Two modes ship by default:

| Mode | Behaviour |
|---|---|
| **Classic** | Random items are given for the whole round. |
| **Rush** | Everyone starts fully geared with a fixed kit and no random drops. |

### Round flow
1. Players queue for a mode. Once the queue reaches `min-players`, a countdown starts.
2. A free arena is assigned and players are teleported into glass cages on their pillars.
3. When the cages open the round begins; a mode's starting kit is given at this point.
4. Items are dropped on a timer (`item-seconds`, with `item-delay-seconds` before the first one).
5. After `grace-seconds` PvP turns on. After `max-game-seconds` the round enters sudden death —
   faster items and lightning on random pillars.
6. The last player standing wins. The winner is announced, players return to the lobby, and the arena
   is cleared and rebuilt for the next match.

A player is eliminated by falling below `void-y`, by leaving the arena's `border-kill` radius, or by
dying in combat.

### Combat details
- **Fire charges** launch a fireball along the crosshair. The blast pushes players away from it and
  applies the configured damage once; vanilla explosion damage is cancelled. Speed, knockback, damage,
  cooldown, radius, block damage, and whether the blast sets fire are all configurable.
- **Fishing rods** are handed out with a single use and break as soon as they are used.
- **Illegal items** (bedrock, barrier, command blocks, spawners, portal frames, and so on) are stripped
  from inventories on a timer and cannot be picked up, dropped, crafted, or placed.

## Lobby

While in the lobby, players get a small hotbar of items:

| Slot | Item | Action |
|---|---|---|
| 1 | Join item | Join the queue, or open the mode menu |
| 5 | Start item (VIP only) | Skip the queue countdown to 5 seconds |
| 8 | Visibility dye | Cycle who you can see: everyone, only your queue, or nobody |
| 9 | Leave item (while queued) | Leave the queue |
| 9 | Hub bed (optional, off by default) | Runs a configurable command, such as `/hub` |

The visibility toggle is a dye whose colour shows the current state; its slot, material, colours and
names are all configurable. The hub bed is disabled by default and its command is fully configurable.

## Leveling

Players earn XP and level up. Every level costs more than the one before, up to a configurable cap, so
progress keeps getting harder without ever becoming impossible (a fixed cost table can also be used as
an override).

XP comes from winning, kills, playing a match, and time spent in a match — all configurable. Winning
while on a win streak adds a bonus that grows with the streak, up to a cap.

Your level is shown on the sidebar next to a progress bar, on the vanilla XP bar, in chat when you level
up, and with `/pof level`.

## Leaderboards and stats

`/pof top [wins|kills|streak|level]` reads straight from the database, so players who are offline are
included. Board size is controlled by `leaderboards.size`.

Stats are stored in a database:

- **H2** (default) — a local file in the plugin folder, no setup required.
- **MySQL / MariaDB** — set `storage.type: mysql` and fill in the connection details.

## Scoreboard and tab

Each player gets their own sidebar with a bold title (the mode's name during a match), the current
date, a level line with a progress bar above the server IP, and per-phase lines. The layouts live in
`messages.board-*` and accept `\n` for line breaks.

The tab list is handled automatically: players only see the people they should — the others in their
match while playing, or the lobby while they are out of a match.

## Commands

`/pof` (aliases: `/pillars`, `/fortune`)

| Command | Permission | Description |
|---|---|---|
| `/pof` | `slop.pof.play` | Show help |
| `/pof join` | `slop.pof.play` | Join the queue, or open the mode menu |
| `/pof leave` | `slop.pof.play` | Leave the queue, or forfeit a match |
| `/pof stats [player]` | `slop.pof.play` / `slop.pof.stats.others` | Show stats |
| `/pof level [player]` | `slop.pof.play` / `slop.pof.stats.others` | Show level and XP progress |
| `/pof top [wins\|kills\|streak\|level]` | `slop.pof.play` | Show a leaderboard |
| `/pof start` | `slop.pof.vip` | Start the queue sooner |
| `/pof stop [id]` | `slop.pof.admin` | Stop one arena, or all of them and clear the queue |
| `/pof setlobby` | `slop.pof.admin` | Save the lobby spawn here |
| `/pof regen` | `slop.pof.admin` | Rebuild idle arenas |
| `/pof arenas` | `slop.pof.admin` | List arenas and their states |
| `/pof debug [on\|off\|arenas\|queue\|world\|storage\|config]` | `slop.pof.admin` | Debug tools |
| `/pof reload` | `slop.pof.admin` | Reload the config |
| `/pof xp <give\|set\|setlevel> <player> <amount>` | `slop.pof.admin` | Change XP or level (offline players work too) |

## Permissions

| Node | Default | Description |
|---|---|---|
| `slop.pof.play` | true | Play: join, leave, own stats, level, leaderboards, lobby items |
| `slop.pof.vip` | op | Skip the queue countdown and receive the start item |
| `slop.pof.stats.others` | true | View another player's stats or level |
| `slop.pof.admin` | op | Everything above, plus all admin commands |

`slop.pof.admin` includes `play`, `vip`, and `stats.others`.

## Placeholders

When PlaceholderAPI is installed:

`%pof_wins%`, `%pof_kills%`, `%pof_deaths%`, `%pof_games%`, `%pof_streak%`, `%pof_best_streak%`,
`%pof_items%`, `%pof_playtime%`, `%pof_level%`, `%pof_xp%`, `%pof_xp_next%`, `%pof_name%`,
`%pof_arena%`, `%pof_state%`, `%pof_alive%`, `%pof_session_kills%`, `%pof_queue%`, and
`%pof_top_<rank>_<name|wins|kills|streak|level|value>%`.

## Configuration

`config.yml` is heavily commented and is the best reference. The main sections are:

| Section | What it controls |
|---|---|
| `prefix`, `server-ip` | Chat prefix and the address shown in chat and on the sidebar |
| `worlds` | Lobby world name, void game world name, lobby weather |
| `arenas` | Arena count, spacing, reset mode, reset speed and reach |
| `pillars` | Pillar count, height, Y level, and offsets |
| `void-y`, `build-radius`, `border-kill`, `max-build-y` | Elimination and building limits |
| `min-players`, `max-players`, `countdown`, `queue-*` | Queue and start timings and reminders |
| `item-seconds`, `item-delay-seconds`, `grace-seconds`, `end-seconds`, `max-game-seconds` | Round timings |
| `gamemodes` | The playable modes and their rules |
| `fireball` | Fire charge behaviour |
| `block-min`, `block-max`, `rates` | Item roll ranges and category weights |
| `lobby-items` | Lobby hotbar items, including the optional hub bed |
| `visibility` | The lobby visibility toggle |
| `leveling` | XP sources, the level curve, and the progress bar |
| `storage` | H2 or MySQL connection details |
| `leaderboards` | Leaderboard size and refresh rate |
| `messages` | Every player-facing string, including the sidebar layouts |
| `items` | Item pools, amounts, chances, enchants, and illegal items |

### Updating

The plugin tracks `config-version` inside `config.yml`. On startup and on `/pof reload` it walks an
older file forward one version at a time, keeps your values, and writes a `config.yml.vN.bak` backup
before writing. `config-version` should not be edited by hand.

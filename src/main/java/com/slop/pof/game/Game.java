package com.slop.pof.game;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import com.slop.pof.Perms;
import com.slop.pof.PoFPlugin;
import com.slop.pof.arena.Arena;
import com.slop.pof.arena.PillarSlots;
import com.slop.pof.config.Gamemode;
import com.slop.pof.config.Settings;
import com.slop.pof.storage.Stats;
import com.slop.pof.storage.TopEntry;
import com.slop.pof.util.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Game {
    public static final int VIP_TIMER_SECONDS = 5;
    public static final int SUDDEN_DEATH_INTERVAL = 2;
    public static final int LIGHTNING_CHANCE = 35;
    public static final int LIGHTNING_EVERY = 10;

    private final PoFPlugin plugin;
    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();
    private final Map<String, List<UUID>> queues = new LinkedHashMap<>();
    private final Map<String, Integer> timers = new HashMap<>();
    private final Map<UUID, String> queuedMode = new HashMap<>();
    private final Set<String> arenasBusy = new HashSet<>();
    private List<TopEntry> top = List.of();
    private boolean topReady;

    public Game(PoFPlugin plugin) {
        this.plugin = plugin;
    }

    public Session session(Player player) {
        return session(player.getUniqueId());
    }

    public Session session(UUID uuid) {
        return sessions.computeIfAbsent(uuid, Session::new);
    }

    public Session peek(UUID uuid) {
        return sessions.get(uuid);
    }

    public boolean topReady() {
        return topReady;
    }

    public boolean isAlive(Player player) {
        return player != null && session(player).alive;
    }

    public Arena arena(Player player) {
        if (player == null) {
            return null;
        }
        int id = session(player).arenaId;
        return id == 0 ? null : plugin.arenas().get(id);
    }

    public boolean queued(UUID uuid) {
        return queuedMode.containsKey(uuid);
    }

    public String queuedMode(UUID uuid) {
        return queuedMode.get(uuid);
    }

    public int queueSize() {
        return queuedMode.size();
    }

    public int queueSize(String modeId) {
        if (modeId == null) {
            return 0;
        }
        List<UUID> waiting = queues.get(modeId);
        return waiting == null ? 0 : waiting.size();
    }

    public int queueTimer(UUID uuid) {
        String modeId = queuedMode.get(uuid);
        if (modeId == null) {
            return 0;
        }
        Integer timer = timers.get(modeId);
        return timer == null ? 0 : timer;
    }

    public boolean waitingForPlayers(UUID uuid) {
        String modeId = queuedMode.get(uuid);
        Gamemode mode = plugin.settings().gamemode(modeId);
        if (mode == null) {
            return false;
        }
        return queueSize(modeId) < mode.minPlayers(plugin.settings());
    }

    public boolean arenasBusy(UUID uuid) {
        String modeId = queuedMode.get(uuid);
        return modeId != null && arenasBusy.contains(modeId);
    }

    /** One line of queue, timer, and arena state for /pof debug. */
    public String debugStatus() {
        StringBuilder text = new StringBuilder();
        text.append("queues=");
        boolean any = false;
        for (Gamemode mode : plugin.settings().enabledGamemodes()) {
            int size = queueSize(mode.id);
            Integer timer = timers.get(mode.id);
            if (size == 0 && timer == null && !arenasBusy.contains(mode.id)) {
                continue;
            }
            if (any) {
                text.append(", ");
            }
            any = true;
            text.append(mode.id).append(':').append(size);
            if (timer != null) {
                text.append(" timer=").append(timer);
            }
            if (arenasBusy.contains(mode.id)) {
                text.append(" busy");
            }
        }
        if (!any) {
            text.append("none");
        }
        Arena free = plugin.arenas().free();
        text.append(" free=").append(free == null ? "none" : free.id);
        text.append(' ').append(plugin.arenas().describe());
        return text.toString();
    }

    public List<TopEntry> top() {
        if (!topReady) {
            refreshTop();
        }
        return top;
    }

    public void refreshTop() {
        try {
            top = plugin.database().top(plugin.settings().leaderboardSize());
            topReady = true;
        } catch (RuntimeException ex) {
            plugin.getLogger().warning("Leaderboard refresh failed: " + ex.getMessage());
        }
    }

    public void invalidateTop() {
        refreshTop();
    }

    public void handleJoin(Player player) {
        plugin.database().ensure(player.getUniqueId(), player.getName());
        session(player);
        toLobby(player);
        plugin.boards().refresh(player);
        say(player, "join-hint");
    }

    public void handleQuit(Player player) {
        Session session = session(player);
        int arenaId = session.arenaId;
        if (session.alive) {
            eliminate(player, "leave");
        }
        leaveQueue(player.getUniqueId());
        Arena arena = arenaId == 0 ? null : plugin.arenas().get(arenaId);
        if (arena != null) {
            arena.players.remove(player.getUniqueId());
            arena.alive.remove(player.getUniqueId());
            checkWin(arena);
        }
        session.arenaId = 0;
        session.alive = false;
        plugin.boards().clear(player);
        sessions.remove(player.getUniqueId());
    }

    public void join(Player player) {
        Settings settings = plugin.settings();
        if (!player.hasPermission(Perms.PLAY)) {
            say(player, "no-permission");
            return;
        }
        plugin.database().ensure(player.getUniqueId(), player.getName());
        Session session = session(player);
        if (session.arenaId != 0) {
            say(player, "already-in-match");
            return;
        }
        List<Gamemode> modes = settings.enabledGamemodes();
        if (modes.isEmpty()) {
            say(player, "gamemode-none");
            return;
        }
        if (modes.size() == 1) {
            joinMode(player, modes.get(0));
            return;
        }
        if (queued(player.getUniqueId())) {
            say(player, "already-queued");
            return;
        }
        plugin.menus().open(player);
    }

    public void joinMode(Player player, Gamemode mode) {
        Settings settings = plugin.settings();
        if (mode == null || !mode.enabled) {
            say(player, "gamemode-none");
            return;
        }
        Session session = session(player);
        if (session.arenaId != 0) {
            say(player, "already-in-match");
            return;
        }
        if (mode.id.equals(queuedMode.get(player.getUniqueId()))) {
            say(player, "already-queued");
            return;
        }
        leaveQueue(player.getUniqueId());
        queue(mode.id).add(player.getUniqueId());
        queuedMode.put(player.getUniqueId(), mode.id);
        int count = queueSize(mode.id);
        int max = mode.maxPlayers(settings);
        String label = Text.color(mode.name);
        say(player, "queued", "mode", label, "count", String.valueOf(count), "max", String.valueOf(max));
        Text.sound(player, settings.sound("queue-join", "random.orb"), 1f, 1.2f);
        plugin.items().giveQueued(player);
        lobby(player, "queue-join-broadcast", "player", player.getName(), "mode", label, "count", String.valueOf(count), "max", String.valueOf(max));
        plugin.boards().refresh(player);
    }

    public void leave(Player player) {
        Settings settings = plugin.settings();
        Session session = session(player);
        if (session.alive) {
            eliminate(player, "leave");
            toLobby(player);
            say(player, "forfeited");
            plugin.boards().refresh(player);
            return;
        }
        String modeId = queuedMode.get(player.getUniqueId());
        if (leaveQueue(player.getUniqueId())) {
            plugin.items().giveLobby(player);
            say(player, "queue-leave");
            Gamemode mode = settings.gamemode(modeId);
            int max = mode == null ? settings.maxPlayers() : mode.maxPlayers(settings);
            String label = mode == null ? "" : Text.color(mode.name);
            lobby(player, "queue-leave-broadcast", "player", player.getName(), "mode", label,
                    "count", String.valueOf(queueSize(modeId)), "max", String.valueOf(max));
            plugin.boards().refresh(player);
            return;
        }
        say(player, "nothing-to-leave");
    }

    public void leaveQueueItem(Player player) {
        Session session = session(player);
        if (session.arenaId != 0) {
            return;
        }
        String modeId = queuedMode.get(player.getUniqueId());
        if (!leaveQueue(player.getUniqueId())) {
            return;
        }
        Settings settings = plugin.settings();
        plugin.items().giveLobby(player);
        say(player, "queue-leave");
        Gamemode mode = settings.gamemode(modeId);
        int max = mode == null ? settings.maxPlayers() : mode.maxPlayers(settings);
        String label = mode == null ? "" : Text.color(mode.name);
        lobby(player, "queue-leave-broadcast", "player", player.getName(), "mode", label,
                "count", String.valueOf(queueSize(modeId)), "max", String.valueOf(max));
        plugin.boards().refresh(player);
    }

    public boolean vipStart(Player player, boolean fromItem) {
        Settings settings = plugin.settings();
        if (!player.hasPermission(Perms.VIP)) {
            say(player, "no-permission");
            return false;
        }
        Session session = session(player);
        if (session.arenaId != 0) {
            say(player, "cannot-during-match");
            return false;
        }
        boolean admin = player.hasPermission(Perms.ADMIN);
        if (!queued(player.getUniqueId()) && (fromItem || !admin)) {
            say(player, "join-queue-first");
            return false;
        }
        return skipTimer(player);
    }

    public boolean skipTimer(Player actor) {
        Settings settings = plugin.settings();
        List<Gamemode> targets = new ArrayList<>();
        if (actor != null && queued(actor.getUniqueId())) {
            Gamemode mode = settings.gamemode(queuedMode.get(actor.getUniqueId()));
            if (mode != null) {
                targets.add(mode);
            }
        } else {
            targets.addAll(settings.enabledGamemodes());
        }
        boolean any = false;
        boolean ready = false;
        for (Gamemode mode : targets) {
            int count = queueSize(mode.id);
            int min = mode.minPlayers(settings);
            if (count < min) {
                continue;
            }
            ready = true;
            any = skipTimer(mode, actor) || any;
        }
        if (!ready && actor != null) {
            int min = targets.isEmpty() ? settings.minPlayers() : targets.get(0).minPlayers(settings);
            say(actor, "need-players", "min", String.valueOf(min));
        }
        return any;
    }

    private boolean skipTimer(Gamemode mode, Player actor) {
        Settings settings = plugin.settings();
        Integer timer = timers.get(mode.id);
        if (timer == null) {
            timer = mode.queueSeconds(settings);
        }
        Arena free = plugin.arenas().free();
        if (plugin.isDebug()) {
            plugin.getLogger().info("VIP " + mode.id + " timer " + timer + " free " + (free == null ? "none" : free.id));
        }
        if (free == null) {
            arenasBusy.add(mode.id);
            if (actor != null) {
                say(actor, "arenas-busy");
            }
            return false;
        }
        arenasBusy.remove(mode.id);
        if (timer > VIP_TIMER_SECONDS) {
            timers.put(mode.id, VIP_TIMER_SECONDS);
            for (UUID uuid : queue(mode.id)) {
                Player queued = Bukkit.getPlayer(uuid);
                if (queued == null) {
                    continue;
                }
                say(queued, "countdown-skipped");
                Text.title(queued, settings.text("title-count", "time", "5"), settings.text("subtitle-count"), 1);
            }
            if (actor != null) {
                say(actor, "timer-set");
            }
            return true;
        }
        if (actor != null) {
            say(actor, "timer-already", "time", String.valueOf(timer));
        }
        return false;
    }

    public void toLobby(Player player) {
        Session session = session(player);
        if (session.arenaId != 0) {
            Arena arena = plugin.arenas().get(session.arenaId);
            if (arena != null) {
                arena.players.remove(player.getUniqueId());
                arena.alive.remove(player.getUniqueId());
            }
        }
        session.arenaId = 0;
        session.alive = false;
        session.kills = 0;
        session.pillar = 0;
        player.setGameMode(plugin.settings().lobbyGamemode());
        clearInventory(player);
        heal(player);
        player.setLevel(0);
        player.setExp(0f);
        clearEffects(player);
        Location lobby = plugin.lobby().resolve();
        if (lobby != null) {
            player.teleport(lobby);
        }
        if (player.hasPermission(Perms.PLAY)) {
            plugin.items().giveLobby(player);
        }
        plugin.boards().refreshVisibility();
    }

    public boolean stopArena(int id) {
        Arena arena = plugin.arenas().get(id);
        if (arena == null) {
            return false;
        }
        for (UUID uuid : new ArrayList<>(arena.players)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                toLobby(player);
                plugin.boards().refresh(player);
            }
        }
        arena.players.clear();
        arena.alive.clear();
        arena.winner = null;
        plugin.resets().enqueue(arena);
        return true;
    }

    public void stopAll() {
        for (Arena arena : plugin.arenas().all()) {
            stopArena(arena.id);
        }
        queues.clear();
        timers.clear();
        queuedMode.clear();
        arenasBusy.clear();
    }

    public void regen() {
        for (Arena arena : plugin.arenas().all()) {
            if (arena.state == Arena.State.WAITING) {
                plugin.resets().enqueue(arena);
            }
        }
    }

    public void second() {
        tryStart();
        for (Arena arena : plugin.arenas().all()) {
            tickArena(arena);
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            plugin.boards().refresh(player);
            borderCheck(player);
        }
    }

    public void fastVoid() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Session session = sessions.get(player.getUniqueId());
            if (session == null || !session.alive) {
                continue;
            }
            Arena arena = plugin.arenas().get(session.arenaId);
            if (arena == null || arena.state == Arena.State.ENDING) {
                continue;
            }
            if (player.getLocation().getY() < plugin.settings().voidY()) {
                eliminate(player, "void");
            }
        }
    }

    public void upkeep() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setFoodLevel(20);
        }
        plugin.worlds().keepWeatherClear();
    }

    public void playtime() {
        boolean onlineOnly = plugin.settings().playtimeOnlineOnly();
        for (Player player : Bukkit.getOnlinePlayers()) {
            Session session = sessions.get(player.getUniqueId());
            if (!onlineOnly && (session == null || session.arenaId == 0)) {
                continue;
            }
            plugin.database().ensure(player.getUniqueId(), player.getName());
            plugin.database().addPlaytime(player.getUniqueId());
        }
    }

    public void eliminate(Player player, String reason) {
        if (player == null) {
            return;
        }
        Session session = session(player);
        if (!session.alive) {
            return;
        }
        Arena arena = plugin.arenas().get(session.arenaId);
        if (arena == null) {
            return;
        }
        Settings settings = plugin.settings();
        session.alive = false;
        arena.alive.remove(player.getUniqueId());
        plugin.database().addDeath(player.getUniqueId());
        clearInventory(player);
        player.setGameMode(GameMode.SPECTATOR);
        Location center = center(arena);
        if (center != null) {
            player.teleport(center);
        }
        Player voidKiller = "void".equals(reason) ? recentAttacker(player) : null;
        if (voidKiller != null) {
            grantKill(player, voidKiller);
            arenaMessage(arena, player, "elim-void-kill", "player", player.getName(), "killer", voidKiller.getName());
        } else {
            String key = switch (reason) {
                case "void" -> "elim-void";
                case "leave" -> "elim-leave";
                default -> "elim-death";
            };
            arenaMessage(arena, player, key, "player", player.getName());
        }
        session.lastHitBy = null;
        session.lastHitAt = 0L;
        Text.title(player, settings.text("title-eliminated"), settings.text("subtitle-eliminated"), 3);
        Text.sound(player, settings.sound("eliminated", "mob.wither.hurt"), 0.25f, 1.3f);
        if (plugin.isDebug()) {
            plugin.getLogger().info("Eliminated " + player.getName() + " (" + reason + ") arena " + arena.id);
        }
        checkWin(arena);
    }

    public void tagAttacker(Player victim, Player attacker) {
        if (victim == null || attacker == null || victim.equals(attacker)) {
            return;
        }
        Session session = session(victim);
        session.lastHitBy = attacker.getUniqueId();
        session.lastHitAt = System.currentTimeMillis();
    }

    public void creditKill(Player victim, Player attacker) {
        if (!grantKill(victim, attacker)) {
            return;
        }
        Arena arena = arena(victim);
        if (arena != null) {
            arenaMessage(arena, victim, "slain", "victim", victim.getName(), "killer", attacker.getName());
        }
    }

    private boolean grantKill(Player victim, Player attacker) {
        if (attacker == null || victim == null || attacker.equals(victim)) {
            return false;
        }
        if (arena(victim) == null) {
            return false;
        }
        plugin.database().addKill(attacker.getUniqueId());
        session(attacker).kills++;
        Text.sound(attacker, plugin.settings().sound("kill", "random.orb"), 1f, 1.4f);
        return true;
    }

    private Player recentAttacker(Player victim) {
        Session session = session(victim);
        if (session.lastHitBy == null) {
            return null;
        }
        long window = plugin.settings().voidCreditSeconds() * 1000L;
        if (window <= 0 || System.currentTimeMillis() - session.lastHitAt > window) {
            return null;
        }
        Player attacker = Bukkit.getPlayer(session.lastHitBy);
        if (attacker == null || attacker.equals(victim)) {
            return null;
        }
        Arena victimArena = arena(victim);
        Arena attackerArena = arena(attacker);
        if (victimArena == null || attackerArena == null || victimArena.id != attackerArena.id) {
            return null;
        }
        return attacker;
    }

    private void tryStart() {
        for (Gamemode mode : plugin.settings().enabledGamemodes()) {
            tryStart(mode);
        }
    }

    private void tryStart(Gamemode mode) {
        Settings settings = plugin.settings();
        List<UUID> waiting = queue(mode.id);
        int min = mode.minPlayers(settings);
        if (waiting.size() < min) {
            timers.remove(mode.id);
            arenasBusy.remove(mode.id);
            return;
        }
        Integer timer = timers.get(mode.id);
        if (timer == null) {
            timers.put(mode.id, mode.queueSeconds(settings));
            arenasBusy.remove(mode.id);
            for (UUID uuid : waiting) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    say(player, "match-starting", "seconds", String.valueOf(mode.queueSeconds(settings)));
                }
            }
            return;
        }
        timer--;
        timers.put(mode.id, timer);
        if (timer <= 0) {
            Arena arena = plugin.arenas().free();
            if (arena == null) {
                timers.put(mode.id, 1);
                if (arenasBusy.add(mode.id)) {
                    for (UUID uuid : waiting) {
                        Player player = Bukkit.getPlayer(uuid);
                        if (player != null) {
                            say(player, "arenas-busy");
                        }
                    }
                }
                if (plugin.isDebug()) {
                    plugin.getLogger().info("Queue " + mode.id + " is ready and every arena is busy. " + debugStatus());
                }
                return;
            }
            arenasBusy.remove(mode.id);
            int cap = Math.min(mode.maxPlayers(settings), settings.pillarCount());
            List<UUID> batch = new ArrayList<>();
            for (UUID uuid : waiting) {
                if (batch.size() >= cap) {
                    break;
                }
                batch.add(uuid);
            }
            for (UUID uuid : batch) {
                leaveQueue(uuid);
            }
            arena.players.addAll(batch);
            arena.alive.addAll(batch);
            arena.gamemodeId = mode.id;
            beginStart(arena);
            return;
        }
        if (timer <= VIP_TIMER_SECONDS) {
            for (UUID uuid : waiting) {
                Player player = Bukkit.getPlayer(uuid);
                if (player == null) {
                    continue;
                }
                Text.title(player, settings.text("title-count", "time", String.valueOf(timer)), settings.text("subtitle-count"), 1);
                Text.sound(player, settings.sound("countdown", "note.pling"), 1f, 1f);
            }
        } else if (timer == 20 || timer == 10) {
            lobby(null, "match-starting-soon", "seconds", String.valueOf(timer));
        }
    }

    private List<UUID> queue(String modeId) {
        return queues.computeIfAbsent(modeId, id -> new ArrayList<>());
    }

    private boolean leaveQueue(UUID uuid) {
        String modeId = queuedMode.remove(uuid);
        if (modeId == null) {
            return false;
        }
        List<UUID> waiting = queues.get(modeId);
        if (waiting != null) {
            waiting.remove(uuid);
        }
        return true;
    }

    private void tickArena(Arena arena) {
        Settings settings = plugin.settings();
        if (arena.state == Arena.State.WAITING || arena.state == Arena.State.RESETTING) {
            return;
        }
        if (arena.state == Arena.State.STARTING) {
            arena.count--;
            for (UUID uuid : arena.players) {
                Player player = Bukkit.getPlayer(uuid);
                if (player == null) {
                    continue;
                }
                if (arena.count <= 5 && arena.count > 0) {
                    Text.title(player, settings.text("title-start-count", "time", String.valueOf(arena.count)),
                            settings.text("subtitle-start-count"), 1);
                    Text.sound(player, settings.sound("countdown", "note.pling"), 1f, 1f);
                }
            }
            if (arena.count <= 0) {
                beginPlay(arena);
            }
            return;
        }
        if (arena.state == Arena.State.INGAME) {
            arena.time++;
            if (arena.grace > 0) {
                arena.grace--;
                if (arena.grace == 0) {
                    arenaMessage(arena, null, "grace-over");
                }
            }
            arena.item--;
            if (arena.item <= 0) {
                int interval = arena.time >= settings.maxGameSeconds() ? SUDDEN_DEATH_INTERVAL : settings.itemSeconds();
                arena.item = interval;
                for (UUID uuid : new ArrayList<>(arena.alive)) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player == null) {
                        continue;
                    }
                    plugin.items().giveRandom(player);
                    Text.sound(player, settings.sound("item", "random.pop"), 1f, 1.3f);
                    Text.actionBar(player, settings.text("action-item"));
                }
                if (arena.time >= settings.maxGameSeconds() && arena.time % LIGHTNING_EVERY == 0) {
                    arenaMessage(arena, null, "sudden-death");
                    for (Location pillar : arena.pillars) {
                        if (Math.random() < (LIGHTNING_CHANCE / 100.0) && pillar.getWorld() != null) {
                            pillar.getWorld().strikeLightning(pillar);
                        }
                    }
                }
            }
            checkWin(arena);
            return;
        }
        if (arena.state == Arena.State.ENDING) {
            arena.end--;
            if (arena.end <= 0) {
                for (UUID uuid : new ArrayList<>(arena.players)) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null) {
                        toLobby(player);
                        plugin.boards().refresh(player);
                    }
                }
                arena.players.clear();
                arena.alive.clear();
                arena.winner = null;
                plugin.resets().enqueue(arena);
            }
        }
    }

    private void beginStart(Arena arena) {
        Settings settings = plugin.settings();
        Gamemode mode = settings.gamemode(arena.gamemodeId);
        int countdown = mode == null ? settings.countdown() : mode.countdown(settings);
        arena.state = Arena.State.STARTING;
        arena.count = countdown;
        if (plugin.isDebug()) {
            plugin.getLogger().info("Match starting in arena " + arena.id + " (" + arena.gamemodeId + "). " + debugStatus());
        }
        plugin.pillars().build(arena);
        int players = arena.players.size();
        int pillarCount = arena.pillars.size();
        int index = 0;
        for (UUID uuid : arena.players) {
            int slot = PillarSlots.slot(index, players, pillarCount);
            int pillar = slot + 1;
            index++;
            Session session = session(uuid);
            session.arenaId = arena.id;
            session.alive = true;
            session.kills = 0;
            session.pillar = pillar;
            plugin.database().ensure(uuid, nameOf(uuid));
            plugin.database().addGame(uuid);
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && pillar <= pillarCount) {
                Location stand = arena.pillars.get(slot).clone().add(0.5, 1.0, 0.5);
                player.teleport(stand);
                plugin.pillars().cage(arena, pillar, true);
                player.setGameMode(GameMode.ADVENTURE);
                clearInventory(player);
                heal(player);
                clearEffects(player);
                Text.title(player, settings.text("title-cages"),
                        settings.text("subtitle-cages", "seconds", String.valueOf(countdown)), 3);
                Text.sound(player, settings.sound("cages", "mob.wither.spawn"), 1f, 1.5f);
            }
        }
        plugin.boards().refreshVisibility();
    }

    private void beginPlay(Arena arena) {
        Settings settings = plugin.settings();
        removeJunk(arena);
        Gamemode mode = settings.gamemode(arena.gamemodeId);
        int delay = mode == null ? settings.itemDelaySeconds() : mode.itemDelay(settings);
        arena.state = Arena.State.INGAME;
        arena.grace = settings.graceSeconds();
        arena.time = 0;
        arena.item = delay > 0 ? delay : settings.itemSeconds();
        plugin.pillars().openAll(arena);
        for (UUID uuid : arena.alive) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                continue;
            }
            player.setGameMode(GameMode.SURVIVAL);
            Text.title(player, settings.text("title-fight"), settings.text("subtitle-fight"), 2);
            Text.sound(player, settings.sound("fight", "random.levelup"), 1f, 0.8f);
            if (delay <= 0) {
                plugin.items().giveRandom(player);
            }
        }
    }

    private void checkWin(Arena arena) {
        if (arena.state != Arena.State.INGAME) {
            return;
        }
        int alive = arena.aliveCount();
        if (alive <= 0) {
            finish(arena, null);
        } else if (alive == 1) {
            finish(arena, arena.alive.iterator().next());
        }
    }

    private void finish(Arena arena, UUID winner) {
        if (arena.state == Arena.State.ENDING) {
            return;
        }
        Settings settings = plugin.settings();
        arena.state = Arena.State.ENDING;
        arena.end = settings.endSeconds();
        arena.winner = winner;
        if (winner == null) {
            arenaMessage(arena, null, "no-winner");
            for (UUID uuid : arena.players) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    Text.title(player, settings.text("title-draw"), settings.text("subtitle-draw"), 4);
                }
            }
            return;
        }
        plugin.database().ensure(winner, nameOf(winner));
        plugin.database().addWin(winner);
        invalidateTop();
        Stats stats = plugin.database().get(winner);
        String name = stats == null ? nameOf(winner) : stats.name;
        Player winnerPlayer = Bukkit.getPlayer(winner);
        if (winnerPlayer != null) {
            name = winnerPlayer.getName();
            Text.title(winnerPlayer, settings.text("title-victory"), settings.text("subtitle-victory"), 5);
            Text.sound(winnerPlayer, settings.sound("victory", "random.levelup"), 1f, 1f);
        }
        arenaMessage(arena, winnerPlayer, "won", "player", name);
        for (UUID uuid : arena.players) {
            if (uuid.equals(winner)) {
                continue;
            }
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                continue;
            }
            Text.title(player, settings.text("title-defeat"), settings.text("subtitle-defeat", "player", name), 4);
            Text.sound(player, settings.sound("defeat", "random.explode"), 1f, 1.4f);
        }
    }

    private void borderCheck(Player player) {
        Session session = sessions.get(player.getUniqueId());
        if (session == null || !session.alive) {
            return;
        }
        Arena arena = plugin.arenas().get(session.arenaId);
        if (arena == null || arena.state == Arena.State.ENDING) {
            return;
        }
        if (player.getLocation().getY() < plugin.settings().voidY()) {
            eliminate(player, "void");
            return;
        }
        Location center = center(arena);
        if (center != null && center.getWorld() != null
                && center.getWorld().equals(player.getWorld())
                && player.getLocation().distance(center) > plugin.settings().borderKill()) {
            eliminate(player, "void");
        }
    }

    private void removeJunk(Arena arena) {
        if (arena.world == null) {
            return;
        }
        int radius = plugin.settings().resetReach();
        Location center = arena.centerAt(plugin.settings().pillarY());
        if (center == null) {
            return;
        }
        for (org.bukkit.entity.Entity entity : arena.world.getNearbyEntities(center, radius, arena.world.getMaxHeight(), radius)) {
            if (!(entity instanceof Player)) {
                entity.remove();
            }
        }
    }

    private void say(Player player, String key, String... pairs) {
        if (player != null) {
            player.sendMessage(plugin.settings().chat(player, key, pairs));
        }
    }

    /** {@code about} is the player the line names. Null resolves placeholders for each recipient. */
    private void arenaMessage(Arena arena, Player about, String key, String... pairs) {
        String shared = about == null ? null : plugin.settings().chat(about, key, pairs);
        for (UUID uuid : arena.players) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                continue;
            }
            player.sendMessage(shared != null ? shared : plugin.settings().chat(player, key, pairs));
        }
    }

    /** {@code about} is the player the line names. Null resolves placeholders for each lobby player. */
    private void lobby(Player about, String key, String... pairs) {
        String shared = about == null ? null : plugin.settings().chat(about, key, pairs);
        for (Player player : Bukkit.getOnlinePlayers()) {
            Session session = sessions.get(player.getUniqueId());
            if (session != null && (session.arenaId != 0 || session.alive)) {
                continue;
            }
            player.sendMessage(shared != null ? shared : plugin.settings().chat(player, key, pairs));
        }
    }

    private Location center(Arena arena) {
        return arena.centerAt(plugin.settings().pillarY());
    }

    private String nameOf(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            return player.getName();
        }
        Stats stats = plugin.database().get(uuid);
        return stats == null ? "unknown" : stats.name;
    }

    private static void clearInventory(Player player) {
        PlayerInventory inv = player.getInventory();
        inv.clear();
        inv.setHelmet(null);
        inv.setChestplate(null);
        inv.setLeggings(null);
        inv.setBoots(null);
    }

    @SuppressWarnings("deprecation")
    private static void heal(Player player) {
        if (player.getHealth() > 0) {
            player.setHealth(player.getMaxHealth());
        }
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.setFireTicks(0);
        player.setFallDistance(0f);
    }

    private static void clearEffects(Player player) {
        for (PotionEffect effect : new ArrayList<>(player.getActivePotionEffects())) {
            player.removePotionEffect(effect.getType());
        }
    }

    public static final class Session {
        public final UUID uuid;
        public int arenaId;
        public boolean alive;
        public int kills;
        public int pillar;
        public UUID lastHitBy;
        public long lastHitAt;

        Session(UUID uuid) {
            this.uuid = uuid;
        }
    }
}

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
import com.slop.pof.config.Settings;
import com.slop.pof.storage.Stats;
import com.slop.pof.storage.TopEntry;
import com.slop.pof.util.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Game {
    public static final int VIP_TIMER_SECONDS = 5;
    public static final int SUDDEN_DEATH_INTERVAL = 2;
    public static final int LIGHTNING_CHANCE = 35;
    public static final int LIGHTNING_EVERY = 10;

    private final PoFPlugin plugin;
    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();
    private final List<UUID> queue = new ArrayList<>();
    private Integer queueTimer;
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
        return queue.contains(uuid);
    }

    public int queueSize() {
        return queue.size();
    }

    public int queueTimer() {
        return queueTimer == null ? 0 : queueTimer;
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
        player.sendMessage(plugin.settings().chat("join-hint"));
    }

    public void handleQuit(Player player) {
        Session session = session(player);
        int arenaId = session.arenaId;
        if (session.alive) {
            eliminate(player, "leave");
        }
        queue.remove(player.getUniqueId());
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
            player.sendMessage(settings.chat("no-permission"));
            return;
        }
        plugin.database().ensure(player.getUniqueId(), player.getName());
        Session session = session(player);
        if (session.arenaId != 0) {
            player.sendMessage(settings.chat("already-in-match"));
            return;
        }
        if (queue.contains(player.getUniqueId())) {
            player.sendMessage(settings.chat("already-queued"));
            return;
        }
        queue.add(player.getUniqueId());
        int count = queue.size();
        player.sendMessage(settings.chat("queued", "count", String.valueOf(count), "max", String.valueOf(settings.maxPlayers())));
        Text.sound(player, settings.sound("queue-join", "random.orb"), 1f, 1.2f);
        plugin.items().giveQueued(player);
        lobby("queue-join-broadcast", "player", player.getName(), "count", String.valueOf(count), "max", String.valueOf(settings.maxPlayers()));
        plugin.boards().refresh(player);
    }

    public void leave(Player player) {
        Settings settings = plugin.settings();
        Session session = session(player);
        if (session.alive) {
            eliminate(player, "leave");
            toLobby(player);
            player.sendMessage(settings.chat("forfeited"));
            plugin.boards().refresh(player);
            return;
        }
        if (queue.remove(player.getUniqueId())) {
            plugin.items().giveLobby(player);
            player.sendMessage(settings.chat("queue-leave"));
            int count = queue.size();
            lobby("queue-leave-broadcast", "player", player.getName(), "count", String.valueOf(count), "max", String.valueOf(settings.maxPlayers()));
            plugin.boards().refresh(player);
            return;
        }
        player.sendMessage(settings.chat("nothing-to-leave"));
    }

    public void leaveQueueItem(Player player) {
        Session session = session(player);
        if (session.arenaId != 0) {
            return;
        }
        if (!queue.remove(player.getUniqueId())) {
            return;
        }
        Settings settings = plugin.settings();
        plugin.items().giveLobby(player);
        player.sendMessage(settings.chat("queue-leave"));
        int count = queue.size();
        lobby("queue-leave-broadcast", "player", player.getName(), "count", String.valueOf(count), "max", String.valueOf(settings.maxPlayers()));
        plugin.boards().refresh(player);
    }

    public boolean vipStart(Player player, boolean fromItem) {
        Settings settings = plugin.settings();
        if (!player.hasPermission(Perms.VIP)) {
            player.sendMessage(settings.chat("no-permission"));
            return false;
        }
        Session session = session(player);
        if (session.arenaId != 0) {
            player.sendMessage(settings.chat("cannot-during-match"));
            return false;
        }
        boolean admin = player.hasPermission(Perms.ADMIN);
        if (!queue.contains(player.getUniqueId()) && (fromItem || !admin)) {
            player.sendMessage(settings.chat("join-queue-first"));
            return false;
        }
        return skipTimer(player);
    }

    public boolean skipTimer(Player actor) {
        Settings settings = plugin.settings();
        int count = queue.size();
        if (count < settings.minPlayers()) {
            if (actor != null) {
                actor.sendMessage(settings.chat("need-players", "min", String.valueOf(settings.minPlayers())));
            }
            return false;
        }
        if (queueTimer == null) {
            queueTimer = settings.queueSeconds();
        }
        if (queueTimer > VIP_TIMER_SECONDS) {
            queueTimer = VIP_TIMER_SECONDS;
            for (UUID uuid : queue) {
                Player queued = Bukkit.getPlayer(uuid);
                if (queued == null) {
                    continue;
                }
                queued.sendMessage(settings.chat("countdown-skipped"));
                Text.title(queued, settings.text("title-count", "time", "5"), settings.text("subtitle-count"), 1);
            }
            if (actor != null) {
                actor.sendMessage(settings.chat("timer-set"));
            }
        } else if (actor != null) {
            actor.sendMessage(settings.chat("timer-already", "time", String.valueOf(queueTimer)));
        }
        return true;
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
        queue.clear();
        queueTimer = null;
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
        String key = switch (reason) {
            case "void" -> "elim-void";
            case "leave" -> "elim-leave";
            default -> "elim-death";
        };
        arenaMessage(arena, settings.chat(key, "player", player.getName()));
        Text.title(player, settings.text("title-eliminated"), settings.text("subtitle-eliminated"), 3);
        Text.sound(player, settings.sound("eliminated", "mob.wither.hurt"), 0.25f, 1.3f);
        if (plugin.isDebug()) {
            plugin.getLogger().info("Eliminated " + player.getName() + " (" + reason + ") arena " + arena.id);
        }
        checkWin(arena);
    }

    public void creditKill(Player victim, Player attacker) {
        if (attacker == null || attacker.equals(victim)) {
            return;
        }
        Arena arena = arena(victim);
        if (arena == null) {
            return;
        }
        plugin.database().addKill(attacker.getUniqueId());
        session(attacker).kills++;
        arenaMessage(arena, plugin.settings().chat("slain", "victim", victim.getName(), "killer", attacker.getName()));
        Text.sound(attacker, plugin.settings().sound("kill", "random.orb"), 1f, 1.4f);
    }

    private void tryStart() {
        Settings settings = plugin.settings();
        if (queue.size() < settings.minPlayers()) {
            queueTimer = null;
            return;
        }
        if (queueTimer == null) {
            queueTimer = settings.queueSeconds();
            for (UUID uuid : queue) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    player.sendMessage(settings.chat("match-starting", "seconds", String.valueOf(settings.queueSeconds())));
                }
            }
            return;
        }
        queueTimer--;
        int time = queueTimer;
        if (time <= 0) {
            Arena arena = plugin.arenas().free();
            if (arena == null) {
                queueTimer = VIP_TIMER_SECONDS;
                return;
            }
            int cap = Math.min(settings.maxPlayers(), plugin.settings().pillarCount());
            List<UUID> batch = new ArrayList<>();
            for (UUID uuid : queue) {
                if (batch.size() >= cap) {
                    break;
                }
                batch.add(uuid);
            }
            queue.removeAll(batch);
            arena.players.addAll(batch);
            arena.alive.addAll(batch);
            queueTimer = null;
            beginStart(arena);
            return;
        }
        if (time <= VIP_TIMER_SECONDS) {
            for (UUID uuid : queue) {
                Player player = Bukkit.getPlayer(uuid);
                if (player == null) {
                    continue;
                }
                Text.title(player, settings.text("title-count", "time", String.valueOf(time)), settings.text("subtitle-count"), 1);
                Text.sound(player, settings.sound("countdown", "note.pling"), 1f, 1f);
            }
        } else if (time == 20 || time == 10) {
            lobby("match-starting-soon", "seconds", String.valueOf(time));
        }
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
                    arenaMessage(arena, settings.chat("grace-over"));
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
                    arenaMessage(arena, settings.chat("sudden-death"));
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
        arena.state = Arena.State.STARTING;
        arena.count = settings.countdown();
        plugin.pillars().build(arena);
        int n = 1;
        for (UUID uuid : arena.players) {
            Session session = session(uuid);
            session.arenaId = arena.id;
            session.alive = true;
            session.kills = 0;
            session.pillar = n;
            plugin.database().ensure(uuid, nameOf(uuid));
            plugin.database().addGame(uuid);
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && n <= arena.pillars.size()) {
                Location pillar = arena.pillars.get(n - 1);
                Location stand = pillar.clone().add(0.5, 1.0, 0.5);
                player.teleport(stand);
                plugin.pillars().cage(arena, n, true);
                player.setGameMode(GameMode.ADVENTURE);
                clearInventory(player);
                heal(player);
                clearEffects(player);
                Text.title(player, settings.text("title-cages"),
                        settings.text("subtitle-cages", "seconds", String.valueOf(settings.countdown())), 3);
                Text.sound(player, settings.sound("cages", "mob.wither.spawn"), 1f, 1.5f);
            }
            n++;
        }
    }

    private void beginPlay(Arena arena) {
        Settings settings = plugin.settings();
        removeJunk(arena);
        arena.state = Arena.State.INGAME;
        arena.item = settings.itemSeconds();
        arena.grace = settings.graceSeconds();
        arena.time = 0;
        plugin.pillars().openAll(arena);
        for (UUID uuid : arena.alive) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                continue;
            }
            player.setGameMode(GameMode.SURVIVAL);
            Text.title(player, settings.text("title-fight"), settings.text("subtitle-fight"), 2);
            Text.sound(player, settings.sound("fight", "random.levelup"), 1f, 0.8f);
            plugin.items().giveRandom(player);
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
            arenaMessage(arena, settings.chat("no-winner"));
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
            winnerPlayer.setPlayerListName(trimList(org.bukkit.ChatColor.GOLD + winnerPlayer.getName()));
        }
        arenaMessage(arena, settings.chat("won", "player", name));
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

    private void arenaMessage(Arena arena, String message) {
        for (UUID uuid : arena.players) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.sendMessage(message);
            }
        }
    }

    private void lobby(String key, String... pairs) {
        String message = plugin.settings().chat(key, pairs);
        for (Player player : Bukkit.getOnlinePlayers()) {
            Session session = sessions.get(player.getUniqueId());
            if (session != null && (session.arenaId != 0 || session.alive)) {
                continue;
            }
            player.sendMessage(message);
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

    private static String trimList(String colored) {
        return colored.length() <= 16 ? colored : colored.substring(0, 16);
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

        Session(UUID uuid) {
            this.uuid = uuid;
        }
    }
}

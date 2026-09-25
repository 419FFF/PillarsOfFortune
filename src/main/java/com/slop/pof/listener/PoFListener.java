package com.slop.pof.listener;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;
import com.slop.pof.Perms;
import com.slop.pof.PoFPlugin;
import com.slop.pof.arena.Arena;
import com.slop.pof.game.Game;

import java.util.Locale;
import java.util.UUID;

public final class PoFListener implements Listener {
    private final PoFPlugin plugin;

    public PoFListener(PoFPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        event.setJoinMessage(plugin.settings().raw("join-message", "player", player.getName()));
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                plugin.game().handleJoin(player);
            }
        }, 5L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        event.setQuitMessage(plugin.settings().raw("quit-message", "player", player.getName()));
        plugin.game().handleQuit(player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        if (isExplosion(event.getCause()) && plugin.fireballs().systemActive()) {
            event.setCancelled(true);
            return;
        }
        if (event instanceof EntityDamageByEntityEvent by) {
            if (by.getDamager() instanceof Fireball fireball && plugin.fireballs().isOurs(fireball)) {
                event.setCancelled(true);
                return;
            }
        }
        Arena arena = plugin.game().arena(victim);
        if (arena == null || arena.state != Arena.State.INGAME || !plugin.game().isAlive(victim)) {
            event.setCancelled(true);
            return;
        }
        if (arena.grace > 0) {
            event.setCancelled(true);
            return;
        }
        Player attacker = attacker(event);
        if (attacker != null) {
            Arena other = plugin.game().arena(attacker);
            if (other == null || other.id != arena.id) {
                event.setCancelled(true);
                return;
            }
            if (!plugin.fireballs().isCustomDamage(victim.getUniqueId())
                    && event instanceof EntityDamageByEntityEvent by
                    && by.getDamager() instanceof Player
                    && holdingRod(attacker)) {
                consumeRod(attacker);
            }
        }
        if (victim.getHealth() - event.getFinalDamage() < 1) {
            event.setCancelled(true);
            plugin.game().creditKill(victim, attacker);
            plugin.game().eliminate(victim, "death");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamageEntity(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Fireball fireball && event.getDamager() instanceof Player) {
            plugin.fireballs().clearOwner(fireball);
        }
        if (event.getDamager() instanceof Player attacker && !(event.getEntity() instanceof Player) && holdingRod(attacker)) {
            consumeRod(attacker);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        if (!plugin.game().isAlive(victim)) {
            return;
        }
        event.setDeathMessage(null);
        event.getDrops().clear();
        event.setDroppedExp(0);
        plugin.game().creditKill(victim, victim.getKiller());
        plugin.game().eliminate(victim, "death");
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Game.Session session = plugin.game().session(player);
        Arena arena = session.arenaId == 0 ? null : plugin.arenas().get(session.arenaId);
        if (arena != null && !session.alive && arena.world != null) {
            Location center = new Location(arena.world, arena.id * 2000.0, plugin.settings().pillarY(), 0);
            event.setRespawnLocation(center);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) {
                    return;
                }
                player.setGameMode(GameMode.SPECTATOR);
                player.teleport(center);
            });
            return;
        }
        Location lobby = plugin.lobby().resolve();
        if (lobby != null) {
            event.setRespawnLocation(lobby);
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                plugin.game().toLobby(player);
                plugin.boards().refresh(player);
            }
        });
    }

    @EventHandler
    public void onProjectile(ProjectileHitEvent event) {
        if (event.getEntity() instanceof Fireball fireball && plugin.fireballs().isOurs(fireball)) {
            plugin.fireballs().onHit(fireball);
        }
    }

    @EventHandler
    public void onPickup(PlayerPickupItemEvent event) {
        if (plugin.items().isIllegal(event.getItem().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        ItemStack stack = event.getItemDrop().getItemStack();
        if (plugin.items().isIllegal(stack) || plugin.items().isLobbyItem(stack)) {
            event.setCancelled(true);
            return;
        }
        if (plugin.game().arena(event.getPlayer()) == null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        if (plugin.items().isIllegal(event.getRecipe().getResult())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_AIR
                && event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null) {
            return;
        }
        if (item.getType() == Material.FIREBALL) {
            Arena arena = plugin.game().arena(player);
            if (plugin.game().isAlive(player) && arena != null && arena.state == Arena.State.INGAME) {
                event.setCancelled(true);
                plugin.fireballs().tryShoot(player);
                return;
            }
        }
        if (plugin.items().isJoin(item)) {
            event.setCancelled(true);
            plugin.game().join(player);
        } else if (plugin.items().isVip(item)) {
            event.setCancelled(true);
            plugin.game().vipStart(player, true);
        } else if (plugin.items().isLeave(item)) {
            event.setCancelled(true);
            plugin.game().leaveQueueItem(player);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Arena arena = plugin.game().arena(player);
        if (arena == null) {
            if (!player.hasPermission(Perms.ADMIN)) {
                event.setCancelled(true);
            }
            return;
        }
        if (!plugin.game().isAlive(player) || arena.state != Arena.State.INGAME) {
            event.setCancelled(true);
            return;
        }
        ItemStack hand = player.getItemInHand();
        if (plugin.items().isIllegal(hand) || (hand != null && hand.getType() == Material.BEDROCK)) {
            event.setCancelled(true);
            return;
        }
        if (player.getLocation().getY() > plugin.settings().maxBuildY()) {
            event.setCancelled(true);
            return;
        }
        Location center = new Location(arena.world, arena.id * 2000.0, plugin.settings().pillarY(), 0);
        double dx = Math.abs(player.getLocation().getX() - center.getX());
        double dz = Math.abs(player.getLocation().getZ() - center.getZ());
        if (dx > plugin.settings().buildRadius() || dz > plugin.settings().buildRadius()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Arena arena = plugin.game().arena(player);
        if (arena == null) {
            if (!player.hasPermission(Perms.ADMIN)) {
                event.setCancelled(true);
            }
            return;
        }
        if (!plugin.game().isAlive(player) || arena.state != Arena.State.INGAME) {
            event.setCancelled(true);
            return;
        }
        if (event.getBlock().getType() == Material.BEDROCK) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFood(FoodLevelChangeEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onPortal(PlayerPortalEvent event) {
        if (plugin.game().arena(event.getPlayer()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        Game.Session session = plugin.game().peek(uuid);
        if (session == null || session.arenaId == 0) {
            return;
        }
        event.setCancelled(true);
        String message = event.getMessage();
        int arenaId = session.arenaId;
        boolean alive = session.alive;
        String name = event.getPlayer().getName();
        Bukkit.getScheduler().runTask(plugin, () -> {
            Arena arena = plugin.arenas().get(arenaId);
            if (arena == null) {
                return;
            }
            String line = alive
                    ? plugin.settings().text("chat-alive", "player", name, "message", message)
                    : plugin.settings().text("chat-spec", "player", name, "message", message);
            for (UUID member : arena.players) {
                Player target = Bukkit.getPlayer(member);
                if (target != null) {
                    target.sendMessage(line);
                }
            }
        });
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (plugin.game().arena(player) == null || player.hasPermission(Perms.ADMIN)) {
            return;
        }
        if (blocked(event.getMessage())) {
            event.setCancelled(true);
        }
    }

    private static boolean blocked(String message) {
        String raw = message.startsWith("/") ? message.substring(1) : message;
        String base = raw.split(" ")[0].toLowerCase(Locale.ROOT);
        int colon = base.lastIndexOf(':');
        if (colon >= 0) {
            base = base.substring(colon + 1);
        }
        return base.equals("give")
                || base.equals("gamemode")
                || base.equals("tp")
                || base.equals("teleport")
                || base.equals("enchant")
                || base.equals("effect");
    }

    private static boolean isExplosion(EntityDamageEvent.DamageCause cause) {
        return cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION
                || cause == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION;
    }

    private static Player attacker(EntityDamageEvent event) {
        if (!(event instanceof EntityDamageByEntityEvent by)) {
            return null;
        }
        Entity damager = by.getDamager();
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile) {
            ProjectileSource source = projectile.getShooter();
            if (source instanceof Player player) {
                return player;
            }
        }
        return null;
    }

    private static boolean holdingRod(Player player) {
        ItemStack hand = player.getItemInHand();
        return hand != null && hand.getType() == Material.FISHING_ROD;
    }

    private static void consumeRod(Player player) {
        ItemStack hand = player.getItemInHand();
        if (hand == null || hand.getType() != Material.FISHING_ROD) {
            return;
        }
        if (hand.getAmount() <= 1) {
            player.setItemInHand(null);
        } else {
            hand.setAmount(hand.getAmount() - 1);
        }
    }
}

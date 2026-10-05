package com.slop.pof.game;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;
import com.slop.pof.PoFPlugin;
import com.slop.pof.arena.Arena;
import com.slop.pof.util.Text;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Hypixel / BedWars-style fire charges on the 1.8.8 API.
 * The charge is launched along the player's look, then the hit applies one custom
 * knockback (away from the blast, with an upward pop) and one custom damage hit.
 * Vanilla fireball damage and explosion damage are cancelled so they are not applied twice.
 */
public final class Fireballs {
    public static final String OWNER = "pof-owner";

    private final PoFPlugin plugin;
    private final Set<Fireball> live = new HashSet<>();
    private final Map<UUID, Long> cooldown = new HashMap<>();
    private final Map<UUID, Long> explosionImmunity = new HashMap<>();
    private final Set<UUID> customDamage = new HashSet<>();

    public Fireballs(PoFPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isCustomDamage(UUID uuid) {
        return customDamage.contains(uuid);
    }

    public boolean isOurs(Entity entity) {
        return entity != null && entity.hasMetadata(OWNER);
    }

    public boolean ignoreExplosion(UUID uuid) {
        Long until = explosionImmunity.get(uuid);
        if (until == null) {
            return false;
        }
        if (until < System.currentTimeMillis()) {
            explosionImmunity.remove(uuid);
            return false;
        }
        return true;
    }

    public void clearOwner(Entity entity) {
        if (entity != null && entity.hasMetadata(OWNER)) {
            entity.removeMetadata(OWNER, plugin);
        }
    }

    /** Drops fireballs that already exploded or fell out of the world. */
    public void tick() {
        if (live.isEmpty()) {
            return;
        }
        Iterator<Fireball> it = live.iterator();
        while (it.hasNext()) {
            Fireball fireball = it.next();
            if (fireball.isDead() || !fireball.isValid()) {
                it.remove();
            }
        }
    }

    public void onHit(Fireball fireball) {
        live.remove(fireball);
        Location blast = fireball.getLocation();
        if (blast.getWorld() == null) {
            return;
        }
        UUID owner = ownerOf(fireball);
        double radius = plugin.settings().fireballRadius();
        for (Entity entity : blast.getWorld().getNearbyEntities(blast, radius, radius, radius)) {
            if (!(entity instanceof Player player)) {
                continue;
            }
            if (!plugin.game().isAlive(player)) {
                continue;
            }
            Arena arena = plugin.game().arena(player);
            if (arena == null || arena.state != Arena.State.INGAME) {
                continue;
            }
            if (player.getLocation().distanceSquared(blast) > radius * radius) {
                continue;
            }
            explosionImmunity.put(player.getUniqueId(), System.currentTimeMillis() + 400L);
            Vector knockback = knockback(player, blast);
            // bedwars1058 sets the velocity right away. A follow-up tick re-asserts it because
            // 1.8 applies the vanilla explosion push after this event.
            player.setVelocity(knockback);
            player.setFallDistance(0f);
            double damage = owner != null && owner.equals(player.getUniqueId())
                    ? plugin.settings().fireballDamageSelf()
                    : plugin.settings().fireballDamageEnemy();
            if (damage > 0) {
                Player shooter = owner == null ? null : plugin.getServer().getPlayer(owner);
                customDamage.add(player.getUniqueId());
                try {
                    if (shooter != null && !shooter.equals(player)) {
                        player.damage(damage, shooter);
                    } else {
                        player.damage(damage);
                    }
                } finally {
                    customDamage.remove(player.getUniqueId());
                }
            }
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (player.isOnline() && plugin.game().isAlive(player)) {
                    player.setVelocity(knockback);
                    player.setFallDistance(0f);
                }
            });
        }
    }

    /**
     * @return true when the click was handled (including a shot that is still on cooldown)
     */
    public boolean tryShoot(Player player) {
        if (!plugin.game().isAlive(player)) {
            return false;
        }
        Arena arena = plugin.game().arena(player);
        if (arena == null || arena.state != Arena.State.INGAME) {
            return false;
        }
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long until = cooldown.get(uuid);
        if (until != null && until > now) {
            return true;
        }
        ItemStack hand = player.getItemInHand();
        if (hand == null || hand.getType() != Material.FIREBALL) {
            return false;
        }
        cooldown.put(uuid, now + (long) (plugin.settings().fireballCooldown() * 1000.0));
        if (hand.getAmount() <= 1) {
            player.setItemInHand(null);
        } else {
            hand.setAmount(hand.getAmount() - 1);
        }
        Vector direction = player.getEyeLocation().getDirection();
        if (direction.lengthSquared() < 1.0E-6) {
            direction = new Vector(0, 0, 1);
        }
        direction.normalize();
        Location spawnAt = player.getEyeLocation().add(direction.clone().multiply(1.2));
        Fireball fireball = player.getWorld().spawn(spawnAt, Fireball.class);
        fireball.setShooter(player);
        // bedwars1058 points the NMS direction at direction * 0.1 and then sets the velocity to
        // that heading times the speed multiplier. Both are needed for a straight 1.8 shot.
        Vector heading = direction.clone().multiply(0.1);
        fireball.setDirection(heading);
        fireball.setVelocity(heading.clone().multiply(plugin.settings().fireballSpeed()));
        fireball.setIsIncendiary(plugin.settings().fireballFire());
        fireball.setYield((float) plugin.settings().fireballYield());
        fireball.setMetadata(OWNER, new FixedMetadataValue(plugin, uuid.toString()));
        live.add(fireball);
        Text.sound(player, plugin.settings().sound("fireball", "mob.ghast.fireball"), 1f, 1.2f);
        return true;
    }

    private Vector knockback(Player player, Location blast) {
        Vector away = blast.toVector().subtract(player.getLocation().toVector());
        if (away.lengthSquared() < 1.0E-6) {
            // Standing exactly on the blast: only the upward pop, no NaN direction.
            return new Vector(0, plugin.settings().fireballKnockY() * 1.5, 0);
        }
        away.normalize();
        // bedwars1058 points this vector at the blast and negates the config value.
        Vector horizontal = away.clone().multiply(-plugin.settings().fireballKnockX());
        double y = away.getY();
        if (y < 0) {
            y += 1.5;
        }
        if (y <= 0.5) {
            y = plugin.settings().fireballKnockY() * 1.5; // knockback when not jumping
        } else {
            y = y * plugin.settings().fireballKnockY() * 1.5; // knockback when jumping
        }
        return horizontal.setY(y);
    }

    private UUID ownerOf(Entity entity) {
        if (!entity.hasMetadata(OWNER) || entity.getMetadata(OWNER).isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(String.valueOf(entity.getMetadata(OWNER).get(0).value()));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}

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
import com.slop.pof.util.Text;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

public final class Fireballs {
    public static final String OWNER = "pof-owner";

    private final PoFPlugin plugin;
    private final Set<Fireball> live = new HashSet<>();
    private final java.util.Map<UUID, Long> cooldown = new java.util.HashMap<>();
    private final Set<UUID> customDamage = new HashSet<>();
    private boolean systemActive;

    public Fireballs(PoFPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean systemActive() {
        return systemActive;
    }

    public boolean isCustomDamage(UUID uuid) {
        return customDamage.contains(uuid);
    }

    public boolean isOurs(Entity entity) {
        return entity != null && entity.hasMetadata(OWNER);
    }

    public void clearOwner(Entity entity) {
        if (entity != null && entity.hasMetadata(OWNER)) {
            entity.removeMetadata(OWNER, plugin);
        }
    }

    public void tick() {
        if (live.isEmpty()) {
            return;
        }
        double accel = plugin.settings().fireballAccel();
        Iterator<Fireball> it = live.iterator();
        while (it.hasNext()) {
            Fireball fireball = it.next();
            if (fireball.isDead() || !fireball.isValid()) {
                it.remove();
                continue;
            }
            Vector velocity = fireball.getVelocity();
            if (velocity.lengthSquared() < 1.0E-6) {
                continue;
            }
            velocity.add(velocity.clone().normalize().multiply(accel));
            fireball.setVelocity(velocity);
        }
    }

    public void onHit(Fireball fireball) {
        live.remove(fireball);
        Location loc = fireball.getLocation();
        UUID owner = ownerOf(fireball);
        double radius = plugin.settings().fireballRadius();
        if (loc.getWorld() == null) {
            return;
        }
        for (Entity entity : loc.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
            if (!(entity instanceof Player player)) {
                continue;
            }
            if (!plugin.game().isAlive(player)) {
                continue;
            }
            if (player.getLocation().distanceSquared(loc) > radius * radius) {
                continue;
            }
            push(player, loc);
            double damage = owner != null && owner.equals(player.getUniqueId())
                    ? plugin.settings().fireballDamageSelf()
                    : plugin.settings().fireballDamageEnemy();
            if (damage <= 0) {
                continue;
            }
            Player shooter = owner == null ? null : plugin.getServer().getPlayer(owner);
            customDamage.add(player.getUniqueId());
            try {
                if (shooter != null) {
                    player.damage(damage, shooter);
                } else {
                    player.damage(damage);
                }
            } finally {
                customDamage.remove(player.getUniqueId());
            }
        }
    }

    public boolean tryShoot(Player player) {
        UUID uuid = player.getUniqueId();
        if (!plugin.game().isAlive(player)) {
            return false;
        }
        var arena = plugin.game().arena(player);
        if (arena == null || arena.state != com.slop.pof.arena.Arena.State.INGAME) {
            return false;
        }
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
        systemActive = true;
        Vector direction = player.getEyeLocation().getDirection().normalize();
        Fireball fireball = player.launchProjectile(Fireball.class);
        fireball.setVelocity(direction.multiply(plugin.settings().fireballSpeed()));
        fireball.setMetadata(OWNER, new FixedMetadataValue(plugin, uuid.toString()));
        live.add(fireball);
        Text.sound(player, plugin.settings().sound("fireball", "mob.ghast.fireball"), 1f, 1.2f);
        return true;
    }

    private void push(Player player, Location loc) {
        double dx = player.getLocation().getX() - loc.getX();
        double dz = player.getLocation().getZ() - loc.getZ();
        Vector velocity = player.getVelocity();
        double kx = plugin.settings().fireballKnockX();
        double ky = plugin.settings().fireballKnockY();
        if (dx > 0.1) {
            velocity.setX(kx);
        } else if (dx < -0.1) {
            velocity.setX(-kx);
        }
        if (dz > 0.1) {
            velocity.setZ(kx);
        } else if (dz < -0.1) {
            velocity.setZ(-kx);
        }
        velocity.setY(ky);
        player.setVelocity(velocity);
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

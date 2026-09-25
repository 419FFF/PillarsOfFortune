package com.slop.pof.lobby;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import com.slop.pof.PoFPlugin;

import java.io.File;
import java.io.IOException;

public final class LobbyStore {
    private final PoFPlugin plugin;
    private final File file;
    private String worldName;
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;
    private boolean saved;

    public LobbyStore(PoFPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
        load();
    }

    public Location resolve() {
        if (saved && worldName != null) {
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                return new Location(world, x, y, z, yaw, pitch);
            }
        }
        World lobby = Bukkit.getWorld(plugin.settings().lobbyWorld());
        if (lobby == null) {
            return null;
        }
        return lobby.getSpawnLocation();
    }

    public void save(Location location) {
        worldName = location.getWorld().getName();
        x = location.getX();
        y = location.getY();
        z = location.getZ();
        yaw = location.getYaw();
        pitch = location.getPitch();
        saved = true;
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("lobby.world", worldName);
        yaml.set("lobby.x", x);
        yaml.set("lobby.y", y);
        yaml.set("lobby.z", z);
        yaml.set("lobby.yaw", yaw);
        yaml.set("lobby.pitch", pitch);
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save lobby: " + e.getMessage());
        }
    }

    private void load() {
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        worldName = yaml.getString("lobby.world");
        if (worldName == null) {
            return;
        }
        x = yaml.getDouble("lobby.x");
        y = yaml.getDouble("lobby.y");
        z = yaml.getDouble("lobby.z");
        yaw = (float) yaml.getDouble("lobby.yaw");
        pitch = (float) yaml.getDouble("lobby.pitch");
        saved = true;
    }
}

package com.slop.pof.game;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import com.slop.pof.PoFPlugin;
import com.slop.pof.config.Settings;
import com.slop.pof.util.Rolls;
import com.slop.pof.util.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class Items {
    private static final Map<String, Material> ALIASES = new HashMap<>();

    static {
        alias("WOOD_SWORD", "WOODEN_SWORD", "WOOD_SWORD");
        alias("STONE_SWORD", "STONE_SWORD");
        alias("GOLD_SWORD", "GOLDEN_SWORD", "GOLD_SWORD");
        alias("IRON_SWORD", "IRON_SWORD");
        alias("DIAMOND_SWORD", "DIAMOND_SWORD");
        alias("WOOD_AXE", "WOODEN_AXE", "WOOD_AXE");
        alias("STONE_AXE", "STONE_AXE");
        alias("IRON_AXE", "IRON_AXE");
        alias("BOW", "BOW");
        alias("ARROW", "ARROW");
        alias("FISHING_ROD", "FISHING_ROD");
        alias("SNOW_BALL", "SNOWBALL", "SNOW_BALL");
        alias("STICK", "STICK");
        alias("LEATHER_HELMET", "LEATHER_HELMET");
        alias("LEATHER_CHESTPLATE", "LEATHER_CHESTPLATE");
        alias("LEATHER_LEGGINGS", "LEATHER_LEGGINGS");
        alias("LEATHER_BOOTS", "LEATHER_BOOTS");
        alias("GOLD_HELMET", "GOLDEN_HELMET", "GOLD_HELMET");
        alias("GOLD_CHESTPLATE", "GOLDEN_CHESTPLATE", "GOLD_CHESTPLATE");
        alias("GOLD_LEGGINGS", "GOLDEN_LEGGINGS", "GOLD_LEGGINGS");
        alias("GOLD_BOOTS", "GOLDEN_BOOTS", "GOLD_BOOTS");
        alias("CHAINMAIL_HELMET", "CHAIN_HELMET", "CHAINMAIL_HELMET");
        alias("CHAINMAIL_CHESTPLATE", "CHAIN_CHESTPLATE", "CHAINMAIL_CHESTPLATE");
        alias("CHAINMAIL_LEGGINGS", "CHAIN_LEGGINGS", "CHAINMAIL_LEGGINGS");
        alias("CHAINMAIL_BOOTS", "CHAIN_BOOTS", "CHAINMAIL_BOOTS");
        alias("IRON_HELMET", "IRON_HELMET");
        alias("IRON_CHESTPLATE", "IRON_CHESTPLATE");
        alias("IRON_LEGGINGS", "IRON_LEGGINGS");
        alias("IRON_BOOTS", "IRON_BOOTS");
        alias("DIAMOND_HELMET", "DIAMOND_HELMET");
        alias("DIAMOND_BOOTS", "DIAMOND_BOOTS");
        alias("COBBLESTONE", "COBBLE", "COBBLESTONE");
        alias("STONE", "STONE");
        alias("DIRT", "DIRT");
        alias("LOG", "LOG");
        alias("WOOD", "PLANK", "PLANKS", "WOODEN_PLANK", "WOOD");
        alias("GLASS", "GLASS");
        alias("WOOL", "WOOL");
        alias("SAND", "SAND");
        alias("GRAVEL", "GRAVEL");
        alias("WEB", "COBWEB", "WEB");
        alias("SOUL_SAND", "SOULSAND", "SOUL_SAND");
        alias("ICE", "ICE");
        alias("PACKED_ICE", "PACKED_ICE");
        alias("SLIME_BLOCK", "SLIME", "SLIME_BLOCK");
        alias("LADDER", "LADDER");
        alias("FENCE", "FENCE");
        alias("GLOWSTONE", "GLOWSTONE");
        alias("NETHERRACK", "NETHERRACK");
        alias("BRICK", "BRICKS", "BRICK");
        alias("SANDSTONE", "SANDSTONE");
        alias("CLAY", "CLAY");
        alias("SNOW_BLOCK", "SNOWBLOCK", "SNOW_BLOCK");
        alias("SMOOTH_BRICK", "STONE_BRICK", "STONEBRICK", "SMOOTH_BRICK");
        alias("ENDER_STONE", "END_STONE", "ENDSTONE", "ENDER_STONE");
        alias("TNT", "TNT");
        alias("FLINT_AND_STEEL", "FLINT_AND_STEEL");
        alias("FIREBALL", "FIRE_CHARGE", "FIRECHARGE", "FIREBALL");
        alias("LAVA_BUCKET", "LAVA_BUCKET");
        alias("WATER_BUCKET", "WATER_BUCKET");
        alias("ANVIL", "ANVIL");
        alias("ENDER_PEARL", "ENDERPEARL", "ENDER_PEARL");
        alias("SHEARS", "SHEARS");
        alias("OBSIDIAN", "OBSIDIAN");
        alias("NETHER_STAR", "NETHER_STAR");
        alias("BARRIER", "BARRIER");
        alias("DIAMOND", "DIAMOND");
        alias("BEDROCK", "BEDROCK");
        alias("COMMAND", "COMMAND_BLOCK", "COMMAND");
        alias("COMMAND_MINECART", "COMMAND_MINECART");
        alias("MOB_SPAWNER", "SPAWNER", "MOB_SPAWNER");
        alias("ENDER_PORTAL_FRAME", "PORTAL_FRAME", "ENDER_PORTAL_FRAME");
        alias("ENDER_PORTAL", "ENDER_PORTAL");
        alias("PORTAL", "PORTAL");
    }

    private final PoFPlugin plugin;

    public Items(PoFPlugin plugin) {
        this.plugin = plugin;
    }

    public ItemStack joinItem() {
        return named(material(plugin.settings().joinMaterial(), Material.NETHER_STAR), plugin.settings().joinName());
    }

    public ItemStack leaveItem() {
        return named(material(plugin.settings().leaveMaterial(), Material.BARRIER), plugin.settings().leaveName());
    }

    public ItemStack vipItem() {
        return named(material(plugin.settings().vipMaterial(), Material.DIAMOND), plugin.settings().vipName());
    }

    public void giveLobby(Player player) {
        PlayerInventory inv = player.getInventory();
        inv.setItem(0, joinItem());
        inv.setItem(4, null);
        inv.setItem(8, null);
        removeNamed(player, plugin.settings().vipName());
        player.updateInventory();
    }

    public void giveQueued(Player player) {
        PlayerInventory inv = player.getInventory();
        inv.setItem(0, null);
        removeNamed(player, plugin.settings().joinName());
        inv.setItem(8, leaveItem());
        if (player.hasPermission(com.slop.pof.Perms.VIP)) {
            inv.setItem(4, vipItem());
        } else {
            inv.setItem(4, null);
            removeNamed(player, plugin.settings().vipName());
        }
        player.updateInventory();
    }

    public boolean isJoin(ItemStack item) {
        return namedLike(item, plugin.settings().joinName());
    }

    public boolean isLeave(ItemStack item) {
        return namedLike(item, plugin.settings().leaveName());
    }

    public boolean isVip(ItemStack item) {
        return namedLike(item, plugin.settings().vipName());
    }

    public boolean isLobbyItem(ItemStack item) {
        return isJoin(item) || isLeave(item) || isVip(item);
    }

    public boolean isIllegal(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        if (isLobbyItem(item)) {
            return false;
        }
        Material type = item.getType();
        for (String raw : plugin.settings().illegal()) {
            Material illegal = material(raw, null);
            if (illegal != null && illegal == type) {
                return true;
            }
        }
        String name = type.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return containsIllegal(name);
    }

    public void strip(Player player) {
        PlayerInventory inv = player.getInventory();
        ItemStack[] contents = inv.getContents();
        for (int i = 0; i < contents.length; i++) {
            if (isIllegal(contents[i])) {
                inv.setItem(i, null);
            }
        }
        if (isIllegal(inv.getHelmet())) {
            inv.setHelmet(null);
        }
        if (isIllegal(inv.getChestplate())) {
            inv.setChestplate(null);
        }
        if (isIllegal(inv.getLeggings())) {
            inv.setLeggings(null);
        }
        if (isIllegal(inv.getBoots())) {
            inv.setBoots(null);
        }
    }

    public void stripAll() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            strip(player);
        }
    }

    public void giveRandom(Player player) {
        if (!plugin.game().isAlive(player)) {
            return;
        }
        Settings settings = plugin.settings();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int roll = 1 + random.nextInt(100);
        int pool = Rolls.poolIndex(roll, settings.rateWeapons(), settings.rateArmor(), settings.rateBlocks());
        List<Spec> specs = switch (pool) {
            case 0 -> parse(settings.weapons());
            case 1 -> parse(settings.armor());
            case 2 -> parse(settings.blocks());
            default -> parse(settings.chaos());
        };
        if (specs.isEmpty()) {
            specs = parse(settings.blocks());
        }
        if (specs.isEmpty()) {
            return;
        }
        Spec spec = specs.get(random.nextInt(specs.size()));
        if (isIllegal(new ItemStack(spec.material))) {
            return;
        }
        int amount = 1;
        if (pool == 2) {
            amount = Rolls.blockAmount(1 + random.nextInt(100), settings.blockMin(), settings.blockMax());
        }
        if (spec.material == Material.OBSIDIAN) {
            amount = 1 + random.nextInt(2);
        }
        if (spec.material == Material.ARROW || spec.material == Material.SNOW_BALL) {
            amount = 1 + random.nextInt(2);
        }
        if (spec.material == Material.TNT) {
            amount = 1;
        }
        if (spec.material == Material.ENDER_PEARL) {
            if (random.nextInt(100) >= settings.pearlChance()) {
                return;
            }
            amount = 1;
        }
        if (spec.enchant != null && random.nextInt(100) >= settings.enchantKeepChance()) {
            if (pool == 3) {
                spec = new Spec(Material.COBBLESTONE, null, 0);
                amount = Rolls.blockAmount(1 + random.nextInt(100), settings.blockMin(), settings.blockMax());
            } else if (pool == 0) {
                spec = new Spec(Material.WOOD_SWORD, null, 0);
                amount = 1;
            } else {
                spec = new Spec(Material.LEATHER_HELMET, null, 0);
                amount = 1;
            }
        }
        ItemStack stack = new ItemStack(spec.material, Math.max(1, amount));
        if (spec.enchant != null) {
            stack.addUnsafeEnchantment(spec.enchant, Math.max(1, spec.level));
        }
        player.getInventory().addItem(stack);
        if (spec.material == Material.FISHING_ROD) {
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && item.getType() == Material.FISHING_ROD) {
                    item.setDurability((short) 64);
                }
            }
        }
        plugin.database().addItem(player.getUniqueId());
        strip(player);
    }

    private static boolean containsIllegal(String name) {
        return name.contains("bedrock")
                || name.contains("barrier")
                || name.contains("command block")
                || name.contains("command minecart")
                || name.contains("minecart with command")
                || name.contains("spawner")
                || name.contains("portal frame")
                || name.contains("ender portal")
                || name.contains("end portal");
    }

    private void removeNamed(Player player, String configName) {
        PlayerInventory inv = player.getInventory();
        ItemStack[] contents = inv.getContents();
        for (int i = 0; i < contents.length; i++) {
            if (namedLike(contents[i], configName)) {
                inv.setItem(i, null);
            }
        }
    }

    private static boolean namedLike(ItemStack item, String configName) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return false;
        }
        String have = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        String want = Text.strip(configName);
        return have != null && want != null && (have.equals(want) || have.contains(want));
    }

    private static ItemStack named(Material material, String name) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Text.color(name));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private List<Spec> parse(List<String> raw) {
        List<Spec> specs = new ArrayList<>();
        for (String line : raw) {
            Spec spec = parseOne(line);
            if (spec != null) {
                specs.add(spec);
            }
        }
        return specs;
    }

    private Spec parseOne(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String body = raw.trim();
        Enchantment enchant = null;
        int level = 0;
        int bar = body.indexOf('|');
        if (bar >= 0) {
            String extra = body.substring(bar + 1).trim();
            body = body.substring(0, bar).trim();
            String[] parts = extra.split(":");
            enchant = Enchantment.getByName(parts[0].trim().toUpperCase(Locale.ROOT));
            if (parts.length > 1) {
                try {
                    level = Integer.parseInt(parts[1].trim());
                } catch (NumberFormatException ignored) {
                    level = 1;
                }
            } else {
                level = 1;
            }
        }
        Material material = material(body, null);
        if (material == null || material == Material.AIR) {
            plugin.getLogger().warning("Unknown item in pool: " + raw);
            return null;
        }
        return new Spec(material, enchant, level);
    }

    static Material material(String raw, Material fallback) {
        if (raw == null) {
            return fallback;
        }
        String key = raw.trim().toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
        Material alias = ALIASES.get(key);
        if (alias != null) {
            return alias;
        }
        Material matched = Material.matchMaterial(key);
        return matched == null ? fallback : matched;
    }

    private static void alias(String material, String... names) {
        Material value = Material.valueOf(material);
        for (String name : names) {
            ALIASES.put(name, value);
        }
    }

    private record Spec(Material material, Enchantment enchant, int level) {
    }
}

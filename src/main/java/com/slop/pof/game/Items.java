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
import com.slop.pof.util.Amounts;
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
        String materialKey = spec.material.name();
        List<EnchantPair> enchants = spec.enchants.isEmpty()
                ? parseEnchants(settings.enchantsFor(materialKey))
                : spec.enchants;
        int chance = spec.chance >= 0
                ? spec.chance
                : settings.chanceFor(materialKey, spec.material == Material.ENDER_PEARL ? settings.pearlChance() : 100);
        if (random.nextInt(100) >= chance) {
            return;
        }
        int amount = amountFor(spec, pool, settings, random);
        int keep = spec.keep >= 0 ? spec.keep : settings.enchantKeepFor(materialKey);
        if (!enchants.isEmpty() && random.nextInt(100) >= keep) {
            Material replacement = fallback(pool, settings);
            spec = new Spec(replacement, List.of(), null, -1, -1);
            enchants = List.of();
            amount = amountFor(spec, pool, settings, random);
        }
        ItemStack stack = new ItemStack(spec.material, Math.max(1, Math.min(2304, amount)));
        for (EnchantPair enchant : enchants) {
            stack.addUnsafeEnchantment(enchant.enchant, Math.max(1, enchant.level));
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

    private int amountFor(Spec spec, int pool, Settings settings, ThreadLocalRandom random) {
        if (spec.amountSpec != null && !spec.amountSpec.isBlank()) {
            return Amounts.roll(spec.amountSpec, random);
        }
        String mapped = settings.amountSpec(spec.material.name());
        if (mapped != null && !mapped.isBlank()) {
            return Amounts.roll(mapped, random);
        }
        if (pool == 2 && isLog(spec.material)) {
            int logMax = Math.max(1, settings.blockMin() - 1);
            return Rolls.blockAmount(1 + random.nextInt(100), 1, logMax);
        }
        if (pool == 2) {
            return Rolls.blockAmount(1 + random.nextInt(100), settings.blockMin(), settings.blockMax());
        }
        return settings.defaultAmount();
    }

    private static boolean isLog(Material material) {
        return material == Material.LOG || material == Material.LOG_2;
    }

    private Material fallback(int pool, Settings settings) {
        String raw = switch (pool) {
            case 0 -> settings.fallbackWeapons();
            case 1 -> settings.fallbackArmor();
            case 2 -> settings.fallbackBlocks();
            default -> settings.fallbackChaos();
        };
        Material material = material(raw, null);
        if (material == null) {
            return pool == 0 ? Material.WOOD_SWORD : pool == 1 ? Material.LEATHER_HELMET : Material.COBBLESTONE;
        }
        return material;
    }

    private Spec parseOne(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String[] tokens = raw.trim().split("\\|");
        Material type = material(tokens[0], null);
        if (type == null || type == Material.AIR) {
            plugin.getLogger().warning("Unknown item in pool: " + raw);
            return null;
        }
        List<EnchantPair> enchants = new ArrayList<>();
        String amount = null;
        int chance = -1;
        int keep = -1;
        for (int i = 1; i < tokens.length; i++) {
            String token = tokens[i].trim();
            if (token.isEmpty()) {
                continue;
            }
            String lower = token.toLowerCase(Locale.ROOT);
            if (lower.startsWith("amount:") || lower.startsWith("amt:")) {
                amount = token.substring(token.indexOf(':') + 1).trim();
            } else if (lower.startsWith("chance:")) {
                chance = parseInt(token.substring(token.indexOf(':') + 1), 100);
            } else if (lower.startsWith("keep:")) {
                keep = parseInt(token.substring(token.indexOf(':') + 1), 100);
            } else {
                enchants.addAll(parseEnchants(List.of(token)));
            }
        }
        return new Spec(type, enchants, amount, chance, keep);
    }

    private List<EnchantPair> parseEnchants(List<String> raw) {
        List<EnchantPair> enchants = new ArrayList<>();
        for (String entry : raw) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            for (String piece : entry.split(",")) {
                String[] parts = piece.trim().split(":");
                if (parts.length == 0 || parts[0].isBlank()) {
                    continue;
                }
                Enchantment enchant = Enchantment.getByName(parts[0].trim().toUpperCase(Locale.ROOT));
                if (enchant == null) {
                    plugin.getLogger().warning("Unknown enchantment: " + piece);
                    continue;
                }
                int level = parts.length > 1 ? parseInt(parts[1], 1) : 1;
                enchants.add(new EnchantPair(enchant, level));
            }
        }
        return enchants;
    }

    private static int parseInt(String raw, int fallback) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    public static Material material(String raw, Material fallback) {
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

    private record Spec(Material material, List<EnchantPair> enchants, String amountSpec, int chance, int keep) {
    }

    private record EnchantPair(Enchantment enchant, int level) {
    }
}

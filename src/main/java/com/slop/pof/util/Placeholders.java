package com.slop.pof.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

/**
 * Fills LuckPerms prefixes and PlaceholderAPI tokens in a message that is about one player.
 * LuckPerms is read directly. PlaceholderAPI runs after that, so either plugin can supply the prefix.
 */
public final class Placeholders {
    private static boolean luckPermsFailed;
    private static boolean papiFailed;

    private Placeholders() {
    }

    public static String apply(Player player, String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        if (player == null) {
            return text.replace("{luckperms_prefix}", "")
                    .replace("{luckperms_suffix}", "")
                    .replace("%luckperms_prefix%", "")
                    .replace("%luckperms_suffix%", "");
        }
        String prefix = meta(player, "getPrefix");
        String suffix = meta(player, "getSuffix");
        String resolved = text;
        if (prefix != null) {
            resolved = resolved.replace("{luckperms_prefix}", prefix).replace("%luckperms_prefix%", prefix);
        }
        if (suffix != null) {
            resolved = resolved.replace("{luckperms_suffix}", suffix).replace("%luckperms_suffix%", suffix);
        }
        resolved = resolved.replace("{luckperms_prefix}", "").replace("{luckperms_suffix}", "");
        resolved = Text.color(resolved);
        return papi(player, resolved);
    }

    private static String papi(Player player, String text) {
        if (papiFailed || text.indexOf('%') < 0) {
            return text;
        }
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return text;
        }
        try {
            return PapiBridge.apply(player, text);
        } catch (Throwable ex) {
            papiFailed = true;
            return text;
        }
    }

    private static String meta(Player player, String method) {
        if (luckPermsFailed || Bukkit.getPluginManager().getPlugin("LuckPerms") == null) {
            return null;
        }
        try {
            Class<?> provider = Class.forName("net.luckperms.api.LuckPermsProvider");
            Object api = provider.getMethod("get").invoke(null);
            Object adapter = api.getClass().getMethod("getPlayerAdapter", Class.class).invoke(api, Player.class);
            Object user = invokeOne(adapter, "getUser", player);
            if (user == null) {
                return "";
            }
            Object cached = user.getClass().getMethod("getCachedData").invoke(user);
            Object meta = cached.getClass().getMethod("getMetaData").invoke(cached);
            Object value = meta.getClass().getMethod(method).invoke(meta);
            return value == null ? "" : String.valueOf(value);
        } catch (Throwable ex) {
            luckPermsFailed = true;
            return null;
        }
    }

    private static Object invokeOne(Object target, String name, Object arg) throws ReflectiveOperationException {
        for (Method method : target.getClass().getMethods()) {
            if (!method.getName().equals(name) || method.getParameterCount() != 1) {
                continue;
            }
            if (method.getParameterTypes()[0].isInstance(arg)) {
                return method.invoke(target, arg);
            }
        }
        throw new NoSuchMethodException(name);
    }

    /** Loaded only when PlaceholderAPI is installed. */
    private static final class PapiBridge {
        private PapiBridge() {
        }

        private static String apply(Player player, String text) {
            return me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, text);
        }
    }
}

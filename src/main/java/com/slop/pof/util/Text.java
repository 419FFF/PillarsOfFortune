package com.slop.pof.util;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public final class Text {
    private static String nms;
    private static boolean titleFailed;
    private static boolean barFailed;

    private Text() {
    }

    public static String color(String input) {
        if (input == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    public static String strip(String input) {
        return ChatColor.stripColor(color(input));
    }

    /**
     * Makes text bold. A colour code clears bold, so the bold code goes right after a leading
     * colour code, for example {@code &6Rush} becomes {@code &6&lRush}.
     */
    public static String bold(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        if (input.contains("&l") || input.contains("\u00A7l")) {
            return input;
        }
        if (input.length() >= 2 && input.charAt(0) == '&'
                && "0123456789abcdef".indexOf(Character.toLowerCase(input.charAt(1))) >= 0) {
            return input.substring(0, 2) + "&l" + input.substring(2);
        }
        return "&l" + input;
    }

    public static void title(Player player, String title, String subtitle, int seconds) {
        String t = color(title == null ? "" : title);
        String s = color(subtitle == null ? "" : subtitle);
        int stay = Math.max(10, seconds * 20);
        if (!sendNmsTitle(player, t, s, 0, stay, 5)) {
            player.sendTitle(t, s);
        }
    }

    public static void actionBar(Player player, String message) {
        sendActionBar(player, color(message));
    }

    public static void sound(Player player, String name, float volume, float pitch) {
        if (player == null || name == null || name.isEmpty()) {
            return;
        }
        player.playSound(player.getLocation(), name, volume, pitch);
    }

    private static String nms() {
        if (nms == null) {
            String pkg = Bukkit.getServer().getClass().getPackage().getName();
            nms = pkg.substring(pkg.lastIndexOf('.') + 1);
        }
        return nms;
    }

    private static Object chat(String text) throws Exception {
        Class<?> serializer = Class.forName("net.minecraft.server." + nms() + ".IChatBaseComponent$ChatSerializer");
        String escaped = text.replace("\\", "\\\\").replace("\"", "\\\"");
        Method a = serializer.getMethod("a", String.class);
        return a.invoke(null, "{\"text\":\"" + escaped + "\"}");
    }

    private static void sendPacket(Player player, Object packet) throws Exception {
        Object handle = player.getClass().getMethod("getHandle").invoke(player);
        Object connection = handle.getClass().getField("playerConnection").get(handle);
        Class<?> packetClass = Class.forName("net.minecraft.server." + nms() + ".Packet");
        connection.getClass().getMethod("sendPacket", packetClass).invoke(connection, packet);
    }

    private static boolean sendNmsTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if (titleFailed) {
            return false;
        }
        try {
            Class<?> actionClass = Class.forName("net.minecraft.server." + nms() + ".PacketPlayOutTitle$EnumTitleAction");
            Class<?> packetClass = Class.forName("net.minecraft.server." + nms() + ".PacketPlayOutTitle");
            Class<?> component = Class.forName("net.minecraft.server." + nms() + ".IChatBaseComponent");
            Object times = actionClass.getField("TIMES").get(null);
            Object titleAction = actionClass.getField("TITLE").get(null);
            Object subAction = actionClass.getField("SUBTITLE").get(null);
            Constructor<?> timing = packetClass.getConstructor(int.class, int.class, int.class);
            Constructor<?> text = packetClass.getConstructor(actionClass, component);
            sendPacket(player, timing.newInstance(fadeIn, stay, fadeOut));
            sendPacket(player, text.newInstance(titleAction, chat(title)));
            sendPacket(player, text.newInstance(subAction, chat(subtitle)));
            return true;
        } catch (Throwable ignored) {
            titleFailed = true;
            return false;
        }
    }

    private static void sendActionBar(Player player, String message) {
        if (barFailed) {
            return;
        }
        try {
            Class<?> component = Class.forName("net.minecraft.server." + nms() + ".IChatBaseComponent");
            Class<?> packetClass = Class.forName("net.minecraft.server." + nms() + ".PacketPlayOutChat");
            Constructor<?> ctor = packetClass.getConstructor(component, byte.class);
            sendPacket(player, ctor.newInstance(chat(message), (byte) 2));
        } catch (Throwable ignored) {
            barFailed = true;
        }
    }
}

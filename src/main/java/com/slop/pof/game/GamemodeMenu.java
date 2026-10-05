package com.slop.pof.game;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import com.slop.pof.PoFPlugin;
import com.slop.pof.config.Gamemode;
import com.slop.pof.config.Settings;
import com.slop.pof.util.Text;

import java.util.ArrayList;
import java.util.List;

/** Chest menu used only when more than one gamemode is enabled. */
public final class GamemodeMenu implements Listener {
    private final PoFPlugin plugin;

    public GamemodeMenu(PoFPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Settings settings = plugin.settings();
        List<Gamemode> modes = settings.enabledGamemodes();
        int size = Math.max(9, Math.min(54, ((modes.size() + 8) / 9) * 9));
        String title = trim(settings.text("gamemode-title"), 32);
        Holder holder = new Holder();
        Inventory inventory = Bukkit.createInventory(holder, size, title);
        holder.inventory = inventory;
        for (int i = 0; i < modes.size() && i < size; i++) {
            inventory.setItem(i, icon(settings, modes.get(i)));
        }
        player.openInventory(inventory);
    }

    private ItemStack icon(Settings settings, Gamemode mode) {
        ItemStack stack = new ItemStack(Items.material(mode.icon, org.bukkit.Material.NETHER_STAR));
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Text.color(Text.bold(mode.name)));
            List<String> lore = new ArrayList<>();
            for (String line : mode.description()) {
                lore.add(Text.color(line));
            }
            if (!mode.description().isEmpty()) {
                lore.add("");
            }
            lore.add(settings.text("gamemode-lore-players",
                    "min", String.valueOf(mode.minPlayers(settings)),
                    "max", String.valueOf(mode.maxPlayers(settings))));
            lore.add(settings.text("gamemode-lore-start", "seconds", String.valueOf(mode.countdown(settings))));
            if (mode.randomItems()) {
                lore.add(settings.text("gamemode-lore-items", "seconds", String.valueOf(mode.itemDelay(settings))));
            } else {
                lore.add(settings.text("gamemode-lore-kit"));
            }
            meta.setLore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!(event.getView().getTopInventory().getHolder() instanceof Holder)) {
            return;
        }
        event.setCancelled(true);
        int slot = event.getRawSlot();
        List<Gamemode> modes = plugin.settings().enabledGamemodes();
        if (slot < 0 || slot >= modes.size() || slot >= event.getView().getTopInventory().getSize()) {
            return;
        }
        Gamemode mode = modes.get(slot);
        player.closeInventory();
        plugin.game().joinMode(player, mode);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Holder) {
            event.setCancelled(true);
        }
    }

    private static String trim(String text, int max) {
        if (text.length() <= max) {
            return text;
        }
        return text.substring(0, max);
    }

    public static final class Holder implements InventoryHolder {
        private Inventory inventory;

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}

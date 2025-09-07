package com.chunksmith.soloSkies.gui;

import com.chunksmith.soloSkies.SoloSkies;
import com.chunksmith.soloSkies.store.PlayerStore;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.WeatherType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Objects;

public class SoloSkiesMenu implements Listener {
    private final SoloSkies plugin;
    private final PlayerStore store;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public SoloSkiesMenu(SoloSkies plugin, PlayerStore store) {
        this.plugin = plugin; this.store = store;
    }

    public void open(Player p) {
        // Use a String title (works on Spigot & Paper) + custom holder for reliable detection.
        Inventory inv = Bukkit.createInventory(new MenuHolder(), 27, "SoloSkies — Personal Time & Weather");

        // Filler
        ItemStack glass = make(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, glass);

        // Time section (left)
        inv.setItem(10, make(Material.SUNFLOWER, "§eDay", lore("Set personal time to day")));
        inv.setItem(11, make(Material.GOLD_BLOCK, "§6Noon", lore("Set personal time to noon")));
        inv.setItem(12, make(Material.BLACK_WOOL, "§8Night", lore("Set personal time to night")));
        inv.setItem(13, make(Material.OBSIDIAN, "§5Midnight", lore("Set personal time to midnight")));
        inv.setItem(19, make(Material.CLOCK, "§fReset Time", lore("Reset to server time")));

        // Weather section (right)
        inv.setItem(14, make(Material.LIGHT_BLUE_WOOL, "§bSun", lore("Clear weather")));
        inv.setItem(15, make(Material.BLUE_WOOL, "§9Rain", lore("Downfall only for you")));
        inv.setItem(16, make(Material.TRIDENT, "§3Thunder", lore("Emulated via downfall")));
        inv.setItem(25, make(Material.WATER_BUCKET, "§fReset Weather", lore("Reset to server weather")));

        // Toggles (bottom)
        boolean onJoin = plugin.getConfig().getBoolean("persist.reapply-on-join", true);
        boolean onWorld = plugin.getConfig().getBoolean("persist.reapply-on-world-change", true);
        inv.setItem(21, toggleItem(onJoin, "§aReapply on Join", "Toggle reapply personal settings on join"));
        inv.setItem(22, toggleItem(onWorld, "§aReapply on World Change", "Toggle reapply personal settings on world switch"));

        p.openInventory(inv);
    }

    private ItemStack make(Material mat, String name, List<String> lore) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            it.setItemMeta(meta);
        }
        return it;
    }
    private List<String> lore(String l1) { return List.of("§7" + l1); }

    private ItemStack toggleItem(boolean on, String title, String desc) {
        return make(on ? Material.LIME_DYE : Material.GRAY_DYE,
                (on ? "§a" : "§7") + title,
                List.of("§7" + desc, "§8Currently: " + (on ? "§aON" : "§7OFF")));
    }

    private ItemStack statusPane(boolean success, String title, String desc) {
        return make(success ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE,
                (success ? "§a" : "§c") + title,
                List.of("§7" + desc));
    }

    private void flashSlot(Player p, int slot, ItemStack flashItem, int ticks) {
        Inventory inv = p.getOpenInventory() != null ? p.getOpenInventory().getTopInventory() : null;
        if (inv == null) return;
        ItemStack old = inv.getItem(slot);
        inv.setItem(slot, flashItem);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (p.getOpenInventory() != null && p.getOpenInventory().getTopInventory().equals(inv)) {
                inv.setItem(slot, old);
            }
        }, ticks);
    }

    private void ok(Player p, String mmMsg) {
        p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.6f, 1.4f);
        String prefix = plugin.getConfig().getString("messages.prefix","<gray>[SoloSkies]</gray> ");
        p.sendMessage(mm.deserialize(prefix + mmMsg));
    }

    private void deny(Player p, Integer slotIfAny) {
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
        String prefix = plugin.getConfig().getString("messages.prefix","<gray>[SoloSkies]</gray> ");
        p.sendMessage(mm.deserialize(prefix + "<red>You don't have permission.</red>"));
        if (slotIfAny != null) {
            flashSlot(p, slotIfAny, statusPane(false, "Denied", "You lack permission"), 20);
        }
    }

    private void logAction(String msg) {
        if (!plugin.getConfig().getBoolean("logging.console-actions", true)) return;
        Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[SoloSkies] " + ChatColor.RESET + msg);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        Inventory top = e.getView().getTopInventory();
        if (!(top.getHolder() instanceof MenuHolder)) return;

        // only handle clicks in the menu itself, ignore bottom/player inv
        if (e.getClickedInventory() == null || e.getClickedInventory() != top) {
            return;
        }

        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;

        int slot = e.getSlot();
        boolean canTime = p.hasPermission("soloskies.ptime.self");
        boolean canWeather = p.hasPermission("soloskies.pweather.self");
        boolean canResetT = p.hasPermission("soloskies.ptime.reset");
        boolean canResetW = p.hasPermission("soloskies.pweather.reset");

        switch (clicked.getType()) {
            // Time
            case SUNFLOWER:
                if (!canTime) { deny(p, slot); return; }
                setTime(p, 1000, "day", slot);
                break;
            case GOLD_BLOCK:
                if (!canTime) { deny(p, slot); return; }
                setTime(p, 6000, "noon", slot);
                break;
            case BLACK_WOOL:
                if (!canTime) { deny(p, slot); return; }
                setTime(p, 13000, "night", slot);
                break;
            case OBSIDIAN:
                if (!canTime) { deny(p, slot); return; }
                setTime(p, 18000, "midnight", slot);
                break;
            case CLOCK:
                if (!canResetT) { deny(p, slot); return; }
                p.resetPlayerTime(); store.setTime(p.getUniqueId(), null);
                ok(p, "<yellow>Personal time reset.");
                flashSlot(p, slot, statusPane(true, "Time Reset", "Back to server time"), 20);
                logAction(p.getName() + " reset personal TIME in world '" + p.getWorld().getName() + "'");
                break;

            // Weather
            case LIGHT_BLUE_WOOL:
                if (!canWeather) { deny(p, slot); return; }
                setWeather(p, WeatherType.CLEAR, "sun", slot);
                break;
            case BLUE_WOOL:
                if (!canWeather) { deny(p, slot); return; }
                setWeather(p, WeatherType.DOWNFALL, "rain", slot);
                break;
            case TRIDENT:
                if (!canWeather) { deny(p, slot); return; }
                setWeather(p, WeatherType.DOWNFALL, "thunder", slot);
                break;
            case WATER_BUCKET:
                if (!canResetW) { deny(p, slot); return; }
                p.resetPlayerWeather(); store.setWeather(p.getUniqueId(), null);
                ok(p, "<yellow>Personal weather reset.");
                flashSlot(p, slot, statusPane(true, "Weather Reset", "Back to server weather"), 20);
                logAction(p.getName() + " reset personal WEATHER in world '" + p.getWorld().getName() + "'");
                break;

            // Toggles
            case LIME_DYE:
            case GRAY_DYE:
                if (slot == 21) {
                    boolean cur = plugin.getConfig().getBoolean("persist.reapply-on-join", true);
                    plugin.getConfig().set("persist.reapply-on-join", !cur);
                    plugin.saveConfig();
                    flashSlot(p, slot, statusPane(true, (!cur ? "Enabled" : "Disabled"), "Reapply on Join"), 20);
                    logAction(p.getName() + " toggled Reapply-on-Join to " + (!cur));
                    Bukkit.getScheduler().runTask(plugin, () -> open(p));
                } else if (slot == 22) {
                    boolean cur = plugin.getConfig().getBoolean("persist.reapply-on-world-change", true);
                    plugin.getConfig().set("persist.reapply-on-world-change", !cur);
                    plugin.saveConfig();
                    flashSlot(p, slot, statusPane(true, (!cur ? "Enabled" : "Disabled"), "Reapply on World Change"), 20);
                    logAction(p.getName() + " toggled Reapply-on-World-Change to " + (!cur));
                    Bukkit.getScheduler().runTask(plugin, () -> open(p));
                } else {
                    p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.2f);
                }
                break;

            default: break;
        }
    }

    private void setTime(Player p, long ticks, String label, int clickedSlot) {
        p.setPlayerTime(ticks, false);
        store.setTime(p.getUniqueId(), ticks);
        ok(p, "<green>Time set to <white>" + label + "</white>.");
        flashSlot(p, clickedSlot, statusPane(true, "Time: " + label, "Applied successfully"), 20);
        logAction(p.getName() + " set personal TIME to '" + label + "' (" + ticks + " ticks) in world '" + p.getWorld().getName() + "'");
    }

    private void setWeather(Player p, WeatherType w, String label, int clickedSlot) {
        p.setPlayerWeather(w);
        store.setWeather(p.getUniqueId(), w);
        ok(p, "<green>Weather set to <white>" + label + "</white>.");
        flashSlot(p, clickedSlot, statusPane(true, "Weather: " + label, "Applied successfully"), 20);
        logAction(p.getName() + " set personal WEATHER to '" + label + "' in world '" + p.getWorld().getName() + "'");
    }

    @EventHandler public void onClose(InventoryCloseEvent e) { /* reserved */ }
}
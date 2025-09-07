package com.chunksmith.soloSkies;

import com.chunksmith.soloSkies.commands.PTimeCommand;
import com.chunksmith.soloSkies.commands.PWeatherCommand;
import com.chunksmith.soloSkies.gui.SoloSkiesMenu;
import com.chunksmith.soloSkies.manager.TempOverrideManager;
import com.chunksmith.soloSkies.store.PlayerStore;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class SoloSkies extends JavaPlugin implements Listener {

    private PlayerStore store;
    private SoloSkiesMenu menu;
    private TempOverrideManager tempManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.store = new PlayerStore(this);
        this.tempManager = new TempOverrideManager(this, store);
        this.menu = new SoloSkiesMenu(this, store);

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(menu, this);

        if (getCommand("ptime") != null) {
            PTimeCommand pt = new PTimeCommand(this, store, tempManager);
            getCommand("ptime").setExecutor(pt);
            getCommand("ptime").setTabCompleter(pt);
        }
        if (getCommand("pweather") != null) {
            PWeatherCommand pw = new PWeatherCommand(this, store, tempManager);
            getCommand("pweather").setExecutor(pw);
            getCommand("pweather").setTabCompleter(pw);
        }
        if (getCommand("skies") != null) {
            getCommand("skies").setExecutor((sender, cmd, label, args) -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("Players only."); return true; }
                if (!p.hasPermission("soloskies.gui")) { sender.sendMessage("No permission."); return true; }
                menu.open(p);
                return true;
            });
            getCommand("skies").setTabCompleter((s, c, l, a) -> java.util.Collections.emptyList());
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            store.apply(p);
        }

        getLogger().info("SoloSkies enabled.");
    }

    @Override
    public void onDisable() {
        if (tempManager != null) tempManager.cancelAll();
        store.save();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (getConfig().getBoolean("persist.reapply-on-join", true)) {
            store.apply(e.getPlayer());
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent e) {
        if (getConfig().getBoolean("persist.reapply-on-world-change", true)) {
            store.apply(e.getPlayer());
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return super.onCommand(sender, command, label, args);
    }
}
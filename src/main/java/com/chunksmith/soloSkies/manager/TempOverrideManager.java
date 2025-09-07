package com.chunksmith.soloSkies.manager;

import com.chunksmith.soloSkies.SoloSkies;
import com.chunksmith.soloSkies.store.PlayerStore;
import com.chunksmith.soloSkies.util.Msg;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.WeatherType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TempOverrideManager {

    private final SoloSkies plugin;
    private final PlayerStore store;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final Msg msg;

    private final Map<UUID, BukkitTask> timeTasks = new HashMap<>();
    private final Map<UUID, BukkitTask> weatherTasks = new HashMap<>();

    public TempOverrideManager(SoloSkies plugin, PlayerStore store) {
        this.plugin = plugin;
        this.store = store;
        this.msg = new Msg(plugin);
    }

    /** Apply a temporary personal time. 'display' is a human label (e.g., "day" or "12000"). */
    public void scheduleTempTime(Player p, long absoluteTicks, String display, long durationTicks, String prettyDuration) {
        cancelTime(p.getUniqueId());
        p.setPlayerTime(absoluteTicks, false);

        String prefix = plugin.getConfig().getString("messages.prefix", "<gray>[SoloSkies]</gray> ");
        String chat = plugin.getConfig().getString("messages.temp-time-set",
                        "<green>Time set to <white><time></white> for <white><duration></white>.</green>")
                .replace("<time>", display).replace("<duration>", prettyDuration);
        p.sendMessage(mm.deserialize(prefix + chat));

        // Action bar pulse
        String ab = plugin.getConfig().getString("messages.action-time-set", "<white>Time:</white> <aqua><time></aqua>");
        msg.sendActionBar(p, ab, new String[][]{{"time", display}});

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player cur = Bukkit.getPlayer(p.getUniqueId());
            if (cur != null && cur.isOnline()) {
                store.apply(cur);
                String expire = plugin.getConfig().getString("messages.temp-time-expired",
                        "<yellow>Your temporary time expired. Restored.</yellow>");
                cur.sendMessage(mm.deserialize(prefix + expire));

                // Action bar on expiry
                String abx = plugin.getConfig().getString("messages.action-temp-expired", "<yellow>Temporary override ended</yellow>");
                msg.sendActionBar(cur, abx, null);
            }
            timeTasks.remove(p.getUniqueId());
        }, durationTicks);

        timeTasks.put(p.getUniqueId(), task);
    }

    /** Apply a temporary personal weather. 'display' is a label (e.g., "sun", "rain", "thunder"). */
    public void scheduleTempWeather(Player p, WeatherType wt, String display, long durationTicks, String prettyDuration) {
        cancelWeather(p.getUniqueId());
        p.setPlayerWeather(wt);

        String prefix = plugin.getConfig().getString("messages.prefix", "<gray>[SoloSkies]</gray> ");
        String chat = plugin.getConfig().getString("messages.temp-weather-set",
                        "<green>Weather set to <white><weather></white> for <white><duration></white>.</green>")
                .replace("<weather>", display).replace("<duration>", prettyDuration);
        p.sendMessage(mm.deserialize(prefix + chat));

        // Action bar pulse
        String ab = plugin.getConfig().getString("messages.action-weather-set", "<white>Weather:</white> <aqua><weather></aqua>");
        msg.sendActionBar(p, ab, new String[][]{{"weather", display}});

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player cur = Bukkit.getPlayer(p.getUniqueId());
            if (cur != null && cur.isOnline()) {
                store.apply(cur);
                String expire = plugin.getConfig().getString("messages.temp-weather-expired",
                        "<yellow>Your temporary weather expired. Restored.</yellow>");
                cur.sendMessage(mm.deserialize(prefix + expire));

                // Action bar on expiry
                String abx = plugin.getConfig().getString("messages.action-temp-expired", "<yellow>Temporary override ended</yellow>");
                msg.sendActionBar(cur, abx, null);
            }
            weatherTasks.remove(p.getUniqueId());
        }, durationTicks);

        weatherTasks.put(p.getUniqueId(), task);
    }

    public void cancelTime(UUID uuid) {
        BukkitTask t = timeTasks.remove(uuid);
        if (t != null) t.cancel();
    }

    public void cancelWeather(UUID uuid) {
        BukkitTask t = weatherTasks.remove(uuid);
        if (t != null) t.cancel();
    }

    public void cancelAll() {
        timeTasks.values().forEach(BukkitTask::cancel);
        weatherTasks.values().forEach(BukkitTask::cancel);
        timeTasks.clear();
        weatherTasks.clear();
    }
}
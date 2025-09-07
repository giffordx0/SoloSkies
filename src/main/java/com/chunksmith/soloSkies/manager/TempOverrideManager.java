package com.chunksmith.soloSkies.manager;

import com.chunksmith.soloSkies.SoloSkies;
import com.chunksmith.soloSkies.store.PlayerStore;
import com.chunksmith.soloSkies.util.Msg;
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

        msg.send(p, "temp-time-set", new String[][]{{"time", display}, {"duration", prettyDuration}});
        msg.sendActionBar(p, "action-time-set", new String[][]{{"time", display}});

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player cur = Bukkit.getPlayer(p.getUniqueId());
            if (cur != null && cur.isOnline()) {
                store.apply(cur);
                msg.send(cur, "temp-time-expired", null);
                msg.sendActionBar(cur, "action-temp-expired", null);
            }
            timeTasks.remove(p.getUniqueId());
        }, durationTicks);

        timeTasks.put(p.getUniqueId(), task);
    }

    /** Apply a temporary personal weather. 'display' is a label (e.g., "sun", "rain", "thunder"). */
    public void scheduleTempWeather(Player p, WeatherType wt, String display, long durationTicks, String prettyDuration) {
        cancelWeather(p.getUniqueId());
        p.setPlayerWeather(wt);

        msg.send(p, "temp-weather-set", new String[][]{{"weather", display}, {"duration", prettyDuration}});
        msg.sendActionBar(p, "action-weather-set", new String[][]{{"weather", display}});

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player cur = Bukkit.getPlayer(p.getUniqueId());
            if (cur != null && cur.isOnline()) {
                store.apply(cur);
                msg.send(cur, "temp-weather-expired", null);
                msg.sendActionBar(cur, "action-temp-expired", null);
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
package com.chunksmith.soloSkies.manager;

import com.chunksmith.soloSkies.SoloSkies;
import com.chunksmith.soloSkies.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles warm-up and cooldown logic for actions such as setting personal
 * time or weather. Warm-ups can be cancelled by movement and cooldowns
 * prevent spamming.
 */
public class ApplyService implements Listener {
    private final SoloSkies plugin;
    private final Msg msg;

    private static class Warmup {
        final BukkitTask task;
        final boolean time; // true = time, false = weather
        Warmup(BukkitTask task, boolean time) { this.task = task; this.time = time; }
    }

    private final Map<UUID, Warmup> warmups = new HashMap<>();
    private final Map<UUID, Long> cdTime = new HashMap<>();
    private final Map<UUID, Long> cdWeather = new HashMap<>();

    public ApplyService(SoloSkies plugin) {
        this.plugin = plugin;
        this.msg = new Msg(plugin);
    }

    public boolean applyTime(Player p, Runnable action) {
        return run(p, action, true);
    }

    public boolean applyWeather(Player p, Runnable action) {
        return run(p, action, false);
    }

    private boolean run(Player p, Runnable action, boolean time) {
        UUID id = p.getUniqueId();
        long now = System.currentTimeMillis();

        // Cooldown check
        if (!p.hasPermission("soloskies.bypass.cooldown")) {
            long cdMs = 1000L * (time ? plugin.getConfig().getInt("cooldown.time-seconds", 0)
                    : plugin.getConfig().getInt("cooldown.weather-seconds", 0));
            if (cdMs > 0) {
                Long until = (time ? cdTime : cdWeather).get(id);
                if (until != null && until > now) {
                    long remain = (until - now) / 1000L;
                    msg.send(p, "cooldown-active", new String[][]{{"seconds", String.valueOf(remain)}});
                    return false;
                }
            }
        }

        // Warm-up
        if (plugin.getConfig().getBoolean("warmup.enabled", false)
                && !p.hasPermission("soloskies.bypass.warmup")) {
            int sec = plugin.getConfig().getInt("warmup.seconds", 0);
            if (sec > 0) {
                cancelWarmup(id, false);
                msg.sendActionBar(p, "warmup-start", new String[][]{{"seconds", String.valueOf(sec)}});
                BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    warmups.remove(id);
                    action.run();
                    startCooldown(id, time);
                }, sec * 20L);
                warmups.put(id, new Warmup(task, time));
                return true;
            }
        }

        action.run();
        startCooldown(id, time);
        return true;
    }

    private void startCooldown(UUID id, boolean time) {
        if (Bukkit.getPlayer(id) != null && Bukkit.getPlayer(id).hasPermission("soloskies.bypass.cooldown")) return;
        long cdMs = 1000L * (time ? plugin.getConfig().getInt("cooldown.time-seconds", 0)
                : plugin.getConfig().getInt("cooldown.weather-seconds", 0));
        if (cdMs <= 0) return;
        long until = System.currentTimeMillis() + cdMs;
        if (time) cdTime.put(id, until); else cdWeather.put(id, until);
    }

    public void cancelAll() {
        warmups.values().forEach(w -> w.task.cancel());
        warmups.clear();
    }

    private void cancelWarmup(UUID id, boolean notify) {
        Warmup w = warmups.remove(id);
        if (w != null) {
            w.task.cancel();
            if (notify) {
                Player p = Bukkit.getPlayer(id);
                if (p != null) msg.send(p, "warmup-cancel", null);
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        if (!plugin.getConfig().getBoolean("warmup.cancel-on-move", true)) return;
        Warmup w = warmups.get(id);
        if (w == null) return;
        if (e.getFrom().getBlockX() != e.getTo().getBlockX()
                || e.getFrom().getBlockY() != e.getTo().getBlockY()
                || e.getFrom().getBlockZ() != e.getTo().getBlockZ()) {
            cancelWarmup(id, true);
        }
    }
}

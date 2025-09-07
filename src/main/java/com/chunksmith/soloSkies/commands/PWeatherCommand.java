package com.chunksmith.soloSkies.commands;

import com.chunksmith.soloSkies.SoloSkies;
import com.chunksmith.soloSkies.manager.TempOverrideManager;
import com.chunksmith.soloSkies.store.PlayerStore;
import com.chunksmith.soloSkies.util.DurationUtil;
import com.chunksmith.soloSkies.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.WeatherType;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class PWeatherCommand implements TabExecutor {
    private final SoloSkies plugin;
    private final PlayerStore store;
    private final TempOverrideManager temps;
    private final Msg msg;

    public PWeatherCommand(SoloSkies plugin, PlayerStore store, TempOverrideManager temps) {
        this.plugin = plugin; this.store = store; this.temps = temps; this.msg = new Msg(plugin);
    }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] a) {
        if (a.length == 0) { s.sendMessage("Usage: /pweather <sun|rain|thunder|reset> [duration] [player]"); return true; }

        ParsedTail tail = parseTail(a);

        Player target;
        if (tail.playerName != null) {
            if (!s.hasPermission("soloskies.pweather.others")) { deny(s); return true; }
            target = Bukkit.getPlayerExact(tail.playerName);
            if (target == null) { msg.send(s, plugin.getConfig().getString("messages.player-not-found"), null); return true; }
        } else {
            if (!(s instanceof Player)) { s.sendMessage("Console must specify a player."); return true; }
            if (!s.hasPermission("soloskies.pweather.self")) { deny(s); return true; }
            target = (Player) s;
        }

        String sub = a[0].toLowerCase();
        switch (sub) {
            case "sun":
            case "clear":
                if (tail.durationArg != null) {
                    Long dur = DurationUtil.parseToTicks(tail.durationArg);
                    if (dur == null) { s.sendMessage("Invalid duration. Use 10s, 5m, 2h, 1d."); return true; }
                    temps.scheduleTempWeather(target, WeatherType.CLEAR, "sun", dur, DurationUtil.pretty(tail.durationArg));
                    return true;
                }
                temps.cancelWeather(target.getUniqueId());
                target.setPlayerWeather(WeatherType.CLEAR);
                store.setWeather(target.getUniqueId(), WeatherType.CLEAR);
                sendSet(s, target, "sun");
                return true;
            case "rain":
                if (tail.durationArg != null) {
                    Long dur = DurationUtil.parseToTicks(tail.durationArg);
                    if (dur == null) { s.sendMessage("Invalid duration. Use 10s, 5m, 2h, 1d."); return true; }
                    temps.scheduleTempWeather(target, WeatherType.DOWNFALL, "rain", dur, DurationUtil.pretty(tail.durationArg));
                    return true;
                }
                temps.cancelWeather(target.getUniqueId());
                target.setPlayerWeather(WeatherType.DOWNFALL);
                store.setWeather(target.getUniqueId(), WeatherType.DOWNFALL);
                sendSet(s, target, "rain");
                return true;
            case "thunder":
                if (tail.durationArg != null) {
                    Long dur = DurationUtil.parseToTicks(tail.durationArg);
                    if (dur == null) { s.sendMessage("Invalid duration. Use 10s, 5m, 2h, 1d."); return true; }
                    temps.scheduleTempWeather(target, WeatherType.DOWNFALL, "thunder", dur, DurationUtil.pretty(tail.durationArg));
                    return true;
                }
                temps.cancelWeather(target.getUniqueId());
                target.setPlayerWeather(WeatherType.DOWNFALL);
                store.setWeather(target.getUniqueId(), WeatherType.DOWNFALL);
                sendSet(s, target, "thunder");
                return true;
            case "reset":
                if (!s.hasPermission("soloskies.pweather.reset")) { deny(s); return true; }
                temps.cancelWeather(target.getUniqueId());
                target.resetPlayerWeather();
                store.setWeather(target.getUniqueId(), null);
                msg.send(s, plugin.getConfig().getString("messages.reset-weather"), null);
                return true;
            default:
                s.sendMessage("Unknown subcommand.");
                return true;
        }
    }

    private void sendSet(CommandSender s, Player target, String w) {
        msg.send(s, plugin.getConfig().getString("messages.set-weather"),
                new String[][]{{"weather", w}});
        // action bar pulse
        String ab = plugin.getConfig().getString("messages.action-weather-set", "<white>Weather:</white> <aqua><weather></aqua>");
        msg.sendActionBar(target, ab, new String[][]{{"weather", w}});
    }

    private void deny(CommandSender s) {
        msg.send(s, plugin.getConfig().getString("messages.no-perms"), null);
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String l, String[] a) {
        List<String> out = new ArrayList<>();
        if (a.length == 1) {
            add(out, "sun","rain","thunder","reset");
        } else {
            add(out, "10s","30s","1m","5m","10m","1h");
            Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
        }
        return out;
    }
    private void add(List<String> l, String... v) { for (String s : v) l.add(s); }

    private ParsedTail parseTail(String[] a) {
        ParsedTail t = new ParsedTail();
        int last = a.length - 1;
        if (last >= 1 && DurationUtil.looksLikeDuration(a[last])) {
            t.durationArg = a[last];
            last--;
        }
        if (last >= 1) {
            String cand = a[last];
            Player p = Bukkit.getPlayerExact(cand);
            if (p != null) t.playerName = p.getName();
        }
        return t;
    }

    private static class ParsedTail {
        String durationArg;
        String playerName;
    }
}
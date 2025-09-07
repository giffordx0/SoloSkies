package com.chunksmith.soloSkies.commands;

import com.chunksmith.soloSkies.SoloSkies;
import com.chunksmith.soloSkies.manager.ApplyService;
import com.chunksmith.soloSkies.manager.TempOverrideManager;
import com.chunksmith.soloSkies.store.PlayerStore;
import com.chunksmith.soloSkies.util.DurationUtil;
import com.chunksmith.soloSkies.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class PTimeCommand implements TabExecutor {
    private final SoloSkies plugin;
    private final PlayerStore store;
    private final TempOverrideManager temps;
    private final ApplyService applyService;
    private final Msg msg;

    public PTimeCommand(SoloSkies plugin, PlayerStore store, TempOverrideManager temps, ApplyService applyService) {
        this.plugin = plugin; this.store = store; this.temps = temps; this.applyService = applyService; this.msg = new Msg(plugin);
    }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] a) {
        if (a.length == 0) {
            s.sendMessage("Usage: /ptime <day|night|noon|midnight|set <ticks>|add <ticks>|reset> [duration] [player]");
            return true;
        }

        boolean needsTicks = "set".equalsIgnoreCase(a[0]) || "add".equalsIgnoreCase(a[0]);
        ParsedTail tail = parseTail(needsTicks, a);

        Player target;
        if (tail.playerName != null) {
            if (!s.hasPermission("soloskies.ptime.others")) { deny(s); return true; }
            target = Bukkit.getPlayerExact(tail.playerName);
            if (target == null) { msg.send(s, "player-not-found", null); return true; }
        } else {
            if (!(s instanceof Player)) { s.sendMessage("Console must specify a player."); return true; }
            if (!s.hasPermission("soloskies.ptime.self")) { deny(s); return true; }
            target = (Player) s;
        }

        String sub = a[0].toLowerCase();
        switch (sub) {
            case "day":      return handlePreset(s, target, 1000, "day", tail.durationArg);
            case "noon":     return handlePreset(s, target, 6000, "noon", tail.durationArg);
            case "night":    return handlePreset(s, target, 13000, "night", tail.durationArg);
            case "midnight": return handlePreset(s, target, 18000, "midnight", tail.durationArg);
            case "reset":
                if (!s.hasPermission("soloskies.ptime.reset")) { deny(s); return true; }
                applyService.applyTime(target, () -> {
                    temps.cancelTime(target.getUniqueId());
                    target.resetPlayerTime();
                    store.setTime(target.getUniqueId(), null);
                    msg.send(s, "reset-time", null);
                });
                return true;
            case "set":
            case "add":
                if (a.length < 2) { s.sendMessage("Usage: /ptime " + sub + " <ticks> [duration] [player]"); return true; }
                long val;
                try { val = Long.parseLong(a[1]); } catch (NumberFormatException e) { s.sendMessage("Ticks must be a number."); return true; }

                if ("set".equals(sub)) {
                    if (tail.durationArg != null) {
                        Long durTicks = DurationUtil.parseToTicks(tail.durationArg);
                        if (durTicks == null) { s.sendMessage("Invalid duration. Use 10s, 5m, 2h, 1d."); return true; }
                        applyService.applyTime(target, () -> temps.scheduleTempTime(target, val, String.valueOf(val), durTicks, DurationUtil.pretty(tail.durationArg)));
                        return true;
                    } else {
                        long finalVal = val;
                        applyService.applyTime(target, () -> {
                            temps.cancelTime(target.getUniqueId());
                            target.setPlayerTime(finalVal, false);
                            store.setTime(target.getUniqueId(), finalVal);
                            msg.send(s, "set-time", new String[][]{{"time", String.valueOf(finalVal)}});
                            msg.sendActionBar(target, "action-time-set", new String[][]{{"time", String.valueOf(finalVal)}});
                        });
                        return true;
                    }
                } else {
                    long newTime = target.getPlayerTime() + val;
                    if (tail.durationArg != null) {
                        Long durTicks = DurationUtil.parseToTicks(tail.durationArg);
                        if (durTicks == null) { s.sendMessage("Invalid duration. Use 10s, 5m, 2h, 1d."); return true; }
                        applyService.applyTime(target, () -> temps.scheduleTempTime(target, newTime, String.valueOf(newTime), durTicks, DurationUtil.pretty(tail.durationArg)));
                        return true;
                    } else {
                        long finalNewTime = newTime;
                        applyService.applyTime(target, () -> {
                            temps.cancelTime(target.getUniqueId());
                            target.setPlayerTime(finalNewTime, false);
                            store.setTime(target.getUniqueId(), finalNewTime);
                            msg.send(s, "add-time", new String[][]{{"delta", String.valueOf(val)}, {"time", String.valueOf(finalNewTime)}});
                            msg.sendActionBar(target, "action-time-set", new String[][]{{"time", String.valueOf(finalNewTime)}});
                        });
                        return true;
                    }
                }
            default:
                s.sendMessage("Unknown subcommand.");
                return true;
        }
    }

    private boolean handlePreset(CommandSender s, Player t, long ticks, String label, String durationArg) {
        if (durationArg != null) {
            Long dur = DurationUtil.parseToTicks(durationArg);
            if (dur == null) { s.sendMessage("Invalid duration. Use 10s, 5m, 2h, 1d."); return true; }
            applyService.applyTime(t, () -> temps.scheduleTempTime(t, ticks, label, dur, DurationUtil.pretty(durationArg)));
            return true;
        }
        long finalTicks = ticks;
        applyService.applyTime(t, () -> {
            temps.cancelTime(t.getUniqueId());
            t.setPlayerTime(finalTicks, false);
            store.setTime(t.getUniqueId(), finalTicks);
            msg.send(s, "set-time", new String[][]{{"time", label}});
            msg.sendActionBar(t, "action-time-set", new String[][]{{"time", label}});
        });
        return true;
    }

    private void deny(CommandSender s) {
        msg.send(s, "no-perms", null);
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String l, String[] a) {
        List<String> out = new ArrayList<>();
        if (a.length == 1) {
            add(out, "day","noon","night","midnight","set","add","reset");
        } else if (a.length == 2 && ("set".equalsIgnoreCase(a[0]) || "add".equalsIgnoreCase(a[0]))) {
            out.add("<ticks>");
        } else {
            add(out, "10s","30s","1m","5m","10m","1h");
            Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
        }
        return out;
    }

    private void add(List<String> l, String... v) { for (String s : v) l.add(s); }

    private ParsedTail parseTail(boolean needsTicks, String[] a) {
        ParsedTail t = new ParsedTail();
        int last = a.length - 1;
        if (last >= 1 && DurationUtil.looksLikeDuration(a[last])) {
            t.durationArg = a[last];
            last--;
        }
        if (last >= (needsTicks ? 2 : 1)) {
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
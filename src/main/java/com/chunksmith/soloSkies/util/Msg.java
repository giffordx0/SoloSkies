package com.chunksmith.soloSkies.util;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class Msg {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private final FileConfiguration messages;
    private final String prefix;

    public Msg(JavaPlugin plugin) {
        String lang = plugin.getConfig().getString("language", "en");
        String fname = "messages_" + lang + ".yml";

        // ensure lang directory exists
        File langDir = new File(plugin.getDataFolder(), "lang");
        if (!langDir.exists()) langDir.mkdirs();

        File f = new File(langDir, fname);
        if (!f.exists()) {
            // copy default from jar resources/lang
            plugin.saveResource("lang/" + fname, false);
        }

        this.messages = YamlConfiguration.loadConfiguration(f);

        // support defaults (fallbacks) from inside jar
        InputStream defStream = plugin.getResource("lang/" + fname);
        if (defStream != null) {
            YamlConfiguration def = YamlConfiguration.loadConfiguration(new InputStreamReader(defStream, StandardCharsets.UTF_8));
            this.messages.setDefaults(def);
        }

        this.prefix = messages.getString("prefix", "<gray>[SoloSkies]</gray> ");
    }

    public String raw(String key) {
        return messages.getString(key);
    }

    public void send(CommandSender to, String key, String[][] placeholders) {
        String template = messages.getString(key, "<gray>-</gray>");
        String out = apply(template, placeholders);
        Component c = MM.deserialize(prefix + out);
        to.sendMessage(c);
    }

    public void sendActionBar(Player to, String key, String[][] placeholders) {
        String template = messages.getString(key);
        if (template == null) return;
        String out = apply(template, placeholders);
        to.sendActionBar(MM.deserialize(out));
    }

    private String apply(String template, String[][] placeholders) {
        String out = template;
        if (placeholders != null) {
            for (String[] ph : placeholders) {
                if (ph != null && ph.length >= 2) {
                    out = out.replace("<" + ph[0] + ">", ph[1]);
                }
            }
        }
        return out;
    }
}
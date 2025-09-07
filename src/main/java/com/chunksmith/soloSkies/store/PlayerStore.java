package com.chunksmith.soloSkies.store;

import com.chunksmith.soloSkies.SoloSkies;
import com.chunksmith.soloSkies.model.PlayerSettings;
import org.bukkit.WeatherType;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class PlayerStore {
    private final SoloSkies plugin;
    private final File file;
    private FileConfiguration data;

    public PlayerStore(SoloSkies plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "players.yml");
        if (!file.exists()) {
            try { file.getParentFile().mkdirs(); file.createNewFile(); } catch (IOException ignored) {}
        }
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    public PlayerSettings get(UUID uuid) {
        String base = "players." + uuid + ".";
        PlayerSettings s = new PlayerSettings();
        if (data.isSet(base + "time")) s.setTimeTicks(data.getLong(base + "time"));
        if (data.isSet(base + "weather")) s.setWeather(WeatherType.valueOf(data.getString(base + "weather")));
        return s;
    }

    public void setTime(UUID uuid, Long ticks) {
        String base = "players." + uuid + ".";
        if (ticks == null) data.set(base + "time", null);
        else data.set(base + "time", ticks);
        save();
    }

    public void setWeather(UUID uuid, WeatherType w) {
        String base = "players." + uuid + ".";
        if (w == null) data.set(base + "weather", null);
        else data.set(base + "weather", w.name());
        save();
    }

    public void apply(Player p) {
        PlayerSettings s = get(p.getUniqueId());
        if (s.hasTime()) p.setPlayerTime(s.getTimeTicks(), false); else p.resetPlayerTime();
        if (s.hasWeather()) p.setPlayerWeather(s.getWeather()); else p.resetPlayerWeather();
    }

    public void save() {
        try { data.save(file); } catch (IOException e) { plugin.getLogger().warning("Failed saving players.yml"); }
    }
}
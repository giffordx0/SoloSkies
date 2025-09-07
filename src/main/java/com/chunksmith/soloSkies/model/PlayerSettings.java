package com.chunksmith.soloSkies.model;

import org.bukkit.WeatherType;

public class PlayerSettings {
    private Long timeTicks;          // null means reset
    private WeatherType weather;     // null means reset

    public Long getTimeTicks() { return timeTicks; }
    public void setTimeTicks(Long t) { this.timeTicks = t; }

    public WeatherType getWeather() { return weather; }
    public void setWeather(WeatherType w) { this.weather = w; }

    public boolean hasTime() { return timeTicks != null; }
    public boolean hasWeather() { return weather != null; }
}
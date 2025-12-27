package com.kbdl.epicsmash;

import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {

    private final EpicSmash plugin;

    public ConfigManager(EpicSmash plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
    }

    public FileConfiguration getConfig() {
        return plugin.getConfig();
    }

    public double getFatigueThreshold() {
        return getConfig().getDouble("fatigue.threshold", 70.0);
    }

    public double getFatigueDecay() {
        return getConfig().getDouble("fatigue.decay-per-sec", 5.0);
    }

    public boolean isRagdollEnabled() {
        return getConfig().getBoolean("ragdoll.enabled", true);
    }

    public double getRagdollLaunchVelocity() {
        return getConfig().getDouble("ragdoll.launch-velocity", 0.8);
    }

    public double getSpaceHeight() {
        return getConfig().getDouble("ragdoll.space-height", 30.0);
    }

    public String getSpacePrizeCommand() {
        // Default changed to ID 384 for 1.8.8 compatibility as per request
        return getConfig().getString("rewards.space-prize.command", "give %player% 384 16");
    }

    public String getSpacePrizeMessage() {
        return getConfig().getString("rewards.space-prize.message", "&6НА ЛУНУ! &e+16 Бутыльков Опыта");
    }

    public String getRagdollName() {
        return getConfig().getString("messages.ragdoll-name", "&c☠");
    }

    public String getComboMessage() {
        return getConfig().getString("messages.combo", "&6&lКОМБО! &e+2 УРОНА");
    }
}

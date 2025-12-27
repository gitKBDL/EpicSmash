package com.kbdl.epicsmash;

import org.bukkit.plugin.java.JavaPlugin;

public class EpicSmash extends JavaPlugin {

    private FatigueManager fatigueManager;
    private ConfigManager configManager;
    private RagdollManager ragdollManager;

    @Override
    public void onEnable() {
        getLogger().info("EpicSmash запускается...");

        configManager = new ConfigManager(this);
        fatigueManager = new FatigueManager(this);
        ragdollManager = new RagdollManager(this, configManager);

        getServer().getPluginManager().registerEvents(new CombatListener(fatigueManager, ragdollManager, configManager),
                this);

        getLogger().info("EpicSmash включен!");
    }

    @Override
    public void onDisable() {
        getLogger().info("EpicSmash выключен!");
    }
}

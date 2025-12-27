package com.kbdl.epicsmash;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class RagdollManager {

    private final EpicSmash plugin;
    private final ConfigManager configManager;
    private final Map<UUID, LivingEntity> activeRagdolls = new HashMap<>(); // UUID -> Entity
    private final Map<UUID, Long> ragdollTimestamps = new HashMap<>(); // UUID -> Creation Time
    private final Map<UUID, Double> startY = new HashMap<>(); // UUID -> Start Y
    private final Map<UUID, UUID> lastHitter = new HashMap<>(); // Ragdoll UUID -> Player UUID

    public RagdollManager(EpicSmash plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        startTask();
    }

    public boolean isRagdoll(LivingEntity entity) {
        return activeRagdolls.containsKey(entity.getUniqueId());
    }

    public void startRagdoll(LivingEntity entity, Player killer) {
        if (activeRagdolls.containsKey(entity.getUniqueId()))
            return;

        // Reset state
        entity.setHealth(entity.getMaxHealth());
        // Translate to Russian
        entity.setCustomName(ChatColor.translateAlternateColorCodes('&', configManager.getRagdollName()));
        entity.setCustomNameVisible(true);

        // Disable AI-like movement
        entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Integer.MAX_VALUE, 255, false, false));
        entity.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, Integer.MAX_VALUE, 200, false, false));
        entity.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, Integer.MAX_VALUE, 255, false, false));

        // Launch initially
        entity.setVelocity(new Vector(0, 0.5, 0));

        UUID uuid = entity.getUniqueId();
        activeRagdolls.put(uuid, entity);
        ragdollTimestamps.put(uuid, System.currentTimeMillis());
        startY.put(uuid, entity.getLocation().getY());

        if (killer != null) {
            lastHitter.put(uuid, killer.getUniqueId());
        }
    }

    public void handleJuggleHit(LivingEntity ragdoll, Player hitter) {
        // Launch UP
        double velocity = configManager.getRagdollLaunchVelocity();
        Vector v = ragdoll.getVelocity();
        ragdoll.setVelocity(new Vector(v.getX(), velocity, v.getZ()));

        // Launch Player (Chase Mechanic)
        if (hitter.getLocation().getPitch() < -10) { // Looking up
            Vector pv = hitter.getVelocity();
            hitter.setVelocity(new Vector(pv.getX(), 0.9, pv.getZ()));
        }

        // Effects
        ragdoll.getWorld().playEffect(ragdoll.getLocation(), Effect.EXPLOSION_LARGE, 0);
        ragdoll.getWorld().playSound(ragdoll.getLocation(), Sound.IRONGOLEM_THROW, 1f, 1.5f);

        lastHitter.put(ragdoll.getUniqueId(), hitter.getUniqueId());
    }

    private void startTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                long maxDuration = configManager.getConfig().getInt("ragdoll.max-duration-seconds", 15) * 1000L;
                double spaceHeight = configManager.getSpaceHeight();

                // Safe iteration
                Iterator<Map.Entry<UUID, LivingEntity>> it = activeRagdolls.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<UUID, LivingEntity> entry = it.next();
                    UUID uuid = entry.getKey();
                    LivingEntity entity = entry.getValue();
                    Long creationTime = ragdollTimestamps.get(uuid);

                    // If entity invalid/removed/too old
                    if (entity == null || !entity.isValid() || entity.isDead()
                            || (creationTime != null && now - creationTime > maxDuration)) {
                        if (entity != null && entity.isValid()) {
                            entity.setHealth(0); // Die for real
                        }
                        it.remove();
                        cleanup(uuid);
                        continue;
                    }

                    // Space Check (Relative)
                    double originY = startY.containsKey(uuid) ? startY.get(uuid) : 0;
                    if (entity.getLocation().getY() >= (originY + spaceHeight)) {
                        rewardSpaceLaunch(entity);
                        entity.remove();
                        it.remove();
                        cleanup(uuid);
                    }
                }
            }
        }.runTaskTimer(plugin, 10L, 10L);
    }

    private void cleanup(UUID uuid) {
        // activeRagdolls.remove(uuid); // Handled by iterator
        ragdollTimestamps.remove(uuid);
        startY.remove(uuid);
        lastHitter.remove(uuid);
    }

    private void rewardSpaceLaunch(LivingEntity ragdoll) {
        UUID hitterId = lastHitter.get(ragdoll.getUniqueId());
        if (hitterId != null) {
            Player p = Bukkit.getPlayer(hitterId);
            if (p != null && p.isOnline()) {
                String cmd = configManager.getSpacePrizeCommand().replace("%player%", p.getName());
                String msg = configManager.getSpacePrizeMessage();

                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                p.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
                p.playSound(p.getLocation(), Sound.LEVEL_UP, 1f, 1f);
            }
        }
        // Big Explosion
        ragdoll.getWorld().createExplosion(ragdoll.getLocation(), 0F); // Sound/Visual only
        ragdoll.getWorld().playEffect(ragdoll.getLocation(), Effect.EXPLOSION_HUGE, 0);
    }
}

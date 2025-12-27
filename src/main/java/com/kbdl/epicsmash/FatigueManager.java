package com.kbdl.epicsmash;

import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FatigueManager {

    private final Map<UUID, Double> fatigueMap = new HashMap<>();
    private static final double MAX_FATIGUE = 100.0;
    private static final double DECAY_RATE = 5.0; // Fatigue decay per second

    // Track last hit to prevent instant decay
    private final Map<UUID, Long> lastHitTime = new HashMap<>();
    // Track who hit the entity last
    private final Map<UUID, UUID> lastAttacker = new HashMap<>();

    public FatigueManager(EpicSmash plugin) {
        // Run decay task every second (20 ticks)
        new BukkitRunnable() {
            @Override
            public void run() {
                decayAll();
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    public void addFatigue(LivingEntity entity, double amount) {
        UUID uuid = entity.getUniqueId();
        double current = fatigueMap.getOrDefault(uuid, 0.0);
        double check = current + amount;
        if (check > MAX_FATIGUE)
            check = MAX_FATIGUE;
        fatigueMap.put(uuid, check);
        lastHitTime.put(uuid, System.currentTimeMillis());
    }

    public double getFatigue(LivingEntity entity) {
        return fatigueMap.getOrDefault(entity.getUniqueId(), 0.0);
    }

    public void reduceFatigue(LivingEntity entity, double amount) {
        UUID uuid = entity.getUniqueId();
        if (!fatigueMap.containsKey(uuid))
            return;
        double current = fatigueMap.get(uuid);
        current -= amount;
        if (current < 0)
            current = 0;
        fatigueMap.put(uuid, current);
    }

    public void clear(LivingEntity entity) {
        UUID uuid = entity.getUniqueId();
        fatigueMap.remove(uuid);
        lastHitTime.remove(uuid);
        lastAttacker.remove(uuid);
    }

    public void setLastAttacker(UUID entityId, UUID attackerId) {
        lastAttacker.put(entityId, attackerId);
    }

    public UUID getLastAttacker(UUID entityId) {
        return lastAttacker.get(entityId);
    }

    private void decayAll() {
        long now = System.currentTimeMillis();
        for (UUID uuid : fatigueMap.keySet()) {
            // Don't decay if hit recently (e.g., within 2 seconds)
            if (lastHitTime.containsKey(uuid)) {
                if (now - lastHitTime.get(uuid) < 2000)
                    continue;
            }

            double current = fatigueMap.get(uuid);
            if (current > 0) {
                current -= DECAY_RATE;
                if (current < 0)
                    current = 0;
                fatigueMap.put(uuid, current);
            }
        }
    }
}

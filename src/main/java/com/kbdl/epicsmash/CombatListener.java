package com.kbdl.epicsmash;

import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class CombatListener implements Listener {

    private final ConfigManager configManager;
    private final FatigueManager fatigueManager;
    private final RagdollManager ragdollManager;
    private static final double FATIGUE_PER_HIT = 15.0;
    private static final double FATIGUE_THRESHOLD = 70.0;
    private static final double JUGGLE_VELOCITY_Y = 0.8;

    public CombatListener(FatigueManager fatigueManager, RagdollManager ragdollManager,
            ConfigManager configManager) {
        this.fatigueManager = fatigueManager;
        this.ragdollManager = ragdollManager;
        this.configManager = configManager;
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity))
            return;

        LivingEntity victim = (LivingEntity) event.getEntity();
        if (!(event.getDamager() instanceof Player))
            return;
        Player attacker = (Player) event.getDamager();

        // 1. Check Ragdoll Hit
        if (ragdollManager.isRagdoll(victim)) {
            event.setCancelled(true); // Don't damage ragdoll
            ragdollManager.handleJuggleHit(victim, attacker);
            return;
        }

        // 2. Track Attacker
        fatigueManager.setLastAttacker(victim.getUniqueId(), attacker.getUniqueId());

        // 3. Normal Fatigue Logic
        boolean isJuggled = victim.getLocation().getY() % 1 > 0.1 && !victim.isOnGround();

        fatigueManager.addFatigue(victim, FATIGUE_PER_HIT);
        double fatigue = fatigueManager.getFatigue(victim);

        if (fatigue > FATIGUE_THRESHOLD) {
            Vector currentVel = victim.getVelocity();
            victim.setVelocity(new Vector(currentVel.getX(), JUGGLE_VELOCITY_Y, currentVel.getZ()));
            victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 10, false, false));

            victim.getWorld().playSound(victim.getLocation(), org.bukkit.Sound.BAT_TAKEOFF, 1.0f, 1.2f);
            victim.getWorld().playEffect(victim.getLocation(), org.bukkit.Effect.EXPLOSION_LARGE, 0);

            if (isJuggled) {
                event.setDamage(event.getDamage() + 2.0);
                attacker.sendMessage(ChatColor.translateAlternateColorCodes('&', configManager.getComboMessage()));
            }
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAnyDamage(org.bukkit.event.entity.EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity))
            return;
        LivingEntity victim = (LivingEntity) event.getEntity();

        // 1. Ragdoll Immunity (Prevent double damage to ragdolls mostly handled in
        // onHit, but safety check)
        if (ragdollManager.isRagdoll(victim)) {
            event.setCancelled(true);
            return;
        }

        // 2. Fatal Check
        if (event.getFinalDamage() >= victim.getHealth()) {
            java.util.UUID attackerId = fatigueManager.getLastAttacker(victim.getUniqueId());
            if (attackerId != null) {
                // Verify attacker is online/valid
                Player attacker = org.bukkit.Bukkit.getPlayer(attackerId);
                if (attacker != null && attacker.isOnline()) {
                    event.setCancelled(true);
                    ragdollManager.startRagdoll(victim, attacker);
                    victim.getWorld().playSound(victim.getLocation(), org.bukkit.Sound.ZOMBIE_DEATH, 1f, 1f);
                }
            }
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        fatigueManager.clear(event.getEntity());
    }
}

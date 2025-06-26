package net.hyper.immortalcultivation.cultivation.realms;

import net.hyper.immortalcultivation.cultivation.IBodyCultivationData;
import net.hyper.immortalcultivation.cultivation.IQiCultivationData;
import net.hyper.immortalcultivation.registry.ICModCultivationRealms;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.List;

public class PerfectFoundationEstablishmentEarly extends CultivationRealm {

    // === Constants ===
    private static final String REALM_ID = "perfect_foundation_establishment_early";
    private static final int REALM_LEVEL = 12;
    private static final String CULTIVATION_STAGE = "Perfect Foundation Establishment";

    private static final long BASE_HEALTH = 300;
    private static final double BASE_HEALTH_REGEN = 0.35;
    private static final double BASE_ATTACK_DAMAGE = 4.0;
    private static final double BASE_MOVEMENT_SPEED = 1.25;
    private static final double BASE_JUMP_HEIGHT = 1.25;

    private static final long BASE_MAX_QI = 900;
    private static final double BASE_QI_REGEN_RATE = 9.0;

    private static final long BASE_MAX_CULTIVATION = 50_000;
    private static final double BASE_CULTIVATION_RATE = 0.1;

    // === Core Identification ===

    @Override
    public String getId() {
        return REALM_ID;
    }

    @Override
    public String getDisplayName() {
        return "Early Foundation Establishment";
    }

    @Override
    public int getRealmLevel() {
        return REALM_LEVEL;
    }

    @Override
    public String getCultivationStage() {
        return CULTIVATION_STAGE;
    }

    @Override
    public String getDescription() {
        return "The first step in true cultivation. Practitioners begin to sense and gather Qi, " +
            "slowly condensing it within their dantian. The body is tempered and enhanced beyond mortal limits.";
    }

    // === Base Physical Stats ===

    @Override
    public long getBaseHealth() {
        return BASE_HEALTH;
    }

    @Override
    public double getBaseHealthRegeneration() {
        return BASE_HEALTH_REGEN;
    }

    @Override
    public double getBaseAttackDamage() {
        return BASE_ATTACK_DAMAGE;
    }

    @Override
    public double getBaseMovementSpeed() {
        return BASE_MOVEMENT_SPEED;
    }

    @Override
    public double getBaseJumpHeight() {
        return BASE_JUMP_HEIGHT;
    }

    // === Cultivation Stats ===

    @Override
    public long getBaseMaxCultivation() {
        return BASE_MAX_CULTIVATION;
    }

    @Override
    public double getBaseCultivationRate() {
        return BASE_CULTIVATION_RATE;
    }

    // === Qi Stats ===

    @Override
    public long getBaseMaxQi() {
        return BASE_MAX_QI;
    }

    @Override
    public double getBaseQiRegenerationRate() {
        return BASE_QI_REGEN_RATE;
    }

    @Override
    public double getBaseCirculatingQiRegenerationRate() {
        return BASE_QI_REGEN_RATE * 9; // 9x rate when circulating
    }

    // === Realm Navigation ===

    @Override
    public List<CultivationRealm> getCultivationRealmGroup() {
        // All Qi Condensation layers belong together
        return List.of(
            this
        );
    }

    @Override
    public List<CultivationRealm> getPreviousCultivationRealms() {
        // Can only come from mortal realm
        return List.of(
            ICModCultivationRealms.QI_CONDENSATION_L10
        );
    }

    @Override
    public List<CultivationRealm> getNextCultivationRealms() {
        // Progress to the 2nd layer
        return List.of(ICModCultivationRealms.QI_CONDENSATION_L10);
    }

    // === Breakthrough Requirements ===

    @Override
    public boolean meetsAdditionalRequirements(Player player, IQiCultivationData qiData) {
        // No special requirements for progressing through Qi Condensation layers
        // Just need to accumulate enough cultivation
        return true;
    }

    @Override
    public boolean meetsAdditionalRequirements(Player player, IBodyCultivationData bodyData) {
        // Body cultivators can also progress through these realms
        // Their path is equally valid
        return true;
    }

    // === Abilities and Features ===

    @Override
    public List<String> getUnlockedAbilities() {
        return List.of(
            "qi_sensing",        // Can sense Qi in the environment
            "basic_meditation",  // Can meditate to cultivate faster
            "qi_circulation",    // Can circulate Qi for enhanced regeneration
            "minor_body_enhancement" // Slightly enhanced physical capabilities
        );
    }

    @Override
    public List<String> getRealmBenefits() {
        return List.of(
            "Access to Qi energy",
            "Enhanced physical capabilities",
            "Slow health regeneration",
            "Basic cultivation techniques",
            "Extended lifespan (120 years)"
        );
    }

    @Override
    public List<String> getAvailableTechniques() {
        return List.of(
            "basic_qi_gathering",     // Active Qi cultivation technique
            "minor_qi_burst",         // Simple offensive technique
            "body_reinforcement",     // Defensive technique
            "swift_step"              // Movement technique
        );
    }

    // === Qi Cultivation Events ===

    @Override
    public void onRealmAdvancement(Player player, IQiCultivationData qiData, @Nullable CultivationRealm previousRealm) {
        if (player instanceof ServerPlayer serverPlayer) {
            // Special message for first breakthrough from mortal
            if (previousRealm != null && previousRealm.getRealmLevel() == 0) {
                serverPlayer.sendSystemMessage(
                    Component.literal("Congratulations! You have stepped into the 2nd Layer of Qi Condensation!")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                );
            } else {
                serverPlayer.sendSystemMessage(
                    Component.literal("You have entered the 2nd Layer of Qi Condensation through Qi cultivation.")
                        .withStyle(ChatFormatting.GOLD)
                );
            }

            // Play advancement effects
            playAdvancementEffects(serverPlayer);
        }
    }

    @Override
    public void onRealmExit(Player player, IQiCultivationData qiData, @Nullable CultivationRealm nextRealm) {
        if (player instanceof ServerPlayer serverPlayer && nextRealm != null) {
            if (nextRealm.getRealmLevel() > this.getRealmLevel()) {
                serverPlayer.sendSystemMessage(
                    Component.literal("Your Qi condenses further as you prepare to advance...")
                        .withStyle(ChatFormatting.AQUA)
                );
            }
        }
    }

    @Override
    public void applyRealmModifiers(Player player, IQiCultivationData qiData) {
        // Could apply attribute modifiers or status effects here
        // For example: slight speed boost, jump boost, etc.
    }

    // === Body Cultivation Events ===

    @Override
    public void onRealmAdvancement(Player player, IBodyCultivationData bodyData, @Nullable CultivationRealm previousRealm) {
        if (player instanceof ServerPlayer serverPlayer) {
            // Special message for first breakthrough from mortal via body cultivation
            if (previousRealm != null && previousRealm.getRealmLevel() == 0) {
                serverPlayer.sendSystemMessage(
                    Component.literal("Congratulations! You have achieved the 2nd Layer of Qi Condensation through Body Tempering!")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                );
            } else {
                // Message for returning or other transitions
                serverPlayer.sendSystemMessage(
                    Component.literal("You have entered the 2nd Layer of Qi Condensation through Body cultivation.")
                        .withStyle(ChatFormatting.GOLD)
                );
            }

            // Play advancement effects with body-themed particles
            playBodyAdvancementEffects(serverPlayer);
        }
    }

    @Override
    public void onRealmExit(Player player, IBodyCultivationData bodyData, @Nullable CultivationRealm nextRealm) {
        if (player instanceof ServerPlayer serverPlayer && nextRealm != null) {
            if (nextRealm.getRealmLevel() > this.getRealmLevel()) {
                serverPlayer.sendSystemMessage(
                    Component.literal("Your body grows stronger as you prepare to advance...")
                        .withStyle(ChatFormatting.RED)
                );
            }
        }
    }

    @Override
    public void applyRealmModifiers(Player player, IBodyCultivationData bodyData) {
        // Body cultivation might provide different bonuses
        // For example: more health, damage resistance, strength
    }

    // === Visual Properties ===

    @Override
    public int getRealmColor() {
        return 0x87CEEB; // Sky blue for Qi Condensation
    }

    @Override
    public ParticleOptions getRealmParticle() {
        return ParticleTypes.ENCHANT;
    }

    // === Helper Methods ===

    private void playAdvancementEffects(ServerPlayer player) {
        // Sound effect
        player.level().playSound(
            null,
            player.blockPosition(),
            SoundEvents.PLAYER_LEVELUP,
            SoundSource.PLAYERS,
            1.0f,
            1.0f
        );

        // Particle effects for Qi breakthrough
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                ParticleTypes.ENCHANT,
                player.getX(),
                player.getY() + 1.0,
                player.getZ(),
                50,
                0.5,
                1.0,
                0.5,
                0.1
            );

            // Add some end rod particles for spiritual effect
            serverLevel.sendParticles(
                ParticleTypes.END_ROD,
                player.getX(),
                player.getY() + 1.0,
                player.getZ(),
                20,
                0.3,
                0.5,
                0.3,
                0.05
            );
        }
    }

    private void playBodyAdvancementEffects(ServerPlayer player) {
        // Sound effect (deeper tone for body cultivation)
        player.level().playSound(
            null,
            player.blockPosition(),
            SoundEvents.PLAYER_LEVELUP,
            SoundSource.PLAYERS,
            1.0f,
            0.8f
        );

        // Particle effects for Body breakthrough
        if (player.level() instanceof ServerLevel serverLevel) {
            // Flame particles for body tempering
            serverLevel.sendParticles(
                ParticleTypes.FLAME,
                player.getX(),
                player.getY() + 0.5,
                player.getZ(),
                30,
                0.3,
                0.5,
                0.3,
                0.05
            );

            // Some smoke for tempering effect
            serverLevel.sendParticles(
                ParticleTypes.SMOKE,
                player.getX(),
                player.getY() + 0.5,
                player.getZ(),
                20,
                0.3,
                0.5,
                0.3,
                0.02
            );
        }
    }
}
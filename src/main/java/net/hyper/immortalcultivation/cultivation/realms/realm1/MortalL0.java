package net.hyper.immortalcultivation.cultivation.realms.realm1;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.cultivation.IBodyCultivationData;
import net.hyper.immortalcultivation.cultivation.IQiCultivationData;
import net.hyper.immortalcultivation.cultivation.realms.CultivationRealm;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
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

public class MortalL0 extends CultivationRealm {

    // === Constants ===
    private static final String REALM_ID = "mortal";
    private static final int REALM_LEVEL = 0;
    private static final String CULTIVATION_STAGE = "Mortal";

    // Base stats - mortals have standard human capabilities
    private static final long BASE_HEALTH = 20;
    private static final double BASE_HEALTH_REGEN = 0.0;
    private static final double BASE_ATTACK_DAMAGE = 0.0;
    private static final double BASE_MOVEMENT_SPEED = 0.0;
    private static final double BASE_JUMP_HEIGHT = 0.0;

    // Qi stats - mortals cannot store or regenerate Qi
    private static final long BASE_MAX_QI = 0;
    private static final double BASE_QI_REGEN_RATE = 0.0;

    // Cultivation stats - mortals can begin accumulating cultivation
    private static final long BASE_MAX_CULTIVATION = 100;
    private static final double BASE_CULTIVATION_RATE = 0.005;

    // === Core Identification ===

    @Override
    public String getId() {
        return REALM_ID;
    }

    @Override
    public String getDisplayName() {
        return "Mortal";
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
        return "Merely a mortal, unaware of the vast cultivation world. " +
            "Through meditation and training, one can step onto the path of immortality.";
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
        return BASE_QI_REGEN_RATE; // Still 0 for mortals
    }

    // === Realm Navigation ===

    @Override
    public List<CultivationRealm> getCultivationRealmGroup() {
        // Mortal realm stands alone
        return List.of(this);
    }

    @Override
    public List<CultivationRealm> getPreviousCultivationRealms() {
        return List.of(); // This is the absolute starting realm
    }

    @Override
    public List<CultivationRealm> getNextCultivationRealms() {
        // From mortal, one can begin either Qi or Body cultivation
        // Both paths lead to the same first realm in this implementation
        return List.of(ICModCultivationRealms.QI_CONDENSATION_L1);
    }

    // === Breakthrough Requirements ===

    @Override
    public boolean meetsAdditionalRequirements(Player player, IQiCultivationData qiData) {
        // Mortals can always attempt to begin Qi cultivation
        // No special requirements beyond accumulating enough cultivation
        return true;
    }

    @Override
    public boolean meetsAdditionalRequirements(Player player, IBodyCultivationData bodyData) {
        // Mortals can always attempt to begin Body cultivation
        // No special requirements beyond accumulating enough cultivation
        return true;
    }

    // === Abilities and Features ===

    @Override
    public List<String> getUnlockedAbilities() {
        // Mortals have no cultivation abilities
        return List.of();
    }

    @Override
    public List<String> getRealmBenefits() {
        return List.of(
            "Potential for cultivation",
            "Can begin meditation practice",
            "Can sense the existence of Qi",
            "Can train the body beyond mortal limits"
        );
    }

    @Override
    public List<String> getAvailableTechniques() {
        // Basic techniques available to help begin cultivation
        return List.of(
            "mortal_meditation", // Very basic meditation to start accumulating cultivation
            "physical_training"  // Basic exercises to prepare the body
        );
    }

    // === Qi Cultivation Events ===

    @Override
    public void onRealmAdvancement(Player player, IQiCultivationData qiData, @Nullable CultivationRealm previousRealm) {
        // This should not be called for mortal realm as it's the starting point
        // But if it is, we'll handle it gracefully
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(
                Component.literal("You are a mortal, standing at the threshold of cultivation.")
                    .withStyle(ChatFormatting.GRAY)
            );
        }
    }

    @Override
    public void onRealmExit(Player player, IQiCultivationData qiData, @Nullable CultivationRealm nextRealm) {
        // Special message when leaving mortal realm through Qi cultivation
        if (player instanceof ServerPlayer serverPlayer && nextRealm != null) {
            serverPlayer.sendSystemMessage(
                Component.literal("You take your first step on the path of Qi cultivation!")
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
            );
            serverPlayer.sendSystemMessage(
                Component.literal("The mortal world fades as spiritual energy flows through your meridians.")
                    .withStyle(ChatFormatting.YELLOW)
            );

            // Play effects
            serverPlayer.level().playSound(
                null,
                serverPlayer.blockPosition(),
                SoundEvents.PLAYER_LEVELUP,
                SoundSource.PLAYERS,
                1.0f,
                0.8f
            );

            // Particle effects
            if (serverPlayer.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                    ParticleTypes.END_ROD,
                    serverPlayer.getX(),
                    serverPlayer.getY() + 1.0,
                    serverPlayer.getZ(),
                    30,
                    0.5,
                    1.0,
                    0.5,
                    0.05
                );
            }
        }
    }

    // === Body Cultivation Events ===

    @Override
    public void onRealmAdvancement(Player player, IBodyCultivationData bodyData, @Nullable CultivationRealm previousRealm) {
        // This should not be called for mortal realm as it's the starting point
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(
                Component.literal("You are a mortal, your body untouched by cultivation.")
                    .withStyle(ChatFormatting.GRAY)
            );
        }
    }

    @Override
    public void onRealmExit(Player player, IBodyCultivationData bodyData, @Nullable CultivationRealm nextRealm) {
        // Special message when leaving mortal realm through Body cultivation
        if (player instanceof ServerPlayer serverPlayer && nextRealm != null) {
            serverPlayer.sendSystemMessage(
                Component.literal("You take your first step on the path of Body cultivation!")
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
            );
            serverPlayer.sendSystemMessage(
                Component.literal("Your mortal flesh begins its transformation into something greater.")
                    .withStyle(ChatFormatting.YELLOW)
            );

            // Play effects
            serverPlayer.level().playSound(
                null,
                serverPlayer.blockPosition(),
                SoundEvents.PLAYER_LEVELUP,
                SoundSource.PLAYERS,
                1.0f,
                1.2f
            );

            // Particle effects
            if (serverPlayer.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                    ParticleTypes.FLAME,
                    serverPlayer.getX(),
                    serverPlayer.getY() + 1.0,
                    serverPlayer.getZ(),
                    30,
                    0.5,
                    1.0,
                    0.5,
                    0.05
                );
            }
        }
    }

    // === Visual Properties ===

    @Override
    public int getRealmColor() {
        return 0x808080; // Gray for mortals
    }

    @Override
    public ParticleOptions getRealmParticle() {
        return ParticleTypes.ASH; // Simple ash particles for mortals
    }

    // === Special Methods ===

    /**
     * Check if a player is ready to begin cultivation
     * @param player The player to check
     * @return true if they have accumulated enough cultivation to breakthrough
     */
    public boolean isReadyToCultivate(Player player) {
        // Check both Qi and Body cultivation progress
        try {
            IQiCultivationData qiData = player.getData(ICModAttachmentTypes.PLAYER_QI_CULTIVATION.get());
            IBodyCultivationData bodyData = player.getData(ICModAttachmentTypes.PLAYER_BODY_CULTIVATION.get());

            return (qiData != null && meetsQiCultivationThreshold(qiData)) ||
                (bodyData != null && meetsBodyCultivationThreshold(bodyData));
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error checking cultivation readiness", e);
            return false;
        }
    }

    /**
     * Get an inspirational message for mortals
     * @return Component with motivational text
     */
    public Component getInspirationMessage() {
        String[] messages = {
            "The path of cultivation begins with a single step.",
            "Even the mightiest immortal was once a mere mortal.",
            "Through meditation, the impossible becomes possible.",
            "Your journey to immortality awaits."
        };

        int index = (int)(Math.random() * messages.length);
        return Component.literal(messages[index])
            .withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY);
    }
}
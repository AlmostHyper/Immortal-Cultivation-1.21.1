package net.hyper.immortalcultivation.cultivation.realms.realm1;

import net.hyper.immortalcultivation.cultivation.IQiCultivationData;
import net.hyper.immortalcultivation.cultivation.realms.CultivationRealm;
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

    // Base stats
    private static final long BASE_HEALTH = 20;
    private static final double BASE_HEALTH_REGEN = 0.0;
    private static final double BASE_ATTACK_DAMAGE = 1.0;
    private static final double BASE_MOVEMENT_SPEED = 0.0;
    private static final double BASE_JUMP_HEIGHT = 0.0;

    // Qi stats
    private static final long BASE_MAX_QI = 0;
    private static final double BASE_QI_REGEN_RATE = 0.0;

    // Cultivation stats
    private static final long BASE_MAX_CULTIVATION = 100;
    private static final double BASE_CULTIVATION_RATE = 0.0;

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
        return "Merely a mortal. Continue down the Path to ascend.";
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

    // === Cultivation Stats === //

    @Override
    public long getBaseMaxCultivation() {return BASE_MAX_CULTIVATION;}

    @Override
    public double getBaseCultivationRate() {return BASE_CULTIVATION_RATE;}

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
    public double getBaseCirculatingQiRegenerationRate() {return BASE_QI_REGEN_RATE * 9;}

    // === Realm Navigation ===

    @Override
    public List<CultivationRealm> getCultivationRealmGroup() {
        // All Qi Condensation levels
        return List.of(
            ICModCultivationRealms.QI_CONDENSATION_L1,
            ICModCultivationRealms.QI_CONDENSATION_L2,
            ICModCultivationRealms.QI_CONDENSATION_L3
        );
    }

    @Override
    public List<CultivationRealm> getPreviousCultivationRealms() {
        return List.of(); // This is the starting realm
    }

    @Override
    public List<CultivationRealm> getNextCultivationRealms() {
        return List.of(
            ICModCultivationRealms.QI_CONDENSATION_L1);
    }

    // === Breakthrough Requirements ===

    @Override
    public boolean meetsAdditionalRequirements(Player player, IQiCultivationData cultivationData) {
        // First level has no special requirements beyond Qi
        return true;
    }

    // === Abilities and Features ===

    @Override
    public List<String> getUnlockedAbilities() {
        return List.of(
            "qi_sensing",
            "basic_meditation"
        );
    }

    @Override
    public List<String> getRealmBenefits() {
        return List.of(
            "Begin cultivation journey",
            "Enhanced physical capabilities",
            "Qi sensing ability",
            "Basic meditation techniques"
        );
    }

    @Override
    public List<String> getAvailableTechniques() {
        return List.of(
            "basic_breathing_technique",
            "qi_gathering_meditation"
        );
    }

    // === Events ===

    @Override
    public void onRealmAdvancement(Player player, IQiCultivationData cultivationData, @Nullable CultivationRealm previousRealm) {
        super.onRealmAdvancement(player, cultivationData, previousRealm);

        if (player instanceof ServerPlayer serverPlayer) {
            // Special message for first cultivation realm
            if (previousRealm == null) {
                serverPlayer.sendSystemMessage(
                    Component.literal("You have begun your cultivation journey!")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                );
                serverPlayer.sendSystemMessage(
                    Component.literal("The path of immortality lies before you.")
                        .withStyle(ChatFormatting.YELLOW)
                );
            }

            // Play advancement effects
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
                    ParticleTypes.ENCHANT,
                    serverPlayer.getX(),
                    serverPlayer.getY() + 1.0,
                    serverPlayer.getZ(),
                    20,
                    0.5,
                    0.5,
                    0.5,
                    0.1
                );
            }
        }
    }

    @Override
    public void applyRealmModifiers(Player player, IQiCultivationData qiData) {
        // This realm doesn't have special modifiers beyond base stats
        // Could add temporary effects, passive abilities, etc.
    }

    // === Visual Properties ===

    @Override
    public int getRealmColor() {
        return 0x87CEEB; // Sky blue for first level
    }

    @Override
    public ParticleOptions getRealmParticle() {
        return ParticleTypes.ENCHANT;
    }

    // === Special Methods ===

    /**
     * Check if this is a player's first cultivation realm
     * @param previousRealm The realm they came from
     * @return true if this is their first cultivation realm
     */
    public boolean isFirstCultivationRealm(@Nullable CultivationRealm previousRealm) {
        return previousRealm == null;
    }

    /**
     * Get welcome message for new cultivators
     * @return Welcome message component
     */
    public Component getWelcomeMessage() {
        return Component.literal("Welcome to the world of cultivation!")
            .withStyle(ChatFormatting.GOLD);
    }
}

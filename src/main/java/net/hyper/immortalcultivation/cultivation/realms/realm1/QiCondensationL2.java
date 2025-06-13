package net.hyper.immortalcultivation.cultivation.realms.realm1;

import net.hyper.immortalcultivation.cultivation.PlayerQiData;
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

public class QiCondensationL2 extends CultivationRealm {

    // === Constants ===
    private static final String REALM_ID = "qi_condensation_l2";
    private static final int REALM_LEVEL = 2;
    private static final String CULTIVATION_STAGE = "2nd Layer of Qi Condensation";

    // Base stats
    private static final long BASE_HEALTH = 30;
    private static final double BASE_HEALTH_REGEN = 0.015;
    private static final double BASE_ATTACK_DAMAGE = 1.2;
    private static final double BASE_MOVEMENT_SPEED = 0.1;
    private static final double BASE_JUMP_HEIGHT = 0.1;

    // Qi stats
    private static final long BASE_MAX_QI = 499;
    private static final double BASE_QI_REGEN_RATE = 0.015;

    // === Core Identification ===

    @Override
    public String getId() {
        return REALM_ID;
    }

    @Override
    public String getDisplayName() {
        return "2nd Layer of Qi Condensation";
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
        return "The first step in cultivation. Practitioners begin to sense and gather Qi, " +
            "slowly condensing it within their dantian. Physical capabilities are slightly enhanced.";
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
    public double getBaseAcceleratedQiRegenerationRate() {
        return BASE_QI_REGEN_RATE * 9;
    }

    // === Realm Navigation ===

    @Override
    public List<CultivationRealm> getCultivationRealmGroup() {
        // All Qi Condensation levels
        return List.of(
            this,
            ICModCultivationRealms.QI_CONDENSATION_L3
        );
    }

    @Override
    public List<CultivationRealm> getPreviousCultivationRealms() {
        return List.of(); // This is the starting realm
    }

    @Override
    public List<CultivationRealm> getNextCultivationRealms() {
        return List.of(ICModCultivationRealms.QI_CONDENSATION_L3);
    }

    // === Breakthrough Requirements ===

    @Override
    protected boolean meetsAdditionalRequirements(Player player, PlayerQiData qiData) {
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
    public void onRealmAdvancement(Player player, PlayerQiData qiData, @Nullable CultivationRealm previousRealm) {
        super.onRealmAdvancement(player, qiData, previousRealm);

        if (player instanceof ServerPlayer serverPlayer) {
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
    public void applyRealmModifiers(Player player, PlayerQiData qiData) {
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
}

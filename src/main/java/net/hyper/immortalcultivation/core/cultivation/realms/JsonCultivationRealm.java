package net.hyper.immortalcultivation.core.cultivation.realms;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.attachments.PlayerStatsAttachment;
import net.hyper.immortalcultivation.common.interfaces.BonusSource;
import net.hyper.immortalcultivation.common.interfaces.ICultivationData;
import net.hyper.immortalcultivation.common.interfaces.IStatsData;
import net.hyper.immortalcultivation.common.registry.ICModAttachmentTypes;
import net.hyper.immortalcultivation.common.registry.ICModCultivationRealms;
import net.hyper.immortalcultivation.server.events.EventManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * A data-driven implementation of CultivationRealm that loads its properties from JSON.
 * Updated to work with the unified ICultivationData interface.
 */
public class JsonCultivationRealm extends CultivationRealm {

    private final CultivationRealmData data;
    private final ParticleOptions particleOptions;

    public JsonCultivationRealm(CultivationRealmData data) {
        this.data = data;
        this.particleOptions = parseParticleEffect(data.particleEffect());
    }

    // === Core Identification ===

    @Override
    public String getId() {
        return data.id();
    }

    @Override
    public String getDisplayName() {
        return data.displayName();
    }

    @Override
    public int getRealmLevel() {
        return data.realmLevel();
    }

    @Override
    public String getCultivationStage() {
        return data.cultivationStage();
    }

    @Override
    public String getDescription() {
        return data.description();
    }

    // === Base Physical Stats ===

    @Override
    public long getBaseMaxHealth() {
        return data.baseHealth();
    }

    @Override
    public double getBaseHealthRegenRate() {
        return data.baseHealthRegeneration();
    }

    @Override
    public double getBaseAttackDamage() {
        return data.baseAttackDamage();
    }

    @Override
    public double getBaseMovementSpeed() {
        return data.baseMovementSpeed();
    }

    @Override
    public double getBaseJumpHeight() {
        return data.baseJumpHeight();
    }

    // === Cultivation Stats ===

    @Override
    public long getBaseMaxCultivation() {
        return data.baseMaxCultivation();
    }

    @Override
    public double getBaseCultivationRate() {
        return data.baseCultivationRate();
    }

    // === Qi Stats ===

    @Override
    public long getBaseMaxQi() {
        return data.baseMaxQi();
    }

    @Override
    public double getBaseQiRegenRate() {
        return data.baseQiRegenerationRate();
    }

    // === Body Stats === //

    @Override
    public long getBaseMaxBody() {return data.baseMaxBody();}

    @Override
    public double getBaseBodyRegenRate() {return data.baseBodyRegenerationRate();}

    // === Realm Navigation ===

    @Override
    public List<CultivationRealm> getPreviousCultivationRealms() {
        List<CultivationRealm> realms = new ArrayList<>();
        for (String realmId : data.previousRealms()) {
            CultivationRealm realm = ICModCultivationRealms.get(realmId);
            if (realm != null) {
                realms.add(realm);
            }
        }
        return realms;
    }

    @Override
    public List<CultivationRealm> getNextCultivationRealms() {
        List<CultivationRealm> realms = new ArrayList<>();
        for (String realmId : data.nextRealms()) {
            CultivationRealm realm = ICModCultivationRealms.get(realmId);
            if (realm != null) {
                realms.add(realm);
            }
        }
        return realms;
    }

    // === Abilities and Features ===

    @Override
    public List<String> getUnlockedAbilities() {
        return new ArrayList<>(data.unlockedAbilities());
    }

    @Override
    public List<String> getRealmBenefits() {
        return new ArrayList<>(data.realmBenefits());
    }

    @Override
    public List<String> getAvailableTechniques() {
        return new ArrayList<>(data.availableTechniques());
    }

    // === Visual Properties ===

    @Override
    public int getRealmColor() {
        return data.realmColor();
    }

    @Override
    public ParticleOptions getRealmParticle() {
        return particleOptions;
    }

    @Override
    public ResourceLocation getRealmIcon() {
        return data.realmIcon().orElse(super.getRealmIcon());
    }

    // === Special Requirements ===

    @Override
    public boolean meetsAdditionalRequirements(Player player, ICultivationData cultivationData, ICultivationData.CultivationType type) {
        if (data.specialRequirements().isEmpty()) {
            return true; // No special requirements means anyone can access
        }

        CultivationRealmData.SpecialRequirements req = data.specialRequirements().get();

        // Check required items in player's inventory
        if (!req.requiredItems().isEmpty()) {
            for (String itemId : req.requiredItems()) {
                if (!hasRequiredItem(player, itemId)) {
                    return false;
                }
            }
        }

        // Check required conditions
        if (!req.requiredConditions().isEmpty()) {
            for (String condition : req.requiredConditions()) {
                if (!checkCondition(player, condition, type)) {
                    return false;
                }
            }
        }

        // Check special event requirement
        if (req.specialEvent().isPresent()) {
            if (!checkSpecialEvent(player, req.specialEvent().get(), type)) {
                return false;
            }
        }

        return true;
    }

    // === Helper Methods for Requirements ===

    private boolean hasRequiredItem(Player player, String itemId) {
        try {
            ResourceLocation itemLocation = ResourceLocation.parse(itemId);
            // Check if player has the required item in their inventory
            return player.getInventory().items.stream()
                .anyMatch(stack -> !stack.isEmpty() &&
                    stack.getItem().builtInRegistryHolder().key().location().equals(itemLocation));
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkCondition(Player player, String condition, ICultivationData.CultivationType type) {
        // Basic condition checking - you can expand this based on your needs
        switch (condition.toLowerCase()) {
            case "storm_weather":
                return player.level().isThundering();
            case "high_altitude":
                return player.getY() > 100;
            case "night_time":
                return !player.level().isDay();
            case "in_water":
                return player.isInWater();
            case "full_health":
                return player.getHealth() >= player.getMaxHealth();
            case "qi_focused":
                return type == ICultivationData.CultivationType.QI;
            case "body_focused":
                return type == ICultivationData.CultivationType.BODY;
            default:
                // Unknown condition defaults to true
                return true;
        }
    }

    private boolean checkSpecialEvent(Player player, String specialEvent, ICultivationData.CultivationType type) {
        // Basic special event checking - you can expand this based on your needs
        switch (specialEvent.toLowerCase()) {
            case "lightning_strike_survived":
                // This would require more complex tracking, for now return true
                return true;
            case "first_meditation":
                // Check if this is the player's first time meditating
                return true;
            case "qi_breakthrough":
                return type == ICultivationData.CultivationType.QI;
            case "body_breakthrough":
                return type == ICultivationData.CultivationType.BODY;
            default:
                // Unknown special event defaults to true
                return true;
        }
    }

    // === Unified Realm Events ===

    @Override
    public void onRealmAdvancement(Player player, ICultivationData cultivationData, @Nullable CultivationRealm previousRealm, ICultivationData.CultivationType type) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        // Send cultivation-type-specific advancement message
        String cultivationTypeText = type == ICultivationData.CultivationType.QI ? "refined your qi" : "tempered your body";
        serverPlayer.sendSystemMessage(
            Component.literal("You have " + cultivationTypeText + " to " + getDisplayName() + "!")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
        );

        // Handle optional realm events
        if (data.realmEvents().isPresent()) {
            CultivationRealmData.RealmEvents events = data.realmEvents().get();

            // Handle on enter event
            if (events.onEnterEvent().isPresent()) {
                handleRealmEvent(serverPlayer, events.onEnterEvent().get(), "enter", type);
            }

            // Handle breakthrough event (since this is an advancement)
            if (events.onBreakthroughEvent().isPresent()) {
                handleRealmEvent(serverPlayer, events.onBreakthroughEvent().get(), "breakthrough", type);
            }
        }

        // Play advancement effects
        playAdvancementEffects(serverPlayer);
    }

    @Override
    public void onRealmExit(Player player, ICultivationData cultivationData, @Nullable CultivationRealm nextRealm, ICultivationData.CultivationType type) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        // Send default exit message
        if (nextRealm != null && nextRealm.getRealmLevel() > this.getRealmLevel()) {
            String typeText = type.getDisplayName().toLowerCase();
            serverPlayer.sendSystemMessage(
                Component.literal("Your " + typeText + " prepares to advance beyond " + getDisplayName() + "...")
                    .withStyle(ChatFormatting.AQUA)
            );
        }

        // Handle optional realm events
        if (data.realmEvents().isPresent()) {
            CultivationRealmData.RealmEvents events = data.realmEvents().get();

            // Handle on exit event
            if (events.onExitEvent().isPresent()) {
                handleRealmEvent(serverPlayer, events.onExitEvent().get(), "exit", type);
            }
        }
    }

    // === Helper Methods ===

    private void handleRealmEvent(ServerPlayer player, String eventId, String eventType, ICultivationData.CultivationType cultivationType) {
        // Enhanced event handling with cultivation type context
        String typeText = cultivationType.getDisplayName().toLowerCase();

        switch (eventType) {
            case "enter":
                player.sendSystemMessage(
                    Component.literal("You feel the " + typeText + " energy of " + getDisplayName() + " coursing through you.")
                        .withStyle(ChatFormatting.GREEN)
                );
                player.level().playSound(null, player.blockPosition(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0F, 1.2F);
                break;
            case "exit":
                player.sendSystemMessage(
                    Component.literal("The " + typeText + " energy of " + getDisplayName() + " begins to fade.")
                        .withStyle(ChatFormatting.GRAY)
                );
                player.level().playSound(null, player.blockPosition(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8F, 0.8F);
                break;
            case "breakthrough":
                player.sendSystemMessage(
                    Component.literal("A " + typeText + " breakthrough! Your cultivation has reached new heights!")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                );
                player.level().playSound(null, player.blockPosition(),
                    SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.0F);
                break;
        }
    }

    private ParticleOptions parseParticleEffect(String particleId) {
        try {
            ResourceLocation particleLoc = ResourceLocation.parse(particleId);
            ParticleType<?> particleType = BuiltInRegistries.PARTICLE_TYPE.get(particleLoc);
            if (particleType != null) {
                // For simple particle types, we can create the options directly
                if (particleType == ParticleTypes.ENCHANT ||
                    particleType == ParticleTypes.CLOUD ||
                    particleType == ParticleTypes.FLAME ||
                    particleType == ParticleTypes.SMOKE) {
                    return (ParticleOptions) particleType;
                }
            }
        } catch (Exception e) {
            // Fall back to default if parsing fails
        }
        return ParticleTypes.ENCHANT;
    }

    private void playAdvancementEffects(ServerPlayer player) {
        // Play particle effects around the player
        if (player.level() instanceof ServerLevel serverLevel) {
            // Spawn particles in a circle around the player
            for (int i = 0; i < 20; i++) {
                double angle = (i / 20.0) * 2 * Math.PI;
                double x = player.getX() + Math.cos(angle) * 1.5;
                double z = player.getZ() + Math.sin(angle) * 1.5;
                double y = player.getY() + 1.0;

                serverLevel.sendParticles(particleOptions, x, y, z, 1, 0.0, 0.1, 0.0, 0.1);
            }
        }

        // Play advancement sound
        player.level().playSound(null, player.blockPosition(),
            SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    /**
     * Applies cumulative permanent bonuses with optional silence.
     *
     * @param player The player advancing to this realm
     * @param cultivationData The player's cultivation data
     * @param type The cultivation type (QI or BODY)
     * @param silent If true, suppresses advancement messages and logging
     */
    public void applyRealmModifiers(Player player, ICultivationData cultivationData, ICultivationData.CultivationType type, boolean silent) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        try {
            PlayerStatsAttachment stats = serverPlayer.getData(ICModAttachmentTypes.PLAYER_STATS.get());
            if (stats == null) {
                ImmortalCultivation.LOGGER.warn("No stats attachment found for player {}", player.getName().getString());
                return;
            }

            // ADD the realm bonuses using source-based tracking
            // This preserves pill and other bonuses while adding realm bonuses

            // === RESOURCE STATS === //
            long currentHealthFromRealm = stats.getSourcedStatBonus(IStatsData.StatType.HEALTH, BonusSource.REALM);
            long currentQiFromRealm = stats.getSourcedStatBonus(IStatsData.StatType.QI, BonusSource.REALM);
            long currentBodyFromRealm = stats.getSourcedStatBonus(IStatsData.StatType.BODY, BonusSource.REALM);

            double currentHealthRegenFromRealm = stats.getSourcedStatRegenBonus(IStatsData.StatType.HEALTH, BonusSource.REALM);
            double currentQiRegenFromRealm = stats.getSourcedStatRegenBonus(IStatsData.StatType.QI, BonusSource.REALM);
            double currentBodyRegenFromRealm = stats.getSourcedStatRegenBonus(IStatsData.StatType.BODY, BonusSource.REALM);

            // === COMBAT STATS === //
            double currentAttackFromRealm = stats.getSourcedCombatBonus(PlayerStatsAttachment.CombatBonusType.ATTACK_DAMAGE, BonusSource.REALM);
            double currentSpeedFromRealm = stats.getSourcedCombatBonus(PlayerStatsAttachment.CombatBonusType.MOVEMENT_SPEED, BonusSource.REALM);
            double currentJumpFromRealm = stats.getSourcedCombatBonus(PlayerStatsAttachment.CombatBonusType.JUMP_HEIGHT, BonusSource.REALM);

            // Apply new realm bonuses by adding realm values to existing realm bonuses
            // (This accumulates across realm advancements while preserving other sources)

            // Resource maximums
            stats.setSourcedStatBonus(IStatsData.StatType.HEALTH, BonusSource.REALM, currentHealthFromRealm + getBaseMaxHealth());
            stats.setSourcedStatBonus(IStatsData.StatType.QI, BonusSource.REALM, currentQiFromRealm + getBaseMaxQi());
            stats.setSourcedStatBonus(IStatsData.StatType.BODY, BonusSource.REALM, currentBodyFromRealm + getBaseMaxBody());

            // Resource regeneration rates
            stats.setSourcedStatRegenBonus(IStatsData.StatType.HEALTH, BonusSource.REALM, currentHealthRegenFromRealm + getBaseHealthRegenRate());
            stats.setSourcedStatRegenBonus(IStatsData.StatType.QI, BonusSource.REALM, currentQiRegenFromRealm + getBaseQiRegenRate());
            stats.setSourcedStatRegenBonus(IStatsData.StatType.BODY, BonusSource.REALM, currentBodyRegenFromRealm + getBaseBodyRegenRate());

            // Combat stats
            stats.setSourcedCombatBonus(PlayerStatsAttachment.CombatBonusType.ATTACK_DAMAGE, BonusSource.REALM, currentAttackFromRealm + getBaseAttackDamage());
            stats.setSourcedCombatBonus(PlayerStatsAttachment.CombatBonusType.MOVEMENT_SPEED, BonusSource.REALM, currentSpeedFromRealm + getBaseMovementSpeed());
            stats.setSourcedCombatBonus(PlayerStatsAttachment.CombatBonusType.JUMP_HEIGHT, BonusSource.REALM, currentJumpFromRealm + getBaseJumpHeight());

            // Force refresh all stats to apply the new permanent bonuses immediately
            EventManager.refreshStats(serverPlayer);

            // === CONDITIONAL LOGGING AND MESSAGES === //
            if (!silent) {
                // Only log and show messages for actual advancement, not login reapplication
                ImmortalCultivation.LOGGER.info("Applied cumulative realm modifiers for {} advancing to {} - " +
                        "Added H:+{} Q:+{} B:+{} | HR:+{} QR:+{} BR:+{} | ATK:+{}% SPD:+{}% JUMP:+{}% " +
                        "(New Totals: H:{}/{} Q:{}/{} B:{}/{})",
                    player.getName().getString(),
                    getDisplayName(),
                    getBaseMaxHealth(), getBaseMaxQi(), getBaseMaxBody(),
                    String.format("%.3f", getBaseHealthRegenRate()),
                    String.format("%.3f", getBaseQiRegenRate()),
                    String.format("%.3f", getBaseBodyRegenRate()),
                    String.format("%.1f", getBaseAttackDamage() * 100),
                    String.format("%.1f", getBaseMovementSpeed() * 100),
                    String.format("%.1f", getBaseJumpHeight() * 100),
                    stats.getCurrentStat(IStatsData.StatType.HEALTH),
                    stats.getMaxStat(IStatsData.StatType.HEALTH),
                    stats.getCurrentStat(IStatsData.StatType.QI),
                    stats.getMaxStat(IStatsData.StatType.QI),
                    stats.getCurrentStat(IStatsData.StatType.BODY),
                    stats.getMaxStat(IStatsData.StatType.BODY));
            } else {
                // Silent mode: only debug logging
                if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                    ImmortalCultivation.LOGGER.debug("Silently reapplied realm modifiers for {} to {} (login reapplication)",
                        player.getName().getString(), getDisplayName());
                }
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error applying realm modifiers for player {} in realm {}",
                player.getName().getString(), getId(), e);
        }
    }
}
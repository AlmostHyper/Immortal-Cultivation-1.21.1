package net.hyper.immortalcultivation.cultivation.realms.data;

import net.hyper.immortalcultivation.cultivation.IBodyCultivationData;
import net.hyper.immortalcultivation.cultivation.IQiCultivationData;
import net.hyper.immortalcultivation.cultivation.realms.CultivationRealm;
import net.hyper.immortalcultivation.registry.ICModCultivationRealms;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A data-driven implementation of CultivationRealm that loads its properties from JSON.
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
    public long getBaseHealth() {
        return data.baseHealth();
    }

    @Override
    public double getBaseHealthRegeneration() {
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
    public double getBaseQiRegenerationRate() {
        return data.baseQiRegenerationRate();
    }

    @Override
    public double getBaseCirculatingQiRegenerationRate() {
        return data.baseCirculatingQiRegenerationRate();
    }

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
    public boolean meetsAdditionalRequirements(Player player, IQiCultivationData qiData) {
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
                if (!checkCondition(player, condition)) {
                    return false;
                }
            }
        }

        // Check special event requirement
        if (req.specialEvent().isPresent()) {
            if (!checkSpecialEvent(player, req.specialEvent().get())) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean meetsAdditionalRequirements(Player player, IBodyCultivationData bodyData) {
        // For now, use the same requirements for both cultivation types
        // In the future, you could differentiate between Qi and Body requirements
        return meetsAdditionalRequirements(player, (IQiCultivationData) bodyData);
    }

    // === Helper Methods for Requirements ===

    private boolean hasRequiredItem(Player player, String itemId) {
        try {
            ResourceLocation itemLocation = ResourceLocation.parse(itemId);
            // Check if player has the required item in their inventory
            // This is a simplified check - you might want to implement more sophisticated logic
            return player.getInventory().items.stream()
                .anyMatch(stack -> !stack.isEmpty() &&
                    stack.getItem().builtInRegistryHolder().key().location().equals(itemLocation));
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkCondition(Player player, String condition) {
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
            default:
                // Unknown condition defaults to true
                return true;
        }
    }

    private boolean checkSpecialEvent(Player player, String specialEvent) {
        // Basic special event checking - you can expand this based on your needs
        switch (specialEvent.toLowerCase()) {
            case "lightning_strike_survived":
                // This would require more complex tracking, for now return true
                return true;
            case "first_meditation":
                // Check if this is the player's first time meditating
                return true;
            default:
                // Unknown special event defaults to true
                return true;
        }
    }

    // === Realm Events ===

    @Override
    public void onRealmAdvancement(Player player, IQiCultivationData qiData, @Nullable CultivationRealm previousRealm) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        // Send default advancement message
        serverPlayer.sendSystemMessage(
            Component.literal("You have advanced to " + getDisplayName() + "!")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
        );

        // Handle optional realm events
        if (data.realmEvents().isPresent()) {
            CultivationRealmData.RealmEvents events = data.realmEvents().get();

            // Handle on enter event
            if (events.onEnterEvent().isPresent()) {
                handleRealmEvent(serverPlayer, events.onEnterEvent().get(), "enter");
            }

            // Handle breakthrough event (since this is an advancement)
            if (events.onBreakthroughEvent().isPresent()) {
                handleRealmEvent(serverPlayer, events.onBreakthroughEvent().get(), "breakthrough");
            }
        }

        // Play default advancement effects
        playAdvancementEffects(serverPlayer);
    }

    @Override
    public void onRealmExit(Player player, IQiCultivationData qiData, @Nullable CultivationRealm nextRealm) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        // Send default exit message
        if (nextRealm != null && nextRealm.getRealmLevel() > this.getRealmLevel()) {
            serverPlayer.sendSystemMessage(
                Component.literal("You prepare to advance beyond " + getDisplayName() + "...")
                    .withStyle(ChatFormatting.AQUA)
            );
        }

        // Handle optional realm events
        if (data.realmEvents().isPresent()) {
            CultivationRealmData.RealmEvents events = data.realmEvents().get();

            // Handle on exit event
            if (events.onExitEvent().isPresent()) {
                handleRealmEvent(serverPlayer, events.onExitEvent().get(), "exit");
            }
        }
    }

    // === Body Cultivation Events ===

    @Override
    public void onRealmAdvancement(Player player, IBodyCultivationData bodyData, @Nullable CultivationRealm previousRealm) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        // Send default advancement message
        serverPlayer.sendSystemMessage(
            Component.literal("Your body has been tempered to " + getDisplayName() + "!")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
        );

        // Handle the same events as Qi cultivation for now
        if (data.realmEvents().isPresent()) {
            CultivationRealmData.RealmEvents events = data.realmEvents().get();

            if (events.onEnterEvent().isPresent()) {
                handleRealmEvent(serverPlayer, events.onEnterEvent().get(), "enter");
            }

            if (events.onBreakthroughEvent().isPresent()) {
                handleRealmEvent(serverPlayer, events.onBreakthroughEvent().get(), "breakthrough");
            }
        }

        playAdvancementEffects(serverPlayer);
    }

    @Override
    public void onRealmExit(Player player, IBodyCultivationData bodyData, @Nullable CultivationRealm nextRealm) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        if (nextRealm != null && nextRealm.getRealmLevel() > this.getRealmLevel()) {
            serverPlayer.sendSystemMessage(
                Component.literal("Your body prepares for further tempering beyond " + getDisplayName() + "...")
                    .withStyle(ChatFormatting.AQUA)
            );
        }

        if (data.realmEvents().isPresent()) {
            CultivationRealmData.RealmEvents events = data.realmEvents().get();

            if (events.onExitEvent().isPresent()) {
                handleRealmEvent(serverPlayer, events.onExitEvent().get(), "exit");
            }
        }
    }

    // === Helper Methods ===

    private void handleRealmEvent(ServerPlayer player, String eventId, String eventType) {
        // Basic event handling - you can expand this to handle different event types
        // For now, we'll just log the event and play a sound

        switch (eventType) {
            case "enter":
                player.sendSystemMessage(
                    Component.literal("You feel the energy of " + getDisplayName() + " coursing through you.")
                        .withStyle(ChatFormatting.GREEN)
                );
                player.level().playSound(null, player.blockPosition(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0F, 1.2F);
                break;
            case "exit":
                player.sendSystemMessage(
                    Component.literal("The energy of " + getDisplayName() + " begins to fade.")
                        .withStyle(ChatFormatting.GRAY)
                );
                player.level().playSound(null, player.blockPosition(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8F, 0.8F);
                break;
            case "breakthrough":
                player.sendSystemMessage(
                    Component.literal("A breakthrough! Your cultivation has reached new heights!")
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
}
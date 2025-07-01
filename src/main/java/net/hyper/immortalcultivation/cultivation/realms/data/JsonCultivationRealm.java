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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
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
            return true;
        }

        CultivationRealmData.SpecialRequirements req = data.specialRequirements().get();

        // Check minimum talent level
        if (req.minimumTalentLevel().isPresent()) {
            // You'd need to implement talent level checking here
            // For now, we'll assume it passes
        }

        // Check minimum time in realm
        if (req.minimumTimeInRealm().isPresent()) {
            long timeRequired = req.minimumTimeInRealm().get();
            if (qiData.getTimeInCurrentRealm() < timeRequired) {
                return false;
            }
        }

        // Check required items
        if (req.requiredItems().isPresent()) {
            // You'd need to implement item checking here
            // For now, we'll assume it passes
        }

        // Check custom requirement using the registry
        if (req.customRequirement().isPresent()) {
            String customReq = req.customRequirement().get();
            if (!CustomRealmBehaviorRegistry.checkRequirement(customReq, player, data)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean meetsAdditionalRequirements(Player player, IBodyCultivationData bodyData) {
        // Similar implementation as above but for body cultivation
        return meetsAdditionalRequirements(player, (IQiCultivationData) null);
    }

    // === Realm Events ===

    @Override
    public void onRealmAdvancement(Player player, IQiCultivationData qiData, @Nullable CultivationRealm previousRealm) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        if (data.realmEvents().isPresent()) {
            CultivationRealmData.RealmEvents events = data.realmEvents().get();

            // Send advancement messages
            if (events.advancementMessages().isPresent()) {
                List<String> messages = events.advancementMessages().get();
                if (!messages.isEmpty()) {
                    String message = messages.get(new Random().nextInt(messages.size()));
                    serverPlayer.sendSystemMessage(
                        Component.literal(message)
                            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                    );
                }
            }

            // Play advancement sound
            if (events.advancementSound().isPresent()) {
                ResourceLocation soundLoc = ResourceLocation.parse(events.advancementSound().get());
                SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(soundLoc);
                if (sound != null) {
                    serverPlayer.level().playSound(null, serverPlayer.blockPosition(),
                        sound, SoundSource.PLAYERS, 1.0F, 1.0F);
                }
            }

            // Execute advancement commands
            if (events.advancementCommands().isPresent()) {
                for (String command : events.advancementCommands().get()) {
                    String processedCommand = command.replace("{player}", serverPlayer.getName().getString());
                    serverPlayer.getServer().getCommands().performPrefixedCommand(
                        serverPlayer.getServer().createCommandSourceStack(), processedCommand);
                }
            }
        }

        // Execute custom advancement handler if registered
        CustomRealmBehaviorRegistry.handleAdvancement(
            data.id(), player, qiData, null, this, previousRealm
        );

        // Play default effects
        playAdvancementEffects(serverPlayer);
    }

    @Override
    public void onRealmExit(Player player, IQiCultivationData qiData, @Nullable CultivationRealm nextRealm) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        if (data.realmEvents().isPresent()) {
            CultivationRealmData.RealmEvents events = data.realmEvents().get();

            // Send exit messages
            if (events.exitMessages().isPresent()) {
                List<String> messages = events.exitMessages().get();
                if (!messages.isEmpty()) {
                    String message = messages.get(new Random().nextInt(messages.size()));
                    serverPlayer.sendSystemMessage(
                        Component.literal(message).withStyle(ChatFormatting.AQUA)
                    );
                }
            }

            // Play exit sound
            if (events.exitSound().isPresent()) {
                ResourceLocation soundLoc = ResourceLocation.parse(events.exitSound().get());
                SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(soundLoc);
                if (sound != null) {
                    serverPlayer.level().playSound(null, serverPlayer.blockPosition(),
                        sound, SoundSource.PLAYERS, 0.8F, 1.0F);
                }
            }

            // Execute exit commands
            if (events.exitCommands().isPresent()) {
                for (String command : events.exitCommands().get()) {
                    String processedCommand = command.replace("{player}", serverPlayer.getName().getString());
                    serverPlayer.getServer().getCommands().performPrefixedCommand(
                        serverPlayer.getServer().createCommandSourceStack(), processedCommand);
                }
            }
        }

        // Execute custom exit handler if registered
        CustomRealmBehaviorRegistry.handleExit(
            data.id(), player, qiData, null, this, nextRealm
        );
    }

    // === Helper Methods ===

    private ParticleOptions parseParticleEffect(String particleId) {
        try {
            ResourceLocation particleLoc = ResourceLocation.parse(particleId);
            ParticleType<?> particleType = BuiltInRegistries.PARTICLE_TYPE.get(particleLoc);
            if (particleType instanceof ParticleOptions options) {
                return options;
            }
        } catch (Exception e) {
            // Fall back to default
        }
        return ParticleTypes.ENCHANT;
    }
}
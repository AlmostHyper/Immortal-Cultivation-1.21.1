package net.hyper.immortalcultivation.cultivation.realms.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import java.util.List;
import java.util.Optional;

/**
 * Data-driven representation of a cultivation realm.
 * This class holds all the data that can be loaded from JSON files.
 * Split into logical groups to comply with codec field limits.
 */
public record CultivationRealmData(
    BasicInfo basicInfo,
    PhysicalStats physicalStats,
    CultivationStats cultivationStats,
    VisualProperties visualProperties,
    Progression progression,
    Features features,
    Optional<SpecialRequirements> specialRequirements,
    Optional<RealmEvents> realmEvents
) {

    public static final Codec<CultivationRealmData> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            BasicInfo.CODEC.fieldOf("basic_info").forGetter(CultivationRealmData::basicInfo),
            PhysicalStats.CODEC.fieldOf("physical_stats").forGetter(CultivationRealmData::physicalStats),
            CultivationStats.CODEC.fieldOf("cultivation_stats").forGetter(CultivationRealmData::cultivationStats),
            VisualProperties.CODEC.fieldOf("visual_properties").forGetter(CultivationRealmData::visualProperties),
            Progression.CODEC.fieldOf("progression").forGetter(CultivationRealmData::progression),
            Features.CODEC.fieldOf("features").forGetter(CultivationRealmData::features),
            SpecialRequirements.CODEC.optionalFieldOf("special_requirements").forGetter(CultivationRealmData::specialRequirements),
            RealmEvents.CODEC.optionalFieldOf("realm_events").forGetter(CultivationRealmData::realmEvents)
        ).apply(instance, CultivationRealmData::new)
    );

    // Convenience methods for easier access to nested data
    public String id() { return basicInfo.id(); }
    public String displayName() { return basicInfo.displayName(); }
    public int realmLevel() { return basicInfo.realmLevel(); }
    public String cultivationStage() { return basicInfo.cultivationStage(); }
    public String description() { return basicInfo.description(); }

    public long baseHealth() { return physicalStats.baseHealth(); }
    public double baseHealthRegeneration() { return physicalStats.baseHealthRegeneration(); }
    public double baseAttackDamage() { return physicalStats.baseAttackDamage(); }
    public double baseMovementSpeed() { return physicalStats.baseMovementSpeed(); }
    public double baseJumpHeight() { return physicalStats.baseJumpHeight(); }

    public long baseMaxCultivation() { return cultivationStats.baseMaxCultivation(); }
    public double baseCultivationRate() { return cultivationStats.baseCultivationRate(); }
    public long baseMaxQi() { return cultivationStats.baseMaxQi(); }
    public double baseQiRegenerationRate() { return cultivationStats.baseQiRegenerationRate(); }
    public double baseCirculatingQiRegenerationRate() { return cultivationStats.baseCirculatingQiRegenerationRate(); }

    public int realmColor() { return visualProperties.realmColor(); }
    public String particleEffect() { return visualProperties.particleEffect(); }
    public Optional<ResourceLocation> realmIcon() { return visualProperties.realmIcon(); }

    public List<String> previousRealms() { return progression.previousRealms(); }
    public List<String> nextRealms() { return progression.nextRealms(); }

    public List<String> unlockedAbilities() { return features.unlockedAbilities(); }
    public List<String> realmBenefits() { return features.realmBenefits(); }
    public List<String> availableTechniques() { return features.availableTechniques(); }

    /**
     * Basic realm identification and description information
     */
    public record BasicInfo(
        String id,
        String displayName,
        int realmLevel,
        String cultivationStage,
        String description
    ) {
        public static final Codec<BasicInfo> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                Codec.STRING.fieldOf("id").forGetter(BasicInfo::id),
                Codec.STRING.fieldOf("display_name").forGetter(BasicInfo::displayName),
                Codec.INT.fieldOf("realm_level").forGetter(BasicInfo::realmLevel),
                Codec.STRING.fieldOf("cultivation_stage").forGetter(BasicInfo::cultivationStage),
                Codec.STRING.fieldOf("description").forGetter(BasicInfo::description)
            ).apply(instance, BasicInfo::new)
        );
    }

    /**
     * Physical enhancement statistics
     */
    public record PhysicalStats(
        long baseHealth,
        double baseHealthRegeneration,
        double baseAttackDamage,
        double baseMovementSpeed,
        double baseJumpHeight
    ) {
        public static final Codec<PhysicalStats> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                Codec.LONG.fieldOf("base_health").forGetter(PhysicalStats::baseHealth),
                Codec.DOUBLE.fieldOf("base_health_regeneration").forGetter(PhysicalStats::baseHealthRegeneration),
                Codec.DOUBLE.fieldOf("base_attack_damage").forGetter(PhysicalStats::baseAttackDamage),
                Codec.DOUBLE.fieldOf("base_movement_speed").forGetter(PhysicalStats::baseMovementSpeed),
                Codec.DOUBLE.fieldOf("base_jump_height").forGetter(PhysicalStats::baseJumpHeight)
            ).apply(instance, PhysicalStats::new)
        );
    }

    /**
     * Cultivation and Qi-related statistics
     */
    public record CultivationStats(
        long baseMaxCultivation,
        double baseCultivationRate,
        long baseMaxQi,
        double baseQiRegenerationRate,
        double baseCirculatingQiRegenerationRate
    ) {
        public static final Codec<CultivationStats> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                Codec.LONG.fieldOf("base_max_cultivation").forGetter(CultivationStats::baseMaxCultivation),
                Codec.DOUBLE.fieldOf("base_cultivation_rate").forGetter(CultivationStats::baseCultivationRate),
                Codec.LONG.fieldOf("base_max_qi").forGetter(CultivationStats::baseMaxQi),
                Codec.DOUBLE.fieldOf("base_qi_regeneration_rate").forGetter(CultivationStats::baseQiRegenerationRate),
                Codec.DOUBLE.fieldOf("base_circulating_qi_regeneration_rate").forGetter(CultivationStats::baseCirculatingQiRegenerationRate)
            ).apply(instance, CultivationStats::new)
        );
    }

    /**
     * Visual appearance and effects
     */
    public record VisualProperties(
        int realmColor,
        String particleEffect,
        Optional<ResourceLocation> realmIcon
    ) {
        public static final Codec<VisualProperties> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                Codec.INT.optionalFieldOf("realm_color", 0xFFFFFF).forGetter(VisualProperties::realmColor),
                Codec.STRING.optionalFieldOf("particle_effect", "minecraft:enchant").forGetter(VisualProperties::particleEffect),
                ResourceLocation.CODEC.optionalFieldOf("realm_icon").forGetter(VisualProperties::realmIcon)
            ).apply(instance, VisualProperties::new)
        );
    }

    /**
     * Realm progression and advancement paths
     */
    public record Progression(
        List<String> previousRealms,
        List<String> nextRealms
    ) {
        public static final Codec<Progression> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                Codec.STRING.listOf().fieldOf("previous_realms").forGetter(Progression::previousRealms),
                Codec.STRING.listOf().fieldOf("next_realms").forGetter(Progression::nextRealms)
            ).apply(instance, Progression::new)
        );
    }

    /**
     * Unlocked features, abilities, and techniques
     */
    public record Features(
        List<String> unlockedAbilities,
        List<String> realmBenefits,
        List<String> availableTechniques
    ) {
        public static final Codec<Features> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                Codec.STRING.listOf().optionalFieldOf("unlocked_abilities", List.of()).forGetter(Features::unlockedAbilities),
                Codec.STRING.listOf().optionalFieldOf("realm_benefits", List.of()).forGetter(Features::realmBenefits),
                Codec.STRING.listOf().optionalFieldOf("available_techniques", List.of()).forGetter(Features::availableTechniques)
            ).apply(instance, Features::new)
        );
    }

    /**
     * Special requirements for breakthrough or realm-specific conditions
     */
    public record SpecialRequirements(
        Optional<List<String>> requiredItems,
        Optional<Integer> minimumTalentLevel,
        Optional<Long> minimumTimeInRealm,
        Optional<String> customRequirement
    ) {
        public static final Codec<SpecialRequirements> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                Codec.STRING.listOf().optionalFieldOf("required_items").forGetter(SpecialRequirements::requiredItems),
                Codec.INT.optionalFieldOf("minimum_talent_level").forGetter(SpecialRequirements::minimumTalentLevel),
                Codec.LONG.optionalFieldOf("minimum_time_in_realm").forGetter(SpecialRequirements::minimumTimeInRealm),
                Codec.STRING.optionalFieldOf("custom_requirement").forGetter(SpecialRequirements::customRequirement)
            ).apply(instance, SpecialRequirements::new)
        );
    }

    /**
     * Event configuration for realm advancement/exit
     */
    public record RealmEvents(
        Optional<List<String>> advancementMessages,
        Optional<List<String>> exitMessages,
        Optional<String> advancementSound,
        Optional<String> exitSound,
        Optional<List<String>> advancementCommands,
        Optional<List<String>> exitCommands
    ) {
        public static final Codec<RealmEvents> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                Codec.STRING.listOf().optionalFieldOf("advancement_messages").forGetter(RealmEvents::advancementMessages),
                Codec.STRING.listOf().optionalFieldOf("exit_messages").forGetter(RealmEvents::exitMessages),
                Codec.STRING.optionalFieldOf("advancement_sound").forGetter(RealmEvents::advancementSound),
                Codec.STRING.optionalFieldOf("exit_sound").forGetter(RealmEvents::exitSound),
                Codec.STRING.listOf().optionalFieldOf("advancement_commands").forGetter(RealmEvents::advancementCommands),
                Codec.STRING.listOf().optionalFieldOf("exit_commands").forGetter(RealmEvents::exitCommands)
            ).apply(instance, RealmEvents::new)
        );
    }
}
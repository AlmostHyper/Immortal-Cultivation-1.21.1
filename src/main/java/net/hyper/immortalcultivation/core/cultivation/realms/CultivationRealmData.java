package net.hyper.immortalcultivation.core.cultivation.realms;

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
    QiStats qiStats,  // FIXED: Added missing QiStats field
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
            QiStats.CODEC.fieldOf("qi_stats").forGetter(CultivationRealmData::qiStats),  // FIXED: Added QiStats to codec
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

    // FIXED: These now correctly reference qiStats instead of cultivationStats
    public long baseMaxQi() { return qiStats.baseMaxQi(); }
    public double baseQiRegenerationRate() { return qiStats.baseQiRegenerationRate(); }
    public double baseCirculatingQiRegenerationRate() { return qiStats.baseCirculatingQiRegenerationRate(); }

    public int realmColor() { return visualProperties.realmColor(); }
    public String particleEffect() { return visualProperties.particleEffect(); }
    public Optional<ResourceLocation> realmIcon() { return visualProperties.realmIcon(); }

    public List<String> previousRealms() { return progression.previousRealms(); }
    public List<String> nextRealms() { return progression.nextRealms(); }

    public List<String> unlockedAbilities() { return features.unlockedAbilities(); }
    public List<String> realmBenefits() { return features.realmBenefits(); }
    public List<String> availableTechniques() { return features.availableTechniques(); }

    /**
     * Basic information about a cultivation realm
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
     * Physical stats provided by a cultivation realm
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
     * Cultivation-related stats for the realm
     */
    public record CultivationStats(
        long baseMaxCultivation,
        double baseCultivationRate
    ) {
        public static final Codec<CultivationStats> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                Codec.LONG.fieldOf("base_max_cultivation").forGetter(CultivationStats::baseMaxCultivation),
                Codec.DOUBLE.fieldOf("base_cultivation_rate").forGetter(CultivationStats::baseCultivationRate)
            ).apply(instance, CultivationStats::new)
        );
    }

    /**
     * Qi-related stats for the realm
     */
    public record QiStats(
        long baseMaxQi,
        double baseQiRegenerationRate,
        double baseCirculatingQiRegenerationRate
    ) {
        public static final Codec<QiStats> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                Codec.LONG.fieldOf("base_max_qi").forGetter(QiStats::baseMaxQi),
                Codec.DOUBLE.fieldOf("base_qi_regeneration_rate").forGetter(QiStats::baseQiRegenerationRate),
                Codec.DOUBLE.fieldOf("base_circulating_qi_regeneration_rate").forGetter(QiStats::baseCirculatingQiRegenerationRate)
            ).apply(instance, QiStats::new)
        );
    }

    /**
     * Visual properties of the realm
     */
    public record VisualProperties(
        int realmColor,
        String particleEffect,
        Optional<ResourceLocation> realmIcon
    ) {
        public static final Codec<VisualProperties> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                Codec.INT.fieldOf("realm_color").forGetter(VisualProperties::realmColor),
                Codec.STRING.fieldOf("particle_effect").forGetter(VisualProperties::particleEffect),
                ResourceLocation.CODEC.optionalFieldOf("realm_icon").forGetter(VisualProperties::realmIcon)
            ).apply(instance, VisualProperties::new)
        );
    }

    /**
     * Realm progression information
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
     * Features and benefits of the realm
     */
    public record Features(
        List<String> unlockedAbilities,
        List<String> realmBenefits,
        List<String> availableTechniques
    ) {
        public static final Codec<Features> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                Codec.STRING.listOf().fieldOf("unlocked_abilities").forGetter(Features::unlockedAbilities),
                Codec.STRING.listOf().fieldOf("realm_benefits").forGetter(Features::realmBenefits),
                Codec.STRING.listOf().fieldOf("available_techniques").forGetter(Features::availableTechniques)
            ).apply(instance, Features::new)
        );
    }

    /**
     * Special requirements for entering the realm (optional)
     */
    public record SpecialRequirements(
        List<String> requiredItems,
        List<String> requiredConditions,
        Optional<String> specialEvent
    ) {
        public static final Codec<SpecialRequirements> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                Codec.STRING.listOf().fieldOf("required_items").forGetter(SpecialRequirements::requiredItems),
                Codec.STRING.listOf().fieldOf("required_conditions").forGetter(SpecialRequirements::requiredConditions),
                Codec.STRING.optionalFieldOf("special_event").forGetter(SpecialRequirements::specialEvent)
            ).apply(instance, SpecialRequirements::new)
        );
    }

    /**
     * Events that trigger when entering/exiting the realm (optional)
     */
    public record RealmEvents(
        Optional<String> onEnterEvent,
        Optional<String> onExitEvent,
        Optional<String> onBreakthroughEvent
    ) {
        public static final Codec<RealmEvents> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                Codec.STRING.optionalFieldOf("on_enter_event").forGetter(RealmEvents::onEnterEvent),
                Codec.STRING.optionalFieldOf("on_exit_event").forGetter(RealmEvents::onExitEvent),
                Codec.STRING.optionalFieldOf("on_breakthrough_event").forGetter(RealmEvents::onBreakthroughEvent)
            ).apply(instance, RealmEvents::new)
        );
    }
}
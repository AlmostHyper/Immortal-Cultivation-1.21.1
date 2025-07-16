package net.hyper.immortalcultivation.core.tribulation.registry;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.core.cultivation.tribulations.TribulationData;
import net.hyper.immortalcultivation.core.cultivation.tribulations.TribulationType;
import net.hyper.immortalcultivation.common.interfaces.ICultivationData;
import net.hyper.immortalcultivation.core.cultivation.realms.CultivationRealm;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Central registry for all tribulation types and data in the Immortal Cultivation system.
 *
 * Manages registration, lookup, and validation of tribulation types following the established
 * Registry pattern. Provides thread-safe access to tribulation data and supports data-driven
 * tribulation configuration through JSON integration.
 *
 * Key Responsibilities:
 * - Register and manage tribulation types and their associated data
 * - Provide lookup methods for tribulation resolution during breakthroughs
 * - Validate tribulation compatibility with realms and cultivation types
 * - Support data-driven configuration and runtime registration
 * - Maintain performance through optimized caching and indexing
 *
 * Architecture:
 * - Registry Pattern: Central registration point for all tribulations
 * - Thread Safety: Concurrent collections for multi-threaded access
 * - Data-Driven: Integration with codec system for JSON configuration
 * - Performance Optimized: Indexed lookups and cached results
 */
public final class TribulationRegistry {

    // === CONSTANTS === //
    private static final String REGISTRY_NAME = "TribulationRegistry";
    private static final boolean DEBUG_REGISTRY = true;

    // === CORE REGISTRIES === //

    /**
     * Primary registry mapping tribulation IDs to their type implementations
     */
    private static final Map<ResourceLocation, TribulationType<?>> TRIBULATION_TYPES = new ConcurrentHashMap<>();

    /**
     * Data registry mapping tribulation IDs to their configuration data
     */
    private static final Map<ResourceLocation, TribulationData> TRIBULATION_DATA = new ConcurrentHashMap<>();

    // === PERFORMANCE INDEXES === //

    /**
     * Index mapping realm levels to applicable tribulations for fast lookup
     */
    private static final Map<Integer, Set<ResourceLocation>> REALM_LEVEL_INDEX = new ConcurrentHashMap<>();

    /**
     * Index mapping cultivation types to compatible tribulations
     */
    private static final Map<ICultivationData.CultivationType, Set<ResourceLocation>> CULTIVATION_TYPE_INDEX = new ConcurrentHashMap<>();

    /**
     * Index mapping breakthrough types to required tribulations
     */
    private static final Map<BreakthroughType, Set<ResourceLocation>> BREAKTHROUGH_TYPE_INDEX = new ConcurrentHashMap<>();

    // === REGISTRATION STATE === //
    private static boolean initialized = false;
    private static boolean indexesBuilt = false;

    // === INITIALIZATION === //

    /**
     * Initializes the tribulation registry and registers default tribulation types.
     * This method should be called during mod initialization.
     */
    public static void initialize() {
        if (initialized) {
            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("[{}] Already initialized, skipping...", REGISTRY_NAME);
            }
            return;
        }

        ImmortalCultivation.LOGGER.info("[{}] Initializing Tribulation Registry...", REGISTRY_NAME);

        try {
            registerDefaultTribulations();
            buildIndexes();
            initialized = true;

            ImmortalCultivation.LOGGER.info("[{}] Initialized successfully with {} tribulation types",
                REGISTRY_NAME, TRIBULATION_TYPES.size());

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[{}] Failed to initialize: {}", REGISTRY_NAME, e.getMessage(), e);
        }
    }

    // === REGISTRATION METHODS === //

    /**
     * Registers a tribulation type with its associated data.
     *
     * @param id Unique identifier for the tribulation
     * @param type The tribulation type implementation
     * @param data Configuration data for the tribulation
     * @param <T> The specific tribulation type
     * @return true if registration was successful
     */
    public static <T extends TribulationType<T>> boolean register(ResourceLocation id, T type, TribulationData data) {
        if (id == null || type == null || data == null) {
            ImmortalCultivation.LOGGER.warn("[{}] Cannot register tribulation with null parameters", REGISTRY_NAME);
            return false;
        }

        if (TRIBULATION_TYPES.containsKey(id)) {
            ImmortalCultivation.LOGGER.warn("[{}] Tribulation {} already registered, skipping...", REGISTRY_NAME, id);
            return false;
        }

        try {
            TRIBULATION_TYPES.put(id, type);
            TRIBULATION_DATA.put(id, data);

            // Update indexes if they're already built
            if (indexesBuilt) {
                updateIndexesForTribulation(id, data);
            }

            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("[{}] Registered tribulation: {}", REGISTRY_NAME, id);
            }

            return true;

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[{}] Failed to register tribulation {}: {}",
                REGISTRY_NAME, id, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Registers tribulation data from JSON configuration.
     *
     * @param id Unique identifier for the tribulation
     * @param data Configuration data loaded from JSON
     * @return true if registration was successful
     */
    public static boolean registerData(ResourceLocation id, TribulationData data) {
        if (data == null) {
            ImmortalCultivation.LOGGER.warn("[{}] Cannot register null tribulation data for {}", REGISTRY_NAME, id);
            return false;
        }

        TRIBULATION_DATA.put(id, data);

        if (indexesBuilt) {
            updateIndexesForTribulation(id, data);
        }

        if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
            ImmortalCultivation.LOGGER.debug("[{}] Registered tribulation data: {}", REGISTRY_NAME, id);
        }

        return true;
    }

    // === LOOKUP METHODS === //

    /**
     * Gets a tribulation type by its identifier.
     *
     * @param id The tribulation identifier
     * @return The tribulation type, or null if not found
     */
    @Nullable
    public static TribulationType<?> getTribulationType(ResourceLocation id) {
        return TRIBULATION_TYPES.get(id);
    }

    /**
     * Gets tribulation data by its identifier.
     *
     * @param id The tribulation identifier
     * @return The tribulation data, or null if not found
     */
    @Nullable
    public static TribulationData getTribulationData(ResourceLocation id) {
        return TRIBULATION_DATA.get(id);
    }

    /**
     * Gets all tribulations applicable for a specific realm breakthrough.
     *
     * @param targetRealm The realm being broken through to
     * @param cultivationType The cultivation type (QI or BODY)
     * @param breakthroughType The type of breakthrough being attempted
     * @return Set of applicable tribulation identifiers
     */
    public static Set<ResourceLocation> getApplicableTribulations(CultivationRealm targetRealm,
                                                                  ICultivationData.CultivationType cultivationType,
                                                                  BreakthroughType breakthroughType) {
        Set<ResourceLocation> applicable = new HashSet<>();

        try {
            // Get tribulations by realm level
            Set<ResourceLocation> realmTribulations = REALM_LEVEL_INDEX.getOrDefault(targetRealm.getRealmLevel(), new HashSet<>());

            // Get tribulations by cultivation type
            Set<ResourceLocation> typeTribulations = CULTIVATION_TYPE_INDEX.getOrDefault(cultivationType, new HashSet<>());

            // Get tribulations by breakthrough type
            Set<ResourceLocation> breakthroughTribulations = BREAKTHROUGH_TYPE_INDEX.getOrDefault(breakthroughType, new HashSet<>());

            // Find intersection of all sets
            applicable.addAll(realmTribulations);
            applicable.retainAll(typeTribulations);
            applicable.retainAll(breakthroughTribulations);

            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("[{}] Found {} applicable tribulations for realm {} ({}, {})",
                    REGISTRY_NAME, applicable.size(), targetRealm.getId(), cultivationType, breakthroughType);
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("[{}] Error finding applicable tribulations: {}",
                REGISTRY_NAME, e.getMessage(), e);
        }

        return applicable;
    }

    /**
     * Determines if a tribulation is required for the given breakthrough context.
     *
     * @param player The player attempting breakthrough
     * @param targetRealm The realm being broken through to
     * @param cultivationType The cultivation type
     * @param breakthroughType The breakthrough type
     * @return true if tribulation is required
     */
    public static boolean isTribulationRequired(Player player, CultivationRealm targetRealm,
                                                ICultivationData.CultivationType cultivationType,
                                                BreakthroughType breakthroughType) {
        // Only major realm and special realm breakthroughs require tribulations
        if (breakthroughType != BreakthroughType.MAJOR && breakthroughType != BreakthroughType.SPECIAL) {
            return false;
        }

        Set<ResourceLocation> applicable = getApplicableTribulations(targetRealm, cultivationType, breakthroughType);
        return !applicable.isEmpty();
    }

    // === UTILITY METHODS === //

    /**
     * Gets all registered tribulation identifiers.
     *
     * @return Unmodifiable set of all tribulation IDs
     */
    public static Set<ResourceLocation> getAllTribulationIds() {
        return Collections.unmodifiableSet(TRIBULATION_TYPES.keySet());
    }

    /**
     * Gets the total number of registered tribulations.
     *
     * @return Number of registered tribulations
     */
    public static int getRegisteredCount() {
        return TRIBULATION_TYPES.size();
    }

    /**
     * Validates that all registered tribulations have corresponding data.
     *
     * @return true if all tribulations are properly configured
     */
    public static boolean validateRegistry() {
        Set<ResourceLocation> typesWithoutData = TRIBULATION_TYPES.keySet().stream()
            .filter(id -> !TRIBULATION_DATA.containsKey(id))
            .collect(Collectors.toSet());

        if (!typesWithoutData.isEmpty()) {
            ImmortalCultivation.LOGGER.warn("[{}] Found {} tribulation types without data: {}",
                REGISTRY_NAME, typesWithoutData.size(), typesWithoutData);
            return false;
        }

        return true;
    }

    // === PRIVATE IMPLEMENTATION === //

    /**
     * Registers the default tribulation types provided by the mod.
     */
    private static void registerDefaultTribulations() {
        // Major realm tribulations
        register(new ResourceLocation(ImmortalCultivation.MOD_ID, "lightning_tribulation"),
            new LightningTribulation(), createDefaultLightningData());

        register(new ResourceLocation(ImmortalCultivation.MOD_ID, "inner_demon_tribulation"),
            new InnerDemonTribulation(), createDefaultInnerDemonData());

        register(new ResourceLocation(ImmortalCultivation.MOD_ID, "elemental_tribulation"),
            new ElementalTribulation(), createDefaultElementalData());

        // Special realm tribulations
        register(new ResourceLocation(ImmortalCultivation.MOD_ID, "dao_heart_tribulation"),
            new DaoHeartTribulation(), createDefaultDaoHeartData());
    }

    /**
     * Builds performance indexes for fast tribulation lookup.
     */
    private static void buildIndexes() {
        REALM_LEVEL_INDEX.clear();
        CULTIVATION_TYPE_INDEX.clear();
        BREAKTHROUGH_TYPE_INDEX.clear();

        for (Map.Entry<ResourceLocation, TribulationData> entry : TRIBULATION_DATA.entrySet()) {
            updateIndexesForTribulation(entry.getKey(), entry.getValue());
        }

        indexesBuilt = true;

        if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
            ImmortalCultivation.LOGGER.debug("[{}] Built indexes: {} realm levels, {} cultivation types, {} breakthrough types",
                REGISTRY_NAME, REALM_LEVEL_INDEX.size(), CULTIVATION_TYPE_INDEX.size(), BREAKTHROUGH_TYPE_INDEX.size());
        }
    }

    /**
     * Updates indexes for a specific tribulation.
     *
     * @param id The tribulation identifier
     * @param data The tribulation data
     */
    private static void updateIndexesForTribulation(ResourceLocation id, TribulationData data) {
        // Update realm level index
        for (int realmLevel : data.triggerConditions().realmLevels()) {
            REALM_LEVEL_INDEX.computeIfAbsent(realmLevel, k -> ConcurrentHashMap.newKeySet()).add(id);
        }

        // Update cultivation type index
        for (ICultivationData.CultivationType type : data.triggerConditions().cultivationTypes()) {
            CULTIVATION_TYPE_INDEX.computeIfAbsent(type, k -> ConcurrentHashMap.newKeySet()).add(id);
        }

        // Update breakthrough type index
        for (BreakthroughType type : data.triggerConditions().breakthroughTypes()) {
            BREAKTHROUGH_TYPE_INDEX.computeIfAbsent(type, k -> ConcurrentHashMap.newKeySet()).add(id);
        }
    }

    // === DEFAULT DATA CREATION === //
    // These methods would create default TribulationData instances
    // Implementation details depend on your TribulationData structure

    private static TribulationData createDefaultLightningData() {
        // TODO: Implement default lightning tribulation data
        return null;
    }

    private static TribulationData createDefaultInnerDemonData() {
        // TODO: Implement default inner demon tribulation data
        return null;
    }

    private static TribulationData createDefaultElementalData() {
        // TODO: Implement default elemental tribulation data
        return null;
    }

    private static TribulationData createDefaultDaoHeartData() {
        // TODO: Implement default dao heart tribulation data
        return null;
    }

    // === NESTED ENUMS === //

    /**
     * Enumeration of breakthrough types that can trigger tribulations.
     */
    public enum BreakthroughType {
        MINOR,      // Stage breakthroughs within same realm
        MAJOR,      // Full realm breakthroughs
        SPECIAL,    // Special/unique realm breakthroughs
        BOUNDARY    // Boundary realm breakthroughs
    }
}
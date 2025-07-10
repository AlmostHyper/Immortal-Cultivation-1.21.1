package net.hyper.immortalcultivation.common.registry;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.core.cultivation.realms.CultivationRealm;
import net.hyper.immortalcultivation.core.cultivation.realms.CultivationRealmLoader;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.*;
import java.util.stream.Collectors;

public class ICModCultivationRealms {

    public static final ResourceKey<Registry<CultivationRealm>> REALM_REGISTRY_KEY =
        ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath("immortalcultivation", "cultivation_realms"));

    public static final DeferredRegister<CultivationRealm> REGISTRY =
        DeferredRegister.create(REALM_REGISTRY_KEY, "immortalcultivation");

    // Combined map for both static and dynamic realms
    private static final Map<String, CultivationRealm> REALM_MAP = new HashMap<>();

    // Track whether we're using JSON loading
    private static boolean useJsonLoading = true;

    /**
     * Register a realm (static or dynamic)
     */
    private static void registerRealm(CultivationRealm realm) {
        if (realm != null) {
            REALM_MAP.put(realm.getId(), realm);
        }
    }

    /**
     * Register a dynamically loaded realm (called by CultivationRealmLoader)
     */
    public static void registerDynamic(String id, CultivationRealm realm) {
        REALM_MAP.put(id, realm);
        ImmortalCultivation.LOGGER.debug("Registered dynamic realm: {}", id);
    }

    /**
     * Clear and re-register realms (called when reloading datapacks)
     */
    public static void clearAndReload(Map<String, CultivationRealm> newRealms) {
        REALM_MAP.clear();

        if (!useJsonLoading) {
            // Re-register static realms
        } else {
            // Register all new realms from JSON
            newRealms.forEach(ICModCultivationRealms::registerDynamic);
        }

        ImmortalCultivation.LOGGER.info("Reloaded {} cultivation realms", REALM_MAP.size());
    }

    /**
     * Get a realm by ID - checks both static and dynamic realms
     */
    public static CultivationRealm get(String id) {
        // First check the main map
        CultivationRealm realm = REALM_MAP.get(id);

        // If not found and using JSON loading, check the loader
        if (realm == null && useJsonLoading) {
            realm = CultivationRealmLoader.getRealm(id);
        }

        // Default to first qi condensation if not found
        if (realm == null) {
            ImmortalCultivation.LOGGER.warn("Realm '{}' not found, returning default", id);
            return REALM_MAP.getOrDefault("qi_condensation_l1", null);
        }

        return realm;
    }

    /**
     * Get all registered realms
     */
    public static Collection<CultivationRealm> getAllRealms() {
        if (useJsonLoading) {
            // Merge static and dynamic realms
            Map<String, CultivationRealm> allRealms = new HashMap<>(REALM_MAP);
            allRealms.putAll(CultivationRealmLoader.getAllRealms());
            return allRealms.values();
        }
        return REALM_MAP.values();
    }
    
    public static String[] getRealmIds() {
        return getAllRealms().stream()
                .map(CultivationRealm::getId)
                .toArray(String[]::new);
    }

    /**
     * Check if a realm exists
     */
    public static boolean exists(String id) {
        return get(id) != null;
    }

    /**
     * Get realms sorted by level
     */
    public static List<CultivationRealm> getRealmsByLevel() {
        List<CultivationRealm> sorted = new ArrayList<>(getAllRealms());
        sorted.sort(Comparator.comparingInt(CultivationRealm::getRealmLevel));
        return sorted;
    }

    /**
     * Toggle between JSON and static realm loading
     */
    public static void setUseJsonLoading(boolean useJson) {
        useJsonLoading = useJson;
        ImmortalCultivation.LOGGER.info("Cultivation realm loading mode: {}",
            useJson ? "JSON (data-driven)" : "Static (code-based)");

        // Re-initialize if switching modes
        REALM_MAP.clear();
        if (!useJson) {
        }
    }

    /**
     * Check if using JSON loading
     */
    public static boolean isUsingJsonLoading() {
        return useJsonLoading;
    }
}

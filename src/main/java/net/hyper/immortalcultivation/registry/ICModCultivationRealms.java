package net.hyper.immortalcultivation.registry;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.cultivation.realms.*;
import net.hyper.immortalcultivation.cultivation.realms.data.CultivationRealmLoader;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.*;

public class ICModCultivationRealms {

    public static final ResourceKey<Registry<CultivationRealm>> REALM_REGISTRY_KEY =
        ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath("immortalcultivation", "cultivation_realms"));

    public static final DeferredRegister<CultivationRealm> REGISTRY =
        DeferredRegister.create(REALM_REGISTRY_KEY, "immortalcultivation");

    // Combined map for both static and dynamic realms
    private static final Map<String, CultivationRealm> REALM_MAP = new HashMap<>();

    // Track whether we're using JSON loading
    private static boolean useJsonLoading = true;

    // Static realm instances (fallback if JSON loading is disabled)
    public static final MortalL0 MORTAL;
    public static final QiCondensationL1 QI_CONDENSATION_L1;
    public static final QiCondensationL2 QI_CONDENSATION_L2;
    public static final QiCondensationL3 QI_CONDENSATION_L3;
    public static final QiCondensationL4 QI_CONDENSATION_L4;
    public static final QiCondensationL5 QI_CONDENSATION_L5;
    public static final QiCondensationL6 QI_CONDENSATION_L6;
    public static final QiCondensationL7 QI_CONDENSATION_L7;
    public static final QiCondensationL8 QI_CONDENSATION_L8;
    public static final QiCondensationL9 QI_CONDENSATION_L9;
    public static final QiCondensationL10 QI_CONDENSATION_L10;
    public static final FoundationEstablishmentEarly FOUNDATION_ESTABLISHMENT_EARLY;

    static {
        // Initialize static realms only if not using JSON loading
        if (!useJsonLoading) {
            // Mortal Realm
            MORTAL = new MortalL0();

            // Qi Condensation Realms
            QI_CONDENSATION_L1 = new QiCondensationL1();
            QI_CONDENSATION_L2 = new QiCondensationL2();
            QI_CONDENSATION_L3 = new QiCondensationL3();
            QI_CONDENSATION_L4 = new QiCondensationL4();
            QI_CONDENSATION_L5 = new QiCondensationL5();
            QI_CONDENSATION_L6 = new QiCondensationL6();
            QI_CONDENSATION_L7 = new QiCondensationL7();
            QI_CONDENSATION_L8 = new QiCondensationL8();
            QI_CONDENSATION_L9 = new QiCondensationL9();
            QI_CONDENSATION_L10 = new QiCondensationL10();

            // Foundation Establishment
            FOUNDATION_ESTABLISHMENT_EARLY = new FoundationEstablishmentEarly();

            // Register static realms
            registerStaticRealms();
        } else {
            // Create placeholder instances that will be replaced by JSON loading
            MORTAL = null;
            QI_CONDENSATION_L1 = null;
            QI_CONDENSATION_L2 = null;
            QI_CONDENSATION_L3 = null;
            QI_CONDENSATION_L4 = null;
            QI_CONDENSATION_L5 = null;
            QI_CONDENSATION_L6 = null;
            QI_CONDENSATION_L7 = null;
            QI_CONDENSATION_L8 = null;
            QI_CONDENSATION_L9 = null;
            QI_CONDENSATION_L10 = null;
            FOUNDATION_ESTABLISHMENT_EARLY = null;

            ImmortalCultivation.LOGGER.info("JSON realm loading enabled - static realms disabled");
        }
    }

    /**
     * Register static realms (only used if JSON loading is disabled)
     */
    private static void registerStaticRealms() {
        if (!useJsonLoading) {
            registerRealm(MORTAL);
            registerRealm(QI_CONDENSATION_L1);
            registerRealm(QI_CONDENSATION_L2);
            registerRealm(QI_CONDENSATION_L3);
            registerRealm(QI_CONDENSATION_L4);
            registerRealm(QI_CONDENSATION_L5);
            registerRealm(QI_CONDENSATION_L6);
            registerRealm(QI_CONDENSATION_L7);
            registerRealm(QI_CONDENSATION_L8);
            registerRealm(QI_CONDENSATION_L9);
            registerRealm(QI_CONDENSATION_L10);
            registerRealm(FOUNDATION_ESTABLISHMENT_EARLY);
        }
    }

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
            registerStaticRealms();
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
            registerStaticRealms();
        }
    }

    /**
     * Check if using JSON loading
     */
    public static boolean isUsingJsonLoading() {
        return useJsonLoading;
    }
}
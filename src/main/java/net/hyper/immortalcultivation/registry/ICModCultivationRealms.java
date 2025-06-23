package net.hyper.immortalcultivation.registry;

import net.hyper.immortalcultivation.cultivation.realms.CultivationRealm;
import net.hyper.immortalcultivation.cultivation.realms.realm1.*;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ICModCultivationRealms {

    public static final ResourceKey<Registry<CultivationRealm>> REALM_REGISTRY_KEY =
        ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath("immortalcultivation", "cultivation_realms"));

    public static final DeferredRegister<CultivationRealm> REGISTRY =
        DeferredRegister.create(REALM_REGISTRY_KEY, "immortalcultivation");

    private static final Map<String, CultivationRealm> REALM_MAP = new HashMap<>();

    // Mortal Realm
    public static final MortalL0 MORTAL = new MortalL0();

    public static final QiCondensationL1 QI_CONDENSATION_L1 = new QiCondensationL1();
    public static final QiCondensationL2 QI_CONDENSATION_L2 = new QiCondensationL2();
    public static final QiCondensationL3 QI_CONDENSATION_L3 = new QiCondensationL3();
    public static final QiCondensationL4 QI_CONDENSATION_L4 = new QiCondensationL4();
    public static final QiCondensationL5 QI_CONDENSATION_L5 = new QiCondensationL5();
    public static final QiCondensationL6 QI_CONDENSATION_L6 = new QiCondensationL6();
    public static final QiCondensationL7 QI_CONDENSATION_L7 = new QiCondensationL7();
    public static final QiCondensationL8 QI_CONDENSATION_L8 = new QiCondensationL8();
    public static final QiCondensationL9 QI_CONDENSATION_L9 = new QiCondensationL9();
    public static final QiCondensationL10 QI_CONDENSATION_L10 = new QiCondensationL10();

    public static final FoundationEstablishmentEarly FOUNDATION_ESTABLISHMENT_EARLY = new FoundationEstablishmentEarly();

    static {
        // Mortal Realm
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

    private static void registerRealm(CultivationRealm realm) {
        REALM_MAP.put(realm.getId(), realm);
    }

    public static CultivationRealm get(String id) {
        return REALM_MAP.getOrDefault(id, QI_CONDENSATION_L1);
    }

    public static Collection<CultivationRealm> getAllRealms() {
        return REALM_MAP.values();
    }
}

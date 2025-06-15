package net.hyper.immortalcultivation.registry;

import net.hyper.immortalcultivation.cultivation.realms.CultivationRealm;
import net.hyper.immortalcultivation.cultivation.realms.realm1.MortalL0;
import net.hyper.immortalcultivation.cultivation.realms.realm1.QiCondensationL1;
import net.hyper.immortalcultivation.cultivation.realms.realm1.QiCondensationL2;
import net.hyper.immortalcultivation.cultivation.realms.realm1.QiCondensationL3;
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

    static {
        // Mortal Realm
        registerRealm(MORTAL);

        registerRealm(QI_CONDENSATION_L1);
        registerRealm(QI_CONDENSATION_L2);
        registerRealm(QI_CONDENSATION_L3);
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

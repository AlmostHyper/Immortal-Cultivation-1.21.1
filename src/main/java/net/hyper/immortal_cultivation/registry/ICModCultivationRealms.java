package net.hyper.immortal_cultivation.registry;

import net.hyper.immortal_cultivation.cultivation.realms.CultivationRealm;
import net.hyper.immortal_cultivation.cultivation.realms.realm1.QiCondensationL1;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegistryBuilder;

import java.util.function.Supplier;

public class ICModCultivationRealms {

    public static final ResourceKey<Registry<CultivationRealm>> REALM_REGISTRY_KEY = ResourceKey.createRegistryKey(ResourceLocation.parse("immortal_cultivation"));
    public static final DeferredRegister<CultivationRealm> REGISTRY = DeferredRegister.create(REALM_REGISTRY_KEY, "immortal_cultivation");

    // Mortal Realm
    public static final DeferredHolder<CultivationRealm, QiCondensationL1> QI_CONDENSATION_L1;
    public static final DeferredHolder<CultivationRealm, QiCondensationL1> QI_CONDENSATION_L2;
    public static final DeferredHolder<CultivationRealm, QiCondensationL1> QI_CONDENSATION_L3;
    public static final DeferredHolder<CultivationRealm, QiCondensationL1> QI_CONDENSATION_L4;
    public static final DeferredHolder<CultivationRealm, QiCondensationL1> QI_CONDENSATION_L5;
    public static final DeferredHolder<CultivationRealm, QiCondensationL1> QI_CONDENSATION_L6;
    public static final DeferredHolder<CultivationRealm, QiCondensationL1> QI_CONDENSATION_L7;
    public static final DeferredHolder<CultivationRealm, QiCondensationL1> QI_CONDENSATION_L8;
    public static final DeferredHolder<CultivationRealm, QiCondensationL1> QI_CONDENSATION_L9;
    public static final DeferredHolder<CultivationRealm, QiCondensationL1> QI_CONDENSATION_L10;

    public static final DeferredHolder<CultivationRealm, QiCondensationL1> FOUNDATION_ESTABLISHMENT_EARLY;
    public static final DeferredHolder<CultivationRealm, QiCondensationL1> FOUNDATION_ESTABLISHMENT_MIDDLE;
    public static final DeferredHolder<CultivationRealm, QiCondensationL1> FOUNDATION_ESTABLISHMENT_LATE;
    public static final DeferredHolder<CultivationRealm, QiCondensationL1> PERFECT_FOUNDATION_ESTABLISHMENT_EARLY;
    public static final DeferredHolder<CultivationRealm, QiCondensationL1> PERFECT_FOUNDATION_ESTABLISHMENT_MIDDLE;
    public static final DeferredHolder<CultivationRealm, QiCondensationL1> PERFECT_FOUNDATION_ESTABLISHMENT_LATE;

    public static final DeferredHolder<CultivationRealm, QiCondensationL1> CORE_FORMATION_EARLY;
    public static final DeferredHolder<CultivationRealm, QiCondensationL1> CORE_FORMATION_MIDDLE;
    public static final DeferredHolder<CultivationRealm, QiCondensationL1> CORE_FORMATION_LATE;
    public static final DeferredHolder<CultivationRealm, QiCondensationL1> GOLDEN_CORE_FORMATION_EARLY;
    public static final DeferredHolder<CultivationRealm, QiCondensationL1> GOLDEN_CORE_FORMATION_MIDDLE;
    public static final DeferredHolder<CultivationRealm, QiCondensationL1> GOLDEN_CORE_FORMATION_LATE;

    public static final DeferredHolder<CultivationRealm, QiCondensationL1> NASCENT_SOUL_EARLY;
    public static final DeferredHolder<CultivationRealm, QiCondensationL1> NASCENT_SOUL_MIDDLE;
    public static final DeferredHolder<CultivationRealm, QiCondensationL1> NASCENT_SOUL_LATE;

    public static final DeferredHolder<CultivationRealm, QiCondensationL1> DEITY_TRANSFORMATION_EARLY;
    public static final DeferredHolder<CultivationRealm, QiCondensationL1> DEITY_TRANSFORMATION_MIDDLE;
    public static final DeferredHolder<CultivationRealm, QiCondensationL1> DEITY_TRANSFORMATION_LATE;

    public static void register(IEventBus eventBus) {REGISTRY.register(eventBus);}

    static {
        // Mortal Realm
        QI_CONDENSATION_L1 = REGISTRY.register("qi_condensation_l1", QiCondensationL1::new);
        QI_CONDENSATION_L2 = REGISTRY.register("qi_condensation_l2", QiCondensationL1::new);
        QI_CONDENSATION_L3 = REGISTRY.register("qi_condensation_l3", QiCondensationL1::new);
        QI_CONDENSATION_L4 = REGISTRY.register("qi_condensation_l4", QiCondensationL1::new);
        QI_CONDENSATION_L5 = REGISTRY.register("qi_condensation_l5", QiCondensationL1::new);
        QI_CONDENSATION_L6 = REGISTRY.register("qi_condensation_l6", QiCondensationL1::new);
        QI_CONDENSATION_L7 = REGISTRY.register("qi_condensation_l7", QiCondensationL1::new);
        QI_CONDENSATION_L8 = REGISTRY.register("qi_condensation_l8", QiCondensationL1::new);
        QI_CONDENSATION_L9 = REGISTRY.register("qi_condensation_l9", QiCondensationL1::new);
        QI_CONDENSATION_L10 = REGISTRY.register("qi_condensation_l10", QiCondensationL1::new);

        FOUNDATION_ESTABLISHMENT_EARLY = REGISTRY.register("foundation_establishment_early", QiCondensationL1::new);
        FOUNDATION_ESTABLISHMENT_MIDDLE = REGISTRY.register("foundation_establishment_middle", QiCondensationL1::new);
        FOUNDATION_ESTABLISHMENT_LATE = REGISTRY.register("foundation_establishment_late", QiCondensationL1::new);
        PERFECT_FOUNDATION_ESTABLISHMENT_EARLY = REGISTRY.register("perfect_foundation_establishment_early", QiCondensationL1::new);
        PERFECT_FOUNDATION_ESTABLISHMENT_MIDDLE = REGISTRY.register("perfect_foundation_establishment_middle", QiCondensationL1::new);
        PERFECT_FOUNDATION_ESTABLISHMENT_LATE = REGISTRY.register("perfect_foundation_establishment_late", QiCondensationL1::new);

        CORE_FORMATION_EARLY = REGISTRY.register("core_formation_early", QiCondensationL1::new);
        CORE_FORMATION_MIDDLE = REGISTRY.register("core_formation_middle", QiCondensationL1::new);
        CORE_FORMATION_LATE = REGISTRY.register("core_formation_late", QiCondensationL1::new);
        GOLDEN_CORE_FORMATION_EARLY = REGISTRY.register("perfect_core_formation_early", QiCondensationL1::new);
        GOLDEN_CORE_FORMATION_MIDDLE = REGISTRY.register("perfect_core_formation_middle", QiCondensationL1::new);
        GOLDEN_CORE_FORMATION_LATE = REGISTRY.register("perfect_core_formation_late", QiCondensationL1::new);

        NASCENT_SOUL_EARLY = REGISTRY.register("nascent_soul_early", QiCondensationL1::new);
        NASCENT_SOUL_MIDDLE = REGISTRY.register("nascent_soul_middle", QiCondensationL1::new);
        NASCENT_SOUL_LATE = REGISTRY.register("nascent_soul_late", QiCondensationL1::new);

        DEITY_TRANSFORMATION_EARLY = REGISTRY.register("deity_transformation_early", QiCondensationL1::new);
        DEITY_TRANSFORMATION_MIDDLE = REGISTRY.register("deity_transformation_middle", QiCondensationL1::new);
        DEITY_TRANSFORMATION_LATE = REGISTRY.register("deity_transformation_late", QiCondensationL1::new);

        // Spirit Realm
    }
}

package net.hyper.immortalcultivation.registry;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.attachment.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class ICModAttachmentTypes {
    public static final DeferredRegister<AttachmentType<?>> REGISTRY = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ImmortalCultivation.MOD_ID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerRealmAttachment>> PLAYER_REALM;
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerQiCultivationAttachment>> PLAYER_QI_CULTIVATION;
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerBodyCultivationAttachment>> PLAYER_BODY_CULTIVATION;
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerCultivationFocusAttachment>> PLAYER_CULTIVATION_FOCUS;
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerCultivationBonusAttachment>> PLAYER_CULTIVATION_BONUS;

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerHealthAttachment>> PLAYER_HEALTH;
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerQiAttachment>> PLAYER_QI;
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerBodyAttachment>> PLAYER_BODY;
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerSpiritualRootAttachment>> PLAYER_SPIRITUAL_ROOT;
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerPhysiqueAttachment>> PLAYER_PHYSIQUE;

    static {
        PLAYER_REALM = REGISTRY.register("player_realm", () -> AttachmentType.builder(() -> new PlayerRealmAttachment()).serialize(PlayerRealmAttachment.CODEC).copyOnDeath().build());
        PLAYER_QI_CULTIVATION = REGISTRY.register("player_qi_cultivation", () -> AttachmentType.builder(() -> new PlayerQiCultivationAttachment()).serialize(PlayerQiCultivationAttachment.CODEC).copyOnDeath().build());
        PLAYER_BODY_CULTIVATION = REGISTRY.register("player_body_cultivation", () -> AttachmentType.builder(() -> new PlayerBodyCultivationAttachment()).serialize(PlayerBodyCultivationAttachment.CODEC).copyOnDeath().build());
        PLAYER_CULTIVATION_FOCUS = REGISTRY.register("player_cultivation_focus", () -> AttachmentType.builder(() -> new PlayerCultivationFocusAttachment()).serialize(PlayerCultivationFocusAttachment.CODEC).copyOnDeath().build());
        PLAYER_CULTIVATION_BONUS = REGISTRY.register("player_cultivation_bonus", () -> AttachmentType.builder(() -> new PlayerCultivationBonusAttachment()).serialize(PlayerCultivationBonusAttachment.CODEC).copyOnDeath().build());

        PLAYER_HEALTH = REGISTRY.register("player_health", () -> AttachmentType.builder(() -> new PlayerHealthAttachment()).serialize(PlayerHealthAttachment.CODEC).copyOnDeath().build());
        PLAYER_QI = REGISTRY.register("player_qi", () -> AttachmentType.builder(() -> new PlayerQiAttachment()).serialize(PlayerQiAttachment.CODEC).copyOnDeath().build());
        PLAYER_BODY = REGISTRY.register("player_body", () -> AttachmentType.builder(() -> new PlayerBodyAttachment()).serialize(PlayerBodyAttachment.CODEC).copyOnDeath().build());
        PLAYER_SPIRITUAL_ROOT = REGISTRY.register("player_spiritual_root", () -> AttachmentType.builder(() -> new PlayerSpiritualRootAttachment()).serialize(PlayerSpiritualRootAttachment.CODEC).copyOnDeath().build());
        PLAYER_PHYSIQUE = REGISTRY.register("player_physique", () -> AttachmentType.builder(() -> new PlayerPhysiqueAttachment()).serialize(PlayerPhysiqueAttachment.CODEC).copyOnDeath().build());
    }

    public static void register(IEventBus modBus) {
        REGISTRY.register(modBus);
    }
}

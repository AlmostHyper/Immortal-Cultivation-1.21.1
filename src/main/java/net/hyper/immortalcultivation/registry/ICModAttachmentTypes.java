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

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerCultivationAttachment>> PLAYER_CULTIVATION;
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerRealmAttachment>> PLAYER_REALM;
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerQiAttachment>> PLAYER_QI;
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerBodyAttachment>> PLAYER_BODY;
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerSpiritualRootAttachment>> PLAYER_SPIRITUAL_ROOT;

    static {
        PLAYER_CULTIVATION = REGISTRY.register("player_cultivation", () -> AttachmentType.builder(() -> new PlayerCultivationAttachment()).build());
        PLAYER_REALM = REGISTRY.register("player_realm", () -> AttachmentType.builder(() -> new PlayerRealmAttachment()).serialize(PlayerRealmAttachment.CODEC).copyOnDeath().build());
        PLAYER_QI = REGISTRY.register("player_qi", () -> AttachmentType.builder(() -> new PlayerQiAttachment()).serialize(PlayerQiAttachment.CODEC).copyOnDeath().build());
        PLAYER_BODY = REGISTRY.register("player_health", () -> AttachmentType.builder(() -> new PlayerBodyAttachment()).build());
        PLAYER_SPIRITUAL_ROOT = REGISTRY.register("player_spiritual_root", () -> AttachmentType.builder(() -> new PlayerSpiritualRootAttachment()).serialize(PlayerSpiritualRootAttachment.CODEC).copyOnDeath().build());
    }

    public static void register(IEventBus modBus) {
        REGISTRY.register(modBus);
    }
}

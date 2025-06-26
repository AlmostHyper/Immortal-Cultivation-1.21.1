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
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerResourceAttachment>> PLAYER_RESOURCES;
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerTalentAttachment>> PLAYER_TALENTS;

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerCombatStatsAttachment>> PLAYER_COMBAT_STATS;

    static {
        PLAYER_CULTIVATION = REGISTRY.register("player_cultivation", () -> AttachmentType.builder(() -> new PlayerCultivationAttachment()).serialize(PlayerCultivationAttachment.CODEC).copyOnDeath().build());
        PLAYER_RESOURCES = REGISTRY.register("player_resource", () -> AttachmentType.builder(() -> new PlayerResourceAttachment()).serialize(PlayerResourceAttachment.CODEC).copyOnDeath().build());
        PLAYER_TALENTS = REGISTRY.register("player_talent", () -> AttachmentType.builder(() -> new PlayerTalentAttachment()).serialize(PlayerTalentAttachment.CODEC).copyOnDeath().build());

        PLAYER_COMBAT_STATS = REGISTRY.register("player_combat_stats", () -> AttachmentType.builder(() -> new PlayerCombatStatsAttachment()).serialize(PlayerCombatStatsAttachment.CODEC).copyOnDeath().build());
    }

    public static void register(IEventBus modBus) {
        REGISTRY.register(modBus);
    }
}

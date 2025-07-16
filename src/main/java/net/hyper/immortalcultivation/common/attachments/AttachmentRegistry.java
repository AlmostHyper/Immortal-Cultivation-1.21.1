package net.hyper.immortalcultivation.common.attachments;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class AttachmentRegistry {
    public static final DeferredRegister<AttachmentType<?>> REGISTRY = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ImmortalCultivation.MOD_ID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerCultivationAttachment>> PLAYER_CULTIVATION;
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerStatsAttachment>> PLAYER_STATS;
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerBreakthroughAttachment>> PLAYER_BREAKTHROUGH;
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerTalentsAttachment>> PLAYER_TALENTS;
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerRaceAttachment>> PLAYER_RACE;
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerPillConsumptionAttachment>> PLAYER_PILL_CONSUMPTION;

    static {
        PLAYER_CULTIVATION = REGISTRY.register("player_cultivation", () ->
            AttachmentType.builder(() -> new PlayerCultivationAttachment())
                .serialize(new PlayerCultivationAttachment.Serializer())
                .copyOnDeath()
                .build());

        PLAYER_STATS = REGISTRY.register("player_stats", () ->
            AttachmentType.builder(() -> new PlayerStatsAttachment())
                .serialize(new PlayerStatsAttachment.Serializer())
                .copyOnDeath()
                .build());

        PLAYER_BREAKTHROUGH = REGISTRY.register("player_breakthrough", () ->
            AttachmentType.builder(() -> new PlayerBreakthroughAttachment())
                .serialize(new PlayerBreakthroughAttachment.Serializer())
                .copyOnDeath()
                .build());

        PLAYER_TALENTS = REGISTRY.register("player_talents", () ->
            AttachmentType.builder(() -> new PlayerTalentsAttachment())
                .serialize(new PlayerTalentsAttachment.Serializer())
                .copyOnDeath()
                .build());

        PLAYER_RACE = REGISTRY.register("player_race", () ->
            AttachmentType.builder(() -> new PlayerRaceAttachment())
                .serialize(new PlayerRaceAttachment.Serializer())
                .copyOnDeath()
                .build());

        PLAYER_PILL_CONSUMPTION = REGISTRY.register("player_pill_consumption", () ->
            AttachmentType.builder(() -> new PlayerPillConsumptionAttachment())
                .serialize(new PlayerPillConsumptionAttachment.Serializer())
                .copyOnDeath()
                .build());
    }

    public static void register(IEventBus modBus) {
        REGISTRY.register(modBus);
    }
}
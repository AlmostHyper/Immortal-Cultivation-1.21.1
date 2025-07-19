package net.hyper.immortalcultivation.core.network.packets.client;

import io.netty.buffer.ByteBuf;
import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.core.attachments.PlayerBreakthroughAttachment;
import net.hyper.immortalcultivation.core.attachments.PlayerCultivationAttachment;
import net.hyper.immortalcultivation.core.attachments.AttachmentRegistry;
import net.hyper.immortalcultivation.core.cultivation.races.RaceType;
import net.hyper.immortalcultivation.core.network.NetworkManager;
import net.hyper.immortalcultivation.core.network.helpers.BreakthroughFeedbackHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

public record UnifiedActionRequestPacket(
    ActionType actionType,
    Optional<PlayerCultivationAttachment.CultivationType> cultivationType,
    Optional<PlayerBreakthroughAttachment.BreakthroughType> breakthroughType,
    Optional<String> stringParam
) implements CustomPacketPayload {

    public static final Type<UnifiedActionRequestPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath("immortalcultivation", "unified_action_request")
    );

    public enum ActionType {
        // --- Core Cultivation ---
        BREAKTHROUGH_REQUEST,
        CULTIVATION_FOCUS_TOGGLE,
        MEDITATION_TOGGLE,
        // --- Player Setup ---
        SELECT_RACE,
    }

    private static final Map<ActionType, BiConsumer<ServerPlayer, UnifiedActionRequestPacket>> actionHandlers = new EnumMap<>(ActionType.class);

    static {
        // Core Cultivation
        actionHandlers.put(ActionType.BREAKTHROUGH_REQUEST, UnifiedActionRequestPacket::processBreakthroughRequest);
        actionHandlers.put(ActionType.CULTIVATION_FOCUS_TOGGLE, (p, pkt) -> processFocusToggle(p));
        actionHandlers.put(ActionType.MEDITATION_TOGGLE, (p, pkt) -> processMeditationToggle(p));
        // Player Setup
        actionHandlers.put(ActionType.SELECT_RACE, UnifiedActionRequestPacket::processRaceSelection);
    }

    // --- Convenience Constructors ---

    public static UnifiedActionRequestPacket createDataRequest(ActionType type) {
        return new UnifiedActionRequestPacket(type, Optional.empty(), Optional.empty(), Optional.empty());
    }
    
    public static UnifiedActionRequestPacket createBreakthroughRequest(PlayerCultivationAttachment.CultivationType cType, PlayerBreakthroughAttachment.BreakthroughType bType) {
        return new UnifiedActionRequestPacket(ActionType.BREAKTHROUGH_REQUEST, Optional.of(cType), Optional.of(bType), Optional.empty());
    }

    public static UnifiedActionRequestPacket createFocusToggle() {
        return new UnifiedActionRequestPacket(ActionType.CULTIVATION_FOCUS_TOGGLE, Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static UnifiedActionRequestPacket createMeditationToggle() {
        return new UnifiedActionRequestPacket(ActionType.MEDITATION_TOGGLE, Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static UnifiedActionRequestPacket createRaceSelection(String raceId) {
        return new UnifiedActionRequestPacket(ActionType.SELECT_RACE, Optional.empty(), Optional.empty(), Optional.of(raceId));
    }

    // --- Serialization ---

    public static final StreamCodec<ByteBuf, UnifiedActionRequestPacket> STREAM_CODEC = StreamCodec.of(
        UnifiedActionRequestPacket::encode, UnifiedActionRequestPacket::decode
    );

    private static void encode(ByteBuf buf, UnifiedActionRequestPacket packet) {
        ByteBufCodecs.STRING_UTF8.encode(buf, packet.actionType.name());
        packet.cultivationType.ifPresentOrElse(
            val -> {
                buf.writeBoolean(true);
                ByteBufCodecs.STRING_UTF8.encode(buf, val.name());
            }, () -> buf.writeBoolean(false)
        );
        packet.breakthroughType.ifPresentOrElse(
            val -> {
                buf.writeBoolean(true);
                ByteBufCodecs.STRING_UTF8.encode(buf, val.name());
            }, () -> buf.writeBoolean(false)
        );
        packet.stringParam.ifPresentOrElse(
            val -> {
                buf.writeBoolean(true);
                ByteBufCodecs.STRING_UTF8.encode(buf, val);
            }, () -> buf.writeBoolean(false)
        );
    }

    private static UnifiedActionRequestPacket decode(ByteBuf buf) {
        ActionType actionType = ActionType.valueOf(ByteBufCodecs.STRING_UTF8.decode(buf));
        Optional<PlayerCultivationAttachment.CultivationType> cType = buf.readBoolean() ? Optional.of(PlayerCultivationAttachment.CultivationType.valueOf(ByteBufCodecs.STRING_UTF8.decode(buf))) : Optional.empty();
        Optional<PlayerBreakthroughAttachment.BreakthroughType> bType = buf.readBoolean() ? Optional.of(PlayerBreakthroughAttachment.BreakthroughType.valueOf(ByteBufCodecs.STRING_UTF8.decode(buf))) : Optional.empty();
        Optional<String> sParam = buf.readBoolean() ? Optional.of(ByteBufCodecs.STRING_UTF8.decode(buf)) : Optional.empty();
        return new UnifiedActionRequestPacket(actionType, cType, bType, sParam);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // --- Server Handling ---

    public static void handleServer(UnifiedActionRequestPacket packet, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer serverPlayer) {
            actionHandlers.getOrDefault(packet.actionType, (p, pkt) ->
                ImmortalCultivation.LOGGER.warn("No handler for action type: {}", pkt.actionType)
            ).accept(serverPlayer, packet);
        }
    }

    // --- Action Implementations ---

    private static void processBreakthroughRequest(ServerPlayer player, UnifiedActionRequestPacket packet) {
        if (packet.cultivationType.isEmpty() || packet.breakthroughType.isEmpty()) return;

        var breakthrough = player.getData(AttachmentRegistry.PLAYER_BREAKTHROUGH.get());
        breakthrough.setPlayer(player);
        var result = breakthrough.attemptTypedBreakthrough(packet.cultivationType.get(), packet.breakthroughType.get());

        BreakthroughFeedbackHelper.sendBreakthroughFeedback(player, result, packet.breakthroughType.get());
        NetworkManager.INSTANCE.scheduleSync(player, AttachmentRegistry.PLAYER_BREAKTHROUGH::get, true);
    }

    private static void processFocusToggle(ServerPlayer player) {
        var cultivation = player.getData(AttachmentRegistry.PLAYER_CULTIVATION.get());
        cultivation.setPlayer(player);
        
        // This logic is preserved from the old CultivationFocusTogglePacket
        boolean success = cultivation.toggleFocus();
        if (success) {
            var newFocus = cultivation.getCurrentFocus();
            player.sendSystemMessage(Component.literal("✨ Cultivation focus shifted to ").append(Component.literal(newFocus.getDisplayName() + " Cultivation").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)).append("!").withStyle(ChatFormatting.GREEN));
            player.sendSystemMessage(Component.literal("» ").append(Component.literal("2x").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)).append(" cultivation speed | ").append(Component.literal("1.5x").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)).append(" energy regeneration").withStyle(ChatFormatting.GRAY));
            player.level().playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 1.5f);
            ImmortalCultivation.LOGGER.info("Player {} changed cultivation focus to {}", player.getName().getString(), newFocus.getDisplayName());
        } else {
            long remainingTicks = cultivation.getFocusChangeCooldownRemaining();
            long remainingMinutes = remainingTicks / 1200;
            player.sendSystemMessage(Component.literal("⏳ Focus change on cooldown! ").append(Component.literal(remainingMinutes + " minutes").withStyle(ChatFormatting.YELLOW)).append(" remaining.").withStyle(ChatFormatting.RED));
            player.level().playSound(null, player.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0f, 1.0f);
        }
        
        // This action only affects cultivation data, so a targeted sync is efficient.
        NetworkManager.INSTANCE.scheduleSync(player, AttachmentRegistry.PLAYER_CULTIVATION::get, false);
    }

    /**
     * Processes meditation toggle requests from clients.
     * Toggles the player's meditation state and applies appropriate effects.
     *
     * @param player The server player requesting meditation toggle
     */
    private static void processMeditationToggle(ServerPlayer player) {
        if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
            ImmortalCultivation.LOGGER.debug("Processing meditation toggle for player: {}",
                player.getName().getString());
        }

        try {
            var cultivationAttachment = player.getData(AttachmentRegistry.PLAYER_CULTIVATION.get());
            cultivationAttachment.setPlayer(player);

            boolean isMeditating = cultivationAttachment.toggleMeditation();

            if (isMeditating) {
                player.sendSystemMessage(Component.literal("🧘 You begin to meditate, feeling your cultivation rate gradually increase...")
                    .withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
            } else {
                player.sendSystemMessage(Component.literal("🚶 You cease meditation and return to normal cultivation.")
                    .withStyle(ChatFormatting.GRAY));
            }

            // Schedule sync to update client
            NetworkManager.INSTANCE.scheduleSync(player, AttachmentRegistry.PLAYER_CULTIVATION::get, true);

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error processing meditation toggle for player {}: {}",
                player.getName().getString(), e.getMessage(), e);
            player.sendSystemMessage(Component.literal("Failed to toggle meditation state.")
                .withStyle(ChatFormatting.RED));
        }
    }

    private static void processRaceSelection(ServerPlayer player, UnifiedActionRequestPacket packet) {
        if (packet.stringParam.isEmpty()) return;
        
        var raceAttachment = player.getData(AttachmentRegistry.PLAYER_RACE.get());
        if (raceAttachment.hasRaceAssigned()) {
            player.sendSystemMessage(Component.literal("You have already chosen a path.").withStyle(ChatFormatting.RED));
            return;
        }

        try {
            var raceType = RaceType.valueOf(packet.stringParam().get().toUpperCase());
            raceAttachment.assignRace(raceType);  // Remove the 'player' parameter
            player.sendSystemMessage(Component.literal("You are now a " + raceType.getDisplayName() + "!").withStyle(ChatFormatting.GOLD));
            // A full sync is appropriate after a major change like race selection.
            NetworkManager.INSTANCE.scheduleSync(player, AttachmentRegistry.PLAYER_RACE::get, true);
        } catch (IllegalArgumentException e) {
            player.sendSystemMessage(Component.literal("Invalid race selected.").withStyle(ChatFormatting.RED));
            ImmortalCultivation.LOGGER.warn("Player {} tried to select invalid race: {}", player.getName().getString(), packet.stringParam().get());
        }
    }
}
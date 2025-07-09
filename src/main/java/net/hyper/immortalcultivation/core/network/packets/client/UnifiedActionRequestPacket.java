package net.hyper.immortalcultivation.core.network.packets.client;

import io.netty.buffer.ByteBuf;
import net.hyper.immortalcultivation.common.attachments.PlayerBreakthroughAttachment;
import net.hyper.immortalcultivation.common.attachments.PlayerCultivationAttachment;
import net.hyper.immortalcultivation.core.network.NetworkSyncManager;
import net.hyper.immortalcultivation.common.registry.ICModAttachmentTypes;
import net.hyper.immortalcultivation.core.network.helpers.BreakthroughFeedbackHelper;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Optional;

/**
 * Unified action request packet that handles all player-initiated cultivation actions.
 * This replaces individual action packets (BreakthroughRequestPacket, etc.) with a single,
 * extensible system that automatically triggers appropriate sync responses.
 */
public record UnifiedActionRequestPacket(
    ActionType actionType,
    Optional<PlayerCultivationAttachment.CultivationType> cultivationType,
    Optional<PlayerBreakthroughAttachment.BreakthroughType> breakthroughType,
    Optional<String> stringParam,
    Optional<Long> longParam
) implements CustomPacketPayload {

    public static final Type<UnifiedActionRequestPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath("immortalcultivation", "unified_action_request")
    );

    /**
     * Enum defining all supported action types for cultivation system interactions.
     * Each action type specifies what parameters are required and how the action should be processed.
     */
    public enum ActionType {
        BREAKTHROUGH_REQUEST,
        CULTIVATION_FOCUS_TOGGLE,
        FORCE_SYNC_REQUEST,
        EMERGENCY_HEALTH_SYNC
    }

    // Convenience constructors for common actions
    public static UnifiedActionRequestPacket createBreakthroughRequest(
        PlayerCultivationAttachment.CultivationType cultivationType,
        PlayerBreakthroughAttachment.BreakthroughType breakthroughType
    ) {
        return new UnifiedActionRequestPacket(
            ActionType.BREAKTHROUGH_REQUEST,
            Optional.of(cultivationType),
            Optional.of(breakthroughType),
            Optional.empty(),
            Optional.empty()
        );
    }

    public static UnifiedActionRequestPacket createFocusToggle() {
        return new UnifiedActionRequestPacket(
            ActionType.CULTIVATION_FOCUS_TOGGLE,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty()
        );
    }

    public static final StreamCodec<ByteBuf, UnifiedActionRequestPacket> STREAM_CODEC = StreamCodec.of(
        UnifiedActionRequestPacket::encode,
        UnifiedActionRequestPacket::decode
    );

    private static void encode(ByteBuf buf, UnifiedActionRequestPacket packet) {
        ByteBufCodecs.STRING_UTF8.encode(buf, packet.actionType.name());

        // Encode optional cultivation type
        ByteBufCodecs.BOOL.encode(buf, packet.cultivationType.isPresent());
        if (packet.cultivationType.isPresent()) {
            ByteBufCodecs.STRING_UTF8.encode(buf, packet.cultivationType.get().name());
        }

        // Encode optional breakthrough type
        ByteBufCodecs.BOOL.encode(buf, packet.breakthroughType.isPresent());
        if (packet.breakthroughType.isPresent()) {
            ByteBufCodecs.STRING_UTF8.encode(buf, packet.breakthroughType.get().name());
        }

        // Encode optional string parameter
        ByteBufCodecs.BOOL.encode(buf, packet.stringParam.isPresent());
        if (packet.stringParam.isPresent()) {
            ByteBufCodecs.STRING_UTF8.encode(buf, packet.stringParam.get());
        }

        // Encode optional long parameter
        ByteBufCodecs.BOOL.encode(buf, packet.longParam.isPresent());
        if (packet.longParam.isPresent()) {
            ByteBufCodecs.VAR_LONG.encode(buf, packet.longParam.get());
        }
    }

    private static UnifiedActionRequestPacket decode(ByteBuf buf) {
        ActionType actionType = ActionType.valueOf(ByteBufCodecs.STRING_UTF8.decode(buf));

        Optional<PlayerCultivationAttachment.CultivationType> cultivationType = Optional.empty();
        if (ByteBufCodecs.BOOL.decode(buf)) {
            cultivationType = Optional.of(PlayerCultivationAttachment.CultivationType.valueOf(
                ByteBufCodecs.STRING_UTF8.decode(buf)));
        }

        Optional<PlayerBreakthroughAttachment.BreakthroughType> breakthroughType = Optional.empty();
        if (ByteBufCodecs.BOOL.decode(buf)) {
            breakthroughType = Optional.of(PlayerBreakthroughAttachment.BreakthroughType.valueOf(
                ByteBufCodecs.STRING_UTF8.decode(buf)));
        }

        Optional<String> stringParam = Optional.empty();
        if (ByteBufCodecs.BOOL.decode(buf)) {
            stringParam = Optional.of(ByteBufCodecs.STRING_UTF8.decode(buf));
        }

        Optional<Long> longParam = Optional.empty();
        if (ByteBufCodecs.BOOL.decode(buf)) {
            longParam = Optional.of(ByteBufCodecs.VAR_LONG.decode(buf));
        }

        return new UnifiedActionRequestPacket(actionType, cultivationType, breakthroughType, stringParam, longParam);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Handles server-side processing of unified action requests.
     * This method processes the action, performs the requested operation, and automatically
     * triggers appropriate sync responses through the NetworkSyncManager.
     */
    public static void handleServer(UnifiedActionRequestPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                processAction(serverPlayer, packet);
            }
        });
    }

    /**
     * Processes the specific action requested by the client.
     * Each action type is handled appropriately and triggers sync updates through the unified system.
     *
     * @param player The server player requesting the action
     * @param packet The action request packet containing action details
     */
    private static void processAction(ServerPlayer player, UnifiedActionRequestPacket packet) {
        switch (packet.actionType) {
            case BREAKTHROUGH_REQUEST -> processBreakthroughRequest(player, packet);
            case CULTIVATION_FOCUS_TOGGLE -> processFocusToggle(player);
            case FORCE_SYNC_REQUEST -> processForceSync(player);
            case EMERGENCY_HEALTH_SYNC -> processEmergencyHealthSync(player);
        }
    }

    /**
     * Processes breakthrough requests using the existing breakthrough system.
     * Automatically triggers high-priority sync after breakthrough attempt.
     */
    private static void processBreakthroughRequest(ServerPlayer player, UnifiedActionRequestPacket packet) {
        if (packet.cultivationType.isEmpty() || packet.breakthroughType.isEmpty()) {
            return;
        }

        PlayerBreakthroughAttachment breakthrough = player.getData(ICModAttachmentTypes.PLAYER_BREAKTHROUGH.get());
        if (breakthrough == null) {
            return;
        }

        breakthrough.setPlayer(player);

        // Perform the breakthrough attempt
        PlayerBreakthroughAttachment.BreakthroughResult result =
            breakthrough.attemptTypedBreakthrough(packet.cultivationType.get(), packet.breakthroughType.get());

        // Send result feedback (this is immediate, not through unified sync)
        BreakthroughFeedbackHelper.sendBreakthroughFeedback(player, result, packet.breakthroughType.get());

        // Trigger high-priority sync to update client with new cultivation state
        NetworkSyncManager.scheduleSync(player, NetworkSyncManager.SyncPriority.CRITICAL);
    }

    /**
     * Processes cultivation focus toggle requests.
     * Automatically triggers normal priority sync after focus change.
     */
    private static void processFocusToggle(ServerPlayer player) {
        PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
        if (cultivation == null) {
            return;
        }

        cultivation.setPlayer(player);
        cultivation.toggleFocus();

        // Trigger normal priority sync for focus changes
        NetworkSyncManager.scheduleSync(player, NetworkSyncManager.SyncPriority.NORMAL);
    }

    /**
     * Processes force sync requests for debugging or error recovery.
     */
    private static void processForceSync(ServerPlayer player) {
        NetworkSyncManager.scheduleSync(player, NetworkSyncManager.SyncPriority.CRITICAL);
    }

    /**
     * Processes emergency health sync requests for critical health desync recovery.
     */
    private static void processEmergencyHealthSync(ServerPlayer player) {
        // This could trigger specialized health recovery logic if needed
        NetworkSyncManager.scheduleSync(player, NetworkSyncManager.SyncPriority.CRITICAL);
    }
}
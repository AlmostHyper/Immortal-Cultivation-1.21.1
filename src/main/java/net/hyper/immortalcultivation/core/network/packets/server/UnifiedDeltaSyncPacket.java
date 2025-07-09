package net.hyper.immortalcultivation.core.network.packets.server;

import io.netty.buffer.ByteBuf;
import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.attachments.*;
import net.hyper.immortalcultivation.core.cultivation.realms.CultivationRealm;
import net.hyper.immortalcultivation.common.interfaces.ICultivationData;
import net.hyper.immortalcultivation.common.registry.ICModAttachmentTypes;
import net.hyper.immortalcultivation.common.registry.ICModCultivationRealms;
import net.hyper.immortalcultivation.server.event.world.ICModCombatEvent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Optional;

/**
 * Unified delta sync packet that efficiently syncs only changed data across all attachment types.
 * Uses bitmasking, compression, and selective field syncing for optimal performance.
 */
public record UnifiedDeltaSyncPacket(
    // Bitmask indicating which attachment types have updates
    byte attachmentMask,

    // Bitmasks for which fields are included for each attachment type
    short cultivationFieldMask,
    short resourceFieldMask,
    byte combatStatsFieldMask,

    // Cultivation data (only included if bits are set)
    Optional<String> currentQiRealmId,
    Optional<String> currentBodyRealmId,
    Optional<Long> currentQiCultivation,
    Optional<Long> maxQiCultivation,
    Optional<Long> currentBodyCultivation,
    Optional<Long> maxBodyCultivation,
    Optional<PlayerCultivationAttachment.CultivationType> currentFocus,
    Optional<PackedBonuses> bonuses,
    Optional<PackedBreakthroughData> breakthroughData,

    // Resource data (only included if bits are set)
    Optional<Long> currentHealth,
    Optional<Long> currentQi,
    Optional<Long> currentBody,
    Optional<PackedPermanentBonuses> permanentBonuses,

    // Combat stats data (only included if bits are set)
    Optional<PackedCombatStats> combatStats,

    Optional<Long> damageAmount,
    Optional<String> damageSourceId

) implements CustomPacketPayload {

    public static final Type<UnifiedDeltaSyncPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath("immortalcultivation", "unified_delta_sync")
    );

    // Attachment type masks
    public static final byte CULTIVATION_MASK = 1;
    public static final byte RESOURCE_MASK = 2;
    public static final byte COMBAT_STATS_MASK = 4;

    // Cultivation field masks
    public static final short CULT_QI_REALM = 1;
    public static final short CULT_BODY_REALM = 2;
    public static final short CULT_QI_CURRENT = 4;
    public static final short CULT_QI_MAX = 8;
    public static final short CULT_BODY_CURRENT = 16;
    public static final short CULT_BODY_MAX = 32;
    public static final short CULT_FOCUS = 64;
    public static final short CULT_BONUSES = 128;
    public static final short CULT_BREAKTHROUGH = 256;

    // Resource field masks
    public static final short RES_HEALTH = 1;
    public static final short RES_QI = 2;
    public static final short RES_BODY = 4;
    public static final short RES_PERMANENT = 8;

    // Combat stats field mask
    public static final byte COMBAT_ALL = 1;
    private static final byte DAMAGE_FEEDBACK_MASK = 8;
    private static final byte DAMAGE_AMOUNT = 1;
    private static final byte DAMAGE_SOURCE = 2;

    /**
     * Packed cultivation bonuses to reduce network overhead
     */
    public record PackedBonuses(
        double qiAdditiveBonus,
        double qiMultiplicativeBonus,
        double bodyAdditiveBonus,
        double bodyMultiplicativeBonus
    ) {}

    /**
     * Packed breakthrough data using bitmasking for booleans
     */
    public record PackedBreakthroughData(
        long qiBreakthroughAttempts,
        long bodyBreakthroughAttempts,
        byte cooldownFlags, // bit 0: qi cooldown, bit 1: body cooldown
        long lastQiBreakthroughAttempt,
        long lastBodyBreakthroughAttempt
    ) {
        public boolean hasQiCooldown() { return (cooldownFlags & 1) != 0; }
        public boolean hasBodyCooldown() { return (cooldownFlags & 2) != 0; }

        public static PackedBreakthroughData create(long qiAttempts, long bodyAttempts,
                                                    boolean qiCooldown, boolean bodyCooldown,
                                                    long lastQi, long lastBody) {
            byte flags = 0;
            if (qiCooldown) flags |= 1;
            if (bodyCooldown) flags |= 2;
            return new PackedBreakthroughData(qiAttempts, bodyAttempts, flags, lastQi, lastBody);
        }
    }

    /**
     * Packed permanent bonuses
     */
    public record PackedPermanentBonuses(
        long permanentHealthBonus,
        double permanentHealthRegenBonus,
        long permanentMaxQiBonus,
        double permanentQiRegenBonus,
        long permanentMaxBodyBonus,
        double permanentBodyRegenBonus
    ) {}

    /**
     * Packed combat stats
     */
    public record PackedCombatStats(
        double attackBonus,
        double speedBonus,
        double jumpBonus,
        double permanentAttackBonus,
        double permanentSpeedBonus,
        double permanentJumpBonus
    ) {}

    // Custom codec implementation for optimal performance
    public static final StreamCodec<ByteBuf, UnifiedDeltaSyncPacket> STREAM_CODEC = StreamCodec.of(
        UnifiedDeltaSyncPacket::encode,
        UnifiedDeltaSyncPacket::decode
    );

    private static void encode(ByteBuf buf, UnifiedDeltaSyncPacket packet) {
        // Write bitmasks
        ByteBufCodecs.BYTE.encode(buf, packet.attachmentMask);

        if ((packet.attachmentMask & CULTIVATION_MASK) != 0) {
            ByteBufCodecs.SHORT.encode(buf, packet.cultivationFieldMask);

            // Encode cultivation fields based on mask
            if ((packet.cultivationFieldMask & CULT_QI_REALM) != 0) {
                ByteBufCodecs.STRING_UTF8.encode(buf, packet.currentQiRealmId.orElse(""));
            }
            if ((packet.cultivationFieldMask & CULT_BODY_REALM) != 0) {
                ByteBufCodecs.STRING_UTF8.encode(buf, packet.currentBodyRealmId.orElse(""));
            }
            if ((packet.cultivationFieldMask & CULT_QI_CURRENT) != 0) {
                ByteBufCodecs.VAR_LONG.encode(buf, packet.currentQiCultivation.orElse(0L));
            }
            if ((packet.cultivationFieldMask & CULT_QI_MAX) != 0) {
                ByteBufCodecs.VAR_LONG.encode(buf, packet.maxQiCultivation.orElse(0L));
            }
            if ((packet.cultivationFieldMask & CULT_BODY_CURRENT) != 0) {
                ByteBufCodecs.VAR_LONG.encode(buf, packet.currentBodyCultivation.orElse(0L));
            }
            if ((packet.cultivationFieldMask & CULT_BODY_MAX) != 0) {
                ByteBufCodecs.VAR_LONG.encode(buf, packet.maxBodyCultivation.orElse(0L));
            }
            if ((packet.cultivationFieldMask & CULT_FOCUS) != 0) {
                ByteBufCodecs.STRING_UTF8.encode(buf, packet.currentFocus.map(Enum::name).orElse("QI"));
            }
            if ((packet.cultivationFieldMask & CULT_BONUSES) != 0) {
                PackedBonuses bonuses = packet.bonuses.orElse(new PackedBonuses(0, 0, 0, 0));
                ByteBufCodecs.DOUBLE.encode(buf, bonuses.qiAdditiveBonus);
                ByteBufCodecs.DOUBLE.encode(buf, bonuses.qiMultiplicativeBonus);
                ByteBufCodecs.DOUBLE.encode(buf, bonuses.bodyAdditiveBonus);
                ByteBufCodecs.DOUBLE.encode(buf, bonuses.bodyMultiplicativeBonus);
            }
            if ((packet.cultivationFieldMask & CULT_BREAKTHROUGH) != 0) {
                PackedBreakthroughData bt = packet.breakthroughData.orElse(
                    new PackedBreakthroughData(0, 0, (byte)0, 0, 0));
                ByteBufCodecs.VAR_LONG.encode(buf, bt.qiBreakthroughAttempts);
                ByteBufCodecs.VAR_LONG.encode(buf, bt.bodyBreakthroughAttempts);
                ByteBufCodecs.BYTE.encode(buf, bt.cooldownFlags);
                ByteBufCodecs.VAR_LONG.encode(buf, bt.lastQiBreakthroughAttempt);
                ByteBufCodecs.VAR_LONG.encode(buf, bt.lastBodyBreakthroughAttempt);
            }
        }

        if ((packet.attachmentMask & RESOURCE_MASK) != 0) {
            ByteBufCodecs.SHORT.encode(buf, packet.resourceFieldMask);

            if ((packet.resourceFieldMask & RES_HEALTH) != 0) {
                ByteBufCodecs.VAR_LONG.encode(buf, packet.currentHealth.orElse(0L));
            }
            if ((packet.resourceFieldMask & RES_QI) != 0) {
                ByteBufCodecs.VAR_LONG.encode(buf, packet.currentQi.orElse(0L));
            }
            if ((packet.resourceFieldMask & RES_BODY) != 0) {
                ByteBufCodecs.VAR_LONG.encode(buf, packet.currentBody.orElse(0L));
            }
            if ((packet.resourceFieldMask & RES_PERMANENT) != 0) {
                PackedPermanentBonuses pb = packet.permanentBonuses.orElse(
                    new PackedPermanentBonuses(0, 0, 0, 0, 0, 0));
                ByteBufCodecs.VAR_LONG.encode(buf, pb.permanentHealthBonus);
                ByteBufCodecs.DOUBLE.encode(buf, pb.permanentHealthRegenBonus);
                ByteBufCodecs.VAR_LONG.encode(buf, pb.permanentMaxQiBonus);
                ByteBufCodecs.DOUBLE.encode(buf, pb.permanentQiRegenBonus);
                ByteBufCodecs.VAR_LONG.encode(buf, pb.permanentMaxBodyBonus);
                ByteBufCodecs.DOUBLE.encode(buf, pb.permanentBodyRegenBonus);
            }
        }

        if ((packet.attachmentMask & COMBAT_STATS_MASK) != 0) {
            ByteBufCodecs.BYTE.encode(buf, packet.combatStatsFieldMask);

            if ((packet.combatStatsFieldMask & COMBAT_ALL) != 0) {
                PackedCombatStats cs = packet.combatStats.orElse(
                    new PackedCombatStats(0, 0, 0, 0, 0, 0));
                ByteBufCodecs.DOUBLE.encode(buf, cs.attackBonus);
                ByteBufCodecs.DOUBLE.encode(buf, cs.speedBonus);
                ByteBufCodecs.DOUBLE.encode(buf, cs.jumpBonus);
                ByteBufCodecs.DOUBLE.encode(buf, cs.permanentAttackBonus);
                ByteBufCodecs.DOUBLE.encode(buf, cs.permanentSpeedBonus);
                ByteBufCodecs.DOUBLE.encode(buf, cs.permanentJumpBonus);
            }
        }
    }

    private static UnifiedDeltaSyncPacket decode(ByteBuf buf) {
        byte attachmentMask = ByteBufCodecs.BYTE.decode(buf);

        short cultivationFieldMask = 0;
        short resourceFieldMask = 0;
        byte combatStatsFieldMask = 0;

        // Initialize all optionals as empty
        Optional<String> currentQiRealmId = Optional.empty();
        Optional<String> currentBodyRealmId = Optional.empty();
        Optional<Long> currentQiCultivation = Optional.empty();
        Optional<Long> maxQiCultivation = Optional.empty();
        Optional<Long> currentBodyCultivation = Optional.empty();
        Optional<Long> maxBodyCultivation = Optional.empty();
        Optional<PlayerCultivationAttachment.CultivationType> currentFocus = Optional.empty();
        Optional<PackedBonuses> bonuses = Optional.empty();
        Optional<PackedBreakthroughData> breakthroughData = Optional.empty();

        Optional<Long> currentHealth = Optional.empty();
        Optional<Long> currentQi = Optional.empty();
        Optional<Long> currentBody = Optional.empty();
        Optional<PackedPermanentBonuses> permanentBonuses = Optional.empty();

        Optional<PackedCombatStats> combatStats = Optional.empty();

        Optional<Long> damageAmount = Optional.empty();
        Optional<String> damageSourceId = Optional.empty();

        // Decode cultivation data
        if ((attachmentMask & CULTIVATION_MASK) != 0) {
            cultivationFieldMask = ByteBufCodecs.SHORT.decode(buf);

            if ((cultivationFieldMask & CULT_QI_REALM) != 0) {
                currentQiRealmId = Optional.of(ByteBufCodecs.STRING_UTF8.decode(buf));
            }
            if ((cultivationFieldMask & CULT_BODY_REALM) != 0) {
                currentBodyRealmId = Optional.of(ByteBufCodecs.STRING_UTF8.decode(buf));
            }
            if ((cultivationFieldMask & CULT_QI_CURRENT) != 0) {
                currentQiCultivation = Optional.of(ByteBufCodecs.VAR_LONG.decode(buf));
            }
            if ((cultivationFieldMask & CULT_QI_MAX) != 0) {
                maxQiCultivation = Optional.of(ByteBufCodecs.VAR_LONG.decode(buf));
            }
            if ((cultivationFieldMask & CULT_BODY_CURRENT) != 0) {
                currentBodyCultivation = Optional.of(ByteBufCodecs.VAR_LONG.decode(buf));
            }
            if ((cultivationFieldMask & CULT_BODY_MAX) != 0) {
                maxBodyCultivation = Optional.of(ByteBufCodecs.VAR_LONG.decode(buf));
            }
            if ((cultivationFieldMask & CULT_FOCUS) != 0) {
                try {
                    currentFocus = Optional.of(PlayerCultivationAttachment.CultivationType.valueOf(
                        ByteBufCodecs.STRING_UTF8.decode(buf)));
                } catch (IllegalArgumentException e) {
                    currentFocus = Optional.of(PlayerCultivationAttachment.CultivationType.QI);
                }
            }
            if ((cultivationFieldMask & CULT_BONUSES) != 0) {
                bonuses = Optional.of(new PackedBonuses(
                    ByteBufCodecs.DOUBLE.decode(buf),
                    ByteBufCodecs.DOUBLE.decode(buf),
                    ByteBufCodecs.DOUBLE.decode(buf),
                    ByteBufCodecs.DOUBLE.decode(buf)
                ));
            }
            if ((cultivationFieldMask & CULT_BREAKTHROUGH) != 0) {
                breakthroughData = Optional.of(new PackedBreakthroughData(
                    ByteBufCodecs.VAR_LONG.decode(buf),
                    ByteBufCodecs.VAR_LONG.decode(buf),
                    ByteBufCodecs.BYTE.decode(buf),
                    ByteBufCodecs.VAR_LONG.decode(buf),
                    ByteBufCodecs.VAR_LONG.decode(buf)
                ));
            }
        }

        // Decode resource data
        if ((attachmentMask & RESOURCE_MASK) != 0) {
            resourceFieldMask = ByteBufCodecs.SHORT.decode(buf);

            if ((resourceFieldMask & RES_HEALTH) != 0) {
                currentHealth = Optional.of(ByteBufCodecs.VAR_LONG.decode(buf));
            }
            if ((resourceFieldMask & RES_QI) != 0) {
                currentQi = Optional.of(ByteBufCodecs.VAR_LONG.decode(buf));
            }
            if ((resourceFieldMask & RES_BODY) != 0) {
                currentBody = Optional.of(ByteBufCodecs.VAR_LONG.decode(buf));
            }
            if ((resourceFieldMask & RES_PERMANENT) != 0) {
                permanentBonuses = Optional.of(new PackedPermanentBonuses(
                    ByteBufCodecs.VAR_LONG.decode(buf),
                    ByteBufCodecs.DOUBLE.decode(buf),
                    ByteBufCodecs.VAR_LONG.decode(buf),
                    ByteBufCodecs.DOUBLE.decode(buf),
                    ByteBufCodecs.VAR_LONG.decode(buf),
                    ByteBufCodecs.DOUBLE.decode(buf)
                ));
            }
        }

        // Decode combat stats data
        if ((attachmentMask & COMBAT_STATS_MASK) != 0) {
            combatStatsFieldMask = ByteBufCodecs.BYTE.decode(buf);

            if ((combatStatsFieldMask & COMBAT_ALL) != 0) {
                combatStats = Optional.of(new PackedCombatStats(
                    ByteBufCodecs.DOUBLE.decode(buf),
                    ByteBufCodecs.DOUBLE.decode(buf),
                    ByteBufCodecs.DOUBLE.decode(buf),
                    ByteBufCodecs.DOUBLE.decode(buf),
                    ByteBufCodecs.DOUBLE.decode(buf),
                    ByteBufCodecs.DOUBLE.decode(buf)
                ));
            }
        }

        // Decode damage feedback data
        if ((attachmentMask & DAMAGE_FEEDBACK_MASK) != 0) {
            byte damageFieldMask = ByteBufCodecs.BYTE.decode(buf);

            if ((damageFieldMask & DAMAGE_AMOUNT) != 0) {
                damageAmount = Optional.of(ByteBufCodecs.VAR_LONG.decode(buf));
            }
            if ((damageFieldMask & DAMAGE_SOURCE) != 0) {
                damageSourceId = Optional.of(ByteBufCodecs.STRING_UTF8.decode(buf));
            }
        }

        return new UnifiedDeltaSyncPacket(
            attachmentMask, cultivationFieldMask, resourceFieldMask, combatStatsFieldMask,
            currentQiRealmId, currentBodyRealmId, currentQiCultivation, maxQiCultivation,
            currentBodyCultivation, maxBodyCultivation, currentFocus, bonuses, breakthroughData,
            currentHealth, currentQi, currentBody, permanentBonuses, combatStats,
            damageAmount, damageSourceId
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleClient(UnifiedDeltaSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player != null) {
                packet.applyDeltaSync(player);
            }
        });
    }

    /**
     * Apply delta sync changes to client-side attachments.
     * Only updates fields that are marked as changed in the bitmasks.
     */
    private void applyDeltaSync(Player player) {
        try {
            // Apply cultivation changes
            if ((attachmentMask & CULTIVATION_MASK) != 0) {
                PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
                if (cultivation != null) {
                    applyCultivationDelta(cultivation);
                }
            }

            // Apply resource changes
            if ((attachmentMask & RESOURCE_MASK) != 0) {
                PlayerResourceAttachment resources = player.getData(ICModAttachmentTypes.PLAYER_RESOURCES.get());
                if (resources != null) {
                    applyResourceDelta(resources);
                }
            }

            // Apply combat stats changes
            if ((attachmentMask & COMBAT_STATS_MASK) != 0) {
                PlayerCombatStatsAttachment combatStats = player.getData(ICModAttachmentTypes.PLAYER_COMBAT_STATS.get());
                if (combatStats != null) {
                    applyCombatStatsDelta(combatStats);
                }
            }

            // Apply damage feedback effects
            if ((attachmentMask & DAMAGE_FEEDBACK_MASK) != 0) {
                applyDamageFeedback(player);
            }

            ImmortalCultivation.LOGGER.debug("Applied unified delta sync for player: {}", player.getName().getString());

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error applying unified delta sync for player {}: {}",
                player.getName().getString(), e.getMessage());
        }
    }

    /**
     * Apply cultivation-specific delta changes
     */
    private void applyCultivationDelta(PlayerCultivationAttachment cultivation) {
        // Realm changes - need to look up realm by ID, not set ID directly
        if ((cultivationFieldMask & CULT_QI_REALM) != 0 && currentQiRealmId.isPresent()) {
            CultivationRealm qiRealm = ICModCultivationRealms.get(currentQiRealmId.get());
            if (qiRealm != null) {
                cultivation.setCurrentRealm(ICultivationData.CultivationType.QI, qiRealm);
            }
        }

        if ((cultivationFieldMask & CULT_BODY_REALM) != 0 && currentBodyRealmId.isPresent()) {
            CultivationRealm bodyRealm = ICModCultivationRealms.get(currentBodyRealmId.get());
            if (bodyRealm != null) {
                cultivation.setCurrentRealm(ICultivationData.CultivationType.BODY, bodyRealm);
            }
        }

        // Cultivation progress changes - use generic interface methods
        if ((cultivationFieldMask & CULT_QI_CURRENT) != 0 && currentQiCultivation.isPresent()) {
            cultivation.setCurrentCultivation(ICultivationData.CultivationType.QI, currentQiCultivation.get());
        }

        if ((cultivationFieldMask & CULT_QI_MAX) != 0 && maxQiCultivation.isPresent()) {
            // Note: You may need to add setMaxCultivation to ICultivationData interface if it doesn't exist
            cultivation.setMaxCultivation(ICultivationData.CultivationType.QI, maxQiCultivation.get());
        }

        if ((cultivationFieldMask & CULT_BODY_CURRENT) != 0 && currentBodyCultivation.isPresent()) {
            cultivation.setCurrentCultivation(ICultivationData.CultivationType.BODY, currentBodyCultivation.get());
        }

        if ((cultivationFieldMask & CULT_BODY_MAX) != 0 && maxBodyCultivation.isPresent()) {
            cultivation.setMaxCultivation(ICultivationData.CultivationType.BODY, maxBodyCultivation.get());
        }

        // Focus changes
        if ((cultivationFieldMask & CULT_FOCUS) != 0 && currentFocus.isPresent()) {
            cultivation.setCurrentFocus(currentFocus.get());
        }

        // Bonus changes - use generic methods
        if ((cultivationFieldMask & CULT_BONUSES) != 0 && bonuses.isPresent()) {
            PackedBonuses packedBonuses = bonuses.get();
            cultivation.setRawAdditiveBonus(ICultivationData.CultivationType.QI, packedBonuses.qiAdditiveBonus());
            cultivation.setRawMultiplicativeBonus(ICultivationData.CultivationType.QI, packedBonuses.qiMultiplicativeBonus());
            cultivation.setRawAdditiveBonus(ICultivationData.CultivationType.BODY, packedBonuses.bodyAdditiveBonus());
            cultivation.setRawMultiplicativeBonus(ICultivationData.CultivationType.BODY, packedBonuses.bodyMultiplicativeBonus());
        }

        // Breakthrough data changes - access through breakthrough attachment, not cultivation
        if ((cultivationFieldMask & CULT_BREAKTHROUGH) != 0 && breakthroughData.isPresent()) {
            Player player = cultivation.getPlayerSafe();
            if (player != null) {
                PlayerBreakthroughAttachment breakthrough = player.getData(ICModAttachmentTypes.PLAYER_BREAKTHROUGH.get());
                if (breakthrough != null) {
                    PackedBreakthroughData btData = breakthroughData.get();
                    breakthrough.setBreakthroughAttempts(PlayerCultivationAttachment.CultivationType.QI, btData.qiBreakthroughAttempts());
                    breakthrough.setBreakthroughAttempts(PlayerCultivationAttachment.CultivationType.BODY, btData.bodyBreakthroughAttempts());
                    breakthrough.setBreakthroughCooldown(PlayerCultivationAttachment.CultivationType.QI, btData.hasQiCooldown());
                    breakthrough.setBreakthroughCooldown(PlayerCultivationAttachment.CultivationType.BODY, btData.hasBodyCooldown());
                    breakthrough.setLastBreakthroughAttempt(PlayerCultivationAttachment.CultivationType.QI, btData.lastQiBreakthroughAttempt());
                    breakthrough.setLastBreakthroughAttempt(PlayerCultivationAttachment.CultivationType.BODY, btData.lastBodyBreakthroughAttempt());
                }
            }
        }
    }

    /**
     * Apply resource-specific delta changes
     */
    private void applyResourceDelta(PlayerResourceAttachment resources) {
        // Current resource values
        if ((resourceFieldMask & RES_HEALTH) != 0 && currentHealth.isPresent()) {
            resources.setCurrentHealth(currentHealth.get());
        }

        if ((resourceFieldMask & RES_QI) != 0 && currentQi.isPresent()) {
            resources.setCurrentQi(currentQi.get());
        }

        if ((resourceFieldMask & RES_BODY) != 0 && currentBody.isPresent()) {
            resources.setCurrentBody(currentBody.get());
        }

        // Permanent bonuses
        if ((resourceFieldMask & RES_PERMANENT) != 0 && permanentBonuses.isPresent()) {
            PackedPermanentBonuses packed = permanentBonuses.get();
            resources.setPermanentHealthBonus(packed.permanentHealthBonus());
            resources.setPermanentHealthRegenBonus(packed.permanentHealthRegenBonus());
            resources.setPermanentMaxQiBonus(packed.permanentMaxQiBonus());
            resources.setPermanentQiRegenBonus(packed.permanentQiRegenBonus());
            resources.setPermanentMaxBodyBonus(packed.permanentMaxBodyBonus());
            resources.setPermanentBodyRegenBonus(packed.permanentBodyRegenBonus());
        }
    }

    /**
     * Apply combat stats-specific delta changes
     */
    private void applyCombatStatsDelta(PlayerCombatStatsAttachment combatStats) {
        if ((combatStatsFieldMask & COMBAT_ALL) != 0 && this.combatStats.isPresent()) {
            PackedCombatStats packed = this.combatStats.get();

            combatStats.setAttackDamageBonus(packed.attackBonus());
            combatStats.setMovementSpeedBonus(packed.speedBonus());
            combatStats.setJumpHeightBonus(packed.jumpBonus());
            combatStats.setPermanentAttackDamageBonus(packed.permanentAttackBonus());
            combatStats.setPermanentMovementSpeedBonus(packed.permanentSpeedBonus());
            combatStats.setPermanentJumpHeightBonus(packed.permanentJumpBonus());
        }
    }

    /**
     * Apply damage feedback effects by delegating to the existing ICModCombatEvent system.
     */
    private void applyDamageFeedback(Player player) {
        if (damageAmount.isPresent() && damageAmount.get() > 0) {
            // Delegate to the existing combat event system for consistent feedback
            net.hyper.immortalcultivation.server.event.world.ICModCombatEvent.applyClientSideFeedback(
                player,
                damageAmount.get()
            );

            ImmortalCultivation.LOGGER.debug("Applied unified damage feedback for {} damage to player: {}",
                damageAmount.get(), player.getName().getString());
        }
    }

    /**
     * Creates a UnifiedDeltaSyncPacket containing only damage feedback data.
     * This factory method simplifies sending damage feedback through the unified system
     * without requiring all other attachment data.
     *
     * @param damageAmount The amount of damage taken
     * @param damageSourceId The ID of the damage source
     * @return A UnifiedDeltaSyncPacket configured for damage feedback only
     */
    public static UnifiedDeltaSyncPacket createDamageFeedbackPacket(long damageAmount, String damageSourceId) {
        return new UnifiedDeltaSyncPacket(
            DAMAGE_FEEDBACK_MASK, // Only damage feedback mask is set
            (short) 0, // No cultivation fields
            (short) 0, // No resource fields
            (byte) 0,  // No combat stats fields
            Optional.empty(), // No cultivation data
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(), // No resource data
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(), // No combat stats
            Optional.of(damageAmount), // Damage amount
            Optional.of(damageSourceId) // Damage source
        );
    }
}
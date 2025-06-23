package net.hyper.immortalcultivation.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.network.CultivationBonusSyncPacket;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.hyper.immortalcultivation.registry.ICModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * Manages permanent cultivation rate bonuses from consumed pills and other sources.
 * These bonuses stack additively with no limits - players can consume unlimited pills.
 */
public class PlayerCultivationBonusAttachment {

    // Permanent additive bonuses from pills (these stack additively with base rate)
    private double qiCultivationAdditiveBonus = 0.0; // Direct addition to base rate
    private double bodyCultivationAdditiveBonus = 0.0; // Direct addition to base rate

    // Permanent multiplicative bonuses (for future items that provide percentage increases)
    private double qiCultivationMultiplicativeBonus = 0.0; // Additional multiplier (0.1 = +10% rate)
    private double bodyCultivationMultiplicativeBonus = 0.0; // Additional multiplier (0.1 = +10% rate)

    // No pill consumption limits - players can consume as many pills as they want

    // Sync management
    private int tickCounter = 0;
    private double lastSyncedQiAdditiveBonus = 0.0;
    private double lastSyncedBodyAdditiveBonus = 0.0;
    private double lastSyncedQiMultiplicativeBonus = 0.0;
    private double lastSyncedBodyMultiplicativeBonus = 0.0;
    private static final int PERIODIC_SYNC_TICKS = 400; // Sync every 20 seconds (bonuses change rarely)

    public PlayerCultivationBonusAttachment() {}

    // === Player Reference Management ===

    private Player findPlayer() {
        // Client side
        if (Minecraft.getInstance().level != null && Minecraft.getInstance().player != null) {
            Player clientPlayer = Minecraft.getInstance().player;
            if (clientPlayer.getData(ICModAttachmentTypes.PLAYER_CULTIVATION_BONUS.get()) == this) {
                return clientPlayer;
            }
        }

        // Server side
        if (ServerLifecycleHooks.getCurrentServer() != null) {
            for (ServerPlayer serverPlayer : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
                PlayerCultivationBonusAttachment attachment = serverPlayer.getData(ICModAttachmentTypes.PLAYER_CULTIVATION_BONUS.get());
                if (attachment != null && attachment == this) {
                    return serverPlayer;
                }
            }
        }

        return null;
    }

    private Player getPlayerSafe() {
        Player player = findPlayer();
        if (player == null) {
            ImmortalCultivation.LOGGER.debug("Could not find player for CultivationBonusAttachment");
        }
        return player;
    }

    // === Bonus Getters ===

    /**
     * Get the additive Qi cultivation rate bonus (added to base rate)
     * @return Amount to add to base Qi cultivation rate
     */
    public double getQiCultivationAdditiveBonus() {
        return qiCultivationAdditiveBonus;
    }

    /**
     * Get the multiplicative Qi cultivation rate bonus (multiplied with base rate)
     * @return Multiplier to apply to base Qi cultivation rate (1.0 + bonus)
     */
    public double getQiCultivationMultiplicativeBonus() {
        return 1.0 + qiCultivationMultiplicativeBonus;
    }

    /**
     * Get the additive Body cultivation rate bonus (added to base rate)
     * @return Amount to add to base Body cultivation rate
     */
    public double getBodyCultivationAdditiveBonus() {
        return bodyCultivationAdditiveBonus;
    }

    /**
     * Get the multiplicative Body cultivation rate bonus (multiplied with base rate)
     * @return Multiplier to apply to base Body cultivation rate (1.0 + bonus)
     */
    public double getBodyCultivationMultiplicativeBonus() {
        return 1.0 + bodyCultivationMultiplicativeBonus;
    }

    /**
     * Calculate the final effective Qi cultivation rate with all bonuses applied
     * Formula: (baseRate + additiveBonus) * multiplicativeBonus
     * @param baseRate The base cultivation rate from realm/meditation
     * @return Final effective rate with all bonuses
     */
    public double applyQiCultivationBonuses(double baseRate) {
        return (baseRate + qiCultivationAdditiveBonus) * (1.0 + qiCultivationMultiplicativeBonus);
    }

    /**
     * Calculate the final effective Body cultivation rate with all bonuses applied
     * Formula: (baseRate + additiveBonus) * multiplicativeBonus
     * @param baseRate The base cultivation rate from realm/meditation
     * @return Final effective rate with all bonuses
     */
    public double applyBodyCultivationBonuses(double baseRate) {
        return (baseRate + bodyCultivationAdditiveBonus) * (1.0 + bodyCultivationMultiplicativeBonus);
    }

    /**
     * Get the raw Qi additive bonus for display
     * @return Additive bonus amount
     */
    public double getQiCultivationRawAdditiveBonus() {
        return qiCultivationAdditiveBonus;
    }

    /**
     * Get the raw Body additive bonus for display
     * @return Additive bonus amount
     */
    public double getBodyCultivationRawAdditiveBonus() {
        return bodyCultivationAdditiveBonus;
    }

    /**
     * Get the raw Qi multiplicative bonus percentage for display
     * @return Multiplicative bonus as percentage (0.1 = 10%)
     */
    public double getQiCultivationRawMultiplicativeBonus() {
        return qiCultivationMultiplicativeBonus;
    }

    /**
     * Get the raw Body multiplicative bonus percentage for display
     * @return Multiplicative bonus as percentage (0.1 = 10%)
     */
    public double getBodyCultivationRawMultiplicativeBonus() {
        return bodyCultivationMultiplicativeBonus;
    }

    // === Pill Consumption Methods ===

    /**
     * Attempt to consume a Qi cultivation pill
     * @param tier The tier of pill being consumed
     * @return true if successful, false if at limit
     */
    public boolean consumeQiCultivationPill(PillTier tier) {

        // Add the pill's additive bonus
        double bonusToAdd = tier.getQiAdditiveBonus();
        qiCultivationAdditiveBonus += bonusToAdd;

        // Log and sync
        Player player = getPlayerSafe();
        if (player != null) {
            ImmortalCultivation.LOGGER.info("Player {} consumed {} Qi cultivation pill. Added: {} rate/sec (Total additive: {})",
                player.getName().getString(), tier.name(),
                String.format("%.3f", bonusToAdd),
                String.format("%.3f", qiCultivationAdditiveBonus));

            if (!player.level().isClientSide()) {
                syncToClient();
            }
        }

        return true;
    }

    /**
     * Attempt to consume a Body cultivation pill
     * @param tier The tier of pill being consumed
     * @return true if successful, false if at limit
     */
    public boolean consumeBodyCultivationPill(PillTier tier) {

        // Add the pill's additive bonus
        double bonusToAdd = tier.getBodyAdditiveBonus();
        bodyCultivationAdditiveBonus += bonusToAdd;

        // Log and sync
        Player player = getPlayerSafe();
        if (player != null) {
            ImmortalCultivation.LOGGER.info("Player {} consumed {} Body cultivation pill. Added: {} rate/sec (Total additive: {})",
                player.getName().getString(), tier.name(),
                String.format("%.3f", bonusToAdd),
                String.format("%.3f", bodyCultivationAdditiveBonus));

            if (!player.level().isClientSide()) {
                syncToClient();
            }
        }

        return true;
    }

    // === Pill Tier Enum ===

    public enum PillTier {
        LOW_QUALITY(0.001, 0.001), // +0.001 Qi rate, +0.001 Body rate
        MEDIUM_QUALITY(0.01, 0.01), // +0.01 Qi rate, +0.01 Body rate
        HIGH_QUALITY(0.1, 0.1), // +0.1 Qi rate, +0.1 Body rate
        TOP_QUALITY(1.0, 1.0); // +1.0 Qi rate, +1.0 Body rate

        private final double qiAdditiveBonus;
        private final double bodyAdditiveBonus;

        PillTier(double qiAdditiveBonus, double bodyAdditiveBonus) {
            this.qiAdditiveBonus = qiAdditiveBonus;
            this.bodyAdditiveBonus = bodyAdditiveBonus;
        }

        public double getQiAdditiveBonus() {
            return qiAdditiveBonus;
        }

        public double getBodyAdditiveBonus() {
            return bodyAdditiveBonus;
        }

        public String getDisplayName() {
            return switch (this) {
                case LOW_QUALITY -> "Low Quality";
                case MEDIUM_QUALITY -> "Medium Quality";
                case HIGH_QUALITY -> "High Quality";
                case TOP_QUALITY -> "Top Quality";
            };
        }

        /**
         * Get a user-friendly description of the bonus amount
         */
        public String getQiBonusDescription() {
            if (qiAdditiveBonus >= 1.0) {
                return "+" + String.format("%.1f", qiAdditiveBonus) + "/sec";
            } else if (qiAdditiveBonus >= 0.1) {
                return "+" + String.format("%.1f", qiAdditiveBonus) + "/sec";
            } else if (qiAdditiveBonus >= 0.01) {
                return "+" + String.format("%.2f", qiAdditiveBonus) + "/sec";
            } else {
                return "+" + String.format("%.3f", qiAdditiveBonus) + "/sec";
            }
        }

        /**
         * Get a user-friendly description of the bonus amount
         */
        public String getBodyBonusDescription() {
            if (bodyAdditiveBonus >= 1.0) {
                return "+" + String.format("%.1f", bodyAdditiveBonus) + "/sec";
            } else if (bodyAdditiveBonus >= 0.1) {
                return "+" + String.format("%.1f", bodyAdditiveBonus) + "/sec";
            } else if (bodyAdditiveBonus >= 0.01) {
                return "+" + String.format("%.2f", bodyAdditiveBonus) + "/sec";
            } else {
                return "+" + String.format("%.3f", bodyAdditiveBonus) + "/sec";
            }
        }
    }

    // === Tick and Sync Methods ===

    public void tick() {
        tickCounter++;

        Player player = getPlayerSafe();
        if (player == null) {
            if (tickCounter % 400 == 0) { // Log every 20 seconds
                ImmortalCultivation.LOGGER.debug("CultivationBonusAttachment ticking without player reference (tick {})", tickCounter);
            }
            return;
        }

        // Periodic sync check
        if (!player.level().isClientSide() && tickCounter % PERIODIC_SYNC_TICKS == 0) {
            if (shouldSync()) {
                syncToClient();
            }
        }
    }

    private boolean shouldSync() {
        return Math.abs(qiCultivationAdditiveBonus - lastSyncedQiAdditiveBonus) > 0.0001 ||
            Math.abs(bodyCultivationAdditiveBonus - lastSyncedBodyAdditiveBonus) > 0.0001 ||
            Math.abs(qiCultivationMultiplicativeBonus - lastSyncedQiMultiplicativeBonus) > 0.001 ||
            Math.abs(bodyCultivationMultiplicativeBonus - lastSyncedBodyMultiplicativeBonus) > 0.001;
    }

    private void syncToClient() {
        Player player = getPlayerSafe();
        if (player instanceof ServerPlayer serverPlayer) {
            lastSyncedQiAdditiveBonus = qiCultivationAdditiveBonus;
            lastSyncedBodyAdditiveBonus = bodyCultivationAdditiveBonus;
            lastSyncedQiMultiplicativeBonus = qiCultivationMultiplicativeBonus;
            lastSyncedBodyMultiplicativeBonus = bodyCultivationMultiplicativeBonus;
            try {
                // Encode all bonuses for sync - we'll focus on additive bonuses for now since pills only use those
                long encodedBonuses = encodeAllBonuses(qiCultivationAdditiveBonus, bodyCultivationAdditiveBonus,
                    qiCultivationMultiplicativeBonus, bodyCultivationMultiplicativeBonus);
                ICModNetwork.sendToPlayer(new CultivationBonusSyncPacket(encodedBonuses), serverPlayer);
            } catch (Exception e) {
                ImmortalCultivation.LOGGER.error("Error syncing cultivation bonuses to client", e);
            }
        }
    }

    public void forceSync() {
        syncToClient();
    }

    /**
     * Encode all four bonus values into a single long for sync packet
     * This is a simple implementation - for more complex data, consider using a custom packet
     */
    private long encodeAllBonuses(double qiAdditive, double bodyAdditive, double qiMultiplicative, double bodyMultiplicative) {
        // For now, we'll encode the additive bonuses primarily since they're what pills use
        // Convert to fixed-point representation (multiply by 100000 to preserve 5 decimal places)
        int qiAdditiveFixed = (int)(qiAdditive * 100000);
        int bodyAdditiveFixed = (int)(bodyAdditive * 100000);

        // Pack into long (32 bits each) - we'll extend this later for multiplicative bonuses if needed
        return ((long)qiAdditiveFixed << 32) | (bodyAdditiveFixed & 0xFFFFFFFFL);
    }

    /**
     * Decode bonuses from sync packet
     */
    private void decodeAllBonuses(long encodedBonuses) {
        int qiAdditiveFixed = (int)(encodedBonuses >>> 32);
        int bodyAdditiveFixed = (int)(encodedBonuses & 0xFFFFFFFFL);

        this.qiCultivationAdditiveBonus = qiAdditiveFixed / 100000.0;
        this.bodyCultivationAdditiveBonus = bodyAdditiveFixed / 100000.0;

        // For now, multiplicative bonuses are not synced - extend this when adding multiplicative pill types
        // this.qiCultivationMultiplicativeBonus = ...;
        // this.bodyCultivationMultiplicativeBonus = ...;
    }

    public void updateFromServer(long encodedBonuses) {
        decodeAllBonuses(encodedBonuses);
        lastSyncedQiAdditiveBonus = qiCultivationAdditiveBonus;
        lastSyncedBodyAdditiveBonus = bodyCultivationAdditiveBonus;
        // lastSyncedQiMultiplicativeBonus and lastSyncedBodyMultiplicativeBonus remain unchanged for now
    }

    // === Serialization ===

    public void deserializeNBT(CompoundTag tag) {
        this.qiCultivationAdditiveBonus = tag.getDouble("qiCultivationAdditiveBonus");
        this.bodyCultivationAdditiveBonus = tag.getDouble("bodyCultivationAdditiveBonus");
        this.qiCultivationMultiplicativeBonus = tag.getDouble("qiCultivationMultiplicativeBonus");
        this.bodyCultivationMultiplicativeBonus = tag.getDouble("bodyCultivationMultiplicativeBonus");
        this.tickCounter = tag.getInt("tickCounter");
        this.lastSyncedQiAdditiveBonus = tag.getDouble("lastSyncedQiAdditiveBonus");
        this.lastSyncedBodyAdditiveBonus = tag.getDouble("lastSyncedBodyAdditiveBonus");
        this.lastSyncedQiMultiplicativeBonus = tag.getDouble("lastSyncedQiMultiplicativeBonus");
        this.lastSyncedBodyMultiplicativeBonus = tag.getDouble("lastSyncedBodyMultiplicativeBonus");
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("qiCultivationAdditiveBonus", qiCultivationAdditiveBonus);
        tag.putDouble("bodyCultivationAdditiveBonus", bodyCultivationAdditiveBonus);
        tag.putDouble("qiCultivationMultiplicativeBonus", qiCultivationMultiplicativeBonus);
        tag.putDouble("bodyCultivationMultiplicativeBonus", bodyCultivationMultiplicativeBonus);
        tag.putInt("tickCounter", tickCounter);
        tag.putDouble("lastSyncedQiAdditiveBonus", lastSyncedQiAdditiveBonus);
        tag.putDouble("lastSyncedBodyAdditiveBonus", lastSyncedBodyAdditiveBonus);
        tag.putDouble("lastSyncedQiMultiplicativeBonus", lastSyncedQiMultiplicativeBonus);
        tag.putDouble("lastSyncedBodyMultiplicativeBonus", lastSyncedBodyMultiplicativeBonus);
        return tag;
    }

    // === Codec ===

    public static final Codec<PlayerCultivationBonusAttachment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.DOUBLE.optionalFieldOf("qiCultivationAdditiveBonus", 0.0).forGetter(comp -> comp.qiCultivationAdditiveBonus),
        Codec.DOUBLE.optionalFieldOf("bodyCultivationAdditiveBonus", 0.0).forGetter(comp -> comp.bodyCultivationAdditiveBonus),
        Codec.DOUBLE.optionalFieldOf("qiCultivationMultiplicativeBonus", 0.0).forGetter(comp -> comp.qiCultivationMultiplicativeBonus),
        Codec.DOUBLE.optionalFieldOf("bodyCultivationMultiplicativeBonus", 0.0).forGetter(comp -> comp.bodyCultivationMultiplicativeBonus),
        Codec.INT.optionalFieldOf("tickCounter", 0).forGetter(comp -> comp.tickCounter),
        Codec.DOUBLE.optionalFieldOf("lastSyncedQiAdditiveBonus", 0.0).forGetter(comp -> comp.lastSyncedQiAdditiveBonus),
        Codec.DOUBLE.optionalFieldOf("lastSyncedBodyAdditiveBonus", 0.0).forGetter(comp -> comp.lastSyncedBodyAdditiveBonus),
        Codec.DOUBLE.optionalFieldOf("lastSyncedQiMultiplicativeBonus", 0.0).forGetter(comp -> comp.lastSyncedQiMultiplicativeBonus),
        Codec.DOUBLE.optionalFieldOf("lastSyncedBodyMultiplicativeBonus", 0.0).forGetter(comp -> comp.lastSyncedBodyMultiplicativeBonus)
    ).apply(instance, (qiAdditive, bodyAdditive, qiMultiplicative, bodyMultiplicative, ticks, lastQiAdd, lastBodyAdd, lastQiMult, lastBodyMult) -> {
        PlayerCultivationBonusAttachment comp = new PlayerCultivationBonusAttachment();
        comp.qiCultivationAdditiveBonus = qiAdditive;
        comp.bodyCultivationAdditiveBonus = bodyAdditive;
        comp.qiCultivationMultiplicativeBonus = qiMultiplicative;
        comp.bodyCultivationMultiplicativeBonus = bodyMultiplicative;
        comp.tickCounter = ticks;
        comp.lastSyncedQiAdditiveBonus = lastQiAdd;
        comp.lastSyncedBodyAdditiveBonus = lastBodyAdd;
        comp.lastSyncedQiMultiplicativeBonus = lastQiMult;
        comp.lastSyncedBodyMultiplicativeBonus = lastBodyMult;
        return comp;
    }));

    // === Debug Methods ===

    public void debugInfo() {
        Player player = getPlayerSafe();
        String side = player != null ? (player.level().isClientSide() ? "Client" : "Server") : "Unknown";
        String playerName = player != null ? player.getName().getString() : "Unknown";

        ImmortalCultivation.LOGGER.info("=== Cultivation Bonus Debug Info ===");
        ImmortalCultivation.LOGGER.info("Player: {} ({})", playerName, side);
        ImmortalCultivation.LOGGER.info("Qi Cultivation Bonuses:");
        ImmortalCultivation.LOGGER.info("  - Additive: +{}/sec", String.format("%.3f", qiCultivationAdditiveBonus));
        ImmortalCultivation.LOGGER.info("  - Multiplicative: {}x", String.format("%.3f", getQiCultivationMultiplicativeBonus()));
        ImmortalCultivation.LOGGER.info("Body Cultivation Bonuses:");
        ImmortalCultivation.LOGGER.info("  - Additive: +{}/sec", String.format("%.3f", bodyCultivationAdditiveBonus));
        ImmortalCultivation.LOGGER.info("  - Multiplicative: {}x", String.format("%.3f", getBodyCultivationMultiplicativeBonus()));
        ImmortalCultivation.LOGGER.info("No pill consumption limits - unlimited cultivation enhancement!");
    }
}
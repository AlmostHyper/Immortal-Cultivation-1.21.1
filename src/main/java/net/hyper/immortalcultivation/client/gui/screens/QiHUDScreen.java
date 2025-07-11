package net.hyper.immortalcultivation.client.gui.screens;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.attachments.PlayerCultivationAttachment;
import net.hyper.immortalcultivation.common.attachments.PlayerResourceAttachment;
import net.hyper.immortalcultivation.common.attachments.PlayerTalentsAttachment;
import net.hyper.immortalcultivation.common.interfaces.ICultivationData;
import net.hyper.immortalcultivation.common.registry.ICModAttachmentTypes;
import net.hyper.immortalcultivation.server.event.world.ICModAttachmentUpdateEvent;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Enhanced HUD for the unified cultivation system.
 * Displays Qi Cultivation, Body Cultivation, Health, Qi Energy, Body Energy, and current focus.
 * The center symbol shows which cultivation type is currently focused for the 2x bonus.
 */
@OnlyIn(Dist.CLIENT)
public class QiHUDScreen {

    // Design constants - optimized for displaying multiple stats efficiently
    private static final int CIRCLE_SIZE = 50; // Slightly larger to accommodate focus symbol
    private static final int CIRCLE_RADIUS = CIRCLE_SIZE / 2;
    private static final int PADDING = 10;
    private static final int TEXT_OFFSET_X = 30; // More space for multiple stats
    private static final float TEXT_SCALE = 0.70f; // Slightly smaller to fit more information
    private static final int STAT_SPACING = 14; // Consistent spacing between stat lines

    // Visual styling
    private static final int BACKGROUND_COLOR = 0x90000000;
    private static final int BORDER_COLOR = 0xFF8B4513;

    // Qi Cultivation colors (spiritual energy progression)
    private static final int QI_CULTIVATION_COLOR_LOW = 0xFF4A5D23;
    private static final int QI_CULTIVATION_COLOR_MID = 0xFF6B8E23;
    private static final int QI_CULTIVATION_COLOR_HIGH = 0xFF9ACD32;
    private static final int QI_CULTIVATION_COLOR_FULL = 0xFFFFD700;

    // Body Cultivation colors (physical enhancement progression)
    private static final int BODY_CULTIVATION_COLOR_LOW = 0xFF8B2635;
    private static final int BODY_CULTIVATION_COLOR_MID = 0xFFCD5C5C;
    private static final int BODY_CULTIVATION_COLOR_HIGH = 0xFFFF6347;
    private static final int BODY_CULTIVATION_COLOR_FULL = 0xFFFF4500;

    // Health colors (life force vitality)
    private static final int HEALTH_COLOR_LOW = 0xFF800000;
    private static final int HEALTH_COLOR_MID = 0xFFDC143C;
    private static final int HEALTH_COLOR_HIGH = 0xFFFF1493;
    private static final int HEALTH_COLOR_FULL = 0xFFFF69B4;

    // Qi Energy colors (consumable spiritual energy)
    private static final int QI_ENERGY_COLOR_LOW = 0xFF2E4A8B;
    private static final int QI_ENERGY_COLOR_MID = 0xFF4169E1;
    private static final int QI_ENERGY_COLOR_HIGH = 0xFF6495ED;
    private static final int QI_ENERGY_COLOR_FULL = 0xFF00BFFF;

    // Body Energy colors (consumable physical energy)
    private static final int BODY_ENERGY_COLOR_LOW = 0xFF8B1538;
    private static final int BODY_ENERGY_COLOR_MID = 0xFFB22222;
    private static final int BODY_ENERGY_COLOR_HIGH = 0xFFDC143C;
    private static final int BODY_ENERGY_COLOR_FULL = 0xFFFF0000;

    // Focus indicator colors
    private static final int FOCUS_QI_COLOR = 0xFF87CEEB;    // Sky blue for Qi focus
    private static final int FOCUS_BODY_COLOR = 0xFFFF6347; // Tomato red for Body focus

    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int TEXT_SHADOW = 0x80000000;

    // Performance optimization - smooth animations for all stats
    private static float qiCultivationDisplayProgress = 0.0f;
    private static float bodyCultivationDisplayProgress = 0.0f;
    private static float healthDisplayProgress = 0.0f;
    private static float qiEnergyDisplayProgress = 0.0f;
    private static float bodyEnergyDisplayProgress = 0.0f;

    // Cache tracking for all stats to optimize updates
    private static long lastQiCultivation = -1;
    private static long lastMaxQiCultivation = -1;
    private static long lastBodyCultivation = -1;
    private static long lastMaxBodyCultivation = -1;
    private static long lastHealth = -1;
    private static long lastMaxHealth = -1;
    private static long lastQiEnergy = -1;
    private static long lastMaxQiEnergy = -1;
    private static long lastBodyEnergy = -1;
    private static long lastMaxBodyEnergy = -1;
    private static PlayerCultivationAttachment.CultivationType lastFocus = null;

    private static int updateCounter = 0;
    private static final int UPDATE_FREQUENCY = 8; // Update every 8 ticks for smooth performance

    // Cached display strings to reduce string allocation overhead
    private static String cachedQiCultivationText = "";
    private static String cachedBodyCultivationText = "";
    private static String cachedHealthText = "";
    private static String cachedQiEnergyText = "";
    private static String cachedBodyEnergyText = "";
    private static String cachedFocusText = "";
    private static String cachedRealmSummaryText = "";
    private static String cachedTalentSummaryText = "";
    private static int cacheUpdateCounter = 0;
    private static boolean needsUpdate = true;
    private static QiHUDScreen instance;

    public static void register() {
        if (instance == null) {
            instance = new QiHUDScreen();
            NeoForge.EVENT_BUS.register(instance);
        }
    }

    /**
     * Main rendering method for the unified cultivation HUD.
     * This orchestrates the display of all cultivation-related information.
     */
    public static void renderQiScreen(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;

        if (player == null || minecraft.options.hideGui) return;

        // Gather all cultivation data from the player's unified attachments
        CultivationDataSnapshot snapshot = gatherCultivationData(player);
        if (snapshot == null) return; // Missing critical attachments

        // Performance-optimized update cycle
        updateCounter++;
        if (updateCounter >= UPDATE_FREQUENCY || needsUpdate) {
            updateCounter = 0;
            updateAllAnimations(snapshot);

            // Update text cache less frequently to reduce string allocation
            cacheUpdateCounter++;
            if (cacheUpdateCounter >= 4 || needsUpdate) { // Update text every 32 ticks
                cacheUpdateCounter = 0;
                updateAllTextCache(snapshot);
            }
            needsUpdate = false;
        }

        // Calculate HUD position
        int centerX = PADDING + CIRCLE_RADIUS;
        int centerY = PADDING + CIRCLE_RADIUS;

        // Render the complete unified cultivation HUD
        renderUnifiedCultivationHUD(guiGraphics, centerX, centerY, snapshot);
    }

    /**
     * Collects all cultivation-related data from unified attachments in one efficient operation.
     * This reduces the number of attachment lookups and provides a consistent snapshot.
     */
    private static CultivationDataSnapshot gatherCultivationData(LocalPlayer player) {
        try {
            // Get unified attachments
            PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
            PlayerResourceAttachment resources = player.getData(ICModAttachmentTypes.PLAYER_RESOURCES.get());
            PlayerTalentsAttachment talents = player.getData(ICModAttachmentTypes.PLAYER_TALENTS.get());

            // Verify we have all critical data
            if (cultivation == null || resources == null || talents == null) {
                return null;
            }

            return new CultivationDataSnapshot(cultivation, resources, talents);
        } catch (Exception e) {
            // Gracefully handle any attachment access errors
            return null;
        }
    }

    /**
     * Updates all stat animations with smooth interpolation.
     * This creates fluid visual feedback as cultivation values change.
     */
    private static void updateAllAnimations(CultivationDataSnapshot snapshot) {
        // Update Qi Cultivation animation
        updateStatAnimation(
            snapshot.cultivation.getCurrentCultivation(ICultivationData.CultivationType.QI),
            snapshot.cultivation.getMaxCultivation(ICultivationData.CultivationType.QI),
            lastQiCultivation, lastMaxQiCultivation,
            (progress) -> qiCultivationDisplayProgress = progress
        );
        lastQiCultivation = snapshot.cultivation.getCurrentCultivation(ICultivationData.CultivationType.QI);
        lastMaxQiCultivation = snapshot.cultivation.getMaxCultivation(ICultivationData.CultivationType.QI);

        // Update Body Cultivation animation
        updateStatAnimation(
            snapshot.cultivation.getCurrentCultivation(ICultivationData.CultivationType.BODY),
            snapshot.cultivation.getMaxCultivation(ICultivationData.CultivationType.BODY),
            lastBodyCultivation, lastMaxBodyCultivation,
            (progress) -> bodyCultivationDisplayProgress = progress
        );
        lastBodyCultivation = snapshot.cultivation.getCurrentCultivation(ICultivationData.CultivationType.BODY);
        lastMaxBodyCultivation = snapshot.cultivation.getMaxCultivation(ICultivationData.CultivationType.BODY);

        // Update Health animation
        updateStatAnimation(
            snapshot.resources.getCurrentHealth(),
            snapshot.resources.getMaxHealth(),
            lastHealth, lastMaxHealth,
            (progress) -> healthDisplayProgress = progress
        );
        lastHealth = snapshot.resources.getCurrentHealth();
        lastMaxHealth = snapshot.resources.getMaxHealth();

        // Update Qi Energy animation
        updateStatAnimation(
            snapshot.resources.getCurrentQi(),
            snapshot.resources.getMaxQi(),
            lastQiEnergy, lastMaxQiEnergy,
            (progress) -> qiEnergyDisplayProgress = progress
        );
        lastQiEnergy = snapshot.resources.getCurrentQi();
        lastMaxQiEnergy = snapshot.resources.getMaxQi();

        // Update Body Energy animation
        updateStatAnimation(
            snapshot.resources.getCurrentBody(),
            snapshot.resources.getMaxBody(),
            lastBodyEnergy, lastMaxBodyEnergy,
            (progress) -> bodyEnergyDisplayProgress = progress
        );
        lastBodyEnergy = snapshot.resources.getCurrentBody();
        lastMaxBodyEnergy = snapshot.resources.getMaxBody();

        // Track focus changes
        lastFocus = snapshot.cultivation.getCurrentFocus();
    }

    /**
     * Generic animation updater for any cultivation stat.
     * Provides smooth interpolation between old and new values.
     */
    private static void updateStatAnimation(long current, long max, long lastCurrent, long lastMax,
                                            ProgressUpdater updater) {
        if (current != lastCurrent || max != lastMax) {
            float targetProgress = max > 0 ? (float) current / max : 0.0f;
            float currentProgress = updater.getCurrentProgress();

            float diff = targetProgress - currentProgress;
            if (Math.abs(diff) < 0.03f) {
                // Snap to target if very close
                updater.updateProgress(targetProgress);
            } else {
                // Smooth interpolation
                updater.updateProgress(currentProgress + diff * 0.25f);
            }
        }
    }

    /**
     * Updates all cached text strings to reduce string allocation during rendering.
     * This optimization is important for maintaining smooth frame rates.
     */
    private static void updateAllTextCache(CultivationDataSnapshot snapshot) {
        // Cache Qi Cultivation text with percentage and absolute values
        double qiCultivationPercentage = snapshot.cultivation.getProgress(ICultivationData.CultivationType.QI) * 100;
        cachedQiCultivationText = String.format("Qi Cultivation: %.0f%% (%d/%d)",
            qiCultivationPercentage,
            snapshot.cultivation.getCurrentCultivation(ICultivationData.CultivationType.QI),
            snapshot.cultivation.getMaxCultivation(ICultivationData.CultivationType.QI));

        // Cache Body Cultivation text
        double bodyCultivationPercentage = snapshot.cultivation.getProgress(ICultivationData.CultivationType.BODY) * 100;
        cachedBodyCultivationText = String.format("Body Cultivation: %.0f%% (%d/%d)",
            bodyCultivationPercentage,
            snapshot.cultivation.getCurrentCultivation(ICultivationData.CultivationType.BODY),
            snapshot.cultivation.getMaxCultivation(ICultivationData.CultivationType.BODY));

        // Cache Health text - show absolute values since health is more intuitive that way
        cachedHealthText = String.format("Health: %d/%d",
            snapshot.resources.getCurrentHealth(),
            snapshot.resources.getMaxHealth());

        // Cache Qi Energy text
        double qiEnergyPercentage = snapshot.resources.getQiPercentage() * 100;
        cachedQiEnergyText = String.format("Qi Energy: %.0f%% (%d/%d)",
            qiEnergyPercentage,
            snapshot.resources.getCurrentQi(),
            snapshot.resources.getMaxQi());

        // Cache Body Energy text
        double bodyEnergyPercentage = snapshot.resources.getBodyPercentage() * 100;
        cachedBodyEnergyText = String.format("Body Energy: %.0f%% (%d/%d)",
            bodyEnergyPercentage,
            snapshot.resources.getCurrentBody(),
            snapshot.resources.getMaxBody());

        // Cache focus text with bonus indication
        cachedFocusText = String.format("%s Focus (2x rate)",
            snapshot.cultivation.getCurrentFocus().getDisplayName());

        // Cache realm summary showing both cultivation paths
        cachedRealmSummaryText = String.format("Qi: %s | Body: %s",
            snapshot.cultivation.getRealm(ICultivationData.CultivationType.QI).getDisplayName(),
            snapshot.cultivation.getRealm(ICultivationData.CultivationType.BODY).getDisplayName());

        // Cache talent summary
        cachedTalentSummaryText = String.format("Talent: %.1fx | %.1fx",
            snapshot.talents.getSpiritualRootMultiplier(),
            snapshot.talents.getPhysiqueMultiplier());
    }

    /**
     * Renders the complete unified cultivation HUD system.
     */
    private static void renderUnifiedCultivationHUD(GuiGraphics guiGraphics, int centerX, int centerY,
                                                    CultivationDataSnapshot snapshot) {
        // Render the focus indicator circle with current cultivation focus
        renderFocusIndicatorCircle(guiGraphics, centerX, centerY, snapshot.cultivation);

        // Render all cultivation statistics
        renderCultivationStatistics(guiGraphics, centerX, centerY, snapshot);
    }

    /**
     * Renders the central focus indicator showing which cultivation type has the 2x bonus.
     * This is the most important visual element for strategic gameplay decisions.
     */
    private static void renderFocusIndicatorCircle(GuiGraphics guiGraphics, int centerX, int centerY,
                                                   PlayerCultivationAttachment cultivation) {
        int size = CIRCLE_SIZE - 4;
        int halfSize = size / 2;

        // Background circle
        guiGraphics.fill(centerX - halfSize, centerY - halfSize,
            centerX + halfSize, centerY + halfSize, BACKGROUND_COLOR);

        // Border with focus-specific color
        PlayerCultivationAttachment.CultivationType currentFocus = cultivation.getCurrentFocus();
        int borderColor = currentFocus == PlayerCultivationAttachment.CultivationType.QI ? FOCUS_QI_COLOR : FOCUS_BODY_COLOR;

        // Top and bottom borders
        guiGraphics.fill(centerX - halfSize - 1, centerY - halfSize - 1,
            centerX + halfSize + 1, centerY - halfSize, borderColor);
        guiGraphics.fill(centerX - halfSize - 1, centerY + halfSize,
            centerX + halfSize + 1, centerY + halfSize + 1, borderColor);

        // Left and right borders
        guiGraphics.fill(centerX - halfSize - 1, centerY - halfSize,
            centerX - halfSize, centerY + halfSize, borderColor);
        guiGraphics.fill(centerX + halfSize, centerY - halfSize,
            centerX + halfSize + 1, centerY + halfSize, borderColor);

        // Focus symbol in center
        Font font = Minecraft.getInstance().font;
        String focusSymbol = currentFocus == PlayerCultivationAttachment.CultivationType.QI ? "氣" : "體"; // Qi (氣) or Body (體)
        int symbolWidth = font.width(focusSymbol);
        int symbolX = centerX - symbolWidth / 2;
        int symbolY = centerY - 4;

        // Draw symbol with shadow for clarity
        guiGraphics.drawString(font, focusSymbol, symbolX + 1, symbolY + 1, TEXT_SHADOW, false);
        guiGraphics.drawString(font, focusSymbol, symbolX, symbolY, borderColor, false);
    }

    /**
     * Renders all cultivation statistics with color-coded progress indicators.
     */
    private static void renderCultivationStatistics(GuiGraphics guiGraphics, int centerX, int centerY,
                                                    CultivationDataSnapshot snapshot) {
        Font font = Minecraft.getInstance().font;

        // Use scaled rendering for compact text display
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(TEXT_SCALE, TEXT_SCALE, 1.0f);

        int textX = (int) ((centerX + TEXT_OFFSET_X) / TEXT_SCALE);
        int baseY = (int) ((centerY - 40) / TEXT_SCALE); // Start higher to fit more stats

        int currentY = baseY;

        // Render focus indicator text
        if (!cachedFocusText.isEmpty()) {
            int focusColor = snapshot.cultivation.getCurrentFocus() == PlayerCultivationAttachment.CultivationType.QI ?
                FOCUS_QI_COLOR : FOCUS_BODY_COLOR;
            guiGraphics.drawString(font, cachedFocusText, textX, currentY, focusColor, false);
            currentY += STAT_SPACING;
        }

        // Render talent summary
        if (!cachedTalentSummaryText.isEmpty()) {
            guiGraphics.drawString(font, cachedTalentSummaryText, textX, currentY, 0xFFDDA0DD, false);
            currentY += STAT_SPACING;
        }

        // Render realm summary
        if (!cachedRealmSummaryText.isEmpty()) {
            guiGraphics.drawString(font, cachedRealmSummaryText, textX, currentY, TEXT_COLOR, false);
            currentY += STAT_SPACING;
        }

        // Render Qi Cultivation with progress-based coloring
        if (!cachedQiCultivationText.isEmpty()) {
            int qiCultivationColor = getQiCultivationColor(qiCultivationDisplayProgress);
            guiGraphics.drawString(font, cachedQiCultivationText, textX, currentY, qiCultivationColor, false);
            currentY += STAT_SPACING;
        }

        // Render Body Cultivation with progress-based coloring
        if (!cachedBodyCultivationText.isEmpty()) {
            int bodyCultivationColor = getBodyCultivationColor(bodyCultivationDisplayProgress);
            guiGraphics.drawString(font, cachedBodyCultivationText, textX, currentY, bodyCultivationColor, false);
            currentY += STAT_SPACING;
        }

        // Render Health with vitality-based coloring
        if (!cachedHealthText.isEmpty()) {
            int healthColor = getHealthColor(healthDisplayProgress);
            guiGraphics.drawString(font, cachedHealthText, textX, currentY, healthColor, false);
            currentY += STAT_SPACING;
        }

        // Render Qi Energy with energy-based coloring
        if (!cachedQiEnergyText.isEmpty()) {
            int qiEnergyColor = getQiEnergyColor(qiEnergyDisplayProgress);
            guiGraphics.drawString(font, cachedQiEnergyText, textX, currentY, qiEnergyColor, false);
            currentY += STAT_SPACING;
        }

        // Render Body Energy with energy-based coloring
        if (!cachedBodyEnergyText.isEmpty()) {
            int bodyEnergyColor = getBodyEnergyColor(bodyEnergyDisplayProgress);
            guiGraphics.drawString(font, cachedBodyEnergyText, textX, currentY, bodyEnergyColor, false);
        }

        guiGraphics.pose().popPose();
    }

    // === Color Calculation Methods ===
    // These methods provide visual feedback about stat levels through color progression

    /**
     * Determines Qi Cultivation color based on progression (green spectrum for spiritual growth)
     */
    private static int getQiCultivationColor(float progress) {
        if (progress > 0.85f) return QI_CULTIVATION_COLOR_FULL;   // Gold - ready for breakthrough
        if (progress > 0.65f) return QI_CULTIVATION_COLOR_HIGH;   // Bright green - strong progress
        if (progress > 0.35f) return QI_CULTIVATION_COLOR_MID;    // Medium green - steady progress
        return QI_CULTIVATION_COLOR_LOW;                          // Dark green - early stage
    }

    /**
     * Determines Body Cultivation color based on progression (red spectrum for physical power)
     */
    private static int getBodyCultivationColor(float progress) {
        if (progress > 0.85f) return BODY_CULTIVATION_COLOR_FULL; // Bright red - peak physical power
        if (progress > 0.65f) return BODY_CULTIVATION_COLOR_HIGH; // Strong red - enhanced strength
        if (progress > 0.35f) return BODY_CULTIVATION_COLOR_MID;  // Medium red - building power
        return BODY_CULTIVATION_COLOR_LOW;                        // Dark red - foundation stage
    }

    /**
     * Determines Health color based on current vitality (pink spectrum for life force)
     */
    private static int getHealthColor(float progress) {
        if (progress > 0.85f) return HEALTH_COLOR_FULL;  // Bright pink - peak vitality
        if (progress > 0.65f) return HEALTH_COLOR_HIGH;  // Strong pink - healthy state
        if (progress > 0.35f) return HEALTH_COLOR_MID;   // Medium pink - adequate health
        return HEALTH_COLOR_LOW;                         // Dark red - low health warning
    }

    /**
     * Determines Qi Energy color based on current reserves (blue spectrum for spiritual energy)
     */
    private static int getQiEnergyColor(float progress) {
        if (progress > 0.85f) return QI_ENERGY_COLOR_FULL; // Bright blue - full energy reserves
        if (progress > 0.65f) return QI_ENERGY_COLOR_HIGH; // Strong blue - good energy
        if (progress > 0.35f) return QI_ENERGY_COLOR_MID;  // Medium blue - moderate energy
        return QI_ENERGY_COLOR_LOW;                        // Dark blue - low energy
    }

    /**
     * Determines Body Energy color based on current reserves (red spectrum for physical energy)
     */
    private static int getBodyEnergyColor(float progress) {
        if (progress > 0.85f) return BODY_ENERGY_COLOR_FULL; // Bright red - full energy reserves
        if (progress > 0.65f) return BODY_ENERGY_COLOR_HIGH; // Strong red - good energy
        if (progress > 0.35f) return BODY_ENERGY_COLOR_MID;  // Medium red - moderate energy
        return BODY_ENERGY_COLOR_LOW;                        // Dark red - low energy
    }

    // === Helper Interfaces and Classes ===

    /**
     * Functional interface for smooth animation updates
     */
    @FunctionalInterface
    private interface ProgressUpdater {
        void updateProgress(float newProgress);

        default float getCurrentProgress() {
            // This would need to be implemented by the caller to return current progress
            return 0.0f;
        }
    }

    /**
     * Data snapshot containing all cultivation information for consistent rendering using unified attachments.
     * This prevents inconsistencies that could occur if attachments change mid-render.
     */
    private record CultivationDataSnapshot(
        PlayerCultivationAttachment cultivation,
        PlayerResourceAttachment resources,
        PlayerTalentsAttachment talents
    ) {
    }

    /**
     * Event handler for attachment updates.
     * This ensures the HUD refreshes when cultivation data changes.
     */
    @SubscribeEvent
    public void onAttachmentUpdate(ICModAttachmentUpdateEvent event) {
        if (event.getPlayer().equals(Minecraft.getInstance().player)) {
            if (event.isAttachmentType(ICModAttachmentTypes.PLAYER_CULTIVATION.get()) ||
                event.isAttachmentType(ICModAttachmentTypes.PLAYER_RESOURCES.get()) ||
                event.isAttachmentType(ICModAttachmentTypes.PLAYER_TALENTS.get())) {

                // Force immediate cache update for health changes
                needsUpdate = true;
                updateCounter = UPDATE_FREQUENCY; // Force immediate update next frame

                // Debug logging to verify client receives updates
                if (event.isAttachmentType(ICModAttachmentTypes.PLAYER_RESOURCES.get())) {
                    LocalPlayer player = Minecraft.getInstance().player;
                    if (player != null) {
                        PlayerResourceAttachment resources = player.getData(ICModAttachmentTypes.PLAYER_RESOURCES.get());
                        if (resources != null) {
                            ImmortalCultivation.LOGGER.info("Client received health update: {}/{}",
                                resources.getCurrentHealth(), resources.getMaxHealth());
                        }
                    }
                }
            }
        }
    }
}
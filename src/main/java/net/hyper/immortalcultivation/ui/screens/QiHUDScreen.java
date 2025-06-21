package net.hyper.immortalcultivation.ui.screens;

import net.hyper.immortalcultivation.attachment.*;
import net.hyper.immortalcultivation.cultivation.*;
import net.hyper.immortalcultivation.cultivation.realms.CultivationRealm;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Enhanced HUD for the dual cultivation system.
 * Displays Qi Cultivation, Body Cultivation, Health, Qi Energy, and current focus.
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

    // Cache tracking for all stats to optimize updates
    private static long lastQiCultivation = -1;
    private static long lastMaxQiCultivation = -1;
    private static long lastBodyCultivation = -1;
    private static long lastMaxBodyCultivation = -1;
    private static long lastHealth = -1;
    private static long lastMaxHealth = -1;
    private static long lastQiEnergy = -1;
    private static long lastMaxQiEnergy = -1;
    private static PlayerRealmAttachment.CultivationType lastFocus = null;

    private static int updateCounter = 0;
    private static final int UPDATE_FREQUENCY = 8; // Update every 8 ticks for smooth performance

    // Cached display strings to reduce string allocation overhead
    private static String cachedQiCultivationText = "";
    private static String cachedBodyCultivationText = "";
    private static String cachedHealthText = "";
    private static String cachedQiEnergyText = "";
    private static String cachedFocusText = "";
    private static String cachedRealmSummaryText = "";
    private static int cacheUpdateCounter = 0;

    /**
     * Main rendering method for the dual cultivation HUD.
     * This orchestrates the display of all cultivation-related information.
     */
    public static void renderQiScreen(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;

        if (player == null || minecraft.options.hideGui) return;

        // Gather all cultivation data from the player's attachments
        CultivationDataSnapshot snapshot = gatherCultivationData(player);
        if (snapshot == null) return; // Missing critical attachments

        // Performance-optimized update cycle
        updateCounter++;
        if (updateCounter >= UPDATE_FREQUENCY) {
            updateCounter = 0;
            updateAllAnimations(snapshot);

            // Update text cache less frequently to reduce string allocation
            cacheUpdateCounter++;
            if (cacheUpdateCounter >= 4) { // Update text every 32 ticks
                cacheUpdateCounter = 0;
                updateAllTextCache(snapshot);
            }
        }

        // Calculate HUD position
        int centerX = PADDING + CIRCLE_RADIUS;
        int centerY = PADDING + CIRCLE_RADIUS;

        // Render the complete dual cultivation HUD
        renderDualCultivationHUD(guiGraphics, centerX, centerY, snapshot);
    }

    /**
     * Collects all cultivation-related data in one efficient operation.
     * This reduces the number of attachment lookups and provides a consistent snapshot.
     */
    private static CultivationDataSnapshot gatherCultivationData(LocalPlayer player) {
        try {
            // Get all required attachments
            IQiCultivationData qiCultivationData = player.getData(ICModAttachmentTypes.PLAYER_QI_CULTIVATION.get());
            IBodyCultivationData bodyCultivationData = player.getData(ICModAttachmentTypes.PLAYER_BODY_CULTIVATION.get());
            IHealthData healthData = player.getData(ICModAttachmentTypes.PLAYER_HEALTH.get());
            IQiData qiEnergyData = player.getData(ICModAttachmentTypes.PLAYER_QI.get());
            PlayerCultivationFocusAttachment focusData = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION_FOCUS.get());
            PlayerRealmAttachment realmData = player.getData(ICModAttachmentTypes.PLAYER_REALM.get());

            // Verify we have all critical data
            if (qiCultivationData == null || bodyCultivationData == null ||
                healthData == null || qiEnergyData == null ||
                focusData == null || realmData == null) {
                return null;
            }

            return new CultivationDataSnapshot(
                qiCultivationData, bodyCultivationData, healthData,
                qiEnergyData, focusData, realmData
            );
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
            snapshot.qiCultivation.getCurrentQiCultivation(),
            snapshot.qiCultivation.getMaxQiCultivation(),
            lastQiCultivation, lastMaxQiCultivation,
            (progress) -> qiCultivationDisplayProgress = progress
        );
        lastQiCultivation = snapshot.qiCultivation.getCurrentQiCultivation();
        lastMaxQiCultivation = snapshot.qiCultivation.getMaxQiCultivation();

        // Update Body Cultivation animation
        updateStatAnimation(
            snapshot.bodyCultivation.getCurrentBodyCultivation(),
            snapshot.bodyCultivation.getMaxBodyCultivation(),
            lastBodyCultivation, lastMaxBodyCultivation,
            (progress) -> bodyCultivationDisplayProgress = progress
        );
        lastBodyCultivation = snapshot.bodyCultivation.getCurrentBodyCultivation();
        lastMaxBodyCultivation = snapshot.bodyCultivation.getMaxBodyCultivation();

        // Update Health animation
        updateStatAnimation(
            snapshot.health.getCurrentHealth(),
            snapshot.health.getMaxHealth(),
            lastHealth, lastMaxHealth,
            (progress) -> healthDisplayProgress = progress
        );
        lastHealth = snapshot.health.getCurrentHealth();
        lastMaxHealth = snapshot.health.getMaxHealth();

        // Update Qi Energy animation
        updateStatAnimation(
            snapshot.qiEnergy.getCurrentQi(),
            snapshot.qiEnergy.getMaxQi(),
            lastQiEnergy, lastMaxQiEnergy,
            (progress) -> qiEnergyDisplayProgress = progress
        );
        lastQiEnergy = snapshot.qiEnergy.getCurrentQi();
        lastMaxQiEnergy = snapshot.qiEnergy.getMaxQi();

        // Track focus changes
        lastFocus = snapshot.focus.getCurrentFocus();
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
        float qiCultivationPercentage = snapshot.qiCultivation.getMaxQiCultivation() > 0 ?
            (float) snapshot.qiCultivation.getCurrentQiCultivation() / snapshot.qiCultivation.getMaxQiCultivation() * 100 : 0;
        cachedQiCultivationText = String.format("Qi Cultivation: %.0f%% (%d/%d)",
            qiCultivationPercentage,
            snapshot.qiCultivation.getCurrentQiCultivation(),
            snapshot.qiCultivation.getMaxQiCultivation());

        // Cache Body Cultivation text
        float bodyCultivationPercentage = snapshot.bodyCultivation.getMaxBodyCultivation() > 0 ?
            (float) snapshot.bodyCultivation.getCurrentBodyCultivation() / snapshot.bodyCultivation.getMaxBodyCultivation() * 100 : 0;
        cachedBodyCultivationText = String.format("Body Cultivation: %.0f%% (%d/%d)",
            bodyCultivationPercentage,
            snapshot.bodyCultivation.getCurrentBodyCultivation(),
            snapshot.bodyCultivation.getMaxBodyCultivation());

        // Cache Health text - show absolute values since health is more intuitive that way
        cachedHealthText = String.format("Health: %d/%d",
            snapshot.health.getCurrentHealth(),
            snapshot.health.getMaxHealth());

        // Cache Qi Energy text
        float qiEnergyPercentage = snapshot.qiEnergy.getMaxQi() > 0 ?
            (float) snapshot.qiEnergy.getCurrentQi() / snapshot.qiEnergy.getMaxQi() * 100 : 0;
        cachedQiEnergyText = String.format("Qi Energy: %.0f%% (%d/%d)",
            qiEnergyPercentage,
            snapshot.qiEnergy.getCurrentQi(),
            snapshot.qiEnergy.getMaxQi());

        // Cache focus text with bonus indication
        cachedFocusText = String.format("%s Focus (2x rate)",
            snapshot.focus.getCurrentFocus().getDisplayName());

        // Cache realm summary showing both cultivation paths
        cachedRealmSummaryText = String.format("Qi: %s | Body: %s",
            snapshot.realm.getCurrentQiRealm().getDisplayName(),
            snapshot.realm.getCurrentBodyRealm().getDisplayName());
    }

    /**
     * Renders the complete dual cultivation HUD system.
     */
    private static void renderDualCultivationHUD(GuiGraphics guiGraphics, int centerX, int centerY,
                                                 CultivationDataSnapshot snapshot) {
        // Render the focus indicator circle with current cultivation focus
        renderFocusIndicatorCircle(guiGraphics, centerX, centerY, snapshot.focus);

        // Render all cultivation statistics
        renderCultivationStatistics(guiGraphics, centerX, centerY, snapshot);
    }

    /**
     * Renders the central focus indicator showing which cultivation type has the 2x bonus.
     * This is the most important visual element for strategic gameplay decisions.
     */
    private static void renderFocusIndicatorCircle(GuiGraphics guiGraphics, int centerX, int centerY,
                                                   PlayerCultivationFocusAttachment focus) {
        int size = CIRCLE_SIZE - 4;
        int halfSize = size / 2;

        // Background circle
        guiGraphics.fill(centerX - halfSize, centerY - halfSize,
            centerX + halfSize, centerY + halfSize, BACKGROUND_COLOR);

        // Border with focus-specific color
        int borderColor = focus.isFocusedOnQi() ? FOCUS_QI_COLOR : FOCUS_BODY_COLOR;

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
        String focusSymbol = focus.isFocusedOnQi() ? "氣" : "體"; // Qi (氣) or Body (體)
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
        int baseY = (int) ((centerY - 30) / TEXT_SCALE); // Start higher to fit more stats

        int currentY = baseY;

        // Render focus indicator text
        if (!cachedFocusText.isEmpty()) {
            int focusColor = snapshot.focus.isFocusedOnQi() ? FOCUS_QI_COLOR : FOCUS_BODY_COLOR;
            guiGraphics.drawString(font, cachedFocusText, textX, currentY, focusColor, false);
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
     * Data snapshot containing all cultivation information for consistent rendering.
     * This prevents inconsistencies that could occur if attachments change mid-render.
     */
    private record CultivationDataSnapshot(
        IQiCultivationData qiCultivation,
        IBodyCultivationData bodyCultivation,
        IHealthData health,
        IQiData qiEnergy,
        PlayerCultivationFocusAttachment focus,
        PlayerRealmAttachment realm
    ) {}
}
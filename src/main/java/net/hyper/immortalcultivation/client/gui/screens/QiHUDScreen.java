package net.hyper.immortalcultivation.client.gui.screens;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.core.attachments.PlayerCultivationAttachment;
import net.hyper.immortalcultivation.core.attachments.PlayerStatsAttachment;
import net.hyper.immortalcultivation.core.attachments.PlayerTalentsAttachment;
import net.hyper.immortalcultivation.core.attachments.ICultivationData;
import net.hyper.immortalcultivation.core.attachments.IStatsData;
import net.hyper.immortalcultivation.core.attachments.AttachmentRegistry;
import net.hyper.immortalcultivation.server.events.server.AttachmentUpdateEventHandler;
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
 *
 * This updated implementation properly utilizes the generic methods from PlayerAttachment
 * and maintains compatibility with the new attachment system.
 */
@OnlyIn(Dist.CLIENT)
public class QiHUDScreen {

    // === DESIGN CONSTANTS === //

    // Layout constants - optimized for displaying multiple stats efficiently
    private static final int CIRCLE_SIZE = 50; // Slightly larger to accommodate focus symbol
    private static final int CIRCLE_RADIUS = CIRCLE_SIZE / 2;
    private static final int PADDING = 10;
    private static final int TEXT_OFFSET_X = 30; // More space for multiple stats
    private static final float TEXT_SCALE = 0.70f; // Slightly smaller to fit more information
    private static final int STAT_SPACING = 14; // Consistent spacing between stat lines

    // Visual styling constants
    private static final int BACKGROUND_COLOR = 0x90000000;
    private static final int BORDER_COLOR = 0xFF8B4513;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int TEXT_SHADOW = 0xFF000000;

    // Focus indicator colors
    private static final int FOCUS_QI_COLOR = 0xFF87CEEB;
    private static final int FOCUS_BODY_COLOR = 0xFFFF6347;

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
    private static final int HEALTH_COLOR_HIGH = 0xFFFF69B4;
    private static final int HEALTH_COLOR_FULL = 0xFFFF1493;

    // Qi Energy colors (spiritual energy reserves)
    private static final int QI_ENERGY_COLOR_LOW = 0xFF000080;
    private static final int QI_ENERGY_COLOR_MID = 0xFF0000FF;
    private static final int QI_ENERGY_COLOR_HIGH = 0xFF4169E1;
    private static final int QI_ENERGY_COLOR_FULL = 0xFF00BFFF;

    // Body Energy colors (physical energy reserves)
    private static final int BODY_ENERGY_COLOR_LOW = 0xFF8B0000;
    private static final int BODY_ENERGY_COLOR_MID = 0xFFFF0000;
    private static final int BODY_ENERGY_COLOR_HIGH = 0xFFFF4500;
    private static final int BODY_ENERGY_COLOR_FULL = 0xFFFFFF00;

    // === ANIMATION AND CACHING FIELDS === //

    // Display progress for smooth animations
    private static float qiCultivationDisplayProgress = 0.0f;
    private static float bodyCultivationDisplayProgress = 0.0f;
    private static float healthDisplayProgress = 1.0f;
    private static float qiEnergyDisplayProgress = 0.0f;
    private static float bodyEnergyDisplayProgress = 0.0f;

    // Last known values for change detection and animation triggering
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

    // Performance optimization fields
    private static int updateCounter = 0;
    private static final int UPDATE_FREQUENCY = 20; // Update every 8 ticks for smooth performance

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

    // Singleton instance management
    private static QiHUDScreen instance;

    private static CultivationDataSnapshot cachedSnapshot = null;
    private static long lastSnapshotUpdate = 0;

    // === REGISTRATION AND LIFECYCLE === //

    /**
     * Registers the QiHUDScreen instance with the event bus for attachment updates.
     * This ensures the HUD updates when cultivation data changes.
     */
    public static void register() {
        if (instance == null) {
            instance = new QiHUDScreen();
            NeoForge.EVENT_BUS.register(instance);
        }
    }

    // === MAIN RENDERING METHOD === //

    /**
     * Main rendering method for the unified cultivation HUD.
     * This orchestrates the display of all cultivation-related information.
     *
     * @param guiGraphics The graphics context for rendering
     * @param deltaTracker Frame delta tracking for smooth animations
     */
    public static void renderQiScreen(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;

        if (player == null || minecraft.options.hideGui) return;

        // Performance optimization: Only gather data when needed
        if (updateCounter >= UPDATE_FREQUENCY || needsUpdate || cachedSnapshot == null) {
            cachedSnapshot = gatherCultivationData(player);
            lastSnapshotUpdate = System.currentTimeMillis();
            if (cachedSnapshot == null) return; // Missing critical attachments
        } else if (cachedSnapshot == null) {
            return; // No cached data available
        }

        // Performance-optimized update cycle
        updateCounter++;
        if (updateCounter >= UPDATE_FREQUENCY || needsUpdate) {
            updateCounter = 0;
            updateAllAnimations(cachedSnapshot);

            // Update text cache less frequently to reduce string allocation
            cacheUpdateCounter++;
            if (cacheUpdateCounter >= 4 || needsUpdate) { // Update text every 32 ticks
                cacheUpdateCounter = 0;
                updateAllTextCache(cachedSnapshot);
            }
            needsUpdate = false;
        }

        // Calculate HUD position
        int centerX = PADDING + CIRCLE_RADIUS;
        int centerY = PADDING + CIRCLE_RADIUS;

        // Render the complete unified cultivation HUD
        renderUnifiedCultivationHUD(guiGraphics, centerX, centerY, cachedSnapshot);
    }

    // === DATA GATHERING === //

    /**
     * Collects all cultivation-related data from unified attachments in one efficient operation.
     * This reduces the number of attachment lookups and provides a consistent snapshot.
     *
     * @param player The player to gather data from
     * @return CultivationDataSnapshot containing all relevant data, or null if attachments are missing
     */
    private static CultivationDataSnapshot gatherCultivationData(LocalPlayer player) {
        try {
            // Get unified attachments using the generic PlayerAttachment methods
            PlayerCultivationAttachment cultivation = player.getData(AttachmentRegistry.PLAYER_CULTIVATION.get());
            PlayerStatsAttachment stats = player.getData(AttachmentRegistry.PLAYER_STATS.get());
            PlayerTalentsAttachment talents = player.getData(AttachmentRegistry.PLAYER_TALENTS.get());

            // Verify we have all critical data
            if (cultivation == null || stats == null || talents == null) {
                if (cultivation != null && cultivation.debugEnabled()) {
                    cultivation.debugWarn("Missing critical attachments for HUD - cultivation: {}, stats: {}, talents: {}",
                        cultivation != null, stats != null, talents != null);
                }
                return null;
            }

            // Ensure player references are set (utilizing PlayerAttachment methods)
            if (cultivation.getPlayer() == null) cultivation.setPlayer(player);
            if (stats.getPlayer() == null) stats.setPlayer(player);
            if (talents.getPlayer() == null) talents.setPlayer(player);

            return new CultivationDataSnapshot(cultivation, stats, talents);
        } catch (Exception e) {
            // Gracefully handle any attachment access errors
            ImmortalCultivation.LOGGER.error("Error gathering cultivation data for HUD: {}", e.getMessage());
            return null;
        }
    }

    // === ANIMATION UPDATES === //

    /**
     * Updates all stat animations with smooth interpolation.
     * This creates fluid visual feedback as cultivation values change.
     * Utilizes the generic cultivation data methods for consistent access.
     *
     * @param snapshot The current cultivation data snapshot
     */
    private static void updateAllAnimations(CultivationDataSnapshot snapshot) {
        // Update Qi Cultivation animation
        updateStatAnimation(
            snapshot.cultivation.getCurrentCultivation(ICultivationData.CultivationType.QI),
            snapshot.cultivation.getMaxCultivation(ICultivationData.CultivationType.QI),
            lastQiCultivation, lastMaxQiCultivation,
            new ProgressUpdater() {
                @Override
                public void updateProgress(float newProgress) {
                    qiCultivationDisplayProgress = newProgress;
                }

                @Override
                public float getCurrentProgress() {
                    return qiCultivationDisplayProgress;
                }
            }
        );
        lastQiCultivation = snapshot.cultivation.getCurrentCultivation(ICultivationData.CultivationType.QI);
        lastMaxQiCultivation = snapshot.cultivation.getMaxCultivation(ICultivationData.CultivationType.QI);

        // Update Body Cultivation animation
        updateStatAnimation(
            snapshot.cultivation.getCurrentCultivation(ICultivationData.CultivationType.BODY),
            snapshot.cultivation.getMaxCultivation(ICultivationData.CultivationType.BODY),
            lastBodyCultivation, lastMaxBodyCultivation,
            new ProgressUpdater() {
                @Override
                public void updateProgress(float newProgress) {
                    bodyCultivationDisplayProgress = newProgress;
                }

                @Override
                public float getCurrentProgress() {
                    return bodyCultivationDisplayProgress;
                }
            }
        );
        lastBodyCultivation = snapshot.cultivation.getCurrentCultivation(ICultivationData.CultivationType.BODY);
        lastMaxBodyCultivation = snapshot.cultivation.getMaxCultivation(ICultivationData.CultivationType.BODY);

        // Update Health animation using stats attachment - THIS IS THE KEY FIX
        updateStatAnimation(
            snapshot.stats.getCurrentStat(IStatsData.StatType.HEALTH),
            snapshot.stats.getMaxStat(IStatsData.StatType.HEALTH),
            lastHealth, lastMaxHealth,
            new ProgressUpdater() {
                @Override
                public void updateProgress(float newProgress) {
                    healthDisplayProgress = newProgress;
                }

                @Override
                public float getCurrentProgress() {
                    return healthDisplayProgress;
                }
            }
        );
        lastHealth = snapshot.stats.getCurrentStat(IStatsData.StatType.HEALTH);
        lastMaxHealth = snapshot.stats.getMaxStat(IStatsData.StatType.HEALTH);

        // Update Qi Energy animation
        updateStatAnimation(
            snapshot.stats.getCurrentStat(IStatsData.StatType.QI),
            snapshot.stats.getMaxStat(IStatsData.StatType.QI),
            lastQiEnergy, lastMaxQiEnergy,
            new ProgressUpdater() {
                @Override
                public void updateProgress(float newProgress) {
                    qiEnergyDisplayProgress = newProgress;
                }

                @Override
                public float getCurrentProgress() {
                    return qiEnergyDisplayProgress;
                }
            }
        );
        lastQiEnergy = snapshot.stats.getCurrentStat(IStatsData.StatType.QI);
        lastMaxQiEnergy = snapshot.stats.getMaxStat(IStatsData.StatType.QI);

        // Update Body Energy animation
        updateStatAnimation(
            snapshot.stats.getCurrentStat(IStatsData.StatType.BODY),
            snapshot.stats.getMaxStat(IStatsData.StatType.BODY),
            lastBodyEnergy, lastMaxBodyEnergy,
            new ProgressUpdater() {
                @Override
                public void updateProgress(float newProgress) {
                    bodyEnergyDisplayProgress = newProgress;
                }

                @Override
                public float getCurrentProgress() {
                    return bodyEnergyDisplayProgress;
                }
            }
        );
        lastBodyEnergy = snapshot.stats.getCurrentStat(IStatsData.StatType.BODY);
        lastMaxBodyEnergy = snapshot.stats.getMaxStat(IStatsData.StatType.BODY);

        // Track focus changes
        lastFocus = snapshot.cultivation.getCurrentFocus();
    }

    /**
     * Generic animation updater for any cultivation stat.
     * Provides smooth interpolation between old and new values.
     *
     * @param current Current stat value
     * @param max Maximum stat value
     * @param lastCurrent Previously cached current value
     * @param lastMax Previously cached maximum value
     * @param updater Function to update the display progress
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

    // === TEXT CACHING === //

    /**
     * Updates all cached text strings to reduce string allocation during rendering.
     * This optimization is important for maintaining smooth frame rates.
     * Uses the generic talent methods for consistent data access.
     *
     * @param snapshot The current cultivation data snapshot
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
            snapshot.stats.getCurrentStat(IStatsData.StatType.HEALTH),
            snapshot.stats.getMaxStat(IStatsData.StatType.HEALTH));

        // Cache Qi Energy text
        double qiEnergyPercentage = snapshot.stats.getCurrentStat(IStatsData.StatType.QI) > 0 && snapshot.stats.getMaxStat(IStatsData.StatType.QI) > 0
            ? (double) snapshot.stats.getCurrentStat(IStatsData.StatType.QI) / snapshot.stats.getMaxStat(IStatsData.StatType.QI) * 100 : 0.0;
        cachedQiEnergyText = String.format("Qi Energy: %.0f%% (%d/%d)",
            qiEnergyPercentage,
            snapshot.stats.getCurrentStat(IStatsData.StatType.QI),
            snapshot.stats.getMaxStat(IStatsData.StatType.QI));

        // Cache Body Energy text
        double bodyEnergyPercentage = snapshot.stats.getCurrentStat(IStatsData.StatType.BODY) > 0 && snapshot.stats.getMaxStat(IStatsData.StatType.BODY) > 0
            ? (double) snapshot.stats.getCurrentStat(IStatsData.StatType.BODY) / snapshot.stats.getMaxStat(IStatsData.StatType.BODY) * 100 : 0.0;
        cachedBodyEnergyText = String.format("Body Energy: %.0f%% (%d/%d)",
            bodyEnergyPercentage,
            snapshot.stats.getCurrentStat(IStatsData.StatType.BODY),
            snapshot.stats.getMaxStat(IStatsData.StatType.BODY));

        // Cache focus text with bonus indication
        cachedFocusText = String.format("%s Focus (2x rate)",
            snapshot.cultivation.getCurrentFocus().getDisplayName());

        // Cache realm summary showing both cultivation paths
        cachedRealmSummaryText = String.format("Qi: %s | Body: %s",
            snapshot.cultivation.getRealm(ICultivationData.CultivationType.QI).getDisplayName(),
            snapshot.cultivation.getRealm(ICultivationData.CultivationType.BODY).getDisplayName());

        // Cache talent summary using generic talent methods
        String spiritualRootDisplay = snapshot.talents.getTalentDisplayName(PlayerTalentsAttachment.TalentType.SPIRITUAL_ROOT);
        String physiqueDisplay = snapshot.talents.getTalentDisplayName(PlayerTalentsAttachment.TalentType.PHYSIQUE);
        float spiritualRootMultiplier = snapshot.talents.getTalentMultiplier(PlayerTalentsAttachment.TalentType.SPIRITUAL_ROOT);
        float physiqueMultiplier = snapshot.talents.getTalentMultiplier(PlayerTalentsAttachment.TalentType.PHYSIQUE);

        cachedTalentSummaryText = String.format("%s (%.1fx) | %s (%.1fx)",
            spiritualRootDisplay, spiritualRootMultiplier,
            physiqueDisplay, physiqueMultiplier);
    }

    // === RENDERING METHODS === //

    /**
     * Renders the complete unified cultivation HUD system.
     * This includes the focus indicator circle and all cultivation statistics.
     *
     * @param guiGraphics The graphics context for rendering
     * @param centerX The X coordinate of the HUD center
     * @param centerY The Y coordinate of the HUD center
     * @param snapshot The current cultivation data snapshot
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
     *
     * @param guiGraphics The graphics context for rendering
     * @param centerX The X coordinate of the circle center
     * @param centerY The Y coordinate of the circle center
     * @param cultivation The player's cultivation attachment
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
     * This displays cached text with appropriate colors based on stat levels.
     *
     * @param guiGraphics The graphics context for rendering
     * @param centerX The X coordinate of the HUD center
     * @param centerY The Y coordinate of the HUD center
     * @param snapshot The current cultivation data snapshot
     */
    private static void renderCultivationStatistics(GuiGraphics guiGraphics, int centerX, int centerY,
                                                    CultivationDataSnapshot snapshot) {
        Font font = Minecraft.getInstance().font;

        // Setup text scaling and positioning
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(TEXT_SCALE, TEXT_SCALE, 1.0f);

        int textX = (int) ((centerX + TEXT_OFFSET_X) / TEXT_SCALE);
        int currentY = (int) ((centerY - (4 * STAT_SPACING)) / TEXT_SCALE);

        // Render focus indicator with appropriate color
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

    // === COLOR CALCULATION METHODS === //

    /**
     * Determines Qi Cultivation color based on progression (green spectrum for spiritual growth).
     *
     * @param progress The cultivation progress as a float between 0.0 and 1.0
     * @return The appropriate color based on progress level
     */
    private static int getQiCultivationColor(float progress) {
        if (progress > 0.85f) return QI_CULTIVATION_COLOR_FULL;   // Gold - ready for breakthrough
        if (progress > 0.65f) return QI_CULTIVATION_COLOR_HIGH;   // Bright green - strong progress
        if (progress > 0.35f) return QI_CULTIVATION_COLOR_MID;    // Medium green - steady progress
        return QI_CULTIVATION_COLOR_LOW;                          // Dark green - early stage
    }

    /**
     * Determines Body Cultivation color based on progression (red spectrum for physical power).
     *
     * @param progress The cultivation progress as a float between 0.0 and 1.0
     * @return The appropriate color based on progress level
     */
    private static int getBodyCultivationColor(float progress) {
        if (progress > 0.85f) return BODY_CULTIVATION_COLOR_FULL; // Bright red - peak physical power
        if (progress > 0.65f) return BODY_CULTIVATION_COLOR_HIGH; // Strong red - enhanced strength
        if (progress > 0.35f) return BODY_CULTIVATION_COLOR_MID;  // Medium red - building power
        return BODY_CULTIVATION_COLOR_LOW;                        // Dark red - foundation stage
    }

    /**
     * Determines Health color based on current vitality (pink spectrum for life force).
     *
     * @param progress The health progress as a float between 0.0 and 1.0
     * @return The appropriate color based on health level
     */
    private static int getHealthColor(float progress) {
        if (progress > 0.85f) return HEALTH_COLOR_FULL;  // Bright pink - peak vitality
        if (progress > 0.65f) return HEALTH_COLOR_HIGH;  // Strong pink - healthy state
        if (progress > 0.35f) return HEALTH_COLOR_MID;   // Medium pink - adequate health
        return HEALTH_COLOR_LOW;                         // Dark red - low health warning
    }

    /**
     * Determines Qi Energy color based on current reserves (blue spectrum for spiritual energy).
     *
     * @param progress The energy progress as a float between 0.0 and 1.0
     * @return The appropriate color based on energy level
     */
    private static int getQiEnergyColor(float progress) {
        if (progress > 0.85f) return QI_ENERGY_COLOR_FULL; // Bright blue - full energy reserves
        if (progress > 0.65f) return QI_ENERGY_COLOR_HIGH; // Strong blue - good energy
        if (progress > 0.35f) return QI_ENERGY_COLOR_MID;  // Medium blue - moderate energy
        return QI_ENERGY_COLOR_LOW;                        // Dark blue - low energy
    }

    /**
     * Determines Body Energy color based on current reserves (red spectrum for physical energy).
     *
     * @param progress The energy progress as a float between 0.0 and 1.0
     * @return The appropriate color based on energy level
     */
    private static int getBodyEnergyColor(float progress) {
        if (progress > 0.85f) return BODY_ENERGY_COLOR_FULL; // Bright red - full energy reserves
        if (progress > 0.65f) return BODY_ENERGY_COLOR_HIGH; // Strong red - good energy
        if (progress > 0.35f) return BODY_ENERGY_COLOR_MID;  // Medium red - moderate energy
        return BODY_ENERGY_COLOR_LOW;                        // Dark red - low energy
    }

    // === HELPER INTERFACES AND CLASSES === //

    /**
     * Functional interface for smooth animation updates.
     * This allows for flexible progress updating across different stat types.
     */
    @FunctionalInterface
    private interface ProgressUpdater {
        /**
         * Updates the progress value for animation interpolation.
         *
         * @param newProgress The new progress value to set
         */
        void updateProgress(float newProgress);

        /**
         * Gets the current progress value for interpolation calculations.
         * Default implementation returns 0.0f - should be overridden when needed.
         *
         * @return The current progress value
         */
        default float getCurrentProgress() {
            return 0.0f;
        }
    }

    /**
     * Data snapshot containing all cultivation information for consistent rendering using unified attachments.
     * This prevents inconsistencies that could occur if attachments change mid-render.
     * Uses the updated attachment types that properly extend PlayerAttachment.
     *
     * @param cultivation The player's cultivation attachment
     * @param stats The player's stats attachment (replaces the old resource attachment)
     * @param talents The player's talents attachment
     */
    private record CultivationDataSnapshot(
        PlayerCultivationAttachment cultivation,
        PlayerStatsAttachment stats,
        PlayerTalentsAttachment talents
    ) {
        /**
         * Validates that all attachments in the snapshot are properly initialized.
         *
         * @return true if all attachments are valid and have debug capabilities
         */
        public boolean isValid() {
            return cultivation != null && stats != null && talents != null &&
                cultivation.debugEnabled() && stats.debugEnabled() && talents.debugEnabled();
        }
    }

    // === EVENT HANDLERS === //

    // === EVENT HANDLERS === //


    /**
     * Event handler for attachment updates.
     * This ensures the HUD refreshes when cultivation data changes.
     * Utilizes the generic PlayerAttachment debugging capabilities.
     *
     * @param event The attachment update event
     */
    @SubscribeEvent
    public void onAttachmentUpdate(AttachmentUpdateEventHandler event) {
        if (event.getPlayer() != null && event.getPlayer().level().isClientSide) {
            // Call the static invalidateCache method to clear cached data
            invalidateCache();

            // Use debug logging if available
            if (instance != null) {
                ImmortalCultivation.LOGGER.debug("[QIHUD] Attachment update received, marking for refresh");
            }
        }
    }

    /**
     * Invalidates the cached snapshot when attachments update.
     * This forces the HUD to re-gather cultivation data on the next render.
     */
    public static void invalidateCache() {
        cachedSnapshot = null;
        needsUpdate = true;
        updateCounter = UPDATE_FREQUENCY; // Force immediate update on next render

        if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
            ImmortalCultivation.LOGGER.debug("[QIHUD] Cache invalidated, forcing data refresh");
        }
    }
}
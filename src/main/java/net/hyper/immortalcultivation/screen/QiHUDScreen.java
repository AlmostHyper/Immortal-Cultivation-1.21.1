package net.hyper.immortalcultivation.screen;

import net.hyper.immortalcultivation.attachment.PlayerRealmAttachment;
import net.hyper.immortalcultivation.cultivation.ICultivationData;
import net.hyper.immortalcultivation.cultivation.IQiData;
import net.hyper.immortalcultivation.cultivation.realms.CultivationRealm;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class QiHUDScreen {

    // Design constants - adjusted for dual stats
    private static final int CIRCLE_SIZE = 45; // Slightly larger for both stats
    private static final int CIRCLE_RADIUS = CIRCLE_SIZE / 2;
    private static final int PADDING = 10;
    private static final int TEXT_OFFSET_X = 25; // More space for both stats
    private static final float TEXT_SCALE = 0.75f; // Slightly larger text
    private static final int STAT_SPACING = 16; // Space between Cultivation and Qi text

    // Colors for different stats
    private static final int BACKGROUND_COLOR = 0x90000000;
    private static final int BORDER_COLOR = 0xFF8B4513;

    // Cultivation colors
    private static final int CULTIVATION_COLOR_LOW = 0xFF4A5D23;
    private static final int CULTIVATION_COLOR_MID = 0xFF6B8E23;
    private static final int CULTIVATION_COLOR_HIGH = 0xFF9ACD32;
    private static final int CULTIVATION_COLOR_FULL = 0xFFFFD700;

    // Qi colors
    private static final int QI_COLOR_LOW = 0xFF2E4A8B;
    private static final int QI_COLOR_MID = 0xFF4169E1;
    private static final int QI_COLOR_HIGH = 0xFF6495ED;
    private static final int QI_COLOR_FULL = 0xFF00BFFF;

    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int TEXT_SHADOW = 0x80000000;

    // Performance variables for both stats
    private static float cultivationDisplayProgress = 0.0f;
    private static float qiDisplayProgress = 0.0f;
    private static long lastCultivation = -1;
    private static long lastMaxCultivation = -1;
    private static long lastQi = -1;
    private static long lastMaxQi = -1;
    private static int updateCounter = 0;
    private static final int UPDATE_FREQUENCY = 10; // Update every 10 ticks for max performance

    // Cached strings for both stats
    private static String cachedCultivationText = "";
    private static String cachedQiText = "";
    private static String cachedRealmText = "";
    private static String cachedCharTranslation = "";
    private static int cacheUpdateCounter = 0;

    public static void renderQiScreen(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;

        if (player == null || minecraft.options.hideGui) return;

        // Get player data for both stats
        ICultivationData cultivationData = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
        IQiData qiData = player.getData(ICModAttachmentTypes.PLAYER_QI); // Adjust this to your Qi attachment type
        PlayerRealmAttachment realmData = player.getData(ICModAttachmentTypes.PLAYER_REALM.get());

        if (cultivationData == null || qiData == null || realmData == null) return;

        long currentCultivation = cultivationData.getCurrentCultivation();
        long maxCultivation = cultivationData.getMaxCultivation();
        long currentQi = qiData.getCurrentQi();
        long maxQi = qiData.getMaxQi();

        // Ultra-throttled updates for both stats
        updateCounter++;
        if (updateCounter >= UPDATE_FREQUENCY) {
            updateCounter = 0;
            updateAnimations(currentCultivation, maxCultivation, currentQi, maxQi);

            // Update cached strings less frequently
            cacheUpdateCounter++;
            if (cacheUpdateCounter >= 3) { // Update text cache every 30 ticks
                cacheUpdateCounter = 0;
                updateTextCache(currentCultivation, maxCultivation, currentQi, maxQi, realmData.getCurrentRealm());
            }
        }

        // Calculate position
        int centerX = PADDING + CIRCLE_RADIUS;
        int centerY = PADDING + CIRCLE_RADIUS;

        // Render the dual-stat HUD
        renderDualStatHUD(guiGraphics, centerX, centerY, realmData.getCurrentRealm());
    }

    private static void updateAnimations(long currentCultivation, long maxCultivation, long currentQi, long maxQi) {
        // Update cultivation animation
        if (currentCultivation != lastCultivation || maxCultivation != lastMaxCultivation) {
            lastCultivation = currentCultivation;
            lastMaxCultivation = maxCultivation;

            float targetProgress = maxCultivation > 0 ? (float) currentCultivation / maxCultivation : 0.0f;
            float diff = targetProgress - cultivationDisplayProgress;
            if (Math.abs(diff) < 0.05f) {
                cultivationDisplayProgress = targetProgress;
            } else {
                cultivationDisplayProgress += diff * 0.2f;
            }
        }

        // Update Qi animation
        if (currentQi != lastQi || maxQi != lastMaxQi) {
            lastQi = currentQi;
            lastMaxQi = maxQi;

            float targetProgress = maxQi > 0 ? (float) currentQi / maxQi : 0.0f;
            float diff = targetProgress - qiDisplayProgress;
            if (Math.abs(diff) < 0.05f) {
                qiDisplayProgress = targetProgress;
            } else {
                qiDisplayProgress += diff * 0.2f;
            }
        }
    }

    private static void updateTextCache(long currentCultivation, long maxCultivation, long currentQi, long maxQi, CultivationRealm realm) {
        // Cache cultivation text
        float cultivationPercentage = maxCultivation > 0 ? (float) currentCultivation / maxCultivation * 100 : 0;
        cachedCultivationText = String.format("Cultivation: %.0f%% (%d/%d)", cultivationPercentage, currentCultivation, maxCultivation);

        // Cache Qi text
        float qiPercentage = maxQi > 0 ? (float) currentQi / maxQi * 100 : 0;
        cachedQiText = String.format("Qi: %.0f%% (%d/%d)", qiPercentage, currentQi, maxQi);

        // Cache realm text
        cachedRealmText = realm.getDisplayName();

        String chineseChar = getRealmSymbol(realm);
        String englishTranslation = getEnglishTranslation(realm);
        cachedCharTranslation = chineseChar + " (" + englishTranslation + ")";
    }

    private static void renderDualStatHUD(GuiGraphics guiGraphics, int centerX, int centerY, CultivationRealm realm) {
        // Render the circular progress indicator with dual stats
        renderDualStatCircle(guiGraphics, centerX, centerY);

        // Render text information
        renderDualStatText(guiGraphics, centerX, centerY, realm);
    }

    private static void renderDualStatCircle(GuiGraphics guiGraphics, int centerX, int centerY) {
        int size = CIRCLE_SIZE - 4;
        int halfSize = size / 2;

        // Background square
        guiGraphics.fill(centerX - halfSize, centerY - halfSize,
            centerX + halfSize, centerY + halfSize, BACKGROUND_COLOR);

        // Border
        guiGraphics.fill(centerX - halfSize - 1, centerY - halfSize - 1,
            centerX + halfSize + 1, centerY - halfSize, BORDER_COLOR); // Top
        guiGraphics.fill(centerX - halfSize - 1, centerY + halfSize,
            centerX + halfSize + 1, centerY + halfSize + 1, BORDER_COLOR); // Bottom
        guiGraphics.fill(centerX - halfSize - 1, centerY - halfSize,
            centerX - halfSize, centerY + halfSize, BORDER_COLOR); // Left
        guiGraphics.fill(centerX + halfSize, centerY - halfSize,
            centerX + halfSize + 1, centerY + halfSize, BORDER_COLOR); // Right

    }

    private static void renderDualStatText(GuiGraphics guiGraphics, int centerX, int centerY, CultivationRealm realm) {
        Font font = Minecraft.getInstance().font;

        // Chinese symbol in center
        String realmSymbol = getRealmSymbol(realm);
        int symbolWidth = font.width(realmSymbol);
        int symbolX = centerX - symbolWidth / 2;
        int symbolY = centerY - 4;

        guiGraphics.drawString(font, realmSymbol, symbolX + 1, symbolY + 1, TEXT_SHADOW, false);
        guiGraphics.drawString(font, realmSymbol, symbolX, symbolY, getRealmColor(realm), false);

        // Scaled text to the right
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(TEXT_SCALE, TEXT_SCALE, 1.0f);

        int textX = (int) ((centerX + TEXT_OFFSET_X) / TEXT_SCALE);
        int baseY = (int) ((centerY - 20) / TEXT_SCALE);

        // Render all cached text
        if (!cachedCharTranslation.isEmpty()) {
            guiGraphics.drawString(font, cachedCharTranslation, textX, baseY, getRealmColor(realm), false);
        }

        if (!cachedRealmText.isEmpty()) {
            guiGraphics.drawString(font, cachedRealmText, textX, baseY + 12, TEXT_COLOR, false);
        }

        if (!cachedCultivationText.isEmpty()) {
            guiGraphics.drawString(font, cachedCultivationText, textX, baseY + 24, getSimpleCultivationColor(cultivationDisplayProgress), false);
        }

        if (!cachedQiText.isEmpty()) {
            guiGraphics.drawString(font, cachedQiText, textX, baseY + 36, getSimpleQiColor(qiDisplayProgress), false);
        }

        guiGraphics.pose().popPose();
    }

    // Get Chinese character for realm (starting with Mortal)
    private static String getRealmSymbol(CultivationRealm realm) {
        String id = realm.getId().toLowerCase();

        if (id.contains("mortal")) return "凡";        // Mortal
        if (id.contains("condensation")) return "氣";   // Qi Condensation
        if (id.contains("foundation")) return "基";     // Foundation
        if (id.contains("core")) return "核";           // Core Formation
        if (id.contains("nascent")) return "嬰";        // Nascent Soul
        if (id.contains("deity")) return "神";          // Deity Transformation

        return "凡"; // Default to Mortal
    }

    // Get English translation for realm (starting with Mortal)
    private static String getEnglishTranslation(CultivationRealm realm) {
        String id = realm.getId().toLowerCase();

        if (id.contains("mortal")) return "Mortal";
        if (id.contains("condensation")) return "Energy";
        if (id.contains("foundation")) return "Foundation";
        if (id.contains("core")) return "Core";
        if (id.contains("nascent")) return "Soul";
        if (id.contains("deity")) return "Spirit";

        return "Mortal"; // Default to Mortal
    }

    // Color calculation for cultivation progress
    private static int getSimpleCultivationColor(float progress) {
        if (progress > 0.8f) return CULTIVATION_COLOR_FULL;
        if (progress > 0.6f) return CULTIVATION_COLOR_HIGH;
        if (progress > 0.3f) return CULTIVATION_COLOR_MID;
        return CULTIVATION_COLOR_LOW;
    }

    // Color calculation for Qi progress
    private static int getSimpleQiColor(float progress) {
        if (progress > 0.8f) return QI_COLOR_FULL;
        if (progress > 0.6f) return QI_COLOR_HIGH;
        if (progress > 0.3f) return QI_COLOR_MID;
        return QI_COLOR_LOW;
    }

    private static int getRealmColor(CultivationRealm realm) {
        int realmColor = realm.getRealmColor();
        return (realmColor == 0) ? 0xFFFFD700 : (realmColor | 0xFF000000);
    }
}
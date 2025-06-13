package net.hyper.immortalcultivation.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.component.PlayerRealmComponent;
import net.hyper.immortalcultivation.cultivation.PlayerQiData;
import net.hyper.immortalcultivation.cultivation.realms.CultivationRealm;
import net.hyper.immortalcultivation.registry.ICModAttachments;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class QiHUDScreen {

    // Design constants - smaller and simpler
    private static final int CIRCLE_SIZE = 40; // Much smaller circle
    private static final int CIRCLE_RADIUS = CIRCLE_SIZE / 2;
    private static final int PADDING = 10;
    private static final int TEXT_OFFSET_X = 20; // Closer to circle
    private static final float TEXT_SCALE = 0.7f; // Smaller text

    // Colors
    private static final int BACKGROUND_COLOR = 0x90000000;
    private static final int BORDER_COLOR = 0xFF8B4513;
    private static final int QI_COLOR_LOW = 0xFF4A5D23;
    private static final int QI_COLOR_MID = 0xFF6B8E23;
    private static final int QI_COLOR_HIGH = 0xFF9ACD32;
    private static final int QI_COLOR_FULL = 0xFFFFD700;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int TEXT_SHADOW = 0x80000000;

    // Ultra-performance variables
    private static float displayProgress = 0.0f;
    private static long lastQi = -1;
    private static long lastMaxQi = -1;
    private static int updateCounter = 0;
    private static final int UPDATE_FREQUENCY = 10; // Update every 10 ticks for max performance

    // Cached strings to avoid repeated allocations
    private static String cachedProgressText = "";
    private static String cachedRealmText = "";
    private static String cachedCharTranslation = "";
    private static int cacheUpdateCounter = 0;

    public static void renderQiScreen(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;

        if (player == null || minecraft.options.hideGui) return;

        // Get player data
        PlayerQiData qiData = player.getData(ICModAttachments.PLAYER_QI.get());
        PlayerRealmComponent realmData = player.getData(ICModAttachments.PLAYER_REALM.get());

        if (qiData == null || realmData == null) return;

        long currentQi = qiData.getCurrentQi();
        long maxQi = qiData.getMaxQi();

        // Ultra-throttled updates
        updateCounter++;
        if (updateCounter >= UPDATE_FREQUENCY) {
            updateCounter = 0;
            updateAnimation(currentQi, maxQi);

            // Update cached strings less frequently
            cacheUpdateCounter++;
            if (cacheUpdateCounter >= 3) { // Update text cache every 30 ticks
                cacheUpdateCounter = 0;
                updateTextCache(currentQi, maxQi, realmData.getCurrentRealm());
            }
        }

        // Calculate position
        int centerX = PADDING + CIRCLE_RADIUS;
        int centerY = PADDING + CIRCLE_RADIUS;

        // Ultra-simple rendering
        renderUltraSimpleQiHUD(guiGraphics, centerX, centerY, realmData.getCurrentRealm());
    }

    private static void updateAnimation(long currentQi, long maxQi) {
        if (currentQi != lastQi || maxQi != lastMaxQi) {
            lastQi = currentQi;
            lastMaxQi = maxQi;

            float targetProgress = maxQi > 0 ? (float) currentQi / maxQi : 0.0f;

            // Simple step animation (no smooth lerp for performance)
            float diff = targetProgress - displayProgress;
            if (Math.abs(diff) < 0.05f) {
                displayProgress = targetProgress;
            } else {
                displayProgress += diff * 0.2f; // Faster animation
            }
        }
    }

    private static void updateTextCache(long currentQi, long maxQi, CultivationRealm realm) {
        // Cache frequently used strings to avoid allocations
        float percentage = maxQi > 0 ? (float) currentQi / maxQi * 100 : 0;
        cachedProgressText = String.format("%.0f%% (%d/%d)", percentage, currentQi, maxQi);
        cachedRealmText = realm.getDisplayName();

        String chineseChar = getRealmSymbol(realm);
        String englishTranslation = getEnglishTranslation(realm);
        cachedCharTranslation = chineseChar + " (" + englishTranslation + ")";
    }

    private static void renderUltraSimpleQiHUD(GuiGraphics guiGraphics, int centerX, int centerY, CultivationRealm realm) {
        // Method 1: Extremely simple square "circle" (fastest)
        renderSquareCircle(guiGraphics, centerX, centerY);

        // Method 2: Minimal text rendering
        renderMinimalText(guiGraphics, centerX, centerY, realm);
    }

    private static void renderSquareCircle(GuiGraphics guiGraphics, int centerX, int centerY) {
        int size = CIRCLE_SIZE - 4; // Slightly smaller for border effect
        int halfSize = size / 2;

        // Background square (1 draw call)
        guiGraphics.fill(centerX - halfSize, centerY - halfSize,
            centerX + halfSize, centerY + halfSize, BACKGROUND_COLOR);

        // Simple border (4 draw calls)
        guiGraphics.fill(centerX - halfSize - 1, centerY - halfSize - 1,
            centerX + halfSize + 1, centerY - halfSize, BORDER_COLOR); // Top
        guiGraphics.fill(centerX - halfSize - 1, centerY + halfSize,
            centerX + halfSize + 1, centerY + halfSize + 1, BORDER_COLOR); // Bottom
        guiGraphics.fill(centerX - halfSize - 1, centerY - halfSize,
            centerX - halfSize, centerY + halfSize, BORDER_COLOR); // Left
        guiGraphics.fill(centerX + halfSize, centerY - halfSize,
            centerX + halfSize + 1, centerY + halfSize, BORDER_COLOR); // Right

        // Progress fill (1 draw call)
        if (displayProgress > 0) {
            int fillHeight = (int) (size * displayProgress);
            int fillY = centerY + halfSize - fillHeight;
            int progressColor = getSimpleProgressColor(displayProgress);

            guiGraphics.fill(centerX - halfSize, fillY,
                centerX + halfSize, centerY + halfSize, progressColor);
        }
    }

    private static void renderMinimalText(GuiGraphics guiGraphics, int centerX, int centerY, CultivationRealm realm) {
        Font font = Minecraft.getInstance().font;

        // Chinese symbol in center (2 draw calls)
        String qiSymbol = getRealmSymbol(realm);
        int symbolWidth = font.width(qiSymbol);
        int symbolX = centerX - symbolWidth / 2;
        int symbolY = centerY - 4;

        guiGraphics.drawString(font, qiSymbol, symbolX + 1, symbolY + 1, TEXT_SHADOW, false);
        guiGraphics.drawString(font, qiSymbol, symbolX, symbolY, getRealmColor(realm), false);

        // Scaled text to the right
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(TEXT_SCALE, TEXT_SCALE, 1.0f);

        int textX = (int) ((centerX + TEXT_OFFSET_X) / TEXT_SCALE);
        int baseY = (int) ((centerY - 15) / TEXT_SCALE);

        // Only render cached strings (3 draw calls total)
        if (!cachedCharTranslation.isEmpty()) {
            guiGraphics.drawString(font, cachedCharTranslation, textX, baseY, getRealmColor(realm), false);
        }

        if (!cachedRealmText.isEmpty()) {
            guiGraphics.drawString(font, cachedRealmText, textX, baseY + 12, TEXT_COLOR, false);
        }

        if (!cachedProgressText.isEmpty()) {
            guiGraphics.drawString(font, cachedProgressText, textX, baseY + 24, 0xFF888888, false);
        }

        guiGraphics.pose().popPose();
    }

    // Get Chinese character (simple lookup)
    private static String getRealmSymbol(CultivationRealm realm) {
        String id = realm.getId().toLowerCase();

        if (id.contains("condensation")) return "氣";
        if (id.contains("foundation")) return "基";
        if (id.contains("core")) return "核";
        if (id.contains("nascent")) return "嬰";
        if (id.contains("deity")) return "神";

        return "氣"; // Default
    }

    // Get English translation (simple lookup)
    private static String getEnglishTranslation(CultivationRealm realm) {
        String id = realm.getId().toLowerCase();

        if (id.contains("condensation")) return "Energy";
        if (id.contains("foundation")) return "Foundation";
        if (id.contains("core")) return "Core";
        if (id.contains("nascent")) return "Soul";
        if (id.contains("deity")) return "Spirit";

        return "Energy"; // Default
    }

    // Extremely simple color calculation
    private static int getSimpleProgressColor(float progress) {
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
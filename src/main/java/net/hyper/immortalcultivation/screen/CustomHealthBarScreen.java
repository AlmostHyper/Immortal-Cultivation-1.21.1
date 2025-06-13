package net.hyper.immortalcultivation.screen;

import net.hyper.immortalcultivation.component.PlayerQiComponent;
import net.hyper.immortalcultivation.component.PlayerRealmComponent;
import net.hyper.immortalcultivation.registry.ICModAttachments;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CustomHealthBarScreen {

    // Vanilla health bar dimensions
    private static final int BAR_WIDTH = 80; // Same as vanilla health/food bar
    private static final int BAR_HEIGHT = 9;  // Same as vanilla
    private static final int HEART_SIZE = 9;  // Vanilla heart size for reference

    // Xianxia color palette
    private static final int VITALITY_GOLD = 0xFFD4AF37;     // Rich gold for high vitality
    private static final int CRIMSON_QI = 0xFFDC143C;        // Deep crimson for life force
    private static final int JADE_GREEN = 0xFF00A86B;        // Jade green for balance
    private static final int PHOENIX_RED = 0xFFB22222;       // Phoenix red for power
    private static final int SUNSET_ORANGE = 0xFFFF8C00;     // Sunset orange for energy
    private static final int MYSTIC_PURPLE = 0xFF9370DB;     // Mystic purple for high cultivation

    // UI colors
    private static final int DARK_BORDER = 0xFF2F1B14;       // Dark brown border
    private static final int BACKGROUND = 0x80000000;        // Semi-transparent black
    private static final int TEXT_GOLD = 0xFFFFD700;         // Gold text
    private static final int TEXT_SHADOW = 0x80000000;       // Text shadow

    // Performance optimization
    private static float displayHealth = 0.0f;
    private static float displayMaxHealth = 20.0f;
    private static int updateCounter = 0;
    private static final int UPDATE_FREQUENCY = 3;

    // Cached text
    private static String cachedHealthText = "";
    private static String cachedPercentText = "";
    private static int textCacheCounter = 0;

    public static void renderXianxiaHealthBar(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;

        if (player == null || minecraft.options.hideGui || player.isCreative()) return;

        float currentHealth = player.getHealth();
        float maxHealth = player.getMaxHealth();

        // Only show for high health values
        if (maxHealth <= 20.0f) return;

        // Throttled updates for performance
        updateCounter++;
        if (updateCounter >= UPDATE_FREQUENCY) {
            updateCounter = 0;
            updateHealthAnimation(currentHealth, maxHealth);

            // Update text cache
            textCacheCounter++;
            if (textCacheCounter >= 5) {
                textCacheCounter = 0;
                updateTextCache(displayHealth, displayMaxHealth);
            }
        }

        // Position at vanilla health bar location
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();

        int x = screenWidth / 2 - 91; // Vanilla health bar X position
        int y = screenHeight - 39;    // Vanilla health bar Y position

        renderVanillaSizedHealthBar(guiGraphics, x, y);
    }

    private static void updateHealthAnimation(float currentHealth, float maxHealth) {
        float healthDiff = currentHealth - displayHealth;
        float maxHealthDiff = maxHealth - displayMaxHealth;

        if (Math.abs(healthDiff) < 0.5f) {
            displayHealth = currentHealth;
        } else {
            displayHealth += healthDiff * 0.12f;
        }

        if (Math.abs(maxHealthDiff) < 0.5f) {
            displayMaxHealth = maxHealth;
        } else {
            displayMaxHealth += maxHealthDiff * 0.12f;
        }
    }

    private static void updateTextCache(float health, float maxHealth) {
        cachedHealthText = formatXianxiaNumber(health) + " / " + formatXianxiaNumber(maxHealth);

        float percentage = maxHealth > 0 ? (health / maxHealth) * 100 : 0;
        cachedPercentText = String.format("%.0f%%", percentage);
    }

    private static void renderVanillaSizedHealthBar(GuiGraphics guiGraphics, int x, int y) {
        // Calculate health percentage
        float healthPercentage = displayMaxHealth > 0 ? displayHealth / displayMaxHealth : 0;
        int filledWidth = (int) (BAR_WIDTH * healthPercentage);

        // Main health bar background
        guiGraphics.fill(x - 1, y - 1, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, DARK_BORDER);
        guiGraphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, BACKGROUND);

        // Render health fill with gradient effect
        if (filledWidth > 0) {
            int healthColor = getXianxiaHealthColor(healthPercentage);

            // Main health fill
            guiGraphics.fill(x, y, x + filledWidth, y + BAR_HEIGHT, healthColor);

            // Gradient shine effect (top portion)
            int shineColor = lightenColor(healthColor, 0.3f);
            guiGraphics.fill(x, y, x + filledWidth, y + 2, shineColor);

            // Inner glow effect (subtle)
            if (filledWidth > 4) {
                int glowColor = lightenColor(healthColor, 0.15f);
                guiGraphics.fill(x + 1, y + 1, x + filledWidth - 1, y + BAR_HEIGHT - 1, glowColor);
            }
        }

        // Decorative border accents (Xianxia style)
        renderXianxiaAccents(guiGraphics, x, y, healthPercentage);

        // Health text
        renderHealthText(guiGraphics, x, y);

        // Percentage text on the side
        renderPercentageText(guiGraphics, x + BAR_WIDTH + 5, y);
    }

    private static void renderXianxiaAccents(GuiGraphics guiGraphics, int x, int y, float healthPercentage) {
        // Corner accents (small decorative elements)
        int accentColor = healthPercentage > 0.8f ? VITALITY_GOLD :
            healthPercentage > 0.5f ? SUNSET_ORANGE :
                healthPercentage > 0.25f ? PHOENIX_RED : CRIMSON_QI;

        // Top corners
        guiGraphics.fill(x - 1, y - 1, x + 2, y, accentColor);
        guiGraphics.fill(x + BAR_WIDTH - 1, y - 1, x + BAR_WIDTH + 2, y, accentColor);

        // Bottom corners
        guiGraphics.fill(x - 1, y + BAR_HEIGHT, x + 2, y + BAR_HEIGHT + 1, accentColor);
        guiGraphics.fill(x + BAR_WIDTH - 1, y + BAR_HEIGHT, x + BAR_WIDTH + 2, y + BAR_HEIGHT + 1, accentColor);
    }

    private static void renderHealthText(GuiGraphics guiGraphics, int x, int y) {
        Font font = Minecraft.getInstance().font;

        if (cachedHealthText.isEmpty()) return;

        // Calculate text position (centered above bar)
        int textWidth = font.width(cachedHealthText);
        int textX = x + (BAR_WIDTH - textWidth) / 2;
        int textY = y - 11;

        // Scale text to fit nicely
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(0.75f, 0.75f, 1.0f);

        float scaledX = textX / 0.75f;
        float scaledY = textY / 0.75f;

        // Text shadow
        guiGraphics.drawString(font, cachedHealthText, scaledX + 1, scaledY + 1, TEXT_SHADOW, false);
        // Main text in gold
        guiGraphics.drawString(font, cachedHealthText, scaledX, scaledY, TEXT_GOLD, false);

        guiGraphics.pose().popPose();
    }

    private static void renderPercentageText(GuiGraphics guiGraphics, int x, int y) {
        Font font = Minecraft.getInstance().font;

        if (cachedPercentText.isEmpty()) return;

        // Position percentage to the right of the health bar
        int textY = y + (BAR_HEIGHT - 8) / 2; // Center vertically with bar

        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(0.8f, 0.8f, 1.0f);

        float scaledX = x / 0.8f;
        float scaledY = textY / 0.8f;

        // Determine color based on health percentage
        float healthPercentage = displayMaxHealth > 0 ? displayHealth / displayMaxHealth : 0;
        int percentColor = getXianxiaHealthColor(healthPercentage);

        // Text shadow
        guiGraphics.drawString(font, cachedPercentText, scaledX + 1, scaledY + 1, TEXT_SHADOW, false);
        // Main percentage text
        guiGraphics.drawString(font, cachedPercentText, scaledX, scaledY, percentColor, false);

        guiGraphics.pose().popPose();
    }

    private static int getXianxiaHealthColor(float percentage) {
        if (percentage >= 0.9f) {
            return VITALITY_GOLD;      // 90-100% - Golden vitality
        } else if (percentage >= 0.75f) {
            return interpolateColor(JADE_GREEN, VITALITY_GOLD, (percentage - 0.75f) * 4);
        } else if (percentage >= 0.5f) {
            return interpolateColor(SUNSET_ORANGE, JADE_GREEN, (percentage - 0.5f) * 4);
        } else if (percentage >= 0.25f) {
            return interpolateColor(PHOENIX_RED, SUNSET_ORANGE, (percentage - 0.25f) * 4);
        } else {
            return interpolateColor(CRIMSON_QI, PHOENIX_RED, percentage * 4);
        }
    }

    private static String formatXianxiaNumber(float health) {
        long healthLong = Math.round(health);

        if (healthLong >= 1_000_000_000L) {
            return String.format("%.1f Billion", healthLong / 1_000_000_000.0);
        } else if (healthLong >= 1_000_000L) {
            return String.format("%.1f Million", healthLong / 1_000_000.0);
        } else if (healthLong >= 10_000L) {
            return String.format("%.1f万", healthLong / 10_000.0); // Chinese "wan" for 10,000
        } else if (healthLong >= 1_000L) {
            return String.format("%.1fK", healthLong / 1_000.0);
        } else {
            return String.valueOf(healthLong);
        }
    }

    // Utility methods for color manipulation
    private static int interpolateColor(int color1, int color2, float factor) {
        factor = Math.max(0, Math.min(1, factor));

        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int a2 = (color2 >> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        int a = (int)(a1 + (a2 - a1) * factor);
        int r = (int)(r1 + (r2 - r1) * factor);
        int g = (int)(g1 + (g2 - g1) * factor);
        int b = (int)(b1 + (b2 - b1) * factor);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int lightenColor(int color, float amount) {
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        r = Math.min(255, (int)(r + (255 - r) * amount));
        g = Math.min(255, (int)(g + (255 - g) * amount));
        b = Math.min(255, (int)(b + (255 - b) * amount));

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    // Force show methods
    public static void forceShow() {
        updateCounter = UPDATE_FREQUENCY;
        textCacheCounter = 5;
    }

    public static boolean isVisible() {
        return true;
    }
}

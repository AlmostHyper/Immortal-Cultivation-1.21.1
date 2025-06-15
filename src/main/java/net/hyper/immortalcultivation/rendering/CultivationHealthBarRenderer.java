package net.hyper.immortalcultivation.rendering;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = "yourmodid", bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class CultivationHealthBarRenderer {

    // Xianxia-themed texture locations
    private static final ResourceLocation QI_BAR_TEXTURE = ResourceLocation.fromNamespaceAndPath("yourmodid", "textures/gui/qi_vitality_bar.png");
    private static final ResourceLocation DRAGON_ORNAMENT = ResourceLocation.fromNamespaceAndPath("yourmodid", "textures/gui/dragon_ornament.png");

    // Traditional Chinese color palette
    private static final int JADE_GREEN = 0xFF00C851;        // Vibrant jade
    private static final int DEEP_JADE = 0xFF1B5E20;         // Dark jade
    private static final int IMPERIAL_GOLD = 0xFFFFD700;     // Gold accents
    private static final int CRIMSON_RED = 0xFFDC143C;       // For low health
    private static final int PHOENIX_ORANGE = 0xFFFF6F00;    // Medium health
    private static final int DRAGON_BLACK = 0xFF212121;      // Border/background
    private static final int SPIRIT_WHITE = 0xFFF5F5F5;      // Text color

    // Bar dimensions with traditional proportions
    private static final int BAR_WIDTH = 120;
    private static final int BAR_HEIGHT = 16;
    private static final int BAR_X_OFFSET = 15;
    private static final int BAR_Y_OFFSET = 15;
    private static final int ORNAMENT_SIZE = 12;

    @SubscribeEvent
    public static void onRenderGuiLayerPre(RenderGuiLayerEvent.Pre event) {
        // Cancel the vanilla health rendering
        if (event.getName().equals(VanillaGuiLayers.PLAYER_HEALTH)) {
            event.setCanceled(true);
        }
    }

    public static LayeredDraw.Layer createQiVitalityBarLayer() {
        return new LayeredDraw.Layer() {
            @Override
            public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
                renderXianxiaHealthBar(guiGraphics);
            }
        };
    }

    private static void renderXianxiaHealthBar(GuiGraphics guiGraphics) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null || mc.options.hideGui) {
            return;
        }

        // Get cultivation vitality values
        float currentQi = getCurrentQiVitality(player);
        float maxQi = getMaxQiVitality(player);

        if (maxQi <= 0) return;

        // Calculate qi percentage
        float qiPercentage = Math.max(0.0f, Math.min(1.0f, currentQi / maxQi));

        // Render the Xianxia-styled health bar
        renderQiVitalityBar(guiGraphics, BAR_X_OFFSET, BAR_Y_OFFSET, BAR_WIDTH, BAR_HEIGHT,
            qiPercentage, currentQi, maxQi);
    }

    private static void renderQiVitalityBar(GuiGraphics guiGraphics, int x, int y, int width, int height,
                                            float percentage, float current, float max) {

        RenderSystem.enableBlend();

        // Traditional Chinese layered border design
        // Outer black border (represents the void/darkness)
        guiGraphics.fill(x - 2, y - 2, x + width + 2, y + height + 2, DRAGON_BLACK);

        // Inner gold accent border (imperial touch)
        guiGraphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, IMPERIAL_GOLD);

        // Main background (deep jade - representing dormant potential)
        guiGraphics.fill(x, y, x + width, y + height, DEEP_JADE);

        // Qi/Vitality fill with traditional gradient effect
        int fillWidth = (int) (width * percentage);
        if (fillWidth > 0) {
            // Create a subtle gradient effect by layering colors
            int qiColor = getQiVitalityColor(percentage);

            // Main qi fill
            guiGraphics.fill(x, y, x + fillWidth, y + height, qiColor);

            // Add inner glow effect (lighter version at top)
            int glowColor = lightenColor(qiColor, 0.3f);
            guiGraphics.fill(x, y, x + fillWidth, y + height/3, glowColor);

            // Add flowing energy effect (animated shimmer would go here in full implementation)
            if (percentage > 0.1f) {
                int shimmerColor = lightenColor(qiColor, 0.5f);
                // Create subtle energy pattern
                for (int i = 0; i < fillWidth; i += 8) {
                    if ((System.currentTimeMillis() / 100 + i) % 16 < 4) {
                        guiGraphics.fill(x + i, y + 1, x + i + 2, y + height - 1, shimmerColor);
                    }
                }
            }
        }

        // Render dragon/phoenix ornaments at the ends
        renderTraditionalOrnaments(guiGraphics, x, y, width, height, percentage);

        // Render vitality text with Chinese-style formatting
        renderVitalityText(guiGraphics, x, y, width, height, current, max, percentage);

        RenderSystem.disableBlend();
    }

    private static void renderTraditionalOrnaments(GuiGraphics guiGraphics, int x, int y, int width, int height, float percentage) {
        // Left ornament (Yin symbol or dragon head)
        drawChineseOrnament(guiGraphics, x - ORNAMENT_SIZE - 2, y + height/2 - ORNAMENT_SIZE/2, true, percentage > 0.5f);

        // Right ornament (Yang symbol or phoenix tail)
        drawChineseOrnament(guiGraphics, x + width + 2, y + height/2 - ORNAMENT_SIZE/2, false, percentage > 0.8f);
    }

    private static void drawChineseOrnament(GuiGraphics guiGraphics, int x, int y, boolean isYin, boolean isActive) {
        int ornamentColor = isActive ? IMPERIAL_GOLD : DEEP_JADE;
        int accentColor = isActive ? SPIRIT_WHITE : DRAGON_BLACK;

        // Simple traditional pattern (in a full mod, you'd use textures)
        // Outer circle
        guiGraphics.fill(x, y + 2, x + ORNAMENT_SIZE, y + ORNAMENT_SIZE - 2, ornamentColor);
        guiGraphics.fill(x + 2, y, x + ORNAMENT_SIZE - 2, y + ORNAMENT_SIZE, ornamentColor);

        // Inner accent
        if (isYin) {
            // Yin pattern (left side)
            guiGraphics.fill(x + 3, y + 3, x + ORNAMENT_SIZE/2, y + ORNAMENT_SIZE - 3, accentColor);
        } else {
            // Yang pattern (right side)
            guiGraphics.fill(x + ORNAMENT_SIZE/2, y + 3, x + ORNAMENT_SIZE - 3, y + ORNAMENT_SIZE - 3, accentColor);
        }
    }

    private static void renderVitalityText(GuiGraphics guiGraphics, int x, int y, int width, int height,
                                           float current, float max, float percentage) {
        // Traditional Chinese health terminology
        String vitalityText;
        if (percentage > 0.9f) {
            vitalityText = String.format("气血充盈 %.0f/%.0f", current, max); // "Qi and Blood Abundant"
        } else if (percentage > 0.7f) {
            vitalityText = String.format("生机勃勃 %.0f/%.0f", current, max); // "Full of Vitality"
        } else if (percentage > 0.5f) {
            vitalityText = String.format("元气 %.0f/%.0f", current, max);       // "Primordial Energy"
        } else if (percentage > 0.3f) {
            vitalityText = String.format("生命力 %.0f/%.0f", current, max);     // "Life Force"
        } else if (percentage > 0.1f) {
            vitalityText = String.format("虚弱 %.0f/%.0f", current, max);       // "Weakened"
        } else {
            vitalityText = String.format("濒死 %.0f/%.0f", current, max);       // "Near Death"
        }

        // For non-Chinese speakers, alternative English terms:
        // vitalityText = String.format("Qi: %.0f/%.0f", current, max);

        int textColor = percentage > 0.3f ? SPIRIT_WHITE : CRIMSON_RED;

        // Center the text with traditional shadow effect
        int textWidth = Minecraft.getInstance().font.width(vitalityText);
        int textX = x + (width - textWidth) / 2;
        int textY = y + (height - 8) / 2;

        // Traditional text shadow (black outline for visibility)
        guiGraphics.drawString(Minecraft.getInstance().font, vitalityText, textX + 1, textY + 1, DRAGON_BLACK, false);
        guiGraphics.drawString(Minecraft.getInstance().font, vitalityText, textX, textY, textColor, false);
    }

    private static int getQiVitalityColor(float percentage) {
        // Traditional Chinese color progression based on cultivation levels
        if (percentage > 0.8f) {
            // Transcendent level - Pure jade with gold highlights
            return blendColors(JADE_GREEN, IMPERIAL_GOLD, (percentage - 0.8f) * 5);
        } else if (percentage > 0.6f) {
            // High cultivation - Vibrant jade
            return JADE_GREEN;
        } else if (percentage > 0.4f) {
            // Moderate cultivation - Jade to phoenix orange transition
            return blendColors(JADE_GREEN, PHOENIX_ORANGE, (0.6f - percentage) * 5);
        } else if (percentage > 0.2f) {
            // Low cultivation - Phoenix orange
            return PHOENIX_ORANGE;
        } else {
            // Critical/mortal danger - Crimson red
            return blendColors(PHOENIX_ORANGE, CRIMSON_RED, (0.2f - percentage) * 5);
        }
    }

    private static int blendColors(int color1, int color2, float ratio) {
        ratio = Math.max(0, Math.min(1, ratio));

        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        int r = (int) (r1 * (1 - ratio) + r2 * ratio);
        int g = (int) (g1 * (1 - ratio) + g2 * ratio);
        int b = (int) (b1 * (1 - ratio) + b2 * ratio);

        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static int lightenColor(int color, float amount) {
        int r = Math.min(255, (int) (((color >> 16) & 0xFF) * (1 + amount)));
        int g = Math.min(255, (int) (((color >> 8) & 0xFF) * (1 + amount)));
        int b = Math.min(255, (int) ((color & 0xFF) * (1 + amount)));

        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    // Replace these methods with your actual cultivation attachment access
    private static float getCurrentQiVitality(Player player) {
        // Example: return player.getData(YourAttachments.CULTIVATION_DATA).getCurrentVitality();
        // For now, returning vanilla health as placeholder
        return player.getHealth();
    }

    private static float getMaxQiVitality(Player player) {
        // Example: return player.getData(YourAttachments.CULTIVATION_DATA).getMaxVitality();
        // For now, returning vanilla max health as placeholder
        return player.getMaxHealth();
    }
}

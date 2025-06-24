package net.hyper.immortalcultivation.rendering;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;

@OnlyIn(Dist.CLIENT)
public class DamageOverlayRenderer {

    private static float damageOverlayIntensity = 0.0f;
    private static int damageOverlayTicks = 0;
    private static final int MAX_OVERLAY_DURATION = 20; // 1 second at 20 TPS

    public static void init() {
        NeoForge.EVENT_BUS.register(DamageOverlayRenderer.class);
    }

    /**
     * Call this from your damage handling code to trigger visual feedback
     * @param damageAmount The amount of damage taken (for intensity scaling)
     * @param maxHealth The player's maximum health (for percentage calculation)
     */
    public static void triggerDamageOverlay(long damageAmount, long maxHealth) {
        if (maxHealth <= 0) return;

        // Calculate intensity based on damage percentage (0.0 to 1.0)
        float damagePercentage = Math.min(1.0f, (float) damageAmount / maxHealth);

        // Scale intensity (minimum 0.3 for small damage, maximum 0.9 for major damage)
        damageOverlayIntensity = Math.max(0.3f, Math.min(0.9f, damagePercentage * 2.0f));
        damageOverlayTicks = MAX_OVERLAY_DURATION;

        // Add screen shake for major damage
        if (damagePercentage > 0.2f) {
            triggerScreenShake(damagePercentage);
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (damageOverlayTicks > 0) {
            renderDamageOverlay(event.getGuiGraphics());
            damageOverlayTicks--;

            // Fade out the overlay
            if (damageOverlayTicks <= 0) {
                damageOverlayIntensity = 0.0f;
            }
        }
    }

    private static void renderDamageOverlay(GuiGraphics guiGraphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        // Calculate current alpha based on remaining time
        float alpha = damageOverlayIntensity * ((float) damageOverlayTicks / MAX_OVERLAY_DURATION);

        // Create pulsing effect
        float pulse = (float) Math.sin((MAX_OVERLAY_DURATION - damageOverlayTicks) * 0.5f) * 0.3f + 0.7f;
        alpha *= pulse;

        // Red overlay with calculated alpha
        int alphaInt = (int) (alpha * 255) << 24;
        int color = alphaInt | 0x00FF0000; // Red color

        // Render full-screen overlay
        guiGraphics.fill(0, 0, screenWidth, screenHeight, color);

        // Add vignette effect for more dramatic damage feedback
        renderVignetteEffect(guiGraphics, screenWidth, screenHeight, alpha * 0.5f);
    }

    private static void renderVignetteEffect(GuiGraphics guiGraphics, int width, int height, float intensity) {
        // Create a vignette effect that's darker at the edges
        int centerX = width / 2;
        int centerY = height / 2;
        float maxDistance = (float) Math.sqrt(centerX * centerX + centerY * centerY);

        // Simple approximation - render darker rectangles at screen edges
        int edgeAlpha = (int) (intensity * 128) << 24;
        int edgeColor = edgeAlpha | 0x00000000; // Black

        int vignetteSize = (int) (Math.min(width, height) * 0.15f * intensity);

        // Top and bottom edges
        guiGraphics.fill(0, 0, width, vignetteSize, edgeColor);
        guiGraphics.fill(0, height - vignetteSize, width, height, edgeColor);

        // Left and right edges
        guiGraphics.fill(0, 0, vignetteSize, height, edgeColor);
        guiGraphics.fill(width - vignetteSize, 0, width, height, edgeColor);
    }

    // === SCREEN SHAKE SYSTEM ===

    private static float shakeIntensity = 0.0f;
    private static int shakeTicks = 0;
    private static float shakeOffsetX = 0.0f;
    private static float shakeOffsetY = 0.0f;

    private static void triggerScreenShake(float damagePercentage) {
        shakeIntensity = Math.min(5.0f, damagePercentage * 8.0f); // Max 5 pixel shake
        shakeTicks = 10; // Half-second shake duration
    }

    public static void updateScreenShake() {
        if (shakeTicks > 0) {
            // Generate random offset within shake intensity
            shakeOffsetX = (float) (Math.random() - 0.5) * 2 * shakeIntensity;
            shakeOffsetY = (float) (Math.random() - 0.5) * 2 * shakeIntensity;

            shakeTicks--;

            // Gradually reduce shake intensity
            shakeIntensity *= 0.9f;
        } else {
            shakeOffsetX = 0.0f;
            shakeOffsetY = 0.0f;
            shakeIntensity = 0.0f;
        }
    }

    public static float getShakeOffsetX() {
        return shakeOffsetX;
    }

    public static float getShakeOffsetY() {
        return shakeOffsetY;
    }
}

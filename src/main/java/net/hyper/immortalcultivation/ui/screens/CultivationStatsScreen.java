package net.hyper.immortalcultivation.ui.screens;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.attachment.*;
import net.hyper.immortalcultivation.cultivation.realms.CultivationRealm;
import net.hyper.immortalcultivation.network.client.CultivationFocusTogglePacket;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.hyper.immortalcultivation.registry.ICModNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

// NEW IMPORTS: These provide access to OpenGL texture filtering controls
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL11;

/**
 * COMPLETE SOLUTION: General cultivation monitoring interface with responsive design,
 * pixel-perfect positioning, and sharp texture rendering for the unified cultivation system.
 *
 * Updated to work with the new unified attachment system:
 * - PlayerCultivationAttachment for realm, cultivation progress, focus, and breakthrough data
 * - PlayerResourceAttachment for health, qi energy, and body energy
 * - PlayerTalentAttachment for spiritual roots and physique
 * - PlayerCombatStatsAttachment for combat bonuses
 */
public class CultivationStatsScreen extends Screen {

    // === RESPONSIVE SIZING SYSTEM ===
    private static final int PREFERRED_WIDTH = 480;
    private static final int PREFERRED_HEIGHT = 520;
    private static final int MIN_WIDTH = 400;
    private static final int MIN_HEIGHT = 450;
    private static final int MAX_WIDTH = 600;
    private static final int MAX_HEIGHT = 700;
    private static final float MARGIN_PERCENTAGE = 0.03f;
    private static final int MIN_MARGIN = 10;
    private static final int MAX_MARGIN = 25;

    // === LAYOUT CONSTANTS ===
    private int windowWidth;
    private int windowHeight;
    private int windowX, windowY;
    private int margin;
    private int sectionSpacing;
    private int lineHeight;
    private int progressBarWidth;
    private int columnWidth;

    // === VISUAL CONSTANTS ===
    private static final int QI_COLOR = 0xFF87CEEB;
    private static final int BODY_COLOR = 0xFFFF6347;
    private static final int HEALTH_COLOR = 0xFFFF69B4;
    private static final int FOCUS_ACTIVE_COLOR = 0xFFFFD700;
    private static final int READY_COLOR = 0xFF90EE90;
    private static final int NOT_READY_COLOR = 0xFFFFB6C1;
    private static final int SECTION_HEADER_COLOR = 0xFFDDA0DD;
    private static final int PROGRESS_BAR_HEIGHT = 8;

    // === PLAYER DATA (Updated for unified system) ===
    private PlayerCultivationAttachment cultivationAttachment;   // Unified cultivation data
    private PlayerResourceAttachment resourceAttachment;        // Unified resource data
    private PlayerTalentAttachment talentAttachment;           // Unified talent data
    private PlayerCombatStatsAttachment combatStatsAttachment; // Combat bonuses

    public CultivationStatsScreen() {
        super(Component.literal("Cultivation Status Overview"));
    }

    // === PIXEL-PERFECT RENDERING HELPERS ===

    /**
     * Gets the current GUI scale factor that Minecraft is using.
     */
    private double getGuiScale() {
        return minecraft.getWindow().getGuiScale();
    }

    /**
     * Rounds a coordinate to be pixel-perfect at the current GUI scale.
     */
    private int alignToPixel(double coordinate) {
        double scale = getGuiScale();
        return (int) Math.round(coordinate / scale) * (int) scale;
    }

    /**
     * Helper methods for drawing text with pixel-perfect positioning.
     */
    private void drawStringAligned(GuiGraphics guiGraphics, String text, int x, int y, int color) {
        int alignedX = alignToPixel(x);
        int alignedY = alignToPixel(y);
        guiGraphics.drawString(font, text, alignedX, alignedY, color);
    }

    private void drawStringAligned(GuiGraphics guiGraphics, Component text, int x, int y, int color) {
        int alignedX = alignToPixel(x);
        int alignedY = alignToPixel(y);
        guiGraphics.drawString(font, text, alignedX, alignedY, color);
    }

    // === NEW: SHARP RENDERING SYSTEM ===

    /**
     * CORE SOLUTION: Enable sharp texture rendering mode
     */
    private void enableSharpRendering() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
    }

    /**
     * ESSENTIAL CLEANUP: Restore normal texture rendering mode
     */
    private void restoreNormalRendering() {
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
    }

    @Override
    public void init() {
        super.init();
        calculateResponsiveDimensionsPixelPerfect();
        calculateLayoutConstantsPixelPerfect();
        loadCultivationData();
        setupStatsInterfacePixelPerfect();
    }

    /**
     * UPDATED: Main render method with sharp rendering integration
     */
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        enableSharpRendering();

        try {
            renderWindowFramePixelPerfect(guiGraphics);
            renderCultivationOverview(guiGraphics);
            super.render(guiGraphics, mouseX, mouseY, partialTicks);
        } finally {
            restoreNormalRendering();
        }
    }

    /**
     * FIX for blurry background - Override renderBackground to prevent blur shader
     */
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, 0x80000000);
    }

    // === LAYOUT CALCULATION METHODS ===

    private void calculateResponsiveDimensionsPixelPerfect() {
        int availableWidth = width - 40;
        int availableHeight = height - 40;

        windowWidth = PREFERRED_WIDTH;
        windowHeight = PREFERRED_HEIGHT;

        if (windowWidth > availableWidth || windowHeight > availableHeight) {
            float widthScale = (float) availableWidth / PREFERRED_WIDTH;
            float heightScale = (float) availableHeight / PREFERRED_HEIGHT;
            float scale = Math.min(widthScale, heightScale);

            windowWidth = (int) (PREFERRED_WIDTH * scale);
            windowHeight = (int) (PREFERRED_HEIGHT * scale);
        }

        windowWidth = Math.max(windowWidth, MIN_WIDTH);
        windowHeight = Math.max(windowHeight, MIN_HEIGHT);
        windowWidth = Math.min(windowWidth, MAX_WIDTH);
        windowHeight = Math.min(windowHeight, MAX_HEIGHT);

        if (windowWidth > availableWidth) {
            windowWidth = Math.min(windowWidth, width - 20);
        }
        if (windowHeight > availableHeight) {
            windowHeight = Math.min(windowHeight, height - 20);
        }

        windowWidth = alignToPixel(windowWidth);
        windowHeight = alignToPixel(windowHeight);

        double centerX = (width - windowWidth) / 2.0;
        double centerY = (height - windowHeight) / 2.0;

        windowX = alignToPixel(centerX);
        windowY = alignToPixel(centerY);
    }

    private void calculateLayoutConstantsPixelPerfect() {
        int rawMargin = Mth.clamp(
            (int) (windowWidth * MARGIN_PERCENTAGE),
            MIN_MARGIN,
            MAX_MARGIN
        );
        margin = alignToPixel(rawMargin);

        int rawSectionSpacing = Math.max(15, windowHeight / 25);
        sectionSpacing = alignToPixel(rawSectionSpacing);

        int rawLineHeight = Math.max(12, windowHeight / 35);
        lineHeight = alignToPixel(rawLineHeight);

        int rawProgressBarWidth = Math.min(200, windowWidth / 3);
        progressBarWidth = alignToPixel(rawProgressBarWidth);

        int rawColumnWidth = (windowWidth - 3 * margin) / 2;
        columnWidth = alignToPixel(rawColumnWidth);
    }

    /**
     * UPDATED: Load cultivation data using unified attachment system
     */
    private void loadCultivationData() {
        if (minecraft.player != null) {
            cultivationAttachment = minecraft.player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
            resourceAttachment = minecraft.player.getData(ICModAttachmentTypes.PLAYER_RESOURCES.get());
            talentAttachment = minecraft.player.getData(ICModAttachmentTypes.PLAYER_TALENTS.get());
            combatStatsAttachment = minecraft.player.getData(ICModAttachmentTypes.PLAYER_COMBAT_STATS.get());

            // Set player reference for the attachments if needed
            if (cultivationAttachment != null) {
                cultivationAttachment.setPlayer(minecraft.player);
            }
            if (resourceAttachment != null) {
                resourceAttachment.setPlayer(minecraft.player);
            }
            if (talentAttachment != null) {
                talentAttachment.setPlayer(minecraft.player);
            }
        }
    }

    private void setupStatsInterfacePixelPerfect() {
        int rawButtonHeight = Math.max(20, lineHeight + 6);
        int buttonHeight = alignToPixel(rawButtonHeight);

        int smallButtonWidth = alignToPixel(Math.max(60, windowWidth / 8));
        int mediumButtonWidth = alignToPixel(Math.max(120, windowWidth / 4));
        int largeButtonWidth = alignToPixel(Math.max(180, windowWidth / 3));

        int closeButtonX = alignToPixel(windowX + windowWidth - smallButtonWidth - margin/2);
        int closeButtonY = alignToPixel(windowY + margin/2);

        addRenderableWidget(Button.builder(Component.literal("Close"),
                button -> onClose())
            .bounds(closeButtonX, closeButtonY, smallButtonWidth, buttonHeight)
            .build());

        if (cultivationAttachment != null) {
            createFocusToggleButtonPixelPerfect(buttonHeight, mediumButtonWidth);
        }

        int bottomY = alignToPixel(windowY + windowHeight - buttonHeight - margin);

        int breakthroughButtonX = alignToPixel(windowX + margin);
        addRenderableWidget(Button.builder(Component.literal("Breakthrough Analysis"),
                button -> openBreakthroughScreen())
            .bounds(breakthroughButtonX, bottomY, largeButtonWidth, buttonHeight)
            .build());

        int refreshButtonX = alignToPixel(windowX + windowWidth - smallButtonWidth - margin);
        addRenderableWidget(Button.builder(Component.literal("Refresh"),
                button -> refreshStats())
            .bounds(refreshButtonX, bottomY, smallButtonWidth, buttonHeight)
            .build());
    }

    /**
     * UPDATED: Create focus toggle button using unified cultivation attachment
     */
    private void createFocusToggleButtonPixelPerfect(int buttonHeight, int buttonWidth) {
        int bottomY = alignToPixel(windowY + windowHeight - buttonHeight - margin);
        int buttonX = alignToPixel(windowX + margin + 200);

        if (buttonX + buttonWidth > windowX + windowWidth - margin) {
            buttonX = alignToPixel(windowX + windowWidth - buttonWidth - margin);
        }

        // Use simplified focus toggle - just show current focus and allow toggle
        PlayerCultivationAttachment.CultivationType currentFocus = cultivationAttachment.getCurrentFocus();
        String switchTo = currentFocus == PlayerCultivationAttachment.CultivationType.QI ? "Body" : "Qi";

        addRenderableWidget(Button.builder(
                Component.literal("Switch to " + switchTo + " Focus"),
                button -> switchCultivationFocus())
            .bounds(buttonX, bottomY, buttonWidth, buttonHeight)
            .build());
    }

    private void renderWindowFramePixelPerfect(GuiGraphics guiGraphics) {
        guiGraphics.fill(0, 0, width, height, 0x80000000);
        guiGraphics.fill(windowX, windowY, windowX + windowWidth, windowY + windowHeight, 0xFF2C2C2C);

        int borderColor = 0xFFFFFFFF;
        guiGraphics.fill(windowX - 1, windowY - 1, windowX + windowWidth + 1, windowY, borderColor);
        guiGraphics.fill(windowX - 1, windowY + windowHeight, windowX + windowWidth + 1, windowY + windowHeight + 1, borderColor);
        guiGraphics.fill(windowX - 1, windowY, windowX, windowY + windowHeight, borderColor);
        guiGraphics.fill(windowX + windowWidth, windowY, windowX + windowWidth + 1, windowY + windowHeight, borderColor);

        String focusIndicator = cultivationAttachment != null ?
            " [" + cultivationAttachment.getCurrentFocus().getDisplayName() + " Focused]" : "";
        Component title = Component.literal("Cultivation Overview" + focusIndicator)
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);

        int titleWidth = font.width(title);
        int titleX = windowX + (windowWidth - titleWidth) / 2;
        int titleY = windowY + margin;
        drawStringAligned(guiGraphics, title, titleX, titleY, 0xFFFFFFFF);
    }

    /**
     * UPDATED: Render cultivation overview using unified attachments
     */
    private void renderCultivationOverview(GuiGraphics guiGraphics) {
        if (cultivationAttachment == null) {
            renderErrorState(guiGraphics);
            return;
        }

        int currentY = alignToPixel(windowY + margin * 2 + lineHeight);

        // Player traits section
        currentY = renderPlayerTraits(guiGraphics, currentY);
        currentY = alignToPixel(currentY + sectionSpacing);

        // Strategic overview
        currentY = renderStrategicOverview(guiGraphics, currentY);
        currentY = alignToPixel(currentY + sectionSpacing);

        // Cultivation progress
        currentY = renderCultivationProgress(guiGraphics, currentY);
        currentY = alignToPixel(currentY + sectionSpacing);

        // Resource status
        currentY = renderResourceStatus(guiGraphics, currentY);
    }

    /**
     * UPDATED: Render player traits using unified talent attachment
     */
    private int renderPlayerTraits(GuiGraphics guiGraphics, int startY) {
        int currentY = alignToPixel(startY);

        renderSectionHeaderPixelPerfect(guiGraphics, "Innate Cultivation Traits", currentY);
        currentY = alignToPixel(currentY + lineHeight + 5);

        // Display spiritual roots
        if (talentAttachment != null && talentAttachment.hasSpiritualRootAssigned()) {
            Component rootsLabel = Component.literal("Spiritual Roots: ")
                .withStyle(ChatFormatting.WHITE);
            Component rootsValue = Component.literal(talentAttachment.getSpiritualRootDisplayName())
                .withStyle(ChatFormatting.BOLD);

            drawStringAligned(guiGraphics, rootsLabel, windowX + margin, currentY, 0xFFFFFFFF);
            int labelWidth = font.width(rootsLabel);
            drawStringAligned(guiGraphics, rootsValue, windowX + margin + labelWidth, currentY,
                talentAttachment.getSpiritualRootColor());

            // Show multiplier
            String multiplierText = String.format(" (×%.1f Qi rates)",
                talentAttachment.getSpiritualRootMultiplier());
            drawStringAligned(guiGraphics, multiplierText,
                windowX + margin + labelWidth + font.width(rootsValue), currentY, QI_COLOR);

            currentY = alignToPixel(currentY + lineHeight);
        }

        // Display physique
        if (talentAttachment != null && talentAttachment.hasPhysiqueAssigned()) {
            Component physiqueLabel = Component.literal("Physique: ")
                .withStyle(ChatFormatting.WHITE);
            Component physiqueValue = Component.literal(talentAttachment.getPhysiqueDisplayName())
                .withStyle(ChatFormatting.BOLD);

            drawStringAligned(guiGraphics, physiqueLabel, windowX + margin, currentY, 0xFFFFFFFF);
            int labelWidth = font.width(physiqueLabel);
            drawStringAligned(guiGraphics, physiqueValue, windowX + margin + labelWidth, currentY,
                talentAttachment.getPhysiqueColor());

            // Show multiplier
            String multiplierText = String.format(" (×%.1f Body rates)",
                talentAttachment.getPhysiqueMultiplier());
            drawStringAligned(guiGraphics, multiplierText,
                windowX + margin + labelWidth + font.width(physiqueValue), currentY, BODY_COLOR);

            currentY = alignToPixel(currentY + lineHeight);
        }

        // Combined talent status
        if (talentAttachment != null && talentAttachment.hasSpiritualRootAssigned() && talentAttachment.hasPhysiqueAssigned()) {
            String combinedDesc = talentAttachment.getCombinedTalentDescription();
            float overallMultiplier = talentAttachment.getOverallCultivationMultiplier();

            String combinedText = String.format("%s (Overall: %.2fx)", combinedDesc, overallMultiplier);
            drawStringAligned(guiGraphics, combinedText, windowX + margin, currentY, 0xFFDDA0DD);
            currentY = alignToPixel(currentY + lineHeight);

            if (talentAttachment.hasDivineDaoDestiny()) {
                drawStringAligned(guiGraphics, "🌟 DIVINE DAO DESTINY 🌟", windowX + margin, currentY, 0xFFFFD700);
                currentY = alignToPixel(currentY + lineHeight);
            }
        }

        return currentY;
    }

    /**
     * UPDATED: Render strategic overview using unified cultivation attachment
     */
    private int renderStrategicOverview(GuiGraphics guiGraphics, int startY) {
        int currentY = alignToPixel(startY);

        renderSectionHeaderPixelPerfect(guiGraphics, "Strategic Overview", currentY);
        currentY = alignToPixel(currentY + lineHeight + 5);

        if (cultivationAttachment != null) {
            String focusText = "Cultivation Focus: " + cultivationAttachment.getCurrentFocus().getDisplayName() +
                " (2× advancement rate)";
            int focusColor = cultivationAttachment.getCurrentFocus() == PlayerCultivationAttachment.CultivationType.QI ? QI_COLOR : BODY_COLOR;
            drawStringAligned(guiGraphics, focusText, windowX + margin, currentY, focusColor);
            currentY = alignToPixel(currentY + lineHeight);

            // Show effective rates
            double qiRate = cultivationAttachment.getEffectiveQiCultivationRate();
            double bodyRate = cultivationAttachment.getEffectiveBodyCultivationRate();
            String ratesText = String.format("Rates: Qi %.2f/s | Body %.2f/s", qiRate, bodyRate);
            drawStringAligned(guiGraphics, ratesText, windowX + margin, currentY, 0xFFCCCCCC);
            currentY = alignToPixel(currentY + lineHeight);
        }

        CultivationRealm qiRealm = cultivationAttachment.getCurrentQiRealm();
        CultivationRealm bodyRealm = cultivationAttachment.getCurrentBodyRealm();
        String overallLevel = String.format("Realms: Qi %s | Body %s", qiRealm.getDisplayName(), bodyRealm.getDisplayName());
        drawStringAligned(guiGraphics, overallLevel, windowX + margin, currentY, 0xFFFFFFFF);
        currentY = alignToPixel(currentY + lineHeight);

        return currentY;
    }

    /**
     * UPDATED: Render cultivation progress using unified cultivation attachment
     */
    private int renderCultivationProgress(GuiGraphics guiGraphics, int startY) {
        int currentY = alignToPixel(startY);

        renderSectionHeaderPixelPerfect(guiGraphics, "Cultivation Progress", currentY);
        currentY = alignToPixel(currentY + lineHeight + 8);

        int leftColumnX = alignToPixel(windowX + margin);
        int rightColumnX = alignToPixel(windowX + margin + columnWidth);

        renderCultivationColumnPixelPerfect(guiGraphics, leftColumnX, currentY, PlayerCultivationAttachment.CultivationType.QI);
        renderCultivationColumnPixelPerfect(guiGraphics, rightColumnX, currentY, PlayerCultivationAttachment.CultivationType.BODY);

        return alignToPixel(currentY + Math.max(140, windowHeight / 4));
    }

    /**
     * UPDATED: Render cultivation column using unified cultivation attachment
     */
    private void renderCultivationColumnPixelPerfect(GuiGraphics guiGraphics, int x, int y, PlayerCultivationAttachment.CultivationType type) {
        int alignedX = alignToPixel(x);
        int currentY = alignToPixel(y);

        int typeColor = type == PlayerCultivationAttachment.CultivationType.QI ? QI_COLOR : BODY_COLOR;
        String typeName = type.getDisplayName();

        drawStringAligned(guiGraphics, typeName + " Cultivation", alignedX, currentY, typeColor);
        currentY = alignToPixel(currentY + lineHeight + 3);

        CultivationRealm currentRealm = type == PlayerCultivationAttachment.CultivationType.QI ?
            cultivationAttachment.getCurrentQiRealm() : cultivationAttachment.getCurrentBodyRealm();
        drawStringAligned(guiGraphics, "Realm: " + currentRealm.getDisplayName(), alignedX, currentY, 0xFFFFFFFF);
        currentY = alignToPixel(currentY + lineHeight);

        long current, max;
        double baseRate, effectiveRate, meditationRate;
        double percentage;

        if (type == PlayerCultivationAttachment.CultivationType.QI) {
            current = cultivationAttachment.getCurrentQiCultivation();
            max = cultivationAttachment.getMaxQiCultivation();
            baseRate = cultivationAttachment.getBaseQiCultivationRate();
            effectiveRate = cultivationAttachment.getEffectiveQiCultivationRate();
            meditationRate = cultivationAttachment.getEffectiveCirculatingQiCultivationRate();
            percentage = cultivationAttachment.getQiProgress();
        } else {
            current = cultivationAttachment.getCurrentBodyCultivation();
            max = cultivationAttachment.getMaxBodyCultivation();
            baseRate = cultivationAttachment.getBaseBodyCultivationRate();
            effectiveRate = cultivationAttachment.getEffectiveBodyCultivationRate();
            meditationRate = cultivationAttachment.getEffectiveCirculatingBodyCultivationRate();
            percentage = cultivationAttachment.getBodyProgress();
        }

        String progressText = String.format("%.1f%% (%d/%d)", percentage * 100, current, max);
        drawStringAligned(guiGraphics, progressText, alignedX, currentY, 0xFFCCCCCC);
        currentY = alignToPixel(currentY + lineHeight + 2);

        int barWidth = Math.min(progressBarWidth, columnWidth - 20);
        int barX = alignToPixel(alignedX);
        int barY = alignToPixel(currentY);
        renderProgressBarPixelPerfect(guiGraphics, barX, barY, barWidth, PROGRESS_BAR_HEIGHT, percentage, typeColor);
        currentY = alignToPixel(currentY + PROGRESS_BAR_HEIGHT + 5);

        // Show base rate
        String baseRateText = String.format("Base: %.3f/s", baseRate);
        drawStringAligned(guiGraphics, baseRateText, alignedX, currentY, 0xFF999999);
        currentY = alignToPixel(currentY + lineHeight);

        // Show effective rate with indicator of multipliers applied
        boolean isFocused = cultivationAttachment.getCurrentFocus() == type;

        // Calculate total multiplier
        double totalMultiplier = effectiveRate / baseRate;
        String effectiveRateText;
        if (Math.abs(totalMultiplier - 1.0) > 0.01) {
            effectiveRateText = String.format("Effective: %.3f/s (×%.2f)", effectiveRate, totalMultiplier);
        } else {
            effectiveRateText = String.format("Effective: %.3f/s", effectiveRate);
        }

        if (isFocused) effectiveRateText += " [FOCUSED]";

        int rateColor = isFocused ? FOCUS_ACTIVE_COLOR : 0xFFFFFFFF;
        drawStringAligned(guiGraphics, effectiveRateText, alignedX, currentY, rateColor);
        currentY = alignToPixel(currentY + lineHeight);

        // Show meditation rate
        double meditationMultiplier = meditationRate / baseRate;
        String meditationText = String.format("Meditation: %.3f/s (×%.1f)", meditationRate, meditationMultiplier);
        drawStringAligned(guiGraphics, meditationText, alignedX, currentY, 0xFFCCCCCC);
        currentY = alignToPixel(currentY + lineHeight);

        // Show bonuses if any
        double additiveBonus = type == PlayerCultivationAttachment.CultivationType.QI ?
            cultivationAttachment.getQiAdditiveBonus() : cultivationAttachment.getBodyAdditiveBonus();

        if (additiveBonus > 0) {
            String bonusText = String.format("Pills: +%.3f/s", additiveBonus);
            drawStringAligned(guiGraphics, bonusText, alignedX, currentY, 0xFFDDA0DD);
            currentY = alignToPixel(currentY + lineHeight);
        }

        // Breakthrough readiness
        boolean ready = current >= max;
        String readinessText = ready ? "✓ Ready" : "Not Ready";
        int readinessColor = ready ? READY_COLOR : NOT_READY_COLOR;
        drawStringAligned(guiGraphics, "Breakthrough: " + readinessText, alignedX, currentY, readinessColor);
    }

    /**
     * UPDATED: Render resource status using unified resource attachment
     */
    private int renderResourceStatus(GuiGraphics guiGraphics, int startY) {
        int currentY = alignToPixel(startY);

        renderSectionHeaderPixelPerfect(guiGraphics, "Current Resources", currentY);
        currentY = alignToPixel(currentY + lineHeight + 8);

        if (resourceAttachment != null) {
            currentY = renderResourceLinePixelPerfect(guiGraphics, currentY, "Health",
                resourceAttachment.getCurrentHealth(), resourceAttachment.getMaxHealth(),
                resourceAttachment.getEffectiveHealthRegenerationRate(), HEALTH_COLOR);

            currentY = renderResourceLinePixelPerfect(guiGraphics, currentY, "Qi Energy",
                resourceAttachment.getCurrentQi(), resourceAttachment.getMaxQi(),
                resourceAttachment.getEffectiveQiRegenerationRate(), QI_COLOR);

            currentY = renderResourceLinePixelPerfect(guiGraphics, currentY, "Body Energy",
                resourceAttachment.getCurrentBody(), resourceAttachment.getMaxBody(),
                resourceAttachment.getEffectiveBodyRegenerationRate(), BODY_COLOR);

            // Show meditation status
            boolean meditating = resourceAttachment.isPlayerMeditating();
            String meditationStatus = "Meditation: " + (meditating ? "Active (10x rates)" : "Inactive");
            int meditationColor = meditating ? 0xFF90EE90 : 0xFFCCCCCC;
            drawStringAligned(guiGraphics, meditationStatus, windowX + margin, currentY, meditationColor);
            currentY = alignToPixel(currentY + lineHeight + 3);
        }

        return currentY;
    }

    private int renderResourceLinePixelPerfect(GuiGraphics guiGraphics, int y, String resourceName,
                                               long current, long max, double regenRate, int color) {
        int alignedY = alignToPixel(y);

        String resourceText = String.format("%s: %d/%d", resourceName, current, max);
        drawStringAligned(guiGraphics, resourceText, windowX + margin, alignedY, color);

        String regenText = String.format("(+%.2f/s)", regenRate);
        int textWidth = font.width(resourceText);
        int regenX = alignToPixel(windowX + margin + textWidth + 10);
        drawStringAligned(guiGraphics, regenText, regenX, alignedY, 0xFFCCCCCC);

        double percentage = max > 0 ? (double) current / max : 0;
        int barX = alignToPixel(windowX + margin + Math.max(250, windowWidth / 2));
        int barY = alignToPixel(alignedY + 2);

        renderProgressBarPixelPerfect(guiGraphics, barX, barY, progressBarWidth, PROGRESS_BAR_HEIGHT, percentage, color);

        return alignToPixel(alignedY + lineHeight + 3);
    }

    private void renderProgressBarPixelPerfect(GuiGraphics guiGraphics, int x, int y, int width, int height,
                                               double percentage, int fillColor) {
        int alignedX = alignToPixel(x);
        int alignedY = alignToPixel(y);
        int alignedWidth = alignToPixel(width);
        int alignedHeight = alignToPixel(height);

        guiGraphics.fill(alignedX, alignedY,
            alignedX + alignedWidth, alignedY + alignedHeight,
            0xFF404040);

        int fillWidth = alignToPixel(alignedWidth * Math.min(1.0, Math.max(0.0, percentage)));
        if (fillWidth > 0) {
            guiGraphics.fill(alignedX, alignedY,
                alignedX + fillWidth, alignedY + alignedHeight,
                fillColor);
        }

        int borderColor = 0xFF808080;
        guiGraphics.fill(alignedX - 1, alignedY - 1, alignedX + alignedWidth + 1, alignedY, borderColor);
        guiGraphics.fill(alignedX - 1, alignedY + alignedHeight, alignedX + alignedWidth + 1, alignedY + alignedHeight + 1, borderColor);
        guiGraphics.fill(alignedX - 1, alignedY, alignedX, alignedY + alignedHeight, borderColor);
        guiGraphics.fill(alignedX + alignedWidth, alignedY, alignedX + alignedWidth + 1, alignedY + alignedHeight, borderColor);
    }

    private void renderSectionHeaderPixelPerfect(GuiGraphics guiGraphics, String headerText, int y) {
        int alignedY = alignToPixel(y);
        Component header = Component.literal(headerText).withStyle(ChatFormatting.UNDERLINE, ChatFormatting.BOLD);
        drawStringAligned(guiGraphics, header, windowX + margin, alignedY, SECTION_HEADER_COLOR);
    }

    private void renderErrorState(GuiGraphics guiGraphics) {
        String errorText = "Cultivation data not available";
        int errorY = alignToPixel(windowY + 100);
        drawStringAligned(guiGraphics, errorText, windowX + margin, errorY, 0xFFFF6666);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // === ACTION METHODS ===

    /**
     * UPDATED: Switch cultivation focus using unified cultivation attachment
     */
    private void switchCultivationFocus() {
        if (cultivationAttachment != null) {
            ImmortalCultivation.LOGGER.info("Client requesting focus toggle from {} to {} cultivation",
                cultivationAttachment.getCurrentFocus().getDisplayName(),
                cultivationAttachment.getCurrentFocus() == PlayerCultivationAttachment.CultivationType.QI ? "Body" : "Qi");

            // Send packet to server to toggle focus
            ICModNetwork.sendToServer(new CultivationFocusTogglePacket());
        } else {
            ImmortalCultivation.LOGGER.info("Cannot switch focus - cultivation attachment missing");
        }
    }

    private void openBreakthroughScreen() {
        minecraft.setScreen(new CultivationBreakthroughScreen());
    }

    /**
     * UPDATED: Refresh stats using unified attachments
     */
    private void refreshStats() {
        if (cultivationAttachment != null) cultivationAttachment.forceSync();
        if (resourceAttachment != null) resourceAttachment.forceSync();
        if (talentAttachment != null) talentAttachment.forceSync();
        if (combatStatsAttachment != null) combatStatsAttachment.forceSync();

        init();
    }
}
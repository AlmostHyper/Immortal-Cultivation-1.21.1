package net.hyper.immortalcultivation.ui.screens;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.attachment.*;
import net.hyper.immortalcultivation.cultivation.realms.CultivationRealm;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
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
 * pixel-perfect positioning, and sharp texture rendering.
 *
 * This version represents the culmination of our systematic approach to solving GUI blurriness:
 * 1. Responsive design ensures proper scaling across different screen sizes
 * 2. Pixel alignment eliminates coordinate-based blurriness
 * 3. Sharp texture filtering eliminates scaling-based blurriness
 * 4. Debug tools helped us identify the root cause systematically
 *
 * Key Technical Insights Demonstrated:
 * - How to diagnose rendering issues methodically
 * - The difference between coordinate precision and texture filtering
 * - Why GUI scale can cause unexpected visual problems
 * - How to control OpenGL texture parameters for crisp GUI rendering
 * - The importance of restoring graphics state after modifications
 */
public class CultivationStatsScreen extends Screen {

    // === RESPONSIVE SIZING SYSTEM ===
    // [All your existing constants remain unchanged - these were working perfectly]

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
    // [All your existing layout variables remain unchanged]

    private int windowWidth;
    private int windowHeight;
    private int windowX, windowY;
    private int margin;
    private int sectionSpacing;
    private int lineHeight;
    private int progressBarWidth;
    private int columnWidth;

    // === VISUAL CONSTANTS ===
    // [All your existing color constants remain unchanged]

    private static final int QI_COLOR = 0xFF87CEEB;
    private static final int BODY_COLOR = 0xFFFF6347;
    private static final int HEALTH_COLOR = 0xFFFF69B4;
    private static final int FOCUS_ACTIVE_COLOR = 0xFFFFD700;
    private static final int READY_COLOR = 0xFF90EE90;
    private static final int NOT_READY_COLOR = 0xFFFFB6C1;
    private static final int SECTION_HEADER_COLOR = 0xFFDDA0DD;
    private static final int PROGRESS_BAR_HEIGHT = 8;

    // === PLAYER DATA ===
    // [All your existing attachment variables remain unchanged]

    private PlayerRealmAttachment realmAttachment;
    private PlayerQiCultivationAttachment qiCultivationAttachment;
    private PlayerBodyCultivationAttachment bodyCultivationAttachment;
    private PlayerCultivationFocusAttachment focusAttachment;
    private PlayerHealthAttachment healthAttachment;
    private PlayerQiAttachment qiAttachment;
    private PlayerBodyAttachment bodyAttachment;
    private PlayerSpiritualRootAttachment spiritualRoots;
    private PlayerPhysiqueAttachment physique;

    public CultivationStatsScreen() {
        super(Component.literal("Cultivation Status Overview"));
    }

    // === PIXEL-PERFECT RENDERING HELPERS ===
    // [All your existing helper methods remain unchanged - they're still essential]

    /**
     * Gets the current GUI scale factor that Minecraft is using.
     * This method remains crucial for our pixel alignment calculations.
     */
    private double getGuiScale() {
        return minecraft.getWindow().getGuiScale();
    }

    /**
     * Rounds a coordinate to be pixel-perfect at the current GUI scale.
     * This method works in conjunction with our texture filtering to ensure
     * complete visual sharpness.
     */
    private int alignToPixel(double coordinate) {
        double scale = getGuiScale();
        return (int) Math.round(coordinate / scale) * (int) scale;
    }

    /**
     * Helper methods for drawing text with pixel-perfect positioning.
     * These now work together with our sharp rendering mode for maximum effect.
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
        // Ensure blending is properly configured for GUI rendering
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // This is the key fix: Configure OpenGL to use nearest-neighbor filtering
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
    }

    /**
     * ESSENTIAL CLEANUP: Restore normal texture rendering mode
     */
    private void restoreNormalRendering() {
        // Restore OpenGL's default texture filtering settings
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
    }

    @Override
    protected void init() {
        super.init();

        // [Your existing initialization logic remains unchanged]
        calculateResponsiveDimensionsPixelPerfect();
        calculateLayoutConstantsPixelPerfect();
        loadCultivationData();
        setupStatsInterfacePixelPerfect();
    }

    /**
     * UPDATED: Main render method with sharp rendering integration and blur fix
     */
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        // SOLUTION: Wrap all rendering in sharp mode
        enableSharpRendering();

        try {
            // All your existing rendering logic remains unchanged
            renderWindowFramePixelPerfect(guiGraphics);
            renderCultivationOverview(guiGraphics);
            super.render(guiGraphics, mouseX, mouseY, partialTicks);
        } finally {
            // CRITICAL: Always restore normal rendering, even if an exception occurs
            restoreNormalRendering();
        }
    }

    /**
     * FIX for blurry background - Override renderBackground to prevent blur shader
     */
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // This prevents the default blur shader from being applied
        // Just render a dark overlay instead
        guiGraphics.fill(0, 0, this.width, this.height, 0x80000000);
    }

    // === ALL YOUR EXISTING METHODS REMAIN UNCHANGED ===

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

    private void loadCultivationData() {
        if (minecraft.player != null) {
            realmAttachment = minecraft.player.getData(ICModAttachmentTypes.PLAYER_REALM.get());
            qiCultivationAttachment = minecraft.player.getData(ICModAttachmentTypes.PLAYER_QI_CULTIVATION.get());
            bodyCultivationAttachment = minecraft.player.getData(ICModAttachmentTypes.PLAYER_BODY_CULTIVATION.get());
            focusAttachment = minecraft.player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION_FOCUS.get());
            healthAttachment = minecraft.player.getData(ICModAttachmentTypes.PLAYER_HEALTH.get());
            qiAttachment = minecraft.player.getData(ICModAttachmentTypes.PLAYER_QI.get());
            bodyAttachment = minecraft.player.getData(ICModAttachmentTypes.PLAYER_BODY.get());
            spiritualRoots = minecraft.player.getData(ICModAttachmentTypes.PLAYER_SPIRITUAL_ROOT.get());
            physique = minecraft.player.getData(ICModAttachmentTypes.PLAYER_PHYSIQUE.get());

            if (realmAttachment != null) {
                realmAttachment.setPlayer(minecraft.player);
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

        if (focusAttachment != null) {
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

    private void createFocusToggleButtonPixelPerfect(int buttonHeight, int buttonWidth) {
        int bottomY = alignToPixel(windowY + windowHeight - buttonHeight - margin);
        int buttonX = alignToPixel(windowX + margin + 200);

        if (buttonX + buttonWidth > windowX + windowWidth - margin) {
            buttonX = alignToPixel(windowX + windowWidth - buttonWidth - margin);
        }

        if (focusAttachment.canSwitchFocus()) {
            String switchTo = focusAttachment.isFocusedOnQi() ? "Body" : "Qi";

            addRenderableWidget(Button.builder(
                    Component.literal("Switch to " + switchTo + " Focus"),
                    button -> switchCultivationFocus())
                .bounds(buttonX, bottomY, buttonWidth, buttonHeight)
                .build());
        } else {
            long remainingSeconds = focusAttachment.getRemainingCooldownTicks() / 20;
            addRenderableWidget(Button.builder(
                    Component.literal("Focus Cooldown: " + remainingSeconds + "s"),
                    button -> {})
                .bounds(buttonX, bottomY, buttonWidth, buttonHeight)
                .build());
        }
    }

    private void renderWindowFramePixelPerfect(GuiGraphics guiGraphics) {
        guiGraphics.fill(0, 0, width, height, 0x80000000);
        guiGraphics.fill(windowX, windowY, windowX + windowWidth, windowY + windowHeight, 0xFF2C2C2C);

        int borderColor = 0xFFFFFFFF;
        guiGraphics.fill(windowX - 1, windowY - 1, windowX + windowWidth + 1, windowY, borderColor);
        guiGraphics.fill(windowX - 1, windowY + windowHeight, windowX + windowWidth + 1, windowY + windowHeight + 1, borderColor);
        guiGraphics.fill(windowX - 1, windowY, windowX, windowY + windowHeight, borderColor);
        guiGraphics.fill(windowX + windowWidth, windowY, windowX + windowWidth + 1, windowY + windowHeight, borderColor);

        String focusIndicator = focusAttachment != null ?
            " [" + focusAttachment.getCurrentFocus().getDisplayName() + " Focused]" : "";
        Component title = Component.literal("Cultivation Overview" + focusIndicator)
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);

        int titleWidth = font.width(title);
        int titleX = windowX + (windowWidth - titleWidth) / 2;
        int titleY = windowY + margin;
        drawStringAligned(guiGraphics, title, titleX, titleY, 0xFFFFFFFF);
    }

    private void renderCultivationOverview(GuiGraphics guiGraphics) {
        if (realmAttachment == null) {
            renderErrorState(guiGraphics);
            return;
        }

        int currentY = alignToPixel(windowY + margin * 2 + lineHeight);

        // New section: Player traits (spiritual roots and physique)
        currentY = renderPlayerTraits(guiGraphics, currentY);
        currentY = alignToPixel(currentY + sectionSpacing);

        currentY = renderStrategicOverview(guiGraphics, currentY);
        currentY = alignToPixel(currentY + sectionSpacing);

        currentY = renderCultivationProgress(guiGraphics, currentY);
        currentY = alignToPixel(currentY + sectionSpacing);

        currentY = renderResourceStatus(guiGraphics, currentY);
    }

    /**
     * NEW: Render player traits section at the top
     */
    private int renderPlayerTraits(GuiGraphics guiGraphics, int startY) {
        int currentY = alignToPixel(startY);

        renderSectionHeaderPixelPerfect(guiGraphics, "Innate Cultivation Traits", currentY);
        currentY = alignToPixel(currentY + lineHeight + 5);

        // Display spiritual roots
        if (spiritualRoots != null) {
            Component rootsLabel = Component.literal("Spiritual Roots: ")
                .withStyle(ChatFormatting.WHITE);
            Component rootsValue = Component.literal(spiritualRoots.getRootType().getDisplayName())
                .withStyle(ChatFormatting.BOLD);

            drawStringAligned(guiGraphics, rootsLabel, windowX + margin, currentY, 0xFFFFFFFF);
            int labelWidth = font.width(rootsLabel);
            drawStringAligned(guiGraphics, rootsValue, windowX + margin + labelWidth, currentY,
                spiritualRoots.getRootType().getColor());

            // Show multiplier
            String multiplierText = String.format(" (×%.1f Qi rates)",
                spiritualRoots.getCultivationMultiplier());
            drawStringAligned(guiGraphics, multiplierText,
                windowX + margin + labelWidth + font.width(rootsValue), currentY, QI_COLOR);

            currentY = alignToPixel(currentY + lineHeight);
        }

        // Display physique
        if (physique != null) {
            Component physiqueLabel = Component.literal("Physique: ")
                .withStyle(ChatFormatting.WHITE);
            Component physiqueValue = Component.literal(physique.getRootType().getDisplayName())
                .withStyle(ChatFormatting.BOLD);

            drawStringAligned(guiGraphics, physiqueLabel, windowX + margin, currentY, 0xFFFFFFFF);
            int labelWidth = font.width(physiqueLabel);
            drawStringAligned(guiGraphics, physiqueValue, windowX + margin + labelWidth, currentY,
                physique.getRootType().getColor());

            // Show multiplier
            String multiplierText = String.format(" (×%.1f Body rates)",
                physique.getCultivationMultiplier());
            drawStringAligned(guiGraphics, multiplierText,
                windowX + margin + labelWidth + font.width(physiqueValue), currentY, BODY_COLOR);

            currentY = alignToPixel(currentY + lineHeight);
        }

        return currentY;
    }

    private int renderStrategicOverview(GuiGraphics guiGraphics, int startY) {
        int currentY = alignToPixel(startY);

        renderSectionHeaderPixelPerfect(guiGraphics, "Strategic Overview", currentY);
        currentY = alignToPixel(currentY + lineHeight + 5);

        if (focusAttachment != null) {
            String focusText = "Cultivation Focus: " + focusAttachment.getCurrentFocus().getDisplayName() +
                " (2× advancement rate)";
            int focusColor = focusAttachment.isFocusedOnQi() ? QI_COLOR : BODY_COLOR;
            drawStringAligned(guiGraphics, focusText, windowX + margin, currentY, focusColor);
            currentY = alignToPixel(currentY + lineHeight);

            long focusTime = focusAttachment.getTicksInCurrentFocus() / 20;
            String efficiencyText = "Time in Focus: " + focusTime + "s";
            drawStringAligned(guiGraphics, efficiencyText, windowX + margin, currentY, 0xFFCCCCCC);
            currentY = alignToPixel(currentY + lineHeight);
        }

        CultivationRealm highestRealm = realmAttachment.getHighestRealm();
        String overallLevel = "Highest Realm: " + highestRealm.getDisplayName();
        drawStringAligned(guiGraphics, overallLevel, windowX + margin, currentY, 0xFFFFFFFF);
        currentY = alignToPixel(currentY + lineHeight);

        return currentY;
    }

    private int renderCultivationProgress(GuiGraphics guiGraphics, int startY) {
        int currentY = alignToPixel(startY);

        renderSectionHeaderPixelPerfect(guiGraphics, "Cultivation Progress", currentY);
        currentY = alignToPixel(currentY + lineHeight + 8);

        int leftColumnX = alignToPixel(windowX + margin);
        int rightColumnX = alignToPixel(windowX + margin + columnWidth);

        renderCultivationColumnPixelPerfect(guiGraphics, leftColumnX, currentY, PlayerRealmAttachment.CultivationType.QI);
        renderCultivationColumnPixelPerfect(guiGraphics, rightColumnX, currentY, PlayerRealmAttachment.CultivationType.BODY);

        return alignToPixel(currentY + Math.max(140, windowHeight / 4));
    }

    /**
     * ENHANCED: Show effective rates with breakdown
     */
    private void renderCultivationColumnPixelPerfect(GuiGraphics guiGraphics, int x, int y, PlayerRealmAttachment.CultivationType type) {
        int alignedX = alignToPixel(x);
        int currentY = alignToPixel(y);

        int typeColor = type == PlayerRealmAttachment.CultivationType.QI ? QI_COLOR : BODY_COLOR;
        String typeName = type.getDisplayName();

        drawStringAligned(guiGraphics, typeName + " Cultivation", alignedX, currentY, typeColor);
        currentY = alignToPixel(currentY + lineHeight + 3);

        CultivationRealm currentRealm = realmAttachment.getCurrentRealm(type);
        drawStringAligned(guiGraphics, "Realm: " + currentRealm.getDisplayName(), alignedX, currentY, 0xFFFFFFFF);
        currentY = alignToPixel(currentY + lineHeight);

        long current, max;
        double baseRate, effectiveRate, meditationRate;

        if (type == PlayerRealmAttachment.CultivationType.QI && qiCultivationAttachment != null) {
            current = qiCultivationAttachment.getCurrentQiCultivation();
            max = qiCultivationAttachment.getMaxQiCultivation();
            baseRate = qiCultivationAttachment.getBaseQiCultivationRate();
            effectiveRate = qiCultivationAttachment.getEffectiveQiCultivationRate();
            meditationRate = qiCultivationAttachment.getEffectiveCirculatingQiCultivationRate();
        } else if (type == PlayerRealmAttachment.CultivationType.BODY && bodyCultivationAttachment != null) {
            current = bodyCultivationAttachment.getCurrentBodyCultivation();
            max = bodyCultivationAttachment.getMaxBodyCultivation();
            baseRate = bodyCultivationAttachment.getBaseBodyCultivationRate();
            effectiveRate = bodyCultivationAttachment.getEffectiveBodyCultivationRate();
            meditationRate = bodyCultivationAttachment.getEffectiveCirculatingBodyCultivationRate();
        } else {
            return;
        }

        double percentage = max > 0 ? (double) current / max : 0;
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
        boolean isFocused = focusAttachment != null && focusAttachment.getCurrentFocus() == type;

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

        long threshold = currentRealm.getBreakthroughCultivationThreshold();
        boolean ready = current >= threshold && !realmAttachment.isOnBreakthroughCooldown(type);

        String readinessText = ready ? "✓ Ready" : "Not Ready";
        int readinessColor = ready ? READY_COLOR : NOT_READY_COLOR;
        drawStringAligned(guiGraphics, "Breakthrough: " + readinessText, alignedX, currentY, readinessColor);
    }

    private int renderResourceStatus(GuiGraphics guiGraphics, int startY) {
        int currentY = alignToPixel(startY);

        renderSectionHeaderPixelPerfect(guiGraphics, "Current Resources", currentY);
        currentY = alignToPixel(currentY + lineHeight + 8);

        if (healthAttachment != null) {
            currentY = renderResourceLinePixelPerfect(guiGraphics, currentY, "Health",
                healthAttachment.getCurrentHealth(), healthAttachment.getMaxHealth(),
                healthAttachment.getEffectiveHealthRegenerationRate(), HEALTH_COLOR);
        }

        if (qiAttachment != null) {
            currentY = renderResourceLinePixelPerfect(guiGraphics, currentY, "Qi Energy",
                qiAttachment.getCurrentQi(), qiAttachment.getMaxQi(),
                qiAttachment.getEffectiveQiRegenerationRate(), QI_COLOR);
        }

        if (bodyAttachment != null) {
            currentY = renderResourceLinePixelPerfect(guiGraphics, currentY, "Body Energy",
                bodyAttachment.getCurrentBody(), bodyAttachment.getMaxBody(),
                bodyAttachment.getEffectiveBodyRegenerationRate(), BODY_COLOR);
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

    // === ACTION METHODS (unchanged) ===

    private void switchCultivationFocus() {
        if (focusAttachment != null && focusAttachment.canSwitchFocus()) {
            focusAttachment.toggleFocus();
            init();
        }
    }

    private void openBreakthroughScreen() {
        minecraft.setScreen(new CultivationBreakthroughScreen());
    }

    private void refreshStats() {
        if (qiCultivationAttachment != null) qiCultivationAttachment.forceSync();
        if (bodyCultivationAttachment != null) bodyCultivationAttachment.forceSync();
        if (healthAttachment != null) healthAttachment.forceSync();
        if (qiAttachment != null) qiAttachment.forceSync();
        if (bodyAttachment != null) bodyAttachment.forceSync();
        if (focusAttachment != null) focusAttachment.forceSync();

        init();
    }
}
package net.hyper.immortalcultivation.ui.screens;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.attachment.PlayerCultivationAttachment;
import net.hyper.immortalcultivation.attachment.PlayerResourceAttachment;
import net.hyper.immortalcultivation.attachment.PlayerTalentAttachment;
import net.hyper.immortalcultivation.cultivation.realms.CultivationRealm;
import net.hyper.immortalcultivation.network.client.BreakthroughRequestPacket;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.hyper.immortalcultivation.registry.ICModNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Specialized breakthrough interface focused exclusively on breakthrough planning and execution.
 * Updated to work with the unified cultivation attachment system.
 *
 * This screen is designed for the specific task of analyzing and attempting cultivation breakthroughs.
 * It provides detailed information about breakthrough options, success rates, and strategic implications
 * without the distraction of general cultivation monitoring information.
 */
public class CultivationBreakthroughScreen extends Screen {
    // Layout constants optimized for breakthrough-specific information
    private static final int WINDOW_WIDTH = 420;
    private static final int WINDOW_HEIGHT = 480;

    // Visual organization for breakthrough-focused display
    private static final int SECTION_SPACING = 18;
    private static final int LINE_HEIGHT = 14;
    private static final int OPTION_BUTTON_HEIGHT = 65;

    // Color scheme specifically for breakthrough analysis
    private static final int QI_COLOR = 0xFF87CEEB;
    private static final int BODY_COLOR = 0xFFFF6347;
    private static final int SUCCESS_HIGH = 0xFF00FF00;    // Green for >70% success
    private static final int SUCCESS_MED = 0xFFFFFF00;     // Yellow for 40-70% success
    private static final int SUCCESS_LOW = 0xFFFF6666;     // Red for <40% success
    private static final int READY_COLOR = 0xFF90EE90;     // Light green for ready
    private static final int NOT_READY_COLOR = 0xFFFFB6C1; // Light red for not ready

    // Player cultivation data needed for breakthrough analysis (unified attachments)
    private PlayerCultivationAttachment cultivationAttachment;
    private PlayerResourceAttachment resourceAttachment;
    private PlayerTalentAttachment talentAttachment;

    private int windowX, windowY;
    private List<BreakthroughOptionButton> breakthroughButtons = new ArrayList<>();

    public CultivationBreakthroughScreen() {
        super(Component.literal("Cultivation Breakthrough Analysis"));
    }

    @Override
    protected void init() {
        super.init();

        // Center the window for optimal breakthrough analysis visibility
        windowX = (width - WINDOW_WIDTH) / 2;
        windowY = (height - WINDOW_HEIGHT) / 2;

        // Gather cultivation data from unified attachments
        if (minecraft.player != null) {
            cultivationAttachment = minecraft.player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
            resourceAttachment = minecraft.player.getData(ICModAttachmentTypes.PLAYER_RESOURCES.get());
            talentAttachment = minecraft.player.getData(ICModAttachmentTypes.PLAYER_TALENTS.get());

            // Ensure player reference is set for breakthrough calculations
            if (cultivationAttachment != null) {
                cultivationAttachment.setPlayer(minecraft.player);
            }
        }

        setupBreakthroughInterface();
    }

    /**
     * Creates the breakthrough-focused interface elements.
     * This interface is specifically designed for breakthrough analysis and execution,
     * providing all information needed for strategic breakthrough decisions.
     */
    private void setupBreakthroughInterface() {
        breakthroughButtons.clear();

        if (cultivationAttachment == null) {
            createErrorInterface();
            return;
        }

        // Close button
        addRenderableWidget(Button.builder(Component.literal("Close"),
                button -> onClose())
            .bounds(windowX + WINDOW_WIDTH - 70, windowY + 10, 60, 20)
            .build());

        // Create breakthrough option buttons organized by cultivation type
        createBreakthroughOptions();
    }

    /**
     * Creates breakthrough option buttons for both cultivation paths.
     * Updated to work with the unified cultivation system.
     */
    private void createBreakthroughOptions() {
        // Calculate starting position for breakthrough options (after analysis display)
        int buttonStartY = windowY + 160;
        int currentY = buttonStartY;

        // Check if Qi breakthrough is available
        boolean qiReady = canAttemptBreakthrough(PlayerCultivationAttachment.CultivationType.QI);
        if (qiReady || isNearBreakthrough(PlayerCultivationAttachment.CultivationType.QI)) {
            currentY = createBreakthroughSection(PlayerCultivationAttachment.CultivationType.QI, currentY);
            currentY += SECTION_SPACING;
        }

        // Check if Body breakthrough is available
        boolean bodyReady = canAttemptBreakthrough(PlayerCultivationAttachment.CultivationType.BODY);
        if (bodyReady || isNearBreakthrough(PlayerCultivationAttachment.CultivationType.BODY)) {
            currentY = createBreakthroughSection(PlayerCultivationAttachment.CultivationType.BODY, currentY);
        }

        // Show helpful message if no breakthrough options are available
        if (!qiReady && !bodyReady && !isNearBreakthrough(PlayerCultivationAttachment.CultivationType.QI) && !isNearBreakthrough(PlayerCultivationAttachment.CultivationType.BODY)) {
            createNoOptionsMessage(buttonStartY);
        }
    }

    /**
     * Creates a section for a specific cultivation type breakthrough.
     */
    private int createBreakthroughSection(PlayerCultivationAttachment.CultivationType cultivationType, int startY) {
        int currentY = startY;

        // Reserve space for section header (rendered in render method)
        currentY += 25;

        // Create breakthrough button
        boolean canAttempt = canAttemptBreakthrough(cultivationType);
        CultivationRealm currentRealm = cultivationType == PlayerCultivationAttachment.CultivationType.QI ?
            cultivationAttachment.getCurrentQiRealm() : cultivationAttachment.getCurrentBodyRealm();

        // Get next realm options (simplified - just get the next logical realm)
        List<CultivationRealm> nextRealms = currentRealm.getNextCultivationRealms();
        if (!nextRealms.isEmpty()) {
            CultivationRealm targetRealm = nextRealms.get(0);

            BreakthroughOptionButton button = new BreakthroughOptionButton(
                windowX + 15, currentY,
                WINDOW_WIDTH - 30, OPTION_BUTTON_HEIGHT,
                cultivationType, targetRealm, canAttempt
            );
            breakthroughButtons.add(button);
            addRenderableWidget(button);
            currentY += OPTION_BUTTON_HEIGHT + 8;
        }

        return currentY;
    }

    /**
     * Creates a helpful message when no breakthrough options are available.
     */
    private void createNoOptionsMessage(int startY) {
        String message = determineNoOptionsReason();

        addRenderableWidget(Button.builder(Component.literal(message), button -> {})
            .bounds(windowX + 20, startY, WINDOW_WIDTH - 40, 40)
            .build());
    }

    /**
     * Determines why no breakthrough options are available and provides specific guidance.
     */
    private String determineNoOptionsReason() {
        if (cultivationAttachment == null) return "Cultivation data unavailable";

        boolean qiCanAttempt = canAttemptBreakthrough(PlayerCultivationAttachment.CultivationType.QI);
        boolean bodyCanAttempt = canAttemptBreakthrough(PlayerCultivationAttachment.CultivationType.BODY);

        if (!qiCanAttempt && !bodyCanAttempt) {
            return "Continue cultivating to meet breakthrough requirements";
        }

        return "No breakthrough paths available from current realms";
    }

    /**
     * Creates error interface when cultivation data is missing.
     */
    private void createErrorInterface() {
        addRenderableWidget(Button.builder(Component.literal("Cultivation data not available"), button -> {})
            .bounds(windowX + 20, windowY + 100, WINDOW_WIDTH - 40, 30)
            .build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        // Render window frame and background
        renderWindowFrame(guiGraphics);

        // Render breakthrough analysis information
        renderBreakthroughAnalysis(guiGraphics);

        // Render section headers for breakthrough options
        renderSectionHeaders(guiGraphics);

        // Render all GUI components
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    /**
     * Renders the window frame and background for the breakthrough interface.
     */
    private void renderWindowFrame(GuiGraphics guiGraphics) {
        // Semi-transparent overlay
        guiGraphics.fill(0, 0, width, height, 0x80000000);

        // Main window background
        guiGraphics.fill(windowX, windowY, windowX + WINDOW_WIDTH, windowY + WINDOW_HEIGHT, 0xFF2C2C2C);

        // Window border
        guiGraphics.fill(windowX - 1, windowY - 1, windowX + WINDOW_WIDTH + 1, windowY, 0xFFFFFFFF);
        guiGraphics.fill(windowX - 1, windowY + WINDOW_HEIGHT, windowX + WINDOW_WIDTH + 1, windowY + WINDOW_HEIGHT + 1, 0xFFFFFFFF);
        guiGraphics.fill(windowX - 1, windowY, windowX, windowY + WINDOW_HEIGHT, 0xFFFFFFFF);
        guiGraphics.fill(windowX + WINDOW_WIDTH, windowY, windowX + WINDOW_WIDTH + 1, windowY + WINDOW_HEIGHT, 0xFFFFFFFF);

        // Title
        Component title = Component.literal("Breakthrough Analysis").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        int titleWidth = font.width(title);
        guiGraphics.drawString(font, title, windowX + (WINDOW_WIDTH - titleWidth) / 2, windowY + 15, 0xFFFFFFFF);
    }

    /**
     * Renders breakthrough analysis information using unified cultivation attachment.
     */
    private void renderBreakthroughAnalysis(GuiGraphics guiGraphics) {
        if (cultivationAttachment == null) return;

        int currentY = windowY + 40;

        // Current focus context (important for breakthrough success rates)
        String focusInfo = "Current Focus: " + cultivationAttachment.getCurrentFocus().getDisplayName() + " Cultivation";
        guiGraphics.drawString(font, focusInfo, windowX + 15, currentY, 0xFFFFD700);
        currentY += LINE_HEIGHT + 2;

        // Show talent bonuses if any
        if (talentAttachment != null && (talentAttachment.hasSpiritualRootAssigned() || talentAttachment.hasPhysiqueAssigned())) {
            String talentInfo = String.format("Talent Multipliers: Qi %.1fx | Body %.1fx",
                talentAttachment.getSpiritualRootMultiplier(),
                talentAttachment.getPhysiqueMultiplier());
            guiGraphics.drawString(font, talentInfo, windowX + 15, currentY, 0xFFDDA0DD);
            currentY += LINE_HEIGHT + 2;
        }

        currentY += 8; // Extra spacing

        // Qi cultivation breakthrough readiness
        currentY = renderBreakthroughReadiness(guiGraphics, currentY, PlayerCultivationAttachment.CultivationType.QI);
        currentY += 8;

        // Body cultivation breakthrough readiness
        currentY = renderBreakthroughReadiness(guiGraphics, currentY, PlayerCultivationAttachment.CultivationType.BODY);
    }

    /**
     * Renders breakthrough readiness information for a specific cultivation type using unified attachment.
     */
    private int renderBreakthroughReadiness(GuiGraphics guiGraphics, int startY, PlayerCultivationAttachment.CultivationType type) {
        int currentY = startY;
        int typeColor = type == PlayerCultivationAttachment.CultivationType.QI ? QI_COLOR : BODY_COLOR;
        String typeName = type.getDisplayName();

        // Section header
        String header = typeName + " Cultivation Status";
        guiGraphics.drawString(font, header, windowX + 15, currentY, typeColor);
        currentY += LINE_HEIGHT + 2;

        // Current realm
        CultivationRealm currentRealm = type == PlayerCultivationAttachment.CultivationType.QI ?
            cultivationAttachment.getCurrentQiRealm() : cultivationAttachment.getCurrentBodyRealm();
        String realmInfo = "Realm: " + currentRealm.getDisplayName();
        guiGraphics.drawString(font, realmInfo, windowX + 25, currentY, 0xFFFFFFFF);
        currentY += LINE_HEIGHT;

        // Cultivation progress
        long current, max;
        double percentage;

        if (type == PlayerCultivationAttachment.CultivationType.QI) {
            current = cultivationAttachment.getCurrentQiCultivation();
            max = cultivationAttachment.getMaxQiCultivation();
            percentage = cultivationAttachment.getQiProgress();
        } else {
            current = cultivationAttachment.getCurrentBodyCultivation();
            max = cultivationAttachment.getMaxBodyCultivation();
            percentage = cultivationAttachment.getBodyProgress();
        }

        String progressInfo = String.format("Progress: %d/%d (%.1f%%)", current, max, percentage * 100);
        guiGraphics.drawString(font, progressInfo, windowX + 25, currentY, 0xFFCCCCCC);
        currentY += LINE_HEIGHT;

        // Breakthrough readiness
        boolean meetsThreshold = current >= max;
        int thresholdColor = meetsThreshold ? READY_COLOR : NOT_READY_COLOR;
        String thresholdText = meetsThreshold ? "✓ Ready for breakthrough" : "Need " + (max - current) + " more cultivation";
        guiGraphics.drawString(font, thresholdText, windowX + 25, currentY, thresholdColor);
        currentY += LINE_HEIGHT;

        return currentY;
    }

    /**
     * Renders section headers for breakthrough option groups.
     */
    private void renderSectionHeaders(GuiGraphics guiGraphics) {
        int currentY = windowY + 185; // Position of first section header

        boolean qiReady = canAttemptBreakthrough(PlayerCultivationAttachment.CultivationType.QI);
        boolean qiNear = isNearBreakthrough(PlayerCultivationAttachment.CultivationType.QI);

        if (qiReady || qiNear) {
            String headerText = qiReady ? "Qi Cultivation Breakthrough Options" : "Qi Cultivation (Not Ready)";
            guiGraphics.drawString(font, headerText, windowX + 15, currentY, QI_COLOR);
            currentY += 25 + OPTION_BUTTON_HEIGHT + 8 + SECTION_SPACING;
        }

        boolean bodyReady = canAttemptBreakthrough(PlayerCultivationAttachment.CultivationType.BODY);
        boolean bodyNear = isNearBreakthrough(PlayerCultivationAttachment.CultivationType.BODY);

        if (bodyReady || bodyNear) {
            String headerText = bodyReady ? "Body Cultivation Breakthrough Options" : "Body Cultivation (Not Ready)";
            guiGraphics.drawString(font, headerText, windowX + 15, currentY, BODY_COLOR);
        }
    }

    /**
     * Checks if a breakthrough can currently be attempted using unified cultivation attachment.
     */
    private boolean canAttemptBreakthrough(PlayerCultivationAttachment.CultivationType type) {
        if (cultivationAttachment == null) return false;

        long current, max;
        if (type == PlayerCultivationAttachment.CultivationType.QI) {
            current = cultivationAttachment.getCurrentQiCultivation();
            max = cultivationAttachment.getMaxQiCultivation();
        } else {
            current = cultivationAttachment.getCurrentBodyCultivation();
            max = cultivationAttachment.getMaxBodyCultivation();
        }

        return current >= max;
    }

    /**
     * Checks if a cultivation type is near breakthrough (>80% progress).
     */
    private boolean isNearBreakthrough(PlayerCultivationAttachment.CultivationType type) {
        if (cultivationAttachment == null) return false;

        double progress = type == PlayerCultivationAttachment.CultivationType.QI ?
            cultivationAttachment.getQiProgress() : cultivationAttachment.getBodyProgress();

        return progress > 0.8;
    }

    /**
     * Calculates estimated success rate for a breakthrough.
     */
    private double calculateSuccessRate(PlayerCultivationAttachment.CultivationType type) {
        // Base success rate starts at 10%
        double baseRate = 0.1;

        // Focus bonus
        if (cultivationAttachment.getCurrentFocus() == type) {
            baseRate += 0.2; // +20% for focused cultivation
        }

        // Talent bonus
        if (talentAttachment != null) {
            if (type == PlayerCultivationAttachment.CultivationType.QI && talentAttachment.hasSpiritualRootAssigned()) {
                float multiplier = talentAttachment.getSpiritualRootMultiplier();
                baseRate += (multiplier - 1.0) * 0.1; // Each 0.1x talent adds 1% success rate
            } else if (type == PlayerCultivationAttachment.CultivationType.BODY && talentAttachment.hasPhysiqueAssigned()) {
                float multiplier = talentAttachment.getPhysiqueMultiplier();
                baseRate += (multiplier - 1.0) * 0.1;
            }
        }

        // Cap at 95%
        return Math.min(0.95, baseRate);
    }

    @Override
    public boolean isPauseScreen() {
        return false; // Allow cultivation to continue while analyzing breakthrough options
    }

    /**
     * Specialized button for breakthrough option display and execution using unified system.
     */
    private class BreakthroughOptionButton extends Button {
        private final PlayerCultivationAttachment.CultivationType cultivationType;
        private final CultivationRealm targetRealm;
        private final boolean canAttempt;

        public BreakthroughOptionButton(int x, int y, int width, int height,
                                        PlayerCultivationAttachment.CultivationType cultivationType,
                                        CultivationRealm targetRealm, boolean canAttempt) {
            super(x, y, width, height,
                Component.literal(cultivationType.getDisplayName().toUpperCase() + " BREAKTHROUGH"),
                button -> attemptBreakthrough(cultivationType),
                DEFAULT_NARRATION);
            this.cultivationType = cultivationType;
            this.targetRealm = targetRealm;
            this.canAttempt = canAttempt;
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
            boolean hovered = isHoveredOrFocused();

            // Background with hover state
            int bgColor = hovered ? 0xFF4A4A4A : 0xFF3A3A3A;
            guiGraphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), bgColor);

            // Border color indicates cultivation type and availability
            int borderColor = getBorderColor();

            // Render border
            guiGraphics.fill(getX() - 1, getY() - 1, getX() + getWidth() + 1, getY(), borderColor);
            guiGraphics.fill(getX() - 1, getY() + getHeight(), getX() + getWidth() + 1, getY() + getHeight() + 1, borderColor);
            guiGraphics.fill(getX() - 1, getY(), getX(), getY() + getHeight(), borderColor);
            guiGraphics.fill(getX() + getWidth(), getY(), getX() + getWidth() + 1, getY() + getHeight(), borderColor);

            // Render breakthrough option information
            renderOptionDetails(guiGraphics);
        }

        /**
         * Renders detailed information about this breakthrough option.
         */
        private void renderOptionDetails(GuiGraphics guiGraphics) {
            // Breakthrough type and target
            String typeText = cultivationType.getDisplayName().toUpperCase() + " BREAKTHROUGH";
            String targetText = "→ " + targetRealm.getDisplayName();

            // Success rate with focus bonus indication
            double successRate = calculateSuccessRate(cultivationType);
            String focusBonus = "";
            if (cultivationAttachment.getCurrentFocus() == cultivationType) {
                focusBonus = " (FOCUSED +BONUS)";
            }
            String chanceText = String.format("Estimated Success: %.1f%%", successRate * 100) + focusBonus;

            // Render text information
            guiGraphics.drawString(font, typeText, getX() + 8, getY() + 6, 0xFFFFFFFF);
            guiGraphics.drawString(font, targetText, getX() + 8, getY() + 20, 0xFFAAFFAA);
            guiGraphics.drawString(font, chanceText, getX() + 8, getY() + 34, getSuccessRateColor());

            // Show unavailability reason if needed
            if (!canAttempt) {
                String reason = getUnavailabilityReason();
                guiGraphics.drawString(font, reason, getX() + 8, getY() + 48, 0xFFFF6666);
            }
        }

        /**
         * Determines the appropriate border color for this breakthrough option.
         */
        private int getBorderColor() {
            if (!canAttempt) {
                return 0xFFAA0000; // Red for unavailable
            }

            // Use cultivation type color, with gold highlight for focused cultivation
            if (cultivationAttachment.getCurrentFocus() == cultivationType) {
                return 0xFFFFD700; // Gold for focused cultivation
            }

            return cultivationType == PlayerCultivationAttachment.CultivationType.QI ? QI_COLOR : BODY_COLOR;
        }

        /**
         * Determines the color for success rate display based on the percentage.
         */
        private int getSuccessRateColor() {
            double chance = calculateSuccessRate(cultivationType);
            if (chance >= 0.7) return SUCCESS_HIGH;
            if (chance >= 0.4) return SUCCESS_MED;
            return SUCCESS_LOW;
        }

        /**
         * Provides specific information about why a breakthrough is unavailable.
         */
        private String getUnavailabilityReason() {
            if (cultivationAttachment == null) return "No cultivation data";

            if (!canAttempt) {
                return "Cultivation requirements not met";
            }

            return "Cannot attempt breakthrough";
        }
    }

    /**
     * Attempts a breakthrough for the specified cultivation type using unified system.
     * This uses the simplified breakthrough system in the unified attachment.
     */
    private void attemptBreakthrough(PlayerCultivationAttachment.CultivationType cultivationType) {
        if (cultivationAttachment == null || !canAttemptBreakthrough(cultivationType)) {
            return;
        }

        try {
            PlayerCultivationAttachment.BreakthroughResult result;

            if (cultivationType == PlayerCultivationAttachment.CultivationType.QI) {
                result = cultivationAttachment.attemptQiBreakthrough();
            } else {
                result = cultivationAttachment.attemptBodyBreakthrough();
            }

            // Show result to player
            if (result.success()) {
                ImmortalCultivation.LOGGER.info("Breakthrough successful for {}: {}",
                    cultivationType.getDisplayName(), result.message());
            } else {
                ImmortalCultivation.LOGGER.info("Breakthrough failed for {}: {}",
                    cultivationType.getDisplayName(), result.message());
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error attempting breakthrough", e);
        }

        // Close the screen after attempt
        onClose();
    }
}
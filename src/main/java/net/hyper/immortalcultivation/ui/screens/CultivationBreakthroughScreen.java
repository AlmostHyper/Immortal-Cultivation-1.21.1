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
 * Updated to work with the unified cultivation attachment system and display accurate success rates.
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
    private static final int OPTION_BUTTON_HEIGHT = 80; // Increased to show attempt count

    // Color scheme specifically for breakthrough analysis
    private static final int QI_COLOR = 0xFF87CEEB;
    private static final int BODY_COLOR = 0xFFFF6347;
    private static final int SUCCESS_HIGH = 0xFF00FF00;    // Green for >70% success
    private static final int SUCCESS_MED = 0xFFFFFF00;     // Yellow for 40-70% success
    private static final int SUCCESS_LOW = 0xFFFF6666;     // Red for <40% success
    private static final int READY_COLOR = 0xFF90EE90;     // Light green for ready
    private static final int NOT_READY_COLOR = 0xFFFFB6C1; // Light red for not ready
    private static final int ATTEMPT_COLOR = 0xFFDDA0DD;   // Plum for attempt counts

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

        // Current cultivation status
        String qiInfo = String.format("Qi: %s (%.1f%%)",
            cultivationAttachment.getCurrentQiRealm().getDisplayName(),
            cultivationAttachment.getQiProgress() * 100);

        String bodyInfo = String.format("Body: %s (%.1f%%)",
            cultivationAttachment.getCurrentBodyRealm().getDisplayName(),
            cultivationAttachment.getBodyProgress() * 100);

        guiGraphics.drawString(font, qiInfo, windowX + 15, currentY, QI_COLOR);
        guiGraphics.drawString(font, bodyInfo, windowX + 15, currentY + LINE_HEIGHT, BODY_COLOR);

        currentY += LINE_HEIGHT * 3;

        // Focus information
        String focusInfo = String.format("Current Focus: %s",
            cultivationAttachment.getCurrentFocus().getDisplayName());
        guiGraphics.drawString(font, focusInfo, windowX + 15, currentY, 0xFFFFFFFF);

        currentY += LINE_HEIGHT * 2;

        // Breakthrough readiness analysis
        renderBreakthroughReadiness(guiGraphics, currentY);
    }

    /**
     * Renders breakthrough readiness analysis for both cultivation types.
     * FIXED: Now shows actual server-side success rates and attempt counts.
     */
    private void renderBreakthroughReadiness(GuiGraphics guiGraphics, int startY) {
        int currentY = startY;

        // Header
        guiGraphics.drawString(font, "Breakthrough Analysis:", windowX + 15, currentY, 0xFFD4AF37);
        currentY += LINE_HEIGHT * 2;

        // Qi breakthrough analysis
        if (canAttemptBreakthrough(PlayerCultivationAttachment.CultivationType.QI) ||
            isNearBreakthrough(PlayerCultivationAttachment.CultivationType.QI)) {

            boolean qiReady = canAttemptBreakthrough(PlayerCultivationAttachment.CultivationType.QI);
            double qiSuccessRate = cultivationAttachment.calculateBreakthroughSuccessRate(PlayerCultivationAttachment.CultivationType.QI);
            long qiAttempts = cultivationAttachment.getQiBreakthroughAttempts();

            String qiStatus = String.format("Qi: %s (%.0f%% success)",
                qiReady ? "READY" : "Preparing", qiSuccessRate * 100);

            int qiColor = qiReady ? READY_COLOR : NOT_READY_COLOR;
            guiGraphics.drawString(font, qiStatus, windowX + 15, currentY, qiColor);
            currentY += LINE_HEIGHT;

            // Show attempt count
            String qiAttemptInfo = String.format("  Failed attempts: %d (+%d%% bonus)", qiAttempts, (int)(qiAttempts * 5));
            guiGraphics.drawString(font, qiAttemptInfo, windowX + 15, currentY, ATTEMPT_COLOR);
            currentY += LINE_HEIGHT;
        }

        // Body breakthrough analysis
        if (canAttemptBreakthrough(PlayerCultivationAttachment.CultivationType.BODY) ||
            isNearBreakthrough(PlayerCultivationAttachment.CultivationType.BODY)) {

            boolean bodyReady = canAttemptBreakthrough(PlayerCultivationAttachment.CultivationType.BODY);
            double bodySuccessRate = cultivationAttachment.calculateBreakthroughSuccessRate(PlayerCultivationAttachment.CultivationType.BODY);
            long bodyAttempts = cultivationAttachment.getBodyBreakthroughAttempts();

            String bodyStatus = String.format("Body: %s (%.0f%% success)",
                bodyReady ? "READY" : "Preparing", bodySuccessRate * 100);

            int bodyColor = bodyReady ? READY_COLOR : NOT_READY_COLOR;
            guiGraphics.drawString(font, bodyStatus, windowX + 15, currentY, bodyColor);
            currentY += LINE_HEIGHT;

            // Show attempt count
            String bodyAttemptInfo = String.format("  Failed attempts: %d (+%d%% bonus)", bodyAttempts, (int)(bodyAttempts * 5));
            guiGraphics.drawString(font, bodyAttemptInfo, windowX + 15, currentY, ATTEMPT_COLOR);
        }
    }

    /**
     * Renders section headers for breakthrough options.
     */
    private void renderSectionHeaders(GuiGraphics guiGraphics) {
        if (cultivationAttachment == null) return;

        int headerY = windowY + 185;

        // Only render headers where sections exist
        boolean qiSectionExists = canAttemptBreakthrough(PlayerCultivationAttachment.CultivationType.QI) ||
            isNearBreakthrough(PlayerCultivationAttachment.CultivationType.QI);

        boolean bodySectionExists = canAttemptBreakthrough(PlayerCultivationAttachment.CultivationType.BODY) ||
            isNearBreakthrough(PlayerCultivationAttachment.CultivationType.BODY);

        if (qiSectionExists) {
            Component qiHeader = Component.literal("▲ QI CULTIVATION BREAKTHROUGH ▲").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);
            int qiHeaderWidth = font.width(qiHeader);
            guiGraphics.drawString(font, qiHeader, windowX + (WINDOW_WIDTH - qiHeaderWidth) / 2, headerY, QI_COLOR);

            if (bodySectionExists) {
                headerY += OPTION_BUTTON_HEIGHT + 8 + SECTION_SPACING + 25;
            }
        }

        if (bodySectionExists) {
            Component bodyHeader = Component.literal("▲ BODY CULTIVATION BREAKTHROUGH ▲").withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
            int bodyHeaderWidth = font.width(bodyHeader);
            guiGraphics.drawString(font, bodyHeader, windowX + (WINDOW_WIDTH - bodyHeaderWidth) / 2, headerY, BODY_COLOR);
        }
    }

    /**
     * Checks if player can attempt breakthrough for given cultivation type.
     * Updated to use unified cultivation attachment.
     */
    private boolean canAttemptBreakthrough(PlayerCultivationAttachment.CultivationType type) {
        if (cultivationAttachment == null) return false;

        double progress = type == PlayerCultivationAttachment.CultivationType.QI ?
            cultivationAttachment.getQiProgress() : cultivationAttachment.getBodyProgress();

        boolean onCooldown = cultivationAttachment.isBreakthroughOnCooldown(type);

        // Allow breakthrough at 100% progress and not on cooldown
        return progress >= 1.0 && !onCooldown;
    }

    /**
     * Checks if player is near breakthrough (>80% progress).
     */
    private boolean isNearBreakthrough(PlayerCultivationAttachment.CultivationType type) {
        if (cultivationAttachment == null) return false;

        double progress = type == PlayerCultivationAttachment.CultivationType.QI ?
            cultivationAttachment.getQiProgress() : cultivationAttachment.getBodyProgress();

        return progress > 0.8;
    }

    @Override
    public boolean isPauseScreen() {
        return false; // Allow cultivation to continue while analyzing breakthrough options
    }

    /**
     * Specialized button for breakthrough option display and execution using unified system.
     * UPDATED: Now shows accurate success rates and attempt counts.
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
            int bgColor = hovered ? 0xFF444444 : 0xFF2F2F2F;
            if (!canAttempt) bgColor = 0xFF1F1F1F;

            guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, bgColor);

            // Border
            int borderColor = canAttempt ? getCultivationTypeColor() : 0xFF666666;
            renderBorder(guiGraphics, getX(), getY(), width, height, borderColor, 1);

            // Title
            Component titleText = Component.literal(cultivationType.getDisplayName().toUpperCase() + " BREAKTHROUGH")
                .withStyle(canAttempt ? ChatFormatting.WHITE : ChatFormatting.GRAY);
            int titleWidth = font.width(titleText);
            guiGraphics.drawString(font, titleText,
                getX() + (width - titleWidth) / 2,
                getY() + 8,
                canAttempt ? 0xFFFFFFFF : 0xFF888888);

            // Target realm
            String targetText = "→ " + targetRealm.getDisplayName();
            guiGraphics.drawString(font, targetText,
                getX() + 8,
                getY() + 25,
                getCultivationTypeColor());

            // FIXED: Use actual server-side success rate calculation
            double successRate = cultivationAttachment.calculateBreakthroughSuccessRate(cultivationType);
            String successText = String.format("Success Rate: %.0f%%", successRate * 100);
            guiGraphics.drawString(font, successText,
                getX() + 8,
                getY() + 38,
                getSuccessRateColor(successRate));

            // Show attempt count and bonus
            long attempts = (cultivationType == PlayerCultivationAttachment.CultivationType.QI) ?
                cultivationAttachment.getQiBreakthroughAttempts() : cultivationAttachment.getBodyBreakthroughAttempts();
            String attemptText = String.format("Attempts: %d (+%d%%)", attempts, (int)(attempts * 5));
            guiGraphics.drawString(font, attemptText,
                getX() + 8,
                getY() + 51,
                ATTEMPT_COLOR);

            // Status indicator
            String statusText = canAttempt ? "READY TO ATTEMPT" : getUnavailabilityReason();
            int statusColor = canAttempt ? READY_COLOR : NOT_READY_COLOR;
            guiGraphics.drawString(font, statusText,
                getX() + 8,
                getY() + 64,
                statusColor);
        }

        /**
         * Renders a border around a rectangle.
         */
        private void renderBorder(GuiGraphics guiGraphics, int x, int y, int width, int height, int color, int thickness) {
            // Top
            guiGraphics.fill(x, y, x + width, y + thickness, color);
            // Bottom
            guiGraphics.fill(x, y + height - thickness, x + width, y + height, color);
            // Left
            guiGraphics.fill(x, y, x + thickness, y + height, color);
            // Right
            guiGraphics.fill(x + width - thickness, y, x + width, y + height, color);
        }

        /**
         * Gets the color associated with the cultivation type.
         */
        private int getCultivationTypeColor() {
            return cultivationType == PlayerCultivationAttachment.CultivationType.QI ?
                QI_COLOR : BODY_COLOR;
        }

        /**
         * FIXED: Determines the color for success rate display based on the actual percentage.
         */
        private int getSuccessRateColor(double successRate) {
            if (successRate >= 0.7) return SUCCESS_HIGH;
            if (successRate >= 0.4) return SUCCESS_MED;
            return SUCCESS_LOW;
        }

        /**
         * Provides specific information about why a breakthrough is unavailable.
         */
        private String getUnavailabilityReason() {
            if (cultivationAttachment == null) return "No cultivation data";

            if (cultivationAttachment.isBreakthroughOnCooldown(cultivationType)) {
                return "Breakthrough on cooldown";
            }

            if (!canAttempt) {
                return "Cultivation requirements not met";
            }

            return "Cannot attempt breakthrough";
        }
    }

    /**
     * FIXED: Attempts a breakthrough for the specified cultivation type using network packet.
     * This now properly sends a BreakthroughRequestPacket to the server instead of trying
     * to execute the breakthrough client-side.
     */
    private void attemptBreakthrough(PlayerCultivationAttachment.CultivationType cultivationType) {
        if (cultivationAttachment == null || !canAttemptBreakthrough(cultivationType)) {
            ImmortalCultivation.LOGGER.warn("Cannot attempt breakthrough: cultivation={}, canAttempt={}",
                cultivationAttachment != null, canAttemptBreakthrough(cultivationType));
            return;
        }

        try {
            // FIXED: Send packet to server instead of attempting breakthrough client-side
            BreakthroughRequestPacket packet = new BreakthroughRequestPacket(cultivationType);
            ICModNetwork.sendToServer(packet);

            ImmortalCultivation.LOGGER.info("Sent {} breakthrough request to server",
                cultivationType.getDisplayName());

            // Close the screen after sending the request
            onClose();

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error sending breakthrough request", e);
        }
    }
}
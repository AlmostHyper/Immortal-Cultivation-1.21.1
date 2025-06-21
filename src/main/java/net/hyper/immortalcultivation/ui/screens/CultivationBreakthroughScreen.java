package net.hyper.immortalcultivation.ui.screens;

import net.hyper.immortalcultivation.attachment.*;
import net.hyper.immortalcultivation.cultivation.realms.CultivationRealm;
import net.hyper.immortalcultivation.network.CultivationBreakthroughRequestPacket;
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
 *
 * This screen is designed for the specific task of analyzing and attempting cultivation breakthroughs.
 * It provides detailed information about breakthrough options, success rates, and strategic implications
 * without the distraction of general cultivation monitoring information.
 *
 * Key Design Principles:
 * - Task-focused: Optimized for breakthrough decision-making
 * - Information-rich: Detailed success rates and strategic context
 * - Action-oriented: Clear paths from analysis to execution
 * - Educational: Teaches breakthrough mechanics through clear feedback
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

    // Player cultivation data needed for breakthrough analysis
    private PlayerRealmAttachment realmAttachment;
    private PlayerQiCultivationAttachment qiCultivationAttachment;
    private PlayerBodyCultivationAttachment bodyCultivationAttachment;
    private PlayerCultivationFocusAttachment focusAttachment;

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

        // Gather cultivation data needed for breakthrough analysis
        if (minecraft.player != null) {
            realmAttachment = minecraft.player.getData(ICModAttachmentTypes.PLAYER_REALM.get());
            qiCultivationAttachment = minecraft.player.getData(ICModAttachmentTypes.PLAYER_QI_CULTIVATION.get());
            bodyCultivationAttachment = minecraft.player.getData(ICModAttachmentTypes.PLAYER_BODY_CULTIVATION.get());
            focusAttachment = minecraft.player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION_FOCUS.get());

            // Ensure player reference is set for breakthrough calculations
            if (realmAttachment != null) {
                realmAttachment.setPlayer(minecraft.player);
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

        if (realmAttachment == null) {
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
     * The buttons are organized to clearly show which options belong to which cultivation type.
     */
    private void createBreakthroughOptions() {
        // Get breakthrough options for both cultivation types
        List<PlayerRealmAttachment.BreakthroughOption> qiOptions =
            realmAttachment.getAvailableBreakthroughs(PlayerRealmAttachment.CultivationType.QI);
        List<PlayerRealmAttachment.BreakthroughOption> bodyOptions =
            realmAttachment.getAvailableBreakthroughs(PlayerRealmAttachment.CultivationType.BODY);

        // Calculate starting position for breakthrough options (after analysis display)
        int buttonStartY = windowY + 160;
        int currentY = buttonStartY;

        // Create Qi cultivation breakthrough options
        if (!qiOptions.isEmpty()) {
            currentY = createCultivationTypeSection(qiOptions, PlayerRealmAttachment.CultivationType.QI, currentY);
            currentY += SECTION_SPACING;
        }

        // Create Body cultivation breakthrough options
        if (!bodyOptions.isEmpty()) {
            currentY = createCultivationTypeSection(bodyOptions, PlayerRealmAttachment.CultivationType.BODY, currentY);
        }

        // Show helpful message if no breakthrough options are available
        if (qiOptions.isEmpty() && bodyOptions.isEmpty()) {
            createNoOptionsMessage(buttonStartY);
        }
    }

    /**
     * Creates a section of breakthrough options for a specific cultivation type.
     * This groups related options together and provides clear visual organization.
     */
    private int createCultivationTypeSection(List<PlayerRealmAttachment.BreakthroughOption> options,
                                             PlayerRealmAttachment.CultivationType cultivationType,
                                             int startY) {
        int currentY = startY;

        // Reserve space for section header (rendered in render method)
        currentY += 25;

        // Create buttons for each breakthrough option
        for (PlayerRealmAttachment.BreakthroughOption option : options) {
            BreakthroughOptionButton button = new BreakthroughOptionButton(
                windowX + 15, currentY,
                WINDOW_WIDTH - 30, OPTION_BUTTON_HEIGHT,
                option, cultivationType
            );
            breakthroughButtons.add(button);
            addRenderableWidget(button);
            currentY += OPTION_BUTTON_HEIGHT + 8; // Space between options
        }

        return currentY;
    }

    /**
     * Creates a helpful message when no breakthrough options are available.
     * This provides guidance about what players need to do to unlock breakthrough opportunities.
     */
    private void createNoOptionsMessage(int startY) {
        String message = determineNoOptionsReason();

        addRenderableWidget(Button.builder(Component.literal(message), button -> {})
            .bounds(windowX + 20, startY, WINDOW_WIDTH - 40, 40)
            .build());
    }

    /**
     * Determines why no breakthrough options are available and provides specific guidance.
     * This helps players understand what they need to do to progress their cultivation.
     */
    private String determineNoOptionsReason() {
        if (realmAttachment == null) return "Cultivation data unavailable";

        boolean qiCanAttempt = realmAttachment.canAttemptBreakthrough(PlayerRealmAttachment.CultivationType.QI);
        boolean bodyCanAttempt = realmAttachment.canAttemptBreakthrough(PlayerRealmAttachment.CultivationType.BODY);

        if (!qiCanAttempt && !bodyCanAttempt) {
            // Check if it's a cultivation level issue or cooldown issue
            boolean qiOnCooldown = realmAttachment.isOnBreakthroughCooldown(PlayerRealmAttachment.CultivationType.QI);
            boolean bodyOnCooldown = realmAttachment.isOnBreakthroughCooldown(PlayerRealmAttachment.CultivationType.BODY);

            if (qiOnCooldown && bodyOnCooldown) {
                return "Both cultivation paths are on breakthrough cooldown";
            } else {
                return "Continue cultivating to meet breakthrough requirements";
            }
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
     * Renders breakthrough analysis information focusing on readiness and strategic context.
     * This provides the essential information players need for breakthrough decision-making.
     */
    private void renderBreakthroughAnalysis(GuiGraphics guiGraphics) {
        if (realmAttachment == null) return;

        int currentY = windowY + 40;

        // Current focus context (important for breakthrough success rates)
        if (focusAttachment != null) {
            String focusInfo = "Current Focus: " + focusAttachment.getCurrentFocus().getDisplayName() + " Cultivation";
            guiGraphics.drawString(font, focusInfo, windowX + 15, currentY, 0xFFFFD700);
            currentY += LINE_HEIGHT + 2;
        }

        currentY += 8; // Extra spacing

        // Qi cultivation breakthrough readiness
        currentY = renderBreakthroughReadiness(guiGraphics, currentY, PlayerRealmAttachment.CultivationType.QI);
        currentY += 8;

        // Body cultivation breakthrough readiness
        currentY = renderBreakthroughReadiness(guiGraphics, currentY, PlayerRealmAttachment.CultivationType.BODY);
    }

    /**
     * Renders breakthrough readiness information for a specific cultivation type.
     * This shows current realm, cultivation progress, and readiness status.
     */
    private int renderBreakthroughReadiness(GuiGraphics guiGraphics, int startY, PlayerRealmAttachment.CultivationType type) {
        int currentY = startY;
        int typeColor = type == PlayerRealmAttachment.CultivationType.QI ? QI_COLOR : BODY_COLOR;
        String typeName = type.getDisplayName();

        // Section header
        String header = typeName + " Cultivation Status";
        guiGraphics.drawString(font, header, windowX + 15, currentY, typeColor);
        currentY += LINE_HEIGHT + 2;

        // Current realm
        CultivationRealm currentRealm = realmAttachment.getCurrentRealm(type);
        String realmInfo = "Realm: " + currentRealm.getDisplayName();
        guiGraphics.drawString(font, realmInfo, windowX + 25, currentY, 0xFFFFFFFF);
        currentY += LINE_HEIGHT;

        // Cultivation progress and threshold
        long current, max, threshold;
        if (type == PlayerRealmAttachment.CultivationType.QI && qiCultivationAttachment != null) {
            current = qiCultivationAttachment.getCurrentQiCultivation();
            max = qiCultivationAttachment.getMaxQiCultivation();
        } else if (type == PlayerRealmAttachment.CultivationType.BODY && bodyCultivationAttachment != null) {
            current = bodyCultivationAttachment.getCurrentBodyCultivation();
            max = bodyCultivationAttachment.getMaxBodyCultivation();
        } else {
            return currentY;
        }

        threshold = currentRealm.getBreakthroughCultivationThreshold();
        double percentage = max > 0 ? (double) current / max * 100 : 0;

        String progressInfo = String.format("Progress: %d/%d (%.1f%%)", current, max, percentage);
        guiGraphics.drawString(font, progressInfo, windowX + 25, currentY, 0xFFCCCCCC);
        currentY += LINE_HEIGHT;

        // Breakthrough threshold status
        boolean meetsThreshold = current >= threshold;
        int thresholdColor = meetsThreshold ? READY_COLOR : NOT_READY_COLOR;
        String thresholdText = meetsThreshold ? "✓ Ready for breakthrough" : "Need " + (threshold - current) + " more cultivation";
        guiGraphics.drawString(font, thresholdText, windowX + 25, currentY, thresholdColor);
        currentY += LINE_HEIGHT;

        // Cooldown status
        if (realmAttachment.isOnBreakthroughCooldown(type)) {
            long remainingSeconds = realmAttachment.getRemainingCooldownTicks(type) / 20;
            String cooldownText = "Cooldown: " + remainingSeconds + "s remaining";
            guiGraphics.drawString(font, cooldownText, windowX + 25, currentY, 0xFFFF6666);
            currentY += LINE_HEIGHT;
        }

        return currentY;
    }

    /**
     * Renders section headers for breakthrough option groups.
     */
    private void renderSectionHeaders(GuiGraphics guiGraphics) {
        List<PlayerRealmAttachment.BreakthroughOption> qiOptions =
            realmAttachment.getAvailableBreakthroughs(PlayerRealmAttachment.CultivationType.QI);
        List<PlayerRealmAttachment.BreakthroughOption> bodyOptions =
            realmAttachment.getAvailableBreakthroughs(PlayerRealmAttachment.CultivationType.BODY);

        int currentY = windowY + 185; // Position of first section header

        if (!qiOptions.isEmpty()) {
            guiGraphics.drawString(font, "Qi Cultivation Breakthrough Options", windowX + 15, currentY, QI_COLOR);
            currentY += 25 + (qiOptions.size() * (OPTION_BUTTON_HEIGHT + 8)) + SECTION_SPACING;
        }

        if (!bodyOptions.isEmpty()) {
            guiGraphics.drawString(font, "Body Cultivation Breakthrough Options", windowX + 15, currentY, BODY_COLOR);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false; // Allow cultivation to continue while analyzing breakthrough options
    }

    /**
     * Specialized button for breakthrough option display and execution.
     * Provides comprehensive information about each breakthrough option including
     * success rates, target realms, and strategic implications.
     */
    private class BreakthroughOptionButton extends Button {
        private final PlayerRealmAttachment.BreakthroughOption option;
        private final PlayerRealmAttachment.CultivationType cultivationType;

        public BreakthroughOptionButton(int x, int y, int width, int height,
                                        PlayerRealmAttachment.BreakthroughOption option,
                                        PlayerRealmAttachment.CultivationType cultivationType) {
            super(x, y, width, height,
                Component.literal(option.type().name() + " BREAKTHROUGH"),
                button -> attemptBreakthrough(option.type(), cultivationType),
                DEFAULT_NARRATION);
            this.option = option;
            this.cultivationType = cultivationType;
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
            boolean hovered = isHoveredOrFocused();
            boolean canAttempt = canAttemptBreakthrough();

            // Background with hover state
            int bgColor = hovered ? 0xFF4A4A4A : 0xFF3A3A3A;
            guiGraphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), bgColor);

            // Border color indicates cultivation type and availability
            int borderColor = getBorderColor(canAttempt);

            // Render border
            guiGraphics.fill(getX() - 1, getY() - 1, getX() + getWidth() + 1, getY(), borderColor);
            guiGraphics.fill(getX() - 1, getY() + getHeight(), getX() + getWidth() + 1, getY() + getHeight() + 1, borderColor);
            guiGraphics.fill(getX() - 1, getY(), getX(), getY() + getHeight(), borderColor);
            guiGraphics.fill(getX() + getWidth(), getY(), getX() + getWidth() + 1, getY() + getHeight(), borderColor);

            // Render breakthrough option information
            renderOptionDetails(guiGraphics, canAttempt);
        }

        /**
         * Renders detailed information about this breakthrough option.
         */
        private void renderOptionDetails(GuiGraphics guiGraphics, boolean canAttempt) {
            // Breakthrough type and target
            String typeText = option.type().name() + " " + cultivationType.getDisplayName().toUpperCase() + " BREAKTHROUGH";
            String targetText = "→ " + option.targetRealm().getDisplayName();

            // Success rate with focus bonus indication
            String focusBonus = "";
            if (focusAttachment != null && focusAttachment.getCurrentFocus() == cultivationType) {
                focusBonus = " (FOCUSED +BONUS)";
            }
            String chanceText = String.format("Success Rate: %.1f%%", option.successChance() * 100) + focusBonus;

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
        private int getBorderColor(boolean canAttempt) {
            if (!canAttempt) {
                return 0xFFAA0000; // Red for unavailable
            }

            // Use cultivation type color, with gold highlight for focused cultivation
            if (focusAttachment != null && focusAttachment.getCurrentFocus() == cultivationType) {
                return 0xFFFFD700; // Gold for focused cultivation
            }

            return cultivationType == PlayerRealmAttachment.CultivationType.QI ? QI_COLOR : BODY_COLOR;
        }

        /**
         * Determines the color for success rate display based on the percentage.
         */
        private int getSuccessRateColor() {
            double chance = option.successChance();
            if (chance >= 0.7) return SUCCESS_HIGH;
            if (chance >= 0.4) return SUCCESS_MED;
            return SUCCESS_LOW;
        }

        /**
         * Checks if this breakthrough can currently be attempted.
         */
        private boolean canAttemptBreakthrough() {
            return realmAttachment != null && realmAttachment.canAttemptBreakthrough(cultivationType);
        }

        /**
         * Provides specific information about why a breakthrough is unavailable.
         */
        private String getUnavailabilityReason() {
            if (realmAttachment == null) return "No realm data";

            if (realmAttachment.isOnBreakthroughCooldown(cultivationType)) {
                long seconds = realmAttachment.getRemainingCooldownTicks(cultivationType) / 20;
                return "Cooldown: " + seconds + "s remaining";
            }

            if (!realmAttachment.meetsBreakthroughRequirements(cultivationType)) {
                return "Cultivation requirements not met";
            }

            return "Cannot attempt breakthrough";
        }
    }

    /**
     * Attempts a breakthrough for the specified cultivation type and breakthrough type.
     * Sends the request to the server and closes the interface.
     */
    private void attemptBreakthrough(PlayerRealmAttachment.BreakthroughType type,
                                     PlayerRealmAttachment.CultivationType cultivationType) {
        ICModNetwork.sendToServer(new CultivationBreakthroughRequestPacket(cultivationType, type));
        onClose();
    }
}
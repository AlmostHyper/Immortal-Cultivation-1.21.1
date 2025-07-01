package net.hyper.immortalcultivation.ui.screens;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.attachment.PlayerCultivationAttachment;
import net.hyper.immortalcultivation.attachment.PlayerResourceAttachment;
import net.hyper.immortalcultivation.attachment.PlayerTalentAttachment;
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
 * Enhanced breakthrough interface supporting multiple breakthrough types with different success rates.
 * Shows detailed success rate breakdowns and allows players to choose their breakthrough strategy.
 */
public class CultivationBreakthroughScreen extends Screen {
    // Layout constants for enhanced interface
    private static final int WINDOW_WIDTH = 480;
    private static final int WINDOW_HEIGHT = 600;

    // Visual organization
    private static final int SECTION_SPACING = 20;
    private static final int LINE_HEIGHT = 14;
    private static final int OPTION_BUTTON_HEIGHT = 90; // Increased for more info

    // Color scheme for breakthrough types
    private static final int QI_COLOR = 0xFF87CEEB;
    private static final int BODY_COLOR = 0xFFFF6347;
    private static final int MINOR_COLOR = 0xFF90EE90;    // Light green
    private static final int MAJOR_COLOR = 0xFFFFD700;    // Gold
    private static final int SPECIAL_COLOR = 0xFFDDA0DD;  // Plum
    private static final int BOUNDARY_COLOR = 0xFFFF6347; // Red
    private static final int SUCCESS_HIGH = 0xFF00FF00;
    private static final int SUCCESS_MED = 0xFFFFFF00;
    private static final int SUCCESS_LOW = 0xFFFF6666;
    private static final int READY_COLOR = 0xFF90EE90;
    private static final int NOT_READY_COLOR = 0xFFFFB6C1;
    private static final int ATTEMPT_COLOR = 0xFFDDA0DD;

    // Player cultivation data
    private PlayerCultivationAttachment cultivationAttachment;
    private PlayerResourceAttachment resourceAttachment;
    private PlayerTalentAttachment talentAttachment;

    private int windowX, windowY;
    private List<EnhancedBreakthroughOptionButton> breakthroughButtons = new ArrayList<>();

    public CultivationBreakthroughScreen() {
        super(Component.literal("Cultivation Breakthrough Analysis"));
    }

    @Override
    protected void init() {
        super.init();

        // Center the window
        windowX = (width - WINDOW_WIDTH) / 2;
        windowY = (height - WINDOW_HEIGHT) / 2;

        // Gather cultivation data
        if (minecraft.player != null) {
            cultivationAttachment = minecraft.player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
            resourceAttachment = minecraft.player.getData(ICModAttachmentTypes.PLAYER_RESOURCES.get());
            talentAttachment = minecraft.player.getData(ICModAttachmentTypes.PLAYER_TALENTS.get());

            if (cultivationAttachment != null) {
                cultivationAttachment.setPlayer(minecraft.player);
            }
        }

        setupBreakthroughInterface();
    }

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

        // Create enhanced breakthrough option buttons
        createEnhancedBreakthroughOptions();
    }

    private void createEnhancedBreakthroughOptions() {
        int buttonStartY = windowY + 200; // Leave more space for analysis
        int currentY = buttonStartY;

        // Get all available breakthrough options
        PlayerCultivationAttachment.BreakthroughOptions options = cultivationAttachment.getAllAvailableBreakthroughOptions();

        // Create Qi breakthrough section
        if (!options.qiOptions().isEmpty()) {
            currentY = createBreakthroughTypeSection(PlayerCultivationAttachment.CultivationType.QI, options.qiOptions(), currentY);
            currentY += SECTION_SPACING;
        }

        // Create Body breakthrough section
        if (!options.bodyOptions().isEmpty()) {
            currentY = createBreakthroughTypeSection(PlayerCultivationAttachment.CultivationType.BODY, options.bodyOptions(), currentY);
        }

        // Show message if no options available
        if (options.qiOptions().isEmpty() && options.bodyOptions().isEmpty()) {
            createNoOptionsMessage(buttonStartY);
        }
    }

    private int createBreakthroughTypeSection(PlayerCultivationAttachment.CultivationType cultivationType,
                                              List<PlayerCultivationAttachment.BreakthroughOption> options, int startY) {
        int currentY = startY;

        // Reserve space for section header (rendered in render method)
        currentY += 25;

        // Create buttons for each breakthrough type
        for (PlayerCultivationAttachment.BreakthroughOption option : options) {
            if (option.available() || isNearBreakthrough(cultivationType)) {
                EnhancedBreakthroughOptionButton button = new EnhancedBreakthroughOptionButton(
                    windowX + 15, currentY,
                    WINDOW_WIDTH - 30, OPTION_BUTTON_HEIGHT,
                    option
                );
                breakthroughButtons.add(button);
                addRenderableWidget(button);
                currentY += OPTION_BUTTON_HEIGHT + 8;
            }
        }

        return currentY;
    }

    private void createNoOptionsMessage(int startY) {
        String message = determineNoOptionsReason();
        addRenderableWidget(Button.builder(Component.literal(message), button -> {})
            .bounds(windowX + 20, startY, WINDOW_WIDTH - 40, 40)
            .build());
    }

    private String determineNoOptionsReason() {
        if (cultivationAttachment == null) return "Cultivation data unavailable";

        boolean qiCanAttempt = canAttemptBreakthrough(PlayerCultivationAttachment.CultivationType.QI);
        boolean bodyCanAttempt = canAttemptBreakthrough(PlayerCultivationAttachment.CultivationType.BODY);

        if (!qiCanAttempt && !bodyCanAttempt) {
            return "Continue cultivating to meet breakthrough requirements";
        }

        return "No breakthrough paths available from current realms";
    }

    private void createErrorInterface() {
        addRenderableWidget(Button.builder(Component.literal("Cultivation data not available"), button -> {})
            .bounds(windowX + 20, windowY + 100, WINDOW_WIDTH - 40, 30)
            .build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        renderWindowFrame(guiGraphics);
        renderEnhancedBreakthroughAnalysis(guiGraphics);
        renderSectionHeaders(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

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

        // Enhanced title
        Component title = Component.literal("Enhanced Breakthrough Analysis").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        int titleWidth = font.width(title);
        guiGraphics.drawString(font, title, windowX + (WINDOW_WIDTH - titleWidth) / 2, windowY + 15, 0xFFFFFFFF);
    }

    private void renderEnhancedBreakthroughAnalysis(GuiGraphics guiGraphics) {
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
        String focusInfo = String.format("Current Focus: %s (+5%% breakthrough bonus)",
            cultivationAttachment.getCurrentFocus().getDisplayName());
        guiGraphics.drawString(font, focusInfo, windowX + 15, currentY, 0xFFFFFFFF);

        currentY += LINE_HEIGHT * 2;

        // Enhanced breakthrough analysis
        renderEnhancedBreakthroughReadiness(guiGraphics, currentY);
    }

    private void renderEnhancedBreakthroughReadiness(GuiGraphics guiGraphics, int startY) {
        int currentY = startY;

        // Header
        guiGraphics.drawString(font, "Breakthrough Type Analysis:", windowX + 15, currentY, 0xFFD4AF37);
        currentY += LINE_HEIGHT * 2;

        // Type explanations
        String[] typeExplanations = {
            "Minor (50% base): Standard realm advancement",
            "Major (30% base): Significant progression",
            "Special (20% base): Unique cultivation paths",
            "Boundary (10% base): Major stage transitions"
        };

        int[] typeColors = {MINOR_COLOR, MAJOR_COLOR, SPECIAL_COLOR, BOUNDARY_COLOR};

        for (int i = 0; i < typeExplanations.length; i++) {
            guiGraphics.drawString(font, typeExplanations[i], windowX + 15, currentY, typeColors[i]);
            currentY += LINE_HEIGHT;
        }

        currentY += LINE_HEIGHT;

        // Show current bonuses
        guiGraphics.drawString(font, "Current Bonuses:", windowX + 15, currentY, 0xFFD4AF37);
        currentY += LINE_HEIGHT;

        // Show attempt bonuses for each type
        long qiAttempts = cultivationAttachment.getQiBreakthroughAttempts();
        long bodyAttempts = cultivationAttachment.getBodyBreakthroughAttempts();

        if (qiAttempts > 0 || bodyAttempts > 0) {
            String qiAttemptInfo = String.format("  Qi failed attempts: %d (+%d%% all types)", qiAttempts, (int)(qiAttempts * 5));
            String bodyAttemptInfo = String.format("  Body failed attempts: %d (+%d%% all types)", bodyAttempts, (int)(bodyAttempts * 5));

            guiGraphics.drawString(font, qiAttemptInfo, windowX + 15, currentY, ATTEMPT_COLOR);
            currentY += LINE_HEIGHT;
            guiGraphics.drawString(font, bodyAttemptInfo, windowX + 15, currentY, ATTEMPT_COLOR);
            currentY += LINE_HEIGHT;
        }

        // Show talent bonuses if applicable
        if (talentAttachment != null) {
            if (talentAttachment.hasSpiritualRootAssigned()) {
                float multiplier = talentAttachment.getSpiritualRootMultiplier();
                double bonus = (multiplier - 1.0) * 20; // 20% per multiplier point
                String talentInfo = String.format("  Spiritual Root: +%.0f%% Qi breakthroughs", bonus);
                guiGraphics.drawString(font, talentInfo, windowX + 15, currentY, 0xFFADD8E6);
                currentY += LINE_HEIGHT;
            }

            if (talentAttachment.hasPhysiqueAssigned()) {
                float multiplier = talentAttachment.getPhysiqueMultiplier();
                double bonus = (multiplier - 1.0) * 20; // 20% per multiplier point
                String talentInfo = String.format("  Physique: +%.0f%% Body breakthroughs", bonus);
                guiGraphics.drawString(font, talentInfo, windowX + 15, currentY, 0xFFADD8E6);
                currentY += LINE_HEIGHT;
            }
        }
    }

    private void renderSectionHeaders(GuiGraphics guiGraphics) {
        if (cultivationAttachment == null) return;

        int headerY = windowY + 225;

        // Qi section header
        boolean qiSectionExists = hasAvailableBreakthroughs(PlayerCultivationAttachment.CultivationType.QI);
        if (qiSectionExists) {
            Component qiHeader = Component.literal("▲ QI CULTIVATION BREAKTHROUGH OPTIONS ▲").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);
            int qiHeaderWidth = font.width(qiHeader);
            guiGraphics.drawString(font, qiHeader, windowX + (WINDOW_WIDTH - qiHeaderWidth) / 2, headerY, QI_COLOR);

            // Calculate Y position for body section
            int qiOptionsCount = cultivationAttachment.getBreakthroughOptionsForType(PlayerCultivationAttachment.CultivationType.QI).size();
            headerY += 25 + (qiOptionsCount * (OPTION_BUTTON_HEIGHT + 8)) + SECTION_SPACING;
        }

        // Body section header
        boolean bodySectionExists = hasAvailableBreakthroughs(PlayerCultivationAttachment.CultivationType.BODY);
        if (bodySectionExists) {
            Component bodyHeader = Component.literal("▲ BODY CULTIVATION BREAKTHROUGH OPTIONS ▲").withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
            int bodyHeaderWidth = font.width(bodyHeader);
            guiGraphics.drawString(font, bodyHeader, windowX + (WINDOW_WIDTH - bodyHeaderWidth) / 2, headerY, BODY_COLOR);
        }
    }

    private boolean hasAvailableBreakthroughs(PlayerCultivationAttachment.CultivationType type) {
        return cultivationAttachment.getBreakthroughOptionsForType(type).stream()
            .anyMatch(option -> option.available() || isNearBreakthrough(type));
    }

    private boolean canAttemptBreakthrough(PlayerCultivationAttachment.CultivationType type) {
        if (cultivationAttachment == null) return false;

        double progress = type == PlayerCultivationAttachment.CultivationType.QI ?
            cultivationAttachment.getQiProgress() : cultivationAttachment.getBodyProgress();

        boolean onCooldown = cultivationAttachment.isBreakthroughOnCooldown(type);

        return progress >= 1.0 && !onCooldown;
    }

    private boolean isNearBreakthrough(PlayerCultivationAttachment.CultivationType type) {
        if (cultivationAttachment == null) return false;

        double progress = type == PlayerCultivationAttachment.CultivationType.QI ?
            cultivationAttachment.getQiProgress() : cultivationAttachment.getBodyProgress();

        return progress > 0.8;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /**
     * Enhanced breakthrough option button showing detailed information for each breakthrough type
     */
    private class EnhancedBreakthroughOptionButton extends Button {
        private final PlayerCultivationAttachment.BreakthroughOption option;

        public EnhancedBreakthroughOptionButton(int x, int y, int width, int height,
                                                PlayerCultivationAttachment.BreakthroughOption option) {
            super(x, y, width, height,
                Component.literal(option.type().getDisplayName()),
                button -> attemptTypedBreakthrough(option),
                DEFAULT_NARRATION);
            this.option = option;
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
            boolean hovered = isHoveredOrFocused();

            // Background with hover state
            int bgColor = hovered ? 0xFF444444 : 0xFF2F2F2F;
            if (!option.available()) bgColor = 0xFF1F1F1F;

            guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, bgColor);

            // Border with type-specific color
            int borderColor = option.available() ? getBreakthroughTypeColor() : 0xFF666666;
            renderBorder(guiGraphics, getX(), getY(), width, height, borderColor, 1);

            // Title with type name
            Component titleText = Component.literal(option.type().getDisplayName().toUpperCase())
                .withStyle(option.available() ? ChatFormatting.WHITE : ChatFormatting.GRAY);
            int titleWidth = font.width(titleText);
            guiGraphics.drawString(font, titleText,
                getX() + (width - titleWidth) / 2,
                getY() + 8,
                option.available() ? 0xFFFFFFFF : 0xFF888888);

            // Target realm
            String targetText = "→ " + option.targetRealm().getDisplayName();
            guiGraphics.drawString(font, targetText,
                getX() + 8,
                getY() + 25,
                getCultivationTypeColor());

            // Success rate with detailed breakdown
            if (option.available()) {
                PlayerCultivationAttachment.BreakthroughChanceBreakdown breakdown =
                    cultivationAttachment.getBreakthroughChanceBreakdown(option.cultivationType(), option.type());

                String successText = String.format("Success Rate: %.0f%%", breakdown.totalChance() * 100);
                guiGraphics.drawString(font, successText,
                    getX() + 8,
                    getY() + 38,
                    getSuccessRateColor(breakdown.totalChance()));

                // Breakdown details
                String breakdownText = breakdown.getFormattedBreakdown();
                guiGraphics.drawString(font, breakdownText,
                    getX() + 8,
                    getY() + 51,
                    0xFFAAAAAA);

                // Attempt count
                String attemptText = String.format("Failed Attempts: %d", breakdown.attempts());
                guiGraphics.drawString(font, attemptText,
                    getX() + 8,
                    getY() + 64,
                    ATTEMPT_COLOR);

                // Status
                String statusText = "READY TO ATTEMPT";
                guiGraphics.drawString(font, statusText,
                    getX() + 8,
                    getY() + 77,
                    READY_COLOR);
            } else {
                // Show unavailability reason
                String reasonText = option.unavailabilityReason();
                guiGraphics.drawString(font, reasonText,
                    getX() + 8,
                    getY() + 38,
                    NOT_READY_COLOR);
            }
        }

        private void renderBorder(GuiGraphics guiGraphics, int x, int y, int width, int height, int color, int thickness) {
            guiGraphics.fill(x, y, x + width, y + thickness, color);
            guiGraphics.fill(x, y + height - thickness, x + width, y + height, color);
            guiGraphics.fill(x, y, x + thickness, y + height, color);
            guiGraphics.fill(x + width - thickness, y, x + width, y + height, color);
        }

        private int getBreakthroughTypeColor() {
            return switch (option.type()) {
                case MINOR -> MINOR_COLOR;
                case MAJOR -> MAJOR_COLOR;
                case SPECIAL -> SPECIAL_COLOR;
                case BOUNDARY -> BOUNDARY_COLOR;
            };
        }

        private int getCultivationTypeColor() {
            return option.cultivationType() == PlayerCultivationAttachment.CultivationType.QI ?
                QI_COLOR : BODY_COLOR;
        }

        private int getSuccessRateColor(double successRate) {
            if (successRate >= 0.7) return SUCCESS_HIGH;
            if (successRate >= 0.4) return SUCCESS_MED;
            return SUCCESS_LOW;
        }
    }

    /**
     * Attempt a specific type of breakthrough
     */
    private void attemptTypedBreakthrough(PlayerCultivationAttachment.BreakthroughOption option) {
        if (cultivationAttachment == null || !option.available()) {
            ImmortalCultivation.LOGGER.warn("Cannot attempt breakthrough: option not available");
            return;
        }

        try {
            // Send the enhanced breakthrough request with specific type
            BreakthroughRequestPacket packet = new BreakthroughRequestPacket(
                option.cultivationType(), option.type());
            ICModNetwork.sendToServer(packet);

            ImmortalCultivation.LOGGER.info("Sent {} {} breakthrough request to server",
                option.cultivationType().getDisplayName(), option.type().getDisplayName());

            onClose();

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error sending breakthrough request", e);
        }
    }
}
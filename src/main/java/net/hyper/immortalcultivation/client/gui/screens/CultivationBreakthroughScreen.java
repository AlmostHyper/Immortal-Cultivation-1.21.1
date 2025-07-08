package net.hyper.immortalcultivation.client.gui.screens;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.capability.attachments.PlayerCultivationAttachment;
import net.hyper.immortalcultivation.common.capability.attachments.PlayerResourceAttachment;
import net.hyper.immortalcultivation.common.capability.attachments.PlayerTalentAttachment;
import net.hyper.immortalcultivation.core.network.packets.client.BreakthroughRequestPacket;
import net.hyper.immortalcultivation.common.registry.ICModAttachmentTypes;
import net.hyper.immortalcultivation.core.network.ICModPacketRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * Enhanced breakthrough interface supporting multiple breakthrough types with different success rates.
 * Shows detailed success rate breakdowns and allows players to choose their breakthrough strategy.
 * Responsive design that adapts to screen size and implements scrolling when needed.
 */
public class CultivationBreakthroughScreen extends Screen {
    // Dynamic layout constants
    private static final int BASE_WINDOW_WIDTH = 480;
    private static final int MIN_WINDOW_HEIGHT = 400;
    private static final int MAX_WINDOW_HEIGHT = 700;
    private static final int HEADER_HEIGHT = 120; // Reduced to give more space for buttons

    // Visual organization
    private static final int SECTION_SPACING = 20;
    private static final int LINE_HEIGHT = 14;
    private static final int OPTION_BUTTON_HEIGHT = 90;
    private static final int SCROLL_SPEED = 15;

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

    // Layout variables
    private int windowX, windowY, windowWidth, windowHeight;
    private int scrollableContentHeight;
    private int scrollOffset = 0;
    private int maxScrollOffset = 0;
    private int contentAreaHeight;

    private List<BreakthroughButtonData> buttonDataList = new ArrayList<>();

    // Data class to store button information
    private static class BreakthroughButtonData {
        final PlayerCultivationAttachment.BreakthroughOption option;
        final int virtualY; // Y position in virtual coordinate system
        final PlayerCultivationAttachment.CultivationType sectionType;

        BreakthroughButtonData(PlayerCultivationAttachment.BreakthroughOption option, int virtualY,
                               PlayerCultivationAttachment.CultivationType sectionType) {
            this.option = option;
            this.virtualY = virtualY;
            this.sectionType = sectionType;
        }
    }

    public CultivationBreakthroughScreen() {
        super(Component.literal("Cultivation Breakthrough Analysis"));
    }

    @Override
    protected void init() {
        super.init();

        // Gather cultivation data first
        if (minecraft.player != null) {
            cultivationAttachment = minecraft.player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
            resourceAttachment = minecraft.player.getData(ICModAttachmentTypes.PLAYER_RESOURCES.get());
            talentAttachment = minecraft.player.getData(ICModAttachmentTypes.PLAYER_TALENTS.get());

            if (cultivationAttachment != null) {
                cultivationAttachment.setPlayer(minecraft.player);
            }
        }

        // Calculate window dimensions and content
        calculateWindowDimensions();
        setupBreakthroughInterface();
    }

    private void calculateWindowDimensions() {
        // Use responsive width but limit to reasonable size
        windowWidth = Math.min(BASE_WINDOW_WIDTH, Math.max(400, (int)(width * 0.8)));

        // Calculate content requirements
        calculateContentRequirements();

        // Determine window height - give more space for content
        int availableHeight = height - 80; // Reduced margin for more space
        int desiredHeight = HEADER_HEIGHT + Math.min(scrollableContentHeight, 500) + 40; // Increased max content height
        windowHeight = Mth.clamp(desiredHeight, MIN_WINDOW_HEIGHT, Math.min(MAX_WINDOW_HEIGHT, availableHeight));

        // Calculate scrollable area
        contentAreaHeight = windowHeight - HEADER_HEIGHT - 40;
        maxScrollOffset = Math.max(0, scrollableContentHeight - contentAreaHeight);
        scrollOffset = 0; // Always start at the top to show Qi options first

        // Center the window
        windowX = (width - windowWidth) / 2;
        windowY = (height - windowHeight) / 2;
    }

    private void calculateContentRequirements() {
        buttonDataList.clear();
        scrollableContentHeight = 0;

        if (cultivationAttachment == null) {
            scrollableContentHeight = 60; // Error message
            return;
        }

        PlayerCultivationAttachment.BreakthroughOptions options = cultivationAttachment.getAllAvailableBreakthroughOptions();
        int currentVirtualY = 0;

        // Qi section - Show ALL Qi options, regardless of availability
        if (!options.qiOptions().isEmpty()) {
            currentVirtualY += 30; // Section header space

            for (PlayerCultivationAttachment.BreakthroughOption option : options.qiOptions()) {
                // Show ALL options, not just available ones
                buttonDataList.add(new BreakthroughButtonData(option, currentVirtualY, PlayerCultivationAttachment.CultivationType.QI));
                currentVirtualY += OPTION_BUTTON_HEIGHT + 8;
            }
            currentVirtualY += SECTION_SPACING;
        }

        // Body section - Show ALL Body options, regardless of availability
        if (!options.bodyOptions().isEmpty()) {
            currentVirtualY += 30; // Section header space

            for (PlayerCultivationAttachment.BreakthroughOption option : options.bodyOptions()) {
                // Show ALL options, not just available ones
                buttonDataList.add(new BreakthroughButtonData(option, currentVirtualY, PlayerCultivationAttachment.CultivationType.BODY));
                currentVirtualY += OPTION_BUTTON_HEIGHT + 8;
            }
        }

        // Handle empty options
        if (options.qiOptions().isEmpty() && options.bodyOptions().isEmpty()) {
            currentVirtualY = 60; // Space for "no options" message
        }

        scrollableContentHeight = currentVirtualY;
    }

    private void setupBreakthroughInterface() {
        clearWidgets();

        // Close button (always visible)
        addRenderableWidget(Button.builder(Component.literal("Close"),
                button -> onClose())
            .bounds(windowX + windowWidth - 70, windowY + 10, 60, 20)
            .build());

        // Create visible breakthrough buttons
        createVisibleBreakthroughButtons();

        // Create no options message if needed
        if (buttonDataList.isEmpty() && cultivationAttachment != null) {
            createNoOptionsMessage();
        } else if (cultivationAttachment == null) {
            createErrorInterface();
        }
    }

    private void createVisibleBreakthroughButtons() {
        int contentStartY = windowY + HEADER_HEIGHT + 20;

        for (BreakthroughButtonData buttonData : buttonDataList) {
            int screenY = contentStartY + buttonData.virtualY - scrollOffset;

            // Create all buttons that could potentially be visible (more generous check)
            if (screenY > windowY && screenY < windowY + windowHeight + OPTION_BUTTON_HEIGHT) {
                EnhancedBreakthroughOptionButton button = new EnhancedBreakthroughOptionButton(
                    windowX + 15, screenY,
                    windowWidth - 30, OPTION_BUTTON_HEIGHT,
                    buttonData.option
                );
                addRenderableWidget(button);
            }
        }
    }

    private boolean isButtonVisibleOnScreen(int buttonY) {
        int contentTop = windowY + HEADER_HEIGHT + 15; // Slightly more generous
        int contentBottom = windowY + windowHeight - 15; // Slightly more generous
        int buttonBottom = buttonY + OPTION_BUTTON_HEIGHT;

        // Button is visible if any part overlaps with content area
        return buttonY <= contentBottom && buttonBottom >= contentTop;
    }

    private void createNoOptionsMessage() {
        String message = determineNoOptionsReason();
        int messageY = windowY + HEADER_HEIGHT + 40 - scrollOffset;

        if (isButtonVisibleOnScreen(messageY)) {
            addRenderableWidget(Button.builder(Component.literal(message), button -> {})
                .bounds(windowX + 20, messageY, windowWidth - 40, 40)
                .build());
        }
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
            .bounds(windowX + 20, windowY + 100, windowWidth - 40, 30)
            .build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTicks);
        renderWindowFrame(guiGraphics);
        renderEnhancedBreakthroughAnalysis(guiGraphics);

        // Set up clipping for scrollable content with generous bounds
        guiGraphics.enableScissor(
            windowX + 10,
            windowY + HEADER_HEIGHT + 15,
            windowX + windowWidth - 10,
            windowY + windowHeight - 15
        );

        renderSectionHeaders(guiGraphics);

        // Render widgets (buttons will be clipped)
        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        guiGraphics.disableScissor();

        // Render scroll indicators
        renderScrollIndicators(guiGraphics);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        // Clean, non-blurry background
        guiGraphics.fill(0, 0, width, height, 0xC0101010);
    }

    private void renderWindowFrame(GuiGraphics guiGraphics) {
        // Main window background
        guiGraphics.fill(windowX, windowY, windowX + windowWidth, windowY + windowHeight, 0xFF1A1A1A);

        // Window border
        guiGraphics.fill(windowX - 2, windowY - 2, windowX + windowWidth + 2, windowY - 1, 0xFF4A4A4A);
        guiGraphics.fill(windowX - 2, windowY + windowHeight + 1, windowX + windowWidth + 2, windowY + windowHeight + 2, 0xFF4A4A4A);
        guiGraphics.fill(windowX - 2, windowY - 1, windowX - 1, windowY + windowHeight + 1, 0xFF4A4A4A);
        guiGraphics.fill(windowX + windowWidth + 1, windowY - 1, windowX + windowWidth + 2, windowY + windowHeight + 1, 0xFF4A4A4A);

        // Title
        Component title = Component.literal("Enhanced Breakthrough Analysis").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        int titleWidth = font.width(title);
        guiGraphics.drawString(font, title, windowX + (windowWidth - titleWidth) / 2, windowY + 15, 0xFFFFFFFF);

        // Separator line
        guiGraphics.fill(windowX + 10, windowY + 35, windowX + windowWidth - 10, windowY + 36, 0xFF4A4A4A);

        // Content area separator
        guiGraphics.fill(windowX + 10, windowY + HEADER_HEIGHT + 10, windowX + windowWidth - 10, windowY + HEADER_HEIGHT + 11, 0xFF333333);
    }

    private void renderEnhancedBreakthroughAnalysis(GuiGraphics guiGraphics) {
        if (cultivationAttachment == null) return;

        int currentY = windowY + 40;

        // Current cultivation status - more compact
        String qiInfo = String.format("Qi: %s (%.1f%%)",
            cultivationAttachment.getCurrentQiRealm().getDisplayName(),
            cultivationAttachment.getQiProgress() * 100);

        String bodyInfo = String.format("Body: %s (%.1f%%)",
            cultivationAttachment.getCurrentBodyRealm().getDisplayName(),
            cultivationAttachment.getBodyProgress() * 100);

        guiGraphics.drawString(font, qiInfo, windowX + 15, currentY, QI_COLOR);
        guiGraphics.drawString(font, bodyInfo, windowX + 250, currentY, BODY_COLOR); // Side by side
        currentY += LINE_HEIGHT + 4;

        // Focus information
        String focusInfo = String.format("Focus: %s (+5%% bonus)",
            cultivationAttachment.getCurrentFocus().getDisplayName());
        guiGraphics.drawString(font, focusInfo, windowX + 15, currentY, 0xFFFFFFFF);
        currentY += LINE_HEIGHT + 4;

        // Debug info about breakthrough options
        PlayerCultivationAttachment.BreakthroughOptions options = cultivationAttachment.getAllAvailableBreakthroughOptions();
        String debugInfo = String.format("Options: Qi(%d) Body(%d)", options.qiOptions().size(), options.bodyOptions().size());
        guiGraphics.drawString(font, debugInfo, windowX + 15, currentY, 0xFF888888);
        currentY += LINE_HEIGHT + 4;

        // Compact breakthrough type info
        String typeInfo = "Types: Minor(50%) • Major(30%) • Special(20%) • Boundary(10%)";
        guiGraphics.drawString(font, typeInfo, windowX + 15, currentY, 0xFFAAAAAA);
        currentY += LINE_HEIGHT + 4;

        // Compact bonuses
        List<String> allBonuses = new ArrayList<>();

        // Attempt bonuses
        long qiAttempts = cultivationAttachment.getQiBreakthroughAttempts();
        long bodyAttempts = cultivationAttachment.getBodyBreakthroughAttempts();
        if (qiAttempts > 0) allBonuses.add(String.format("Qi Fails +%d%%", (int)(qiAttempts * 5)));
        if (bodyAttempts > 0) allBonuses.add(String.format("Body Fails +%d%%", (int)(bodyAttempts * 5)));

        // Talent bonuses
        if (talentAttachment != null) {
            if (talentAttachment.hasSpiritualRootAssigned()) {
                float multiplier = talentAttachment.getSpiritualRootMultiplier();
                double bonus = (multiplier - 1.0) * 20;
                allBonuses.add(String.format("Qi Talent +%.0f%%", bonus));
            }
            if (talentAttachment.hasPhysiqueAssigned()) {
                float multiplier = talentAttachment.getPhysiqueMultiplier();
                double bonus = (multiplier - 1.0) * 20;
                allBonuses.add(String.format("Body Talent +%.0f%%", bonus));
            }
        }

        if (!allBonuses.isEmpty()) {
            String bonusInfo = "Bonuses: " + String.join(" • ", allBonuses);
            guiGraphics.drawString(font, bonusInfo, windowX + 15, currentY, 0xFFADD8E6);
        }
    }

    private void renderSectionHeaders(GuiGraphics guiGraphics) {
        if (cultivationAttachment == null || buttonDataList.isEmpty()) return;

        int contentStartY = windowY + HEADER_HEIGHT + 20;

        // Track which sections we need to render
        boolean hasQiSection = buttonDataList.stream().anyMatch(data -> data.sectionType == PlayerCultivationAttachment.CultivationType.QI);
        boolean hasBodySection = buttonDataList.stream().anyMatch(data -> data.sectionType == PlayerCultivationAttachment.CultivationType.BODY);

        if (hasQiSection) {
            int headerY = contentStartY + 5 - scrollOffset;
            Component qiHeader = Component.literal("▲ QI BREAKTHROUGH OPTIONS ▲").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);
            int qiHeaderWidth = font.width(qiHeader);
            guiGraphics.drawString(font, qiHeader, windowX + (windowWidth - qiHeaderWidth) / 2, headerY, QI_COLOR);
        }

        if (hasBodySection) {
            // Find where body section starts
            int bodyStartY = 0;
            for (BreakthroughButtonData data : buttonDataList) {
                if (data.sectionType == PlayerCultivationAttachment.CultivationType.BODY) {
                    bodyStartY = data.virtualY - 25; // Account for header space
                    break;
                }
            }

            int headerY = contentStartY + bodyStartY + 5 - scrollOffset;
            Component bodyHeader = Component.literal("▲ BODY BREAKTHROUGH OPTIONS ▲").withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
            int bodyHeaderWidth = font.width(bodyHeader);
            guiGraphics.drawString(font, bodyHeader, windowX + (windowWidth - bodyHeaderWidth) / 2, headerY, BODY_COLOR);
        }
    }

    private void renderScrollIndicators(GuiGraphics guiGraphics) {
        if (maxScrollOffset <= 0) return;

        int scrollBarX = windowX + windowWidth - 12;
        int scrollBarTop = windowY + HEADER_HEIGHT + 25;
        int scrollBarHeight = contentAreaHeight - 10;

        // Scroll track
        guiGraphics.fill(scrollBarX, scrollBarTop, scrollBarX + 8, scrollBarTop + scrollBarHeight, 0xFF333333);

        // Scroll thumb
        int thumbHeight = Math.max(20, (int)((double)contentAreaHeight * scrollBarHeight / scrollableContentHeight));
        int thumbY = scrollBarTop + (int)((double)scrollOffset / maxScrollOffset * (scrollBarHeight - thumbHeight));

        guiGraphics.fill(scrollBarX + 1, thumbY, scrollBarX + 7, thumbY + thumbHeight, 0xFF888888);

        // Scroll arrows or indicators
        if (scrollOffset > 0) {
            guiGraphics.drawString(font, "▲", windowX + windowWidth - 15, windowY + HEADER_HEIGHT + 15, 0xFFFFFFFF);
        }
        if (scrollOffset < maxScrollOffset) {
            guiGraphics.drawString(font, "▼", windowX + windowWidth - 15, windowY + windowHeight - 25, 0xFFFFFFFF);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (maxScrollOffset > 0) {
            int oldScrollOffset = scrollOffset;
            scrollOffset = Mth.clamp(scrollOffset - (int)(scrollY * SCROLL_SPEED), 0, maxScrollOffset);

            if (oldScrollOffset != scrollOffset) {
                // Rebuild the interface with new scroll position
                setupBreakthroughInterface();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
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

            // Background with hover state - different colors for available vs unavailable
            int bgColor;
            if (!option.available()) {
                bgColor = hovered ? 0xFF333333 : 0xFF1F1F1F; // Darker for unavailable
            } else {
                bgColor = hovered ? 0xFF444444 : 0xFF2F2F2F; // Normal for available
            }

            guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, bgColor);

            // Border with type-specific color (dimmer if not available)
            int borderColor = option.available() ? getBreakthroughTypeColor() : (getBreakthroughTypeColor() & 0x7F7F7F7F);
            renderBorder(guiGraphics, getX(), getY(), width, height, borderColor, 1);

            // Title with type name
            Component titleText = Component.literal(option.type().getDisplayName().toUpperCase())
                .withStyle(option.available() ? ChatFormatting.WHITE : ChatFormatting.DARK_GRAY);
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
                option.available() ? getCultivationTypeColor() : (getCultivationTypeColor() & 0x7F7F7F7F));

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

                // Show what's needed to unlock
                String requirementText = "Complete cultivation progress to unlock";
                guiGraphics.drawString(font, requirementText,
                    getX() + 8,
                    getY() + 51,
                    0xFF888888);
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
            ICModPacketRegistry.sendToServer(packet);

            ImmortalCultivation.LOGGER.info("Sent {} {} breakthrough request to server",
                option.cultivationType().getDisplayName(), option.type().getDisplayName());

            onClose();

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error sending breakthrough request", e);
        }
    }
}
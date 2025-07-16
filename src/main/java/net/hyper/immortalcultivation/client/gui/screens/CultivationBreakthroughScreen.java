package net.hyper.immortalcultivation.client.gui.screens;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.attachments.PlayerBreakthroughAttachment;
import net.hyper.immortalcultivation.common.attachments.PlayerCultivationAttachment;
import net.hyper.immortalcultivation.common.attachments.PlayerStatsAttachment;
import net.hyper.immortalcultivation.common.attachments.PlayerTalentsAttachment;
import net.hyper.immortalcultivation.common.interfaces.ICultivationData;
import net.hyper.immortalcultivation.common.attachments.AttachmentRegistry;
import net.hyper.immortalcultivation.core.network.NetworkRegistry;
import net.hyper.immortalcultivation.core.network.packets.client.UnifiedActionRequestPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Refactored breakthrough interface using the updated cultivation system.
 *
 * This implementation preserves all existing styling and functionality while
 * utilizing the refactored codebase methods and unified action packet system.
 * Features include:
 * - Responsive design that adapts to screen size
 * - Scrollable content with smooth scrolling  
 * - Detailed success rate breakdowns
 * - Color-coded breakthrough options
 * - Performance optimizations with cached calculations
 * - Enhanced error handling and validation
 *
 * Updated to use:
 * - ICultivationData.CultivationType interface methods
 * - UnifiedActionRequestPacket for network communication
 * - Refactored PlayerBreakthroughAttachment methods
 */
public class CultivationBreakthroughScreen extends Screen {

    // === LAYOUT CONSTANTS (Preserved from original) ===
    private static final int BASE_WINDOW_WIDTH = 480;
    private static final int MIN_WINDOW_HEIGHT = 400;
    private static final int MAX_WINDOW_HEIGHT = 700;
    private static final int HEADER_HEIGHT = 120;
    private static final int SECTION_SPACING = 20;
    private static final int LINE_HEIGHT = 14;
    private static final int OPTION_BUTTON_HEIGHT = 90;
    private static final int SCROLL_SPEED = 15;

    // === COLOR CONSTANTS (Preserved from original) ===
    private static final int QI_COLOR = 0xFF87CEEB;
    private static final int BODY_COLOR = 0xFFFF6347;
    private static final int MINOR_COLOR = 0xFF90EE90;
    private static final int MAJOR_COLOR = 0xFFFFD700;
    private static final int SPECIAL_COLOR = 0xFFDDA0DD;
    private static final int BOUNDARY_COLOR = 0xFFFF6347;
    private static final int SUCCESS_HIGH = 0xFF00FF00;
    private static final int SUCCESS_MED = 0xFFFFFF00;
    private static final int SUCCESS_LOW = 0xFFFF6666;
    private static final int READY_COLOR = 0xFF90EE90;
    private static final int NOT_READY_COLOR = 0xFFFFB6C1;
    private static final int ATTEMPT_COLOR = 0xFFDDA0DD;

    // === INSTANCE FIELDS ===
    private int windowX, windowY, windowWidth, windowHeight;
    private boolean dataCalculated = false;

    // --- Refactored Cultivation Data ---
    @Nullable private CultivationData cultivationData;
    private final List<BreakthroughButtonData> buttonDataList = new ArrayList<>();
    private final ScrollManager scrollManager = new ScrollManager();

    /**
     * Constructor for the refactored breakthrough screen.
     */
    public CultivationBreakthroughScreen() {
        super(Component.literal("Cultivation Breakthrough"));
    }

    // === COMPONENT CLASSES (Using refactored system) ===

    /**
     * Data structure containing player cultivation attachments using refactored system.
     * Encapsulates all required data for breakthrough calculations and reduces attachment lookups.
     */
    private static class CultivationData {
        final PlayerCultivationAttachment cultivation;
        final PlayerStatsAttachment stats;
        final PlayerTalentsAttachment talents;
        final PlayerBreakthroughAttachment breakthrough;

        /**
         * Creates cultivation data container with refactored attachments.
         * @param cultivation The player's cultivation attachment
         * @param stats The player's resource attachment  
         * @param talents The player's talents attachment
         * @param breakthrough The player's breakthrough attachment
         */
        CultivationData(PlayerCultivationAttachment cultivation,
                        PlayerStatsAttachment stats,
                        PlayerTalentsAttachment talents,
                        PlayerBreakthroughAttachment breakthrough) {
            this.cultivation = cultivation;
            this.stats = stats;
            this.talents = talents;
            this.breakthrough = breakthrough;
        }

        /**
         * Validates that all required attachments are present and properly initialized.
         * @return True if all attachments are valid for breakthrough operations
         */
        boolean isValid() {
            return cultivation != null && stats != null &&
                talents != null && breakthrough != null;
        }
    }

    /**
     * Manages scroll state for the breakthrough options list.
     * Preserved from original implementation for consistency.
     */
    private static class ScrollManager {
        private int scrollOffset = 0;
        private int scrollableContentHeight = 0;
        private int maxVisibleHeight = 0;

        /**
         * Updates the maximum visible height based on window dimensions.
         * @param windowHeight The current window height
         */
        void updateMaxVisibleHeight(int windowHeight) {
            this.maxVisibleHeight = windowHeight - HEADER_HEIGHT - 40;
        }

        /**
         * Gets the maximum scroll offset allowed.
         * @return Maximum scroll offset in pixels
         */
        int getMaxScrollOffset() {
            return Math.max(0, scrollableContentHeight - maxVisibleHeight);
        }

        /**
         * Gets the current scroll offset.
         * @return Current scroll offset in pixels
         */
        int getScrollOffset() {
            return scrollOffset;
        }

        /**
         * Checks if scrolling up is possible.
         * @return True if can scroll up
         */
        boolean canScrollUp() {
            return scrollOffset > 0;
        }

        /**
         * Checks if scrolling down is possible.
         * @return True if can scroll down
         */
        boolean canScrollDown() {
            return scrollOffset < getMaxScrollOffset();
        }

        /**
         * Scrolls by the specified amount, clamping to valid range.
         * @param amount The amount to scroll (positive = down, negative = up)
         */
        void scroll(int amount) {
            scrollOffset = Mth.clamp(scrollOffset + amount, 0, getMaxScrollOffset());
        }
    }

    /**
     * Data structure for pre-calculated breakthrough button information.
     * Uses refactored breakthrough option system.
     */
    private static class BreakthroughButtonData {
        final PlayerBreakthroughAttachment.BreakthroughOption option;
        final int virtualY;
        final String displayText;
        final int typeColor;
        final int successColor;
        final boolean isClickable;

        /**
         * Creates button data from a refactored breakthrough option.
         * @param option The breakthrough option from the refactored system
         * @param virtualY The virtual Y position for layout
         * @param cultivationData The cultivation data container
         */
        BreakthroughButtonData(PlayerBreakthroughAttachment.BreakthroughOption option,
                               int virtualY,
                               CultivationData cultivationData) {
            this.option = option;
            this.virtualY = virtualY;
            this.typeColor = getTypeColor(option.type());
            this.successColor = getSuccessColor(option.successChance());
            this.isClickable = option.canAttempt();
            this.displayText = buildDisplayText(option, cultivationData);
        }

        /**
         * Gets the color for the breakthrough type using refactored system.
         * @param type The breakthrough type
         * @return Color value for the type
         */
        private static int getTypeColor(PlayerBreakthroughAttachment.BreakthroughType type) {
            return switch (type) {
                case MINOR -> MINOR_COLOR;
                case MAJOR -> MAJOR_COLOR;
                case SPECIAL -> SPECIAL_COLOR;
                case BOUNDARY -> BOUNDARY_COLOR;
            };
        }

        /**
         * Gets the color for success chance display.
         * @param successChance The success chance as a decimal
         * @return Color value based on success probability
         */
        private static int getSuccessColor(double successChance) {
            if (successChance >= 0.7) return SUCCESS_HIGH;
            if (successChance >= 0.4) return SUCCESS_MED;
            return SUCCESS_LOW;
        }

        /**
         * Builds the display text for the breakthrough option using refactored methods.
         * @param option The breakthrough option
         * @param cultivationData The cultivation data container
         * @return Formatted display text
         */
        private static String buildDisplayText(PlayerBreakthroughAttachment.BreakthroughOption option,
                                               CultivationData cultivationData) {
            StringBuilder text = new StringBuilder();

            // Breakthrough type and cultivation type
            text.append(option.type().getDisplayName())
                .append(" (").append(option.cultivationType().getDisplayName()).append(")\n");

            // Realm transition using refactored realm access
            text.append(option.currentRealm().getDisplayName())
                .append(" → ").append(option.targetRealm().getDisplayName()).append("\n");

            // Success rate with percentage
            text.append(String.format("Success: %.1f%%", option.successChance() * 100)).append("\n");

            // Status and availability using refactored data
            if (!option.canAttempt()) {
                String reason = option.unavailabilityReason();
                text.append("Status: ").append(reason != null ? reason : "Not Available");
            } else {
                text.append("Status: Ready to Attempt");
                if (option.attempts() > 0) {
                    text.append(" (").append(option.attempts()).append(" previous attempts)");
                }
            }

            return text.toString();
        }
    }

    // === INITIALIZATION METHODS ===

    /**
     * Initializes the screen with cultivation data using the refactored system.
     */
    @Override
    protected void init() {
        calculateWindowDimensions();
        loadCultivationData();

        if (cultivationData != null && cultivationData.isValid()) {
            calculateContentRequirements();
            setupBreakthroughInterface();
        } else {
            setupErrorInterface();
        }

        if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
            ImmortalCultivation.LOGGER.debug("RefactoredCultivationBreakthroughScreen initialized - {} options available",
                buttonDataList.size());
        }
    }

    /**
     * Calculates responsive window dimensions.
     * Preserved from original for consistent styling.
     */
    private void calculateWindowDimensions() {
        windowWidth = Math.min(BASE_WINDOW_WIDTH, width - 40);

        int contentHeight = 200; // Base content estimate
        if (minecraft != null && minecraft.player != null) {
            // Estimate content based on available options
            contentHeight = estimateContentHeight();
        }

        windowHeight = Mth.clamp(contentHeight + HEADER_HEIGHT + 40,
            MIN_WINDOW_HEIGHT,
            Math.min(MAX_WINDOW_HEIGHT, height - 40));

        windowX = (width - windowWidth) / 2;
        windowY = (height - windowHeight) / 2;

        scrollManager.updateMaxVisibleHeight(windowHeight);
    }

    /**
     * Loads cultivation data using the refactored attachment system.
     */
    private void loadCultivationData() {
        if (minecraft == null || minecraft.player == null) {
            cultivationData = null;
            return;
        }

        try {
            // Load attachments using refactored system
            PlayerCultivationAttachment cultivation = minecraft.player.getData(AttachmentRegistry.PLAYER_CULTIVATION.get());
            PlayerStatsAttachment stats = minecraft.player.getData(AttachmentRegistry.PLAYER_STATS.get());
            PlayerTalentsAttachment talents = minecraft.player.getData(AttachmentRegistry.PLAYER_TALENTS.get());
            PlayerBreakthroughAttachment breakthrough = minecraft.player.getData(AttachmentRegistry.PLAYER_BREAKTHROUGH.get());

            // Set player references for proper functionality
            cultivation.setPlayer(minecraft.player);
            stats.setPlayer(minecraft.player);
            talents.setPlayer(minecraft.player);
            breakthrough.setPlayer(minecraft.player);

            cultivationData = new CultivationData(cultivation, stats, talents, breakthrough);

            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("Loaded cultivation data for player: {}",
                    minecraft.player.getName().getString());
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Failed to load cultivation data: {}", e.getMessage());
            cultivationData = null;
        }
    }

    /**
     * Estimates content height for responsive layout.
     * @return Estimated content height in pixels
     */
    private int estimateContentHeight() {
        if (cultivationData == null || !cultivationData.isValid()) {
            return 100; // Error message space
        }

        try {
            PlayerBreakthroughAttachment.BreakthroughOptions options =
                cultivationData.breakthrough.getAllAvailableBreakthroughOptions();

            int optionCount = options.qiOptions().size() + options.bodyOptions().size();
            int sectionHeaders = 0;
            if (!options.qiOptions().isEmpty()) sectionHeaders++;
            if (!options.bodyOptions().isEmpty()) sectionHeaders++;

            return (optionCount * (OPTION_BUTTON_HEIGHT + 8)) +
                (sectionHeaders * 30) +
                SECTION_SPACING * 2;
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.warn("Failed to estimate content height: {}", e.getMessage());
            return 200;
        }
    }

    // === CONTENT CALCULATION METHODS ===

    /**
     * Analyzes available breakthrough options and pre-calculates layout information.
     * Updated to use the refactored breakthrough system.
     */
    private void calculateContentRequirements() {
        buttonDataList.clear();

        if (cultivationData == null || !cultivationData.isValid()) {
            scrollManager.scrollableContentHeight = 60; // Error message space
            return;
        }

        try {
            // Get breakthrough options using refactored system
            PlayerBreakthroughAttachment.BreakthroughOptions options =
                cultivationData.breakthrough.getAllAvailableBreakthroughOptions();

            int currentVirtualY = 0;

            // Process Qi options using refactored data
            if (!options.qiOptions().isEmpty()) {
                currentVirtualY += 30; // Section header space

                for (PlayerBreakthroughAttachment.BreakthroughOption option : options.qiOptions()) {
                    buttonDataList.add(new BreakthroughButtonData(option, currentVirtualY, cultivationData));
                    currentVirtualY += OPTION_BUTTON_HEIGHT + 8;
                }
                currentVirtualY += SECTION_SPACING;
            }

            // Process Body options using refactored data
            if (!options.bodyOptions().isEmpty()) {
                currentVirtualY += 30; // Section header space

                for (PlayerBreakthroughAttachment.BreakthroughOption option : options.bodyOptions()) {
                    buttonDataList.add(new BreakthroughButtonData(option, currentVirtualY, cultivationData));
                    currentVirtualY += OPTION_BUTTON_HEIGHT + 8;
                }
            }

            // Handle empty content
            if (buttonDataList.isEmpty()) {
                currentVirtualY = Math.max(60, currentVirtualY);
            }

            scrollManager.scrollableContentHeight = currentVirtualY;
            dataCalculated = true;

            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("Calculated content requirements - {} buttons, {} height",
                    buttonDataList.size(), currentVirtualY);
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Failed to calculate content requirements: {}", e.getMessage());
            scrollManager.scrollableContentHeight = 60;
            dataCalculated = false;
        }
    }

    /**
     * Sets up the breakthrough interface with all buttons and widgets.
     * Updated to use refactored action system.
     */
    private void setupBreakthroughInterface() {
        clearWidgets();

        // Close button (always visible, preserved styling)
        addRenderableWidget(Button.builder(Component.literal("Close"), button -> onClose())
            .bounds(windowX + windowWidth - 70, windowY + 10, 60, 20)
            .build());

        // Create breakthrough buttons or show no options message
        if (cultivationData != null && cultivationData.isValid()) {
            createVisibleBreakthroughButtons();

            if (buttonDataList.isEmpty()) {
                // No additional widgets needed - message shown in render
            }
        }
    }

    /**
     * Sets up error interface when cultivation data is unavailable.
     */
    private void setupErrorInterface() {
        clearWidgets();

        // Close button (preserved styling)
        addRenderableWidget(Button.builder(Component.literal("Close"), button -> onClose())
            .bounds(windowX + windowWidth - 70, windowY + 10, 60, 20)
            .build());

        // Retry button for error state
        addRenderableWidget(Button.builder(Component.literal("Retry"), button -> {
                loadCultivationData();
                init(); // Re-initialize
            }).bounds(windowX + windowWidth - 140, windowY + 10, 60, 20)
            .build());
    }

    /**
     * Creates visible breakthrough buttons within the scrollable area.
     * Uses refactored action packet system for button functionality.
     */
    private void createVisibleBreakthroughButtons() {
        int contentStartY = windowY + HEADER_HEIGHT + 20;
        int scrollOffset = scrollManager.getScrollOffset();
        int contentAreaHeight = windowHeight - HEADER_HEIGHT - 40;

        for (BreakthroughButtonData data : buttonDataList) {
            int buttonY = contentStartY + data.virtualY - scrollOffset;

            // Only create buttons that are at least partially visible
            if (buttonY + OPTION_BUTTON_HEIGHT >= contentStartY &&
                buttonY <= contentStartY + contentAreaHeight) {

                createBreakthroughButton(data, buttonY);
            }
        }
    }

    /**
     * Creates a single breakthrough button with proper styling and refactored action handling.
     * @param data The pre-calculated button data containing all display information
     * @param buttonY The Y position where the button should be rendered
     */
    private void createBreakthroughButton(BreakthroughButtonData data, int buttonY) {
        int buttonX = windowX + 15;
        int buttonWidth = windowWidth - 30;

        // Extract first line for button text (preserved styling)
        String buttonText = data.displayText.split("\n")[0];

        Button button = Button.builder(
                Component.literal(buttonText),
                btn -> attemptBreakthroughRefactored(data.option) // Updated action method
            )
            .bounds(buttonX, buttonY, buttonWidth, OPTION_BUTTON_HEIGHT)
            .build();

        // Enable/disable based on availability (preserved logic)
        button.active = data.isClickable;

        addRenderableWidget(button);
    }

    // === INPUT HANDLING ===

    /**
     * Handles mouse scrolling with preserved scroll behavior.
     * @param mouseX Mouse X coordinate
     * @param mouseY Mouse Y coordinate
     * @param scrollY Scroll delta
     * @return True if scroll was handled
     */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollManager.getMaxScrollOffset() > 0) {
            scrollManager.scroll((int) (-scrollY * SCROLL_SPEED));

            // Recreate visible buttons after scrolling
            if (cultivationData != null && cultivationData.isValid()) {
                clearWidgets();
                setupBreakthroughInterface();
            }

            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    /**
     * Handles keyboard input for scrolling with preserved key bindings.
     * @param keyCode The key code
     * @param scanCode The scan code  
     * @param modifiers Key modifiers
     * @return True if key was handled
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Arrow key scrolling (preserved behavior)
        if (keyCode == 265) { // UP arrow
            scrollManager.scroll(-SCROLL_SPEED);
            if (cultivationData != null && cultivationData.isValid()) {
                clearWidgets();
                setupBreakthroughInterface();
            }
            return true;
        } else if (keyCode == 264) { // DOWN arrow
            scrollManager.scroll(SCROLL_SPEED);
            if (cultivationData != null && cultivationData.isValid()) {
                clearWidgets();
                setupBreakthroughInterface();
            }
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // === ACTION METHODS ===

    /**
     * Attempts a breakthrough using the refactored action system.
     * Updated to use UnifiedActionRequestPacket instead of direct method calls.
     * @param option The breakthrough option to attempt
     */
    private void attemptBreakthroughRefactored(PlayerBreakthroughAttachment.BreakthroughOption option) {
        if (minecraft == null || minecraft.player == null) {
            ImmortalCultivation.LOGGER.error("Cannot attempt breakthrough - no player reference");
            return;
        }

        try {
            // Create breakthrough request using refactored packet system
            UnifiedActionRequestPacket packet = UnifiedActionRequestPacket.createBreakthroughRequest(
                option.cultivationType(), option.type());

            // Send packet to server using refactored network system
            NetworkRegistry.sendToServer(packet);

            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("Sent breakthrough request - Type: {}, Cultivation: {}",
                    option.type(), option.cultivationType());
            }

            // Close screen after attempt (preserved behavior)
            onClose();

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Failed to send breakthrough request: {}", e.getMessage());

            // Show error message to player (preserved error handling)
            if (minecraft.player != null) {
                minecraft.player.sendSystemMessage(Component.literal("Failed to send breakthrough request").withStyle(ChatFormatting.RED));
            }
        }
    }

    // === RENDERING METHODS ===

    /**
     * Main render method with preserved visual styling.
     * @param guiGraphics The graphics context for rendering
     * @param mouseX Mouse X coordinate
     * @param mouseY Mouse Y coordinate
     * @param partialTick Partial tick for smooth animation
     */
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Render background (preserved styling)
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        // Render window frame with preserved styling
        renderWindowFrame(guiGraphics);

        // Render content based on data availability
        if (cultivationData != null && cultivationData.isValid()) {
            renderEnhancedBreakthroughAnalysis(guiGraphics);
            renderBreakthroughContent(guiGraphics);
            renderScrollIndicators(guiGraphics);
        } else {
            renderErrorMessage(guiGraphics);
        }

        // Render widgets (buttons) on top
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    /**
     * Renders the window frame with preserved styling from original.
     * @param guiGraphics The graphics context for rendering
     */
    private void renderWindowFrame(GuiGraphics guiGraphics) {
        // Main window background (preserved colors)
        guiGraphics.fill(windowX, windowY, windowX + windowWidth, windowY + windowHeight, 0xFF1A1A1A);

        // Window border (preserved styling)
        renderBorder(guiGraphics, windowX - 2, windowY - 2, windowWidth + 4, windowHeight + 4, 0xFF4A4A4A);

        // Title with preserved formatting
        Component title = Component.literal("Enhanced Breakthrough Analysis")
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        int titleWidth = font.width(title);
        guiGraphics.drawString(font, title, windowX + (windowWidth - titleWidth) / 2, windowY + 15, 0xFFFFFFFF);

        // Separator lines (preserved styling)
        guiGraphics.fill(windowX + 10, windowY + 35, windowX + windowWidth - 10, windowY + 36, 0xFF4A4A4A);
        guiGraphics.fill(windowX + 10, windowY + HEADER_HEIGHT + 10,
            windowX + windowWidth - 10, windowY + HEADER_HEIGHT + 11, 0xFF4A4A4A);
    }

    /**
     * Renders a border around the specified area with preserved styling.
     * @param guiGraphics The graphics context
     * @param x The X position
     * @param y The Y position
     * @param width The border width
     * @param height The border height
     * @param color The border color
     */
    private void renderBorder(GuiGraphics guiGraphics, int x, int y, int width, int height, int color) {
        // Top and bottom
        guiGraphics.fill(x, y, x + width, y + 1, color);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, color);

        // Left and right
        guiGraphics.fill(x, y, x + 1, y + height, color);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    /**
     * Renders the enhanced breakthrough analysis header using refactored cultivation data.
     * Preserved styling with updated data access methods.
     * @param guiGraphics The graphics context for rendering
     */
    private void renderEnhancedBreakthroughAnalysis(GuiGraphics guiGraphics) {
        if (cultivationData == null || !cultivationData.isValid()) {
            renderErrorMessage(guiGraphics);
            return;
        }

        int currentY = windowY + 45;

        // Current focus and realms using refactored system
        renderFocusAndRealms(guiGraphics, currentY);
        currentY += LINE_HEIGHT * 2;

        // Breakthrough statistics using refactored data
        renderBreakthroughStatistics(guiGraphics, currentY);
        currentY += LINE_HEIGHT;

        // Bonus information using refactored talent system
        renderBonusInformation(guiGraphics, currentY);
    }

    /**
     * Renders current cultivation focus and realm information using refactored data access.
     * @param guiGraphics The graphics context for rendering
     * @param startY The starting Y position for rendering
     */
    private void renderFocusAndRealms(GuiGraphics guiGraphics, int startY) {
        PlayerCultivationAttachment cultivation = cultivationData.cultivation;

        // Focus display using refactored getCurrentFocus method
        PlayerCultivationAttachment.CultivationType focus = cultivation.getCurrentFocus();
        String focusText = String.format("Current Focus: %s (2x rate)", focus.getDisplayName());
        int focusColor = focus == PlayerCultivationAttachment.CultivationType.QI ? QI_COLOR : BODY_COLOR;
        guiGraphics.drawString(font, focusText, windowX + 15, startY, focusColor);

        // Realm information using refactored getRealm method with ICultivationData interface
        String realmsText = String.format("Realms: Qi %s | Body %s",
            cultivation.getRealm(ICultivationData.CultivationType.QI).getDisplayName(),
            cultivation.getRealm(ICultivationData.CultivationType.BODY).getDisplayName());
        guiGraphics.drawString(font, realmsText, windowX + 15, startY + LINE_HEIGHT, 0xFFFFFFFF);
    }

    /**
     * Renders breakthrough attempt statistics using refactored breakthrough data.
     * @param guiGraphics The graphics context for rendering
     * @param startY The starting Y position for rendering
     */
    private void renderBreakthroughStatistics(GuiGraphics guiGraphics, int startY) {
        PlayerBreakthroughAttachment breakthrough = cultivationData.breakthrough;

        // Attempt counts using refactored methods
        long qiAttempts = breakthrough.getBreakthroughAttempts(PlayerCultivationAttachment.CultivationType.QI);
        long bodyAttempts = breakthrough.getBreakthroughAttempts(PlayerCultivationAttachment.CultivationType.BODY);

        String attemptsText = String.format("Attempts: Qi %d | Body %d", qiAttempts, bodyAttempts);
        guiGraphics.drawString(font, attemptsText, windowX + 15, startY, 0xFFCCCCCC);

        // Cooldown status using refactored cooldown methods
        boolean qiCooldown = breakthrough.isBreakthroughOnCooldown(PlayerCultivationAttachment.CultivationType.QI);
        boolean bodyCooldown = breakthrough.isBreakthroughOnCooldown(PlayerCultivationAttachment.CultivationType.BODY);

        if (qiCooldown || bodyCooldown) {
            String cooldownText = "Cooldown: " +
                (qiCooldown ? "Qi " : "") +
                (bodyCooldown ? "Body" : "");
            guiGraphics.drawString(font, cooldownText.trim(), windowX + 200, startY, 0xFFFF6666);
        }
    }

    /**
     * Renders bonus information from talents using refactored talent system.
     * @param guiGraphics The graphics context for rendering
     * @param startY The starting Y position for rendering
     */
    private void renderBonusInformation(GuiGraphics guiGraphics, int startY) {
        PlayerTalentsAttachment talents = cultivationData.talents;

        // Talent bonuses using refactored methods
        double spiritualBonus = talents.getSpiritualRootMultiplier();
        double physiqueBonus = talents.getPhysiqueMultiplier();

        String bonusText = String.format("Talent Bonuses: Spiritual %.1fx | Physique %.1fx",
            spiritualBonus, physiqueBonus);
        guiGraphics.drawString(font, bonusText, windowX + 15, startY, 0xFFDDDDDD);
    }

    /**
     * Renders the main breakthrough content area with preserved styling.
     * @param guiGraphics The graphics context for rendering
     */
    private void renderBreakthroughContent(GuiGraphics guiGraphics) {
        int contentStartY = windowY + HEADER_HEIGHT + 20;
        int scrollOffset = scrollManager.getScrollOffset();
        int contentAreaHeight = windowHeight - HEADER_HEIGHT - 40;

        // Render section headers and button details that are visible
        renderVisibleSectionHeaders(guiGraphics, contentStartY, scrollOffset, contentAreaHeight);
        renderVisibleButtonDetails(guiGraphics, contentStartY, scrollOffset, contentAreaHeight);

        // Show message if no options available
        if (buttonDataList.isEmpty()) {
            renderNoOptionsMessage(guiGraphics, contentStartY);
        }
    }

    /**
     * Renders section headers for Qi and Body cultivation options.
     * @param guiGraphics The graphics context for rendering
     * @param contentStartY The starting Y position of content area
     * @param scrollOffset Current scroll offset
     * @param contentAreaHeight Height of visible content area
     */
    private void renderVisibleSectionHeaders(GuiGraphics guiGraphics, int contentStartY, int scrollOffset, int contentAreaHeight) {
        if (cultivationData == null || !cultivationData.isValid()) return;

        try {
            PlayerBreakthroughAttachment.BreakthroughOptions options =
                cultivationData.breakthrough.getAllAvailableBreakthroughOptions();

            int currentVirtualY = 0;

            // Qi section header
            if (!options.qiOptions().isEmpty()) {
                int headerY = contentStartY + currentVirtualY - scrollOffset;
                if (headerY >= contentStartY - 20 && headerY <= contentStartY + contentAreaHeight) {
                    Component qiHeader = Component.literal("Qi Cultivation Breakthroughs")
                        .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);
                    guiGraphics.drawString(font, qiHeader, windowX + 15, headerY, QI_COLOR);
                }
                currentVirtualY += 30 + (options.qiOptions().size() * (OPTION_BUTTON_HEIGHT + 8)) + SECTION_SPACING;
            }

            // Body section header
            if (!options.bodyOptions().isEmpty()) {
                int headerY = contentStartY + currentVirtualY - scrollOffset;
                if (headerY >= contentStartY - 20 && headerY <= contentStartY + contentAreaHeight) {
                    Component bodyHeader = Component.literal("Body Cultivation Breakthroughs")
                        .withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
                    guiGraphics.drawString(font, bodyHeader, windowX + 15, headerY, BODY_COLOR);
                }
            }
        } catch (Exception e) {
            ImmortalCultivation.LOGGER.warn("Failed to render section headers: {}", e.getMessage());
        }
    }

    /**
     * Renders detailed information for visible breakthrough buttons.
     * @param guiGraphics The graphics context for rendering
     * @param contentStartY The starting Y position of content area
     * @param scrollOffset Current scroll offset
     * @param contentAreaHeight Height of visible content area
     */
    private void renderVisibleButtonDetails(GuiGraphics guiGraphics, int contentStartY, int scrollOffset, int contentAreaHeight) {
        for (BreakthroughButtonData data : buttonDataList) {
            int buttonY = contentStartY + data.virtualY - scrollOffset;

            // Only render details for visible buttons
            if (buttonY + OPTION_BUTTON_HEIGHT >= contentStartY &&
                buttonY <= contentStartY + contentAreaHeight) {

                renderButtonDetails(guiGraphics, data, buttonY);
            }
        }
    }

    /**
     * Renders detailed information for a single breakthrough button with preserved styling.
     * @param guiGraphics The graphics context for rendering
     * @param data The button data containing display information
     * @param buttonY The Y position of the button
     */
    private void renderButtonDetails(GuiGraphics guiGraphics, BreakthroughButtonData data, int buttonY) {
        int textX = windowX + 25;
        int currentY = buttonY + 20;

        // Render each line of the display text with preserved color scheme
        String[] lines = data.displayText.split("\n");
        for (int i = 0; i < lines.length; i++) {
            int color = switch (i) {
                case 0 -> data.typeColor; // Breakthrough type
                case 1 -> 0xFFFFFFFF;   // Realm transition
                case 2 -> data.successColor; // Success rate
                case 3 -> data.isClickable ? READY_COLOR : NOT_READY_COLOR; // Status
                default -> 0xFFCCCCCC;
            };

            guiGraphics.drawString(font, lines[i], textX, currentY, color);
            currentY += 12;
        }
    }

    /**
     * Renders scroll indicators when content exceeds visible area.
     * Preserved from original implementation.
     * @param guiGraphics The graphics context for rendering
     */
    private void renderScrollIndicators(GuiGraphics guiGraphics) {
        if (scrollManager.getMaxScrollOffset() <= 0) return;

        int indicatorX = windowX + windowWidth - 15;
        int contentStartY = windowY + HEADER_HEIGHT + 20;
        int contentEndY = windowY + windowHeight - 20;

        // Up arrow
        if (scrollManager.canScrollUp()) {
            guiGraphics.drawString(font, "↑", indicatorX, contentStartY, 0xFFFFFFFF);
        }

        // Down arrow
        if (scrollManager.canScrollDown()) {
            guiGraphics.drawString(font, "↓", indicatorX, contentEndY - 10, 0xFFFFFFFF);
        }

        // Scroll position indicator (preserved styling)
        if (scrollManager.getMaxScrollOffset() > 0) {
            int barHeight = contentEndY - contentStartY - 40;
            int barY = contentStartY + 20;
            float scrollPercent = (float) scrollManager.getScrollOffset() / scrollManager.getMaxScrollOffset();
            int thumbY = barY + (int) (scrollPercent * (barHeight - 10));

            // Scroll track
            guiGraphics.fill(indicatorX + 8, barY, indicatorX + 10, barY + barHeight, 0xFF444444);
            // Scroll thumb
            guiGraphics.fill(indicatorX + 8, thumbY, indicatorX + 10, thumbY + 10, 0xFFAAAAA);
        }
    }

    /**
     * Renders error message when cultivation data is unavailable.
     * @param guiGraphics The graphics context for rendering
     */
    private void renderErrorMessage(GuiGraphics guiGraphics) {
        int currentY = windowY + 50;
        String errorText = "Cultivation system unavailable - please try again";
        guiGraphics.drawString(font, errorText, windowX + 15, currentY, 0xFFFF6666);
    }

    /**
     * Renders message when no breakthrough options are available.
     * @param guiGraphics The graphics context for rendering
     * @param contentStartY The starting Y position of content area
     */
    private void renderNoOptionsMessage(GuiGraphics guiGraphics, int contentStartY) {
        Component message = Component.literal("No breakthrough options currently available")
            .withStyle(ChatFormatting.GRAY);
        int messageY = contentStartY + 50;
        guiGraphics.drawString(font, message, windowX + 15, messageY, 0xFF888888);
    }

    // === UTILITY METHODS ===

    /**
     * Refreshes the screen data and rebuilds the interface.
     * Useful for responding to cultivation data updates.
     */
    public void refreshData() {
        loadCultivationData();
        if (cultivationData != null && cultivationData.isValid()) {
            calculateContentRequirements();
            setupBreakthroughInterface();
        } else {
            setupErrorInterface();
        }

        if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
            ImmortalCultivation.LOGGER.debug("RefactoredCultivationBreakthroughScreen data refreshed");
        }
    }

    /**
     * Gets the current cultivation data container.
     * @return The cultivation data, or null if unavailable
     */
    @Nullable
    public CultivationData getCultivationData() {
        return cultivationData;
    }

    /**
     * Checks if the screen has valid cultivation data.
     * @return True if cultivation data is available and valid
     */
    public boolean hasValidData() {
        return cultivationData != null && cultivationData.isValid();
    }

    /**
     * Gets the number of available breakthrough options.
     * @return The total number of breakthrough options
     */
    public int getAvailableOptionCount() {
        return buttonDataList.size();
    }

    /**
     * Checks if the screen content is scrollable.
     * @return True if content exceeds visible area
     */
    public boolean isScrollable() {
        return scrollManager.getMaxScrollOffset() > 0;
    }

    /**
     * Gets debug information about the current screen state.
     * @return Debug information as a formatted string
     */
    public String getDebugInfo() {
        StringBuilder info = new StringBuilder();
        info.append("RefactoredCultivationBreakthroughScreen Debug Info:\n");
        info.append("- Valid Data: ").append(hasValidData()).append("\n");
        info.append("- Option Count: ").append(getAvailableOptionCount()).append("\n");
        info.append("- Scrollable: ").append(isScrollable()).append("\n");
        info.append("- Window Size: ").append(windowWidth).append("x").append(windowHeight).append("\n");
        info.append("- Scroll Offset: ").append(scrollManager.getScrollOffset()).append("/").append(scrollManager.getMaxScrollOffset()).append("\n");

        if (hasValidData()) {
            try {
                PlayerBreakthroughAttachment.BreakthroughOptions options =
                    cultivationData.breakthrough.getAllAvailableBreakthroughOptions();
                info.append("- Qi Options: ").append(options.qiOptions().size()).append("\n");
                info.append("- Body Options: ").append(options.bodyOptions().size()).append("\n");
            } catch (Exception e) {
                info.append("- Options: Error retrieving (").append(e.getMessage()).append(")\n");
            }
        }

        return info.toString();
    }

    // === SCREEN LIFECYCLE ===

    /**
     * Called when the screen is resized.
     * Recalculates layout and rebuilds interface.
     * @param minecraft The minecraft instance
     * @param width New screen width
     * @param height New screen height
     */
    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        super.resize(minecraft, width, height);

        // Recalculate window dimensions for new screen size
        calculateWindowDimensions();

        // Rebuild interface with new dimensions
        if (cultivationData != null && cultivationData.isValid()) {
            calculateContentRequirements();
            setupBreakthroughInterface();
        } else {
            setupErrorInterface();
        }
    }

    /**
     * Called when the screen is closing.
     * Performs cleanup and logging.
     */
    @Override
    public void onClose() {
        if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
            ImmortalCultivation.LOGGER.debug("RefactoredCultivationBreakthroughScreen closing - {} options were available",
                getAvailableOptionCount());
        }

        super.onClose();
    }

    /**
     * Checks if the screen should pause the game when opened.
     * @return False to allow game to continue running
     */
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
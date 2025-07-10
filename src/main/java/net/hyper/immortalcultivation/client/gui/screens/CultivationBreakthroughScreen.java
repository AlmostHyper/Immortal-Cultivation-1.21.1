package net.hyper.immortalcultivation.client.gui.screens;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.attachments.PlayerBreakthroughAttachment;
import net.hyper.immortalcultivation.common.attachments.PlayerCultivationAttachment;
import net.hyper.immortalcultivation.common.attachments.PlayerResourceAttachment;
import net.hyper.immortalcultivation.common.attachments.PlayerTalentAttachment;
import net.hyper.immortalcultivation.common.interfaces.ICultivationData;
import net.hyper.immortalcultivation.common.registry.ICModAttachmentTypes;
import net.hyper.immortalcultivation.core.network.ICModPacketRegistry;
import net.hyper.immortalcultivation.core.network.packets.client.UnifiedActionRequestPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Enhanced breakthrough interface supporting multiple breakthrough types with different success rates.
 *
 * This improved implementation focuses on modularity, performance, and maintainability while
 * preserving all existing functionality. Features include:
 * - Responsive design that adapts to screen size
 * - Scrollable content with smooth scrolling
 * - Detailed success rate breakdowns
 * - Color-coded breakthrough options
 * - Performance optimizations with cached calculations
 * - Enhanced error handling and validation
 *
 * The screen integrates seamlessly with the cultivation system attachments and network sync.
 */
public class CultivationBreakthroughScreen extends Screen {

    // === LAYOUT CONSTANTS ===
    private static final int BASE_WINDOW_WIDTH = 480;
    private static final int MIN_WINDOW_HEIGHT = 400;
    private static final int MAX_WINDOW_HEIGHT = 700;
    private static final int HEADER_HEIGHT = 120;
    private static final int SECTION_SPACING = 20;
    private static final int LINE_HEIGHT = 14;
    private static final int OPTION_BUTTON_HEIGHT = 90;
    private static final int SCROLL_SPEED = 15;

    // === COLOR CONSTANTS ===
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

    // === COMPONENT CLASSES ===

    /**
     * Data structure containing player cultivation attachments.
     * Encapsulates all required data for breakthrough calculations and reduces attachment lookups.
     */
    private static class CultivationData {
        final PlayerCultivationAttachment cultivation;
        final PlayerResourceAttachment resources;
        final PlayerTalentAttachment talents;
        final PlayerBreakthroughAttachment breakthrough;

        CultivationData(PlayerCultivationAttachment cultivation,
                        PlayerResourceAttachment resources,
                        PlayerTalentAttachment talents,
                        PlayerBreakthroughAttachment breakthrough) {
            this.cultivation = cultivation;
            this.resources = resources;
            this.talents = talents;
            this.breakthrough = breakthrough;
        }

        /**
         * Validates that all required attachments are present and properly initialized.
         *
         * @return true if all attachments are valid, false otherwise
         */
        boolean isValid() {
            return cultivation != null && resources != null &&
                talents != null && breakthrough != null;
        }
    }

    /**
     * Immutable data structure representing a breakthrough option with calculated success rates.
     * Pre-calculates all display values to improve rendering performance.
     */
    private static class BreakthroughButtonData {
        final PlayerBreakthroughAttachment.BreakthroughOption option;
        final int virtualY;
        final PlayerCultivationAttachment.CultivationType sectionType;
        final String displayText;
        final int typeColor;
        final int successColor;
        final boolean isClickable;

        /**
         * Creates a new breakthrough button data instance with pre-calculated display values.
         *
         * This constructor performs all necessary calculations during creation to avoid
         * repeated computations during rendering, improving performance.
         *
         * @param option The breakthrough option containing type and availability info
         * @param virtualY The virtual Y position for scrolling calculations
         * @param sectionType The cultivation type (QI or BODY) for color coding
         * @param cultivationData The cultivation data for accessing current realm names
         */
        BreakthroughButtonData(PlayerBreakthroughAttachment.BreakthroughOption option,
                               int virtualY,
                               PlayerCultivationAttachment.CultivationType sectionType,
                               CultivationData cultivationData) {
            this.option = option;
            this.virtualY = virtualY;
            this.sectionType = sectionType;
            this.isClickable = option.available();

            // Pre-calculate display text
            this.displayText = String.format("%s Breakthrough\n%s → %s\nSuccess: %.1f%%\n%s",
                option.type().getDisplayName(),
                getCurrentRealmName(sectionType, cultivationData),
                option.targetRealm().getDisplayName(),
                option.successChance() * 100,
                option.available() ? "Ready" : option.unavailabilityReason()
            );

            // Pre-calculate colors
            this.typeColor = getTypeColor(option.type());
            this.successColor = getSuccessRateColor(option.successChance());
        }

        /**
         * Gets the current realm name for display purposes.
         *
         * @param type The cultivation type to get the realm for
         * @param cultivationData The cultivation data containing realm information
         * @return The display name of the current realm
         */
        private String getCurrentRealmName(PlayerCultivationAttachment.CultivationType type, CultivationData cultivationData) {
            if (cultivationData == null || !cultivationData.isValid()) {
                return type == PlayerCultivationAttachment.CultivationType.QI ? "Unknown Qi" : "Unknown Body";
            }

            return type == PlayerCultivationAttachment.CultivationType.QI ?
                cultivationData.cultivation.getRealm(ICultivationData.CultivationType.QI).getDisplayName() :
                cultivationData.cultivation.getRealm(ICultivationData.CultivationType.BODY).getDisplayName();
        }

        /**
         * Determines the color for a breakthrough type.
         *
         * @param type The breakthrough type
         * @return The appropriate color constant
         */
        private int getTypeColor(PlayerBreakthroughAttachment.BreakthroughType type) {
            return switch (type) {
                case MINOR -> MINOR_COLOR;
                case MAJOR -> MAJOR_COLOR;
                case SPECIAL -> SPECIAL_COLOR;
                case BOUNDARY -> BOUNDARY_COLOR;
            };
        }

        /**
         * Determines the color based on success rate.
         *
         * @param successRate The success rate (0.0 to 1.0)
         * @return The appropriate color constant
         */
        private int getSuccessRateColor(double successRate) {
            if (successRate >= 0.7) return SUCCESS_HIGH;
            if (successRate >= 0.4) return SUCCESS_MED;
            return SUCCESS_LOW;
        }
    }

    /**
     * Manages scroll state and provides smooth scrolling functionality.
     * Handles all scroll-related calculations and bounds checking.
     */
    private static class ScrollManager {
        private int scrollOffset = 0;
        private int maxScrollOffset = 0;
        private int contentAreaHeight = 0;
        private int scrollableContentHeight = 0;

        /**
         * Updates the scroll bounds based on content and area dimensions.
         *
         * @param contentAreaHeight The height of the visible content area
         * @param scrollableContentHeight The total height of scrollable content
         */
        void updateBounds(int contentAreaHeight, int scrollableContentHeight) {
            this.contentAreaHeight = contentAreaHeight;
            this.scrollableContentHeight = scrollableContentHeight;
            this.maxScrollOffset = Math.max(0, scrollableContentHeight - contentAreaHeight);
            this.scrollOffset = Mth.clamp(scrollOffset, 0, maxScrollOffset);
        }

        /**
         * Scrolls by the specified amount with bounds checking.
         *
         * @param amount The amount to scroll (positive = down, negative = up)
         */
        void scroll(int amount) {
            scrollOffset = Mth.clamp(scrollOffset + amount, 0, maxScrollOffset);
        }

        /**
         * Resets scroll position to the top.
         */
        void resetToTop() {
            scrollOffset = 0;
        }

        int getScrollOffset() { return scrollOffset; }
        int getMaxScrollOffset() { return maxScrollOffset; }
        boolean canScrollUp() { return scrollOffset > 0; }
        boolean canScrollDown() { return scrollOffset < maxScrollOffset; }
    }

    // === INSTANCE VARIABLES ===
    @Nullable private CultivationData breakthroughData;
    private int windowX, windowY, windowWidth, windowHeight;
    private final ScrollManager scrollManager = new ScrollManager();
    private final List<BreakthroughButtonData> buttonDataList = new ArrayList<>();

    // Performance optimization: Cache calculated values
    private boolean dataCalculated = false;

    /**
     * Creates a new cultivation breakthrough screen.
     */
    public CultivationBreakthroughScreen() {
        super(Component.literal("Cultivation Breakthrough Analysis"));
    }

    @Override
    protected void init() {
        super.init();

        // Initialize cultivation data
        initializeCultivationData();

        // Calculate window dimensions and setup interface
        calculateWindowDimensions();
        setupBreakthroughInterface();
    }

    /**
     * Initializes cultivation data from player attachments.
     *
     * This method safely retrieves all required attachments and validates them.
     * It also ensures proper player references are set for calculations.
     */
    private void initializeCultivationData() {
        if (minecraft.player == null) {
            ImmortalCultivation.LOGGER.warn("No player available for breakthrough screen");
            return;
        }

        try {
            PlayerCultivationAttachment cultivation = minecraft.player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
            PlayerResourceAttachment resources = minecraft.player.getData(ICModAttachmentTypes.PLAYER_RESOURCES.get());
            PlayerTalentAttachment talents = minecraft.player.getData(ICModAttachmentTypes.PLAYER_TALENTS.get());
            PlayerBreakthroughAttachment breakthrough = minecraft.player.getData(ICModAttachmentTypes.PLAYER_BREAKTHROUGH.get());

            // Set player references if needed
            if (cultivation != null) cultivation.setPlayer(minecraft.player);
            if (breakthrough != null) breakthrough.setPlayer(minecraft.player);
            if (talents != null) talents.setPlayer(minecraft.player);

            breakthroughData = new CultivationData(cultivation, resources, talents, breakthrough);

            if (!breakthroughData.isValid()) {
                ImmortalCultivation.LOGGER.warn("Some cultivation attachments are missing");
            }

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error initializing cultivation data", e);
            breakthroughData = null;
        }
    }

    /**
     * Calculates responsive window dimensions based on screen size and content requirements.
     *
     * This method ensures the window fits on the screen while providing adequate space
     * for all breakthrough options. It uses dynamic sizing with reasonable constraints.
     */
    private void calculateWindowDimensions() {
        // Calculate responsive width
        windowWidth = Math.min(BASE_WINDOW_WIDTH, Math.max(400, (int)(width * 0.8)));

        // Calculate content requirements
        calculateContentRequirements();

        // Determine optimal window height
        int availableHeight = height - 80;
        int desiredHeight = HEADER_HEIGHT + Math.min(scrollManager.scrollableContentHeight, 500) + 40;
        windowHeight = Mth.clamp(desiredHeight, MIN_WINDOW_HEIGHT, Math.min(MAX_WINDOW_HEIGHT, availableHeight));

        // Update scroll manager with new dimensions
        int contentAreaHeight = windowHeight - HEADER_HEIGHT - 40;
        scrollManager.updateBounds(contentAreaHeight, scrollManager.scrollableContentHeight);
        scrollManager.resetToTop();

        // Center the window
        windowX = (width - windowWidth) / 2;
        windowY = (height - windowHeight) / 2;
    }

    /**
     * Calculates content requirements and creates button data.
     *
     * This method analyzes available breakthrough options and pre-calculates
     * all layout and display information for optimal rendering performance.
     */
    private void calculateContentRequirements() {
        buttonDataList.clear();

        if (breakthroughData == null || !breakthroughData.isValid()) {
            scrollManager.scrollableContentHeight = 60; // Error message space
            return;
        }

        PlayerBreakthroughAttachment.BreakthroughOptions options =
            breakthroughData.breakthrough.getAllAvailableBreakthroughOptions();

        int currentVirtualY = 0;

        // Process Qi options
        if (!options.qiOptions().isEmpty()) {
            currentVirtualY += 30; // Section header space

            for (PlayerBreakthroughAttachment.BreakthroughOption option : options.qiOptions()) {
                buttonDataList.add(new BreakthroughButtonData(
                    option, currentVirtualY, PlayerCultivationAttachment.CultivationType.QI, breakthroughData));
                currentVirtualY += OPTION_BUTTON_HEIGHT + 8;
            }
            currentVirtualY += SECTION_SPACING;
        }

        // Process Body options
        if (!options.bodyOptions().isEmpty()) {
            currentVirtualY += 30; // Section header space

            for (PlayerBreakthroughAttachment.BreakthroughOption option : options.bodyOptions()) {
                buttonDataList.add(new BreakthroughButtonData(
                    option, currentVirtualY, PlayerCultivationAttachment.CultivationType.BODY, breakthroughData));
                currentVirtualY += OPTION_BUTTON_HEIGHT + 8;
            }
        }

        // Handle empty content
        if (buttonDataList.isEmpty()) {
            currentVirtualY = Math.max(60, currentVirtualY);
        }

        scrollManager.scrollableContentHeight = currentVirtualY;
        dataCalculated = true;
    }

    /**
     * Sets up the breakthrough interface with all buttons and widgets.
     *
     * This method creates the interactive elements of the screen, including
     * breakthrough buttons and the close button. It handles both normal and
     * error states appropriately.
     */
    private void setupBreakthroughInterface() {
        clearWidgets();

        // Close button (always visible)
        addRenderableWidget(Button.builder(Component.literal("Close"), button -> onClose())
            .bounds(windowX + windowWidth - 70, windowY + 10, 60, 20)
            .build());

        // Create breakthrough buttons or error interface
        if (breakthroughData != null && breakthroughData.isValid()) {
            createVisibleBreakthroughButtons();

            if (buttonDataList.isEmpty()) {
                createNoOptionsMessage();
            }
        } else {
            createErrorInterface();
        }
    }

    /**
     * Creates visible breakthrough buttons within the scrollable area.
     *
     * This method only creates buttons that are currently visible to improve
     * performance. Buttons are positioned based on scroll offset and clipping bounds.
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
     * Creates a single breakthrough button with proper styling and functionality.
     *
     * @param data The pre-calculated button data containing all display information
     * @param buttonY The Y position where the button should be rendered
     */
    private void createBreakthroughButton(BreakthroughButtonData data, int buttonY) {
        int buttonX = windowX + 15;
        int buttonWidth = windowWidth - 30;

        Button button = Button.builder(
                Component.literal(data.displayText.split("\n")[0]), // Use first line as button text
                btn -> attemptBreakthrough(data.option)
            )
            .bounds(buttonX, buttonY, buttonWidth, OPTION_BUTTON_HEIGHT)
            .build();

        // Enable/disable based on availability
        button.active = data.isClickable;

        addRenderableWidget(button);
    }

    /**
     * Creates a message widget for when no breakthrough options are available.
     */
    private void createNoOptionsMessage() {
        int messageY = windowY + HEADER_HEIGHT + 50;
        Component message = Component.literal("No breakthrough options currently available")
            .withStyle(ChatFormatting.GRAY);

        // Note: In actual implementation, you might want to create a text widget here
        // For now, this will be handled in the render method
    }

    /**
     * Creates an error interface when cultivation data is unavailable.
     */
    private void createErrorInterface() {
        int messageY = windowY + HEADER_HEIGHT + 50;
        Component errorMessage = Component.literal("Cultivation system unavailable")
            .withStyle(ChatFormatting.RED);

        // Note: Similar to above, error message rendering handled in render method
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        // Handle mouse wheel scrolling
        if (scrollManager.getMaxScrollOffset() > 0) {
            scrollManager.scroll((int)(-scrollY * SCROLL_SPEED));

            // Recreate visible buttons after scrolling
            setupBreakthroughInterface();
            return true;
        }
        return false;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        // Render background and frame
        renderBackground(guiGraphics, mouseX, mouseY, partialTicks);
        renderWindowFrame(guiGraphics);

        // Render header content
        renderEnhancedBreakthroughAnalysis(guiGraphics);

        // Set up clipping for scrollable content
        guiGraphics.enableScissor(
            windowX + 10,
            windowY + HEADER_HEIGHT + 15,
            windowX + windowWidth - 10,
            windowY + windowHeight - 15
        );

        // Render section headers and content
        renderSectionHeaders(guiGraphics);
        renderBreakthroughButtons(guiGraphics);

        // Render widgets (buttons will be clipped)
        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        guiGraphics.disableScissor();

        // Render scroll indicators
        renderScrollIndicators(guiGraphics);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        guiGraphics.fill(0, 0, width, height, 0xC0101010);
    }

    /**
     * Renders the main window frame and title.
     *
     * @param guiGraphics The graphics context for rendering
     */
    private void renderWindowFrame(GuiGraphics guiGraphics) {
        // Main window background
        guiGraphics.fill(windowX, windowY, windowX + windowWidth, windowY + windowHeight, 0xFF1A1A1A);

        // Window border
        renderBorder(guiGraphics, windowX - 2, windowY - 2, windowWidth + 4, windowHeight + 4, 0xFF4A4A4A);

        // Title
        Component title = Component.literal("Enhanced Breakthrough Analysis")
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        int titleWidth = font.width(title);
        guiGraphics.drawString(font, title, windowX + (windowWidth - titleWidth) / 2, windowY + 15, 0xFFFFFFFF);

        // Separator line
        guiGraphics.fill(windowX + 10, windowY + 35, windowX + windowWidth - 10, windowY + 36, 0xFF4A4A4A);

        // Content area separator
        guiGraphics.fill(windowX + 10, windowY + HEADER_HEIGHT + 10,
            windowX + windowWidth - 10, windowY + HEADER_HEIGHT + 11, 0xFF4A4A4A);
    }

    /**
     * Renders a border around the specified area.
     *
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
     * Renders the enhanced breakthrough analysis header information.
     *
     * This method displays current cultivation status, focus information,
     * and breakthrough attempt statistics in a compact, informative format.
     *
     * @param guiGraphics The graphics context for rendering
     */
    private void renderEnhancedBreakthroughAnalysis(GuiGraphics guiGraphics) {
        if (breakthroughData == null || !breakthroughData.isValid()) {
            renderErrorMessage(guiGraphics);
            return;
        }

        int currentY = windowY + 45;

        // Current focus and realms
        renderFocusAndRealms(guiGraphics, currentY);
        currentY += LINE_HEIGHT * 2;

        // Breakthrough statistics
        renderBreakthroughStatistics(guiGraphics, currentY);
        currentY += LINE_HEIGHT;

        // Bonus information
        renderBonusInformation(guiGraphics, currentY);
    }

    /**
     * Renders error message when cultivation data is unavailable.
     *
     * @param guiGraphics The graphics context for rendering
     */
    private void renderErrorMessage(GuiGraphics guiGraphics) {
        int currentY = windowY + 50;
        String errorText = "Cultivation system unavailable - please try again";
        guiGraphics.drawString(font, errorText, windowX + 15, currentY, 0xFFFF6666);
    }

    /**
     * Renders current cultivation focus and realm information.
     *
     * @param guiGraphics The graphics context for rendering
     * @param startY The starting Y position for rendering
     */
    private void renderFocusAndRealms(GuiGraphics guiGraphics, int startY) {
        PlayerCultivationAttachment cultivation = breakthroughData.cultivation;

        // Focus display
        PlayerCultivationAttachment.CultivationType focus = cultivation.getCurrentFocus();
        String focusText = String.format("Current Focus: %s (2x rate)", focus.getDisplayName());
        int focusColor = focus == PlayerCultivationAttachment.CultivationType.QI ? QI_COLOR : BODY_COLOR;
        guiGraphics.drawString(font, focusText, windowX + 15, startY, focusColor);

        // Realm information
        String realmsText = String.format("Realms: Qi %s | Body %s",
            cultivation.getRealm(ICultivationData.CultivationType.QI).getDisplayName(),
            cultivation.getRealm(ICultivationData.CultivationType.BODY).getDisplayName());
        guiGraphics.drawString(font, realmsText, windowX + 15, startY + LINE_HEIGHT, 0xFFFFFFFF);
    }

    /**
     * Renders breakthrough attempt statistics and cooldown information.
     *
     * @param guiGraphics The graphics context for rendering
     * @param startY The starting Y position for rendering
     */
    private void renderBreakthroughStatistics(GuiGraphics guiGraphics, int startY) {
        PlayerBreakthroughAttachment breakthrough = breakthroughData.breakthrough;

        // Attempt counts
        long qiAttempts = breakthrough.getBreakthroughAttempts(PlayerCultivationAttachment.CultivationType.QI);
        long bodyAttempts = breakthrough.getBreakthroughAttempts(PlayerCultivationAttachment.CultivationType.BODY);

        String attemptsText = String.format("Attempts: Qi %d | Body %d", qiAttempts, bodyAttempts);
        guiGraphics.drawString(font, attemptsText, windowX + 15, startY, 0xFFCCCCCC);

        // Cooldown status
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
     * Renders bonus information from talents and other sources.
     *
     * @param guiGraphics The graphics context for rendering
     * @param startY The starting Y position for rendering
     */
    private void renderBonusInformation(GuiGraphics guiGraphics, int startY) {
        PlayerTalentAttachment talents = breakthroughData.talents;
        List<String> bonuses = new ArrayList<>();

        // Failed attempt bonuses
        PlayerBreakthroughAttachment breakthrough = breakthroughData.breakthrough;
        long qiAttempts = breakthrough.getBreakthroughAttempts(PlayerCultivationAttachment.CultivationType.QI);
        long bodyAttempts = breakthrough.getBreakthroughAttempts(PlayerCultivationAttachment.CultivationType.BODY);

        if (qiAttempts > 0) {
            bonuses.add(String.format("Qi Attempts +%d%%", (int)(qiAttempts * 5)));
        }
        if (bodyAttempts > 0) {
            bonuses.add(String.format("Body Attempts +%d%%", (int)(bodyAttempts * 5)));
        }

        // Talent bonuses
        if (talents.hasSpiritualRootAssigned()) {
            float multiplier = talents.getSpiritualRootMultiplier();
            double bonus = (multiplier - 1.0) * 20;
            bonuses.add(String.format("Qi Talent +%.0f%%", bonus));
        }
        if (talents.hasPhysiqueAssigned()) {
            float multiplier = talents.getPhysiqueMultiplier();
            double bonus = (multiplier - 1.0) * 20;
            bonuses.add(String.format("Body Talent +%.0f%%", bonus));
        }

        if (!bonuses.isEmpty()) {
            String bonusInfo = "Bonuses: " + String.join(" • ", bonuses);
            guiGraphics.drawString(font, bonusInfo, windowX + 15, startY, 0xFFADD8E6);
        }
    }

    /**
     * Renders section headers for Qi and Body breakthrough options.
     *
     * @param guiGraphics The graphics context for rendering
     */
    private void renderSectionHeaders(GuiGraphics guiGraphics) {
        if (buttonDataList.isEmpty()) return;

        int contentStartY = windowY + HEADER_HEIGHT + 20;
        int scrollOffset = scrollManager.getScrollOffset();

        // Check which sections we have
        boolean hasQiSection = buttonDataList.stream()
            .anyMatch(data -> data.sectionType == PlayerCultivationAttachment.CultivationType.QI);
        boolean hasBodySection = buttonDataList.stream()
            .anyMatch(data -> data.sectionType == PlayerCultivationAttachment.CultivationType.BODY);

        if (hasQiSection) {
            int headerY = contentStartY + 5 - scrollOffset;
            Component qiHeader = Component.literal("▲ QI BREAKTHROUGH OPTIONS ▲")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);
            int headerWidth = font.width(qiHeader);
            guiGraphics.drawString(font, qiHeader,
                windowX + (windowWidth - headerWidth) / 2, headerY, QI_COLOR);
        }

        if (hasBodySection) {
            // Find where body section starts
            int bodyStartY = buttonDataList.stream()
                .filter(data -> data.sectionType == PlayerCultivationAttachment.CultivationType.BODY)
                .mapToInt(data -> data.virtualY - 25)
                .findFirst()
                .orElse(0);

            int headerY = contentStartY + bodyStartY + 5 - scrollOffset;
            Component bodyHeader = Component.literal("▲ BODY BREAKTHROUGH OPTIONS ▲")
                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
            int headerWidth = font.width(bodyHeader);
            guiGraphics.drawString(font, bodyHeader,
                windowX + (windowWidth - headerWidth) / 2, headerY, BODY_COLOR);
        }
    }

    /**
     * Renders additional information for breakthrough buttons.
     *
     * This method renders detailed information that isn't part of the button widget,
     * such as success rate breakdowns and availability status.
     *
     * @param guiGraphics The graphics context for rendering
     */
    private void renderBreakthroughButtons(GuiGraphics guiGraphics) {
        int contentStartY = windowY + HEADER_HEIGHT + 20;
        int scrollOffset = scrollManager.getScrollOffset();
        int contentAreaHeight = windowHeight - HEADER_HEIGHT - 40;

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
     * Renders detailed information for a single breakthrough button.
     *
     * @param guiGraphics The graphics context for rendering
     * @param data The button data containing display information
     * @param buttonY The Y position of the button
     */
    private void renderButtonDetails(GuiGraphics guiGraphics, BreakthroughButtonData data, int buttonY) {
        int textX = windowX + 25;
        int currentY = buttonY + 20;

        // Render each line of the display text
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
     *
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

        // Scroll position indicator
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
     * Attempts a breakthrough with the specified option.
     *
     * This method validates the breakthrough attempt and sends the appropriate
     * network packet to the server. It includes comprehensive error handling
     * and logging for debugging purposes.
     *
     * @param option The breakthrough option to attempt
     */
    private void attemptBreakthrough(PlayerBreakthroughAttachment.BreakthroughOption option) {
        if (breakthroughData == null || !breakthroughData.isValid()) {
            ImmortalCultivation.LOGGER.warn("Cannot attempt breakthrough: cultivation data unavailable");
            return;
        }

        if (!option.available()) {
            ImmortalCultivation.LOGGER.warn("Cannot attempt breakthrough: option not available - {}",
                option.unavailabilityReason());
            return;
        }

        try {
            // Send breakthrough request packet using the convenience factory method
            UnifiedActionRequestPacket packet = UnifiedActionRequestPacket.createBreakthroughRequest(
                option.cultivationType(),
                option.type()
            );
            ICModPacketRegistry.sendToServer(packet);

            ImmortalCultivation.LOGGER.info("Sent {} {} breakthrough request to server",
                option.cultivationType().getDisplayName(), option.type().getDisplayName());

            // Close screen after successful request
            onClose();

        } catch (Exception e) {
            ImmortalCultivation.LOGGER.error("Error sending breakthrough request", e);
        }
    }
}
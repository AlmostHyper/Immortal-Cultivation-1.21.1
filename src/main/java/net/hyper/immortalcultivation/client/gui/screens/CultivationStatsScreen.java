package net.hyper.immortalcultivation.client.gui.screens;

import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.attachments.*;
import net.hyper.immortalcultivation.common.interfaces.ICultivationData;
import net.hyper.immortalcultivation.common.interfaces.IStatsData;
import net.hyper.immortalcultivation.core.cultivation.realms.CultivationRealm;
import net.hyper.immortalcultivation.core.cultivation.races.RaceType;
import net.hyper.immortalcultivation.core.cultivation.talents.SpiritualRootType;
import net.hyper.immortalcultivation.core.cultivation.talents.PhysiqueType;
import net.hyper.immortalcultivation.common.registry.ICModAttachmentTypes;
import net.hyper.immortalcultivation.core.network.ICModPacketRegistry;
import net.hyper.immortalcultivation.core.network.packets.client.UnifiedActionRequestPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Completely refactored Cultivation Status Screen that properly utilizes the updated attachment system.
 *
 * This implementation fixes the rendering positioning issues and provides a comprehensive view of
 * all cultivation-related data using the generic methods from PlayerAttachment classes.
 *
 * Key Features:
 * - Proper screen centering and responsive layout
 * - Full integration with all attachment types
 * - Tabbed interface for organized information display
 * - Real-time progress tracking and visual indicators
 * - Enhanced error handling and data validation
 */
public class CultivationStatsScreen extends Screen {

    // === TAB SYSTEM === //
    public enum Tab {
        OVERVIEW("Overview"),
        CULTIVATION("Cultivation"),
        STATS("Stats & Health"),
        TALENTS("Talents & Race"),
        BREAKTHROUGHS("Breakthroughs");

        private final String displayName;

        Tab(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() { return displayName; }
    }

    private Tab currentTab = Tab.OVERVIEW;

    // === LAYOUT CONSTANTS === //
    private static final int PREFERRED_WIDTH = 520;
    private static final int PREFERRED_HEIGHT = 400;
    private static final int MIN_WIDTH = 400;
    private static final int MIN_HEIGHT = 300;
    private static final int MARGIN = 16;
    private static final int TAB_HEIGHT = 28;
    private static final int BUTTON_HEIGHT = 20;
    private static final int LINE_HEIGHT = 12;
    private static final int SECTION_SPACING = 16;
    private static final int PROGRESS_BAR_HEIGHT = 8;

    // === VISUAL CONSTANTS === //
    private static final int BACKGROUND_COLOR = 0xC0101010;
    private static final int BORDER_COLOR = 0xFF404040;
    private static final int TAB_ACTIVE_COLOR = 0xFF606060;
    private static final int TAB_INACTIVE_COLOR = 0xFF303030;
    private static final int TAB_BORDER_COLOR = 0xFF505050;

    // Cultivation colors
    private static final int QI_COLOR = 0xFF87CEEB;
    private static final int BODY_COLOR = 0xFFFF6347;
    private static final int HEALTH_COLOR = 0xFFFF69B4;
    private static final int FOCUS_ACTIVE_COLOR = 0xFFFFD700;
    private static final int READY_COLOR = 0xFF90EE90;
    private static final int NOT_READY_COLOR = 0xFFFFB6C1;
    private static final int SECTION_HEADER_COLOR = 0xFFDDA0DD;
    private static final int RACE_COLOR = 0xFFFFD700;

    // === SCREEN LAYOUT === //
    private int windowWidth;
    private int windowHeight;
    private int windowX, windowY;
    private int contentAreaY;
    private int contentAreaHeight;

    // === PLAYER DATA === //
    private PlayerCultivationAttachment cultivationData;
    private PlayerStatsAttachment statsData;
    private PlayerTalentsAttachment talentsData;
    private PlayerRaceAttachment raceData;
    private PlayerBreakthroughAttachment breakthroughData;
    private PlayerPillConsumptionAttachment pillData;

    // === DATA VALIDATION === //
    private boolean dataLoaded = false;
    private String errorMessage = null;

    /**
     * Creates a new RefactoredCultivationStatsScreen instance.
     */
    public CultivationStatsScreen() {
        super(Component.literal("Cultivation Status"));
    }

    // === SCREEN LIFECYCLE === //

    /**
     * Initializes the screen, loads player data, and sets up the interface.
     */
    @Override
    protected void init() {
        super.init();
        calculateScreenLayout();
        loadPlayerData();
        setupInterface();

        if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
            ImmortalCultivation.LOGGER.debug("RefactoredCultivationStatsScreen initialized - Data loaded: {}, Error: {}",
                dataLoaded, errorMessage);
        }
    }

    /**
     * Calculates responsive screen layout based on available space.
     */
    private void calculateScreenLayout() {
        // Calculate window size with proper bounds
        windowWidth = Math.min(PREFERRED_WIDTH, width - 40);
        windowHeight = Math.min(PREFERRED_HEIGHT, height - 40);

        windowWidth = Math.max(windowWidth, MIN_WIDTH);
        windowHeight = Math.max(windowHeight, MIN_HEIGHT);

        // Center the window properly
        windowX = (width - windowWidth) / 2;
        windowY = (height - windowHeight) / 2;

        // Calculate content area
        contentAreaY = windowY + MARGIN + TAB_HEIGHT + 8;
        contentAreaHeight = windowHeight - (MARGIN * 2) - TAB_HEIGHT - BUTTON_HEIGHT - 16;

        if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
            ImmortalCultivation.LOGGER.debug("Screen layout calculated - Window: {}x{} at ({}, {}), Content area: {} high",
                windowWidth, windowHeight, windowX, windowY, contentAreaHeight);
        }
    }

    /**
     * Loads all player attachment data using the updated attachment system.
     */
    private void loadPlayerData() {
        dataLoaded = false;
        errorMessage = null;

        if (minecraft.player == null) {
            errorMessage = "Player not available";
            return;
        }

        try {
            // Load all attachments using the registry
            cultivationData = minecraft.player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
            statsData = minecraft.player.getData(ICModAttachmentTypes.PLAYER_STATS.get());
            talentsData = minecraft.player.getData(ICModAttachmentTypes.PLAYER_TALENTS.get());
            raceData = minecraft.player.getData(ICModAttachmentTypes.PLAYER_RACE.get());
            breakthroughData = minecraft.player.getData(ICModAttachmentTypes.PLAYER_BREAKTHROUGH.get());
            pillData = minecraft.player.getData(ICModAttachmentTypes.PLAYER_PILL_CONSUMPTION.get());

            // Verify critical attachments
            if (cultivationData == null || statsData == null || talentsData == null) {
                errorMessage = "Critical attachment data missing";
                return;
            }

            // Set player references using generic PlayerAttachment methods
            cultivationData.setPlayer(minecraft.player);
            statsData.setPlayer(minecraft.player);
            talentsData.setPlayer(minecraft.player);

            if (raceData != null) raceData.setPlayer(minecraft.player);
            if (breakthroughData != null) breakthroughData.setPlayer(minecraft.player);
            if (pillData != null) pillData.setPlayer(minecraft.player);

            dataLoaded = true;

            if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
                ImmortalCultivation.LOGGER.debug("All player attachment data loaded successfully");
            }

        } catch (Exception e) {
            errorMessage = "Error loading player data: " + e.getMessage();
            ImmortalCultivation.LOGGER.error("Failed to load player attachment data", e);
        }
    }

    /**
     * Sets up the user interface with tabs and action buttons.
     */
    private void setupInterface() {
        clearWidgets();

        if (!dataLoaded) {
            setupErrorInterface();
            return;
        }

        setupTabButtons();
        setupActionButtons();
    }

    /**
     * Creates tab selection buttons for navigation.
     */
    private void setupTabButtons() {
        int tabWidth = windowWidth / Tab.values().length;
        int tabX = windowX;

        for (Tab tab : Tab.values()) {
            Button tabButton = Button.builder(
                    Component.literal(tab.getDisplayName()),
                    (button) -> {
                        currentTab = tab;
                        setupInterface(); // Refresh interface for new tab
                    }
                )
                .bounds(tabX, windowY + MARGIN, tabWidth, TAB_HEIGHT)
                .build();

            addRenderableWidget(tabButton);
            tabX += tabWidth;
        }
    }

    /**
     * Creates action buttons for cultivation management.
     */
    private void setupActionButtons() {
        int buttonY = windowY + windowHeight - MARGIN - BUTTON_HEIGHT;
        int buttonWidth = 80;
        int spacing = 10;

        // Focus toggle button
        String focusText = cultivationData.getCurrentFocus() == ICultivationData.CultivationType.QI ? "Focus: Qi" : "Focus: Body";
        Button focusButton = Button.builder(
                Component.literal(focusText),
                (button) -> ICModPacketRegistry.sendToServer(UnifiedActionRequestPacket.createFocusToggle())
            )
            .bounds(windowX + MARGIN, buttonY, buttonWidth, BUTTON_HEIGHT)
            .build();
        addRenderableWidget(focusButton);

        // Close button
        Button closeButton = Button.builder(
                Component.literal("Close"),
                (button) -> onClose()
            )
            .bounds(windowX + windowWidth - MARGIN - buttonWidth, buttonY, buttonWidth, BUTTON_HEIGHT)
            .build();
        addRenderableWidget(closeButton);
    }

    /**
     * Sets up error interface when data loading fails.
     */
    private void setupErrorInterface() {
        // Simple close button for error state
        Button closeButton = Button.builder(
                Component.literal("Close"),
                (button) -> onClose()
            )
            .bounds(windowX + windowWidth - MARGIN - 80, windowY + windowHeight - MARGIN - BUTTON_HEIGHT, 80, BUTTON_HEIGHT)
            .build();
        addRenderableWidget(closeButton);
    }

    // === RENDERING === //

    /**
     * Main render method that handles the complete screen rendering.
     * @param guiGraphics The graphics context for rendering
     * @param mouseX Current mouse X position
     * @param mouseY Current mouse Y position
     * @param partialTick Partial tick for smooth animation
     */
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Clear background
        guiGraphics.fill(0, 0, width, height, 0x80000000);

        // Draw main window
        drawWindow(guiGraphics);

        // Draw tab headers
        drawTabHeaders(guiGraphics);

        // Draw content based on current tab
        if (dataLoaded) {
            drawTabContent(guiGraphics);
        } else {
            drawErrorMessage(guiGraphics);
        }

        // Render widgets (buttons)
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    /**
     * Draws the main window background and border.
     * @param guiGraphics The graphics context for rendering
     */
    private void drawWindow(GuiGraphics guiGraphics) {
        // Main window background
        guiGraphics.fill(windowX, windowY, windowX + windowWidth, windowY + windowHeight, BACKGROUND_COLOR);

        // Window border
        guiGraphics.fill(windowX - 1, windowY - 1, windowX + windowWidth + 1, windowY + windowHeight + 1, BORDER_COLOR);
        guiGraphics.fill(windowX, windowY, windowX + windowWidth, windowY + windowHeight, BACKGROUND_COLOR);
    }

    /**
     * Draws tab headers with active/inactive styling.
     * @param guiGraphics The graphics context for rendering
     */
    private void drawTabHeaders(GuiGraphics guiGraphics) {
        int tabWidth = windowWidth / Tab.values().length;
        int tabX = windowX;

        for (Tab tab : Tab.values()) {
            int color = (tab == currentTab) ? TAB_ACTIVE_COLOR : TAB_INACTIVE_COLOR;

            // Tab background
            guiGraphics.fill(tabX, windowY + MARGIN, tabX + tabWidth, windowY + MARGIN + TAB_HEIGHT, color);

            // Tab border
            guiGraphics.fill(tabX, windowY + MARGIN, tabX + tabWidth, windowY + MARGIN + 1, TAB_BORDER_COLOR);
            guiGraphics.fill(tabX, windowY + MARGIN + TAB_HEIGHT - 1, tabX + tabWidth, windowY + MARGIN + TAB_HEIGHT, TAB_BORDER_COLOR);

            tabX += tabWidth;
        }
    }

    /**
     * Draws content for the currently selected tab.
     * @param guiGraphics The graphics context for rendering
     */
    private void drawTabContent(GuiGraphics guiGraphics) {
        switch (currentTab) {
            case OVERVIEW -> drawOverviewTab(guiGraphics);
            case CULTIVATION -> drawCultivationTab(guiGraphics);
            case STATS -> drawStatsTab(guiGraphics);
            case TALENTS -> drawTalentsTab(guiGraphics);
            case BREAKTHROUGHS -> drawBreakthroughsTab(guiGraphics);
        }
    }

    /**
     * Draws the overview tab with key cultivation information.
     * @param guiGraphics The graphics context for rendering
     */
    private void drawOverviewTab(GuiGraphics guiGraphics) {
        int y = contentAreaY + MARGIN;

        // Current focus
        ICultivationData.CultivationType focus = cultivationData.getCurrentFocus();
        String focusText = "Current Focus: " + (focus == ICultivationData.CultivationType.QI ? "Qi Cultivation" : "Body Cultivation");
        guiGraphics.drawString(font, focusText, windowX + MARGIN, y, FOCUS_ACTIVE_COLOR);
        y += LINE_HEIGHT + 4;

        // Cultivation realms
        drawSectionHeader(guiGraphics, "Cultivation Realms", windowX + MARGIN, y);
        y += LINE_HEIGHT + 4;

        for (ICultivationData.CultivationType type : ICultivationData.CultivationType.values()) {
            CultivationRealm realm = cultivationData.getRealm(type);
            int color = type == ICultivationData.CultivationType.QI ? QI_COLOR : BODY_COLOR;

            String realmText = type.name() + " Cultivation: " + (realm != null ? realm.getDisplayName() : "None");
            guiGraphics.drawString(font, realmText, windowX + MARGIN, y, color);
            y += LINE_HEIGHT;

            // Progress bar
            long current = cultivationData.getCurrentCultivation(type);
            long max = cultivationData.getMaxCultivation(type);
            drawProgressBar(guiGraphics, windowX + MARGIN, y, 200, current, max, color);
            y += PROGRESS_BAR_HEIGHT + 8;
        }

        // Health information
        y += SECTION_SPACING;
        drawSectionHeader(guiGraphics, "Health & Energy", windowX + MARGIN, y);
        y += LINE_HEIGHT + 4;

        double health = statsData.getCurrentStat(IStatsData.StatType.HEALTH);
        double maxHealth = statsData.getMaxStat(IStatsData.StatType.HEALTH);
        String healthText = String.format("Health: %.1f / %.1f", health, maxHealth);
        guiGraphics.drawString(font, healthText, windowX + MARGIN, y, HEALTH_COLOR);
        y += LINE_HEIGHT;

        drawProgressBar(guiGraphics, windowX + MARGIN, y, 200, (long)health, (long)maxHealth, HEALTH_COLOR);
    }

    /**
     * Draws the detailed cultivation tab.
     * @param guiGraphics The graphics context for rendering
     */
    private void drawCultivationTab(GuiGraphics guiGraphics) {
        int y = contentAreaY + MARGIN;

        for (ICultivationData.CultivationType type : ICultivationData.CultivationType.values()) {
            int color = type == ICultivationData.CultivationType.QI ? QI_COLOR : BODY_COLOR;

            drawSectionHeader(guiGraphics, type.name() + " Cultivation", windowX + MARGIN, y);
            y += LINE_HEIGHT + 4;

            // Current realm and progress
            CultivationRealm realm = cultivationData.getRealm(type);
            if (realm != null) {
                guiGraphics.drawString(font, "Realm: " + realm.getDisplayName(), windowX + MARGIN + 8, y, color);
                y += LINE_HEIGHT;
            }

            // Cultivation progress
            long current = cultivationData.getCurrentCultivation(type);
            long max = cultivationData.getMaxCultivation(type);
            String progressText = String.format("Progress: %s / %s", formatNumber(current), formatNumber(max));
            guiGraphics.drawString(font, progressText, windowX + MARGIN + 8, y, 0xFFFFFFFF);
            y += LINE_HEIGHT;

            drawProgressBar(guiGraphics, windowX + MARGIN + 8, y, 180, current, max, color);
            y += PROGRESS_BAR_HEIGHT + 4;

            // Cultivation rate
            double rate = cultivationData.getEffectiveCultivationRate(type);
            String rateText = String.format("Rate: %.2f/tick", rate);
            guiGraphics.drawString(font, rateText, windowX + MARGIN + 8, y, 0xFFCCCCCC);
            y += LINE_HEIGHT + SECTION_SPACING;
        }
    }

    /**
     * Draws the stats and health tab.
     * @param guiGraphics The graphics context for rendering
     */
    private void drawStatsTab(GuiGraphics guiGraphics) {
        int y = contentAreaY + MARGIN;

        drawSectionHeader(guiGraphics, "Health Statistics", windowX + MARGIN, y);
        y += LINE_HEIGHT + 4;

        // Health values using IStatsData interface
        long health = statsData.getCurrentStat(IStatsData.StatType.HEALTH);
        long maxHealth = statsData.getMaxStat(IStatsData.StatType.HEALTH);
        double healthRegen = statsData.getStatRegenRate(IStatsData.StatType.HEALTH);

        guiGraphics.drawString(font, String.format("Current Health: %d", health), windowX + MARGIN + 8, y, HEALTH_COLOR);
        y += LINE_HEIGHT;
        guiGraphics.drawString(font, String.format("Maximum Health: %d", maxHealth), windowX + MARGIN + 8, y, HEALTH_COLOR);
        y += LINE_HEIGHT;
        guiGraphics.drawString(font, String.format("Health Regeneration: %.2f/tick", healthRegen), windowX + MARGIN + 8, y, 0xFF98FB98);
        y += LINE_HEIGHT + 4;

        drawProgressBar(guiGraphics, windowX + MARGIN + 8, y, 200, health, maxHealth, HEALTH_COLOR);
        y += PROGRESS_BAR_HEIGHT + SECTION_SPACING;

        // Energy information using separate Qi and Body StatTypes
        drawSectionHeader(guiGraphics, "Energy Statistics", windowX + MARGIN, y);
        y += LINE_HEIGHT + 4;

        // Qi Energy
        long currentQiEnergy = statsData.getCurrentStat(IStatsData.StatType.QI);
        long maxQiEnergy = statsData.getMaxStat(IStatsData.StatType.QI);
        double qiEnergyRegen = statsData.getStatRegenRate(IStatsData.StatType.QI);

        guiGraphics.drawString(font, String.format("Qi Energy: %d / %d", currentQiEnergy, maxQiEnergy), windowX + MARGIN + 8, y, QI_COLOR);
        y += LINE_HEIGHT;
        guiGraphics.drawString(font, String.format("Qi Regeneration: %.2f/tick", qiEnergyRegen), windowX + MARGIN + 12, y, 0xFFCCCCCC);
        y += LINE_HEIGHT;

        drawProgressBar(guiGraphics, windowX + MARGIN + 8, y, 180, currentQiEnergy, maxQiEnergy, QI_COLOR);
        y += PROGRESS_BAR_HEIGHT + 8;

        // Body Energy
        long currentBodyEnergy = statsData.getCurrentStat(IStatsData.StatType.BODY);
        long maxBodyEnergy = statsData.getMaxStat(IStatsData.StatType.BODY);
        double bodyEnergyRegen = statsData.getStatRegenRate(IStatsData.StatType.BODY);

        guiGraphics.drawString(font, String.format("Body Energy: %d / %d", currentBodyEnergy, maxBodyEnergy), windowX + MARGIN + 8, y, BODY_COLOR);
        y += LINE_HEIGHT;
        guiGraphics.drawString(font, String.format("Body Regeneration: %.2f/tick", bodyEnergyRegen), windowX + MARGIN + 12, y, 0xFFCCCCCC);
        y += LINE_HEIGHT;

        drawProgressBar(guiGraphics, windowX + MARGIN + 8, y, 180, currentBodyEnergy, maxBodyEnergy, BODY_COLOR);
        y += PROGRESS_BAR_HEIGHT + 8;
    }

    /**
     * Draws the talents and race tab.
     * @param guiGraphics The graphics context for rendering
     */
    private void drawTalentsTab(GuiGraphics guiGraphics) {
        int y = contentAreaY + MARGIN;

        // Race information
        if (raceData != null) {
            drawSectionHeader(guiGraphics, "Race Information", windowX + MARGIN, y);
            y += LINE_HEIGHT + 4;

            if (raceData.hasRaceAssigned()) {
                RaceType race = raceData.getRaceType();
                guiGraphics.drawString(font, "Race: " + race.getDisplayName(), windowX + MARGIN + 8, y, RACE_COLOR);
                y += LINE_HEIGHT;

                double qiMult = raceData.getCultivationMultiplier(ICultivationData.CultivationType.QI);
                double bodyMult = raceData.getCultivationMultiplier(ICultivationData.CultivationType.BODY);

                guiGraphics.drawString(font, String.format("Qi Multiplier: %.1fx", qiMult), windowX + MARGIN + 8, y, QI_COLOR);
                y += LINE_HEIGHT;
                guiGraphics.drawString(font, String.format("Body Multiplier: %.1fx", bodyMult), windowX + MARGIN + 8, y, BODY_COLOR);
                y += LINE_HEIGHT + SECTION_SPACING;
            } else {
                guiGraphics.drawString(font, "Race: Not Assigned", windowX + MARGIN + 8, y, 0xFFFF0000);
                y += LINE_HEIGHT + SECTION_SPACING;
            }
        }

        // Talents information
        drawSectionHeader(guiGraphics, "Talents", windowX + MARGIN, y);
        y += LINE_HEIGHT + 4;

        if (talentsData.isTalentAssigned(PlayerTalentsAttachment.TalentType.SPIRITUAL_ROOT)) {
            SpiritualRootType spiritualRoot = talentsData.getSpiritualRootType();
            guiGraphics.drawString(font, "Spiritual Root: " + spiritualRoot.getDisplayName(), windowX + MARGIN + 8, y, QI_COLOR);
            y += LINE_HEIGHT;

            double spiritualMult = talentsData.getTalentMultiplier(PlayerTalentsAttachment.TalentType.SPIRITUAL_ROOT);
            guiGraphics.drawString(font, String.format("Multiplier: %.1fx", spiritualMult), windowX + MARGIN + 12, y, 0xFFCCCCCC);
            y += LINE_HEIGHT;
        } else {
            guiGraphics.drawString(font, "Spiritual Root: Not Assigned", windowX + MARGIN + 8, y, 0xFFFF0000);
            y += LINE_HEIGHT;
        }

        if (talentsData.isTalentAssigned(PlayerTalentsAttachment.TalentType.PHYSIQUE)) {
            PhysiqueType physique = talentsData.getPhysiqueType();
            guiGraphics.drawString(font, "Physique: " + physique.getDisplayName(), windowX + MARGIN + 8, y, BODY_COLOR);
            y += LINE_HEIGHT;

            double physiqueMult = talentsData.getTalentMultiplier(PlayerTalentsAttachment.TalentType.PHYSIQUE);
            guiGraphics.drawString(font, String.format("Multiplier: %.1fx", physiqueMult), windowX + MARGIN + 12, y, 0xFFCCCCCC);
            y += LINE_HEIGHT;
        } else {
            guiGraphics.drawString(font, "Physique: Not Assigned", windowX + MARGIN + 8, y, 0xFFFF0000);
            y += LINE_HEIGHT;
        }
    }

    /**
     * Draws the breakthroughs tab.
     * @param guiGraphics The graphics context for rendering
     */
    private void drawBreakthroughsTab(GuiGraphics guiGraphics) {
        int y = contentAreaY + MARGIN;

        if (breakthroughData == null) {
            guiGraphics.drawString(font, "Breakthrough data not available", windowX + MARGIN, y, 0xFFFF0000);
            return;
        }

        drawSectionHeader(guiGraphics, "Breakthrough Status", windowX + MARGIN, y);
        y += LINE_HEIGHT + 4;

        for (ICultivationData.CultivationType type : ICultivationData.CultivationType.values()) {
            int color = type == ICultivationData.CultivationType.QI ? QI_COLOR : BODY_COLOR;
            String typeName = type == ICultivationData.CultivationType.QI ? "Qi" : "Body";

            boolean canBreakthrough = cultivationData.canBreakthrough(type);
            boolean onCooldown = breakthroughData.isBreakthroughOnCooldown(type);

            String status;
            int statusColor;
            if (onCooldown) {
                status = "On Cooldown";
                statusColor = 0xFFFF0000;
            } else if (canBreakthrough) {
                status = "Ready";
                statusColor = READY_COLOR;
            } else {
                status = "Not Ready";
                statusColor = NOT_READY_COLOR;
            }

            guiGraphics.drawString(font, typeName + " Breakthrough: " + status, windowX + MARGIN + 8, y, statusColor);
            y += LINE_HEIGHT + 8;
        }
    }

    // === UTILITY METHODS === //

    /**
     * Draws a section header with consistent styling.
     * @param guiGraphics The graphics context for rendering
     * @param text The header text
     * @param x The X position
     * @param y The Y position
     */
    private void drawSectionHeader(GuiGraphics guiGraphics, String text, int x, int y) {
        guiGraphics.drawString(font, text, x, y, SECTION_HEADER_COLOR);
    }

    /**
     * Draws a progress bar with current and maximum values.
     * @param guiGraphics The graphics context for rendering
     * @param x The X position
     * @param y The Y position
     * @param width The total width of the progress bar
     * @param current The current value
     * @param max The maximum value
     * @param color The color of the progress fill
     */
    private void drawProgressBar(GuiGraphics guiGraphics, int x, int y, int width, long current, long max, int color) {
        // Background
        guiGraphics.fill(x, y, x + width, y + PROGRESS_BAR_HEIGHT, 0xFF404040);

        // Progress fill
        if (max > 0) {
            int fillWidth = (int) ((double) current / max * width);
            guiGraphics.fill(x, y, x + fillWidth, y + PROGRESS_BAR_HEIGHT, color);
        }

        // Border
        guiGraphics.fill(x - 1, y - 1, x + width + 1, y + PROGRESS_BAR_HEIGHT + 1, BORDER_COLOR);
        guiGraphics.fill(x, y, x + width, y + PROGRESS_BAR_HEIGHT, 0x00000000);
    }

    /**
     * Draws an error message when data loading fails.
     * @param guiGraphics The graphics context for rendering
     */
    private void drawErrorMessage(GuiGraphics guiGraphics) {
        int y = contentAreaY + MARGIN;

        guiGraphics.drawString(font, "Error loading cultivation data:", windowX + MARGIN, y, 0xFFFF0000);
        y += LINE_HEIGHT + 4;

        if (errorMessage != null) {
            guiGraphics.drawString(font, errorMessage, windowX + MARGIN, y, 0xFFFFAAAA);
        }
    }

    /**
     * Formats large numbers with appropriate suffixes.
     * @param number The number to format
     * @return Formatted number string
     */
    private String formatNumber(long number) {
        if (number < 1000) return String.valueOf(number);
        if (number < 1000000) return String.format("%.1fK", number / 1000.0);
        if (number < 1000000000) return String.format("%.1fM", number / 1000000.0);
        return String.format("%.1fB", number / 1000000000.0);
    }

    /**
     * Handles screen resizing by recalculating layout and refreshing interface.
     * @param minecraft The minecraft instance
     * @param width New screen width
     * @param height New screen height
     */
    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        super.resize(minecraft, width, height);
        calculateScreenLayout();
        setupInterface();
    }

    /**
     * Called when the screen is closing for cleanup.
     */
    @Override
    public void onClose() {
        if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
            ImmortalCultivation.LOGGER.debug("RefactoredCultivationStatsScreen closing - Tab: {}, Data loaded: {}",
                currentTab, dataLoaded);
        }
        super.onClose();
    }

    /**
     * Determines if the screen should pause the game.
     * @return False to allow game to continue running
     */
    @Override
    public boolean isPauseScreen() {
        return false; // Don't pause the game - cultivation continues
    }
}
package net.hyper.immortalcultivation.client.gui.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.common.attachments.*;
import net.hyper.immortalcultivation.common.interfaces.ICultivationData;
import net.hyper.immortalcultivation.common.interfaces.IStatsData;
import net.hyper.immortalcultivation.core.cultivation.realms.CultivationRealm;
import net.hyper.immortalcultivation.core.cultivation.races.RaceType;
import net.hyper.immortalcultivation.core.cultivation.talents.SpiritualRootType;
import net.hyper.immortalcultivation.core.cultivation.talents.PhysiqueType;
import net.hyper.immortalcultivation.common.attachments.AttachmentRegistry;
import net.hyper.immortalcultivation.core.network.NetworkRegistry;
import net.hyper.immortalcultivation.core.network.packets.client.UnifiedActionRequestPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.opengl.GL11;

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
    private static final int BACKGROUND_COLOR = 0xFF101010;
    private static final int BORDER_COLOR = 0xFF404040;
    private static final int TAB_ACTIVE_COLOR = 0xFF606060;
    private static final int TAB_INACTIVE_COLOR = 0xFF303030;
    private static final int TAB_BORDER_COLOR = 0xFF505050;

    // Cultivation colors
    private static final int QI_COLOR = 0xFFAADDFF;
    private static final int BODY_COLOR = 0xFFFF8A65;
    private static final int HEALTH_COLOR = 0xFFFF8BCC;
    private static final int SECTION_HEADER_COLOR = 0xFFEEBBEE;
    private static final int FOCUS_ACTIVE_COLOR = 0xFFFFFF00;
    private static final int READY_COLOR = 0xFFAAFFAA;
    private static final int NOT_READY_COLOR = 0xFFFFB6C1;
    private static final int RACE_COLOR = 0xFFFFFF88;

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

    private boolean hasLoadedOnce = false;

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

        if (!hasLoadedOnce) {
            loadPlayerData();
            hasLoadedOnce = true;
        }

        setupInterface();

        if (ImmortalCultivation.LOGGER.isDebugEnabled()) {
            ImmortalCultivation.LOGGER.debug("RefactoredCultivationStatsScreen initialized - Data loaded: {}, Error: {}",
                dataLoaded, errorMessage);
        }
    }

    /**
     * Calculates responsive screen layout without manual GUI scale manipulation.
     * Works entirely in GUI coordinates as provided by Minecraft.
     */
    private void calculateScreenLayout() {
        // Work directly with the GUI coordinates - no manual scaling needed
        windowWidth = Math.min(PREFERRED_WIDTH, width - 40);
        windowHeight = Math.min(PREFERRED_HEIGHT, height - 40);

        windowWidth = Math.max(windowWidth, MIN_WIDTH);
        windowHeight = Math.max(windowHeight, MIN_HEIGHT);

        // Ensure whole pixel coordinates to prevent blur
        windowX = Math.round((width - windowWidth) / 2.0f);
        windowY = Math.round((height - windowHeight) / 2.0f);

        // Calculate content area with whole pixels
        contentAreaY = windowY + MARGIN + TAB_HEIGHT + 8;
        contentAreaHeight = windowHeight - (MARGIN * 2) - TAB_HEIGHT - BUTTON_HEIGHT - 16;
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
            cultivationData = minecraft.player.getData(AttachmentRegistry.PLAYER_CULTIVATION.get());
            statsData = minecraft.player.getData(AttachmentRegistry.PLAYER_STATS.get());
            talentsData = minecraft.player.getData(AttachmentRegistry.PLAYER_TALENTS.get());
            raceData = minecraft.player.getData(AttachmentRegistry.PLAYER_RACE.get());
            breakthroughData = minecraft.player.getData(AttachmentRegistry.PLAYER_BREAKTHROUGH.get());
            pillData = minecraft.player.getData(AttachmentRegistry.PLAYER_PILL_CONSUMPTION.get());

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
                        // DON'T recreate interface, just switch tab
                        // setupInterface(); // REMOVE THIS LINE
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
                (button) -> NetworkRegistry.sendToServer(UnifiedActionRequestPacket.createFocusToggle())
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
        enableSharpRendering();
        try {
            this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
            drawWindow(guiGraphics);
            drawTabHeaders(guiGraphics);

            if (dataLoaded) {
                drawTabContent(guiGraphics);
            } else {
                drawErrorMessage(guiGraphics);
            }

            super.render(guiGraphics, mouseX, mouseY, partialTick);
        } finally {
            restoreNormalRendering();
        }
    }

    /**
     * Renders a lighter background overlay that doesn't dim text as much.
     * @param guiGraphics The graphics context for rendering
     * @param mouseX Mouse X position
     * @param mouseY Mouse Y position
     * @param partialTick Partial tick for smooth animation
     */
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Much lighter overlay - less dimming effect
        guiGraphics.fill(0, 0, this.width, this.height, 0x40000000);
    }

    /**
     * Draws the main window background and border with improved contrast.
     * @param guiGraphics The graphics context for rendering
     */
    private void drawWindow(GuiGraphics guiGraphics) {
        // Draw border first (larger rect) - slightly lighter border
        guiGraphics.fill(windowX - 1, windowY - 1, windowX + windowWidth + 1, windowY + windowHeight + 1, 0xFF606060);

        // Much lighter background for better text contrast
        guiGraphics.fill(windowX, windowY, windowX + windowWidth, windowY + windowHeight, 0xFF505050);
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
        drawScaledText(guiGraphics, focusText, windowX + MARGIN, y, FOCUS_ACTIVE_COLOR);
        y += LINE_HEIGHT + 4;

        // Cultivation realms
        drawSectionHeader(guiGraphics, "Cultivation Realms", windowX + MARGIN, y);
        y += LINE_HEIGHT + 4;

        for (ICultivationData.CultivationType type : ICultivationData.CultivationType.values()) {
            CultivationRealm realm = cultivationData.getRealm(type);
            int color = type == ICultivationData.CultivationType.QI ? QI_COLOR : BODY_COLOR;

            String realmText = type.name() + " Cultivation: " + (realm != null ? realm.getDisplayName() : "None");
            drawScaledText(guiGraphics, realmText, windowX + MARGIN, y, color);
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
        String healthText = String.format("Health: %s / %s", formatNumber(health), formatNumber(maxHealth));
        drawScaledText(guiGraphics, healthText, windowX + MARGIN, y, HEALTH_COLOR);
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
                drawScaledText(guiGraphics, "Realm: " + realm.getDisplayName(), windowX + MARGIN + 8, y, color);
                y += LINE_HEIGHT;
            }

            // Cultivation progress
            long current = cultivationData.getCurrentCultivation(type);
            long max = cultivationData.getMaxCultivation(type);
            String progressText = String.format("Progress: %s / %s", formatNumber(current), formatNumber(max));
            drawScaledText(guiGraphics, progressText, windowX + MARGIN + 8, y, 0xFFFFFFFF);
            y += LINE_HEIGHT;

            drawProgressBar(guiGraphics, windowX + MARGIN + 8, y, 180, current, max, color);
            y += PROGRESS_BAR_HEIGHT + 4;

            // Cultivation rate
            double rate = cultivationData.getEffectiveCultivationRate(type);
            String rateText = String.format("Rate: %.5f/tick", rate);
            drawScaledText(guiGraphics, rateText, windowX + MARGIN + 8, y, 0xFFCCCCCC);
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

        drawScaledText(guiGraphics, String.format("Current Health: %d", health), windowX + MARGIN + 8, y, HEALTH_COLOR);
        y += LINE_HEIGHT;
        drawScaledText(guiGraphics, String.format("Maximum Health: %d", maxHealth), windowX + MARGIN + 8, y, HEALTH_COLOR);
        y += LINE_HEIGHT;
        drawScaledText(guiGraphics, String.format("Health Regeneration: %.5f/tick", healthRegen), windowX + MARGIN + 8, y, 0xFF98FB98);
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

        drawScaledText(guiGraphics, String.format("Qi Energy: %s / %s", formatNumber(currentQiEnergy), formatNumber(maxQiEnergy)), windowX + MARGIN + 8, y, QI_COLOR);
        y += LINE_HEIGHT;
        drawScaledText(guiGraphics, String.format("Qi Regeneration: %.5f/tick", qiEnergyRegen), windowX + MARGIN + 12, y, 0xFFCCCCCC);
        y += LINE_HEIGHT;

        drawProgressBar(guiGraphics, windowX + MARGIN + 8, y, 180, currentQiEnergy, maxQiEnergy, QI_COLOR);
        y += PROGRESS_BAR_HEIGHT + 8;

        // Body Energy
        long currentBodyEnergy = statsData.getCurrentStat(IStatsData.StatType.BODY);
        long maxBodyEnergy = statsData.getMaxStat(IStatsData.StatType.BODY);
        double bodyEnergyRegen = statsData.getStatRegenRate(IStatsData.StatType.BODY);

        drawScaledText(guiGraphics, String.format("Body Energy: %s / %s", formatNumber(currentBodyEnergy), formatNumber(maxBodyEnergy)), windowX + MARGIN + 8, y, BODY_COLOR);
        y += LINE_HEIGHT;
        drawScaledText(guiGraphics, String.format("Body Regeneration: %.5f/tick", bodyEnergyRegen), windowX + MARGIN + 12, y, 0xFFCCCCCC);
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
                drawScaledText(guiGraphics, "Race: " + race.getDisplayName(), windowX + MARGIN + 8, y, RACE_COLOR);
                y += LINE_HEIGHT;

                double qiMult = raceData.getCultivationMultiplier(ICultivationData.CultivationType.QI);
                double bodyMult = raceData.getCultivationMultiplier(ICultivationData.CultivationType.BODY);

                drawScaledText(guiGraphics, String.format("Qi Multiplier: %.1fx", qiMult), windowX + MARGIN + 8, y, QI_COLOR);
                y += LINE_HEIGHT;
                drawScaledText(guiGraphics, String.format("Body Multiplier: %.1fx", bodyMult), windowX + MARGIN + 8, y, BODY_COLOR);
                y += LINE_HEIGHT + SECTION_SPACING;
            } else {
                drawScaledText(guiGraphics, "Race: Not Assigned", windowX + MARGIN + 8, y, 0xFFFF0000);
                y += LINE_HEIGHT + SECTION_SPACING;
            }
        }

        // Talents information
        drawSectionHeader(guiGraphics, "Talents", windowX + MARGIN, y);
        y += LINE_HEIGHT + 4;

        if (talentsData.isTalentAssigned(PlayerTalentsAttachment.TalentType.SPIRITUAL_ROOT)) {
            SpiritualRootType spiritualRoot = talentsData.getSpiritualRootType();
            drawScaledText(guiGraphics, "Spiritual Root: " + spiritualRoot.getDisplayName(), windowX + MARGIN + 8, y, QI_COLOR);
            y += LINE_HEIGHT;

            double spiritualMult = talentsData.getTalentMultiplier(PlayerTalentsAttachment.TalentType.SPIRITUAL_ROOT);
            drawScaledText(guiGraphics, String.format("Multiplier: %.1fx", spiritualMult), windowX + MARGIN + 12, y, 0xFFCCCCCC);
            y += LINE_HEIGHT;
        } else {
            drawScaledText(guiGraphics, "Spiritual Root: Not Assigned", windowX + MARGIN + 8, y, 0xFFFF0000);
            y += LINE_HEIGHT;
        }

        if (talentsData.isTalentAssigned(PlayerTalentsAttachment.TalentType.PHYSIQUE)) {
            PhysiqueType physique = talentsData.getPhysiqueType();
            drawScaledText(guiGraphics, "Physique: " + physique.getDisplayName(), windowX + MARGIN + 8, y, BODY_COLOR);
            y += LINE_HEIGHT;

            double physiqueMult = talentsData.getTalentMultiplier(PlayerTalentsAttachment.TalentType.PHYSIQUE);
            drawScaledText(guiGraphics, String.format("Multiplier: %.1fx", physiqueMult), windowX + MARGIN + 12, y, 0xFFCCCCCC);
            y += LINE_HEIGHT;
        } else {
            drawScaledText(guiGraphics, "Physique: Not Assigned", windowX + MARGIN + 8, y, 0xFFFF0000);
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
            drawScaledText(guiGraphics, "Breakthrough data not available", windowX + MARGIN, y, 0xFFFF0000);
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

            drawScaledText(guiGraphics, typeName + " Breakthrough: " + status, windowX + MARGIN + 8, y, statusColor);
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
        drawScaledText(guiGraphics, text, x, y, SECTION_HEADER_COLOR);
    }

    /**
     * Draws a progress bar with current and maximum values.
     * Fixed to render border first, then background, then progress fill.
     * @param guiGraphics The graphics context for rendering
     * @param x The X position
     * @param y The Y position
     * @param width The total width of the progress bar
     * @param current The current value
     * @param max The maximum value
     * @param color The color of the progress fill
     */
    private void drawProgressBar(GuiGraphics guiGraphics, int x, int y, int width, long current, long max, int color) {
        double percentage = max > 0 ? (double) current / max : 0.0;
        int fillWidth = (int) (percentage * width);

        // Draw border FIRST (underneath everything)
        guiGraphics.fill(x - 1, y - 1, x + width + 1, y + PROGRESS_BAR_HEIGHT + 1, BORDER_COLOR);

        // Draw background (inside the border)
        guiGraphics.fill(x, y, x + width, y + PROGRESS_BAR_HEIGHT, 0xFF202020);

        // Draw progress fill (on top of background, inside border)
        if (max > 0 && current > 0) {
            int visibleFill = Math.max(2, fillWidth); // At least 2 pixels if any progress
            visibleFill = Math.min(visibleFill, width); // Don't exceed the bar width

            guiGraphics.fill(x, y, x + visibleFill, y + PROGRESS_BAR_HEIGHT, color);

            // Add a brighter edge for very small progress (more subtle)
            if (fillWidth < 8 && visibleFill > 1) {
                int brightColor = brightenColor(color);
                guiGraphics.fill(x + visibleFill - 1, y, x + visibleFill, y + PROGRESS_BAR_HEIGHT, brightColor);
            }
        }
    }

    // Helper method to brighten colors
    private int brightenColor(int color) {
        int alpha = (color >> 24) & 0xFF;
        int red = Math.min(255, ((color >> 16) & 0xFF) + 40);
        int green = Math.min(255, ((color >> 8) & 0xFF) + 40);
        int blue = Math.min(255, (color & 0xFF) + 40);
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    /**
     * Draws an error message when data loading fails.
     * @param guiGraphics The graphics context for rendering
     */
    private void drawErrorMessage(GuiGraphics guiGraphics) {
        int y = contentAreaY + MARGIN;

        drawScaledText(guiGraphics, "Error loading cultivation data:", windowX + MARGIN, y, 0xFFFF0000);
        y += LINE_HEIGHT + 4;

        if (errorMessage != null) {
            drawScaledText(guiGraphics, errorMessage, windowX + MARGIN, y, 0xFFFFAAAA);
        }
    }

    /**
     * Formats large numbers with comma separators for better readability.
     * @param number The number to format
     * @return Formatted number string with comma separators (e.g., "1,234,567")
     */
    private String formatNumber(long number) {
        return String.format("%,d", number);
    }

    /**
     * Formats large decimal numbers with comma separators for better readability.
     * @param number The decimal number to format
     * @param decimalPlaces The number of decimal places to show
     * @return Formatted number string with comma separators (e.g., "1,234.5")
     */
    private String formatNumber(double number, int decimalPlaces) {
        String format = "%,." + decimalPlaces + "f";
        return String.format(format, number);
    }

    /**
     * Formats decimal numbers with comma separators, automatically choosing precision.
     * @param number The decimal number to format
     * @return Formatted number string with appropriate decimal places
     */
    private String formatNumber(double number) {
        if (number == Math.floor(number)) {
            // If it's a whole number, show no decimals
            return String.format("%,.0f", number);
        } else {
            // Show 1 decimal place for non-whole numbers
            return String.format("%,.1f", number);
        }
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
    /**
     * Renders text with proper scaling to prevent blur
     * @param guiGraphics The graphics context for rendering
     * @param text The text to render
     * @param x The X position
     * @param y The Y position
     * @param color The text color
     */
    private void drawScaledText(GuiGraphics guiGraphics, String text, int x, int y, int color) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(1.0f, 1.0f, 1.0f); // No scaling for now

        guiGraphics.drawString(font, text, x, y, color, false);

        guiGraphics.pose().popPose();
    }

    private double getGuiScale() {
        return minecraft.getWindow().getGuiScale();
    }

    private int alignToPixel(double coordinate) {
        double scale = getGuiScale();
        return (int) Math.round(coordinate / scale) * (int) scale;
    }

    private void enableSharpRendering() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
    }

    private void restoreNormalRendering() {
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
    }
}
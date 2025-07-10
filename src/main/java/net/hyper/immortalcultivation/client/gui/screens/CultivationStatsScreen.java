package net.hyper.immortalcultivation.client.gui.screens;

import net.hyper.immortalcultivation.common.attachments.*;
import net.hyper.immortalcultivation.common.interfaces.ICultivationData;
import net.hyper.immortalcultivation.core.cultivation.realms.CultivationRealm;
import net.hyper.immortalcultivation.core.network.ICModPacketRegistry;
import net.hyper.immortalcultivation.core.network.packets.client.UnifiedActionRequestPacket;
import net.hyper.immortalcultivation.common.registry.ICModAttachmentTypes;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import com.mojang.blaze3d.systems.RenderSystem;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.opengl.GL11;

/**
 * Tabbed Cultivation Status Screen for organized information display
 * Scales properly across different GUI scales and organizes content into logical tabs
 */
public class CultivationStatsScreen extends Screen {

    // === TAB SYSTEM ===
    public enum Tab {
        OVERVIEW("Overview"),
        CULTIVATION("Cultivation"),
        RESOURCES("Resources"),
        DETAILS("Details");

        private final String displayName;

        Tab(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() { return displayName; }
    }

    private Tab currentTab = Tab.OVERVIEW;

    // === RESPONSIVE SIZING SYSTEM ===
    private static final int PREFERRED_WIDTH = 500;
    private static final int PREFERRED_HEIGHT = 480;
    private static final int MIN_WIDTH = 400;
    private static final int MIN_HEIGHT = 350;
    private static final int MAX_WIDTH = 700;
    private static final int MAX_HEIGHT = 600;
    private static final float MARGIN_PERCENTAGE = 0.02f;
    private static final int MIN_MARGIN = 8;
    private static final int MAX_MARGIN = 20;

    // === LAYOUT CONSTANTS ===
    private int windowWidth;
    private int windowHeight;
    private int windowX, windowY;
    private int margin;
    private int sectionSpacing;
    private int lineHeight;
    private int progressBarWidth;
    private int columnWidth;
    private int tabHeight;
    private int contentAreaY;
    private int contentAreaHeight;

    // === VISUAL CONSTANTS ===
    private static final int QI_COLOR = 0xFF87CEEB;
    private static final int BODY_COLOR = 0xFFFF6347;
    private static final int HEALTH_COLOR = 0xFFFF69B4;
    private static final int HEALTH_REGEN_COLOR = 0xFF98FB98; // Light green for health regen
    private static final int FOCUS_ACTIVE_COLOR = 0xFFFFD700;
    private static final int READY_COLOR = 0xFF90EE90;
    private static final int NOT_READY_COLOR = 0xFFFFB6C1;
    private static final int SECTION_HEADER_COLOR = 0xFFDDA0DD;
    private static final int RACE_COLOR = 0xFFFFD700;
    private static final int PROGRESS_BAR_HEIGHT = 8;
    private static final int TAB_ACTIVE_COLOR = 0xFF4A4A4A;
    private static final int TAB_INACTIVE_COLOR = 0xFF2A2A2A;
    private static final int TAB_BORDER_COLOR = 0xFF666666;

    // === PLAYER DATA ===
    private PlayerCultivationAttachment cultivationAttachment;
    private PlayerResourceAttachment resourceAttachment;
    private PlayerTalentAttachment talentAttachment;
    private PlayerRaceAttachment raceAttachment;
    private PlayerCombatStatsAttachment combatStatsAttachment;

    public CultivationStatsScreen() {
        super(Component.literal("Cultivation Status"));
    }

    // === PIXEL-PERFECT RENDERING HELPERS ===

    private double getGuiScale() {
        return minecraft.getWindow().getGuiScale();
    }

    private int alignToPixel(double coordinate) {
        double scale = getGuiScale();
        return (int) Math.round(coordinate / scale) * (int) scale;
    }

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

    // === SHARP RENDERING SYSTEM ===

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

    @Override
    protected void init() {
        super.init();
        if (!isSubscribed) {
            NeoForge.EVENT_BUS.register(this);
            isSubscribed = true;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        enableSharpRendering();

        try {
            renderBackground(guiGraphics, mouseX, mouseY, partialTicks);
            renderWindowFrame(guiGraphics);
            renderTabs(guiGraphics, mouseX, mouseY);
            renderCurrentTabContent(guiGraphics);
            super.render(guiGraphics, mouseX, mouseY, partialTicks);
        } finally {
            restoreNormalRendering();
        }
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, 0x80000000);
    }

    // === LAYOUT CALCULATION ===

    private void calculateResponsiveDimensions() {
        // Calculate available space
        int availableWidth = width - 40;
        int availableHeight = height - 40;

        // Start with preferred size
        windowWidth = PREFERRED_WIDTH;
        windowHeight = PREFERRED_HEIGHT;

        // Scale down if needed
        if (windowWidth > availableWidth || windowHeight > availableHeight) {
            float widthScale = (float) availableWidth / PREFERRED_WIDTH;
            float heightScale = (float) availableHeight / PREFERRED_HEIGHT;
            float scale = Math.min(widthScale, heightScale);

            windowWidth = (int) (PREFERRED_WIDTH * scale);
            windowHeight = (int) (PREFERRED_HEIGHT * scale);
        }

        // Apply constraints
        windowWidth = Mth.clamp(windowWidth, MIN_WIDTH, Math.min(MAX_WIDTH, availableWidth));
        windowHeight = Mth.clamp(windowHeight, MIN_HEIGHT, Math.min(MAX_HEIGHT, availableHeight));

        // Align to pixels
        windowWidth = alignToPixel(windowWidth);
        windowHeight = alignToPixel(windowHeight);

        // Center the window
        windowX = alignToPixel((width - windowWidth) / 2.0);
        windowY = alignToPixel((height - windowHeight) / 2.0);
    }

    private void calculateLayoutConstants() {
        // Calculate responsive margins and spacing
        int rawMargin = Mth.clamp(
            (int) (Math.min(windowWidth, windowHeight) * MARGIN_PERCENTAGE),
            MIN_MARGIN,
            MAX_MARGIN
        );
        margin = alignToPixel(rawMargin);

        int rawLineHeight = Math.max(10, Math.min(16, windowHeight / 30));
        lineHeight = alignToPixel(rawLineHeight);

        int rawSectionSpacing = Math.max(10, lineHeight);
        sectionSpacing = alignToPixel(rawSectionSpacing);

        int rawProgressBarWidth = Math.min(180, (windowWidth - 3 * margin) / 2);
        progressBarWidth = alignToPixel(rawProgressBarWidth);

        int rawColumnWidth = (windowWidth - 3 * margin) / 2;
        columnWidth = alignToPixel(rawColumnWidth);

        // Tab system layout
        tabHeight = alignToPixel(Math.max(24, lineHeight + 8));
        contentAreaY = windowY + margin + tabHeight + 5;
        contentAreaHeight = windowHeight - margin - tabHeight - 45; // Leave room for buttons
    }

    private void loadCultivationData() {
        if (minecraft.player != null) {
            cultivationAttachment = minecraft.player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
            resourceAttachment = minecraft.player.getData(ICModAttachmentTypes.PLAYER_RESOURCES.get());
            talentAttachment = minecraft.player.getData(ICModAttachmentTypes.PLAYER_TALENTS.get());
            raceAttachment = minecraft.player.getData(ICModAttachmentTypes.PLAYER_RACE.get());
            combatStatsAttachment = minecraft.player.getData(ICModAttachmentTypes.PLAYER_COMBAT_STATS.get());

            // Set player references
            if (cultivationAttachment != null) cultivationAttachment.setPlayer(minecraft.player);
            if (resourceAttachment != null) resourceAttachment.setPlayer(minecraft.player);
            if (talentAttachment != null) talentAttachment.setPlayer(minecraft.player);
            if (raceAttachment != null) raceAttachment.setPlayer(minecraft.player);
        }
    }

    // === INTERFACE SETUP ===

    private void setupInterface() {
        setupTabButtons();
        setupActionButtons();
    }

    private void setupTabButtons() {
        Tab[] tabs = Tab.values();
        int tabWidth = alignToPixel((windowWidth - 2 * margin) / tabs.length);

        for (int i = 0; i < tabs.length; i++) {
            Tab tab = tabs[i];
            int tabX = windowX + margin + (i * tabWidth);
            int tabY = windowY + margin;

            addRenderableWidget(Button.builder(
                    Component.literal(tab.getDisplayName()),
                    button -> switchToTab(tab))
                .bounds(tabX, tabY, tabWidth, tabHeight)
                .build());
        }
    }

    private void setupActionButtons() {
        int buttonHeight = alignToPixel(Math.max(20, lineHeight + 4));
        int buttonWidth = alignToPixel(Math.max(60, windowWidth / 8));
        int bottomY = windowY + windowHeight - margin - buttonHeight;

        // Close button
        int closeX = windowX + windowWidth - margin - buttonWidth;
        addRenderableWidget(Button.builder(
                Component.literal("Close"),
                button -> onClose())
            .bounds(closeX, bottomY, buttonWidth, buttonHeight)
            .build());

        // Focus toggle button (if cultivation data available)
        if (cultivationAttachment != null) {
            int focusWidth = alignToPixel(Math.max(120, windowWidth / 4));
            int focusX = closeX - focusWidth - 5;

            String switchTo = cultivationAttachment.getCurrentFocus() == PlayerCultivationAttachment.CultivationType.QI ? "Body" : "Qi";
            addRenderableWidget(Button.builder(
                    Component.literal("Focus: " + switchTo),
                    button -> switchCultivationFocus())
                .bounds(focusX, bottomY, focusWidth, buttonHeight)
                .build());
        }

        // Refresh button
        int refreshWidth = alignToPixel(Math.max(60, windowWidth / 10));
        int refreshX = windowX + margin;
        addRenderableWidget(Button.builder(
                Component.literal("Refresh"),
                button -> refreshStats())
            .bounds(refreshX, bottomY, refreshWidth, buttonHeight)
            .build());
    }

    // === RENDERING ===

    private void renderWindowFrame(GuiGraphics guiGraphics) {
        // Main window background
        guiGraphics.fill(windowX, windowY, windowX + windowWidth, windowY + windowHeight, 0xFF2C2C2C);

        // Window border
        int borderColor = 0xFFFFFFFF;
        guiGraphics.fill(windowX - 1, windowY - 1, windowX + windowWidth + 1, windowY, borderColor);
        guiGraphics.fill(windowX - 1, windowY + windowHeight, windowX + windowWidth + 1, windowY + windowHeight + 1, borderColor);
        guiGraphics.fill(windowX - 1, windowY, windowX, windowY + windowHeight, borderColor);
        guiGraphics.fill(windowX + windowWidth, windowY, windowX + windowWidth + 1, windowY + windowHeight, borderColor);
    }

    private void renderTabs(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Tab[] tabs = Tab.values();
        int tabWidth = alignToPixel((windowWidth - 2 * margin) / tabs.length);

        for (int i = 0; i < tabs.length; i++) {
            Tab tab = tabs[i];
            int tabX = windowX + margin + (i * tabWidth);
            int tabY = windowY + margin;

            // Tab background
            int tabBgColor = (tab == currentTab) ? TAB_ACTIVE_COLOR : TAB_INACTIVE_COLOR;
            guiGraphics.fill(tabX, tabY, tabX + tabWidth, tabY + tabHeight, tabBgColor);

            // Tab border
            if (tab == currentTab) {
                guiGraphics.fill(tabX, tabY, tabX + tabWidth, tabY + 1, TAB_BORDER_COLOR);
                guiGraphics.fill(tabX, tabY, tabX + 1, tabY + tabHeight, TAB_BORDER_COLOR);
                guiGraphics.fill(tabX + tabWidth - 1, tabY, tabX + tabWidth, tabY + tabHeight, TAB_BORDER_COLOR);
            }

            // Tab text
            String tabText = tab.getDisplayName();
            int textWidth = font.width(tabText);
            int textX = tabX + (tabWidth - textWidth) / 2;
            int textY = tabY + (tabHeight - 8) / 2;
            int textColor = (tab == currentTab) ? 0xFFFFFFFF : 0xFFCCCCCC;
            drawStringAligned(guiGraphics, tabText, textX, textY, textColor);
        }
    }

    private void renderCurrentTabContent(GuiGraphics guiGraphics) {
        if (cultivationAttachment == null) {
            renderErrorState(guiGraphics);
            return;
        }

        switch (currentTab) {
            case OVERVIEW -> renderOverviewTab(guiGraphics);
            case CULTIVATION -> renderCultivationTab(guiGraphics);
            case RESOURCES -> renderResourcesTab(guiGraphics);
            case DETAILS -> renderDetailsTab(guiGraphics);
        }
    }

    // === TAB CONTENT RENDERING ===

    private void renderOverviewTab(GuiGraphics guiGraphics) {
        int currentY = contentAreaY;

        // Character overview
        currentY = renderSectionHeader(guiGraphics, "Character Overview", currentY);
        currentY += 5;

        // Race info
        if (raceAttachment != null && raceAttachment.hasRaceAssigned()) {
            String raceText = String.format("Race: %s", raceAttachment.getRaceType().getDisplayName());
            drawStringAligned(guiGraphics, raceText, windowX + margin, currentY, RACE_COLOR);
            currentY += lineHeight;

            String bonusText = raceAttachment.getRaceType().getDescription();
            if (!bonusText.equals("No bonuses or penalties")) {
                drawStringAligned(guiGraphics, "  " + bonusText, windowX + margin, currentY, 0xFFCCCCCC);
                currentY += lineHeight;
            }
        }

        // Spiritual roots and physique
        if (talentAttachment != null) {
            if (talentAttachment.hasSpiritualRootAssigned()) {
                String rootText = String.format("Spiritual Root: %s (×%.1f)",
                    talentAttachment.getSpiritualRootType().getDisplayName(),
                    talentAttachment.getSpiritualRootMultiplier());
                drawStringAligned(guiGraphics, rootText, windowX + margin, currentY, QI_COLOR);
                currentY += lineHeight;
            }

            if (talentAttachment.hasPhysiqueAssigned()) {
                String physiqueText = String.format("Physique: %s (×%.1f)",
                    talentAttachment.getPhysiqueType().getDisplayName(),
                    talentAttachment.getPhysiqueMultiplier());
                drawStringAligned(guiGraphics, physiqueText, windowX + margin, currentY, BODY_COLOR);
                currentY += lineHeight;
            }
        }

        currentY += sectionSpacing;

        // Current status
        currentY = renderSectionHeader(guiGraphics, "Current Status", currentY);
        currentY += 5;

        // Focus and realms
        PlayerCultivationAttachment.CultivationType focus = cultivationAttachment.getCurrentFocus();
        String focusText = "Focus: " + focus.getDisplayName() + " Cultivation";
        int focusColor = focus == PlayerCultivationAttachment.CultivationType.QI ? QI_COLOR : BODY_COLOR;
        drawStringAligned(guiGraphics, focusText, windowX + margin, currentY, focusColor);
        currentY += lineHeight;

        CultivationRealm qiRealm = cultivationAttachment.getRealm(ICultivationData.CultivationType.QI);
        CultivationRealm bodyRealm = cultivationAttachment.getRealm(ICultivationData.CultivationType.BODY);
        String realmsText = String.format("Realms: Qi %s | Body %s", qiRealm.getDisplayName(), bodyRealm.getDisplayName());
        drawStringAligned(guiGraphics, realmsText, windowX + margin, currentY, 0xFFFFFFFF);
        currentY += lineHeight;

        // Cultivation rates
        double qiRate = cultivationAttachment.getEffectiveCultivationRate(ICultivationData.CultivationType.QI);
        double bodyRate = cultivationAttachment.getEffectiveCultivationRate(ICultivationData.CultivationType.BODY);
        String ratesText = String.format("Rates: Qi %.2f/s | Body %.2f/s", qiRate, bodyRate);
        drawStringAligned(guiGraphics, ratesText, windowX + margin, currentY, 0xFFCCCCCC);
    }

    private void renderCultivationTab(GuiGraphics guiGraphics) {
        int currentY = contentAreaY;

        currentY = renderSectionHeader(guiGraphics, "Cultivation Progress", currentY);
        currentY += 10;

        int leftColumnX = windowX + margin;
        int rightColumnX = windowX + margin + columnWidth + 10;

        renderCultivationColumn(guiGraphics, leftColumnX, currentY, PlayerCultivationAttachment.CultivationType.QI);
        renderCultivationColumn(guiGraphics, rightColumnX, currentY, PlayerCultivationAttachment.CultivationType.BODY);
    }

    private void renderResourcesTab(GuiGraphics guiGraphics) {
        int currentY = contentAreaY;

        if (resourceAttachment == null) {
            drawStringAligned(guiGraphics, "Resource data unavailable", windowX + margin, currentY, 0xFFFF6666);
            return;
        }

        currentY = renderSectionHeader(guiGraphics, "Resource Status", currentY);
        currentY += 10;

        // Health
        long currentHealth = resourceAttachment.getCurrentHealth();
        long maxHealth = resourceAttachment.getMaxHealth();
        double healthPercentage = maxHealth > 0 ? (double) currentHealth / maxHealth : 0.0;

        String healthText = String.format("Health: %d/%d (%.1f%%)", currentHealth, maxHealth, healthPercentage * 100);
        drawStringAligned(guiGraphics, healthText, windowX + margin, currentY, HEALTH_COLOR);
        currentY += lineHeight + 2;

        int barX = windowX + margin;
        renderProgressBar(guiGraphics, barX, currentY, progressBarWidth, PROGRESS_BAR_HEIGHT, healthPercentage, HEALTH_COLOR);
        currentY += PROGRESS_BAR_HEIGHT + 3;

        // Health regeneration rate
        double healthRegenRate = resourceAttachment.getEffectiveHealthRegenerationRate();
        String healthRegenText = String.format("Health Regen: %.3f/s", healthRegenRate);
        drawStringAligned(guiGraphics, healthRegenText, windowX + margin, currentY, HEALTH_REGEN_COLOR);
        currentY += lineHeight + sectionSpacing;

        // Qi Energy
        long currentQi = resourceAttachment.getCurrentQi();
        long maxQi = resourceAttachment.getMaxQi();
        double qiPercentage = maxQi > 0 ? (double) currentQi / maxQi : 0.0;

        String qiText = String.format("Qi Energy: %d/%d (%.1f%%)", currentQi, maxQi, qiPercentage * 100);
        drawStringAligned(guiGraphics, qiText, windowX + margin, currentY, QI_COLOR);
        currentY += lineHeight + 2;

        renderProgressBar(guiGraphics, barX, currentY, progressBarWidth, PROGRESS_BAR_HEIGHT, qiPercentage, QI_COLOR);
        currentY += PROGRESS_BAR_HEIGHT + sectionSpacing;

        // Body Energy
        long currentBody = resourceAttachment.getCurrentBody();
        long maxBody = resourceAttachment.getMaxBody();
        double bodyPercentage = maxBody > 0 ? (double) currentBody / maxBody : 0.0;

        String bodyText = String.format("Body Energy: %d/%d (%.1f%%)", currentBody, maxBody, bodyPercentage * 100);
        drawStringAligned(guiGraphics, bodyText, windowX + margin, currentY, BODY_COLOR);
        currentY += lineHeight + 2;

        renderProgressBar(guiGraphics, barX, currentY, progressBarWidth, PROGRESS_BAR_HEIGHT, bodyPercentage, BODY_COLOR);
    }

    private void renderDetailsTab(GuiGraphics guiGraphics) {
        int currentY = contentAreaY;

        currentY = renderSectionHeader(guiGraphics, "Detailed Information", currentY);
        currentY += 10;

        // Race details - Always show race information if attachment exists
        if (raceAttachment != null) {
            currentY = renderSectionHeader(guiGraphics, "Race Details", currentY);
            currentY += 5;

            // Race name
            String raceName = raceAttachment.hasRaceAssigned() ?
                raceAttachment.getRaceType().getDisplayName() : "Not Assigned";
            String raceText = "Current Race: " + raceName;
            drawStringAligned(guiGraphics, raceText, windowX + margin, currentY, RACE_COLOR);
            currentY += lineHeight + 3;

            if (raceAttachment.hasRaceAssigned()) {
                // Race description
                String raceDesc = raceAttachment.getRaceType().getDescription();
                if (raceDesc != null && !raceDesc.isEmpty()) {
                    // Word wrap the description if it's too long
                    int maxWidth = windowWidth - 2 * margin - 20;
                    if (font.width(raceDesc) > maxWidth) {
                        // Simple word wrapping
                        String[] words = raceDesc.split(" ");
                        StringBuilder currentLine = new StringBuilder();

                        for (String word : words) {
                            if (font.width(currentLine + " " + word) > maxWidth) {
                                if (currentLine.length() > 0) {
                                    drawStringAligned(guiGraphics, currentLine.toString(), windowX + margin, currentY, 0xFFCCCCCC);
                                    currentY += lineHeight;
                                    currentLine = new StringBuilder();
                                }
                            }
                            if (currentLine.length() > 0) currentLine.append(" ");
                            currentLine.append(word);
                        }
                        if (currentLine.length() > 0) {
                            drawStringAligned(guiGraphics, currentLine.toString(), windowX + margin, currentY, 0xFFCCCCCC);
                            currentY += lineHeight;
                        }
                    } else {
                        drawStringAligned(guiGraphics, raceDesc, windowX + margin, currentY, 0xFFCCCCCC);
                        currentY += lineHeight;
                    }
                    currentY += 5;
                }

                // Race bonus/penalty description
                String bonusDesc = raceAttachment.getRaceType().getBonusDescription();
                if (bonusDesc != null && !bonusDesc.isEmpty()) {
                    String bonusText = "Racial Effects: " + bonusDesc;
                    drawStringAligned(guiGraphics, bonusText, windowX + margin, currentY, 0xFFAADDFF);
                    currentY += lineHeight + 3;
                }

                // Detailed multipliers
                String multipliersText = String.format("Cultivation Multipliers: Qi ×%.1f | Body ×%.1f",
                    raceAttachment.getQiMultiplier(), raceAttachment.getBodyMultiplier());
                drawStringAligned(guiGraphics, multipliersText, windowX + margin, currentY, 0xFF999999);
                currentY += lineHeight;

                // Show if race has bonuses or penalties
                boolean hasBonus = raceAttachment.getRaceType().hasBonus();
                boolean hasPenalty = raceAttachment.getRaceType().hasPenalty();

                if (hasBonus || hasPenalty) {
                    String effectType = "";
                    int effectColor = 0xFFCCCCCC;

                    if (hasBonus && hasPenalty) {
                        effectType = "Mixed Effects: Both bonuses and penalties";
                        effectColor = 0xFFFFDD77;
                    } else if (hasBonus) {
                        effectType = "Beneficial Race: Provides cultivation bonuses";
                        effectColor = 0xFF77FF77;
                    } else if (hasPenalty) {
                        effectType = "Challenging Race: Has cultivation penalties";
                        effectColor = 0xFFFF7777;
                    }

                    drawStringAligned(guiGraphics, effectType, windowX + margin, currentY, effectColor);
                    currentY += lineHeight;
                }

                currentY += sectionSpacing;
            } else {
                // Show message if no race assigned
                drawStringAligned(guiGraphics, "No race has been assigned to this character.", windowX + margin, currentY, 0xFFFF9999);
                drawStringAligned(guiGraphics, "Race assignment may be required for cultivation.", windowX + margin, currentY + lineHeight, 0xFFCCCCCC);
                currentY += lineHeight * 2 + sectionSpacing;
            }
        } else {
            // Show error if race attachment is missing
            currentY = renderSectionHeader(guiGraphics, "Race Information", currentY);
            currentY += 5;
            drawStringAligned(guiGraphics, "Race data unavailable - attachment missing", windowX + margin, currentY, 0xFFFF6666);
            currentY += lineHeight + sectionSpacing;
        }

        // Cultivation rates breakdown
        currentY = renderSectionHeader(guiGraphics, "Cultivation Rate Breakdown", currentY);
        currentY += 5;

        double qiBase = cultivationAttachment.getBaseCultivation(ICultivationData.CultivationType.QI);
        double qiEffective = cultivationAttachment.getEffectiveCultivationRate(ICultivationData.CultivationType.QI);
        double bodyBase = cultivationAttachment.getBaseCultivation(ICultivationData.CultivationType.BODY);
        double bodyEffective = cultivationAttachment.getEffectiveCultivationRate(ICultivationData.CultivationType.BODY);

        String qiBreakdown = String.format("Qi: %.3f base → %.3f effective (×%.2f total)", qiBase, qiEffective, qiEffective / qiBase);
        drawStringAligned(guiGraphics, qiBreakdown, windowX + margin, currentY, QI_COLOR);
        currentY += lineHeight;

        String bodyBreakdown = String.format("Body: %.3f base → %.3f effective (×%.2f total)", bodyBase, bodyEffective, bodyEffective / bodyBase);
        drawStringAligned(guiGraphics, bodyBreakdown, windowX + margin, currentY, BODY_COLOR);
        currentY += lineHeight + 5;

        // Show multiplier breakdown if race affects rates
        if (raceAttachment != null && raceAttachment.hasRaceAssigned()) {
            float qiRaceMultiplier = raceAttachment.getQiMultiplier();
            float bodyRaceMultiplier = raceAttachment.getBodyMultiplier();

            if (qiRaceMultiplier != 1.0f || bodyRaceMultiplier != 1.0f) {
                drawStringAligned(guiGraphics, "Rate Multiplier Sources:", windowX + margin, currentY, 0xFFDDDDDD);
                currentY += lineHeight;

                if (qiRaceMultiplier != 1.0f) {
                    String qiRaceText = String.format("  Qi from Race (%s): ×%.1f",
                        raceAttachment.getRaceType().getDisplayName(), qiRaceMultiplier);
                    int qiRaceColor = qiRaceMultiplier > 1.0f ? 0xFF77FF77 : 0xFFFF7777;
                    drawStringAligned(guiGraphics, qiRaceText, windowX + margin, currentY, qiRaceColor);
                    currentY += lineHeight;
                }

                if (bodyRaceMultiplier != 1.0f) {
                    String bodyRaceText = String.format("  Body from Race (%s): ×%.1f",
                        raceAttachment.getRaceType().getDisplayName(), bodyRaceMultiplier);
                    int bodyRaceColor = bodyRaceMultiplier > 1.0f ? 0xFF77FF77 : 0xFFFF7777;
                    drawStringAligned(guiGraphics, bodyRaceText, windowX + margin, currentY, bodyRaceColor);
                    currentY += lineHeight;
                }

                // Show other multiplier sources
                if (talentAttachment != null) {
                    if (talentAttachment.hasSpiritualRootAssigned()) {
                        String rootText = String.format("  Qi from Spiritual Root (%s): ×%.1f",
                            talentAttachment.getSpiritualRootType().getDisplayName(),
                            talentAttachment.getSpiritualRootMultiplier());
                        drawStringAligned(guiGraphics, rootText, windowX + margin, currentY, QI_COLOR);
                        currentY += lineHeight;
                    }

                    if (talentAttachment.hasPhysiqueAssigned()) {
                        String physiqueText = String.format("  Body from Physique (%s): ×%.1f",
                            talentAttachment.getPhysiqueType().getDisplayName(),
                            talentAttachment.getPhysiqueMultiplier());
                        drawStringAligned(guiGraphics, physiqueText, windowX + margin, currentY, BODY_COLOR);
                        currentY += lineHeight;
                    }
                }

                // Show focus bonus
                PlayerCultivationAttachment.CultivationType currentFocus = cultivationAttachment.getCurrentFocus();
                String focusText = String.format("  Focus Bonus (%s): ×2.0", currentFocus.getDisplayName());
                int focusColor = currentFocus == PlayerCultivationAttachment.CultivationType.QI ? QI_COLOR : BODY_COLOR;
                drawStringAligned(guiGraphics, focusText, windowX + margin, currentY, focusColor);
            }
        }
    }

    // === HELPER RENDERING METHODS ===

    private void renderCultivationColumn(GuiGraphics guiGraphics, int x, int y, PlayerCultivationAttachment.CultivationType type) {
        int currentY = y;
        int typeColor = type == PlayerCultivationAttachment.CultivationType.QI ? QI_COLOR : BODY_COLOR;
        String typeName = type.getDisplayName();

        drawStringAligned(guiGraphics, typeName + " Cultivation", x, currentY, typeColor);
        currentY += lineHeight + 3;

        CultivationRealm currentRealm = type == PlayerCultivationAttachment.CultivationType.QI ?
            cultivationAttachment.getRealm(ICultivationData.CultivationType.QI) : cultivationAttachment.getRealm(ICultivationData.CultivationType.BODY);
        drawStringAligned(guiGraphics, "Realm: " + currentRealm.getDisplayName(), x, currentY, 0xFFFFFFFF);
        currentY += lineHeight;

        long current, max;
        double baseRate, effectiveRate;
        double percentage;

        if (type == PlayerCultivationAttachment.CultivationType.QI) {
            current = cultivationAttachment.getCurrentCultivation(ICultivationData.CultivationType.QI);
            max = cultivationAttachment.getMaxCultivation(ICultivationData.CultivationType.QI);
            baseRate = cultivationAttachment.getBaseCultivationRate(ICultivationData.CultivationType.QI);
            effectiveRate = cultivationAttachment.getEffectiveCultivationRate(ICultivationData.CultivationType.QI);
            percentage = cultivationAttachment.getProgress(ICultivationData.CultivationType.QI);
        } else {
            current = cultivationAttachment.getCurrentCultivation(ICultivationData.CultivationType.BODY);
            max = cultivationAttachment.getMaxCultivation(ICultivationData.CultivationType.BODY);
            baseRate = cultivationAttachment.getBaseCultivationRate(ICultivationData.CultivationType.BODY);
            effectiveRate = cultivationAttachment.getEffectiveCultivationRate(ICultivationData.CultivationType.BODY);
            percentage = cultivationAttachment.getProgress(ICultivationData.CultivationType.BODY);
        }

        String progressText = String.format("%.1f%% (%d/%d)", percentage * 100, current, max);
        drawStringAligned(guiGraphics, progressText, x, currentY, 0xFFCCCCCC);
        currentY += lineHeight + 2;

        int barWidth = Math.min(progressBarWidth, columnWidth - 10);
        renderProgressBar(guiGraphics, x, currentY, barWidth, PROGRESS_BAR_HEIGHT, percentage, typeColor);
        currentY += PROGRESS_BAR_HEIGHT + 5;

        String baseRateText = String.format("Base: %.3f/s", baseRate);
        drawStringAligned(guiGraphics, baseRateText, x, currentY, 0xFF999999);
        currentY += lineHeight;

        boolean isFocused = cultivationAttachment.getCurrentFocus() == type;
        double totalMultiplier = effectiveRate / baseRate;
        String effectiveRateText;
        if (Math.abs(totalMultiplier - 1.0) > 0.01) {
            effectiveRateText = String.format("Effective: %.3f/s (×%.2f)", effectiveRate, totalMultiplier);
        } else {
            effectiveRateText = String.format("Effective: %.3f/s", effectiveRate);
        }

        if (isFocused) effectiveRateText += " [FOCUSED]";

        int rateColor = isFocused ? FOCUS_ACTIVE_COLOR : 0xFFCCCCCC;
        drawStringAligned(guiGraphics, effectiveRateText, x, currentY, rateColor);
    }

    private int renderSectionHeader(GuiGraphics guiGraphics, String title, int y) {
        drawStringAligned(guiGraphics, title, windowX + margin, y, SECTION_HEADER_COLOR);

        int lineY = y + lineHeight + 2;
        int lineWidth = font.width(title) + 20;
        guiGraphics.fill(windowX + margin, lineY, windowX + margin + lineWidth, lineY + 1, SECTION_HEADER_COLOR);

        return lineY + 5;
    }

    private void renderProgressBar(GuiGraphics guiGraphics, int x, int y, int width, int height, double percentage, int color) {
        // Background
        guiGraphics.fill(x, y, x + width, y + height, 0xFF333333);

        // Progress fill
        if (percentage > 0) {
            int fillWidth = (int) (width * Math.min(1.0, percentage));
            guiGraphics.fill(x, y, x + fillWidth, y + height, color);
        }

        // Border
        guiGraphics.fill(x - 1, y - 1, x + width + 1, y, 0xFFFFFFFF);
        guiGraphics.fill(x - 1, y + height, x + width + 1, y + height + 1, 0xFFFFFFFF);
        guiGraphics.fill(x - 1, y, x, y + height, 0xFFFFFFFF);
        guiGraphics.fill(x + width, y, x + width + 1, y + height, 0xFFFFFFFF);
    }

    private void renderErrorState(GuiGraphics guiGraphics) {
        String errorMsg = "Cultivation data not available";
        int textWidth = font.width(errorMsg);
        int textX = windowX + (windowWidth - textWidth) / 2;
        int textY = contentAreaY + 50;
        drawStringAligned(guiGraphics, errorMsg, textX, textY, 0xFFFF6666);
    }

    // === TAB SWITCHING ===

    private void switchToTab(Tab tab) {
        this.currentTab = tab;
        // Clear existing widgets and rebuild interface
        clearWidgets();
        setupInterface();
    }

    // === ACTION METHODS ===

    private void switchCultivationFocus() {
        if (cultivationAttachment != null) {
            ICModPacketRegistry.sendToServer(UnifiedActionRequestPacket.createFocusToggle());
            // Rebuild interface to update button text
            clearWidgets();
            setupInterface();
        }
    }

    private void refreshStats() {
        // No need for forceSync() - NetworkSyncManager handles server-side sync automatically
        // On client-side, just refresh the UI by re-initializing to read current attachment data
        init();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
package net.hyper.immortalcultivation.client.gui.screens;

import net.hyper.immortalcultivation.core.cultivation.races.RaceType;
import net.hyper.immortalcultivation.core.network.ICModPacketRegistry;
import net.hyper.immortalcultivation.core.network.packets.client.RaceSelectionRequestPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

/**
 * Screen for selecting a cultivation race when first joining a world.
 * Provides detailed information about each race's bonuses and penalties.
 */
public class RaceSelectionScreen extends Screen {

    // Layout constants
    private static final int WINDOW_WIDTH = 500;
    private static final int WINDOW_HEIGHT = 400;
    private static final int RACE_BUTTON_WIDTH = 140;
    private static final int RACE_BUTTON_HEIGHT = 80;
    private static final int BUTTON_SPACING = 20;
    private static final int CONFIRM_BUTTON_WIDTH = 120;
    private static final int CONFIRM_BUTTON_HEIGHT = 30;

    // Colors
    private static final int BACKGROUND_COLOR = 0xE0000000;
    private static final int WINDOW_COLOR = 0xFF2C2C2C;
    private static final int BORDER_COLOR = 0xFFFFFFFF;
    private static final int SELECTED_BORDER_COLOR = 0xFFFFD700;

    private int windowX, windowY;
    private RaceType selectedRace = null;
    private Button confirmButton;

    public RaceSelectionScreen() {
        super(Component.literal("Choose Your Race"));
    }

    @Override
    protected void init() {
        super.init();

        windowX = (width - WINDOW_WIDTH) / 2;
        windowY = (height - WINDOW_HEIGHT) / 2;

        // Create race selection buttons
        createRaceButtons();

        // Create confirm button (initially disabled)
        confirmButton = Button.builder(
                Component.literal("Confirm Selection"),
                button -> confirmSelection())
            .bounds(windowX + (WINDOW_WIDTH - CONFIRM_BUTTON_WIDTH) / 2,
                windowY + WINDOW_HEIGHT - 50,
                CONFIRM_BUTTON_WIDTH,
                CONFIRM_BUTTON_HEIGHT)
            .build();
        confirmButton.active = false;
        addRenderableWidget(confirmButton);
    }

    private void createRaceButtons() {
        int totalButtonWidth = (RACE_BUTTON_WIDTH * 3) + (BUTTON_SPACING * 2);
        int startX = windowX + (WINDOW_WIDTH - totalButtonWidth) / 2;
        int buttonY = windowY + 100;

        int x = startX;
        for (RaceType race : RaceType.values()) {
            addRenderableWidget(new RaceButton(x, buttonY, RACE_BUTTON_WIDTH, RACE_BUTTON_HEIGHT, race));
            x += RACE_BUTTON_WIDTH + BUTTON_SPACING;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        // Render dark overlay
        guiGraphics.fill(0, 0, width, height, BACKGROUND_COLOR);

        // Render window background
        guiGraphics.fill(windowX, windowY, windowX + WINDOW_WIDTH, windowY + WINDOW_HEIGHT, WINDOW_COLOR);

        // Render window border
        renderBorder(guiGraphics, windowX, windowY, WINDOW_WIDTH, WINDOW_HEIGHT, BORDER_COLOR, 2);

        // Render title
        Component title = Component.literal("CHOOSE YOUR CULTIVATION RACE")
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        int titleWidth = font.width(title);
        guiGraphics.drawString(font, title,
            windowX + (WINDOW_WIDTH - titleWidth) / 2,
            windowY + 20,
            0xFFFFFFFF);

        // Render subtitle
        Component subtitle = Component.literal("This choice is permanent!")
            .withStyle(ChatFormatting.GRAY);
        int subtitleWidth = font.width(subtitle);
        guiGraphics.drawString(font, subtitle,
            windowX + (WINDOW_WIDTH - subtitleWidth) / 2,
            windowY + 40,
            0xFFCCCCCC);

        // Render race details if one is selected
        if (selectedRace != null) {
            renderRaceDetails(guiGraphics);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    private void renderRaceDetails(GuiGraphics guiGraphics) {
        int detailsY = windowY + 220;
        int centerX = windowX + WINDOW_WIDTH / 2;

        // Race name
        Component raceName = Component.literal(selectedRace.getDisplayName())
            .withStyle(Style.EMPTY.withColor(selectedRace.getColor()).withBold(true));
        int nameWidth = font.width(raceName);
        guiGraphics.drawString(font, raceName, centerX - nameWidth / 2, detailsY, 0xFFFFFFFF);

        // Description
        Component description = Component.literal(selectedRace.getDescription())
            .withStyle(ChatFormatting.WHITE);
        drawCenteredWrappedText(guiGraphics, description, centerX, detailsY + 20, WINDOW_WIDTH - 60);

        // Bonuses/Penalties
        Component bonuses = Component.literal(selectedRace.getBonusDescription())
            .withStyle(selectedRace.hasBonus() ? ChatFormatting.GREEN : ChatFormatting.RED);
        int bonusWidth = font.width(bonuses);
        guiGraphics.drawString(font, bonuses, centerX - bonusWidth / 2, detailsY + 60, 0xFFFFFFFF);
    }

    private void drawCenteredWrappedText(GuiGraphics guiGraphics, Component text, int centerX, int y, int maxWidth) {
        // Simple text wrapping - in a real implementation, you'd want more sophisticated wrapping
        String fullText = text.getString();
        if (font.width(fullText) <= maxWidth) {
            int textWidth = font.width(text);
            guiGraphics.drawString(font, text, centerX - textWidth / 2, y, 0xFFFFFFFF);
        } else {
            // Split into multiple lines (simplified)
            String[] words = fullText.split(" ");
            StringBuilder currentLine = new StringBuilder();
            int currentY = y;

            for (String word : words) {
                if (font.width(currentLine + word) > maxWidth && currentLine.length() > 0) {
                    Component lineComponent = Component.literal(currentLine.toString().trim())
                        .withStyle(text.getStyle());
                    int lineWidth = font.width(lineComponent);
                    guiGraphics.drawString(font, lineComponent, centerX - lineWidth / 2, currentY, 0xFFFFFFFF);
                    currentY += 12;
                    currentLine = new StringBuilder();
                }
                currentLine.append(word).append(" ");
            }

            if (currentLine.length() > 0) {
                Component lineComponent = Component.literal(currentLine.toString().trim())
                    .withStyle(text.getStyle());
                int lineWidth = font.width(lineComponent);
                guiGraphics.drawString(font, lineComponent, centerX - lineWidth / 2, currentY, 0xFFFFFFFF);
            }
        }
    }

    private void renderBorder(GuiGraphics guiGraphics, int x, int y, int width, int height, int color, int thickness) {
        // Top
        guiGraphics.fill(x - thickness, y - thickness, x + width + thickness, y, color);
        // Bottom
        guiGraphics.fill(x - thickness, y + height, x + width + thickness, y + height + thickness, color);
        // Left
        guiGraphics.fill(x - thickness, y, x, y + height, color);
        // Right
        guiGraphics.fill(x + width, y, x + width + thickness, y + height, color);
    }

    private void selectRace(RaceType race) {
        this.selectedRace = race;
        this.confirmButton.active = true;
    }

    private void confirmSelection() {
        if (selectedRace != null) {
            // Send packet to server
            ICModPacketRegistry.sendToServer(new RaceSelectionRequestPacket(selectedRace.name()));
            // Close screen
            minecraft.setScreen(null);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false; // Force player to make a choice
    }

    /**
     * Custom button for race selection with visual feedback
     */
    private class RaceButton extends Button {
        private final RaceType race;

        public RaceButton(int x, int y, int width, int height, RaceType race) {
            super(x, y, width, height,
                Component.literal(race.getDisplayName()),
                button -> selectRace(race),
                DEFAULT_NARRATION);
            this.race = race;
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
            boolean selected = selectedRace == race;
            boolean hovered = isHoveredOrFocused();

            // Background
            int bgColor = selected ? 0xFF3A3A3A : (hovered ? 0xFF2F2F2F : 0xFF252525);
            guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, bgColor);

            // Border
            int borderColor = selected ? SELECTED_BORDER_COLOR : (hovered ? 0xFFAAAAAA : 0xFF666666);
            renderBorder(guiGraphics, getX(), getY(), width, height, borderColor, 1);

            // Race icon/color indicator
            int iconSize = 30;
            int iconX = getX() + (width - iconSize) / 2;
            int iconY = getY() + 10;
            guiGraphics.fill(iconX, iconY, iconX + iconSize, iconY + iconSize, race.getColor());
            renderBorder(guiGraphics, iconX, iconY, iconSize, iconSize, borderColor, 1);

            // Race name
            Component raceName = Component.literal(race.getDisplayName())
                .withStyle(selected ? ChatFormatting.GOLD : ChatFormatting.WHITE);
            int textWidth = font.width(raceName);
            guiGraphics.drawString(font, raceName,
                getX() + (width - textWidth) / 2,
                getY() + height - 20,
                0xFFFFFFFF);
        }
    }
}
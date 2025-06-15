package net.hyper.immortalcultivation.screen;

import net.hyper.immortalcultivation.attachment.PlayerCultivationAttachment;
import net.hyper.immortalcultivation.attachment.PlayerRealmAttachment;
import net.hyper.immortalcultivation.attachment.PlayerSpiritualRootsAttachment;
import net.hyper.immortalcultivation.cultivation.realms.CultivationRealm;
import net.hyper.immortalcultivation.registry.ICModAttachmentTypes;
import net.hyper.immortalcultivation.registry.ICModNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class CultivationBreakthroughScreen extends Screen {
    private static final int WINDOW_WIDTH = 300;
    private static final int WINDOW_HEIGHT = 400;

    private PlayerRealmAttachment realmAttachment;
    private PlayerCultivationAttachment cultivationAttachment;
    private PlayerSpiritualRootsAttachment spiritualRoots;

    private int windowX, windowY;
    private List<BreakthroughButton> breakthroughButtons = new ArrayList<>();

    public CultivationBreakthroughScreen() {
        super(Component.literal("Breakthrough"));
    }

    @Override
    protected void init() {
        super.init();

        // Center the window
        windowX = (width - WINDOW_WIDTH) / 2;
        windowY = (height - WINDOW_HEIGHT) / 2;

        // Get player data
        if (minecraft.player != null) {
            realmAttachment = minecraft.player.getData(ICModAttachmentTypes.PLAYER_REALM.get());
            cultivationAttachment = minecraft.player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
            spiritualRoots = minecraft.player.getData(ICModAttachmentTypes.PLAYER_SPIRITUAL_ROOT.get());
        }

        setupButtons();
    }

    private void setupButtons() {
        breakthroughButtons.clear();

        if (realmAttachment == null) return;

        // Close button
        addRenderableWidget(Button.builder(Component.literal("Close"),
                button -> onClose())
            .bounds(windowX + WINDOW_WIDTH - 60, windowY + 10, 50, 20)
            .build());

        // Get available breakthroughs
        List<PlayerRealmAttachment.BreakthroughOption> options = realmAttachment.getAvailableBreakthroughs();

        int yOffset = 120;
        for (PlayerRealmAttachment.BreakthroughOption option : options) {
            BreakthroughButton button = new BreakthroughButton(
                windowX + 20, windowY + yOffset,
                WINDOW_WIDTH - 40, 60,
                option
            );
            breakthroughButtons.add(button);
            addRenderableWidget(button);
            yOffset += 70;
        }

        // If no breakthrough options available
        if (options.isEmpty()) {
            Component text = realmAttachment.canAttemptBreakthrough() ?
                Component.literal("No breakthrough paths available") :
                Component.literal("Requirements not met for breakthrough");

            addRenderableWidget(Button.builder(text, button -> {})
                .bounds(windowX + 20, windowY + yOffset, WINDOW_WIDTH - 40, 20)
                .build());
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        // Dark background
        guiGraphics.fill(0, 0, width, height, 0x80000000);

        // Window background
        guiGraphics.fill(windowX, windowY, windowX + WINDOW_WIDTH, windowY + WINDOW_HEIGHT, 0xFF2C2C2C);

        // Window border
        guiGraphics.fill(windowX - 1, windowY - 1, windowX + WINDOW_WIDTH + 1, windowY, 0xFFFFFFFF);
        guiGraphics.fill(windowX - 1, windowY + WINDOW_HEIGHT, windowX + WINDOW_WIDTH + 1, windowY + WINDOW_HEIGHT + 1, 0xFFFFFFFF);
        guiGraphics.fill(windowX - 1, windowY, windowX, windowY + WINDOW_HEIGHT, 0xFFFFFFFF);
        guiGraphics.fill(windowX + WINDOW_WIDTH, windowY, windowX + WINDOW_WIDTH + 1, windowY + WINDOW_HEIGHT, 0xFFFFFFFF);

        // Title
        Component title = Component.literal("Breakthrough").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        int titleWidth = font.width(title);
        guiGraphics.drawString(font, title, windowX + (WINDOW_WIDTH - titleWidth) / 2, windowY + 15, 0xFFFFFFFF);

        renderRealmInfo(guiGraphics);
        renderRequirements(guiGraphics);

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    private void renderRealmInfo(GuiGraphics guiGraphics) {
        if (realmAttachment == null || cultivationAttachment == null) return;

        CultivationRealm currentRealm = realmAttachment.getCurrentRealm();

        // Current realm
        Component realmText = Component.literal("Current Realm: " + currentRealm.getDisplayName())
            .withStyle(ChatFormatting.AQUA);
        guiGraphics.drawString(font, realmText, windowX + 10, windowY + 40, 0xFFFFFFFF);

        // Cultivation progress
        long current = cultivationAttachment.getCurrentCultivation();
        long max = cultivationAttachment.getMaxCultivation();
        double percentage = max > 0 ? (double) current / max * 100 : 0;

        Component cultivationText = Component.literal(
                String.format("Cultivation: %d/%d (%.1f%%)", current, max, percentage))
            .withStyle(ChatFormatting.GREEN);
        guiGraphics.drawString(font, cultivationText, windowX + 10, windowY + 55, 0xFFFFFFFF);

        // Spiritual roots
        if (spiritualRoots != null) {
            Component rootsText = Component.literal("Spiritual Roots: " + spiritualRoots.getRootType().getDisplayName())
                .withStyle(ChatFormatting.YELLOW);
            guiGraphics.drawString(font, rootsText, windowX + 10, windowY + 70, 0xFFFFFFFF);
        }
    }

    private void renderRequirements(GuiGraphics guiGraphics) {
        if (realmAttachment == null || cultivationAttachment == null) return;

        // Breakthrough threshold
        CultivationRealm currentRealm = realmAttachment.getCurrentRealm();
        long threshold = currentRealm.getBreakthroughCultivationThreshold();
        long current = cultivationAttachment.getCurrentCultivation();

        ChatFormatting color = current >= threshold ? ChatFormatting.GREEN : ChatFormatting.RED;
        Component thresholdText = Component.literal(
            String.format("Required: %d cultivation", threshold)).withStyle(color);
        guiGraphics.drawString(font, thresholdText, windowX + 10, windowY + 90, 0xFFFFFFFF);

        // Cooldown info
        if (realmAttachment.isOnBreakthroughCooldown()) {
            long remainingTicks = realmAttachment.getRemainingCooldownTicks();
            long remainingSeconds = remainingTicks / 20;
            Component cooldownText = Component.literal(
                String.format("Cooldown: %ds", remainingSeconds)).withStyle(ChatFormatting.RED);
            guiGraphics.drawString(font, cooldownText, windowX + 150, windowY + 90, 0xFFFFFFFF);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // Custom button for breakthrough options
    private class BreakthroughButton extends Button {
        private final PlayerRealmAttachment.BreakthroughOption option;

        public BreakthroughButton(int x, int y, int width, int height,
                                  PlayerRealmAttachment.BreakthroughOption option) {
            super(x, y, width, height,
                Component.literal(option.type().name() + " BREAKTHROUGH"),
                button -> attemptBreakthrough(option.type()),
                DEFAULT_NARRATION);
            this.option = option;
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
            // Custom button rendering
            boolean hovered = isHovered(mouseX, mouseY);
            int bgColor = hovered ? 0xFF4A4A4A : 0xFF3A3A3A;
            int borderColor = canAttempt() ? 0xFF00AA00 : 0xFFAA0000;

            // Background
            guiGraphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), bgColor);

            // Border
            guiGraphics.fill(getX() - 1, getY() - 1, getX() + getWidth() + 1, getY(), borderColor);
            guiGraphics.fill(getX() - 1, getY() + getHeight(), getX() + getWidth() + 1, getY() + getHeight() + 1, borderColor);
            guiGraphics.fill(getX() - 1, getY(), getX(), getY() + getHeight(), borderColor);
            guiGraphics.fill(getX() + getWidth(), getY(), getX() + getWidth() + 1, getY() + getHeight(), borderColor);

            // Text
            String typeText = option.type().name() + " BREAKTHROUGH";
            String targetText = "→ " + option.targetRealm().getDisplayName();
            String chanceText = String.format("Success: %.1f%%", option.successChance() * 100);

            guiGraphics.drawString(CultivationBreakthroughScreen.this.font, typeText,
                getX() + 5, getY() + 5, 0xFFFFFFFF);
            guiGraphics.drawString(CultivationBreakthroughScreen.this.font, targetText,
                getX() + 5, getY() + 20, 0xFFAAFFAA);
            guiGraphics.drawString(CultivationBreakthroughScreen.this.font, chanceText,
                getX() + 5, getY() + 35, getChanceColor());
        }

        private boolean canAttempt() {
            return realmAttachment != null && realmAttachment.canAttemptBreakthrough();
        }

        private int getChanceColor() {
            double chance = option.successChance();
            if (chance >= 0.7) return 0xFF00FF00; // Green
            if (chance >= 0.4) return 0xFFFFFF00; // Yellow
            return 0xFFFF6666; // Red
        }

        private boolean isHovered(int mouseX, int mouseY) {
            return mouseX >= getX() && mouseX < getX() + getWidth() &&
                mouseY >= getY() && mouseY < getY() + getHeight();
        }
    }

    private void attemptBreakthrough(PlayerRealmAttachment.BreakthroughType type) {
        // Send packet to server
        ICModNetwork.sendToServer(new BreakthroughAttemptPacket(type));
        onClose();
    }
}
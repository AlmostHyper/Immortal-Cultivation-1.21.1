package net.hyper.immortalcultivation.client.gui.overlays;

import com.mojang.blaze3d.systems.RenderSystem;
import net.hyper.immortalcultivation.core.attachments.AttachmentRegistry;
import net.hyper.immortalcultivation.core.attachments.ICultivationData;
import net.hyper.immortalcultivation.core.attachments.player.PlayerCultivationAttachment;
import net.hyper.immortalcultivation.core.cultivation.realms.CultivationRealm;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

public class CultivationOverlay {

    private final CultivationOverlayConfig config;

    public CultivationOverlay() {this.config = new CultivationOverlayConfig();}

    public void render(GuiGraphics guiGraphics, DeltaTracker partialTick, int screenWidth, int screenHeight) {
        LocalPlayer player = Minecraft.getInstance().player;
        PlayerCultivationAttachment cultivation = player.getData(AttachmentRegistry.PLAYER_CULTIVATION.get());

        RenderSystem.enableBlend();

        int baseX = config.getBaseX(screenWidth);
        int baseY = config.getBaseY(screenHeight);
        int currentY = baseY;

        if (config.shouldShowRealm()) {currentY += renderRealmInfo(guiGraphics, cultivation, baseX, currentY);}
        if (config.shouldShowQi()) {currentY += renderCultivationBar(guiGraphics, cultivation, ICultivationData.CultivationType.QI, baseX, currentY);}
        if (config.shouldShowBody()) {currentY += renderCultivationBar(guiGraphics, cultivation, ICultivationData.CultivationType.BODY, baseX, currentY);}
        if (config.shouldShowFocus()) {currentY += renderCurrentFocus(guiGraphics, cultivation, baseX, currentY);}

        RenderSystem.disableBlend();
    }

    private int renderRealmInfo(GuiGraphics guiGraphics, PlayerCultivationAttachment cultivation, int x, int y) {
        CultivationRealm currentRealm = cultivation.getCurrentRealm(ICultivationData.CultivationType.QI);

        String realmText = currentRealm.getDisplayName();
        Component realmComponent = Component.literal(realmText);

        int textWidth = Minecraft.getInstance().font.width(realmComponent);

        if (config.hasBackground()) {guiGraphics.fill(x - 2, y - 2, x + textWidth + 2, y + 10, config.getBackgroundColor());}

        guiGraphics.drawString(
            Minecraft.getInstance().font,
            realmComponent,
            x, y,
            config.getRealmTextColor(),
            config.hasTextShadow()
        );

        return 12;
    }

    private int renderCultivationBar(GuiGraphics guiGraphics, PlayerCultivationAttachment cultivation,
                                     ICultivationData.CultivationType type, int x, int y) {
        long current = cultivation.getCurrentCultivation(type);
        long max = cultivation.getMaxCultivation(type);

        String typeLabel = type == ICultivationData.CultivationType.QI ? "Qi" : "Body";
        String progressText = String.format("%s: %d / %d", typeLabel, current, max);
        Component textComponent = Component.literal(progressText);

        int textWidth = Minecraft.getInstance().font.width(textComponent);
        int barWidth = Math.max(textWidth, config.getBarWidth());

        if (config.hasBackground()) {guiGraphics.fill(x - 2, y - 2, x + barWidth + 2, y + 12, config.getBackgroundColor());}

        guiGraphics.drawString(
            Minecraft.getInstance().font,
            textComponent,
            x, y,
            config.getCultivationTextColor(type),
            config.hasTextShadow()
        );

        if (config.shouldShowProgressBar()) {
            int barY = y + 11;
            int barHeight = config.getBarHeight();

            guiGraphics.fill(x, barY, x + barWidth, barY + barHeight, config.getBarBackgroundColor());

            if (max > 0) {
                int filledWidth = (int) ((double) current / max * barWidth);
                guiGraphics.fill(x, barY, x + filledWidth, barY + barHeight, config.getBarFillColor(type));
            }

            return 14 + barHeight;
        }

        return 12;
    }

    private int renderCurrentFocus(GuiGraphics guiGraphics, PlayerCultivationAttachment cultivation, int x, int y) {
        ICultivationData.CultivationType focus = cultivation.getCurrentFocus();
        String focusText = "Focus: " + (focus == ICultivationData.CultivationType.QI ? "Qi" : "Body");
        Component focusComponent = Component.literal(focusText);

        int textWidth = Minecraft.getInstance().font.width(focusComponent);

        if (config.hasBackground()) {guiGraphics.fill(x - 2, y - 2, x + textWidth + 2, y + 10, config.getBackgroundColor());}

        guiGraphics.drawString(
            Minecraft.getInstance().font,
            focusComponent,
            x, y,
            config.getFocusTextColor(),
            config.hasTextShadow()
        );

        return 12;
    }

    public CultivationOverlayConfig getConfig() {
        return config;
    }

    public static class CultivationOverlayConfig {
        private boolean enabled = true;
        private OverlayPosition position = OverlayPosition.TOP_LEFT;
        private int offsetX = 10;
        private int offsetY = 10;

        private boolean showRealm = true;
        private boolean showQi = true;
        private boolean showBody = true;
        private boolean showFocus = true;
        private boolean showProgressBar = true;

        private boolean hasBackground = true;
        private boolean hasTextShadow = true;

        private int barWidth = 100;
        private int barHeight = 4;

        private int backgroundColor = 0x80000000;
        private int realmTextColor = 0xFFD700;
        private int qiTextColor = 0x87CEEB;
        private int bodyTextColor = 0xCD853F;
        private int focusTextColor = 0xFFFFFF;

        private int barBackgroundColor = 0xFF404040;
        private int qiBarFillColor = 0xFF87CEEB;
        private int bodyBarFillColor = 0xFFCD853F;

        public enum OverlayPosition {
            TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, CENTER_LEFT, CENTER_RIGHT
        }

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public OverlayPosition getPosition() { return position; }
        public void setPosition(OverlayPosition position) { this.position = position; }

        public int getOffsetX() { return offsetX; }
        public void setOffsetX(int offsetX) { this.offsetX = offsetX; }

        public int getOffsetY() { return offsetY; }
        public void setOffsetY(int offsetY) { this.offsetY = offsetY; }

        public int getBaseX(int screenWidth) {
            return switch (position) {
                case TOP_LEFT, BOTTOM_LEFT, CENTER_LEFT -> offsetX;
                case TOP_RIGHT, BOTTOM_RIGHT, CENTER_RIGHT -> screenWidth - offsetX - getEstimatedWidth();
            };
        }

        public int getBaseY(int screenHeight) {
            return switch (position) {
                case TOP_LEFT, TOP_RIGHT -> offsetY;
                case BOTTOM_LEFT, BOTTOM_RIGHT -> screenHeight - offsetY - getEstimatedHeight();
                case CENTER_LEFT, CENTER_RIGHT -> screenHeight / 2 - getEstimatedHeight() / 2;
            };
        }

        private int getEstimatedWidth() {
            return Math.max(barWidth, 120);
        }

        private int getEstimatedHeight() {
            int height = 0;
            if (showRealm) height += 12;
            if (showQi) height += showProgressBar ? 14 + barHeight : 12;
            if (showBody) height += showProgressBar ? 14 + barHeight : 12;
            if (showFocus) height += 12;
            return height;
        }

        public boolean shouldShowRealm() { return showRealm; }
        public void setShowRealm(boolean showRealm) { this.showRealm = showRealm; }

        public boolean shouldShowQi() { return showQi; }
        public void setShowQi(boolean showQi) { this.showQi = showQi; }

        public boolean shouldShowBody() { return showBody; }
        public void setShowBody(boolean showBody) { this.showBody = showBody; }

        public boolean shouldShowFocus() { return showFocus; }
        public void setShowFocus(boolean showFocus) { this.showFocus = showFocus; }

        public boolean shouldShowProgressBar() { return showProgressBar; }
        public void setShowProgressBar(boolean showProgressBar) { this.showProgressBar = showProgressBar; }

        public boolean hasBackground() { return hasBackground; }
        public void setHasBackground(boolean hasBackground) { this.hasBackground = hasBackground; }

        public boolean hasTextShadow() { return hasTextShadow; }
        public void setHasTextShadow(boolean hasTextShadow) { this.hasTextShadow = hasTextShadow; }

        public int getBarWidth() { return barWidth; }
        public void setBarWidth(int barWidth) { this.barWidth = barWidth; }

        public int getBarHeight() { return barHeight; }
        public void setBarHeight(int barHeight) { this.barHeight = barHeight; }

        public int getBackgroundColor() { return backgroundColor; }
        public void setBackgroundColor(int backgroundColor) { this.backgroundColor = backgroundColor; }

        public int getRealmTextColor() { return realmTextColor; }
        public void setRealmTextColor(int realmTextColor) { this.realmTextColor = realmTextColor; }

        public int getCultivationTextColor(ICultivationData.CultivationType type) {
            return type == ICultivationData.CultivationType.QI ? qiTextColor : bodyTextColor;
        }

        public int getQiTextColor() { return qiTextColor; }
        public void setQiTextColor(int qiTextColor) { this.qiTextColor = qiTextColor; }

        public int getBodyTextColor() { return bodyTextColor; }
        public void setBodyTextColor(int bodyTextColor) { this.bodyTextColor = bodyTextColor; }

        public int getFocusTextColor() { return focusTextColor; }
        public void setFocusTextColor(int focusTextColor) { this.focusTextColor = focusTextColor; }

        public int getBarBackgroundColor() { return barBackgroundColor; }
        public void setBarBackgroundColor(int barBackgroundColor) { this.barBackgroundColor = barBackgroundColor; }

        public int getBarFillColor(ICultivationData.CultivationType type) {
            return type == ICultivationData.CultivationType.QI ? qiBarFillColor : bodyBarFillColor;
        }

        public int getQiBarFillColor() { return qiBarFillColor; }
        public void setQiBarFillColor(int qiBarFillColor) { this.qiBarFillColor = qiBarFillColor; }

        public int getBodyBarFillColor() { return bodyBarFillColor; }
        public void setBodyBarFillColor(int bodyBarFillColor) { this.bodyBarFillColor = bodyBarFillColor; }
    }
}

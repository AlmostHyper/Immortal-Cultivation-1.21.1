package net.hyper.immortalcultivation.ui.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import net.hyper.immortalcultivation.ImmortalCultivation;
import net.hyper.immortalcultivation.ui.menus.PillFurnaceMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class PillFurnaceScreen extends AbstractContainerScreen<PillFurnaceMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        ImmortalCultivation.MOD_ID, "textures/gui/pill_furnace.png");

    public PillFurnaceScreen(PillFurnaceMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageHeight = 166; // Standard height to match your texture
        this.inventoryLabelY = this.imageHeight - 94; // Standard position for inventory label
    }

    @Override
    protected void init() {
        super.init();
        // Center the title to match the "Pill Furnace" text in your texture
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Draw main GUI - your texture already contains all the slot outlines and decorations
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // Draw vertical progress arrow (pointing down)
        if (this.menu.isCrafting()) {
            int progress = this.menu.getScaledProgress();
            // Draw progress arrow filling from top to bottom
            // Positioned between input slots (top) and output slots (bottom)
            guiGraphics.blit(TEXTURE, x + 62, y + 35, 176, 0, 14, progress);
        }

        // Draw fuel flame (if you add flame graphics to your texture)
        if (this.menu.isBurning()) {
            int fuel = this.menu.getScaledFuelTime();
            // Draw fuel flame from bottom up (typical fuel flame behavior)
            guiGraphics.blit(TEXTURE, x + 98, y + 35 + 14 - fuel, 190, 14 - fuel, 14, fuel);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // The title and inventory labels are already in your texture, so we don't need to draw them
        // Unless you want to override them with dynamic text

        // Optional: You can still add the Chinese characters for rarity if desired
        // But since your texture already has the colored slot outlines, this might be redundant

        // If you want to add small rarity indicators:
        /*
        int rarityY = 43; // Adjust to position above output slots

        guiGraphics.drawString(this.font, "凡", 30, rarityY, 0x8B4513, false); // Common
        guiGraphics.drawString(this.font, "黄", 48, rarityY, 0x32CD32, false); // Uncommon
        guiGraphics.drawString(this.font, "玄", 66, rarityY, 0x4169E1, false); // Rare
        guiGraphics.drawString(this.font, "地", 84, rarityY, 0x9932CC, false); // Epic
        guiGraphics.drawString(this.font, "天", 102, rarityY, 0xFFD700, false); // Legendary
        */
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        // Render tooltips
        renderTooltip(guiGraphics, mouseX, mouseY);

        // Optional: Add hover effects for output slots
        int relX = mouseX - ((this.width - this.imageWidth) / 2);
        int relY = mouseY - ((this.height - this.imageHeight) / 2);

        // Check if hovering over output slots for rarity tooltips
        if (relY >= 53 && relY <= 71) {
            if (relX >= 26 && relX <= 44) {
                guiGraphics.renderTooltip(this.font, Component.literal("Common Pills (凡品)"), mouseX, mouseY);
            } else if (relX >= 44 && relX <= 62) {
                guiGraphics.renderTooltip(this.font, Component.literal("Uncommon Pills (黄品)"), mouseX, mouseY);
            } else if (relX >= 62 && relX <= 80) {
                guiGraphics.renderTooltip(this.font, Component.literal("Rare Pills (玄品)"), mouseX, mouseY);
            } else if (relX >= 80 && relX <= 98) {
                guiGraphics.renderTooltip(this.font, Component.literal("Epic Pills (地品)"), mouseX, mouseY);
            } else if (relX >= 98 && relX <= 116) {
                guiGraphics.renderTooltip(this.font, Component.literal("Legendary Pills (天品)"), mouseX, mouseY);
            }
        }
    }
}
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
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        // Center the title
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Draw main GUI
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // Draw progress arrow
        if (this.menu.isCrafting()) {
            int progress = this.menu.getScaledProgress();
            guiGraphics.blit(TEXTURE, x + 103, y + 35, 176, 14, progress, 17);
        }

        // Draw fuel flame
        if (this.menu.isBurning()) {
            int fuel = this.menu.getScaledFuelTime();
            guiGraphics.blit(TEXTURE, x + 26, y + 47 + 14 - fuel, 176, 14 - fuel, 14, fuel);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        this.renderBackground(guiGraphics, mouseX, mouseY, delta);
        super.render(guiGraphics, mouseX, mouseY, delta);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        // Render custom tooltips
        if (isHovering(103, 35, 24, 17, mouseX, mouseY)) {
            // Progress tooltip
            int progress = this.menu.getProgress();
            int maxProgress = this.menu.getMaxProgress();
            Component progressText = Component.literal(String.format("Progress: %d/%d", progress, maxProgress));
            guiGraphics.renderTooltip(this.font, progressText, mouseX, mouseY);
        }

        if (isHovering(26, 47, 14, 14, mouseX, mouseY)) {
            // Fuel tooltip
            int fuel = this.menu.getFuelTime();
            int maxFuel = this.menu.getMaxFuelTime();
            Component fuelText = Component.literal(String.format("Fuel: %d/%d", fuel, maxFuel));
            guiGraphics.renderTooltip(this.font, fuelText, mouseX, mouseY);
        }
    }
}
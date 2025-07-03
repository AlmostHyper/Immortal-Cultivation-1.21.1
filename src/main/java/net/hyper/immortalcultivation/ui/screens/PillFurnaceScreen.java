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
    // ===== TEXTURE RESOURCES =====
    // Main 256x256 texture atlas (keep your existing filename)
    // If you see purple/black checkerboard, check that this file exists in:
    // src/main/resources/assets/immortalcultivation/textures/gui/pill_furnace.png
    private static final ResourceLocation TEXTURE_ATLAS = ResourceLocation.fromNamespaceAndPath(
        ImmortalCultivation.MOD_ID, "textures/gui/pill_furnace.png");

    // Decorative scroll texture (used for both top and bottom)
    private static final ResourceLocation SCROLL_DECORATION = ResourceLocation.fromNamespaceAndPath(
        ImmortalCultivation.MOD_ID, "textures/gui/scroll.png");

    // ===== GUI DIMENSIONS =====
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 182;

    // Scroll decoration dimensions (your scroll.png is 200x30)
    private static final int SCROLL_WIDTH = 200;
    private static final int SCROLL_HEIGHT = 30;

    // Total screen height including decorations
    private static final int TOTAL_HEIGHT = GUI_HEIGHT + (2 * SCROLL_HEIGHT);

    // ===== TEXTURE ATLAS COORDINATES =====
    // Main GUI background position in the 256x256 atlas
    private static final int GUI_TEXTURE_X = 0;
    private static final int GUI_TEXTURE_Y = 0;

    // Progress arrow coordinates in atlas (vertical arrow pointing down)
    private static final int PROGRESS_ARROW_TEXTURE_X = 176;
    private static final int PROGRESS_ARROW_TEXTURE_Y = 0;
    private static final int PROGRESS_ARROW_WIDTH = 14;
    private static final int PROGRESS_ARROW_HEIGHT = 16;

    // Fuel flame coordinates in atlas
    private static final int FUEL_FLAME_TEXTURE_X = 176;
    private static final int FUEL_FLAME_TEXTURE_Y = 0;
    private static final int FUEL_FLAME_WIDTH = 14;
    private static final int FUEL_FLAME_HEIGHT = 14;

    // ===== GUI POSITIONING =====
    // Progress arrow position on screen (relative to GUI)
    private static final int PROGRESS_ARROW_GUI_X = 62;
    private static final int PROGRESS_ARROW_GUI_Y = 35;

    // Fuel flame position on screen (relative to GUI)
    private static final int FUEL_FLAME_GUI_X = 18;
    private static final int FUEL_FLAME_GUI_Y = 27;

    // ===== LABEL STYLING & POSITIONING =====
    private static final int LABEL_COLOR = 0x404040;           // Standard dark gray (vanilla style)
    private static final int XIANXIA_LABEL_COLOR = 0x2D1B69;   // Deep purple for Xianxia theme (optional)
    private static final int PROGRESS_TEXT_COLOR = 0x404040;    // Progress text color

    // Label positioning constants (easy to adjust)
    private static final int TITLE_OFFSET_X = 62;      // Title distance from left edge
    private static final int TITLE_OFFSET_Y = 6;      // Title distance from top edge
    private static final int INVENTORY_OFFSET_X = 8;  // Inventory distance from left edge
    private static final int INVENTORY_OFFSET_Y = 89; // Inventory distance from bottom edge

    public PillFurnaceScreen(PillFurnaceMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);

        // Set GUI dimensions (just the main interface, decorations are handled separately)
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;

        // Adjust inventory label position for the GUI height
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        // Center the title
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;

        // Ensure screen has enough space for decorations by adjusting if needed
        // This prevents the bottom from being cut off
        int totalNeededHeight = GUI_HEIGHT + (2 * SCROLL_HEIGHT) + 40; // +40 for padding
        if (this.height < totalNeededHeight) {
            // If screen is too small, we might need to scale or adjust positioning
            // For now, let's just ensure minimum spacing
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        int guiLeft = (this.width - this.imageWidth) / 2;
        int guiTop = (this.height - this.imageHeight) / 2;


        // Top scroll (attached directly to top of main GUI)
        int scrollX = guiLeft + (this.imageWidth - SCROLL_WIDTH) / 2;  // Center horizontally
        guiGraphics.blit(SCROLL_DECORATION,
            scrollX, guiTop - SCROLL_HEIGHT,           // Position directly above GUI (no gap)
            0, 0,                                       // Top half of scroll texture
            SCROLL_WIDTH, SCROLL_HEIGHT,                // Render size
            SCROLL_WIDTH, SCROLL_HEIGHT                 // Texture size
        );

        // Bottom scroll (attached directly to bottom of main GUI)
        guiGraphics.blit(SCROLL_DECORATION,
            scrollX, guiTop + this.imageHeight,        // Position directly below GUI (no gap)
            0, 0,                                       // Same texture (you can use 0,30 if you have different top/bottom in one file)
            SCROLL_WIDTH, SCROLL_HEIGHT,                // Render size
            SCROLL_WIDTH, SCROLL_HEIGHT                 // Texture size
        );


        // ===== RENDER MAIN GUI BACKGROUND =====
        // Draw main GUI background from the 256x256 atlas
        // Make sure your texture file is exactly 256x256 pixels
        guiGraphics.blit(TEXTURE_ATLAS,
            guiLeft, guiTop,                          // Screen position
            GUI_TEXTURE_X, GUI_TEXTURE_Y,             // Atlas coordinates (0, 0)
            this.imageWidth, this.imageHeight,         // Render size (176x182)
            256, 256                                   // Atlas texture size (256x256)
        );

        // Draw fuel flame (fills from bottom to top)
        if (this.menu.isBurning()) {
            int fuel = this.menu.getScaledFuelTime();
            int flameY = FUEL_FLAME_HEIGHT - fuel; // Start from bottom

            guiGraphics.blit(TEXTURE_ATLAS,
                guiLeft + FUEL_FLAME_GUI_X, guiTop + FUEL_FLAME_GUI_Y + flameY,
                FUEL_FLAME_TEXTURE_X, FUEL_FLAME_TEXTURE_Y + flameY,
                FUEL_FLAME_WIDTH, fuel,
                256, 256
            );
        }

        Component containerTitle = Component.literal("Pill Furnace");
        Component inventoryTitle = Component.literal("Inventory");

        // ===== POSITIONING CALCULATION =====
        // Using constants for easy adjustment
        int titleX = guiLeft + TITLE_OFFSET_X;
        int titleY = guiTop + TITLE_OFFSET_Y;

        // For centered title, use this calculation instead:
        // int titleX = guiLeft + (this.imageWidth - this.font.width(containerTitle)) / 2;

        int inventoryX = guiLeft + INVENTORY_OFFSET_X;
        int inventoryY = guiTop + GUI_HEIGHT - INVENTORY_OFFSET_Y;

        // ===== RENDER THE LABELS =====
        // Choose color: LABEL_COLOR (vanilla gray) or XIANXIA_LABEL_COLOR (purple)
        if (containerTitle != null) {
            guiGraphics.drawString(this.font, containerTitle, titleX, titleY, LABEL_COLOR, false);
        }

        if (inventoryTitle != null) {
            guiGraphics.drawString(this.font, inventoryTitle, inventoryX, inventoryY, LABEL_COLOR, false);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Don't render default labels since they're in the texture
        // Only render dynamic information if needed

        // Optional: Render batch progress text
        if (this.menu.isCrafting()) {
            String batchText = this.menu.getCurrentBatch() + "/" + this.menu.getBatchSize();
            int textWidth = this.font.width(batchText);
            guiGraphics.drawString(this.font, batchText,
                this.imageWidth - textWidth - 8, 8, 0x404040, false);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        // Render item tooltips
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
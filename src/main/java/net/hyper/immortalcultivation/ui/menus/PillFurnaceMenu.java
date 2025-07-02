package net.hyper.immortalcultivation.ui.menus;

import net.hyper.immortalcultivation.block.entities.PillFurnaceBlockEntity;
import net.hyper.immortalcultivation.registry.ICModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class PillFurnaceMenu extends AbstractContainerMenu {
    // ===== SLOT POSITION CONSTANTS - ADJUST THESE EASILY =====

    // Input slots (top row)
    private static final int INPUT_1_X = 58;
    private static final int INPUT_1_Y = 17;
    private static final int INPUT_2_X = 82;
    private static final int INPUT_2_Y = 17;
    private static final int INPUT_3_X = 106;
    private static final int INPUT_3_Y = 17;

    // Special slots (middle row)
    private static final int SPIRIT_STONE_X = 58;
    private static final int SPIRIT_STONE_Y = 40;
    private static final int FUEL_X = 106;
    private static final int FUEL_Y = 40;

    // Output slots (bottom row)
    private static final int OUTPUT_COMMON_X = 36;
    private static final int OUTPUT_COMMON_Y = 61;
    private static final int OUTPUT_UNCOMMON_X = 60;
    private static final int OUTPUT_UNCOMMON_Y = 61;
    private static final int OUTPUT_RARE_X = 82;
    private static final int OUTPUT_RARE_Y = 61;
    private static final int OUTPUT_EPIC_X = 106;
    private static final int OUTPUT_EPIC_Y = 61;
    private static final int OUTPUT_LEGENDARY_X = 130;
    private static final int OUTPUT_LEGENDARY_Y = 61;

    // Player inventory positions and spacing (adjust these to match your texture)
    private static final int PLAYER_INVENTORY_START_X = 8;
    private static final int PLAYER_INVENTORY_START_Y = 92;
    private static final int PLAYER_INVENTORY_SLOT_SPACING_X = 18;  // Horizontal spacing between slots
    private static final int PLAYER_INVENTORY_SLOT_SPACING_Y = 20;  // Vertical spacing between rows

    private static final int PLAYER_HOTBAR_START_X = 8;
    private static final int PLAYER_HOTBAR_START_Y = 156;
    private static final int PLAYER_HOTBAR_SLOT_SPACING_X = 18;     // Horizontal spacing for hotbar

    // ===========================================================

    private final PillFurnaceBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;

    // Constructor for server-side (called from BlockEntity)
    public PillFurnaceMenu(int containerId, Inventory playerInventory, PillFurnaceBlockEntity blockEntity, ContainerData data) {
        super(ICModMenuTypes.PILL_FURNACE_MENU.get(), containerId);
        checkContainerSize(playerInventory, PillFurnaceBlockEntity.SLOT_COUNT);
        this.blockEntity = blockEntity;
        this.level = playerInventory.player.level();
        this.data = data;

        addDataSlots(data);

        IItemHandler itemHandler = blockEntity.getItemHandler(null);

        // Add pill furnace slots
        addPillFurnaceSlots(itemHandler);

        // Add player inventory slots
        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    // Constructor for client-side (called from network)
    public PillFurnaceMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        super(ICModMenuTypes.PILL_FURNACE_MENU.get(), containerId);

        this.level = playerInventory.player.level();

        // Handle the case where extraData is null (common issue in NeoForge 1.21.1)
        BlockPos blockPos;
        if (extraData != null) {
            blockPos = extraData.readBlockPos();
        } else {
            // Fallback: try to find a nearby Pill Furnace block
            blockPos = findNearbyPillFurnace(playerInventory.player);
            if (blockPos == null) {
                // Last resort: use player position
                blockPos = playerInventory.player.blockPosition();
            }
        }

        BlockEntity blockEntity = this.level.getBlockEntity(blockPos);
        if (blockEntity instanceof PillFurnaceBlockEntity pillFurnace) {
            this.blockEntity = pillFurnace;
            this.data = new SimpleContainerData(6);

            addDataSlots(this.data);

            IItemHandler itemHandler = pillFurnace.getItemHandler(null);

            // Add pill furnace slots
            addPillFurnaceSlots(itemHandler);
        } else {
            // Emergency fallback - create a dummy block entity
            this.blockEntity = new PillFurnaceBlockEntity(blockPos, level.getBlockState(blockPos));
            this.data = new SimpleContainerData(6);

            addDataSlots(this.data);

            IItemHandler itemHandler = this.blockEntity.getItemHandler(null);
            addPillFurnaceSlots(itemHandler);
        }

        // Add player inventory slots
        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    // Helper method to find a nearby Pill Furnace block
    private BlockPos findNearbyPillFurnace(Player player) {
        BlockPos playerPos = player.blockPosition();

        // Search in a 5x5x5 area around the player
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    BlockPos checkPos = playerPos.offset(x, y, z);
                    BlockEntity blockEntity = level.getBlockEntity(checkPos);
                    if (blockEntity instanceof PillFurnaceBlockEntity) {
                        return checkPos;
                    }
                }
            }
        }

        return null;
    }

    private void addPillFurnaceSlots(IItemHandler itemHandler) {
        // Input slots (top row)
        this.addSlot(new SlotItemHandler(itemHandler, PillFurnaceBlockEntity.INPUT_SLOT_1, INPUT_1_X, INPUT_1_Y));
        this.addSlot(new SlotItemHandler(itemHandler, PillFurnaceBlockEntity.INPUT_SLOT_2, INPUT_2_X, INPUT_2_Y));
        this.addSlot(new SlotItemHandler(itemHandler, PillFurnaceBlockEntity.INPUT_SLOT_3, INPUT_3_X, INPUT_3_Y));

        // Special slots (middle row)
        this.addSlot(new SlotItemHandler(itemHandler, PillFurnaceBlockEntity.SPIRIT_STONE_SLOT, SPIRIT_STONE_X, SPIRIT_STONE_Y));
        this.addSlot(new SlotItemHandler(itemHandler, PillFurnaceBlockEntity.FUEL_SLOT, FUEL_X, FUEL_Y));

        // Output slots (bottom row) - these are output-only
        this.addSlot(new SlotItemHandler(itemHandler, PillFurnaceBlockEntity.OUTPUT_SLOT_COMMON, OUTPUT_COMMON_X, OUTPUT_COMMON_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false; // Output only
            }
        });

        this.addSlot(new SlotItemHandler(itemHandler, PillFurnaceBlockEntity.OUTPUT_SLOT_UNCOMMON, OUTPUT_UNCOMMON_X, OUTPUT_UNCOMMON_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false; // Output only
            }
        });

        this.addSlot(new SlotItemHandler(itemHandler, PillFurnaceBlockEntity.OUTPUT_SLOT_RARE, OUTPUT_RARE_X, OUTPUT_RARE_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false; // Output only
            }
        });

        this.addSlot(new SlotItemHandler(itemHandler, PillFurnaceBlockEntity.OUTPUT_SLOT_EPIC, OUTPUT_EPIC_X, OUTPUT_EPIC_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false; // Output only
            }
        });

        this.addSlot(new SlotItemHandler(itemHandler, PillFurnaceBlockEntity.OUTPUT_SLOT_LEGENDARY, OUTPUT_LEGENDARY_X, OUTPUT_LEGENDARY_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false; // Output only
            }
        });
    }

    private void addPlayerInventory(Inventory playerInventory) {
        // Player inventory slots (3 rows of 9) - using adjustable spacing
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9,
                    PLAYER_INVENTORY_START_X + col * PLAYER_INVENTORY_SLOT_SPACING_X,
                    PLAYER_INVENTORY_START_Y + row * PLAYER_INVENTORY_SLOT_SPACING_Y));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        // Player hotbar slots (1 row of 9) - using adjustable spacing
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col,
                PLAYER_HOTBAR_START_X + col * PLAYER_HOTBAR_SLOT_SPACING_X,
                PLAYER_HOTBAR_START_Y));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemstack = slotStack.copy();

            // If clicking on furnace slots
            if (index < PillFurnaceBlockEntity.SLOT_COUNT) {
                // Try to move to player inventory
                if (!this.moveItemStackTo(slotStack, PillFurnaceBlockEntity.SLOT_COUNT,
                    PillFurnaceBlockEntity.SLOT_COUNT + 36, true)) {
                    return ItemStack.EMPTY;
                }
            }
            // If clicking on player inventory
            else {
                // Try to move to appropriate furnace slots
                boolean moved = false;

                // Check if it's a valid input/fuel/spirit stone
                if (isValidInput(slotStack)) {
                    // Try input slots first
                    moved = this.moveItemStackTo(slotStack, PillFurnaceBlockEntity.INPUT_SLOT_1,
                        PillFurnaceBlockEntity.INPUT_SLOT_3 + 1, false);
                }

                if (!moved && isValidFuel(slotStack)) {
                    // Try fuel slot
                    moved = this.moveItemStackTo(slotStack, PillFurnaceBlockEntity.FUEL_SLOT,
                        PillFurnaceBlockEntity.FUEL_SLOT + 1, false);
                }

                if (!moved && isValidSpiritStone(slotStack)) {
                    // Try spirit stone slot
                    moved = this.moveItemStackTo(slotStack, PillFurnaceBlockEntity.SPIRIT_STONE_SLOT,
                        PillFurnaceBlockEntity.SPIRIT_STONE_SLOT + 1, false);
                }

                // If couldn't move to furnace slots, try moving within player inventory
                if (!moved) {
                    if (index < PillFurnaceBlockEntity.SLOT_COUNT + 27) {
                        // From player inventory to hotbar
                        if (!this.moveItemStackTo(slotStack, PillFurnaceBlockEntity.SLOT_COUNT + 27,
                            PillFurnaceBlockEntity.SLOT_COUNT + 36, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else {
                        // From hotbar to player inventory
                        if (!this.moveItemStackTo(slotStack, PillFurnaceBlockEntity.SLOT_COUNT,
                            PillFurnaceBlockEntity.SLOT_COUNT + 27, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                }
            }

            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (slotStack.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, slotStack);
        }

        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.blockEntity.stillValid(player);
    }

    // Helper methods to check item validity
    private boolean isValidInput(ItemStack stack) {
        // TODO: Implement proper checks for pill ingredients
        return !stack.isEmpty();
    }

    private boolean isValidFuel(ItemStack stack) {
        // TODO: Implement proper fuel checking
        return !stack.isEmpty();
    }

    private boolean isValidSpiritStone(ItemStack stack) {
        // TODO: Check if item has spirit stone tag
        return !stack.isEmpty();
    }

    // Getters for GUI to access progress data
    public int getProgress() {
        return this.data.get(0);
    }

    public int getMaxProgress() {
        return this.data.get(1);
    }

    public int getFuelTime() {
        return this.data.get(2);
    }

    public int getMaxFuelTime() {
        return this.data.get(3);
    }

    public int getBatchSize() {
        return this.data.get(4);
    }

    public int getCurrentBatch() {
        return this.data.get(5);
    }

    public boolean isCrafting() {
        return getProgress() > 0;
    }

    public boolean isBurning() {
        return getFuelTime() > 0;
    }

    // For GUI rendering
    public int getScaledProgress() {
        int maxProgress = getMaxProgress();
        int progress = getProgress();
        int progressArrowHeight = 16; // Height of vertical progress arrow (adjust to match your arrow height)

        return maxProgress != 0 && progress != 0 ? progress * progressArrowHeight / maxProgress : 0;
    }

    public int getScaledFuelTime() {
        int maxFuel = getMaxFuelTime();
        int fuel = getFuelTime();
        int fuelFlameHeight = 14; // Height of fuel flame in GUI

        return maxFuel != 0 ? fuel * fuelFlameHeight / maxFuel : 0;
    }
}
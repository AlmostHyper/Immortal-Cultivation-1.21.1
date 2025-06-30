package net.hyper.immortalcultivation.ui.menus;

import net.hyper.immortalcultivation.block.entities.PillFurnaceBlockEntity;
import net.hyper.immortalcultivation.registry.ICModMenuTypes;
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
        this(containerId, playerInventory,
            (PillFurnaceBlockEntity) playerInventory.player.level().getBlockEntity(extraData.readBlockPos()),
            new SimpleContainerData(6)); // Updated to 6 for new ContainerData size
    }

    private void addPillFurnaceSlots(IItemHandler itemHandler) {
        // Input slots (3 slots in a row)
        this.addSlot(new SlotItemHandler(itemHandler, PillFurnaceBlockEntity.INPUT_SLOT_1, 26, 35));
        this.addSlot(new SlotItemHandler(itemHandler, PillFurnaceBlockEntity.INPUT_SLOT_2, 44, 35));
        this.addSlot(new SlotItemHandler(itemHandler, PillFurnaceBlockEntity.INPUT_SLOT_3, 62, 35));

        // Fuel slot (bottom left)
        this.addSlot(new SlotItemHandler(itemHandler, PillFurnaceBlockEntity.FUEL_SLOT, 26, 62));

        // Spirit stone slot (middle)
        this.addSlot(new SlotItemHandler(itemHandler, PillFurnaceBlockEntity.SPIRIT_STONE_SLOT, 80, 35));

        // Output slots - arranged vertically by rarity (Legendary at top, Common at bottom)
        // This follows Xianxia tradition where higher quality items are "more elevated"

        // Legendary output slot (top - most prestigious)
        this.addSlot(new SlotItemHandler(itemHandler, PillFurnaceBlockEntity.OUTPUT_SLOT_LEGENDARY, 134, 17) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false; // Output only
            }
        });

        // Epic output slot
        this.addSlot(new SlotItemHandler(itemHandler, PillFurnaceBlockEntity.OUTPUT_SLOT_EPIC, 134, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false; // Output only
            }
        });

        // Rare output slot
        this.addSlot(new SlotItemHandler(itemHandler, PillFurnaceBlockEntity.OUTPUT_SLOT_RARE, 134, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false; // Output only
            }
        });

        // Uncommon output slot
        this.addSlot(new SlotItemHandler(itemHandler, PillFurnaceBlockEntity.OUTPUT_SLOT_UNCOMMON, 134, 71) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false; // Output only
            }
        });

        // Common output slot (bottom - most common)
        this.addSlot(new SlotItemHandler(itemHandler, PillFurnaceBlockEntity.OUTPUT_SLOT_COMMON, 134, 89) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false; // Output only
            }
        });
    }

    private void addPlayerInventory(Inventory playerInventory) {
        // Player inventory slots (3 rows of 9)
        // Adjusted Y position to accommodate the vertical output slots
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 118 + row * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        // Player hotbar slots (1 row of 9)
        // Adjusted Y position to match new inventory position
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 176));
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
        int progressArrowSize = 24; // Width of progress arrow in GUI

        return maxProgress != 0 && progress != 0 ? progress * progressArrowSize / maxProgress : 0;
    }

    public int getScaledFuelTime() {
        int maxFuel = getMaxFuelTime();
        int fuel = getFuelTime();
        int fuelFlameHeight = 14; // Height of fuel flame in GUI

        return maxFuel != 0 ? fuel * fuelFlameHeight / maxFuel : 0;
    }
}
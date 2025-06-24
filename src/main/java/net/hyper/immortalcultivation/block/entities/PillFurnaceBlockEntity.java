package net.hyper.immortalcultivation.block.entities;

import net.hyper.immortalcultivation.recipe.PillFurnaceRecipe;
import net.hyper.immortalcultivation.recipe.PillFurnaceRecipeInput;
import net.hyper.immortalcultivation.registry.ICModBlockEntities;
import net.hyper.immortalcultivation.registry.ICModRecipes;
import net.hyper.immortalcultivation.ui.menus.PillFurnaceMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;
import net.hyper.immortalcultivation.block.crafting.PillFurnaceBlock;

import javax.annotation.Nullable;
import java.util.Optional;

public class PillFurnaceBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {

    // === VARIABLES === //

    // Slots
    public static final int INPUT_SLOT_1 = 0;
    public static final int INPUT_SLOT_2 = 1;
    public static final int INPUT_SLOT_3 = 2;
    public static final int FUEL_SLOT = 3;
    public static final int SPIRIT_STONE_SLOT = 4;
    public static final int OUTPUT_SLOT = 5;
    public static final int SLOT_COUNT = 6;

    // Data
    private static final int PROGRESS_INDEX = 0;
    private static final int MAX_PROGRESS_INDEX = 1;
    private static final int FUEL_TIME_INDEX = 2;
    private static final int MAX_FUEL_TIME_INDEX = 3;
    private static final int DATA_COUNT = 4;

    // Processing
    private int progress = 0;
    private int maxProgress = 200; // Base processing time (10 seconds at 20 tps)
    private int fuelTime = 0;
    private int maxFuelTime = 0;

    // Store items as NonNullList for BaseContainerBlockEntity compatibility
    private NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);

    // === CONSTRUCTORS === //

    public PillFurnaceBlockEntity(BlockPos pos, BlockState blockState) {
        super(ICModBlockEntities.PILL_FURNACE_BLOCK_ENTITY.get(), pos, blockState);
    }

    // === CORE === //

    // Validates inserted items
    private final ItemStackHandler itemHandler = new ItemStackHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            // Sync with the NonNullList
            if (slot >= 0 && slot < SLOT_COUNT) {
                items.set(slot, getStackInSlot(slot).copy());
            }
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case INPUT_SLOT_1, INPUT_SLOT_2, INPUT_SLOT_3 -> isValidInput(stack);
                case FUEL_SLOT -> isValidFuel(stack);
                case SPIRIT_STONE_SLOT -> isValidSpiritStone(stack);
                case OUTPUT_SLOT -> false;
                default -> false;
            };
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            super.setStackInSlot(slot, stack);
            // Sync with the NonNullList
            if (slot >= 0 && slot < SLOT_COUNT) {
                items.set(slot, stack.copy());
            }
        }
    };

    // Container data for syncing with client
    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case PROGRESS_INDEX -> progress;
                case MAX_PROGRESS_INDEX -> maxProgress;
                case FUEL_TIME_INDEX -> fuelTime;
                case MAX_FUEL_TIME_INDEX -> maxFuelTime;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case PROGRESS_INDEX -> progress = value;
                case MAX_PROGRESS_INDEX -> maxProgress = value;
                case FUEL_TIME_INDEX -> fuelTime = value;
                case MAX_FUEL_TIME_INDEX -> maxFuelTime = value;
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    // Tick method for processing
    public static void tick(Level level, BlockPos pos, BlockState state, PillFurnaceBlockEntity entity) {
        if (level.isClientSide()) return;

        boolean wasLit = entity.isLit();
        boolean changed = false;

        // Consume fuel if needed and available
        if (entity.fuelTime <= 0 && entity.canProcess()) {
            entity.fuelTime = entity.getFuelBurnTime(entity.itemHandler.getStackInSlot(FUEL_SLOT));
            entity.maxFuelTime = entity.fuelTime;

            if (entity.fuelTime > 0) {
                entity.itemHandler.extractItem(FUEL_SLOT, 1, false);
                changed = true;
            }
        }

        // Process if fuel is available
        if (entity.isLit() && entity.canProcess()) {
            entity.progress++;

            // Apply modifiers based on player realm, dao, spirit stones, etc.
            int processSpeed = entity.getProcessingSpeed();
            if (entity.progress >= entity.maxProgress / processSpeed) {
                entity.processRecipe();
                entity.progress = 0;
                changed = true;
            }
        } else if (!entity.canProcess()) {
            entity.progress = 0;
        }

        // Consume fuel time
        if (entity.fuelTime > 0) {
            entity.fuelTime--;
            changed = true;
        }

        // Update block state if lit status changed
        if (wasLit != entity.isLit()) {
            changed = true;
            // Update the LIT property in the blockstate
            level.setBlock(pos, state.setValue(PillFurnaceBlock.LIT, entity.isLit()), 3);
        }

        if (changed) {
            entity.setChanged();
        }
    }

    // Process validation
    private boolean canProcess() {
        // Check if we have a valid recipe
        PillFurnaceRecipe recipe = getValidRecipe();
        if (recipe == null) return false;

        // Check if output slot can accept the result
        ItemStack result = recipe.getResult();
        ItemStack outputSlot = itemHandler.getStackInSlot(OUTPUT_SLOT);

        if (outputSlot.isEmpty()) return true;
        if (!ItemStack.isSameItem(result, outputSlot)) return false;

        return outputSlot.getCount() + result.getCount() <= outputSlot.getMaxStackSize();
    }

    // Processes recipe along Pill Furnace guidelines with weighted system
    private void processRecipe() {
        PillFurnaceRecipe recipe = getValidRecipe();
        if (recipe == null) return;

        // Consume inputs
        itemHandler.extractItem(INPUT_SLOT_1, recipe.getInput1Count(), false);
        itemHandler.extractItem(INPUT_SLOT_2, recipe.getInput2Count(), false);
        itemHandler.extractItem(INPUT_SLOT_3, recipe.getInput3Count(), false);

        // Consume spirit stone (if required)
        if (recipe.requiresSpiritStone()) {
            itemHandler.extractItem(SPIRIT_STONE_SLOT, 1, false);
        }

        // Calculate success chance based on player factors
        double successChance = calculateSuccessChance(recipe);

        if (level.random.nextDouble() < successChance) {
            // Success - add result to output
            ItemStack result = recipe.getResult().copy();
            ItemStack outputSlot = itemHandler.getStackInSlot(OUTPUT_SLOT);

            if (outputSlot.isEmpty()) {
                itemHandler.setStackInSlot(OUTPUT_SLOT, result);
            } else {
                outputSlot.grow(result.getCount());
            }
        } else {
            // Failure - could add failure items here
            // For now, just waste the materials
        }
    }

    // Calculates chance using various systems like Dao, also based on furnace tier
    private double calculateSuccessChance(PillFurnaceRecipe recipe) {
        double baseChance = recipe.getBaseSuccessChance();

        // Apply tier modifier
        double tierModifier = getTierSuccessModifier();

        // Other modifiers
        double realmModifier = getRealmModifier();
        double daoModifier = getDaoModifier();
        double spiritStoneModifier = getSpiritStoneModifier();

        return Math.min(1.0, baseChance * tierModifier * realmModifier * daoModifier * spiritStoneModifier);
    }

    private double getRealmModifier() {
        // TODO: Get player's realm and calculate modifier
        // Higher realms should have better success rates
        return 1.0; // Placeholder
    }

    private double getDaoModifier() {
        // TODO: Check player's Dao comprehension
        // Relevant Daos (like Dao of Alchemy) should improve success
        return 1.0; // Placeholder
    }

    private double getSpiritStoneModifier() {
        ItemStack spiritStone = itemHandler.getStackInSlot(SPIRIT_STONE_SLOT);
        if (spiritStone.isEmpty()) return 0.8; // Penalty for no spirit stone

        // TODO: Different spirit stone qualities provide different bonuses
        return 1.2; // Placeholder bonus
    }

    private int getProcessingSpeed() {
        // Base speed modified by furnace tier
        int baseSpeed = getTierSpeedModifier();

        // TODO: Add additional modifiers based on:
        // - Player's realm (higher realms = faster)
        // - Spirit stone quality
        // - Environmental factors

        return baseSpeed;
    }

    private PillFurnaceRecipe getValidRecipe() {
        if (level == null) return null;

        // Create recipe input from current items in slots
        PillFurnaceRecipeInput recipeInput = new PillFurnaceRecipeInput(
            itemHandler.getStackInSlot(INPUT_SLOT_1),
            itemHandler.getStackInSlot(INPUT_SLOT_2),
            itemHandler.getStackInSlot(INPUT_SLOT_3)
        );

        // Get recipe manager and find matching recipe
        RecipeManager recipeManager = level.getRecipeManager();
        Optional<RecipeHolder<PillFurnaceRecipe>> recipe = recipeManager
            .getRecipeFor(ICModRecipes.PILL_FURNACE_TYPE.get(), recipeInput, level);

        return recipe.map(RecipeHolder::value).orElse(null);
    }

    private boolean isLit() {
        return fuelTime > 0;
    }

    private int getFuelBurnTime(ItemStack fuel) {
        if (fuel.isEmpty()) return 0;

        // Base fuel values (you can expand this)
        int baseBurnTime = switch (fuel.getItem().toString()) {
            case "minecraft:coal" -> 1600;
            case "minecraft:charcoal" -> 1600;
            case "minecraft:coal_block" -> 16000;
            case "minecraft:blaze_rod" -> 2400;
            case "minecraft:lava_bucket" -> 20000;
            // Add your cultivation-themed fuels here
            default -> 0;
        };

        // Apply fuel efficiency modifier
        return (baseBurnTime * getTierFuelEfficiency()) / 100;
    }

    // Validation methods for different slot types
    private boolean isValidInput(ItemStack stack) {
        // TODO: Check if item can be used as pill ingredient
        return !stack.isEmpty();
    }

    private boolean isValidFuel(ItemStack stack) {
        // TODO: Check if item can be used as fuel
        return !stack.isEmpty();
    }

    private boolean isValidSpiritStone(ItemStack stack) {
        // TODO: Check if item is a spirit stone
        return !stack.isEmpty();
    }

    // Container implementation
    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (!itemHandler.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return itemHandler.getStackInSlot(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return itemHandler.extractItem(slot, amount, false);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return itemHandler.extractItem(slot, itemHandler.getStackInSlot(slot).getCount(), false);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        itemHandler.setStackInSlot(slot, stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < SLOT_COUNT; i++) {
            itemHandler.setStackInSlot(i, ItemStack.EMPTY);
        }
        items.clear();
        for (int i = 0; i < SLOT_COUNT; i++) {
            items.add(ItemStack.EMPTY);
        }
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.immortalcultivation.pill_furnace");
    }

    // ✅ FIXED: Properly implement getItems() and setItems()
    @Override
    protected NonNullList<ItemStack> getItems() {
        // Sync itemHandler to items list before returning
        for (int i = 0; i < SLOT_COUNT; i++) {
            items.set(i, itemHandler.getStackInSlot(i).copy());
        }
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> itemList) {
        // Sync items list to itemHandler
        for (int i = 0; i < Math.min(itemList.size(), SLOT_COUNT); i++) {
            itemHandler.setStackInSlot(i, itemList.get(i).copy());
            items.set(i, itemList.get(i).copy());
        }
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new PillFurnaceMenu(containerId, inventory, this, dataAccess);
    }

    // === FURNACE TIER SYSTEM === //

    /**
     * Get the tier of this furnace. Override in subclasses for different tiers.
     * Tier 0 = Basic (Stone)
     * Tier 1 = Iron
     * Tier 2 = Gold
     * Tier 3 = Diamond
     * Tier 4 = Netherite
     * Tier 5+ = Cultivation materials (Spiritual Iron, Heavenly Gold, etc.)
     */
    public int getFurnaceTier() {
        return 0; // Basic tier by default
    }

    /**
     * Get the success rate modifier based on furnace tier.
     * Higher tier furnaces have better success rates.
     */
    protected double getTierSuccessModifier() {
        return 1.0; // No modifier for basic tier
    }

    /**
     * Get the processing speed modifier based on furnace tier.
     * Higher values mean faster processing.
     */
    protected int getTierSpeedModifier() {
        return 1; // Base speed for basic tier
    }

    /**
     * Get the fuel efficiency percentage based on furnace tier.
     * 100 = normal efficiency, 150 = 50% more efficient
     */
    protected int getTierFuelEfficiency() {
        return 100; // Normal efficiency for basic tier
    }

    // === WRAPPER === //

    // Wrapper for sided access
    private final SidedInvWrapper[] sidedWrappers = {
        new SidedInvWrapper(this, Direction.UP),    // Top
        new SidedInvWrapper(this, Direction.DOWN),  // Bottom
        new SidedInvWrapper(this, Direction.NORTH), // Sides
        new SidedInvWrapper(this, Direction.SOUTH),
        new SidedInvWrapper(this, Direction.EAST),
        new SidedInvWrapper(this, Direction.WEST)
    };

    // WorldlyContainer implementation for sided access
    @Override
    public int[] getSlotsForFace(Direction side) {
        return switch (side) {
            case UP -> new int[]{INPUT_SLOT_1, INPUT_SLOT_2, INPUT_SLOT_3, SPIRIT_STONE_SLOT};
            case DOWN -> new int[]{OUTPUT_SLOT};
            default -> new int[]{FUEL_SLOT}; // Sides for fuel
        };
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack itemStack, @Nullable Direction direction) {
        return canPlaceItem(index, itemStack);
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return index == OUTPUT_SLOT || (index == FUEL_SLOT && direction != Direction.UP);
    }

    // === SERIALIZATION === //

    // NBT handling
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", itemHandler.serializeNBT(registries));
        tag.putInt("Progress", progress);
        tag.putInt("MaxProgress", maxProgress);
        tag.putInt("FuelTime", fuelTime);
        tag.putInt("MaxFuelTime", maxFuelTime);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        itemHandler.deserializeNBT(registries, tag.getCompound("Inventory"));
        progress = tag.getInt("Progress");
        maxProgress = tag.getInt("MaxProgress");
        fuelTime = tag.getInt("FuelTime");
        maxFuelTime = tag.getInt("MaxFuelTime");

        // Sync ItemStackHandler to NonNullList after loading
        for (int i = 0; i < SLOT_COUNT; i++) {
            items.set(i, itemHandler.getStackInSlot(i).copy());
        }
    }

    // Capability handling
    public IItemHandlerModifiable getItemHandler(@Nullable Direction side) {
        if (side == null) return itemHandler;
        return sidedWrappers[side.ordinal() % sidedWrappers.length];
    }
}
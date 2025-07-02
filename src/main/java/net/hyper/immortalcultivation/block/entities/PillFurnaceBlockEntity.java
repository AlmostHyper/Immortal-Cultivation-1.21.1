package net.hyper.immortalcultivation.block.entities;

import net.hyper.immortalcultivation.recipe.PillFurnaceRecipe;
import net.hyper.immortalcultivation.recipe.PillFurnaceRecipeInput;
import net.hyper.immortalcultivation.registry.*;
import net.hyper.immortalcultivation.attachment.PlayerCultivationAttachment;
import net.hyper.immortalcultivation.ui.menus.PillFurnaceMenu;
import net.hyper.immortalcultivation.block.crafting.PillFurnaceBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public class PillFurnaceBlockEntity extends BlockEntity implements Container, MenuProvider {

    // Updated slot indices for 5 output slots
    public static final int INPUT_SLOT_1 = 0;
    public static final int INPUT_SLOT_2 = 1;
    public static final int INPUT_SLOT_3 = 2;
    public static final int FUEL_SLOT = 3;
    public static final int SPIRIT_STONE_SLOT = 4;
    public static final int OUTPUT_SLOT_COMMON = 5;
    public static final int OUTPUT_SLOT_UNCOMMON = 6;
    public static final int OUTPUT_SLOT_RARE = 7;
    public static final int OUTPUT_SLOT_EPIC = 8;
    public static final int OUTPUT_SLOT_LEGENDARY = 9;
    public static final int SLOT_COUNT = 10;

    // Item handling
    private final ItemStackHandler itemHandler = new ItemStackHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case INPUT_SLOT_1, INPUT_SLOT_2, INPUT_SLOT_3 -> isValidInput(stack);
                case FUEL_SLOT -> isValidFuel(stack);
                case SPIRIT_STONE_SLOT -> isValidSpiritStone(stack);
                case OUTPUT_SLOT_COMMON, OUTPUT_SLOT_UNCOMMON, OUTPUT_SLOT_RARE,
                     OUTPUT_SLOT_EPIC, OUTPUT_SLOT_LEGENDARY -> false; // Output only
                default -> false;
            };
        }
    };

    // Processing state
    protected int progress = 0;
    protected int maxProgress = 200; // Base processing time
    protected int fuelTime = 0;
    protected int maxFuelTime = 0;
    protected int batchSize = 1;
    protected int currentBatch = 0;

    // Player tracking for rarity calculations
    @Nullable
    protected UUID lastPlayerUUID;

    // Rarity enum for pill outputs
    public enum PillRarity {
        COMMON(0, "Common", 0.4f, OUTPUT_SLOT_COMMON, ChatFormatting.WHITE, 1.0),
        UNCOMMON(1, "Uncommon", 0.3f, OUTPUT_SLOT_UNCOMMON, ChatFormatting.GREEN, 1.5),
        RARE(2, "Rare", 0.2f, OUTPUT_SLOT_RARE, ChatFormatting.BLUE, 2.0),
        EPIC(3, "Epic", 0.08f, OUTPUT_SLOT_EPIC, ChatFormatting.LIGHT_PURPLE, 3.0),
        LEGENDARY(4, "Legendary", 0.02f, OUTPUT_SLOT_LEGENDARY, ChatFormatting.GOLD, 5.0);

        private final int level;
        private final String name;
        private final float baseChance;
        private final int outputSlot;
        private final ChatFormatting color;
        private final double effectMultiplier;

        PillRarity(int level, String name, float baseChance, int outputSlot, ChatFormatting color, double effectMultiplier) {
            this.level = level;
            this.name = name;
            this.baseChance = baseChance;
            this.outputSlot = outputSlot;
            this.color = color;
            this.effectMultiplier = effectMultiplier;
        }

        public int getLevel() { return level; }
        public String getName() { return name; }
        public float getBaseChance() { return baseChance; }
        public int getOutputSlot() { return outputSlot; }
        public ChatFormatting getColor() { return color; }
        public double getEffectMultiplier() { return effectMultiplier; }
    }

    // Container data for client sync
    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> maxProgress;
                case 2 -> fuelTime;
                case 3 -> maxFuelTime;
                case 4 -> batchSize;
                case 5 -> currentBatch;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> progress = value;
                case 1 -> maxProgress = value;
                case 2 -> fuelTime = value;
                case 3 -> maxFuelTime = value;
                case 4 -> batchSize = value;
                case 5 -> currentBatch = value;
            }
        }

        @Override
        public int getCount() {
            return 6;
        }
    };

    // Container items for compatibility
    protected NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);

    public PillFurnaceBlockEntity(BlockPos pos, BlockState blockState) {
        super(ICModBlockEntities.PILL_FURNACE_BLOCK_ENTITY.get(), pos, blockState);
    }

    public IItemHandler getItemHandler(@Nullable Direction side) {
        return itemHandler;
    }

    // NBT serialization
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", itemHandler.serializeNBT(registries));
        tag.putInt("Progress", progress);
        tag.putInt("FuelTime", fuelTime);
        tag.putInt("MaxFuelTime", maxFuelTime);
        tag.putInt("BatchSize", batchSize);
        tag.putInt("CurrentBatch", currentBatch);
        if (lastPlayerUUID != null) {
            tag.putUUID("LastPlayer", lastPlayerUUID);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        progress = tag.getInt("Progress");
        fuelTime = tag.getInt("FuelTime");
        maxFuelTime = tag.getInt("MaxFuelTime");
        batchSize = tag.getInt("BatchSize");
        currentBatch = tag.getInt("CurrentBatch");
        if (tag.hasUUID("LastPlayer")) {
            lastPlayerUUID = tag.getUUID("LastPlayer");
        }
    }

    // Network sync
    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    // Main processing logic
    public static void tick(Level level, BlockPos pos, BlockState state, PillFurnaceBlockEntity entity) {
        if (level.isClientSide()) return;

        boolean changed = false;
        boolean wasLit = entity.isLit();

        // Check and consume fuel if needed
        if (entity.needsFuel() && !entity.isLit()) {
            ItemStack fuelStack = entity.itemHandler.getStackInSlot(FUEL_SLOT);
            if (!fuelStack.isEmpty()) {
                int burnTime = entity.getFuelBurnTime(fuelStack);
                if (burnTime > 0) {
                    entity.fuelTime = entity.maxFuelTime = burnTime;
                    fuelStack.shrink(1);
                    changed = true;
                }
            }
        }

        // Process recipe if we have fuel and a valid recipe
        if (entity.isLit() && entity.canProcess()) {
            entity.progress++;
            int processSpeed = entity.getProcessingSpeed();
            if (entity.progress >= entity.maxProgress / processSpeed) {
                entity.processBatch();
                changed = true;
            }
        } else if (!entity.canProcess()) {
            entity.progress = 0;
            entity.currentBatch = 0;
        }

        // Consume fuel time
        if (entity.fuelTime > 0) {
            entity.fuelTime--;
            changed = true;
        }

        // Update block state if lit status changed
        if (wasLit != entity.isLit()) {
            changed = true;
            level.setBlock(pos, state.setValue(PillFurnaceBlock.LIT, entity.isLit()), 3);
        }

        if (changed) {
            entity.setChanged();
        }
    }

    // Batch processing system
    private void processBatch() {
        PillFurnaceRecipe recipe = getValidRecipe();
        if (recipe == null) return;

        // Calculate batch size based on available ingredients
        int possibleBatches = calculateMaxBatches(recipe);
        batchSize = Math.min(possibleBatches, 5); // Max 5 batches at once

        // Track rarity distribution for this batch
        int[] rarityCount = new int[PillRarity.values().length];

        // Consume ingredients once for the entire batch
        itemHandler.extractItem(INPUT_SLOT_1, recipe.getInput1Count() * batchSize, false);
        itemHandler.extractItem(INPUT_SLOT_2, recipe.getInput2Count() * batchSize, false);
        itemHandler.extractItem(INPUT_SLOT_3, recipe.getInput3Count() * batchSize, false);

        if (recipe.requiresSpiritStone()) {
            itemHandler.extractItem(SPIRIT_STONE_SLOT, batchSize, false);
        }

        // Process each pill in the batch
        for (int i = 0; i < batchSize; i++) {
            PillRarity rarity = calculatePillRarity(recipe);
            ItemStack result = createPillWithRarity(recipe.getResult(), rarity);

            // Add to appropriate output slot
            int outputSlot = rarity.getOutputSlot();
            ItemStack existingOutput = itemHandler.getStackInSlot(outputSlot);

            if (existingOutput.isEmpty()) {
                itemHandler.setStackInSlot(outputSlot, result);
            } else if (canStackPills(existingOutput, result)) {
                existingOutput.grow(result.getCount());
            }

            rarityCount[rarity.ordinal()]++;
        }

        // Send batch complete message
        if (lastPlayerUUID != null && level instanceof Level serverLevel) {
            Player player = serverLevel.getPlayerByUUID(lastPlayerUUID);
            if (player instanceof ServerPlayer serverPlayer) {
                sendBatchCompleteMessage(serverPlayer, rarityCount, recipe.getResult());
            }
        }

        // Reset progress
        progress = 0;
        currentBatch = 0;
    }

    // Send detailed batch completion message
    private void sendBatchCompleteMessage(ServerPlayer player, int[] rarityCount, ItemStack baseResult) {
        Component pillName = baseResult.getDisplayName();
        player.sendSystemMessage(
            Component.literal("Batch complete! Refined " + batchSize + " ")
                .append(pillName)
                .append(" pills:")
        );

        for (PillRarity rarity : PillRarity.values()) {
            int count = rarityCount[rarity.ordinal()];
            if (count > 0) {
                player.sendSystemMessage(
                    Component.literal("  " + count + " " + rarity.getName() + " quality")
                );
            }
        }
    }

    // Calculate max possible batches based on available ingredients
    private int calculateMaxBatches(PillFurnaceRecipe recipe) {
        ItemStack input1 = itemHandler.getStackInSlot(INPUT_SLOT_1);
        ItemStack input2 = itemHandler.getStackInSlot(INPUT_SLOT_2);
        ItemStack input3 = itemHandler.getStackInSlot(INPUT_SLOT_3);
        ItemStack spiritStone = itemHandler.getStackInSlot(SPIRIT_STONE_SLOT);

        int batch1 = input1.getCount() / recipe.getInput1Count();
        int batch2 = input2.isEmpty() ? Integer.MAX_VALUE : input2.getCount() / recipe.getInput2Count();
        int batch3 = input3.isEmpty() ? Integer.MAX_VALUE : input3.getCount() / recipe.getInput3Count();
        int batchSpirit = recipe.requiresSpiritStone() ? spiritStone.getCount() : Integer.MAX_VALUE;

        return Math.min(Math.min(Math.min(batch1, batch2), batch3), batchSpirit);
    }

    // Check if we can stack pills together
    private boolean canStackPills(ItemStack existing, ItemStack newStack) {
        return ItemStack.isSameItemSameComponents(existing, newStack) &&
            existing.getCount() + newStack.getCount() <= existing.getMaxStackSize();
    }

    // Calculate pill rarity based on player cultivation and other factors
    private PillRarity calculatePillRarity(PillFurnaceRecipe recipe) {
        RandomSource random = level.random;
        float totalChance = 0.0f;
        float roll = random.nextFloat();

        // Get player cultivation bonus if available
        float cultivationBonus = getCultivationBonus();
        float spiritStoneBonus = getSpiritStoneBonus();
        float tierBonus = (float) getTierSuccessModifier();

        // Calculate cumulative chances with bonuses
        for (PillRarity rarity : PillRarity.values()) {
            float adjustedChance = rarity.getBaseChance() * (1 + cultivationBonus + spiritStoneBonus + tierBonus);
            totalChance += adjustedChance;

            if (roll <= totalChance) {
                return rarity;
            }
        }

        return PillRarity.COMMON; // Fallback
    }

    // Get cultivation bonus from last player
    private float getCultivationBonus() {
        if (lastPlayerUUID != null && level instanceof Level serverLevel) {
            Player player = serverLevel.getPlayerByUUID(lastPlayerUUID);
            if (player != null) {
                PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION);
                if (cultivation != null) {
                    long qiCultivation = cultivation.getCurrentQiCultivation();
                    // Simple bonus: 1% per 1000 cultivation points, max 20%
                    return Math.min(qiCultivation / 100000.0f, 0.2f);
                }
            }
        }
        return 0.0f;
    }

    // Get spirit stone bonus
    private float getSpiritStoneBonus() {
        ItemStack spiritStone = itemHandler.getStackInSlot(SPIRIT_STONE_SLOT);
        if (!spiritStone.isEmpty()) {
            // Different spirit stone qualities provide different bonuses
            if (spiritStone.is(ICModItems.HIGH_QUALITY_SPIRIT_STONE.get())) {
                return 0.15f; // 15% bonus
            } else if (spiritStone.is(ICModItems.MEDIUM_QUALITY_SPIRIT_STONE.get())) {
                return 0.10f; // 10% bonus
            } else if (spiritStone.is(ICModItems.LOW_QUALITY_SPIRIT_STONE.get())) {
                return 0.05f; // 5% bonus
            }
        }
        return 0.0f;
    }

    // Create pill with specific rarity
    private ItemStack createPillWithRarity(ItemStack baseResult, PillRarity rarity) {
        ItemStack result = baseResult.copy();

        // Set custom name with rarity
        Component originalName = result.getHoverName();
        Component rarityName = Component.literal(rarity.getName() + " ").append(originalName);
        result.set(DataComponents.CUSTOM_NAME, rarityName);

        // Add custom data to track rarity - using "PillRarity" key to match CultivationPillItems
        CompoundTag customData = new CompoundTag();
        customData.putString("PillRarity", rarity.name());
        customData.putInt("RarityLevel", rarity.getLevel());
        result.set(DataComponents.CUSTOM_DATA, CustomData.of(customData));

        return result;
    }

    // Furnace tier system - override in subclasses
    protected double getTierSuccessModifier() {
        return 0.0; // Base furnace has no bonus
    }

    protected int getTierSpeedModifier() {
        return 1; // Base speed multiplier
    }

    protected int getTierFuelEfficiency() {
        return 100; // Base efficiency (100% = normal)
    }

    // Processing logic
    private boolean canProcess() {
        return getValidRecipe() != null && hasRoomForOutput();
    }

    private boolean needsFuel() {
        return getValidRecipe() != null;
    }

    private boolean isLit() {
        return fuelTime > 0;
    }

    private int getProcessingSpeed() {
        return getTierSpeedModifier();
    }

    // Get valid recipe for current inputs
    @Nullable
    private PillFurnaceRecipe getValidRecipe() {
        if (level == null) return null;

        SimpleContainer container = new SimpleContainer(3);
        container.setItem(0, itemHandler.getStackInSlot(INPUT_SLOT_1));
        container.setItem(1, itemHandler.getStackInSlot(INPUT_SLOT_2));
        container.setItem(2, itemHandler.getStackInSlot(INPUT_SLOT_3));

        PillFurnaceRecipeInput input = new PillFurnaceRecipeInput(
            container.getItem(0),
            container.getItem(1),
            container.getItem(2)
        );

        RecipeManager recipeManager = level.getRecipeManager();
        Optional<RecipeHolder<PillFurnaceRecipe>> recipe =
            recipeManager.getRecipeFor(ICModRecipes.PILL_FURNACE_TYPE.get(), input, level);

        return recipe.map(RecipeHolder::value).orElse(null);
    }

    // Check if there's room for output
    private boolean hasRoomForOutput() {
        for (int i = OUTPUT_SLOT_COMMON; i <= OUTPUT_SLOT_LEGENDARY; i++) {
            ItemStack outputStack = itemHandler.getStackInSlot(i);
            if (outputStack.isEmpty() || outputStack.getCount() < outputStack.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    // Fuel system
    private int getFuelBurnTime(ItemStack fuel) {
        if (fuel.isEmpty()) return 0;

        int baseBurnTime = switch (fuel.getItem().toString()) {
            case "minecraft:coal" -> 1600;
            case "minecraft:charcoal" -> 1600;
            case "minecraft:coal_block" -> 16000;
            case "minecraft:blaze_rod" -> 2400;
            case "minecraft:lava_bucket" -> 20000;
            default -> {
                Item item = fuel.getItem();
                if (item == ICModItems.LOW_QUALITY_SPIRIT_STONE.get()) {
                    yield 3200;
                } else if (item == ICModItems.MEDIUM_QUALITY_SPIRIT_STONE.get()) {
                    yield 6400;
                } else if (item == ICModItems.HIGH_QUALITY_SPIRIT_STONE.get()) {
                    yield 12800;
                } else if (item == ICModItems.TOP_QUALITY_SPIRIT_STONE.get()) {
                    yield 25600;
                }
                yield 0;
            }
        };

        return (baseBurnTime * getTierFuelEfficiency()) / 100;
    }

    // Validation methods
    private boolean isValidInput(ItemStack stack) {
        if (stack.isEmpty()) return false;
        // Add your input validation logic here
        return true; // For now, accept any item as input
    }

    private boolean isValidFuel(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return getFuelBurnTime(stack) > 0;
    }

    private boolean isValidSpiritStone(ItemStack stack) {
        if (stack.isEmpty()) return true; // Spirit stones are optional
        return stack.is(ICModItems.LOW_QUALITY_SPIRIT_STONE.get()) ||
            stack.is(ICModItems.MEDIUM_QUALITY_SPIRIT_STONE.get()) ||
            stack.is(ICModItems.HIGH_QUALITY_SPIRIT_STONE.get());
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
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.immortalcultivation.pill_furnace");
    }

    protected Component getDefaultName() {
        return Component.translatable("container.immortalcultivation.pill_furnace");
    }

    protected NonNullList<ItemStack> getItems() {
        for (int i = 0; i < SLOT_COUNT; i++) {
            items.set(i, itemHandler.getStackInSlot(i).copy());
        }
        return items;
    }

    protected void setItems(NonNullList<ItemStack> itemList) {
        for (int i = 0; i < Math.min(itemList.size(), SLOT_COUNT); i++) {
            itemHandler.setStackInSlot(i, itemList.get(i).copy());
            items.set(i, itemList.get(i).copy());
        }
    }

    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new PillFurnaceMenu(containerId, inventory, this, dataAccess);
    }

    // MenuProvider interface implementation - this is what gets called when using NetworkHooks.openScreen()
    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        // Track the player for cultivation bonuses
        lastPlayerUUID = player.getUUID();
        return new PillFurnaceMenu(containerId, inventory, this, dataAccess);
    }
}
package net.hyper.immortalcultivation.block.entities;

import net.hyper.immortalcultivation.recipe.PillFurnaceRecipe;
import net.hyper.immortalcultivation.recipe.PillFurnaceRecipeInput;
import net.hyper.immortalcultivation.registry.*;
import net.hyper.immortalcultivation.attachment.PlayerCultivationAttachment;
import net.hyper.immortalcultivation.ui.menus.PillFurnaceMenu;
import net.hyper.immortalcultivation.block.crafting.PillFurnaceBlock;
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

    // 5 Output slots - one for each rarity of the current pill
    public static final int OUTPUT_SLOT_COMMON = 5;
    public static final int OUTPUT_SLOT_UNCOMMON = 6;
    public static final int OUTPUT_SLOT_RARE = 7;
    public static final int OUTPUT_SLOT_EPIC = 8;
    public static final int OUTPUT_SLOT_LEGENDARY = 9;

    public static final int SLOT_COUNT = 10;

    // Pill rarity tiers (within each quality level)
    public enum PillRarity {
        COMMON(0, "Common", 0.5, 1.0),          // 50% chance, 1x effectiveness
        UNCOMMON(1, "Uncommon", 0.3, 1.25),     // 30% chance, 1.25x effectiveness
        RARE(2, "Rare", 0.15, 1.5),             // 15% chance, 1.5x effectiveness
        EPIC(3, "Epic", 0.04, 2.0),             // 4% chance, 2x effectiveness
        LEGENDARY(4, "Legendary", 0.01, 3.0);   // 1% chance, 3x effectiveness

        private final int tier;
        private final String name;
        private final double baseChance;
        private final double effectMultiplier;

        PillRarity(int tier, String name, double baseChance, double effectMultiplier) {
            this.tier = tier;
            this.name = name;
            this.baseChance = baseChance;
            this.effectMultiplier = effectMultiplier;
        }

        public int getTier() { return tier; }
        public String getName() { return name; }
        public double getBaseChance() { return baseChance; }
        public double getEffectMultiplier() { return effectMultiplier; }

        public int getOutputSlot() {
            return switch (this) {
                case COMMON -> OUTPUT_SLOT_COMMON;
                case UNCOMMON -> OUTPUT_SLOT_UNCOMMON;
                case RARE -> OUTPUT_SLOT_RARE;
                case EPIC -> OUTPUT_SLOT_EPIC;
                case LEGENDARY -> OUTPUT_SLOT_LEGENDARY;
            };
        }

        public net.minecraft.ChatFormatting getColor() {
            return switch (this) {
                case COMMON -> net.minecraft.ChatFormatting.WHITE;
                case UNCOMMON -> net.minecraft.ChatFormatting.GREEN;
                case RARE -> net.minecraft.ChatFormatting.BLUE;
                case EPIC -> net.minecraft.ChatFormatting.LIGHT_PURPLE;
                case LEGENDARY -> net.minecraft.ChatFormatting.GOLD;
            };
        }
    }

    // Item handler for inventory
    protected final ItemStackHandler itemHandler = new ItemStackHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
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
    protected int maxProgress = 200;
    protected int fuelTime = 0;
    protected int maxFuelTime = 0;
    protected UUID lastPlayerUUID = null;

    // Batch processing
    protected int batchSize = 1;
    protected int currentBatch = 0;

    // Container data for GUI sync
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
                .append(":")
                .withStyle(net.minecraft.ChatFormatting.GREEN)
        );

        for (PillRarity rarity : PillRarity.values()) {
            int count = rarityCount[rarity.ordinal()];
            if (count > 0) {
                player.sendSystemMessage(
                    Component.literal("  " + count + "x ")
                        .append(Component.literal(rarity.getName())
                            .withStyle(rarity.getColor()))
                );
            }
        }
    }

    // FIXED: Calculate maximum possible batches with proper spirit stone validation
    private int calculateMaxBatches(PillFurnaceRecipe recipe) {
        ItemStack input1 = itemHandler.getStackInSlot(INPUT_SLOT_1);
        ItemStack input2 = itemHandler.getStackInSlot(INPUT_SLOT_2);
        ItemStack input3 = itemHandler.getStackInSlot(INPUT_SLOT_3);
        ItemStack spiritStone = itemHandler.getStackInSlot(SPIRIT_STONE_SLOT);

        int batches = Integer.MAX_VALUE;

        // Check input 1 (always required)
        if (recipe.getInput1Count() > 0) {
            if (input1.isEmpty()) return 0; // No input1 available
            batches = Math.min(batches, input1.getCount() / recipe.getInput1Count());
        }

        // Check input 2 (if required by recipe)
        if (recipe.getInput2Count() > 0) {
            if (input2.isEmpty()) return 0; // Required but not available
            batches = Math.min(batches, input2.getCount() / recipe.getInput2Count());
        }

        // Check input 3 (if required by recipe)
        if (recipe.getInput3Count() > 0) {
            if (input3.isEmpty()) return 0; // Required but not available
            batches = Math.min(batches, input3.getCount() / recipe.getInput3Count());
        }

        // FIXED: Properly validate spirit stone requirement
        if (recipe.requiresSpiritStone()) {
            if (spiritStone.isEmpty() || !isValidSpiritStone(spiritStone)) {
                return 0; // Spirit stone required but not available or invalid
            }
            batches = Math.min(batches, spiritStone.getCount());
        }

        return Math.max(0, batches);
    }

    // Calculate pill rarity based on multiple factors
    private PillRarity calculatePillRarity(PillFurnaceRecipe recipe) {
        RandomSource random = level.random;
        double roll = random.nextDouble();

        // Get modifiers
        double cultivationModifier = getCultivationModifier();
        double spiritStoneModifier = getSpiritStoneModifier();
        double furnaceTierModifier = getTierSuccessModifier();
        double daoModifier = getDaoModifier();

        // Calculate cumulative chances
        double totalModifier = cultivationModifier * spiritStoneModifier * furnaceTierModifier * daoModifier;

        // Build cumulative probability distribution
        double cumulative = 0.0;
        PillRarity[] rarities = PillRarity.values();

        // Start from highest rarity and work down
        for (int i = rarities.length - 1; i >= 0; i--) {
            PillRarity rarity = rarities[i];
            double chance = rarity.getBaseChance() * totalModifier;

            // Cap individual rarity chances
            if (rarity == PillRarity.LEGENDARY) {
                chance = Math.min(chance, 0.15); // Max 15% for Legendary
            } else if (rarity == PillRarity.EPIC) {
                chance = Math.min(chance, 0.25); // Max 25% for Epic
            }

            cumulative += chance;
            if (roll < cumulative) {
                return rarity;
            }
        }

        return PillRarity.COMMON; // Default to common
    }

    // Create a pill item with rarity stored in NBT
    private ItemStack createPillWithRarity(ItemStack basePill, PillRarity rarity) {
        ItemStack result = basePill.copy();

        // Store rarity in CustomData using DataComponents
        CompoundTag tag = result.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putString("PillRarity", rarity.name());
        tag.putDouble("EffectMultiplier", rarity.getEffectMultiplier());
        result.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

        // Update display name to include rarity
        Component baseName = basePill.getDisplayName();
        Component rarityPrefix = Component.literal(rarity.getName() + " ")
            .withStyle(rarity.getColor());
        result.set(DataComponents.CUSTOM_NAME, rarityPrefix.copy().append(baseName));

        return result;
    }

    // Check if two pills can stack (same base item and rarity)
    private boolean canStackPills(ItemStack stack1, ItemStack stack2) {
        if (!ItemStack.isSameItem(stack1, stack2)) return false;

        // Get custom data using DataComponents
        CustomData customData1 = stack1.get(DataComponents.CUSTOM_DATA);
        CustomData customData2 = stack2.get(DataComponents.CUSTOM_DATA);

        if (customData1 == null || customData2 == null) return false;

        CompoundTag tag1 = customData1.copyTag();
        CompoundTag tag2 = customData2.copyTag();

        String rarity1 = tag1.getString("PillRarity");
        String rarity2 = tag2.getString("PillRarity");

        return rarity1.equals(rarity2);
    }

    // Get cultivation realm modifier
    private double getCultivationModifier() {
        if (lastPlayerUUID == null || level == null) return 1.0;

        Player player = level.getPlayerByUUID(lastPlayerUUID);
        if (player == null) return 1.0;

        PlayerCultivationAttachment cultivation = player.getData(ICModAttachmentTypes.PLAYER_CULTIVATION.get());
        if (cultivation == null) return 1.0;

        // Higher realms give better modifiers
        int realmLevel = cultivation.getCurrentQiRealm().getRealmLevel();
        return 1.0 + (realmLevel * 0.1); // +10% per realm level
    }

    // Get spirit stone quality modifier
    private double getSpiritStoneModifier() {
        ItemStack spiritStone = itemHandler.getStackInSlot(SPIRIT_STONE_SLOT);
        if (spiritStone.isEmpty()) return 1.0;

        Item item = spiritStone.getItem();
        if (item == ICModItems.TOP_QUALITY_SPIRIT_STONE.get()) {
            return 2.0; // 100% bonus
        } else if (item == ICModItems.HIGH_QUALITY_SPIRIT_STONE.get()) {
            return 1.5; // 50% bonus
        } else if (item == ICModItems.MEDIUM_QUALITY_SPIRIT_STONE.get()) {
            return 1.25; // 25% bonus
        } else if (item == ICModItems.LOW_QUALITY_SPIRIT_STONE.get()) {
            return 1.1; // 10% bonus
        }

        return 1.0;
    }

    // Get Dao modifier (placeholder)
    private double getDaoModifier() {
        return 1.0 + (level.random.nextDouble() * 0.2); // 0-20% bonus
    }

    // Track player interaction
    @Override
    public void startOpen(Player player) {
        Container.super.startOpen(player);
        lastPlayerUUID = player.getUUID();
    }

    // FIXED: Enhanced process validation with spirit stone requirement checking
    private boolean canProcess() {
        PillFurnaceRecipe recipe = getValidRecipe();
        if (recipe == null) return false;

        // Check spirit stone requirement FIRST
        if (recipe.requiresSpiritStone()) {
            ItemStack spiritStone = itemHandler.getStackInSlot(SPIRIT_STONE_SLOT);
            if (spiritStone.isEmpty() || !isValidSpiritStone(spiritStone)) {
                return false; // Recipe requires spirit stone but none available or invalid
            }
        }

        // Check if we have sufficient ingredients for at least 1 batch
        if (calculateMaxBatches(recipe) <= 0) {
            return false; // Not enough ingredients
        }

        // Check if any output slot can accept results
        for (PillRarity rarity : PillRarity.values()) {
            int slot = rarity.getOutputSlot();
            ItemStack outputSlot = itemHandler.getStackInSlot(slot);

            if (outputSlot.isEmpty() || outputSlot.getCount() < outputSlot.getMaxStackSize()) {
                return true; // At least one output slot available
            }
        }

        return false; // All output slots full
    }

    // Get processing speed
    public int getProcessingSpeed() {
        int baseSpeed = getTierSpeedModifier();

        ItemStack spiritStone = itemHandler.getStackInSlot(SPIRIT_STONE_SLOT);
        if (!spiritStone.isEmpty()) {
            Item item = spiritStone.getItem();
            if (item == ICModItems.TOP_QUALITY_SPIRIT_STONE.get()) {
                baseSpeed += 2;
            } else if (item == ICModItems.HIGH_QUALITY_SPIRIT_STONE.get()) {
                baseSpeed += 1;
            }
        }

        return baseSpeed;
    }

    private PillFurnaceRecipe getValidRecipe() {
        if (level == null) return null;

        PillFurnaceRecipeInput recipeInput = new PillFurnaceRecipeInput(
            itemHandler.getStackInSlot(INPUT_SLOT_1),
            itemHandler.getStackInSlot(INPUT_SLOT_2),
            itemHandler.getStackInSlot(INPUT_SLOT_3)
        );

        RecipeManager recipeManager = level.getRecipeManager();
        Optional<RecipeHolder<PillFurnaceRecipe>> recipe = recipeManager
            .getRecipeFor(ICModRecipes.PILL_FURNACE_TYPE.get(), recipeInput, level);

        return recipe.map(RecipeHolder::value).orElse(null);
    }

    private boolean needsFuel() {
        return true;
    }

    private boolean isLit() {
        return fuelTime > 0;
    }

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
        return !stack.isEmpty();
    }

    private boolean isValidFuel(ItemStack stack) {
        return getFuelBurnTime(stack) > 0;
    }

    // FIXED: Enhanced spirit stone validation
    private boolean isValidSpiritStone(ItemStack stack) {
        if (stack.isEmpty()) return true; // Empty is valid for slot acceptance
        Item item = stack.getItem();
        return item == ICModItems.LOW_QUALITY_SPIRIT_STONE.get() ||
            item == ICModItems.MEDIUM_QUALITY_SPIRIT_STONE.get() ||
            item == ICModItems.HIGH_QUALITY_SPIRIT_STONE.get() ||
            item == ICModItems.TOP_QUALITY_SPIRIT_STONE.get();
    }

    // Furnace tier system
    public int getFurnaceTier() {
        return 0;
    }

    protected double getTierSuccessModifier() {
        return 1.0;
    }

    protected int getTierSpeedModifier() {
        return 1;
    }

    protected int getTierFuelEfficiency() {
        return 100;
    }

    // Container implementation methods
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

    @Override
    public @Nullable AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return new PillFurnaceMenu(i, inventory, this, dataAccess);
    }
}
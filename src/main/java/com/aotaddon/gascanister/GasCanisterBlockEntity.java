package com.aotaddon.gascanister;

import com.aotaddon.gas.GasCanisterItemReflection;
import com.aotaddon.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Storage: 0-3000 gas, loaded by inserting a portable GasCanisterItem (which is
 * instantly drained into this block's reservoir, up to whatever capacity remains).
 *
 * Implements GeoBlockEntity so the model renders via GeoBlockRenderer instead of
 * a static blockstate model - same pattern daot's own StrwsBlockEntity uses.
 * No animation controllers registered yet (matching StrwsBlockEntity's own
 * empty registerControllers) - add one here once there's an actual animation
 * to trigger (e.g. a fill-level gauge).
 */
public class GasCanisterBlockEntity extends BlockEntity implements Container, MenuProvider, GeoBlockEntity {

    public static final int MAX_STORED_GAS = 3000;
    private static final String STORED_GAS_KEY = "StoredGas";

    private int storedGas = 0;
    private final NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache((GeoAnimatable) this);

    private final ContainerData data = new SimpleContainerData(1) {
        @Override
        public int get(int index) {
            return index == 0 ? storedGas : 0;
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) storedGas = value;
        }

        @Override
        public int getCount() {
            return 1;
        }
    };

    public GasCanisterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GAS_CANISTER.get(), pos, state);
    }

    public int getStoredGas() {
        return storedGas;
    }

    public ContainerData getContainerData() {
        return data;
    }

    /** @return gas actually accepted (may be less than requested if near capacity) */
    public int addGas(int amount) {
        int accepted = Math.min(amount, MAX_STORED_GAS - storedGas);
        if (accepted > 0) {
            storedGas += accepted;
            setChanged();
        }
        return accepted;
    }

    /** @return gas actually removed (may be less than requested if not enough stored) */
    public int removeGas(int amount) {
        int removed = Math.min(amount, storedGas);
        if (removed > 0) {
            storedGas -= removed;
            setChanged();
        }
        return removed;
    }

    // -------------------------------------------------------------------------
    // Server tick = instantly drains an inserted canister into storedGas
    // -------------------------------------------------------------------------

    /** Gas transferred from an inserted canister into storage per tick - "rapid but not instant". */
    private static final int CANISTER_DRAIN_PER_TICK = 25;

    public static void serverTick(Level level, BlockPos pos, BlockState state, GasCanisterBlockEntity be) {
        ItemStack slotStack = be.items.get(0);
        if (slotStack.isEmpty()) return;
        if (!GasCanisterItemReflection.isGasCanisterItem(slotStack.getItem())) return;

        int canisterGas = GasCanisterItemReflection.getGas(slotStack);
        if (canisterGas <= 0) return;

        int capacityRemaining = MAX_STORED_GAS - be.storedGas;
        if (capacityRemaining <= 0) return;

        int transfer = Math.min(CANISTER_DRAIN_PER_TICK, Math.min(canisterGas, capacityRemaining));
        GasCanisterItemReflection.setGas(slotStack, canisterGas - transfer);
        be.addGas(transfer);
    }

    // -------------------------------------------------------------------------
    // Container (single canister-input slot)
    // -------------------------------------------------------------------------

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return items.get(0).isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(0);
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack result = ContainerHelper.removeItem(items, 0, count);
        if (!result.isEmpty()) setChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, 0);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(0, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        if (level == null || level.getBlockEntity(worldPosition) != this) return false;
        return player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void clearContent() {
        items.set(0, ItemStack.EMPTY);
    }

    // -------------------------------------------------------------------------
    // MenuProvider
    // -------------------------------------------------------------------------

    @Override
    public Component getDisplayName() {
        return Component.literal("Gas Canister");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new GasCanisterMenu(containerId, playerInventory, this, data);
    }

    // -------------------------------------------------------------------------
    // GeoBlockEntity
    // -------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {

    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    // -------------------------------------------------------------------------
    // NBT persistence
    // -------------------------------------------------------------------------

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt(STORED_GAS_KEY, storedGas);
        ContainerHelper.saveAllItems(tag, items, registries);
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        storedGas = tag.getInt(STORED_GAS_KEY);
        items.clear();
        ContainerHelper.loadAllItems(tag, items, registries);
    }
}
// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import io.github._5thlayer.groundworks.Footprint;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;

/**
 * The Reactor running: Fission Fuel and water in, Superheated Steam out, at {@link ReactorSpec}'s one
 * rate and {@link ReactorCycle}'s stop. It has no heat (ADR 0012).
 *
 * <p>It keeps a water tank of the four water ports and a steam tank of the steam port, the back
 * middle of the middle layer. Each port block's fluid face reaches its tank (see
 * {@link WireworksRegistries}), and the Reactor hands its steam on to whatever stands against its steam
 * port, a pipe or the end of a Turbine row. Water is consumed, never created.
 *
 * <p>A loaded cell lasts {@link ReactorSpec#CELL_TICKS} ticks of running, and burnt out it leaves what
 * {@link #remainderOf} says in the depleted slot: a Depleted Fuel Cell for a Fuel Cell. The Reactor stops
 * while that slot has no room for what the cell it burns, or would load, leaves.
 */
public class ReactorBlockEntity extends BlockEntity implements Container {

    private static final String WATER = "Water";
    private static final String STEAM = "Steam";
    private static final String CELL_TICKS = "CellTicks";
    private static final String CELL_REMAINDER = "CellRemainder";

    private final NonNullList<ItemStack> items = NonNullList.withSize(ReactorSlots.SIZE, ItemStack.EMPTY);
    private final SingleFluidTank water = new SingleFluidTank(Fluids.WATER, ReactorSpec.WATER_CAPACITY, this::setChanged);
    private final SingleFluidTank steam =
            new SingleFluidTank(SteamFluids.SUPERHEATED_STEAM_SOURCE.get(), ReactorSpec.STEAM_CAPACITY, this::setChanged);
    private final ResourceHandler<FluidResource> waterFace = new FluidFace(water, true, true);
    private final ResourceHandler<FluidResource> steamFace = new FluidFace(steam, false, true);
    private final ResourceHandler<ItemResource> itemFace = new ReactorItemHandler(this);
    private int cellTicks;
    /** What the loaded cell leaves once burnt, empty when none is loaded or it leaves nothing. */
    private ItemStack cellRemainder = ItemStack.EMPTY;

    public ReactorBlockEntity(BlockPos pos, BlockState state) {
        super(WireworksRegistries.REACTOR_ENTITY.get(), pos, state);
    }

    /**
     * The fluid face of the footprint block numbered {@code part}, or null where its port does not
     * open onto {@code side}: only the four water ports and the steam port open, each onto its outer
     * face. The null side answers every port block, for a tool that names no face.
     */
    @Nullable ResourceHandler<FluidResource> fluidFace(int part, Direction facing, @Nullable Direction side) {
        ReactorPort port = SteamFootprints.reactorPort(part);
        if (port == null || side != null && !SteamFootprints.reactorPortOpens(part, facing, side)) {
            return null;
        }
        return port == ReactorPort.WATER ? waterFace : steamFace;
    }

    ResourceHandler<ItemResource> itemFace() {
        return itemFace;
    }

    /** The water in its tank, in mB, which Jade shows. */
    public int water() {
        return water.amount();
    }

    /** The superheated steam in its tank, in mB, which Jade shows. */
    public int steam() {
        return steam.amount();
    }

    /** The loaded cell's ticks of running left, 0 when none is loaded, which Jade shows. */
    public int cellTicksLeft() {
        return cellTicks;
    }

    void serverTick() {
        if (!(level instanceof ServerLevel)) {
            return;
        }
        ItemStack leaves = cellTicks > 0 ? cellRemainder : remainderOf(items.get(ReactorSlots.FUEL));
        ReactorCycle.Tick tick = ReactorCycle.tick(water.amount(), steam.room(), hasDepletedRoomFor(leaves),
                ReactorSpec.MILLIBUCKETS_PER_TICK, cellTicks, ReactorSpec.CELL_TICKS, this::loadCell);
        if (tick.converted() > 0) {
            // Unit for unit, as the Boiler: the heat exchange Factorio's reactor feeds is a temperature change.
            water.fill(water.amount() - tick.converted());
            steam.fill(steam.amount() + tick.converted());
            cellTicks = tick.ticksLeft();
            if (tick.burntOut()) {
                leave(cellRemainder);
                cellRemainder = ItemStack.EMPTY;
            }
            setChanged();
        }
        passSteamOn();
    }

    private boolean loadCell() {
        ItemStack fuel = items.get(ReactorSlots.FUEL);
        if (!isFissionFuel(fuel)) {
            return false;
        }
        cellRemainder = remainderOf(fuel);
        fuel.shrink(1);
        return true;
    }

    /**
     * What one burnt cell of this Fission Fuel leaves: the Fuel Cell a Depleted Fuel Cell, any other its
     * crafting remainder, as the Boiler treats fuel, or nothing.
     */
    static ItemStack remainderOf(ItemStack fuel) {
        if (fuel.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (fuel.is(WireworksRegistries.FUEL_CELL.get())) {
            return new ItemStack(WireworksRegistries.DEPLETED_FUEL_CELL.get());
        }
        ItemStackTemplate remainder = fuel.getCraftingRemainder();
        return remainder == null ? ItemStack.EMPTY : remainder.create();
    }

    /** Whether the depleted slot takes {@code leaves} whole; a cell that leaves nothing always has room. */
    private boolean hasDepletedRoomFor(ItemStack leaves) {
        ItemStack depleted = items.get(ReactorSlots.DEPLETED);
        return leaves.isEmpty() || depleted.isEmpty()
                || ItemStack.isSameItemSameComponents(depleted, leaves)
                && depleted.getCount() + leaves.getCount() <= depleted.getMaxStackSize();
    }

    private void leave(ItemStack leaves) {
        if (leaves.isEmpty()) {
            return;
        }
        ItemStack depleted = items.get(ReactorSlots.DEPLETED);
        if (depleted.isEmpty()) {
            items.set(ReactorSlots.DEPLETED, leaves.copy());
        } else {
            depleted.grow(leaves.getCount());
        }
    }

    /** Offers the steam to whatever stands against the steam port. */
    private void passSteamOn() {
        if (steam.amount() <= 0) {
            return;
        }
        Direction facing = getBlockState().getValue(Footprint.FACING);
        Direction side = SteamFootprints.reactorSteamSide(facing);
        BlockPos port = WireworksRegistries.REACTOR_FOOTPRINT.positions(worldPosition, facing).get(SteamFootprints.REACTOR_STEAM_PART);
        ResourceHandler<FluidResource> next = level.getCapability(Capabilities.Fluid.BLOCK, port.relative(side), side.getOpposite());
        if (next != null) {
            ResourceHandlerUtil.move(steam, next, resource -> true, steam.amount(), null);
        }
    }

    public static boolean isFissionFuel(ItemStack stack) {
        return !stack.isEmpty() && stack.is(WireworksTags.FISSION_FUEL);
    }

    @Override
    public int getContainerSize() {
        return ReactorSlots.SIZE;
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot >= 0 && slot < ReactorSlots.SIZE ? items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        stack.limitSize(getMaxStackSize(stack));
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    /** Fuel goes in its slot; nothing is ever placed in the depleted slot, which only the Reactor fills. */
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == ReactorSlots.FUEL && isFissionFuel(stack);
    }

    /**
     * Only depleted cells come out. A hopper under the anchor reaches this container itself rather
     * than the item face, and would otherwise pull the fuel the Reactor is about to burn.
     */
    @Override
    public boolean canTakeItem(Container target, int slot, ItemStack stack) {
        return slot == ReactorSlots.DEPLETED;
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items.clear();
        ContainerHelper.loadAllItems(input, items);
        water.fill(input.getIntOr(WATER, 0));
        steam.fill(input.getIntOr(STEAM, 0));
        cellTicks = Math.max(0, input.getIntOr(CELL_TICKS, 0));
        cellRemainder = input.read(CELL_REMAINDER, ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putInt(WATER, water.amount());
        output.putInt(STEAM, steam.amount());
        output.putInt(CELL_TICKS, cellTicks);
        output.store(CELL_REMAINDER, ItemStack.OPTIONAL_CODEC, cellRemainder);
    }
}

// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import java.util.List;

import io.github._5thlayer.groundworks.Footprint;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
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
 * <p>Each item of Fission Fuel loaded lasts {@link ReactorSpec#CELL_TICKS} ticks of running, and burnt out it leaves what
 * {@link #remainderOf} says in the depleted slot: a Depleted Fuel Cell for a Fuel Cell. The Reactor stops
 * while that slot has no room for what the Fission Fuel it burns, or would load, leaves.
 *
 * <p>A water container in the water slot empties into the water tank, whole, and goes to the bucket
 * output, as the Boiler's does (see {@link FluidContainers}). Its screen is {@link ReactorMenu}.
 */
public class ReactorBlockEntity extends BlockEntity implements Container, MenuProvider {

    public static final int DATA_WATER = 0;
    public static final int DATA_STEAM = 1;
    public static final int DATA_WATER_CAPACITY = 2;
    public static final int DATA_STEAM_CAPACITY = 3;
    public static final int DATA_CELL_TICKS = 4;
    public static final int DATA_STATUS = 5;
    public static final int DATA_COUNT = 6;

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
    /** What the loaded Fission Fuel leaves once burnt, empty when none is loaded or it leaves nothing. */
    private ItemStack leftByBurning = ItemStack.EMPTY;

    /** What the last tick did, for the screen and Jade; worked out again every tick, so never saved. */
    private ReactorStatus status = ReactorStatus.NO_FUEL;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_WATER -> water.amount();
                case DATA_STEAM -> steam.amount();
                case DATA_WATER_CAPACITY -> water.capacity();
                case DATA_STEAM_CAPACITY -> steam.capacity();
                case DATA_CELL_TICKS -> cellTicks;
                case DATA_STATUS -> status.ordinal();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

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

    public ContainerData data() {
        return data;
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

    /** Whether the Reactor ran on its last tick, and if not, why not. */
    public ReactorStatus status() {
        return status;
    }

    void serverTick() {
        if (!(level instanceof ServerLevel)) {
            return;
        }
        pullWater();
        emptyWaterContainer();
        ItemStack leaves = cellTicks > 0 ? leftByBurning : remainderOf(items.get(ReactorSlots.FUEL));
        ReactorCycle.Tick tick = ReactorCycle.tick(water.amount(), steam.room(), hasDepletedRoomFor(leaves),
                ReactorSpec.MILLIBUCKETS_PER_TICK, cellTicks, ReactorSpec.CELL_TICKS, this::loadCell);
        if (tick.converted() > 0) {
            // Unit for unit, as the Boiler: the heat exchange Factorio's reactor feeds is a temperature change.
            water.fill(water.amount() - tick.converted());
            steam.fill(steam.amount() + tick.converted());
            cellTicks = tick.ticksLeft();
            if (tick.burntOut()) {
                leave(leftByBurning);
                leftByBurning = ItemStack.EMPTY;
            }
            setChanged();
        }
        status = statusAfter(tick.converted(), leaves);
        passSteamOn();
    }

    /**
     * Draws water from whatever stands against each water port, since a pipe pushes nothing. Another
     * machine's port is left alone, as the Boiler leaves it.
     */
    private void pullWater() {
        Direction facing = getBlockState().getValue(Footprint.FACING);
        List<BlockPos> positions = WireworksRegistries.REACTOR_FOOTPRINT.positions(worldPosition, facing);
        for (int part = 0; part < positions.size() && water.room() > 0; part++) {
            if (SteamFootprints.reactorPort(part) != ReactorPort.WATER) {
                continue;
            }
            for (Direction side : Direction.Plane.HORIZONTAL) {
                if (!SteamFootprints.reactorPortOpens(part, facing, side)) {
                    continue;
                }
                BlockPos beside = positions.get(part).relative(side);
                BlockState there = level.getBlockState(beside);
                if (SteamFootprints.pushesItsOwn(there)) {
                    continue;
                }
                ResourceHandler<FluidResource> from = level.getCapability(Capabilities.Fluid.BLOCK, beside, side.getOpposite());
                if (from != null) {
                    ResourceHandlerUtil.move(from, water, resource -> resource.is(Fluids.WATER), water.room(), null);
                }
            }
        }
    }

    /** Why a tick that converted nothing did not, asked in {@link ReactorCycle}'s order. */
    private ReactorStatus statusAfter(int converted, ItemStack leaves) {
        if (converted > 0) {
            return ReactorStatus.RUNNING;
        }
        if (water.amount() < ReactorSpec.MILLIBUCKETS_PER_TICK) {
            // A water container still in its slot with the tank this low is waiting on the bucket output.
            return items.get(ReactorSlots.WATER).isEmpty() ? ReactorStatus.NO_WATER : ReactorStatus.BUCKETS_FULL;
        }
        if (steam.room() < ReactorSpec.MILLIBUCKETS_PER_TICK) {
            return ReactorStatus.STEAM_BACKED_UP;
        }
        return hasDepletedRoomFor(leaves) ? ReactorStatus.NO_FUEL : ReactorStatus.DEPLETED_FULL;
    }

    /** Empties one container in the water slot into the water tank and sends it to the bucket output. */
    private void emptyWaterContainer() {
        ItemStack stack = items.get(ReactorSlots.WATER);
        ItemStack after = FluidContainers.emptyInto(stack, items.get(ReactorSlots.BUCKETS), water);
        if (after == null) {
            return;
        }
        stack.shrink(1);
        items.set(ReactorSlots.BUCKETS, after);
        setChanged();
    }

    private boolean loadCell() {
        ItemStack fuel = items.get(ReactorSlots.FUEL);
        if (!isFissionFuel(fuel)) {
            return false;
        }
        leftByBurning = remainderOf(fuel);
        fuel.shrink(1);
        return true;
    }

    /**
     * What one burnt item of this Fission Fuel leaves: the Fuel Cell a Depleted Fuel Cell, any other its
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

    /** Whether the depleted slot takes {@code leaves} whole; a Fission Fuel that leaves nothing always has room. */
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

    /**
     * Whether {@code slot} takes {@code stack}: Fission Fuel in the fuel slot, a water container in the
     * water slot, and nothing in the depleted slot or the bucket output, which only the Reactor fills.
     * The menu asks this too, on either side.
     */
    public static boolean accepts(int slot, ItemStack stack) {
        return switch (slot) {
            case ReactorSlots.FUEL -> isFissionFuel(stack);
            case ReactorSlots.WATER -> FluidContainers.holdsWater(stack);
            default -> false;
        };
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

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return accepts(slot, stack);
    }

    /**
     * Only the depleted slot and the bucket output come out. A hopper under the anchor reaches this
     * container itself rather than the item face, and would otherwise pull the fuel the Reactor is about
     * to burn.
     */
    @Override
    public boolean canTakeItem(Container target, int slot, ItemStack stack) {
        return !ReactorSlots.isInput(slot);
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ReactorMenu(containerId, playerInventory, this, data);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items.clear();
        ContainerHelper.loadAllItems(input, items);
        water.fill(input.getIntOr(WATER, 0));
        steam.fill(input.getIntOr(STEAM, 0));
        cellTicks = Math.max(0, input.getIntOr(CELL_TICKS, 0));
        leftByBurning = input.read(CELL_REMAINDER, ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putInt(WATER, water.amount());
        output.putInt(STEAM, steam.amount());
        output.putInt(CELL_TICKS, cellTicks);
        output.store(CELL_REMAINDER, ItemStack.OPTIONAL_CODEC, leftByBurning);
    }
}

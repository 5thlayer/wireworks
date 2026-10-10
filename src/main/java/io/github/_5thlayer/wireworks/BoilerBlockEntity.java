// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

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
 * The Boiler running: solid fuel and water in, steam out, at {@link BoilerSpec}'s rate and
 * {@link BoilerCycle}'s stall.
 *
 * <p>It keeps a water tank of the front row's three ports and a steam tank of the back middle port.
 * Each port block's fluid face reaches its tank (see {@link WireworksRegistries}), and the Boiler
 * hands the steam on to whatever stands against its steam port, a pipe or a Steam Engine. Water is
 * consumed, never created.
 *
 * <p>Fuel is banked whole as joules by {@link FuelBuffer} and spent a tick at a time, as
 * {@link BoilerFuel} values an item from its vanilla burn time.
 */
public class BoilerBlockEntity extends BlockEntity implements Container, MenuProvider {

    public static final int DATA_FUEL = 0;
    public static final int DATA_FUEL_CAPACITY = 1;
    public static final int DATA_WATER = 2;
    public static final int DATA_STEAM = 3;
    public static final int DATA_WATER_CAPACITY = 4;
    public static final int DATA_STEAM_CAPACITY = 5;
    public static final int DATA_COUNT = 6;

    private static final String WATER = "Water";
    private static final String STEAM = "Steam";

    private final NonNullList<ItemStack> items = NonNullList.withSize(BoilerSlots.SIZE, ItemStack.EMPTY);
    private final FuelBuffer fuel = new FuelBuffer();
    private final SingleFluidTank water = new SingleFluidTank(Fluids.WATER, BoilerSpec.WATER_PORTS * BoilerSpec.PORT_VOLUME, this::setChanged);
    private final SingleFluidTank steam =
            new SingleFluidTank(SteamFluids.STEAM_SOURCE.get(), BoilerSpec.PORT_VOLUME, this::setChanged);
    private final ResourceHandler<FluidResource> waterFace = new FluidFace(water, true, true);
    private final ResourceHandler<FluidResource> steamFace = new FluidFace(steam, false, true);
    private final ResourceHandler<ItemResource> itemFace = new BoilerItemHandler(this);

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_FUEL -> clampToInt(fuel.storedJoules());
                case DATA_FUEL_CAPACITY -> clampToInt(fuel.gaugeCapacity());
                case DATA_WATER -> water.amount();
                case DATA_STEAM -> steam.amount();
                case DATA_WATER_CAPACITY -> water.capacity();
                case DATA_STEAM_CAPACITY -> steam.capacity();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case DATA_FUEL -> fuel.load(value, fuel.lastLitJoules());
                case DATA_FUEL_CAPACITY -> fuel.load(fuel.storedJoules(), value);
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public BoilerBlockEntity(BlockPos pos, BlockState state) {
        super(WireworksRegistries.BOILER_ENTITY.get(), pos, state);
    }

    /** Saturating: a wrapped int would draw a full gauge as an empty one. */
    private static int clampToInt(long value) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, value));
    }

    public ContainerData data() {
        return data;
    }

    /**
     * The fluid face of the footprint block numbered {@code part}, or null where its port does not
     * open onto {@code side}: the anchor and the back corners open onto nothing. The null side
     * answers every port block, for a tool that names no face.
     */
    @Nullable ResourceHandler<FluidResource> fluidFace(int part, Direction facing, @Nullable Direction side) {
        BoilerPort port = SteamFootprints.boilerPort(part);
        if (port == null || side != null && !SteamFootprints.boilerPortOpens(part, facing, side)) {
            return null;
        }
        return port == BoilerPort.WATER ? waterFace : steamFace;
    }

    ResourceHandler<ItemResource> itemFace() {
        return itemFace;
    }

    void serverTick() {
        if (!(level instanceof ServerLevel)) {
            return;
        }
        int converted = BoilerCycle.tick(water.amount(), steam.room(), BoilerSpec.MILLIBUCKETS_PER_TICK,
                BoilerSpec.JOULES_PER_TICK, fuel, this::light);
        if (converted > 0) {
            // Unit for unit: the Boiler is a temperature change, not a reaction.
            water.fill(water.amount() - converted);
            steam.fill(steam.amount() + converted);
            setChanged();
        }
        passSteamOn();
    }

    /** Offers the steam to whatever stands against the steam port. */
    private void passSteamOn() {
        if (steam.amount() <= 0) {
            return;
        }
        Direction facing = getBlockState().getValue(Footprint.FACING);
        Direction side = SteamFootprints.boilerSteamSide(facing);
        BlockPos port = WireworksRegistries.BOILER_FOOTPRINT.positions(worldPosition, facing).get(SteamFootprints.BOILER_STEAM_PART);
        ResourceHandler<FluidResource> next = level.getCapability(Capabilities.Fluid.BLOCK, port.relative(side), side.getOpposite());
        if (next != null) {
            ResourceHandlerUtil.move(steam, next, resource -> true, steam.amount(), null);
        }
    }

    /**
     * Consumes one fuel item whole and reports what it was worth: its burn time in joules, times the
     * burner's effectivity.
     */
    private long light() {
        ItemStack stack = items.get(BoilerSlots.FUEL);
        long joules = fuelValue(stack);
        if (joules <= 0L) {
            return 0L;
        }
        ItemStackTemplate remainder = stack.getCraftingRemainder();
        stack.shrink(1);
        if (stack.isEmpty() && remainder != null) {
            items.set(BoilerSlots.FUEL, remainder.create());
        }
        return joules;
    }

    private long fuelValue(ItemStack stack) {
        return level == null || stack.isEmpty() ? 0L : BoilerFuel.joules(stack.getBurnTime(null, level.fuelValues()));
    }

    public boolean isFuel(ItemStack stack) {
        return fuelValue(stack) > 0L;
    }

    @Override
    public int getContainerSize() {
        return BoilerSlots.SIZE;
    }

    @Override
    public boolean isEmpty() {
        return items.get(BoilerSlots.FUEL).isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot == BoilerSlots.FUEL ? items.get(BoilerSlots.FUEL) : ItemStack.EMPTY;
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
        stack.limitSize(getMaxStackSize());
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == BoilerSlots.FUEL && isFuel(stack);
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
        return new BoilerMenu(containerId, playerInventory, this, data);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items.clear();
        ContainerHelper.loadAllItems(input, items);
        fuel.load(input.getLongOr("FuelJoules", 0L), input.getLongOr("FuelLitJoules", 0L));
        water.fill(input.getIntOr(WATER, 0));
        steam.fill(input.getIntOr(STEAM, 0));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putLong("FuelJoules", fuel.storedJoules());
        output.putLong("FuelLitJoules", fuel.lastLitJoules());
        output.putInt(WATER, water.amount());
        output.putInt(STEAM, steam.amount());
    }
}

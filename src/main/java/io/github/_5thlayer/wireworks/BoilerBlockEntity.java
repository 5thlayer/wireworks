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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
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
    public static final int DATA_STATUS = 6;
    public static final int DATA_COUNT = 7;

    private static final String WATER = "Water";
    private static final String STEAM = "Steam";

    private final NonNullList<ItemStack> items = NonNullList.withSize(BoilerSlots.SIZE, ItemStack.EMPTY);
    private final FuelBuffer fuel = new FuelBuffer();
    private final SingleFluidTank water = new SingleFluidTank(Fluids.WATER, BoilerSpec.WATER_CAPACITY, this::setChanged);
    private final SingleFluidTank steam =
            new SingleFluidTank(SteamFluids.STEAM_SOURCE.get(), BoilerSpec.STEAM_CAPACITY, this::setChanged);
    private final ResourceHandler<FluidResource> waterFace = new FluidFace(water, true, true);
    private final ResourceHandler<FluidResource> steamFace = new FluidFace(steam, false, true);
    private final ResourceHandler<ItemResource> itemFace = new BoilerItemHandler(this);

    /** What the last tick did, for the screen and Jade; worked out again every tick, so never saved. */
    private BoilerStatus status = BoilerStatus.NO_FUEL;

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
                case DATA_STATUS -> status.ordinal();
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

    public int water() {
        return water.amount();
    }

    public int steam() {
        return steam.amount();
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
        exchange(BoilerSlots.WATER);
        int converted = BoilerCycle.tick(water.amount(), steam.room(), BoilerSpec.MILLIBUCKETS_PER_TICK,
                BoilerSpec.JOULES_PER_TICK, fuel, this::light);
        if (converted > 0) {
            // Unit for unit: the Boiler is a temperature change, not a reaction.
            water.fill(water.amount() - converted);
            steam.fill(steam.amount() + converted);
            setChanged();
        }
        status = statusAfter(converted);
        exchange(BoilerSlots.STEAM);
        passSteamOn();
    }

    /** Why a tick that converted nothing did not, asked in {@link BoilerCycle}'s order. */
    private BoilerStatus statusAfter(int converted) {
        if (converted > 0) {
            return BoilerStatus.BOILING;
        }
        if (water.amount() < BoilerSpec.MILLIBUCKETS_PER_TICK) {
            // A water container still in its slot with the tank this low is waiting on the output.
            return items.get(BoilerSlots.WATER).isEmpty() ? BoilerStatus.NO_WATER : BoilerStatus.OUTPUT_FULL;
        }
        if (steam.room() < BoilerSpec.MILLIBUCKETS_PER_TICK) {
            return BoilerStatus.STEAM_BACKED_UP;
        }
        ItemStack stack = items.get(BoilerSlots.FUEL);
        return fuelValue(stack) > 0L && !hasOutputRoom(leftBy(stack)) ? BoilerStatus.OUTPUT_FULL : BoilerStatus.NO_FUEL;
    }

    public BoilerStatus status() {
        return status;
    }

    /**
     * Empties one container in the water slot into the water tank, or fills one in the steam slot from
     * the steam tank, and sends it to the output. It goes whole or not at all: a bucket waits while
     * the tank has too little room or too little steam for all of it, or while the output has no room
     * for what it becomes. The swap runs on a copy of the two slots inside a transaction, as the
     * Boiler's own slots are not transactional, and lands only once it has committed.
     */
    private void exchange(int slot) {
        ItemStack stack = items.get(slot);
        if (stack.isEmpty()) {
            return;
        }
        ItemStacksResourceHandler swap = new ItemStacksResourceHandler(
                NonNullList.of(ItemStack.EMPTY, stack.copyWithCount(1), items.get(BoilerSlots.OUTPUT).copy()));
        ResourceHandler<FluidResource> container = new SwapAccess(swap).getCapability(Capabilities.Fluid.ITEM);
        if (container == null) {
            return;
        }
        boolean emptying = slot == BoilerSlots.WATER;
        // A bucket moves all or nothing by itself; a larger container must too.
        long whole = emptying ? held(container) : room(container, steam.fluid());
        try (Transaction transaction = Transaction.openRoot()) {
            int moved = emptying
                    ? ResourceHandlerUtil.move(container, water, resource -> true, Integer.MAX_VALUE, transaction)
                    : ResourceHandlerUtil.move(steam, container, resource -> true, Integer.MAX_VALUE, transaction);
            if (moved <= 0 || moved != whole) {
                return;
            }
            transaction.commit();
        }
        stack.shrink(1);
        items.set(BoilerSlots.OUTPUT, swap.getResource(1).toStack(swap.getAmountAsInt(1)));
        setChanged();
    }

    private static long held(ResourceHandler<FluidResource> container) {
        long held = 0;
        for (int index = 0; index < container.size(); index++) {
            held += container.getAmountAsLong(index);
        }
        return held;
    }

    private static long room(ResourceHandler<FluidResource> container, FluidResource fluid) {
        long room = 0;
        for (int index = 0; index < container.size(); index++) {
            room += container.getCapacityAsLong(index, fluid) - container.getAmountAsLong(index);
        }
        return room;
    }

    /** The container being swapped, at index 0 of a copy, and the output it goes to, at index 1. */
    private record SwapAccess(ItemStacksResourceHandler swap) implements ItemAccess {

        @Override
        public ItemResource getResource() {
            return swap.getResource(0);
        }

        @Override
        public int getAmount() {
            return swap.getAmountAsInt(0);
        }

        @Override
        public int insert(ItemResource resource, int amount, TransactionContext transaction) {
            return swap.insert(1, resource, amount, transaction);
        }

        @Override
        public int extract(ItemResource resource, int amount, TransactionContext transaction) {
            return swap.extract(0, resource, amount, transaction);
        }
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
     * burner's effectivity. What the item leaves, a lava bucket's bucket, goes to the output; with no
     * room for it there, nothing is lit.
     */
    private long light() {
        ItemStack stack = items.get(BoilerSlots.FUEL);
        long joules = fuelValue(stack);
        ItemStack left = leftBy(stack);
        if (joules <= 0L || !hasOutputRoom(left)) {
            return 0L;
        }
        stack.shrink(1);
        leave(left);
        return joules;
    }

    /** What burning one of {@code stack} leaves, a lava bucket's bucket, or nothing. */
    private static ItemStack leftBy(ItemStack stack) {
        ItemStackTemplate remainder = stack.getCraftingRemainder();
        return remainder == null ? ItemStack.EMPTY : remainder.create();
    }

    private boolean hasOutputRoom(ItemStack left) {
        ItemStack output = items.get(BoilerSlots.OUTPUT);
        return left.isEmpty() || output.isEmpty()
                || (ItemStack.isSameItemSameComponents(output, left)
                        && output.getCount() + left.getCount() <= output.getMaxStackSize());
    }

    private void leave(ItemStack left) {
        if (left.isEmpty()) {
            return;
        }
        ItemStack output = items.get(BoilerSlots.OUTPUT);
        if (output.isEmpty()) {
            items.set(BoilerSlots.OUTPUT, left);
        } else {
            output.grow(left.getCount());
        }
    }

    private long fuelValue(ItemStack stack) {
        return fuelValue(level, stack);
    }

    private static long fuelValue(@Nullable Level level, ItemStack stack) {
        return level == null || stack.isEmpty() ? 0L : BoilerFuel.joules(stack.getBurnTime(null, level.fuelValues()));
    }

    /**
     * Whether {@code slot} takes {@code stack}: fuel in the fuel slot, a container of water in the water
     * slot, an empty container steam can go into in the steam slot, and nothing in the output, which only
     * the Boiler fills. The menu asks this too, on either side.
     */
    public static boolean accepts(@Nullable Level level, int slot, ItemStack stack) {
        return switch (slot) {
            case BoilerSlots.FUEL -> fuelValue(level, stack) > 0L;
            case BoilerSlots.WATER -> holdsWater(stack);
            case BoilerSlots.STEAM -> takesSteam(stack);
            default -> false;
        };
    }

    private static @Nullable ResourceHandler<FluidResource> fluidsIn(ItemStack stack) {
        return stack.isEmpty() ? null : ItemAccess.forStack(stack.copyWithCount(1)).getCapability(Capabilities.Fluid.ITEM);
    }

    private static boolean holdsWater(ItemStack stack) {
        ResourceHandler<FluidResource> container = fluidsIn(stack);
        if (container == null) {
            return false;
        }
        for (int index = 0; index < container.size(); index++) {
            if (container.getAmountAsLong(index) > 0 && container.getResource(index).getFluid() == Fluids.WATER) {
                return true;
            }
        }
        return false;
    }

    private static boolean takesSteam(ItemStack stack) {
        ResourceHandler<FluidResource> container = fluidsIn(stack);
        if (container == null || !ResourceHandlerUtil.isEmpty(container)) {
            return false;
        }
        FluidResource steam = FluidResource.of(SteamFluids.STEAM_SOURCE.get());
        for (int index = 0; index < container.size(); index++) {
            if (container.getCapacityAsLong(index, steam) > 0 && container.isValid(index, steam)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getContainerSize() {
        return BoilerSlots.SIZE;
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot >= 0 && slot < BoilerSlots.SIZE ? items.get(slot) : ItemStack.EMPTY;
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
        return accepts(level, slot, stack);
    }

    /**
     * Only the output comes out. A hopper under the anchor reaches this container itself rather than
     * the item face, and would otherwise pull the fuel the Boiler is about to burn.
     */
    @Override
    public boolean canTakeItem(Container target, int slot, ItemStack stack) {
        return slot == BoilerSlots.OUTPUT;
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

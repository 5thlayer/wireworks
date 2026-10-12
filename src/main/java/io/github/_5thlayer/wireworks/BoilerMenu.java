// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The Boiler's menu, laid out as Fieldworks' Harvester's: the three input slots down the left, fuel,
 * then a water container, then an empty container for steam, each beside the gauge it feeds or fills;
 * the output on the right, where all three leave what they become; and the state the Boiler is in,
 * which says why a stopped Boiler stopped.
 */
public class BoilerMenu extends AbstractContainerMenu {

    /** The input column, one slot per row, each row's gauge beside it. */
    public static final int INPUT_X = 8;
    public static final int FUEL_Y = 18;
    public static final int WATER_Y = 36;
    public static final int STEAM_Y = 54;
    /** The output, right of the gauges, level with the middle row. */
    public static final int OUTPUT_X = 134;
    public static final int OUTPUT_Y = 36;
    /** Where the player's inventory starts, from the top of the screen: below the state line. */
    public static final int INVENTORY_Y = 100;

    /** The container slots shift-click fills, in the order it tries them. */
    private static final int[] INPUTS = {BoilerSlots.FUEL, BoilerSlots.WATER, BoilerSlots.STEAM};

    private final Container container;
    private final ContainerData data;
    private final Level level;

    /** Client side: the block entity is not reachable, so the slot stands over a stub. */
    public BoilerMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(BoilerSlots.SIZE),
                new SimpleContainerData(BoilerBlockEntity.DATA_COUNT));
    }

    public BoilerMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(WireworksRegistries.BOILER_MENU.get(), containerId);
        this.container = container;
        this.data = data;
        this.level = playerInventory.player.level();
        checkContainerSize(container, BoilerSlots.SIZE);
        checkContainerDataCount(data, BoilerBlockEntity.DATA_COUNT);

        // In the container's own order, so a menu slot's index is the container slot's.
        addSlot(new InputSlot(container, BoilerSlots.FUEL, INPUT_X, FUEL_Y));
        addSlot(new InputSlot(container, BoilerSlots.OUTPUT, OUTPUT_X, OUTPUT_Y));
        addSlot(new InputSlot(container, BoilerSlots.WATER, INPUT_X, WATER_Y));
        addSlot(new InputSlot(container, BoilerSlots.STEAM, INPUT_X, STEAM_Y));

        addStandardInventorySlots(playerInventory, 8, INVENTORY_Y);
        addDataSlots(data);
    }

    /** How full the fuel buffer is, 0..1: the gauge fills to the last item the Boiler lit. */
    public float fuelLevel() {
        int capacity = data.get(BoilerBlockEntity.DATA_FUEL_CAPACITY);
        return capacity <= 0 ? 0F : Math.min(1F, data.get(BoilerBlockEntity.DATA_FUEL) / (float) capacity);
    }

    public int fuelStored() {
        return data.get(BoilerBlockEntity.DATA_FUEL);
    }

    public int fuelCapacity() {
        return data.get(BoilerBlockEntity.DATA_FUEL_CAPACITY);
    }

    public int water() {
        return data.get(BoilerBlockEntity.DATA_WATER);
    }

    public int steam() {
        return data.get(BoilerBlockEntity.DATA_STEAM);
    }

    public int waterCapacity() {
        return data.get(BoilerBlockEntity.DATA_WATER_CAPACITY);
    }

    public BoilerState state() {
        return BoilerState.of(data.get(BoilerBlockEntity.DATA_STATE));
    }

    public int steamCapacity() {
        return data.get(BoilerBlockEntity.DATA_STEAM_CAPACITY);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (index < BoilerSlots.SIZE) {
            if (!moveItemStackTo(stack, BoilerSlots.SIZE, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveToItsSlot(stack)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    /**
     * To the one slot that takes the stack, and never the output: vanilla's merge pass skips
     * {@code mayPlace}, so a range spanning the output would top up a stack already in it.
     */
    private boolean moveToItsSlot(ItemStack stack) {
        for (int slot : INPUTS) {
            if (BoilerBlockEntity.accepts(level, slot, stack)) {
                return moveItemStackTo(stack, slot, slot + 1, false);
            }
        }
        return false;
    }

    /** A slot that takes only what the Boiler's slot of the same index does: nothing, for the output. */
    private final class InputSlot extends Slot {

        InputSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return BoilerBlockEntity.accepts(level, getContainerSlot(), stack);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }
}

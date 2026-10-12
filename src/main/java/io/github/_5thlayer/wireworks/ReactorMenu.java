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

/**
 * The Reactor's menu, laid out as the Boiler's: the fuel slot beside the loaded cell's bar and the
 * water slot beside the water gauge, down the left, with the steam gauge below them; the depleted slot
 * and the bucket output on the right, where what each input becomes goes; and the status the Reactor is
 * in, which says why a stopped Reactor stopped.
 */
public class ReactorMenu extends AbstractContainerMenu {

    /** The input column, one row each, each row's bar or gauge beside it. */
    public static final int INPUT_X = 8;
    public static final int FUEL_Y = 18;
    public static final int WATER_Y = 36;
    /** The steam gauge's row, with no slot: Superheated Steam has no bucket. */
    public static final int STEAM_Y = 54;
    /** The outputs, right of the gauges, each level with the input that feeds it. */
    public static final int OUTPUT_X = 134;
    /** Where the player's inventory starts, from the top of the screen: below the status line. */
    public static final int INVENTORY_Y = 100;

    /** The container slots shift-click fills, in the order it tries them. */
    private static final int[] INPUTS = {ReactorSlots.FUEL, ReactorSlots.WATER};

    private final Container container;
    private final ContainerData data;

    /** Client side: the block entity is not reachable, so the slot stands over a stub. */
    public ReactorMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(ReactorSlots.SIZE),
                new SimpleContainerData(ReactorBlockEntity.DATA_COUNT));
    }

    public ReactorMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(WireworksRegistries.REACTOR_MENU.get(), containerId);
        this.container = container;
        this.data = data;
        checkContainerSize(container, ReactorSlots.SIZE);
        checkContainerDataCount(data, ReactorBlockEntity.DATA_COUNT);

        // In the container's own order, so a menu slot's index is the container slot's.
        addSlot(new ReactorSlot(container, ReactorSlots.FUEL, INPUT_X, FUEL_Y));
        addSlot(new ReactorSlot(container, ReactorSlots.DEPLETED, OUTPUT_X, FUEL_Y));
        addSlot(new ReactorSlot(container, ReactorSlots.WATER, INPUT_X, WATER_Y));
        addSlot(new ReactorSlot(container, ReactorSlots.BUCKETS, OUTPUT_X, WATER_Y));

        addStandardInventorySlots(playerInventory, 8, INVENTORY_Y);
        addDataSlots(data);
    }

    public int water() {
        return data.get(ReactorBlockEntity.DATA_WATER);
    }

    public int steam() {
        return data.get(ReactorBlockEntity.DATA_STEAM);
    }

    public int waterCapacity() {
        return data.get(ReactorBlockEntity.DATA_WATER_CAPACITY);
    }

    public int steamCapacity() {
        return data.get(ReactorBlockEntity.DATA_STEAM_CAPACITY);
    }

    /** The loaded cell's ticks of running left, 0 when none is loaded. */
    public int cellTicksLeft() {
        return data.get(ReactorBlockEntity.DATA_CELL_TICKS);
    }

    /** How much of the loaded cell is left, 0..1. */
    public float cellLevel() {
        return Math.min(1F, cellTicksLeft() / (float) ReactorSpec.CELL_TICKS);
    }

    /** Whether the Reactor ran on its last tick, and if not, why not. */
    public ReactorStatus status() {
        return ReactorStatus.of(data.get(ReactorBlockEntity.DATA_STATUS));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (index < ReactorSlots.SIZE) {
            if (!moveItemStackTo(stack, ReactorSlots.SIZE, slots.size(), true)) {
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
     * To the one slot that takes the stack, and never an output: vanilla's merge pass skips
     * {@code mayPlace}, so a range spanning an output would top up a stack already in it.
     */
    private boolean moveToItsSlot(ItemStack stack) {
        for (int slot : INPUTS) {
            if (ReactorBlockEntity.accepts(slot, stack)) {
                return moveItemStackTo(stack, slot, slot + 1, false);
            }
        }
        return false;
    }

    /** A slot that takes only what the Reactor's slot of the same index does: nothing, for an output. */
    private static final class ReactorSlot extends Slot {

        ReactorSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return ReactorBlockEntity.accepts(getContainerSlot(), stack);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }
}

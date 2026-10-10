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
 * The Boiler's menu: one fuel slot and three gauges. A Boiler that has stopped is out of fuel, out of
 * water or backed up, and those three look the same from outside the block.
 */
public class BoilerMenu extends AbstractContainerMenu {

    private final Container container;
    private final ContainerData data;

    /** Client side: the block entity is not reachable, so the slot stands over a stub. */
    public BoilerMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(BoilerSlots.SIZE),
                new SimpleContainerData(BoilerBlockEntity.DATA_COUNT));
    }

    public BoilerMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(WireworksRegistries.BOILER_MENU.get(), containerId);
        this.container = container;
        this.data = data;
        checkContainerSize(container, BoilerSlots.SIZE);
        checkContainerDataCount(data, BoilerBlockEntity.DATA_COUNT);

        addSlot(new Slot(container, BoilerSlots.FUEL, 56, 53));

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, 142));
        }
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
        } else if (!moveItemStackTo(stack, 0, BoilerSlots.SIZE, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }
}

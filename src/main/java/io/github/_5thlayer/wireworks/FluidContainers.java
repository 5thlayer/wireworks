// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
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
 * A machine's container slots: a bucket, or any item with NeoForge's fluid item capability, emptied into
 * a tank or filled from one, and sent to the machine's output slot. The Boiler empties water and fills
 * steam this way, and the Reactor empties water.
 *
 * <p>A container goes whole or not at all: it waits while the tank has too little room, or too little
 * fluid, for all of it, or while the output has no room for what it becomes. The swap runs on a copy of
 * the two slots inside a transaction, as the machines' own slots are not transactional, and the caller
 * lands it only once it has committed.
 */
final class FluidContainers {

    private FluidContainers() {
    }

    /**
     * Empties one of {@code stack} into {@code tank}, the output slot holding {@code output}. What the
     * output slot holds after, the empty container added, or null when nothing moved: the caller then
     * takes one from {@code stack} and sets the output to it.
     */
    static @Nullable ItemStack emptyInto(ItemStack stack, ItemStack output, ResourceHandler<FluidResource> tank) {
        return swap(stack, output, tank, null);
    }

    /** Fills one of {@code stack} with {@code fluid} from {@code tank}; as {@link #emptyInto}. */
    static @Nullable ItemStack fillFrom(ItemStack stack, ItemStack output, ResourceHandler<FluidResource> tank,
                                        FluidResource fluid) {
        return swap(stack, output, tank, fluid);
    }

    /** @param filling the fluid a fill draws, or null to empty the container into the tank */
    private static @Nullable ItemStack swap(ItemStack stack, ItemStack output, ResourceHandler<FluidResource> tank,
                                            @Nullable FluidResource filling) {
        if (stack.isEmpty()) {
            return null;
        }
        ItemStacksResourceHandler swap = new ItemStacksResourceHandler(
                NonNullList.of(ItemStack.EMPTY, stack.copyWithCount(1), output.copy()));
        ResourceHandler<FluidResource> container = new SwapAccess(swap).getCapability(Capabilities.Fluid.ITEM);
        if (container == null) {
            return null;
        }
        // A bucket moves all or nothing by itself; a larger container must too.
        long whole = filling == null ? held(container) : room(container, filling);
        try (Transaction transaction = Transaction.openRoot()) {
            int moved = filling == null
                    ? ResourceHandlerUtil.move(container, tank, resource -> true, Integer.MAX_VALUE, transaction)
                    : ResourceHandlerUtil.move(tank, container, resource -> true, Integer.MAX_VALUE, transaction);
            if (moved <= 0 || moved != whole) {
                return null;
            }
            transaction.commit();
        }
        return swap.getResource(1).toStack(swap.getAmountAsInt(1));
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

    private static @Nullable ResourceHandler<FluidResource> fluidsIn(ItemStack stack) {
        return stack.isEmpty() ? null : ItemAccess.forStack(stack.copyWithCount(1)).getCapability(Capabilities.Fluid.ITEM);
    }

    /** Whether {@code stack} is a container holding water, which a water slot takes. */
    static boolean holdsWater(ItemStack stack) {
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

    /** Whether {@code stack} is an empty container {@code fluid} can go into. */
    static boolean takes(ItemStack stack, FluidResource fluid) {
        ResourceHandler<FluidResource> container = fluidsIn(stack);
        if (container == null || !ResourceHandlerUtil.isEmpty(container)) {
            return false;
        }
        for (int index = 0; index < container.size(); index++) {
            if (container.getCapacityAsLong(index, fluid) > 0 && container.isValid(index, fluid)) {
                return true;
            }
        }
        return false;
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
}

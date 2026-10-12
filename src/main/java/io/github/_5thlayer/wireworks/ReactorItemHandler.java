// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import io.github._5thlayer.libworks.transfer.GuardedResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * The Reactor's item face, for every side: Fission Fuel in, Depleted Fuel Cells out, and nothing the
 * other way. The guarded base keeps a slot-less call from walking past either refusal.
 */
final class ReactorItemHandler extends GuardedResourceHandler<ItemResource> {

    ReactorItemHandler(ReactorBlockEntity reactor) {
        super(VanillaContainerWrapper.of(reactor));
    }

    @Override
    public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
        return index == ReactorSlots.FUEL ? super.insert(index, resource, amount, transaction) : 0;
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
        return index == ReactorSlots.DEPLETED ? super.extract(index, resource, amount, transaction) : 0;
    }
}

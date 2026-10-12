// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import io.github._5thlayer.libworks.transfer.GuardedResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * The Boiler's item face, for every side: fuel and containers in, the output out, and nothing the
 * other way. The fuel in the slot is what the Boiler is burning, and the guarded base keeps a
 * slot-less call from walking past either refusal; which slot an item may go in is the Boiler's
 * {@code canPlaceItem}.
 */
final class BoilerItemHandler extends GuardedResourceHandler<ItemResource> {

    BoilerItemHandler(BoilerBlockEntity boiler) {
        super(VanillaContainerWrapper.of(boiler));
    }

    @Override
    public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
        return index != BoilerSlots.OUTPUT ? super.insert(index, resource, amount, transaction) : 0;
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
        return index == BoilerSlots.OUTPUT ? super.extract(index, resource, amount, transaction) : 0;
    }
}

// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import io.github._5thlayer.wireworks.internal.GuardedResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * The Boiler's one item face, for every side and for the null side: fuel in and nothing out. Every
 * slot refuses extraction, since a Boiler holds only the fuel it is burning, and a pipe or funnel
 * must not take it back by naming no slot either.
 */
final class BoilerItemHandler extends GuardedResourceHandler<ItemResource> {

    BoilerItemHandler(BoilerBlockEntity boiler) {
        super(VanillaContainerWrapper.of(boiler));
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
        if (!BoilerSlots.canExtract(convertIndex(index))) {
            return 0;
        }
        return super.extract(index, resource, amount, transaction);
    }
}

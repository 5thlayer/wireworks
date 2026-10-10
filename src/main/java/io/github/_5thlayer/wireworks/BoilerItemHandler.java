// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import io.github._5thlayer.wireworks.internal.GuardedResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * The Boiler's item face, for every side: fuel in, nothing out. The fuel in the slot is what the
 * Boiler is burning, and the guarded base keeps a slot-less extract from walking past the refusal.
 */
final class BoilerItemHandler extends GuardedResourceHandler<ItemResource> {

    BoilerItemHandler(BoilerBlockEntity boiler) {
        super(VanillaContainerWrapper.of(boiler));
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
        return 0;
    }
}

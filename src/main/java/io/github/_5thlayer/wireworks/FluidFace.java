// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import io.github._5thlayer.wireworks.internal.GuardedResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * A fluid port of a machine: its tank, with a direction refused. Water goes in and out of the
 * Boiler, steam only out, and the Steam Engine takes steam and gives none back. The refusal is
 * per slot, and the guarded base sends the slot-less calls through it, so a pipe cannot walk past it.
 */
final class FluidFace extends GuardedResourceHandler<FluidResource> {

    private final boolean canInsert;
    private final boolean canExtract;

    FluidFace(ResourceHandler<FluidResource> tank, boolean canInsert, boolean canExtract) {
        super(tank);
        this.canInsert = canInsert;
        this.canExtract = canExtract;
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
        return canInsert ? super.insert(index, resource, amount, transaction) : 0;
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
        return canExtract ? super.extract(index, resource, amount, transaction) : 0;
    }
}

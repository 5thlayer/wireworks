// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;

/** One tank of one fluid: the Boiler's water and steam and the Steam Engine's steam. */
final class SteamTank extends FluidStacksResourceHandler {

    private final FluidResource fluid;
    private final Runnable onChange;

    SteamTank(Fluid fluid, int capacity, Runnable onChange) {
        super(1, capacity);
        this.fluid = FluidResource.of(fluid);
        this.onChange = onChange;
    }

    FluidResource fluid() {
        return fluid;
    }

    int amount() {
        return getAmountAsInt(0);
    }

    int capacity() {
        return capacity;
    }

    int room() {
        return capacity - amount();
    }

    /** Sets the tank to {@code amount} of its fluid, for the block entity's own bookkeeping and load. */
    void fill(int amount) {
        int clamped = Math.max(0, Math.min(capacity, amount));
        set(0, clamped == 0 ? FluidResource.EMPTY : fluid, clamped);
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        return resource.equals(fluid);
    }

    @Override
    protected void onContentsChanged(int index, FluidStack previous) {
        onChange.run();
    }
}

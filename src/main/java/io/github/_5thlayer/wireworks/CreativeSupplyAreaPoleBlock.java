// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * A supply-area pole that is also an unlimited generator for its network: the dev tool an energy face is checked with.
 *
 * <p>Place it, place a machine beside it, read Jade. Without it, checking a machine's draw by hand
 * means first building a working power chain, because the shipped poles generate nothing.
 *
 * <h2>It is a subclass, not a fourth {@link PoleTier}</h2>
 *
 * <p>{@code PoleTier} is Factorio's ladder of poles, and the config and the registered blocks each
 * walk {@code PoleTier.values()}. A dev tool has nothing to say in either, so this is a separate block
 * wearing the substation's footprint: the largest area, so a machine can go anywhere nearby.
 *
 * <p>Everything else is inherited and stays shared: the column, the scan, the water-fill
 * rationing, the Jade line and the network it is wired into. What is being tested has to be the thing
 * that ships, so the only difference is that {@link ElectricNetworks} counts it as a generator --
 * and that is decided from the blockstate rather than from here, because Minecraft rebuilds a block
 * entity from the type when a chunk loads and never asks the block again.
 *
 * <h2>It ships</h2>
 *
 * <p>Creative tab and {@code /give}, with no recipe, which is what keeps it out of survival -- the
 * same arrangement vanilla's creative-only blocks have.
 */
public class CreativeSupplyAreaPoleBlock extends SupplyAreaPoleBlock {

    /** The registry path. Not derived from a tier -- it is not on the ladder. */
    public static final String BLOCK_NAME = "creative_electric_pole";

    public CreativeSupplyAreaPoleBlock(BlockBehaviour.Properties props) {
        super(PoleTier.SUBSTATION, props);
    }
}

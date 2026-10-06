// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * A pole's block entity as {@link ElectricNetworks} sees it: whatever reports into the network tick,
 * a Distribution Pole, a Transmission Pole or a Transformer, so a change to any of the three sets
 * rebuilds the topology. Implemented by block entities, which already answer the position, the level
 * and whether they are gone.
 */
interface NetworkPole {

    /** This pole as {@link PoleNetworks} sees it: where it stands and what kind it is. */
    PoleNetworks.Pole shape();

    BlockPos getBlockPos();

    Level getLevel();

    boolean isRemoved();

    /** What the pole's Electric Network did on the last tick, for the Jade line and for nothing else. */
    void recordNetworkTick(NetworkReading reading, NetworkExchange exchange);
}

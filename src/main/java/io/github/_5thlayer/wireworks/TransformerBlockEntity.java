// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A Transformer's block entity: it holds no energy and scans no area. It reports into the network
 * tick, so a Transformer joining or leaving rebuilds the Electric Networks, and keeps what its
 * network last did for the Jade line.
 */
public class TransformerBlockEntity extends BlockEntity implements NetworkPole {

    private NetworkReading lastReading = NetworkReading.NONE;
    private NetworkExchange lastExchange = NetworkExchange.NONE;

    public TransformerBlockEntity(BlockPos pos, BlockState state) {
        super(WireworksRegistries.TRANSFORMER_ENTITY.get(), pos, state);
    }

    @Override
    public PoleNetworks.Pole shape() {
        return LevelWires.pole(getBlockPos(), TransformerSpec.kind());
    }

    void serverTick() {
        if (level != null && !level.isClientSide()) {
            ElectricNetworks.of(level).report(this);
        }
    }

    /**
     * What crossed this Transformer last tick: its share of each District it joins, imported to the
     * District or exported from it. Jade reads this.
     */
    public NetworkExchange networkExchange() {
        return lastExchange;
    }

    /** The network as it stood after the last tick, summed over its Districts. Jade reads this. */
    public NetworkReading networkReading() {
        return lastReading;
    }

    @Override
    public void recordNetworkTick(NetworkReading reading, NetworkExchange exchange) {
        lastReading = reading;
        lastExchange = exchange;
    }
}

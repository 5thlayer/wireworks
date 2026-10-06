// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A Transmission Pole's block entity: it holds no energy and settles nothing. A block entity is how
 * the level finds the poles standing near a placed one without walking blocks, and how a wire is
 * drawn from a pole; it also reports the pole into the network tick, so a Transmission Pole joining
 * or leaving rebuilds the Electric Networks.
 */
public class TransmissionPoleBlockEntity extends BlockEntity implements NetworkPole {

    private NetworkExchange lastExchange = NetworkExchange.NONE;

    public TransmissionPoleBlockEntity(BlockPos pos, BlockState state) {
        super(WireworksRegistries.TRANSMISSION_POLE_ENTITY.get(), pos, state);
    }

    @Override
    public PoleNetworks.Pole shape() {
        return LevelWires.pole(getBlockPos(), PoleKind.TRANSMISSION);
    }

    void serverTick() {
        if (level != null && !level.isClientSide()) {
            ElectricNetworks.of(level).report(this);
        }
    }

    /** The Electric Network's surplus and shortfall last tick, and what crossed its Transformers. Jade reads this. */
    public NetworkExchange networkExchange() {
        return lastExchange;
    }

    @Override
    public void recordNetworkTick(NetworkReading reading, NetworkExchange exchange) {
        lastExchange = exchange;
    }
}

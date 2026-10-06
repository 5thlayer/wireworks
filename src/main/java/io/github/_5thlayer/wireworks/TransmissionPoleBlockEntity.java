// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A Transmission Pole's block entity: it holds nothing and does nothing. A block entity is how the
 * level finds the poles standing near a placed one without walking blocks, and how a wire is drawn
 * from a pole.
 */
public class TransmissionPoleBlockEntity extends BlockEntity {

    public TransmissionPoleBlockEntity(BlockPos pos, BlockState state) {
        super(WireworksRegistries.TRANSMISSION_POLE_ENTITY.get(), pos, state);
    }
}

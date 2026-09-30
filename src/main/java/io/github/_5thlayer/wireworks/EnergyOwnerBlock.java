// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A block with no block entity whose energy face is another block's, such as a multiblock's hull.
 * Unresolved, a pole would count one machine once per block it reaches, and offer and draw it that
 * many times.
 */
public interface EnergyOwnerBlock {

    /** The block whose energy the one at {@code pos} stands for. */
    BlockPos energyOwner(BlockPos pos, BlockState state);
}

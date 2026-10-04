// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import io.github._5thlayer.groundworks.Footprint;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The Accumulator's anchor: the corner of its flat 2x2. */
public class AccumulatorBlock extends EnergyAnchorBlock {

    public static final String BLOCK_NAME = "accumulator";

    public AccumulatorBlock(Properties properties) {
        super(properties);
    }

    @Override
    Footprint footprint() {
        return WireworksRegistries.ACCUMULATOR_FOOTPRINT;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AccumulatorBlockEntity(pos, state);
    }
}

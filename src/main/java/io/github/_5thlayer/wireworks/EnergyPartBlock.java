// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import io.github._5thlayer.groundworks.Footprint;
import io.github._5thlayer.groundworks.FootprintPartBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

/**
 * A block of a Wireworks footprint other than its anchor. It names the anchor as its {@link EnergyOwner
 * energy owner}: without it, a pole whose area covers several blocks of one footprint counts the
 * same buffer once per block.
 */
public class EnergyPartBlock extends FootprintPartBlock implements EnergyOwnerBlock {

    public EnergyPartBlock(Properties properties, Supplier<Footprint> footprint) {
        super(properties.noLootTable(), footprint);
    }

    @Override
    public BlockPos energyOwner(BlockPos pos, BlockState state) {
        return footprint().shape().originOf(pos, state.getValue(PART), state.getValue(Footprint.FACING));
    }
}

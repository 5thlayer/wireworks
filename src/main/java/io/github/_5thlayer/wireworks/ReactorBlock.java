// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import io.github._5thlayer.groundworks.Footprint;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The Reactor's anchor: the bottom centre of its 3x3x3, walled in by its parts. Fuel reaches it, and
 * its depleted cells leave it, through every block of the footprint.
 */
public class ReactorBlock extends EnergyAnchorBlock {

    public static final String BLOCK_NAME = "reactor";

    public ReactorBlock(Properties properties) {
        super(properties.strength(5.0F).requiresCorrectToolForDrops());
    }

    @Override
    Footprint footprint() {
        return WireworksRegistries.REACTOR_FOOTPRINT;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ReactorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                           BlockEntityType<T> type) {
        if (level.isClientSide() || type != WireworksRegistries.REACTOR_ENTITY.get()) {
            return null;
        }
        return (lvl, pos, st, be) -> {
            if (be instanceof ReactorBlockEntity reactor) {
                reactor.serverTick();
            }
        };
    }
}

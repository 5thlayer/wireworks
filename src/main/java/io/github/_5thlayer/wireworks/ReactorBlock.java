// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import io.github._5thlayer.groundworks.Footprint;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The Reactor's anchor: the bottom centre of its 3x3x3, walled in by its parts. Fuel reaches it, and
 * its depleted cells leave it, through every block of the footprint, and a click on any of them opens
 * its screen.
 */
public class ReactorBlock extends EnergyAnchorBlock {

    public static final String BLOCK_NAME = "reactor";

    public ReactorBlock(Properties properties) {
        super(properties.requiresCorrectToolForDrops(), 5.0F);
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

    /** A plain right-click opens the Reactor, since it has slots. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof ReactorBlockEntity reactor) {
            player.openMenu(reactor);
        }
        return InteractionResult.CONSUME;
    }
}

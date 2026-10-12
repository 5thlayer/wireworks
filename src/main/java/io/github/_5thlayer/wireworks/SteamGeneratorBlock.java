// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jspecify.annotations.Nullable;

/**
 * The anchor block of a {@link SteamGeneratorBlockEntity}: the Steam Engine's or the Steam Turbine's.
 * {@code RUNNING} is set while it burns and for a second after, on the anchor and on every part that
 * carries it too.
 */
public abstract class SteamGeneratorBlock extends EnergyAnchorBlock {

    public static final BooleanProperty RUNNING = BooleanProperty.create("running");

    SteamGeneratorBlock(Properties properties) {
        super(properties.requiresCorrectToolForDrops());
        registerDefaultState(defaultBlockState().setValue(RUNNING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(RUNNING);
    }

    abstract BlockEntityType<? extends SteamGeneratorBlockEntity> entityType();

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                           BlockEntityType<T> type) {
        if (level.isClientSide() || type != entityType()) {
            return null;
        }
        return (lvl, pos, st, be) -> {
            if (be instanceof SteamGeneratorBlockEntity generator) {
                generator.serverTick();
            }
        };
    }
}

// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import io.github._5thlayer.groundworks.Footprint;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The Boiler's anchor: the middle of the front row of its 3x2. Fuel reaches it through every block of
 * the footprint, the facing shows which side the firebox is on, and {@code LIT} is set while it boils.
 */
public class BoilerBlock extends EnergyAnchorBlock {

    public static final String BLOCK_NAME = "boiler";
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    /** How much light a lit firebox gives off, a lit furnace's 13. */
    private static final int LIT_LIGHT = 13;

    public BoilerBlock(Properties properties) {
        super(properties.requiresCorrectToolForDrops().lightLevel(state -> state.getValue(LIT) ? LIT_LIGHT : 0), 3.5F);
        registerDefaultState(defaultBlockState().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LIT);
    }

    @Override
    Footprint footprint() {
        return WireworksRegistries.BOILER_FOOTPRINT;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BoilerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                           BlockEntityType<T> type) {
        if (level.isClientSide() || type != WireworksRegistries.BOILER_ENTITY.get()) {
            return null;
        }
        return (lvl, pos, st, be) -> {
            if (be instanceof BoilerBlockEntity boiler) {
                boiler.serverTick();
            }
        };
    }

    /** A plain right-click opens the Boiler, since it has a fuel slot. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof BoilerBlockEntity boiler) {
            player.openMenu(boiler);
        }
        return InteractionResult.CONSUME;
    }
}

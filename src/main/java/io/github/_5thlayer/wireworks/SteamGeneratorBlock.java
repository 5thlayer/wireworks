// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** The anchor block of a {@link SteamGeneratorBlockEntity}: the Steam Engine's or the Steam Turbine's. */
public abstract class SteamGeneratorBlock extends EnergyAnchorBlock {

    SteamGeneratorBlock(Properties properties) {
        super(properties.requiresCorrectToolForDrops());
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

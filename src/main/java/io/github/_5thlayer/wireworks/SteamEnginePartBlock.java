// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import com.mojang.serialization.MapCodec;
import io.github._5thlayer.groundworks.Footprint;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

import java.util.function.Supplier;

/**
 * A part of the Steam Engine, drawn from its own model as every {@link DrawnPartBlock} is, and running
 * with its anchor: {@link SteamGeneratorBlock#RUNNING} is set on every block of the Engine at once, since
 * the engine its front and back draw spans all four and turns as one.
 */
final class SteamEnginePartBlock extends DrawnPartBlock {

    private final MapCodec<SteamEnginePartBlock> codec;

    SteamEnginePartBlock(Properties properties, Supplier<Footprint> footprint) {
        super(properties, footprint);
        this.codec = simpleCodec(props -> new SteamEnginePartBlock(props, footprint));
        registerDefaultState(defaultBlockState().setValue(SteamGeneratorBlock.RUNNING, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return codec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(SteamGeneratorBlock.RUNNING);
    }
}

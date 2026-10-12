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
 * A part of the Boiler, drawn from its own model as every {@link DrawnPartBlock} is, and lit with its
 * anchor: {@link BoilerBlock#LIT} is set on every block of the Boiler at once, and each gives off the
 * same light. A block that gives off light is drawn at its own light level, so a Boiler lit at the
 * anchor alone would draw the anchor brighter than the parts round it.
 */
final class BoilerPartBlock extends DrawnPartBlock {

    private final MapCodec<BoilerPartBlock> codec;

    BoilerPartBlock(Properties properties, Supplier<Footprint> footprint) {
        super(properties.lightLevel(BoilerBlock::lightLevel), footprint);
        this.codec = simpleCodec(props -> new BoilerPartBlock(props, footprint));
        registerDefaultState(defaultBlockState().setValue(BoilerBlock.LIT, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return codec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BoilerBlock.LIT);
    }
}

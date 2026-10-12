// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import com.mojang.serialization.MapCodec;
import io.github._5thlayer.groundworks.Footprint;
import io.github._5thlayer.groundworks.FootprintPartBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

/**
 * The Reactor's part block, drawn block by block. Groundworks draws no part, leaving the origin's model
 * to draw the whole, but a block model reaches only a block past its own, and the Reactor stands three
 * tall over a bottom centre anchor. Until its model is drawn, each part wears its own vanilla stand-in.
 */
final class ReactorPartBlock extends FootprintPartBlock {

    private final MapCodec<ReactorPartBlock> codec;

    ReactorPartBlock(Properties properties, Supplier<Footprint> footprint) {
        super(properties, footprint);
        this.codec = simpleCodec(props -> new ReactorPartBlock(props, footprint));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return codec;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}

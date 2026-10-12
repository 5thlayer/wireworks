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
 * A part block drawn from its own model. Groundworks draws no part, leaving the origin's model to draw
 * the whole, but a block model reaches only a block past its own: the Reactor stands three tall over a
 * bottom centre anchor, and the Boiler's, the Steam Engine's and the Steam Turbine's anchors are
 * one-block models. Each part wears its own block's textures, picked by its part number and facing. The Boiler's parts are a
 * {@link BoilerPartBlock}, which also lights with its anchor, and the Steam Engine's a
 * {@link SteamEnginePartBlock}, which runs with it.
 */
class DrawnPartBlock extends FootprintPartBlock {

    private final MapCodec<DrawnPartBlock> codec;

    DrawnPartBlock(Properties properties, Supplier<Footprint> footprint) {
        super(properties, footprint);
        this.codec = simpleCodec(props -> new DrawnPartBlock(props, footprint));
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

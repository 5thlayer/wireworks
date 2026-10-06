// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Transformer (ADR 0008): the only block that wires to both Transmission Poles and Distribution
 * Poles, and the joint through which energy crosses between Districts. One block, not a Pole Column:
 * it has no Supply Area, no Supply Area Box and no GUI, and does not stack.
 *
 * <p>It shares the pole block's wiring, breaking and loot ({@link PoleBlock}), and reports into the
 * network tick like every pole does ({@link TransformerBlockEntity}).
 */
public class TransformerBlock extends PoleBlock implements EntityBlock {

    /** The registry path. */
    public static final String BLOCK_NAME = "transformer";

    public TransformerBlock(BlockBehaviour.Properties props) {
        super(props);
    }

    @Override
    public PoleKind kind() {
        return TransformerSpec.kind();
    }

    @Override
    public boolean stacks() {
        return false;
    }

    /** A whole cube, where a pole is a post. */
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TransformerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                 BlockEntityType<T> type) {
        if (level.isClientSide() || type != WireworksRegistries.TRANSFORMER_ENTITY.get()) {
            return null;
        }
        return (lvl, pos, st, be) -> {
            if (be instanceof TransformerBlockEntity transformer) {
                transformer.serverTick();
            }
        };
    }
}

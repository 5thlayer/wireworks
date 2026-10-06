// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Transmission Pole (ADR 0008): no tier and no Supply Area, so it powers nothing and carries
 * Wires far. Its own block rather than a {@link SupplyAreaPoleBlock}, since that one scans an area by
 * tier; it takes the Pole Column from {@link PoleBlock}.
 *
 * <p>It carries no energy. Its block entity lets the level find a pole through a chunk's block
 * entities, as it finds the others, draws the wires between poles, and reports the base into the
 * network tick so a change to the set of Transmission Poles rebuilds the Electric Networks.
 */
public class TransmissionPoleBlock extends PoleBlock implements EntityBlock {

    /** The registry path. */
    public static final String BLOCK_NAME = "transmission_pole";

    public TransmissionPoleBlock(BlockBehaviour.Properties props) {
        super(props);
    }

    @Override
    public PoleKind kind() {
        return PoleKind.TRANSMISSION;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TransmissionPoleBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                 BlockEntityType<T> type) {
        if (level.isClientSide() || type != WireworksRegistries.TRANSMISSION_POLE_ENTITY.get()) {
            return null;
        }
        // Only a column's base reports, as with the Distribution Poles.
        return (lvl, pos, st, be) -> {
            if (be instanceof TransmissionPoleBlockEntity pole && PoleColumn.isBase(lvl, pos)) {
                pole.serverTick();
            }
        };
    }
}

// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Transmission Pole (ADR 0008): no tier and no Supply Area, so it powers nothing and carries
 * Wires far. Its own block rather than a {@link SupplyAreaPoleBlock}, since that one scans an area by
 * tier; it takes the Pole Column from {@link PoleBlock}.
 *
 * <p>It carries no energy and ticks nothing. Its block entity exists only so the level can find a
 * pole through a chunk's block entities, as it finds the others, and so the wires between poles are
 * drawn.
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
}

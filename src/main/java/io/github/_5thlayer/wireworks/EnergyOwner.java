// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import io.github._5thlayer.groundworks.FootprintPartBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A block entity whose energy belongs to another block's: a machine's part answering for its
 * anchor. A pole follows the answer until it stops, so it counts and feeds each owner once however
 * many of its blocks the area reaches. {@link EnergyOwnerBlock} is the same for a block with no
 * block entity.
 *
 * <p>A Groundworks footprint needs neither: its parts forward every energy lookup to its origin, so
 * a part stands for the origin standing for it, whatever mod declared the footprint. That is how a
 * machine from a mod that never builds against Wireworks, such as Craftworks' Assembler, is counted
 * once. An orphan part, with no origin standing, answers no energy lookup, so a pole skips it.
 *
 * <p>A pole measures a machine's room with an insert it then aborts, so an energy face the network
 * reaches must journal its buffer through the transaction. One that doesn't keeps a probe's worth
 * of energy every tick.
 */
public interface EnergyOwner {

    /** The block whose energy this one's face stands for, or {@code null} if it is its own. */
    BlockPos wireworks$energyOwner();

    /**
     * A block entity's owner, or failing that a hull block's, or failing that a Groundworks part's
     * standing origin, or {@code null}.
     */
    static BlockPos of(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof EnergyOwner owned) {
            return owned.wireworks$energyOwner();
        }
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof EnergyOwnerBlock hull) {
            return hull.energyOwner(pos, state);
        }
        return state.getBlock() instanceof FootprintPartBlock part
                ? part.footprint().standingOrigin(level, pos, state) : null;
    }
}

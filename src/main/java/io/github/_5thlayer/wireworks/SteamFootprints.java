// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import io.github._5thlayer.groundworks.FootprintShape;
import io.github._5thlayer.groundworks.FootprintShape.Local;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/** Where the blocks of the Boiler, the Steam Engine, the Steam Turbine and the Reactor stand, in the anchor's frame. */
final class SteamFootprints {

    /**
     * Factorio's 3x2 boiler, one block tall. The anchor is the front-middle block; local {@code x}
     * runs backward from the front, so the second row is {@code x = 1}.
     */
    static final FootprintShape BOILER = FootprintShape.of(
            new Local(0, 0, -1), new Local(0, 0, 1),
            new Local(1, 0, -1), new Local(1, 0, 0), new Local(1, 0, 1));

    /** The Steam Engine is 2x1x2: the anchor and three parts. */
    static final FootprintShape STEAM_ENGINE = FootprintShape.of(
            new Local(0, 1, 0), new Local(0, 0, -1), new Local(0, 1, -1));

    /**
     * The Steam Turbine is 3x1x2: three wide across its row and two tall, the anchor the bottom middle.
     * Its row runs along local {@code x}, as the Engine's does.
     */
    static final FootprintShape STEAM_TURBINE = FootprintShape.of(
            new Local(0, 0, -1), new Local(0, 0, 1),
            new Local(0, 1, -1), new Local(0, 1, 0), new Local(0, 1, 1));

    /** The Reactor is 3x3x3, the anchor the bottom centre, so it stands centred on the block clicked. */
    static final FootprintShape REACTOR = FootprintShape.standing(3, 3);

    private SteamFootprints() {
    }

    /**
     * The face of a Steam Engine's or Steam Turbine's row end that its local {@code +x} looks out of;
     * its row runs along this axis.
     */
    static Direction rowSide(Direction facing) {
        return world(facing, 1, 0);
    }

    static int partOf(BlockState state) {
        return state.getValue(io.github._5thlayer.groundworks.FootprintPartBlock.PART);
    }

    /** The world direction a local {@code x, z} offset points along, with the anchor facing {@code facing}. */
    static Direction world(Direction facing, int x, int z) {
        var offset = new Local(x, 0, z).inWorld(facing);
        return Direction.getApproximateNearest(offset.getX(), offset.getY(), offset.getZ());
    }
}

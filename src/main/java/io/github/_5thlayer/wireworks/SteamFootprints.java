// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import io.github._5thlayer.groundworks.FootprintShape;
import io.github._5thlayer.groundworks.FootprintShape.Local;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** Where the blocks of the Boiler, the Steam Engine, the Steam Turbine and the Reactor stand, in the anchor's frame. */
final class SteamFootprints {

    /**
     * Factorio's 3x2 boiler, one block tall. The anchor is the front-middle block; local {@code x}
     * runs backward from the front, so the second row is {@code x = 1}.
     */
    static final FootprintShape BOILER = FootprintShape.of(
            new Local(0, 0, -1), new Local(0, 0, 1),
            new Local(1, 0, -1), new Local(1, 0, 0), new Local(1, 0, 1));

    /** The back middle block, the only steam port; its index is the part number and the position index. */
    static final int BOILER_STEAM_PART = BOILER.offsets().indexOf(new Local(1, 0, 0));

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

    /** The back middle block of the middle layer, the only superheated steam port. */
    static final int REACTOR_STEAM_PART = REACTOR.offsets().indexOf(new Local(1, 1, 0));

    private SteamFootprints() {
    }

    /** The Reactor port a block of the footprint is, from its part number: 0 is the anchor. */
    static @Nullable ReactorPort reactorPort(int part) {
        Local at = REACTOR.offsets().get(part);
        return ReactorPort.at(at.x(), at.y(), at.z());
    }

    /**
     * Whether the Reactor's port block of this part number opens onto this face: each water port
     * outward from its side, the steam port backward. Every other face opens onto nothing.
     */
    static boolean reactorPortOpens(int part, Direction facing, Direction face) {
        ReactorPort port = reactorPort(part);
        if (port == null) {
            return false;
        }
        Local at = REACTOR.offsets().get(part);
        return switch (port) {
            case WATER -> face == world(facing, at.x(), at.z());
            case STEAM -> face == reactorSteamSide(facing);
        };
    }

    /** The direction the Reactor's steam port faces, where a pipe or a Turbine row takes the steam from it. */
    static Direction reactorSteamSide(Direction facing) {
        return world(facing, 1, 0);
    }

    /** The Boiler port a block of the footprint is, from its part number: 0 is the anchor. */
    static @Nullable BoilerPort boilerPort(int part) {
        Local at = BOILER.offsets().get(part);
        return BoilerPort.at(at.x(), at.y(), at.z());
    }

    /**
     * Whether the port block of this part number opens onto this face: water outward along the front
     * row from each end, steam backwards. The faces between blocks of the footprint open onto nothing.
     */
    static boolean boilerPortOpens(int part, Direction facing, Direction face) {
        Local at = BOILER.offsets().get(part);
        BoilerPort port = BoilerPort.at(at.x(), at.y(), at.z());
        if (port == null) {
            return false;
        }
        return switch (port) {
            case WATER -> at.z() != 0 && face == world(facing, 0, Integer.signum(at.z()));
            case STEAM -> face == world(facing, 1, 0);
        };
    }

    /** The direction the steam port faces, where a pipe or an Engine takes the steam from it. */
    static Direction boilerSteamSide(Direction facing) {
        return world(facing, 1, 0);
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

    private static Direction world(Direction facing, int x, int z) {
        var offset = new Local(x, 0, z).inWorld(facing);
        return Direction.getApproximateNearest(offset.getX(), offset.getY(), offset.getZ());
    }
}

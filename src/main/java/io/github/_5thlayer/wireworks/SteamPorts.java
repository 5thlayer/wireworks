// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import java.util.List;

import io.github._5thlayer.groundworks.Footprint;
import io.github._5thlayer.groundworks.FootprintShape.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

import org.jspecify.annotations.Nullable;

/**
 * A steam machine's Ports (ADR-0011): the fluid faces its footprint answers with, and the pump that
 * draws into them and passes steam on. A pipe pushes nothing, so the machine pulls what it takes. It
 * never draws from another machine's Port, which pushes its own or gives none, so nothing trades back
 * and forth.
 *
 * <p>The owner calls {@link #pullIn()} as its tick begins and {@link #passOn()} as it ends.
 */
abstract sealed class SteamPorts permits SteamPorts.Fixed, SteamPorts.Row {

    private final BlockEntity owner;
    private final Footprint footprint;

    private SteamPorts(BlockEntity owner, Footprint footprint) {
        this.owner = owner;
        this.footprint = footprint;
    }

    /** The Boiler's or the Reactor's Ports, laid out by {@code layout}: water ports fill {@code water}, steam ports empty {@code steam}. */
    static Fixed fixed(BlockEntity owner, Footprint footprint, PortLayout layout, SingleFluidTank water, SingleFluidTank steam) {
        return new Fixed(owner, footprint, layout, water, steam);
    }

    /** A Steam Engine's or Steam Turbine's row, whose ends fill and empty {@code tank}. */
    static Row row(BlockEntity owner, Footprint footprint, SingleFluidTank tank) {
        return new Row(owner, footprint, tank);
    }

    /** Draws into the machine's tanks from whatever stands against its Ports. */
    abstract void pullIn();

    /** Offers the steam to whatever stands against the Port it leaves by. */
    abstract void passOn();

    final Direction facing() {
        return owner.getBlockState().getValue(Footprint.FACING);
    }

    final List<BlockPos> positions() {
        return footprint.positions(owner.getBlockPos(), facing());
    }

    final Level level() {
        return owner.getLevel();
    }

    /** What stands against {@code pos} on {@code side} for a pull to draw from: null where nothing answers, or another machine's Port. */
    final @Nullable ResourceHandler<FluidResource> source(BlockPos pos, Direction side) {
        ResourceHandler<FluidResource> from = neighbour(pos, side);
        return from instanceof FluidFace ? null : from;
    }

    final @Nullable ResourceHandler<FluidResource> neighbour(BlockPos pos, Direction side) {
        return level().getCapability(Capabilities.Fluid.BLOCK, pos.relative(side), side.getOpposite());
    }

    /** The Ports a {@link PortLayout} places: each opens on one face of its block onto one tank. */
    static final class Fixed extends SteamPorts {

        private final PortLayout layout;
        private final SingleFluidTank water;
        private final SingleFluidTank steam;
        private final ResourceHandler<FluidResource> waterFace;
        private final ResourceHandler<FluidResource> steamFace;
        private final List<Local> offsets;

        private Fixed(BlockEntity owner, Footprint footprint, PortLayout layout, SingleFluidTank water, SingleFluidTank steam) {
            super(owner, footprint);
            this.layout = layout;
            this.water = water;
            this.steam = steam;
            waterFace = new FluidFace(water, true, true);
            steamFace = new FluidFace(steam, false, true);
            offsets = footprint.shape().offsets();
        }

        /**
         * The fluid face of the footprint block numbered {@code part}, or null where no Port opens onto
         * {@code side}. The null side answers every Port's block, for a tool that names no face.
         */
        @Nullable ResourceHandler<FluidResource> face(int part, @Nullable Direction side) {
            PortLayout.Port port = portOf(part);
            if (port == null || side != null && side != opening(port)) {
                return null;
            }
            return port.tank() == PortLayout.Tank.WATER ? waterFace : steamFace;
        }

        @Override
        void pullIn() {
            List<BlockPos> positions = positions();
            for (int part = 0; part < positions.size() && water.room() > 0; part++) {
                PortLayout.Port port = portOf(part);
                if (port == null || port.tank() != PortLayout.Tank.WATER) {
                    continue;
                }
                ResourceHandler<FluidResource> from = source(positions.get(part), opening(port));
                if (from != null) {
                    ResourceHandlerUtil.move(from, water, resource -> resource.is(Fluids.WATER), water.room(), null);
                }
            }
        }

        @Override
        void passOn() {
            if (steam.amount() <= 0) {
                return;
            }
            List<BlockPos> positions = positions();
            for (int part = 0; part < positions.size(); part++) {
                PortLayout.Port port = portOf(part);
                if (port == null || port.tank() != PortLayout.Tank.STEAM) {
                    continue;
                }
                ResourceHandler<FluidResource> next = neighbour(positions.get(part), opening(port));
                if (next != null) {
                    ResourceHandlerUtil.move(steam, next, resource -> true, steam.amount(), null);
                }
            }
        }

        private PortLayout.@Nullable Port portOf(int part) {
            Local at = offsets.get(part);
            return layout.at(at.x(), at.y(), at.z());
        }

        private Direction opening(PortLayout.Port port) {
            return SteamFootprints.world(facing(), port.outX(), port.outZ());
        }
    }

    /**
     * A generator's row, with a Port at each end on every block of the footprint. Steam that comes in at
     * one end goes on out of the other ({@link SteamRow}); every other face only fills the tank.
     */
    static final class Row extends SteamPorts {

        private static final String PASS_ON = "PassOn";

        private final SingleFluidTank tank;
        private final SteamRow<Direction> row = new SteamRow<>();
        private final ResourceHandler<FluidResource> sideFace;
        private final ResourceHandler<FluidResource> enteredAtRowSide;
        private final ResourceHandler<FluidResource> enteredAtRowBack;

        private Row(BlockEntity owner, Footprint footprint, SingleFluidTank tank) {
            super(owner, footprint);
            this.tank = tank;
            sideFace = new FluidFace(tank, true, false);
            enteredAtRowSide = new FluidFace(tank, true, false, () -> row.passesOnAt(rowSide().getOpposite()));
            enteredAtRowBack = new FluidFace(tank, true, false, () -> row.passesOnAt(rowSide()));
        }

        /**
         * The fluid face on {@code side}. At either end of the row it remembers which, so the steam it
         * takes goes on out of the other; on every other side, and for no side, it only fills the tank.
         */
        ResourceHandler<FluidResource> face(@Nullable Direction side) {
            Direction rowSide = rowSide();
            if (side == rowSide) {
                return enteredAtRowSide;
            }
            return side == rowSide.getOpposite() ? enteredAtRowBack : sideFace;
        }

        /** Leaves alone the end the steam goes out of, so a pipe there is not drawn back from. */
        @Override
        void pullIn() {
            Direction rowSide = rowSide();
            Direction[] ends = {rowSide, rowSide.getOpposite()};
            SteamRow<Direction>.Pull pull = row.pull();
            for (BlockPos block : positions()) {
                for (Direction side : ends) {
                    if (tank.room() <= 0) {
                        return;
                    }
                    if (!pull.drawsAt(side)) {
                        continue;
                    }
                    ResourceHandler<FluidResource> from = source(block, side);
                    if (from != null) {
                        ResourceHandlerUtil.move(from, face(side), resource -> true, tank.room(), null);
                    }
                }
            }
        }

        /** Offers what the tank holds to whatever stands against the row end the steam did not come in at. */
        @Override
        void passOn() {
            Direction way = row.passOn();
            if (way == null || tank.amount() <= 0) {
                return;
            }
            for (BlockPos block : positions()) {
                ResourceHandler<FluidResource> next = neighbour(block, way);
                if (next != null) {
                    ResourceHandlerUtil.move(tank, next, resource -> true, tank.amount(), null);
                }
            }
        }

        void load(ValueInput input) {
            row.restore(Direction.byName(input.getStringOr(PASS_ON, "")));
        }

        void save(ValueOutput output) {
            Direction way = row.passOn();
            if (way != null) {
                output.putString(PASS_ON, way.getName());
            }
        }

        private Direction rowSide() {
            return SteamFootprints.rowSide(facing());
        }
    }
}

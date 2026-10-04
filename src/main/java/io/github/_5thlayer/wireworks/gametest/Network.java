// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks.gametest;

import io.github._5thlayer.groundworks.Footprint;
import io.github._5thlayer.wireworks.PoleTier;
import io.github._5thlayer.wireworks.WireworksRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.List;

/** What the network tests share: the poles they place and the consumer they read. */
final class Network {

    /** Past one rescan interval (40) plus the tick the network is rebuilt on. */
    static final int SETTLE = 45;

    private Network() {
    }

    static void creative(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, WireworksRegistries.CREATIVE_POLE.get());
    }

    static void small(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, WireworksRegistries.pole(PoleTier.SMALL).get());
    }

    static void consumer(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, TestConsumer.BLOCK.get());
    }

    static long stored(GameTestHelper helper, BlockPos consumer) {
        return helper.getBlockEntity(consumer, TestConsumer.ConsumerEntity.class).stored;
    }

    static void drain(GameTestHelper helper, BlockPos consumer) {
        helper.getBlockEntity(consumer, TestConsumer.ConsumerEntity.class).stored = 0;
    }

    /** Lays every block of a footprint at once, its anchor at {@code anchor}. */
    static List<BlockPos> footprint(GameTestHelper helper, Footprint footprint, BlockPos anchor, Direction facing) {
        List<BlockPos> positions = footprint.positions(helper.absolutePos(anchor), facing);
        for (int i = 0; i < positions.size(); i++) {
            helper.getLevel().setBlock(positions.get(i), footprint.stateAt(i, facing), Block.UPDATE_ALL);
        }
        return positions;
    }

    static EnergyHandler energy(GameTestHelper helper, BlockPos pos) {
        EnergyHandler handler = helper.getLevel().getCapability(Capabilities.Energy.BLOCK, helper.absolutePos(pos), null);
        if (handler == null) {
            helper.fail("nothing answers the energy capability", pos);
        }
        return handler;
    }

    /** Puts FE into a face the way a charger would, one committed insert at a time. Whether it all fit. */
    static boolean fill(EnergyHandler handler, long amount) {
        long left = amount;
        while (left > 0L) {
            int inserted;
            try (Transaction transaction = Transaction.open(null)) {
                inserted = handler.insert((int) Math.min(left, Integer.MAX_VALUE), transaction);
                transaction.commit();
            }
            if (inserted == 0) {
                return false;
            }
            left -= inserted;
        }
        return true;
    }
}

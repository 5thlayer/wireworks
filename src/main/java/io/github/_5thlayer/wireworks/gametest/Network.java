// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks.gametest;

import io.github._5thlayer.wireworks.PoleTier;
import io.github._5thlayer.wireworks.WireworksRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;

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
}

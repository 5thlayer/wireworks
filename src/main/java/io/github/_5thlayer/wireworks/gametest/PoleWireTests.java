// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks.gametest;

import io.github._5thlayer.wireworks.WireworksRegistries;
import io.github._5thlayer.wireworks.LevelWires;
import io.github._5thlayer.wireworks.PoleWiring;
import io.github._5thlayer.wireworks.PoleTier;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * A stored wire, not reach, joins two poles (#296, ADR-0068).
 *
 * <p>{@code PoleNetworksFromWiresTest}, {@code PoleWiringPlacementTest} and
 * {@code PoleWiringClickTest} hold the rules. What none of them can see is that the level keeps the
 * wires, that placing a pole writes them, and that the network is rebuilt from them when one is cut.
 *
 * <h2>The layout</h2>
 *
 * <p>A chain along x: a creative pole at 1 (its area reaches x 10), a small pole at 8 and another at
 * 15, each seven blocks from the last, and a consumer at 17, inside only the last pole's
 * area. The consumer can be fed only down the whole chain.
 */
final class PoleWireTests {

    private static final BlockPos A = new BlockPos(1, 1, 3);
    private static final BlockPos B = new BlockPos(8, 1, 3);
    private static final BlockPos C = new BlockPos(15, 1, 3);
    private static final BlockPos CONSUMER = new BlockPos(17, 1, 3);
    /** In reach of both A and B, which are no longer wired. */
    private static final BlockPos D = new BlockPos(4, 1, 5);

    /** Past one rescan interval (40) plus the tick the network is rebuilt on. */
    private static final int SETTLE = Network.SETTLE;

    private PoleWireTests() {
    }

    static void register(WireworksGameTests.Registrar tests) {
        tests.test("cutting_the_only_wire_splits_the_network", 200, PoleWireTests::cuttingSplits);
        tests.test("wiring_a_cut_pair_again_rejoins_the_network", 300, PoleWireTests::rewiringRejoins);
        tests.test("a_placed_pole_does_not_restore_a_cut_wire", 200, PoleWireTests::placingDoesNotRestore);
    }

    private static void cuttingSplits(GameTestHelper helper) {
        placeChain(helper);
        helper.startSequence()
                .thenIdle(SETTLE)
                .thenExecute(() -> {
                    if (stored(helper) <= 0L) {
                        helper.fail("a consumer at the end of a placed chain holds no FE", CONSUMER);
                    }
                    click(helper, A, B, PoleWiring.Click.CUT);
                    drain(helper);
                })
                .thenIdle(5)
                .thenExecute(() -> expectUnfed(helper, "after the only wire to the creative pole was cut"))
                .thenSucceed();
    }

    private static void rewiringRejoins(GameTestHelper helper) {
        placeChain(helper);
        helper.startSequence()
                .thenIdle(SETTLE)
                .thenExecute(() -> {
                    click(helper, A, B, PoleWiring.Click.CUT);
                    drain(helper);
                })
                .thenIdle(5)
                .thenExecute(() -> {
                    expectUnfed(helper, "after its wire was cut");
                    click(helper, B, A, PoleWiring.Click.WIRED);
                })
                .thenIdle(5)
                .thenExecute(() -> {
                    if (stored(helper) <= 0L) {
                        helper.fail("the consumer holds no FE after the cut wire was made again", CONSUMER);
                    }
                })
                .thenSucceed();
    }

    private static void placingDoesNotRestore(GameTestHelper helper) {
        placeChain(helper);
        helper.startSequence()
                .thenExecute(() -> click(helper, A, B, PoleWiring.Click.CUT))
                .thenExecute(() -> helper.setBlock(D, WireworksRegistries.pole(PoleTier.SMALL).get()))
                .thenExecute(() -> {
                    if (wires(helper).contains(helper.absolutePos(A), helper.absolutePos(B))) {
                        helper.fail("placing a pole in reach of a cut pair wired the pair again", B);
                    }
                })
                .thenSucceed();
    }

    private static void placeChain(GameTestHelper helper) {
        Network.creative(helper, A);
        Network.small(helper, B);
        Network.small(helper, C);
        Network.consumer(helper, CONSUMER);
    }

    private static void click(GameTestHelper helper, BlockPos anchor, BlockPos target, PoleWiring.Click expected) {
        PoleWiring.Click got = wires(helper).click(helper.getLevel(),
                helper.absolutePos(anchor), helper.absolutePos(target));
        if (got != expected) {
            helper.fail("clicking " + anchor + " then " + target + " was " + got + ", not " + expected, target);
        }
    }

    private static LevelWires wires(GameTestHelper helper) {
        return LevelWires.of(helper.getLevel());
    }

    private static void drain(GameTestHelper helper) {
        Network.drain(helper, CONSUMER);
    }

    private static void expectUnfed(GameTestHelper helper, String when) {
        long stored = stored(helper);
        if (stored != 0L) {
            helper.fail("the consumer took " + stored + " FE " + when, CONSUMER);
        }
    }

    private static long stored(GameTestHelper helper) {
        return Network.stored(helper, CONSUMER);
    }
}

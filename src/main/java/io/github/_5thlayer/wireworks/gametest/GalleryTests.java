// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import io.github._5thlayer.libworks.gallery.GalleryPack;
import io.github._5thlayer.libworks.testkit.GalleryCheck;
import io.github._5thlayer.libworks.testkit.GalleryCheck.Scene;
import io.github._5thlayer.wireworks.AccumulatorBlockEntity;
import io.github._5thlayer.wireworks.LevelWires;
import io.github._5thlayer.wireworks.PoleNetworks;
import io.github._5thlayer.wireworks.PoleTier;
import io.github._5thlayer.wireworks.SteamGeneratorBlock;
import io.github._5thlayer.wireworks.Wireworks;
import io.github._5thlayer.wireworks.WireworksRegistries;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.gametest.framework.StructureUtils;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The Gallery pack (libworks ADR 0010), through libworks' {@link GalleryCheck}: offered and off, its
 * build running every command without failing, and each scene's key block where it stands. The scenes
 * stand in rows of three, each row 16 blocks north of the last, which keeps them near enough to where
 * the gallery is run for their machines to tick; {@link GalleryCheck#builds} lays one long row, so the
 * test runs its parts a row at a time. Beyond the build, no wire joins two scenes, the Steam scene's
 * Engine runs and charges its Accumulators, the By Area scene's pole charges all four of its own, and
 * a scene's sign, clicked, builds it again. Each test turns the pack on, which reloads the server's
 * data, and off again, passing or failing, and each builds where the others do, so each runs in a
 * batch of its own.
 */
final class GalleryTests {

    private static final String PACK = GalleryPack.id(Wireworks.MOD_ID);

    /** How many scenes a row holds, and how far north each row is of the last, as {@code gallery/build} lays them. */
    private static final int ROW = 3;
    private static final int ROW_DEPTH = 16;

    /** Where the gallery is run from: the platform's south-west corner, its plots spilling north and east of it. */
    private static final BlockPos ORIGIN = new BlockPos(0, 1, 8);
    /** Where a scene's sign stands, from where the scene is run. */
    private static final BlockPos SIGN = new BlockPos(14, 0, 0);
    /** Where the Steam scene's Boiler, its Engine and its two Accumulators stand, from where the scene is run. */
    private static final BlockPos STEAM_BOILER = new BlockPos(13, 0, -4);
    private static final BlockPos STEAM_ENGINE = new BlockPos(13, 0, -6);
    private static final List<BlockPos> STEAM_ACCUMULATORS = List.of(new BlockPos(19, 0, -4), new BlockPos(19, 0, -8));
    /** Where the By Area scene's four Accumulators stand, each facing another way, from where the scene is run. */
    private static final List<BlockPos> BY_AREA_ACCUMULATORS = List.of(
            new BlockPos(9, 0, -6), new BlockPos(21, 0, -6), new BlockPos(16, 0, -3), new BlockPos(15, 0, -10));

    /** How many plots a test of one scene loads: a scene run alone, from where the gallery is run, fills only the first. */
    private static final int ONE_SCENE = 1;

    private GalleryTests() {
    }

    /** The scenes in the order {@code build} lays them, each with a key block and where, from where the scene is run, its function places it. */
    private static List<Scene> scenes() {
        var small = WireworksRegistries.pole(PoleTier.SMALL).get();
        return List.of(
                new Scene("tiers", new BlockPos(13, 0, -6), WireworksRegistries.pole(PoleTier.LARGE).get()),
                new Scene("by_area", new BlockPos(16, 0, -6), WireworksRegistries.CREATIVE_POLE.get()),
                new Scene("generation", new BlockPos(11, 0, -6), WireworksRegistries.SOLAR_PANEL.get()),
                new Scene("steam", STEAM_ENGINE, WireworksRegistries.STEAM_ENGINE.get()),
                new Scene("wiring", new BlockPos(16, 0, -6), small),
                new Scene("pole_line", new BlockPos(19, 2, -6), small),
                new Scene("jade", new BlockPos(14, 0, -6), small));
    }

    static void register(WireworksGameTests.Registrar tests) {
        tests.isolated("gallery_builds_a_scene_of_each_feature", GalleryCheck.TICKS, GalleryTests::builds);
        tests.isolated("gallery_steam_scene_runs_its_engine", GalleryCheck.TICKS, GalleryTests::steamRuns);
        tests.isolated("gallery_by_area_scene_charges_every_accumulator", GalleryCheck.TICKS, GalleryTests::byAreaCharges);
        tests.isolated("gallery_scenes_sign_clicked_builds_the_scene_again", GalleryCheck.TICKS, GalleryTests::signReplays);
    }

    private static void builds(GameTestHelper helper) {
        var scenes = scenes();
        var functions = helper.getLevel().getServer().getFunctions();
        withPack(helper, scenes.size(), run -> {
            GalleryCheck.assertLoads(helper, run.source(), Wireworks.MOD_ID);
            for (var scene : scenes) {
                helper.assertTrue(functions.get(GalleryPack.scene(Wireworks.MOD_ID, scene.name())).isPresent(),
                        "there is no scene function " + scene.name());
            }
            GalleryCheck.assertRuns(helper, run.source(), GalleryPack.build(Wireworks.MOD_ID));
            for (int row = 0; row * ROW < scenes.size(); row++) {
                GalleryCheck.assertKeyBlocks(helper, rowOrigin(helper, row),
                        scenes.subList(row * ROW, Math.min(scenes.size(), (row + 1) * ROW)));
            }
            assertNoWireJoinsTwoScenes(helper, run);
        });
    }

    /**
     * Every wire in the gallery joins two poles of one scene: a pole wires itself to any in reach, so
     * a scene's pole standing too near the next scene's would join their networks, and the Steam
     * scene's Engine would then charge nothing, the By Area scene's creative pole charging all.
     */
    private static void assertNoWireJoinsTwoScenes(GameTestHelper helper, Run run) {
        var wires = LevelWires.of(helper.getLevel()).wires();
        var plots = new ArrayList<BoundingBox>();
        for (int plot = 0; plot < run.plots(); plot++) {
            plots.add(GalleryCheck.plot(plotOrigin(helper, plot)));
        }
        run.area().intersectingChunks().forEach(chunk -> {
            for (var wire : wires.touching(chunk.x(), chunk.z())) {
                int from = plotOf(plots, wire.a());
                helper.assertTrue(from >= 0 && from == plotOf(plots, wire.b()),
                        "a wire joins " + wire.a() + " and " + wire.b() + ", in two scenes' plots");
            }
        });
    }

    /** The plot, numbered as {@code build} lays them, that {@code pos} stands in, or -1. */
    private static int plotOf(List<BoundingBox> plots, PoleNetworks.Pos pos) {
        for (int plot = 0; plot < plots.size(); plot++) {
            if (plots.get(plot).isInside(pos.x(), pos.y(), pos.z())) {
                return plot;
            }
        }
        return -1;
    }

    /** The Steam scene: its Boiler feeds its Engine, which runs and charges both Accumulators through its pole. */
    private static void steamRuns(GameTestHelper helper) {
        var engine = at(helper, STEAM_ENGINE);
        withPack(helper, ONE_SCENE, run -> GalleryCheck.assertRuns(helper, run.source(), GalleryPack.scene(Wireworks.MOD_ID, "steam")),
                () -> {
                    var state = helper.getLevel().getBlockState(engine);
                    helper.assertTrue(state.is(WireworksRegistries.STEAM_ENGINE.get()) && state.getValue(SteamGeneratorBlock.RUNNING),
                            "the Steam Engine is not running yet");
                    for (var accumulator : STEAM_ACCUMULATORS) {
                        helper.assertTrue(stored(helper, accumulator) > 0, "the Engine has not charged the Accumulator at " + accumulator.toShortString() + " yet");
                    }
                });
    }

    /** The By Area scene: its creative pole charges each Accumulator in its area, whichever way it faces, with no wire to any. */
    private static void byAreaCharges(GameTestHelper helper) {
        withPack(helper, ONE_SCENE, run -> GalleryCheck.assertRuns(helper, run.source(), GalleryPack.scene(Wireworks.MOD_ID, "by_area")),
                () -> {
                    for (var accumulator : BY_AREA_ACCUMULATORS) {
                        helper.assertTrue(stored(helper, accumulator) > 0, "the pole has not charged the Accumulator at " + accumulator.toShortString() + " yet");
                    }
                });
    }

    /**
     * A scene's sign, clicked, builds the scene again where it stands, leaving nothing loose: here the
     * Steam scene's Boiler, broken, which drops its water bucket and its coal.
     */
    private static void signReplays(GameTestHelper helper) {
        var boiler = at(helper, STEAM_BOILER);
        var sign = at(helper, SIGN);
        withPack(helper, ONE_SCENE, run -> {
            var level = helper.getLevel();
            GalleryCheck.assertRuns(helper, run.source(), GalleryPack.scene(Wireworks.MOD_ID, "steam"));
            level.destroyBlock(boiler, true);
            helper.assertTrue(level.getBlockEntity(sign) instanceof SignBlockEntity entity
                            && entity.executeClickCommandsIfPresent(level, run.source().getPlayer(), sign, true),
                    "the scene's sign runs no command when clicked");
            helper.assertTrue(level.getBlockState(boiler).is(WireworksRegistries.BOILER.get()),
                    "clicking the sign did not build the Boiler again where it stood");
            var plot = GalleryCheck.plot(helper.absolutePos(ORIGIN));
            var loose = level.getEntitiesOfClass(ItemEntity.class, AABB.of(plot));
            helper.assertTrue(loose.isEmpty(), "building the scene again left items loose: " + loose.stream().map(ItemEntity::getItem).toList());
            helper.assertTrue(level.getBlockState(sign).is(Blocks.OAK_SIGN), "building the scene again left no sign");
        });
    }

    private static long stored(GameTestHelper helper, BlockPos accumulator) {
        var entity = helper.getLevel().getBlockEntity(at(helper, accumulator));
        return entity instanceof AccumulatorBlockEntity found ? found.energy().getAmountAsLong() : 0L;
    }

    /** As {@link #withPack(GameTestHelper, int, Consumer, Runnable)}, waiting for nothing after {@code body}. */
    private static void withPack(GameTestHelper helper, int plots, Consumer<Run> body) {
        withPack(helper, plots, body, () -> {
        });
    }

    /**
     * Turns the pack on over the first {@code plots} plots, as {@code build} lays them, loaded; runs
     * {@code body} as an operator player standing where the gallery is run; waits until {@code until}
     * passes; then clears the plots and turns the pack off, passing or failing. A single scene run
     * from where the gallery is run stands on the first plot.
     */
    private static void withPack(GameTestHelper helper, int plots, Consumer<Run> body, Runnable until) {
        var server = helper.getLevel().getServer();
        GalleryCheck.assertOfferedAndOff(helper, PACK);
        var run = new Run(helper, plots, GalleryCheck.operator(helper, helper.absolutePos(ORIGIN)));
        var area = run.area();
        area.intersectingChunks().forEach(chunk -> helper.getLevel().setChunkForced(chunk.x(), chunk.z(), true));
        var on = GalleryCheck.select(server, PACK, true);
        helper.testInfo.addListener(new ClearAwayIfFailed(run, on));
        var off = new AtomicReference<CompletableFuture<Void>>();
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(on.isDone(), PACK + " is not on yet"))
                .thenWaitUntil(() -> helper.assertTrue(area.intersectingChunks().allMatch(chunk -> helper.getLevel().hasChunk(chunk.x(), chunk.z())),
                        "the gallery's plots are not loaded yet"))
                .thenExecute(() -> body.accept(run))
                .thenWaitUntil(until)
                .thenExecute(() -> off.set(run.clearAway()))
                .thenWaitUntil(() -> helper.assertTrue(off.get() != null && off.get().isDone(), PACK + " is not off yet"))
                .thenSucceed();
    }

    /** {@code offset}, from where the gallery is run, absolute. */
    private static BlockPos at(GameTestHelper helper, BlockPos offset) {
        return helper.absolutePos(ORIGIN).offset(offset);
    }

    /** Where row {@code row} of the gallery is run from, absolute. */
    private static BlockPos rowOrigin(GameTestHelper helper, int row) {
        return helper.absolutePos(ORIGIN).north(ROW_DEPTH * row);
    }

    /** Where plot {@code plot} of the gallery, numbered from 0 as {@code build} lays them, is run from, absolute. */
    private static BlockPos plotOrigin(GameTestHelper helper, int plot) {
        return rowOrigin(helper, plot / ROW).east(GalleryCheck.PLOT_WIDTH * (plot % ROW));
    }

    /** A test's run of the gallery: its first {@code plots} plots, built by {@code source}, standing where the gallery is run. */
    private record Run(GameTestHelper helper, int plots, CommandSourceStack source) {

        /** The plots, their floors included. */
        BoundingBox area() {
            int rows = Math.ceilDiv(plots, ROW);
            return BoundingBox.encapsulatingBoxes(List.of(
                    GalleryCheck.plot(helper.absolutePos(ORIGIN)),
                    GalleryCheck.strip(rowOrigin(helper, rows - 1), Math.min(plots, ROW)))).orElseThrow();
        }

        /** Air where the plots stood, their entities gone, and the pack off. The runner releases the forced chunks once the batch ends. */
        CompletableFuture<Void> clearAway() {
            // The pack's own clear first, which removes each pole as a player's world would, its wires
            // with it: the runner's clear sets blocks with no removal, which leaves the wires saved
            // for the next test's poles to meet.
            var commands = helper.getLevel().getServer().getCommands();
            for (int plot = 0; plot < plots; plot++) {
                var from = Vec3.atBottomCenterOf(plotOrigin(helper, plot));
                commands.performPrefixedCommand(source.withPosition(from), "function " + Wireworks.MOD_ID + ":gallery/clear");
            }
            StructureUtils.clearSpaceForStructure(area(), helper.getLevel());
            return GalleryCheck.select(helper.getLevel().getServer(), PACK, false);
        }
    }

    /** A failed test skips its own last steps, which would leave the pack on; it clears away once turning it on is done. */
    private record ClearAwayIfFailed(Run run, CompletableFuture<Void> on) implements GameTestListener {

        @Override
        public void testStructureLoaded(GameTestInfo testInfo) {
        }

        @Override
        public void testPassed(GameTestInfo testInfo, GameTestRunner runner) {
        }

        @Override
        public void testFailed(GameTestInfo testInfo, GameTestRunner runner) {
            var server = run.helper().getLevel().getServer();
            on.whenComplete((done, failed) -> server.execute(run::clearAway));
        }

        @Override
        public void testAddedForRerun(GameTestInfo original, GameTestInfo copy, GameTestRunner runner) {
        }
    }
}

// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks.gametest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.github._5thlayer.libworks.gallery.GalleryPack;
import io.github._5thlayer.libworks.testkit.GalleryCheck;
import io.github._5thlayer.libworks.testkit.GalleryCheck.Scene;
import io.github._5thlayer.wireworks.LevelWires;
import io.github._5thlayer.wireworks.PoleColumn;
import io.github._5thlayer.wireworks.PoleNetworks;
import io.github._5thlayer.wireworks.PoleTier;
import io.github._5thlayer.wireworks.Wireworks;
import io.github._5thlayer.wireworks.WireworksRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;

/**
 * The Gallery pack (libworks ADR 0010), through libworks' {@link GalleryCheck}: offered and off, its
 * build running every command without failing, and each scene's key block where it stands. The scenes
 * stand in rows of three, each row 16 blocks north of the last, as {@link #LAYOUT} lays them. Beyond
 * {@link GalleryCheck#builds}' checks, no wire joins two scenes, and a scene's sign, clicked, builds it
 * again. They check the pack alone, never that a scene's machines
 * work: each machine's own game tests check that. Each test turns the pack on, which reloads
 * the server's data, and off again, passing or failing, and each builds where the others do, so each
 * runs in a batch of its own.
 */
final class GalleryTests {

    /** How many scenes a row holds, and how far north each row is of the last, as {@code gallery/build} lays them. */
    private static final int ROW = 3;
    private static final int ROW_DEPTH = 16;
    private static final GalleryCheck.Layout LAYOUT = new GalleryCheck.Layout(ROW, ROW_DEPTH);

    /** Where the gallery is run from: the platform's south-west corner, its plots spilling north and east of it. */
    private static final BlockPos ORIGIN = new BlockPos(0, 1, 8);
    /** Where a scene's sign stands, from where the scene is run. */
    private static final BlockPos SIGN = new BlockPos(14, 0, 0);
    /** Where the Steam scene's Boiler stands, from where the scene is run. */
    private static final BlockPos STEAM_BOILER = new BlockPos(13, 0, -4);

    /** How many plots a test of one scene loads: a scene run alone, from where the gallery is run, fills only the first. */
    private static final int ONE_SCENE = 1;

    private GalleryTests() {
    }

    /** The scenes in the order {@code build} lays them, each with a key block and where, from where the scene is run, its function places it. */
    private static List<Scene> scenes() {
        var small = WireworksRegistries.pole(PoleTier.SMALL).get();
        return List.of(
                new Scene("tiers", new BlockPos(13, PoleColumn.MAX_SEGMENTS - 1, -6), WireworksRegistries.pole(PoleTier.LARGE).get()),
                new Scene("by_area", new BlockPos(16, 0, -6), WireworksRegistries.CREATIVE_POLE.get()),
                new Scene("generation", new BlockPos(11, 0, -6), WireworksRegistries.SOLAR_PANEL.get()),
                new Scene("steam", new BlockPos(13, 0, -6), WireworksRegistries.STEAM_ENGINE.get()),
                new Scene("reactor", new BlockPos(13, 0, -4), WireworksRegistries.REACTOR.get()),
                new Scene("wiring", new BlockPos(16, 0, -6), small),
                new Scene("transmission", new BlockPos(19, 0, -6), WireworksRegistries.TRANSFORMER.get()),
                new Scene("pole_line", new BlockPos(19, 2, -6), small),
                new Scene("jade", new BlockPos(14, 0, -6), small));
    }

    static void register(WireworksGameTests.Registrar tests) {
        tests.isolated("gallery_builds_a_scene_of_each_feature", GalleryCheck.TICKS, GalleryTests::builds);
        tests.isolated("gallery_scenes_sign_clicked_builds_the_scene_again", GalleryCheck.TICKS, GalleryTests::signReplays);
        tests.isolated("gallery_clear_empties_its_plot", GalleryCheck.TICKS,
                helper -> GalleryCheck.clears(helper, Wireworks.MOD_ID, ORIGIN, GalleryCheck.Layout.ONE_ROW));
    }

    private static void builds(GameTestHelper helper) {
        var scenes = scenes();
        var functions = helper.getLevel().getServer().getFunctions();
        int plots = scenes.size();
        GalleryCheck.withPack(helper, Wireworks.MOD_ID, ORIGIN, LAYOUT, plots, (source, steps) -> steps.thenExecute(() -> {
            GalleryCheck.assertLoads(helper, source, Wireworks.MOD_ID);
            for (var scene : scenes) {
                helper.assertTrue(functions.get(GalleryPack.scene(Wireworks.MOD_ID, scene.name())).isPresent(),
                        "there is no scene function " + scene.name());
            }
            var laidBefore = wiresTouching(helper, plots);
            GalleryCheck.assertRuns(helper, source, GalleryPack.build(Wireworks.MOD_ID));
            GalleryCheck.assertKeyBlocks(helper, helper.absolutePos(ORIGIN), LAYOUT, scenes);
            assertNoWireJoinsTwoScenes(helper, plots, laidBefore);
        })).thenSucceed();
    }

    /**
     * Every wire the build laid joins two poles of one scene: a pole wires itself to any in reach, so
     * a scene's pole standing too near the next scene's would join their networks into one, and no
     * scene would then show its feature alone. The wires {@code laidBefore} the build are left out:
     * the runner lays every batch's tests in one grid and clears none of them, and the plots' chunks
     * reach past the plots onto the test laid out just west of the gallery, its poles standing, wired.
     */
    private static void assertNoWireJoinsTwoScenes(GameTestHelper helper, int scenes, Set<PoleNetworks.Wire> laidBefore) {
        var plots = new ArrayList<BoundingBox>();
        for (int plot = 0; plot < scenes; plot++) {
            plots.add(LAYOUT.plot(LAYOUT.sceneOrigin(helper.absolutePos(ORIGIN), plot)));
        }
        for (var wire : wiresTouching(helper, scenes)) {
            if (laidBefore.contains(wire)) {
                continue;
            }
            int from = plotOf(plots, wire.a());
            helper.assertTrue(from >= 0 && from == plotOf(plots, wire.b()),
                    "a wire joins " + wire.a() + " and " + wire.b() + ", in two scenes' plots");
        }
    }

    /** Every wire touching the chunks of the gallery's first {@code plots} plots. */
    private static Set<PoleNetworks.Wire> wiresTouching(GameTestHelper helper, int plots) {
        var wires = LevelWires.of(helper.getLevel()).wires();
        var touching = new HashSet<PoleNetworks.Wire>();
        LAYOUT.area(helper.absolutePos(ORIGIN), plots).intersectingChunks().forEach(chunk -> touching.addAll(wires.touching(chunk.x(), chunk.z())));
        return touching;
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

    /**
     * A scene's sign, clicked, builds the scene again where it stands, leaving nothing loose: here the
     * Steam scene's Boiler, broken, which drops its water bucket and its coal.
     */
    private static void signReplays(GameTestHelper helper) {
        var boiler = at(helper, STEAM_BOILER);
        var sign = at(helper, SIGN);
        GalleryCheck.withPack(helper, Wireworks.MOD_ID, ORIGIN, LAYOUT, ONE_SCENE, (source, steps) -> steps.thenExecute(() -> {
            var level = helper.getLevel();
            GalleryCheck.assertRuns(helper, source, GalleryPack.scene(Wireworks.MOD_ID, "steam"));
            level.destroyBlock(boiler, true);
            helper.assertTrue(level.getBlockEntity(sign) instanceof SignBlockEntity entity
                            && entity.executeClickCommandsIfPresent(level, source.getPlayer(), sign, true),
                    "the scene's sign runs no command when clicked");
            helper.assertTrue(level.getBlockState(boiler).is(WireworksRegistries.BOILER.get()),
                    "clicking the sign did not build the Boiler again where it stood");
            var plot = LAYOUT.plot(helper.absolutePos(ORIGIN));
            var loose = level.getEntitiesOfClass(ItemEntity.class, AABB.of(plot));
            helper.assertTrue(loose.isEmpty(), "building the scene again left items loose: " + loose.stream().map(ItemEntity::getItem).toList());
            helper.assertTrue(level.getBlockState(sign).is(Blocks.OAK_SIGN), "building the scene again left no sign");
        })).thenSucceed();
    }

    /** {@code offset}, from where the gallery is run, absolute. */
    private static BlockPos at(GameTestHelper helper, BlockPos offset) {
        return helper.absolutePos(ORIGIN).offset(offset);
    }
}

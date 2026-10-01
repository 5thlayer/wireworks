// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks.gametest;

import io.github._5thlayer.wireworks.LevelWires;
import io.github._5thlayer.wireworks.WireworksRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Wiring by hand reaches the rules only through the right-click event and the player tick, so each
 * test clicks through the player's game mode and ticks through the event bus. The default
 * {@code wireworks:wire_tools} holds the copper ingot.
 */
final class WireGestureTests {

    /** Five blocks apart: in a small pole's reach, so placing them wires them. */
    private static final BlockPos A = new BlockPos(1, 1, 3);
    private static final BlockPos B = new BlockPos(6, 1, 3);

    private WireGestureTests() {
    }

    static void register(WireworksGameTests.Registrar tests) {
        tests.test("a_wire_tool_cuts_and_wires_again", 20, WireGestureTests::cutsAndWiresAgain);
        tests.test("an_item_outside_the_tag_holds_no_end", 20, WireGestureTests::otherItemHoldsNoEnd);
        tests.test("a_held_end_drops_when_the_tool_leaves_the_hand", 20, WireGestureTests::endDropsOutOfHand);
        tests.test("a_held_end_drops_when_the_tool_is_on_the_cursor", 20, WireGestureTests::endDropsOnTheCursor);
        tests.test("a_held_end_drops_when_the_tool_is_thrown", 20, WireGestureTests::endDropsWhenThrown);
        tests.test("a_player_who_may_not_build_cannot_wire", 20, WireGestureTests::adventureCannotWire);
    }

    private static void endDropsOnTheCursor(GameTestHelper helper) {
        ItemStack tool = new ItemStack(Items.COPPER_INGOT);
        ServerPlayer player = holding(helper, tool);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.containerMenu.setCarried(tool);
        NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(player));
        if (tool.has(WireworksRegistries.PENDING_WIRE.get())) {
            helper.fail("the end stayed held on the cursor", A);
        }
        helper.succeed();
    }

    private static void endDropsWhenThrown(GameTestHelper helper) {
        ItemStack tool = new ItemStack(Items.COPPER_INGOT);
        ServerPlayer player = holding(helper, tool);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        ItemEntity thrown = player.drop(tool, false);
        if (thrown == null) {
            helper.fail("the tool was not thrown", A);
            return;
        }
        if (thrown.getItem().has(WireworksRegistries.PENDING_WIRE.get())) {
            helper.fail("the thrown tool still holds an end", A);
        }
        helper.succeed();
    }

    private static void adventureCannotWire(GameTestHelper helper) {
        ServerPlayer player = poles(helper, new ItemStack(Items.COPPER_INGOT));
        player.setGameMode(GameType.ADVENTURE);
        click(helper, player, A);
        click(helper, player, B);
        expectWired(helper, true, "an adventure player clicking A then B");
        if (player.getMainHandItem().has(WireworksRegistries.PENDING_WIRE.get())) {
            helper.fail("an adventure player's tool held an end", A);
        }
        helper.succeed();
    }

    /** A player whose tool holds A's end. */
    private static ServerPlayer holding(GameTestHelper helper, ItemStack tool) {
        ServerPlayer player = poles(helper, tool);
        click(helper, player, A);
        if (!tool.has(WireworksRegistries.PENDING_WIRE.get())) {
            helper.fail("clicking a pole with the tool held no end", A);
        }
        return player;
    }

    private static void cutsAndWiresAgain(GameTestHelper helper) {
        ServerPlayer player = poles(helper, new ItemStack(Items.COPPER_INGOT));
        expectWired(helper, true, "placing two small poles five apart");
        click(helper, player, A);
        click(helper, player, B);
        expectWired(helper, false, "clicking A then B on a wired pair");
        click(helper, player, A);
        click(helper, player, B);
        expectWired(helper, true, "clicking A then B on a cut pair");
        if (player.getMainHandItem().has(WireworksRegistries.PENDING_WIRE.get())) {
            helper.fail("the tool still holds an end after the wire was made", B);
        }
        helper.succeed();
    }

    private static void otherItemHoldsNoEnd(GameTestHelper helper) {
        ServerPlayer player = poles(helper, new ItemStack(Items.STICK));
        click(helper, player, A);
        click(helper, player, B);
        if (player.getMainHandItem().has(WireworksRegistries.PENDING_WIRE.get())) {
            helper.fail("a stick held a wire end", A);
        }
        expectWired(helper, true, "clicking A then B with a stick");
        helper.succeed();
    }

    private static void endDropsOutOfHand(GameTestHelper helper) {
        ItemStack tool = new ItemStack(Items.COPPER_INGOT);
        ServerPlayer player = poles(helper, tool);
        click(helper, player, A);
        if (!tool.has(WireworksRegistries.PENDING_WIRE.get())) {
            helper.fail("clicking a pole with the tool held no end", A);
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.getInventory().setItem(9, tool);
        NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(player));
        if (tool.has(WireworksRegistries.PENDING_WIRE.get())) {
            helper.fail("the end stayed held after the tool left the main hand", A);
        }
        helper.succeed();
    }

    private static ServerPlayer poles(GameTestHelper helper, ItemStack held) {
        Network.small(helper, A);
        Network.small(helper, B);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos stand = helper.absolutePos(new BlockPos(3, 1, 1));
        player.snapTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5);
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        return player;
    }

    private static void click(GameTestHelper helper, ServerPlayer player, BlockPos pos) {
        BlockPos absolute = helper.absolutePos(pos);
        player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(absolute).relative(Direction.NORTH, 0.5), Direction.NORTH, absolute, false));
    }

    private static void expectWired(GameTestHelper helper, boolean wired, String after) {
        if (LevelWires.of(helper.getLevel()).contains(helper.absolutePos(A), helper.absolutePos(B)) != wired) {
            helper.fail("A and B are " + (wired ? "not " : "") + "wired after " + after, A);
        }
    }
}

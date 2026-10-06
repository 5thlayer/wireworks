// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;


import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * The pole in the hand, and the only thing that says what a pole does.
 *
 * <h2>Why a pole ships its own explanation</h2>
 *
 * <p>Every other way a player learns a machine is missing here. There is no cable to trace, so the
 * shape of the network cannot be read off the world. There is no GUI, so nothing can be inspected.
 * The recipe says nothing about reach.
 *
 * <p>The <b>Supply Area Square</b> shows where the area lands (ADR 0005), but only while a pole is held
 * or looked at, and it says nothing about the pole being wireless or about the area being measured
 * at the base. Those are this tooltip's alone, and the numbers here are what the square is read
 * against.
 *
 * <p>So it is stated here, in three lines, always shown rather than hidden behind Shift. This is
 * not detail a player goes looking for; it is the block's basic contract, and a tooltip nobody
 * opens teaches nobody.
 *
 * <p>The live half -- how many machines are actually in the area, and whether they are being fed --
 * cannot come from an item and belongs to the Jade line instead.
 */
public class SupplyAreaPoleItem extends PoleItem {

    public SupplyAreaPoleItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    private PoleTier tier() {
        return ((SupplyAreaPoleBlock) getBlock()).tier();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        PoleTier tier = tier();
        tooltip.accept(Component.translatable("tooltip.wireworks.pole.area",
                tier.supplySize(), tier.supplySize()).withStyle(ChatFormatting.GRAY));
        // A player not told it is wireless goes looking for the cable and concludes the pole is broken.
        tooltip.accept(Component.translatable("tooltip.wireworks.pole.wireless")
                .withStyle(ChatFormatting.GRAY));
        if (getBlock() instanceof CreativeSupplyAreaPoleBlock) {
            tooltip.accept(Component.translatable("tooltip.wireworks.pole.creative")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        if (!flag.hasShiftDown()) {
            tooltip.accept(Component.translatable("tooltip.wireworks.hold_shift")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            return;
        }
        tooltip.accept(Component.translatable("tooltip.wireworks.pole.band", tier.verticalRadius())
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.accept(Component.translatable("tooltip.wireworks.pole.column",
                Component.translatable(getBlock().getDescriptionId()), PoleColumn.MAX_SEGMENTS)
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}

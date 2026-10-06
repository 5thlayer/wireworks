// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;

/**
 * The Transformer in the hand. An ordinary block item: it is not a Pole Column, so aiming it at a pole
 * places it beside, and it says what it joins and how far its two wires reach.
 */
public class TransformerItem extends BlockItem {

    public TransformerItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.translatable("tooltip.wireworks.transformer.joins")
                .withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.wireworks.transformer.reach",
                (int) TransformerSpec.lineReach(), (int) TransformerSpec.districtReach())
                .withStyle(ChatFormatting.GRAY));
    }
}

// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;

/** The Transmission Pole in the hand: it says what the pole is for and how far it carries a wire. */
public class TransmissionPoleItem extends PoleItem {

    public TransmissionPoleItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.translatable("tooltip.wireworks.transmission_pole.reach",
                (int) TransmissionSpec.wireReach()).withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.wireworks.transmission_pole.powers_nothing")
                .withStyle(ChatFormatting.GRAY));
        if (!flag.hasShiftDown()) {
            tooltip.accept(Component.translatable("tooltip.wireworks.hold_shift")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            return;
        }
        tooltip.accept(Component.translatable("tooltip.wireworks.pole.column",
                Component.translatable(getBlock().getDescriptionId()), PoleColumn.MAX_SEGMENTS)
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}

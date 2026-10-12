// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import io.github._5thlayer.groundworks.FootprintItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/** Places the whole Reactor, or nothing, and says what it makes and how long a cell lasts. */
public class ReactorItem extends FootprintItem {

    public ReactorItem(Properties properties) {
        super(WireworksRegistries.REACTOR_FOOTPRINT, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.translatable("tooltip.wireworks.reactor.makes_steam", ReactorSpec.steamPerSecond())
                .withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.wireworks.reactor.cell_lasts", ReactorSpec.CELL_SECONDS)
                .withStyle(ChatFormatting.GRAY));
    }
}

// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import io.github._5thlayer.groundworks.Footprint;
import io.github._5thlayer.groundworks.FootprintItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/** Places a whole Steam Engine or Steam Turbine, or nothing, and says what it burns and what it makes. */
public class SteamGeneratorItem extends FootprintItem {

    private final SteamEngineSpec spec;
    private final String name;

    /** @param name the block's name, which keys its tooltip lines */
    public SteamGeneratorItem(Footprint footprint, SteamEngineSpec spec, String name, Properties properties) {
        super(footprint, properties);
        this.spec = spec;
        this.name = name;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.translatable("tooltip.wireworks." + name + ".burns_steam",
                BoilerItem.number(spec.steamPerSecond())).withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.wireworks." + name + ".makes_up_to",
                BoilerItem.number(spec.energyPerTick())).withStyle(ChatFormatting.GRAY));
    }
}

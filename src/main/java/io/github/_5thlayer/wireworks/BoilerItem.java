// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import io.github._5thlayer.groundworks.FootprintItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.function.Consumer;

/** Places the whole Boiler, or nothing, and says what it makes and what it burns. */
public class BoilerItem extends FootprintItem {

    public BoilerItem(Properties properties) {
        super(WireworksRegistries.BOILER_FOOTPRINT, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.translatable("tooltip.wireworks.boiler.makes_steam", BoilerSpec.steamPerSecond())
                .withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.wireworks.boiler.burns", BoilerSpec.JOULES_PER_TICK)
                .withStyle(ChatFormatting.GRAY));
    }

    /** A rate to at most two decimals, whatever the locale. */
    static String number(double value) {
        return new DecimalFormat("#,##0.##", DecimalFormatSymbols.getInstance(Locale.ROOT)).format(value);
    }
}

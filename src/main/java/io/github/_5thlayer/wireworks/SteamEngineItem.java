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

/** Places the whole Steam Engine, or nothing, and says what it burns and what it makes. */
public class SteamEngineItem extends FootprintItem {

    public SteamEngineItem(Properties properties) {
        super(WireworksRegistries.STEAM_ENGINE_FOOTPRINT, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        SteamEngineSpec spec = SteamEngineBlockEntity.spec();
        tooltip.accept(Component.translatable("tooltip.wireworks.steam_engine.burns_steam",
                BoilerItem.number(spec.steamPerSecond())).withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.wireworks.steam_engine.makes_up_to",
                BoilerItem.number(spec.energyPerTick())).withStyle(ChatFormatting.GRAY));
    }
}

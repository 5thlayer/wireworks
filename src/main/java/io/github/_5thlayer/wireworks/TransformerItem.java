// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import io.github._5thlayer.groundworks.PlacementPlan;
import io.github._5thlayer.groundworks.Placements;
import io.github._5thlayer.groundworks.PlansPlacement;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;

import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * The Transformer in the hand. A block item with an ordinary plan: it is not a Pole Column, so aiming it at a pole
 * places it beside, and it says what it joins and how far its two wires reach.
 */
public class TransformerItem extends BlockItem implements PlansPlacement {

    public TransformerItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    /**
     * The Transformer's plan is ordinary placement, one block, wherever the aim lands: it never
     * extends a column, so aiming it at a pole places it beside. Having a plan is what draws the
     * translucent block and the would-be wires.
     */
    @Override
    public @Nullable PlacementPlan plan(BlockPlaceContext context) {
        return Placements.vanillaPlan(this, context);
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

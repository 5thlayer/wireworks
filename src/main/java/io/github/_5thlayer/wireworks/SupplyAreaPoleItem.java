// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import io.github._5thlayer.groundworks.PlacementPlan;
import io.github._5thlayer.groundworks.Placements;
import io.github._5thlayer.groundworks.PlansPlacement;

import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * The pole in the hand, and the only thing that says what a pole does.
 *
 * <h2>Why a pole ships its own explanation</h2>
 *
 * <p>Every other way a player learns a machine is missing here. There is no cable to trace, so the
 * shape of the network cannot be read off the world. There is no GUI, so nothing can be inspected.
 * The recipe says nothing about reach. The area itself used to be invisible with it, which left a
 * block whose entire behaviour had to be taken on trust unless it was stated somewhere.
 *
 * <p>The <b>Supply Area Box</b> now shows where the area lands (factoryworks#158, FactoryWorks ADR-0070), so the area is no
 * longer invisible -- but it is shown only while a pole is held or looked at, and it says nothing
 * about wireless reach or about the area being measured at the base. Those are still this tooltip's
 * alone, and the numbers here are what the box is read against.
 *
 * <p>So it is stated here, in three lines, always shown rather than hidden behind Shift. This is
 * not detail a player goes looking for; it is the block's basic contract, and a tooltip nobody
 * opens teaches nobody.
 *
 * <p>The live half -- how many machines are actually in the area, and whether they are being fed --
 * cannot come from an item and belongs to the Jade line instead.
 */
public class SupplyAreaPoleItem extends BlockItem implements PlansPlacement {

    public SupplyAreaPoleItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    private PoleTier tier() {
        return ((SupplyAreaPoleBlock) getBlock()).tier();
    }

    /**
     * The pole's plan (factoryworks#297, FactoryWorks ADR-0069): ordinary placement, except where the aim lands on a pole,
     * where the column rule takes over.
     *
     * <p><b>The extension shows the real result</b>, not the spot vanilla would have chosen. A pole
     * aimed at any segment of a same-tier column previews the segment that would land on
     * <em>top</em>, because that is what {@link SupplyAreaPoleBlock#useItemOn} does -- clicking the
     * base raises a pole past the player's own reach, and a preview drawn beside the base would
     * describe a placement that never happens.
     *
     * <p>A column's refusals draw at the position the segment was headed for, which is the only
     * place a refusal about a column means anything.
     *
     * <p>A sneak places beside, because a sneaking player's click never reaches the block's
     * {@code useItemOn}.
     */
    @Override
    public @Nullable PlacementPlan plan(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos aimed = Placements.aimedPos(context);
        BlockState aimedState = level.getBlockState(aimed);
        if (!(aimedState.getBlock() instanceof SupplyAreaPoleBlock) || context.isSecondaryUseActive()) {
            return Placements.vanillaPlan(this, context);
        }
        if (!aimedState.is(getBlock())) {
            // Refused rather than placed beside, which would look exactly like the extension the
            // player asked for; drawn at the segment the player was plainly asking for.
            BlockPos top = PoleColumn.topOf(level, aimed);
            BlockPos at = top == null ? aimed.above() : top.above();
            return PlacementPlan.refused(at, getBlock().defaultBlockState(),
                    WireworksRefusal.OTHER_TIER);
        }
        BlockPos top = PoleColumn.topOf(level, aimed);
        if (top == null) {
            return Placements.vanillaPlan(this, context);
        }
        BlockPos next = top.above();
        BlockState segment = getBlock().defaultBlockState();
        if (PoleColumn.height(level, aimed) >= PoleColumn.MAX_SEGMENTS) {
            return PlacementPlan.refused(next, segment, WireworksRefusal.COLUMN_FULL);
        }
        if (!level.getBlockState(next).canBeReplaced()) {
            return PlacementPlan.refused(next, segment, WireworksRefusal.BLOCKED_TOP);
        }
        return PlacementPlan.accepted(next, segment);
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
        if (!Minecraft.getInstance().hasShiftDown()) {
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

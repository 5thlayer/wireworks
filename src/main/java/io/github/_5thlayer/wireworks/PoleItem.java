// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import io.github._5thlayer.groundworks.PlacementPlan;
import io.github._5thlayer.groundworks.Placements;
import io.github._5thlayer.groundworks.PlansPlacement;


/**
 * A pole in the hand: ordinary placement, except where the aim lands on a pole, where the column rule
 * takes over. What a pole says about itself is its own item's tooltip.
 */
public class PoleItem extends BlockItem implements PlansPlacement {

    public PoleItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    /**
     * The pole's plan (Groundworks ADR 0001 and 0002): ordinary placement, except where the aim lands
     * on a pole, where the column rule takes over.
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
        if (!(aimedState.getBlock() instanceof PoleBlock aimedPole && aimedPole.stacks())
                || context.isSecondaryUseActive()) {
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
}

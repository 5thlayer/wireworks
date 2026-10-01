// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import java.util.ArrayList;
import java.util.List;

import io.github._5thlayer.groundworks.PlacementPlan;
import io.github._5thlayer.groundworks.Refusal;
import io.github._5thlayer.groundworks.ReplaceBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Groundworks' builder for a pole's Fast Replace: the whole aimed column swapped to the held tier,
 * base first, for one pole charged and one handed back. Wireworks states no Replace group; a
 * Consumer passes this to {@code FastReplace.group} with the tiers it groups (ADR 0006).
 */
public final class PoleColumnReplace implements ReplaceBuilder {

    public static final PoleColumnReplace BUILDER = new PoleColumnReplace();

    private PoleColumnReplace() {
    }

    /** Null for the same tier, whose click extends the column instead. */
    @Override
    public @Nullable PlacementPlan plan(Level level, @Nullable Player player, ItemStack held, BlockPos aimed,
                                        BlockState old) {
        if (!(held.getItem() instanceof BlockItem item) || !(item.getBlock() instanceof SupplyAreaPoleBlock)
                || !(old.getBlock() instanceof SupplyAreaPoleBlock) || old.is(item.getBlock())) {
            return null;
        }
        BlockPos base = PoleColumn.baseOf(level, aimed);
        if (base == null) {
            return null;
        }
        int height = PoleColumn.height(level, aimed);
        BlockState segment = item.getBlock().defaultBlockState();
        List<PlacementPlan.Placed> blocks = new ArrayList<>(height);
        for (int i = 0; i < height; i++) {
            blocks.add(new PlacementPlan.Placed(base.above(i), segment));
        }
        return PlacementPlan.replacing(blocks, null);
    }

    @Override
    public Component message(Refusal refusal) {
        return Component.translatable("message.wireworks.replace_refused");
    }
}

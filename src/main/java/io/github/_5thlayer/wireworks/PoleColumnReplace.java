// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import java.util.ArrayList;
import java.util.List;

import io.github._5thlayer.groundworks.FastReplace;
import io.github._5thlayer.groundworks.PlacementPlan;
import io.github._5thlayer.groundworks.Refusal;
import io.github._5thlayer.groundworks.ReplaceBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.jspecify.annotations.Nullable;

/**
 * Groundworks' builder for a pole's Fast Replace: the whole aimed column swapped to the held tier,
 * base first, for one pole charged and one handed back.
 *
 * <p>Wireworks states a default Replace group, the three tiers without the creative pole, once
 * every mod is constructed. A Consumer that passes this builder to {@code FastReplace.group} at
 * its own construction states its group first, and a block belongs to the first group that claims
 * it, so the Consumer's group replaces the default for each tier it holds (ADR 0006).
 */
public final class PoleColumnReplace implements ReplaceBuilder {

    public static final PoleColumnReplace BUILDER = new PoleColumnReplace();

    /** The default group's id. */
    public static final Identifier DEFAULT_GROUP = Identifier.fromNamespaceAndPath(Wireworks.MOD_ID, "poles");

    private PoleColumnReplace() {
    }

    /** States the default group at common setup, after every mod's construction. */
    static void register(IEventBus modBus) {
        modBus.addListener(FMLCommonSetupEvent.class, event -> FastReplace.group(DEFAULT_GROUP,
                block -> block instanceof SupplyAreaPoleBlock && !(block instanceof CreativeSupplyAreaPoleBlock),
                BUILDER));
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

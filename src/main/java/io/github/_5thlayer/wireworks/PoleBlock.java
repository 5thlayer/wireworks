// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import io.github._5thlayer.groundworks.PlacementPlan;
import io.github._5thlayer.groundworks.Placements;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/**
 * What every pole block shares (ADR 0002, ADR 0008): the column. A pole stands as tall as the player
 * builds it, up to {@link PoleColumn#MAX_SEGMENTS}, and {@link PoleColumn} has the why. This class
 * owns the two world-facing consequences, extending a column and breaking one, and says what kind of
 * pole it is ({@link #kind}); a Supply Area Pole and a Transmission Pole differ in nothing else a
 * column cares about.
 *
 * <p>A column is segments of one block, so "the same kind below" is "the same block below": each tier
 * is its own block, and so is the Transmission Pole.
 */
public abstract class PoleBlock extends Block {

    /** A pole is a post, not a cube -- 6/16 square and full height. */
    private static final VoxelShape SHAPE = Block.box(5.0D, 0.0D, 5.0D, 11.0D, 16.0D, 11.0D);

    private static final String OTHER_TIER_KEY = "message.wireworks.other_tier";

    protected PoleBlock(BlockBehaviour.Properties props) {
        super(props
                .strength(1.5F)
                .sound(SoundType.COPPER)
                // No tool requirement: no mining-tool tag names the pole,
                // and requiring one here would make a pole break to nothing by hand.
                .noOcclusion());
    }

    /** What this pole is to the topology: its tier's Distribution Pole, or the Transmission Pole. */
    public abstract PoleKind kind();

    /**
     * Whether this pole is a Pole Column: segments of the same block stack into one pole. The
     * Transformer is one block and does not, so a second one on top is a second Transformer.
     */
    public boolean stacks() {
        return true;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level,
                                  BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /**
     * Right-clicking any segment with this tier's own pole grows the column by one.
     *
     * <p>Clicking <em>any</em> segment rather than only the top is the point: the new block lands on
     * top of the column, so a player standing on the ground raises a pole past their own reach by
     * clicking the base repeatedly. That matters because {@link PoleColumn#MAX_SEGMENTS} is chosen
     * so the top stays reachable -- if extending also demanded reaching the top, the cap would have
     * had to be smaller still.
     *
     * <p><b>The pole in hand is not consumed.</b> A column is one pole however tall it is: its height
     * is a wiring decision, not a cost (ADR 0002). Breaking the column pays back the one item it cost,
     * since a segment dropped by the cascade carries no loot.
     *
     * <p>A pole of another tier does nothing at all, and says why, rather than falling through to
     * ordinary placement. Falling through would set a second, separate pole against the side of this
     * column, with its own supply area and its own block entity, and it would look exactly like the
     * extension the player was asking for.
     */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                              BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        if (!stacks() || !(stack.getItem() instanceof PoleItem item)) {
            // Includes the empty hand and every other item, which must fall through to their own
            // placement.
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }

        // The column rule is asked for, not restated (Groundworks ADR 0001 and 0002): this executes
        // the same plan the preview draws, so the two cannot disagree about where a segment lands or
        // whether one may.
        PlacementPlan plan = Placements.planFor(item, new BlockPlaceContext(level, player, hand, stack, hit));
        if (plan != null && plan.isRefused() && player instanceof ServerPlayer server) {
            String reason = plan.refusal() == WireworksRefusal.OTHER_TIER ? OTHER_TIER_KEY : null;
            if (reason != null) {
                server.sendSystemMessage(Component.translatable(reason,
                        Component.translatable(item.getBlock().getDescriptionId()),
                        Component.translatable(getDescriptionId())), true);
            }
        }
        if (plan == null || plan.isRefused()) {
            return InteractionResult.CONSUME;
        }
        if (plan.blocks().size() != 1) {
            return InteractionResult.CONSUME;
        }
        PlacementPlan.Placed segment = plan.blocks().getFirst();

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        level.setBlockAndUpdate(segment.pos(), segment.state());
        level.playSound(null, segment.pos(), getSoundType(state, level, segment.pos(), player).getPlaceSound(),
                SoundSource.BLOCKS, 1.0F, 1.0F);
        return InteractionResult.SUCCESS;
    }


    /** A placed base wires itself (ADR 0004). */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState,
                           boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        // A Fast Replace swaps one pole for another in place: the column keeps its wires (ADR 0006).
        if (level instanceof ServerLevel server && !(oldState.getBlock() instanceof PoleBlock)) {
            LevelWires.of(server).placed(server, pos);
        }
    }

    /**
     * Only a base drops a pole: a column is one item however tall (ADR 0002).
     *
     * <p>Without this, extending for free and breaking the top segment back off would be a pole
     * duplicator, and it would look like ordinary play rather than an exploit. The loot table is the
     * same one either way; what changes is whether it is asked at all.
     */
    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockPos pos = BlockPos.containing(params.getOptionalParameter(LootContextParams.ORIGIN));
        if (stacks() && params.getLevel().getBlockState(pos.below()).is(this)) {
            return List.of();
        }
        return super.getDrops(state, params);
    }

    /**
     * Breaking any segment drops the column above it.
     *
     * <p>Chains and scaffolding both do this, so the muscle memory is already there, and a column is
     * one object to the player. Recursion is via {@code destroyBlock}, which
     * re-enters here for the block above, so the column unwinds one segment at a time.
     */
    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos,
                                               boolean movedByPiston) {
        // 26.1 calls this whenever the block changes, after setting the new one: a pole there is a
        // Fast Replace, which keeps the column and its wires, not a break.
        if (level.getBlockState(pos).getBlock() instanceof PoleBlock) {
            return;
        }
        // A base's wires go with it; an extension holds none (ADR 0004).
        if (!stacks() || !level.getBlockState(pos.below()).is(this)) {
            LevelWires.of(level).broken(level, pos);
        }
        BlockPos above = pos.above();
        if (stacks() && level.getBlockState(above).is(this)) {
            // Dropped without loot: extending a column costs nothing, so a segment must pay nothing
            // back, or a tall pole broken is a pole duplicator. The block the player actually broke
            // pays out through its own loot table, which is the single item the column cost.
            level.destroyBlock(above, false);
        }
    }
}

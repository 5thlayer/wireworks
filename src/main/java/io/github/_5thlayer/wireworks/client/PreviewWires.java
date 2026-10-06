// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks.client;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github._5thlayer.wireworks.ClientWires;
import io.github._5thlayer.wireworks.PoleColumn;
import io.github._5thlayer.wireworks.PoleKind;
import io.github._5thlayer.wireworks.PoleNetworks;
import io.github._5thlayer.wireworks.PoleTier;
import io.github._5thlayer.wireworks.PoleWiring;
import io.github._5thlayer.wireworks.SupplyAreaPoleBlock;
import io.github._5thlayer.wireworks.client.WireGeometry;
import io.github._5thlayer.groundworks.PlacementPlan;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

/**
 * The wires a held pole would add, drawn with its Placement Preview (ADR 0004, ADR 0005).
 *
 * <p>The selection is the server's own {@link PoleWiring#wouldAdd} -- no triangles, at most five,
 * nearest first -- asked of a hypothetical pole at the previewed spot, so the preview cannot promise
 * a wire the placement will not make. It is answered entirely on the client: the poles are in the
 * client's block entities and the wires in {@link ClientWires}, so there is no packet and no round
 * trip.
 *
 * <p>Faded rather than tinted: green, orange and red belong to the hand gesture's slack (ADR 0004)
 * and would read here as the outcome of a click that is not being made.
 */
public final class PreviewWires {

    /** The wire's own brown, faded, so a promised wire is visibly not a standing one. */
    private static final float FADE = 0.5F;

    private PreviewWires() {
    }

    /**
     * Draws the wires the plan's pole would add, or nothing: a refused plan puts no pole down, a
     * placement that only grows a column adds no wire, and a replaced column keeps its own (ADR 0006).
     */
    public static void draw(SubmitNodeCollector collector, PoseStack poseStack, ClientLevel level,
            Vec3 camera, PlacementPlan plan) {
        if (plan.isRefused() || plan.isReplace()) {
            return;
        }
        for (PlacementPlan.Placed placed : plan.blocks()) {
            if (placed.state().getBlock() instanceof SupplyAreaPoleBlock pole) {
                draw(collector, poseStack, level, camera, placed.pos(), pole);
                return;
            }
        }
    }

    private static void draw(SubmitNodeCollector collector, PoseStack poseStack, ClientLevel level,
            Vec3 camera, BlockPos pos, SupplyAreaPoleBlock block) {
        // A placement touching a standing column of the same pole extends or joins it, whichever end
        // it lands on, and a column that merely grew is not a new pole.
        boolean joinsAColumn = level.getBlockState(pos.below()).is(block)
                || level.getBlockState(pos.above()).is(block);
        PoleNetworks.Pole would = new PoleNetworks.Pole(pos.getX(), pos.getY(), pos.getZ(), PoleKind.distribution(block.tier()));
        List<PoleNetworks.Pole> targets = PoleWiring.wouldAdd(would, standingNear(level, pos, block.tier()),
                ClientWires.wires(), joinsAColumn);
        if (targets.isEmpty()) {
            return;
        }
        // The pole is not placed yet, so its column is the one block the preview draws.
        Vec3 start = new Vec3(pos.getX() + 0.5, pos.getY() + WireGeometry.ATTACH_HEIGHT, pos.getZ() + 0.5);
        for (PoleNetworks.Pole target : targets) {
            BlockPos base = new BlockPos(target.x(), target.y(), target.z());
            EntityRenderState.LeashState wire = new EntityRenderState.LeashState();
            wire.start = start;
            wire.end = WireGeometry.attachPoint(level, base);
            wire.offset = start.subtract(camera);
            BlockPos startAt = BlockPos.containing(start);
            BlockPos endAt = BlockPos.containing(wire.end);
            wire.startBlockLight = level.getBrightness(LightLayer.BLOCK, startAt);
            wire.endBlockLight = level.getBrightness(LightLayer.BLOCK, endAt);
            wire.startSkyLight = level.getBrightness(LightLayer.SKY, startAt);
            wire.endSkyLight = level.getBrightness(LightLayer.SKY, endAt);
            collector.submitCustomGeometry(poseStack, RenderTypes.leash(),
                    (pose, buffer) -> WireGeometry.draw(pose.pose(), buffer, wire,
                            0.5F * FADE, 0.4F * FADE, 0.3F * FADE, 1.0F));
        }
    }

    /**
     * Every pole base within the held pole's reach, out of the client's own loaded chunks. A wire's
     * reach is the shorter of its two ends', so the held pole's own reach bounds the search, exactly
     * as the server's does.
     */
    private static List<PoleNetworks.Pole> standingNear(ClientLevel level, BlockPos pos, PoleTier tier) {
        int reach = (int) Math.ceil(tier.wireReach());
        List<PoleNetworks.Pole> found = new ArrayList<>();
        for (int cx = SectionPos.blockToSectionCoord(pos.getX() - reach);
             cx <= SectionPos.blockToSectionCoord(pos.getX() + reach); cx++) {
            for (int cz = SectionPos.blockToSectionCoord(pos.getZ() - reach);
                 cz <= SectionPos.blockToSectionCoord(pos.getZ() + reach); cz++) {
                LevelChunk chunk = level.getChunkSource().getChunk(cx, cz, false);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    BlockPos at = be.getBlockPos();
                    if (level.getBlockState(at).getBlock() instanceof SupplyAreaPoleBlock other
                            && !at.equals(pos) && PoleColumn.isBase(level, at)) {
                        found.add(new PoleNetworks.Pole(at.getX(), at.getY(), at.getZ(), PoleKind.distribution(other.tier())));
                    }
                }
            }
        }
        return found;
    }
}

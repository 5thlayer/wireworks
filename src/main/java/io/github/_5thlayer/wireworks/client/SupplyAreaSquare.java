// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks.client;

import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import io.github._5thlayer.wireworks.PoleTier;
import io.github._5thlayer.wireworks.SupplyArea;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

import org.joml.Vector3f;

/**
 * The Supply Area Square (ADR 0005, amended by ADR 0009): a pole's supply area drawn as the outline
 * of the square it covers, on the plane the pole's base stands on. It is the third thing the
 * Placement Preview shows after the translucent block (Groundworks ADR 0001 and 0002) and the wires
 * the pole would add ({@link PreviewWires}).
 *
 * <p>The area is a volume -- the tier's square by two blocks up and down from the base -- but only
 * its footprint is drawn. ADR 0005 drew the whole box, and in play it read as a cage around a player
 * who almost always stands inside it. The band is no longer drawn at all: the machine outlines show
 * which machines in it are reached, and the item tooltip states how far it goes.
 *
 * <h2>One builder, two call sites</h2>
 *
 * <p>{@link #offsets} is the whole geometry, and it is a function of the tier alone -- nothing here
 * reads the world. The held item's preview and the placed pole's renderer both draw what it
 * returns, so they cannot disagree about where the area is, which is the placement plan's rule
 * applied to the drawing. The x and z extent come from {@link SupplyArea#bounds}: the large pole's
 * even-sided area takes its extra block on the negative side, and a renderer that centred the square
 * instead would be wrong by half a block on the one tier where it shows.
 *
 * <h2>The machines are outlined too</h2>
 *
 * <p>The square says where the footprint lands, and a player reading it wants to know which machines
 * are inside -- a question no flat shape can answer, because membership depends on the band and the
 * square has no height. So every block the pole reaches is outlined in the same yellow
 * ({@link SuppliedMachines}), and those outlines are what carry the band: a machine two blocks above
 * or below the base is outlined where it stands. They are the scan's own answer, not a capability
 * sweep, so they agree with the network by construction rather than by coincidence.
 *
 * <h2>Edges only, and drawn through terrain</h2>
 *
 * <p>No fill. A fill drawn through terrain washes over every machine standing in the square -- the
 * very machines being positioned -- and a large pole's 18-wide area would be a sheet over the whole
 * base. Depth-tested instead, it would z-fight the ground on flat terrain, sink into a rising slope
 * and float over a falling one (ADR 0009). Vanilla draws the structure block's bounding box as edges
 * only at the same scale, for the same reason.
 *
 * <p>The edges ignore depth. The square sits on the plane the base stands on, which on flat ground is
 * exactly the ground's top face, so a depth-tested line would z-fight it; and a slope rising into
 * the square would hide the part of it behind the hill, which is the ground extent the square exists
 * to show. No stock line type passes depth unconditionally, so {@link #PIPELINE} is vanilla's own
 * {@code LINES_SNIPPET} with {@link CompareOp#ALWAYS_PASS} and no depth write. It costs nothing
 * extra: the same line segments and the same arithmetic, with the test off.
 *
 * <h2>Why the square's edges are not a shape</h2>
 *
 * <p>The machines' outlines come off a {@link VoxelShape}, but the square cannot: {@link Shapes#create}
 * returns the empty shape for a box whose extent on any axis is under {@link Shapes#EPSILON}, and
 * the square has no height. A flat box would silently draw nothing, so its four edges are emitted
 * directly.
 */
public final class SupplyAreaSquare {

    /**
     * Bright yellow, Factorio's own electric-network colour, and the one tint no other pole gesture
     * uses: green, orange, red and brown are a wire tool's outcomes (ADR 0004) and
     * white and red are the placement preview's accepted and refused.
     *
     * <p><b>One state, never changing.</b> It does not turn red on a refused placement -- the red
     * block already says that -- and it does not react to network load. The square describes an area,
     * not an outcome; whether machines in it are being fed is the Jade line's answer, on the machine.
     */
    private static final int COLOUR = 0xFFFFE04C;

    /** Vanilla's own line width for a world-space outline. */
    private static final float WIDTH = 2.0F;

    /** What a machine whose shape is empty is outlined as. */
    private static final AABB FULL_BLOCK = new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);

    /**
     * Vanilla's line pipeline with the depth test passed unconditionally and no depth write.
     *
     * <p>Registered through NeoForge's {@link RegisterRenderPipelinesEvent} rather than built inline:
     * a pipeline has to be known before it is first used, and an unregistered one is a crash on the
     * frame the square is first drawn rather than at startup.
     */
    private static final RenderPipeline PIPELINE = RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
            .withLocation("pipeline/wireworks_supply_area_square")
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .build();

    private static final RenderType RENDER_TYPE = RenderType.create(
            "wireworks_supply_area_square",
            RenderSetup.builder(PIPELINE).createRenderSetup());

    private SupplyAreaSquare() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(SupplyAreaSquare::onRegisterPipelines);
    }

    private static void onRegisterPipelines(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(PIPELINE);
    }

    /**
     * The square a pole of this tier covers, as offsets from its base.
     *
     * <p>Flat: its y is the base's own, the plane the base stands on (the top of the block under it),
     * where the area's band runs two blocks either way. The bounds are inclusive block offsets, so
     * the far edges take a {@code +1} to enclose the end blocks rather than stopping at their near
     * corners.
     */
    private static AABB offsets(PoleTier tier) {
        SupplyArea.Bounds bounds = SupplyArea.bounds(tier);
        return new AABB(
                bounds.minX(), 0.0, bounds.minZ(),
                bounds.maxX() + 1, 0.0, bounds.maxZ() + 1);
    }

    /** The same square in world space, for a pole of this tier based here. */
    private static AABB around(BlockPos base, PoleTier tier) {
        return offsets(tier).move(base.getX(), base.getY(), base.getZ());
    }

    /**
     * Draws the square for a pole based at the <em>pose's own origin</em>.
     *
     * <p>For a block entity renderer, whose pose is already translated to the block, this is the
     * call: the square is the bare offsets and needs no camera. See
     * {@link #drawAt} for a pose sitting at the camera instead.
     */
    public static void drawAtPose(SubmitNodeCollector collector, PoseStack poseStack, Level level,
            BlockPos base, PoleTier tier) {
        drawSquare(collector, poseStack, offsets(tier));
        for (BlockPos machine : SuppliedMachines.around(level, base, tier)) {
            drawOutline(collector, poseStack, outlineOf(level, machine)
                    .move(machine.getX() - base.getX(), machine.getY() - base.getY(),
                            machine.getZ() - base.getZ()));
        }
    }

    /**
     * Draws the square for a pole based at {@code base}, for a pose sitting at the camera.
     *
     * <p>The placement preview's call: its pose is the level's, so the square carries the world
     * position and the camera offset itself.
     */
    public static void drawAt(SubmitNodeCollector collector, PoseStack poseStack, Level level,
            Vec3 camera, BlockPos base, PoleTier tier) {
        drawSquare(collector, poseStack, around(base, tier).move(-camera.x(), -camera.y(), -camera.z()));
        for (BlockPos machine : SuppliedMachines.around(level, base, tier)) {
            drawOutline(collector, poseStack, outlineOf(level, machine)
                    .move(machine.getX() - camera.x(), machine.getY() - camera.y(),
                            machine.getZ() - camera.z()));
        }
    }

    /**
     * A machine's own outline, as offsets from its block corner.
     *
     * <p>The block's shape rather than a full cube, so a machine that does not fill its block is
     * outlined where it actually is. A shape can be empty -- and an empty one would draw nothing at
     * all, which reads as the machine not being reached -- so that falls back to the whole block.
     */
    private static AABB outlineOf(Level level, BlockPos pos) {
        VoxelShape shape = level.getBlockState(pos).getShape(level, pos);
        return shape.isEmpty() ? FULL_BLOCK : shape.bounds();
    }

    /**
     * The square's four edges, at the square's own y.
     *
     * <p>Emitted directly rather than off a {@link VoxelShape}: {@link Shapes#create} gives the empty
     * shape for a box with no height, and an empty shape draws nothing at all.
     */
    private static void drawSquare(SubmitNodeCollector collector, PoseStack poseStack, AABB flat) {
        collector.submitCustomGeometry(poseStack, RENDER_TYPE, (pose, buffer) -> {
            double y = flat.minY;
            line(pose, buffer, flat.minX, y, flat.minZ, flat.maxX, y, flat.minZ);
            line(pose, buffer, flat.maxX, y, flat.minZ, flat.maxX, y, flat.maxZ);
            line(pose, buffer, flat.maxX, y, flat.maxZ, flat.minX, y, flat.maxZ);
            line(pose, buffer, flat.minX, y, flat.maxZ, flat.minX, y, flat.minZ);
        });
    }

    /**
     * A machine's outline comes off its {@link VoxelShape}, the way every other outline in the game
     * is drawn, rather than from hand-written edges that could differ from the shape; the square's
     * edges are hand-written only because it has no shape ({@link #drawSquare}). Vanilla's own
     * {@code ShapeRenderer} is not reused because it wants a whole {@link PoseStack} while the
     * collector hands out a single {@link PoseStack.Pose}, and the callback may run after the stack
     * has been popped.
     */
    private static void drawOutline(SubmitNodeCollector collector, PoseStack poseStack, AABB box) {
        collector.submitCustomGeometry(poseStack, RENDER_TYPE, (pose, buffer) ->
                Shapes.create(box).forAllEdges((x1, y1, z1, x2, y2, z2) ->
                        line(pose, buffer, x1, y1, z1, x2, y2, z2)));
    }

    /** One segment, in the colour, normal and width every outline here shares. */
    private static void line(PoseStack.Pose pose, VertexConsumer buffer,
            double x1, double y1, double z1, double x2, double y2, double z2) {
        Vector3f normal = new Vector3f(
                (float) (x2 - x1), (float) (y2 - y1), (float) (z2 - z1)).normalize();
        buffer.addVertex(pose, (float) x1, (float) y1, (float) z1)
                .setColor(COLOUR).setNormal(pose, normal).setLineWidth(WIDTH);
        buffer.addVertex(pose, (float) x2, (float) y2, (float) z2)
                .setColor(COLOUR).setNormal(pose, normal).setLineWidth(WIDTH);
    }
}

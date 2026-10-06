// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks.client;

import com.mojang.blaze3d.vertex.VertexConsumer;

import io.github._5thlayer.wireworks.PoleColumn;
import io.github._5thlayer.wireworks.WireLook;
import io.github._5thlayer.wireworks.WireStrip;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.LightCoordsUtil;
import org.joml.Matrix4f;

/**
 * A wire's geometry, drawn from one end to the other: vanilla's {@code LeashFeatureRenderer} shape
 * with the colour chosen by the caller.
 *
 * <p>Vanilla's own leash colour is fixed, and every wire Wireworks draws that is not plain brown --
 * the hand gesture's slack ({@link PoleWireRenderer}) and the Placement Preview's would-be wires
 * (ADR 0004) -- needs its own. One copy of the curve rather than one per caller, so a wire drawn by
 * the preview hangs exactly where the wire it promises will hang.
 */
public final class WireGeometry {

    /** Just under the top face of the top segment, where Factorio hangs its wire off the pole's head. */
    public static final double ATTACH_HEIGHT = 0.9;

    private WireGeometry() {
    }

    /** Where a wire hangs off the column based at {@code base}: the top segment's head. */
    public static Vec3 attachPoint(Level level, BlockPos base) {
        BlockPos top = PoleColumn.topOf(level, base);
        if (top == null) {
            top = base;
        }
        return new Vec3(top.getX() + 0.5, top.getY() + ATTACH_HEIGHT, top.getZ() + 0.5);
    }

    /**
     * @param look   the system's thickness and sag; the colour is the caller's, so a tinted slack or a
     *               faded preview can wear a system's shape
     * @param poseIn the pose at the drawing block's own origin; {@code leash.offset} carries it to
     *               the wire's start, as vanilla's does
     */
    public static void draw(Matrix4f poseIn, VertexConsumer buffer, EntityRenderState.LeashState leash,
            WireLook look, float r, float g, float b, float alpha) {
        Matrix4f pose = new Matrix4f(poseIn).translate((float) leash.offset.x, (float) leash.offset.y,
                (float) leash.offset.z);
        float dx = (float) (leash.end.x - leash.start.x);
        float dy = (float) (leash.end.y - leash.start.y);
        float dz = (float) (leash.end.z - leash.start.z);
        for (WireStrip.Vertex v : WireStrip.vertices(dx, dy, dz, look)) {
            float progress = v.step() / (float) WireStrip.STEPS;
            int block = (int) (leash.startBlockLight + (leash.endBlockLight - leash.startBlockLight) * progress);
            int sky = (int) (leash.startSkyLight + (leash.endSkyLight - leash.startSkyLight) * progress);
            buffer.addVertex(pose, v.x(), v.y(), v.z())
                    .setColor(r * v.shade(), g * v.shade(), b * v.shade(), alpha)
                    .setLight(LightCoordsUtil.pack(block, sky));
        }
    }
}

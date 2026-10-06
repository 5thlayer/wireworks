// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github._5thlayer.wireworks.WireworksRegistries;
import io.github._5thlayer.wireworks.PoleWiring;
import io.github._5thlayer.wireworks.SupplyAreaPoleBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.GlobalPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.joml.Matrix4f;
import io.github._5thlayer.wireworks.PoleColumn;
import io.github._5thlayer.wireworks.PoleKind;
import io.github._5thlayer.wireworks.PoleNetworks;
import io.github._5thlayer.wireworks.PoleTier;
import io.github._5thlayer.wireworks.ClientWires;
import io.github._5thlayer.wireworks.PoleBlock;
import io.github._5thlayer.wireworks.TransmissionSpec;
import io.github._5thlayer.wireworks.WireLook;
import io.github._5thlayer.wireworks.WireSystem;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The wire between wired poles (ADR 0004): cosmetic, and drawn with vanilla's leash geometry.
 *
 * <p>Each base pole draws the stored wires it is the first end of ({@link ClientWires}, ADR 0004),
 * from the top of its column to the top of the other's. A wire whose other end is not a loaded base
 * is not drawn, so breaking a pole takes its wires with it before the server's resend arrives.
 *
 * <p>While the local player's wire tool holds this pole as a pending end, a slack wire also hangs from it
 * to the player's hand. Looking at another pole moves its end to that pole's top, as a preview of
 * the wire, green when the click would wire, orange when it would cut and red when it would be refused ({@link PoleWiring#refuses}). Vanilla's leash colour is fixed, so the slack is drawn here.
 */
public final class PoleWireRenderer
        implements BlockEntityRenderer<BlockEntity, PoleWireRenderer.State> {

    /** Just under the top face of the top segment, where Factorio hangs its wire off the pole's head. */
    private static final double ATTACH_HEIGHT = 0.9;

    /** In first person the slack ends just ahead of and below the eye, where it stays in view. */
    private static final double FIRST_PERSON_REACH = 0.8;
    private static final double FIRST_PERSON_DROP = 0.4;

    /** The slack's colour: vanilla's leash brown while held, green, orange or red over another pole: wire, cut or refused. */
    enum Tint {
        HELD(0.5F, 0.4F, 0.3F),
        ACCEPTED(0.2F, 0.8F, 0.2F),
        CUT(1.0F, 0.55F, 0.0F),
        REFUSED(0.8F, 0.1F, 0.1F);

        final float r;
        final float g;
        final float b;

        Tint(float r, float g, float b) {
            this.r = r;
            this.g = g;
            this.b = b;
        }
    }

    public static final class State extends BlockEntityRenderState {
        final List<EntityRenderState.LeashState> wires = new ArrayList<>();
        /** Index-aligned with {@link #wires}: the stored system of each, which decides its look. */
        final List<WireSystem> systems = new ArrayList<>();
        WireSystem slackSystem = WireSystem.DISTRIBUTION;
        EntityRenderState.@Nullable LeashState slack;
        Tint tint = Tint.HELD;
        /** The tier whose Supply Area Square to draw, or null when this pole is not the one looked at. */
        @Nullable PoleTier areaTier;
        /** The base and its level, for the scan behind the square's machine outlines. */
        @Nullable Level areaLevel;
        @Nullable BlockPos areaBase;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(BlockEntity pole, State state, float partialTicks,
            Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(pole, state, partialTicks, cameraPosition, breakProgress);
        state.wires.clear();
        state.systems.clear();
        state.slackSystem = WireSystem.DISTRIBUTION;
        state.slack = null;
        state.areaTier = null;
        state.areaLevel = null;
        state.areaBase = null;
        Level level = pole.getLevel();
        if (level == null || !PoleColumn.isBase(level, pole.getBlockPos())) {
            return;
        }
        extractSupplyArea(level, pole.getBlockPos(), state);
        BlockPos from = pole.getBlockPos();
        PoleNetworks.Pos self = new PoleNetworks.Pos(from.getX(), from.getY(), from.getZ());
        Vec3 start = attachPoint(level, from);
        for (PoleNetworks.Wire stored : ClientWires.wires().all()) {
            // A stored wire's first end sorts first by position, so exactly one end draws it.
            if (!stored.a().equals(self) || previewsCut(level, stored)) {
                continue;
            }
            BlockPos to = new BlockPos(stored.b().x(), stored.b().y(), stored.b().z());
            if (!level.isLoaded(to) || !PoleColumn.isBase(level, to)) {
                continue;
            }
            EntityRenderState.LeashState wire = new EntityRenderState.LeashState();
            wire.start = start;
            wire.end = attachPoint(level, to);
            wire.offset = start.subtract(Vec3.atLowerCornerOf(from));
            BlockPos startTop = BlockPos.containing(start);
            BlockPos endTop = BlockPos.containing(wire.end);
            wire.startBlockLight = level.getBrightness(LightLayer.BLOCK, startTop);
            wire.endBlockLight = level.getBrightness(LightLayer.BLOCK, endTop);
            wire.startSkyLight = level.getBrightness(LightLayer.SKY, startTop);
            wire.endSkyLight = level.getBrightness(LightLayer.SKY, endTop);
            wire.slack = true;
            state.wires.add(wire);
            state.systems.add(stored.system());
        }
        extractSlack(level, from, start, partialTicks, state);
    }

    /**
     * The Supply Area Square for a placed pole (ADR 0005, ADR 0009), drawn only while it is the
     * pole the local player is looking at.
     *
     * <p><b>Only the aimed pole.</b> Drawing every loaded pole's square would carpet a built base in
     * overlapping wireframes, which is the opposite of legible and is not what Factorio does -- it
     * shows the area of the pole under the cursor. A pole wired to this one draws nothing either:
     * "do my two poles cover the gap" is answered by aiming at each in turn.
     *
     * <p>Looking at any segment of the column counts, and the square is the base's, the same way the
     * capability and the Jade line read from the base whatever segment is held against.
     */
    private static void extractSupplyArea(Level level, BlockPos base, State state) {
        Minecraft minecraft = Minecraft.getInstance();
        // The type is checked as well as the class: a miss is also a BlockHitResult, whose position
        // is the rounded end of the ray. A pole's collision shape is thin, so a ray can pass beside
        // one and expire in air inside that same block position -- and the square would then draw while
        // the player is looking at nothing.
        if (!(minecraft.hitResult instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK
                || !(level.getBlockState(hit.getBlockPos()).getBlock() instanceof SupplyAreaPoleBlock looked)) {
            return;
        }
        BlockPos lookedBase = PoleColumn.baseOf(level, hit.getBlockPos());
        if (lookedBase == null || !lookedBase.equals(base)) {
            return;
        }
        state.areaTier = looked.tier();
        state.areaLevel = level;
        state.areaBase = base;
    }

    /**
     * Whether the local player's held end and looked-at pole are this stored wire: its orange slack
     * is drawn over it instead, since the two would hang on the same curve and fight for the pixels.
     */
    private static boolean previewsCut(Level level, PoleNetworks.Wire stored) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !(minecraft.hitResult instanceof BlockHitResult hit)
                || !(level.getBlockState(hit.getBlockPos()).getBlock() instanceof PoleBlock)) {
            return false;
        }
        GlobalPos pending = minecraft.player.getMainHandItem().get(WireworksRegistries.PENDING_WIRE.get());
        if (pending == null || !pending.dimension().equals(level.dimension())) {
            return false;
        }
        PoleNetworks.Pos anchor = pos(pending.pos());
        PoleNetworks.Pos target = pos(PoleColumn.baseOf(level, hit.getBlockPos()));
        return (stored.a().equals(anchor) && stored.b().equals(target))
                || (stored.a().equals(target) && stored.b().equals(anchor));
    }

    private static PoleNetworks.Pos pos(BlockPos at) {
        return new PoleNetworks.Pos(at.getX(), at.getY(), at.getZ());
    }

    private static void extractSlack(Level level, BlockPos from, Vec3 start, float partialTicks, State state) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        GlobalPos pending = player.getMainHandItem().get(WireworksRegistries.PENDING_WIRE.get());
        if (pending == null || !pending.dimension().equals(level.dimension()) || !pending.pos().equals(from)
                || !(level.getBlockState(from).getBlock() instanceof PoleBlock anchorBlock)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Tint tint = Tint.HELD;
        // Held to the hand the wire is the anchor's own; over a pole it takes the system the click would make.
        WireSystem system = WireSystem.between(anchorBlock.kind(), anchorBlock.kind());
        Vec3 end = null;
        if (minecraft.hitResult instanceof BlockHitResult hit
                && level.getBlockState(hit.getBlockPos()).getBlock() instanceof PoleBlock targetBlock) {
            BlockPos base = PoleColumn.baseOf(level, hit.getBlockPos());
            PoleNetworks.Pole anchor = new PoleNetworks.Pole(from.getX(), from.getY(), from.getZ(), anchorBlock.kind());
            PoleNetworks.Pole target = new PoleNetworks.Pole(base.getX(), base.getY(), base.getZ(), targetBlock.kind());
            // Looking at another pole previews the wire itself, ending where it would hang.
            if (!base.equals(from)) {
                end = attachPoint(level, base);
                system = WireSystem.between(anchor.kind(), target.kind());
                if (PoleWiring.refuses(anchor, target)) {
                    tint = Tint.REFUSED;
                } else if (ClientWires.wires().contains(pos(from), pos(base))) {
                    tint = Tint.CUT;
                } else {
                    tint = Tint.ACCEPTED;
                }
            }
        }
        if (end == null) {
            end = minecraft.options.getCameraType().isFirstPerson()
                    // The rope hold position is at the body, behind the first-person camera.
                    ? player.getEyePosition(partialTicks).add(player.getViewVector(partialTicks).scale(FIRST_PERSON_REACH))
                            .add(0.0, -FIRST_PERSON_DROP, 0.0)
                    : player.getRopeHoldPosition(partialTicks);
        }
        EntityRenderState.LeashState slack = new EntityRenderState.LeashState();
        slack.start = start;
        slack.end = end;
        slack.offset = start.subtract(Vec3.atLowerCornerOf(from));
        BlockPos startTop = BlockPos.containing(start);
        BlockPos endAt = BlockPos.containing(end);
        slack.startBlockLight = level.getBrightness(LightLayer.BLOCK, startTop);
        slack.endBlockLight = level.getBrightness(LightLayer.BLOCK, endAt);
        slack.startSkyLight = level.getBrightness(LightLayer.SKY, startTop);
        slack.endSkyLight = level.getBrightness(LightLayer.SKY, endAt);
        state.slack = slack;
        state.tint = tint;
        state.slackSystem = system;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        PoleTier areaTier = state.areaTier;
        Level areaLevel = state.areaLevel;
        BlockPos areaBase = state.areaBase;
        if (areaTier != null && areaLevel != null && areaBase != null) {
            // This renderer's pose is already at the base's own block, so the square is the bare
            // offsets -- it must not take the camera a second time.
            SupplyAreaSquare.drawAtPose(collector, poseStack, areaLevel, areaBase, areaTier);
        }
        for (int i = 0; i < state.wires.size(); i++) {
            EntityRenderState.LeashState wire = state.wires.get(i);
            WireSystem system = state.systems.get(i);
            if (system == WireSystem.DISTRIBUTION) {
                // Vanilla's own leash, exactly as before the systems.
                collector.submitLeash(poseStack, wire);
            } else {
                WireLook look = WireLook.of(system);
                collector.submitCustomGeometry(poseStack, RenderTypes.leash(),
                        (pose, buffer) -> WireGeometry.draw(pose.pose(), buffer, wire, look,
                                look.red(), look.green(), look.blue(), 1.0F));
            }
        }
        EntityRenderState.LeashState slack = state.slack;
        if (slack != null) {
            Tint tint = state.tint;
            WireLook look = WireLook.of(state.slackSystem);
            collector.submitCustomGeometry(poseStack, RenderTypes.leash(),
                    (pose, buffer) -> WireGeometry.draw(pose.pose(), buffer, slack, look, tint.r, tint.g, tint.b, 1.0F));
        }
    }

    /** A wire leaves the frustum long after the pole that draws it does. */
    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    /**
     * Only one end draws a wire, so that end must stay drawn while the other is in view: the
     * default distance plus the longest span.
     */
    @Override
    public int getViewDistance() {
        return BlockEntityRenderer.super.getViewDistance() + (int) Math.ceil(maxWireReach());
    }

    @Override
    public AABB getRenderBoundingBox(BlockEntity pole) {
        double reach = maxWireReach();
        return new AABB(pole.getBlockPos()).inflate(reach, reach, reach);
    }

    private static double maxWireReach() {
        return Math.max(PoleTier.maxWireReach(), TransmissionSpec.wireReach());
    }

    private static Vec3 attachPoint(Level level, BlockPos base) {
        return WireGeometry.attachPoint(level, base);
    }
}

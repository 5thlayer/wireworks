// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks.client;

import io.github._5thlayer.groundworks.PlacementPlan;
import io.github._5thlayer.groundworks.client.PlacementPreviewEvent;
import io.github._5thlayer.wireworks.SupplyAreaPoleBlock;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

/** What a held pole adds to Groundworks' Placement Preview: its supply area and the wires it would make. */
final class PreviewOverlay {

    private PreviewOverlay() {
    }

    static void onOverlay(PlacementPreviewEvent.Overlay event) {
        SubmitCustomGeometryEvent geometry = event.getGeometry();
        drawSupplyArea(geometry, event.getLevel(), event.getPlan());
        PreviewWires.draw(geometry.getSubmitNodeCollector(), geometry.getPoseStack(), event.getLevel(),
                geometry.getLevelRenderState().cameraRenderState.pos, event.getPlan());
    }

    /**
     * An extension draws no box, since the column it joins already draws one; only the same pole
     * below makes one, so a small pole beside a medium column stands apart from it.
     */
    private static void drawSupplyArea(SubmitCustomGeometryEvent event, ClientLevel level, PlacementPlan plan) {
        if (plan.isRefused()) {
            return;
        }
        for (PlacementPlan.Placed placed : plan.blocks()) {
            if (!(placed.state().getBlock() instanceof SupplyAreaPoleBlock pole)) {
                continue;
            }
            if (level.getBlockState(placed.pos().below()).is(placed.state().getBlock())) {
                return;
            }
            SupplyAreaBox.drawAt(event.getSubmitNodeCollector(), event.getPoseStack(), level,
                    event.getLevelRenderState().cameraRenderState.pos, placed.pos(), pole.tier());
            return;
        }
    }
}

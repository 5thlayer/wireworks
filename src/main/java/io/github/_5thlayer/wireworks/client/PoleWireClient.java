// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks.client;

import io.github._5thlayer.wireworks.WireworksRegistries;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * The client half of the Electric Network (factoryworks#281): the wire between linked poles.
 *
 * <p>Called only on the client, from {@code Wireworks}.
 */
public final class PoleWireClient {

    private PoleWireClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(PoleWireClient::registerRenderers);
        // The Supply Area Box (factoryworks#158): its line pipeline ignores depth, which no stock line type
        // does, so it has to be registered before the first frame that draws one.
        SupplyAreaBox.register(modBus);
        NeoForge.EVENT_BUS.addListener(PreviewOverlay::onOverlay);
        NeoForge.EVENT_BUS.addListener(io.github._5thlayer.wireworks.ClientWires::onLevelUnload);
        NeoForge.EVENT_BUS.addListener(PoleWireClient::onLevelUnload);
    }

    /** The box's machine scan is cached, and a cached answer must not outlive its world. */
    private static void onLevelUnload(net.neoforged.neoforge.event.level.LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            SuppliedMachines.clear();
        }
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(WireworksRegistries.SUPPLY_AREA_POLE.get(), context -> new PoleWireRenderer());
    }
}

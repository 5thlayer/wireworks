// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import io.github._5thlayer.wireworks.client.PoleWireClient;
import io.github._5thlayer.wireworks.gametest.WireworksGameTests;
import io.github._5thlayer.wireworks.network.PoleWiresPacket;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Electric poles: a pole powers every machine inside its supply area, and wires join poles into networks. */
@Mod(Wireworks.MOD_ID)
public final class Wireworks {

    /** The mod id, which gradle.properties' {@code mod_id} must match. */
    public static final String MOD_ID = "wireworks";

    private static final String NETWORK_VERSION = "1";

    public Wireworks(IEventBus modBus, ModContainer container) {
        WireworksRegistries.register(modBus);
        WireworksConfig.register(container, modBus);
        modBus.addListener(Wireworks::registerPayloads);
        // No pole moves energy without this: poles only report and scan, and the networks settle here.
        NeoForge.EVENT_BUS.addListener(ElectricNetworks::onLevelTick);
        NeoForge.EVENT_BUS.addListener(ElectricNetworks::onLevelUnload);
        NeoForge.EVENT_BUS.addListener(LevelWires::onChunkSent);
        NeoForge.EVENT_BUS.addListener(PoleWireGesture::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(PoleWireGesture::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(PoleWireGesture::onEntityJoinLevel);
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            PoleWireClient.register(modBus);
        }
        WireworksGameTests.register(modBus);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar(NETWORK_VERSION)
                .playToClient(PoleWiresPacket.TYPE, PoleWiresPacket.STREAM_CODEC, PoleWiresPacket::handle);
    }
}

// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks.client;

import io.github._5thlayer.wireworks.SteamFluids;
import io.github._5thlayer.wireworks.Wireworks;
import io.github._5thlayer.wireworks.WireworksRegistries;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.fluid.FluidTintSources;

/** The client half of the Boiler and steam: the Boiler's screen and how steam draws in a tank or a pipe. */
public final class SteamClient {

    private static final Material STEAM_SPRITE =
            new Material(Identifier.fromNamespaceAndPath(Wireworks.MOD_ID, "block/fluid/steam"));

    private SteamClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(SteamClient::registerScreens);
        modBus.addListener(SteamClient::registerFluidModels);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(WireworksRegistries.BOILER_MENU.get(), BoilerScreen::new);
    }

    private static void registerFluidModels(RegisterFluidModelsEvent event) {
        FluidModel.Unbaked model =
                new FluidModel.Unbaked(STEAM_SPRITE, STEAM_SPRITE, null, FluidTintSources.constant(0xFFFFFFFF));
        event.register(model, SteamFluids.STEAM_SOURCE, SteamFluids.STEAM_FLOWING);
    }
}

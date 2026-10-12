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

/**
 * The client half of the Boiler and the steams: the Boiler's screen and how each steam draws in a tank
 * or a pipe. Superheated Steam wears Steam's sprite, tinted warm, until it has its own.
 */
public final class SteamClient {

    private static final Material STEAM_SPRITE =
            new Material(Identifier.fromNamespaceAndPath(Wireworks.MOD_ID, "block/fluid/steam"));

    /** Steam's sprite is near white; this greys it so a gauge's white label reads over it. */
    private static final int STEAM_TINT = 0xFF8C96A0;
    private static final int SUPERHEATED_TINT = 0xFFFFD8B0;

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
                new FluidModel.Unbaked(STEAM_SPRITE, STEAM_SPRITE, null, FluidTintSources.constant(STEAM_TINT));
        event.register(model, SteamFluids.STEAM_SOURCE, SteamFluids.STEAM_FLOWING);
        FluidModel.Unbaked superheated =
                new FluidModel.Unbaked(STEAM_SPRITE, STEAM_SPRITE, null, FluidTintSources.constant(SUPERHEATED_TINT));
        event.register(superheated, SteamFluids.SUPERHEATED_STEAM_SOURCE, SteamFluids.SUPERHEATED_STEAM_FLOWING);
    }
}

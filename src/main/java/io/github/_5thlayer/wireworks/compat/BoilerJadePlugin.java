// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks.compat;

import java.util.List;

import io.github._5thlayer.libworks.machine.Tank;
import io.github._5thlayer.libworks.machine.jade.MachineItems;
import io.github._5thlayer.libworks.machine.jade.MachineReport;
import io.github._5thlayer.libworks.machine.jade.MachineTanks;
import io.github._5thlayer.wireworks.BoilerBlock;
import io.github._5thlayer.wireworks.BoilerBlockEntity;
import io.github._5thlayer.wireworks.BoilerSpec;
import io.github._5thlayer.wireworks.BoilerStatus;
import io.github._5thlayer.wireworks.SteamFluids;
import io.github._5thlayer.wireworks.Wireworks;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * A Boiler in Jade, on libworks-runtime's machine view as Fieldworks' Harvester is: its status, in the
 * same words as its screen, so a stopped Boiler says why; its slots as icons with their counts; and
 * each tank as its own titled bar, since its tanks answer on no face of the anchor Jade reads. The fuel
 * it has banked is the screen's.
 *
 * <p>Found by Jade's annotation scan and named nowhere else, so Jade stays a soft dependency. Every
 * block of the footprint shows this: Groundworks' own Jade plugin reads a part as its origin.
 */
@WailaPlugin
public class BoilerJadePlugin implements IWailaPlugin {

    private static final Identifier ITEMS = id("boiler_items");
    private static final Identifier TANKS = id("boiler_tanks");

    private static final StreamCodec<RegistryFriendlyByteBuf, BoilerStatus> STATUS_CODEC =
            ByteBufCodecs.idMapper(BoilerStatus::of, BoilerStatus::ordinal).cast();

    private static final MachineReport<BoilerBlockEntity, BoilerStatus> REPORT =
            new MachineReport<>(id("boiler"), BoilerBlockEntity.class, BoilerBlockEntity::status, STATUS_CODEC);

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Wireworks.MOD_ID, path);
    }

    private static List<Tank> tanks(BoilerBlockEntity boiler) {
        return List.of(
                new Tank("water", new FluidStack(Fluids.WATER, boiler.water()), BoilerSpec.WATER_CAPACITY,
                        "tooltip.wireworks.boiler.jade.tank.water"),
                new Tank("steam", new FluidStack(SteamFluids.STEAM_SOURCE.get(), boiler.steam()), BoilerSpec.STEAM_CAPACITY,
                        "tooltip.wireworks.boiler.jade.tank.steam"));
    }

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(REPORT, BoilerBlockEntity.class);
        registration.registerItemStorage(new MachineItems(ITEMS), BoilerBlockEntity.class);
        registration.registerFluidStorage(new MachineTanks<>(TANKS, BoilerBlockEntity.class, BoilerJadePlugin::tanks),
                BoilerBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(new MachineReport.Client<>(REPORT, (status, tooltip) -> tooltip.add(status.line())),
                BoilerBlock.class);
        registration.registerItemStorageClient(new MachineItems.Client(ITEMS));
        registration.registerFluidStorageClient(new MachineTanks.Client(TANKS));
    }
}

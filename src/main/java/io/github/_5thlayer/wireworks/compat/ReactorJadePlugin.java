// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks.compat;

import java.util.List;

import io.github._5thlayer.libworks.machine.Tank;
import io.github._5thlayer.libworks.machine.jade.MachineItems;
import io.github._5thlayer.libworks.machine.jade.MachineReport;
import io.github._5thlayer.libworks.machine.jade.MachineTanks;
import io.github._5thlayer.wireworks.ReactorBlock;
import io.github._5thlayer.wireworks.ReactorBlockEntity;
import io.github._5thlayer.wireworks.ReactorSpec;
import io.github._5thlayer.wireworks.ReactorStatus;
import io.github._5thlayer.wireworks.SteamFluids;
import io.github._5thlayer.wireworks.Wireworks;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * A Reactor in Jade, as the Boiler is, on libworks-runtime's machine view: its status, in the same
 * words as its screen, so a stopped Reactor says why, and the loaded cell's ticks of running left; its
 * slots as icons with their counts; and each tank as its own titled bar, since its tanks answer on no
 * face of the anchor Jade reads.
 *
 * <p>Found by Jade's annotation scan and named nowhere else, so Jade stays a soft dependency. Every
 * block of the footprint shows this: Groundworks' own Jade plugin reads a part as its origin.
 */
@WailaPlugin
public class ReactorJadePlugin implements IWailaPlugin {

    private static final Identifier ITEMS = id("reactor_items");
    private static final Identifier TANKS = id("reactor_tanks");

    /** What the server sends: the status line and the cell's ticks left. */
    private record Report(ReactorStatus status, int cellTicks) {

        static Report of(ReactorBlockEntity reactor) {
            return new Report(reactor.status(), reactor.cellTicksLeft());
        }
    }

    private static final StreamCodec<RegistryFriendlyByteBuf, Report> REPORT_CODEC = StreamCodec.<ByteBuf, Report, ReactorStatus, Integer>composite(
            ByteBufCodecs.idMapper(ReactorStatus::of, ReactorStatus::ordinal), Report::status,
            ByteBufCodecs.VAR_INT, Report::cellTicks,
            Report::new).cast();

    private static final MachineReport<ReactorBlockEntity, Report> REPORT =
            new MachineReport<>(id("reactor"), ReactorBlockEntity.class, Report::of, REPORT_CODEC);

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Wireworks.MOD_ID, path);
    }

    private static List<Tank> tanks(ReactorBlockEntity reactor) {
        return List.of(
                new Tank("water", new FluidStack(Fluids.WATER, reactor.water()), ReactorSpec.WATER_CAPACITY,
                        "tooltip.wireworks.reactor.tank.water"),
                new Tank("steam", new FluidStack(SteamFluids.SUPERHEATED_STEAM_SOURCE.get(), reactor.steam()),
                        ReactorSpec.STEAM_CAPACITY, "tooltip.wireworks.reactor.tank.steam"));
    }

    private static void lines(Report report, ITooltip tooltip) {
        tooltip.add(report.status().line());
        tooltip.add(report.cellTicks() > 0
                ? Component.translatable("tooltip.wireworks.reactor.cell_ticks", report.cellTicks(), ReactorSpec.CELL_TICKS)
                : Component.translatable("tooltip.wireworks.reactor.no_cell").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(REPORT, ReactorBlockEntity.class);
        registration.registerItemStorage(new MachineItems(ITEMS), ReactorBlockEntity.class);
        registration.registerFluidStorage(new MachineTanks<>(TANKS, ReactorBlockEntity.class, ReactorJadePlugin::tanks),
                ReactorBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(new MachineReport.Client<>(REPORT, ReactorJadePlugin::lines), ReactorBlock.class);
        registration.registerItemStorageClient(new MachineItems.Client(ITEMS));
        registration.registerFluidStorageClient(new MachineTanks.Client(TANKS));
    }
}

// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks.compat;

import io.github._5thlayer.wireworks.AccumulatorBlock;
import io.github._5thlayer.wireworks.AccumulatorBlockEntity;
import io.github._5thlayer.wireworks.AccumulatorStatus;
import io.github._5thlayer.wireworks.NetworkExchange;
import io.github._5thlayer.wireworks.TransformerBlock;
import io.github._5thlayer.wireworks.TransformerBlockEntity;
import io.github._5thlayer.wireworks.TransmissionPoleBlock;
import io.github._5thlayer.wireworks.TransmissionPoleBlockEntity;
import io.github._5thlayer.wireworks.Wireworks;
import io.github._5thlayer.wireworks.NetworkReading;
import io.github._5thlayer.wireworks.PoleColumn;
import io.github._5thlayer.wireworks.SolarPanelBlock;
import io.github._5thlayer.wireworks.SteamEngineBlock;
import io.github._5thlayer.wireworks.SteamEngineBlockEntity;
import io.github._5thlayer.wireworks.SteamEngineStatus;
import io.github._5thlayer.wireworks.SolarPanelBlockEntity;
import io.github._5thlayer.wireworks.SupplyAreaPoleBlock;
import io.github._5thlayer.wireworks.SupplyAreaPoleBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/**
 * What a pole is doing right now, on the HUD, and the same for the Solar Panel, the Accumulator and
 * the Steam Engine the Library ships.
 *
 * <p>The item tooltip states what a pole <em>is</em> -- its footprint, that it is wireless, that the
 * area is measured at the base. This is the other half: the two numbers that can only be read from
 * a running pole, at the moment a player is standing in front of one asking why a machine is dark.
 *
 * <p>Below those, the pole's whole Electric Network as Factorio's hover summary shows it:
 * satisfaction, production, consumption and accumulators. The graphs over time are
 * 5thlayer/factoryworks#286.
 *
 * <p>The first two are chosen to separate the only two failure modes a player cannot otherwise tell apart.
 * <strong>Out of range</strong> reads as a machine count that does not include the machine in
 * question. <strong>Underfed</strong> reads as a count that does, with delivery below demand.
 * Without both, those two look identical -- a machine that is not running -- and the pack has no
 * other surface that would ever distinguish them.
 *
 * <p>Deliberately not here: the tier and the footprint, which the item tooltip already carries and
 * Jade prints the block's name above anyway.
 *
 * <p>This class is found by Jade's own annotation scan and is
 * referenced from nowhere else in the mod, so the jar is a compile-time dependency only.
 */
@WailaPlugin
public class PoleJadePlugin implements IWailaPlugin {

    private static final Identifier UID =
            Identifier.fromNamespaceAndPath(Wireworks.MOD_ID, "supply_area_pole");

    private static final String MACHINES = "PoleMachines";
    private static final String DELIVERED = "PoleDelivered";
    private static final String DEMANDED = "PoleDemanded";
    private static final String PRODUCED = "NetProduced";
    private static final String CHARGED = "NetCharged";
    private static final String DISCHARGED = "NetDischarged";
    private static final String STORED = "NetStored";
    private static final String CAPACITY = "NetCapacity";
    private static final String ACCUMULATORS = "NetAccumulators";
    private static final String POLES = "NetPoles";
    private static final String LINE_EXPORTED = "LineExported";
    private static final String LINE_IMPORTED = "LineImported";
    private static final String LINE_SURPLUS = "LineSurplus";
    private static final String LINE_SHORTFALL = "LineShortfall";
    private static final String SOLAR_OUTPUT = "SolarPanelOutput";
    private static final String SOLAR_SKY_HIDDEN = "SolarPanelSkyHidden";
    private static final String ACCUMULATOR_STATUS = "AccumulatorStatus";
    private static final String STEAM_ENGINE_STATUS = "SteamEngineStatus";

    private static final Identifier SOLAR_UID =
            Identifier.fromNamespaceAndPath(Wireworks.MOD_ID, "solar_panel");
    private static final Identifier ACCUMULATOR_UID =
            Identifier.fromNamespaceAndPath(Wireworks.MOD_ID, "accumulator");
    private static final Identifier STEAM_ENGINE_UID =
            Identifier.fromNamespaceAndPath(Wireworks.MOD_ID, "steam_engine");

    /**
     * The numbers live on the server, so they have to be asked for.
     *
     * <p>The block entity Jade hands over is the one at the position being looked at, which for an
     * extension is that segment's inert block entity. So this resolves to the base the same way the
     * capability does -- a column reads as one object on the HUD, exactly as it does to a connector.
     */
    private static final IServerDataProvider<BlockAccessor> DATA = new IServerDataProvider<>() {
        @Override
        public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
            BlockPos base = PoleColumn.baseOf(accessor.getLevel(), accessor.getPosition());
            if (base == null
                    || !(accessor.getLevel().getBlockEntity(base)
                    instanceof SupplyAreaPoleBlockEntity pole)) {
                return;
            }
            NetworkReading reading = pole.networkReading();
            tag.putInt(MACHINES, pole.machineCount());
            tag.putLong(DELIVERED, reading.delivered());
            tag.putLong(DEMANDED, reading.demanded());
            tag.putLong(PRODUCED, reading.produced());
            tag.putLong(CHARGED, reading.charged());
            tag.putLong(DISCHARGED, reading.discharged());
            tag.putLong(STORED, reading.stored());
            tag.putLong(CAPACITY, reading.capacity());
            tag.putInt(ACCUMULATORS, reading.accumulators());
            tag.putInt(POLES, reading.poles());
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    };

    private static final IBlockComponentProvider TOOLTIP = new IBlockComponentProvider() {
        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (!data.contains(MACHINES)) {
                return;
            }
            NetworkReading reading = new NetworkReading(
                    data.getLongOr(PRODUCED, 0L), data.getLongOr(DELIVERED, 0L),
                    data.getLongOr(DEMANDED, 0L), data.getLongOr(CHARGED, 0L),
                    data.getLongOr(DISCHARGED, 0L), data.getLongOr(STORED, 0L),
                    data.getLongOr(CAPACITY, 0L), data.getIntOr(ACCUMULATORS, 0),
                    data.getIntOr(POLES, 0));
            tooltip.add(Component.translatable("tooltip.wireworks.pole.jade.machines",
                    data.getIntOr(MACHINES, 0)));
            tooltip.add(Component.translatable("tooltip.wireworks.pole.jade.poles",
                    reading.poles()));
            int satisfaction = reading.satisfactionPercent();
            // Factorio's colours: full is green, short is yellow, nothing is red.
            ChatFormatting colour = satisfaction >= 100 ? ChatFormatting.GREEN
                    : satisfaction > 0 ? ChatFormatting.YELLOW : ChatFormatting.RED;
            tooltip.add(Component.translatable("tooltip.wireworks.pole.jade.satisfaction",
                    Component.literal(satisfaction + "%").withStyle(colour)));
            tooltip.add(Component.translatable("tooltip.wireworks.pole.jade.production",
                    reading.produced()));
            tooltip.add(Component.translatable("tooltip.wireworks.pole.jade.consumption",
                    reading.delivered(), reading.demanded()));
            if (reading.hasAccumulators()) {
                long flow = reading.accumulatorFlow();
                tooltip.add(Component.translatable("tooltip.wireworks.pole.jade.accumulators",
                        reading.stored(), reading.capacity(),
                        (flow > 0L ? "+" : "") + flow));
            }
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    };

    private static void putExchange(CompoundTag tag, NetworkExchange exchange) {
        tag.putLong(LINE_EXPORTED, exchange.exported());
        tag.putLong(LINE_IMPORTED, exchange.imported());
        tag.putLong(LINE_SURPLUS, exchange.surplus());
        tag.putLong(LINE_SHORTFALL, exchange.shortfall());
    }

    /** Transmission Poles and Transformers answer under the same id, so the "Pole network" toggle covers them. */
    private static final IServerDataProvider<BlockAccessor> LINE_DATA = new IServerDataProvider<>() {
        @Override
        public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
            BlockPos base = PoleColumn.baseOf(accessor.getLevel(), accessor.getPosition());
            if (base == null) {
                return;
            }
            var entity = accessor.getLevel().getBlockEntity(base);
            if (entity instanceof TransmissionPoleBlockEntity pole) {
                putExchange(tag, pole.networkExchange());
            } else if (entity instanceof TransformerBlockEntity transformer) {
                putExchange(tag, transformer.networkExchange());
            }
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    };

    /** The network's surplus or shortfall, which a Transmission Pole and a Transformer both name. */
    private static void networkLines(ITooltip tooltip, CompoundTag data) {
        long surplus = data.getLongOr(LINE_SURPLUS, 0L);
        long shortfall = data.getLongOr(LINE_SHORTFALL, 0L);
        if (surplus > 0L) {
            tooltip.add(Component.translatable("tooltip.wireworks.line.jade.surplus", surplus)
                    .withStyle(ChatFormatting.GREEN));
        }
        if (shortfall > 0L) {
            tooltip.add(Component.translatable("tooltip.wireworks.line.jade.shortfall", shortfall)
                    .withStyle(ChatFormatting.RED));
        }
        if (surplus == 0L && shortfall == 0L) {
            tooltip.add(Component.translatable("tooltip.wireworks.line.jade.balanced"));
        }
    }

    private static final IBlockComponentProvider TRANSMISSION_TOOLTIP = new IBlockComponentProvider() {
        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (data.contains(LINE_SURPLUS)) {
                networkLines(tooltip, data);
            }
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    };

    private static final IBlockComponentProvider TRANSFORMER_TOOLTIP = new IBlockComponentProvider() {
        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (!data.contains(LINE_SURPLUS)) {
                return;
            }
            long exported = data.getLongOr(LINE_EXPORTED, 0L);
            long imported = data.getLongOr(LINE_IMPORTED, 0L);
            if (imported > 0L) {
                tooltip.add(Component.translatable("tooltip.wireworks.transformer.jade.imported", imported));
            } else if (exported > 0L) {
                tooltip.add(Component.translatable("tooltip.wireworks.transformer.jade.exported", exported));
            } else {
                tooltip.add(Component.translatable("tooltip.wireworks.transformer.jade.idle"));
            }
            networkLines(tooltip, data);
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    };

    private static final IServerDataProvider<BlockAccessor> SOLAR_DATA = new IServerDataProvider<>() {
        @Override
        public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
            if (accessor.getBlockEntity() instanceof SolarPanelBlockEntity panel) {
                tag.putLong(SOLAR_OUTPUT, panel.currentOutputFe());
                tag.putBoolean(SOLAR_SKY_HIDDEN, panel.skyHidden());
            }
        }

        @Override
        public Identifier getUid() {
            return SOLAR_UID;
        }
    };

    private static final IBlockComponentProvider SOLAR_TOOLTIP = new IBlockComponentProvider() {
        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (!data.contains(SOLAR_OUTPUT)) {
                return;
            }
            if (data.getBooleanOr(SOLAR_SKY_HIDDEN, false)) {
                tooltip.add(Component.translatable("tooltip.wireworks.solar_panel.jade.roofed")
                        .withStyle(ChatFormatting.RED));
                return;
            }
            long fe = data.getLongOr(SOLAR_OUTPUT, 0L);
            tooltip.add(Component.translatable("tooltip.wireworks.solar_panel.jade.output", fe)
                    .withStyle(fe > 0L ? ChatFormatting.GREEN : ChatFormatting.GRAY));
        }

        @Override
        public Identifier getUid() {
            return SOLAR_UID;
        }
    };

    private static final IServerDataProvider<BlockAccessor> ACCUMULATOR_DATA = new IServerDataProvider<>() {
        @Override
        public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
            if (accessor.getBlockEntity() instanceof AccumulatorBlockEntity accumulator) {
                tag.putInt(ACCUMULATOR_STATUS, accumulator.status().map(Enum::ordinal).orElse(-1));
            }
        }

        @Override
        public Identifier getUid() {
            return ACCUMULATOR_UID;
        }
    };

    /** The charge is Jade's own energy row, read through the face; this names only what that cannot. */
    private static final IBlockComponentProvider ACCUMULATOR_TOOLTIP = new IBlockComponentProvider() {
        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            AccumulatorStatus.fromOrdinal(accessor.getServerData().getIntOr(ACCUMULATOR_STATUS, -1))
                    .ifPresent(status -> tooltip.add(Component.translatable(status.langKey())
                            .withStyle(status.problem() ? ChatFormatting.RED : ChatFormatting.GREEN)));
        }

        @Override
        public Identifier getUid() {
            return ACCUMULATOR_UID;
        }
    };

    private static final IServerDataProvider<BlockAccessor> STEAM_ENGINE_DATA = new IServerDataProvider<>() {
        @Override
        public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
            if (accessor.getBlockEntity() instanceof SteamEngineBlockEntity engine) {
                tag.putInt(STEAM_ENGINE_STATUS, engine.status().map(Enum::ordinal).orElse(-1));
            }
        }

        @Override
        public Identifier getUid() {
            return STEAM_ENGINE_UID;
        }
    };

    /** The charge is Jade's own energy row and the steam its fluid row; this names only what they cannot. */
    private static final IBlockComponentProvider STEAM_ENGINE_TOOLTIP = new IBlockComponentProvider() {
        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            SteamEngineStatus.fromOrdinal(accessor.getServerData().getIntOr(STEAM_ENGINE_STATUS, -1))
                    .ifPresent(status -> tooltip.add(Component.translatable(status.langKey())
                            .withStyle(ChatFormatting.RED)));
        }

        @Override
        public Identifier getUid() {
            return STEAM_ENGINE_UID;
        }
    };

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(DATA, SupplyAreaPoleBlockEntity.class);
        registration.registerBlockDataProvider(LINE_DATA, TransmissionPoleBlockEntity.class);
        registration.registerBlockDataProvider(LINE_DATA, TransformerBlockEntity.class);
        registration.registerBlockDataProvider(SOLAR_DATA, SolarPanelBlockEntity.class);
        registration.registerBlockDataProvider(ACCUMULATOR_DATA, AccumulatorBlockEntity.class);
        registration.registerBlockDataProvider(STEAM_ENGINE_DATA, SteamEngineBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(TOOLTIP, SupplyAreaPoleBlock.class);
        registration.registerBlockComponent(TRANSMISSION_TOOLTIP, TransmissionPoleBlock.class);
        registration.registerBlockComponent(TRANSFORMER_TOOLTIP, TransformerBlock.class);
        registration.registerBlockComponent(SOLAR_TOOLTIP, SolarPanelBlock.class);
        registration.registerBlockComponent(ACCUMULATOR_TOOLTIP, AccumulatorBlock.class);
        registration.registerBlockComponent(STEAM_ENGINE_TOOLTIP, SteamEngineBlock.class);
    }
}

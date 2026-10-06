// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import java.util.EnumMap;
import java.util.Map;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * {@code wireworks-server.toml}: each pole tier's supply area and wire reach, the Solar Panel's peak
 * and the Accumulator's capacity and flow, the last three in Factorio's watts and joules at
 * {@link ForgeEnergy#JOULES_PER_FE} J per FE. A server config, so the client draws the same areas and
 * reaches the server powers and wires by.
 */
public final class WireworksConfig {

    private record Tier(ModConfigSpec.IntValue supplySize, ModConfigSpec.DoubleValue wireReach) {
    }

    private static final Map<PoleTier, Tier> TIERS = new EnumMap<>(PoleTier.class);
    private static final ModConfigSpec SPEC;
    private static final ModConfigSpec.DoubleValue TRANSMISSION_WIRE_REACH;
    private static final ModConfigSpec.LongValue SOLAR_PEAK_WATTS;
    private static final ModConfigSpec.LongValue ACCUMULATOR_CAPACITY_JOULES;
    private static final ModConfigSpec.LongValue ACCUMULATOR_MAX_WATTS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        for (PoleTier tier : PoleTier.values()) {
            builder.push(tier.serializedName());
            TIERS.put(tier, new Tier(
                    builder.comment("The side of the square the pole supplies, in blocks.")
                            .defineInRange("supplySize", tier.defaultSupplySize(), 1, 64),
                    builder.comment("How far a wire reaches from this pole, in blocks.")
                            .defineInRange("wireReach", tier.defaultWireReach(), 1.0, 64.0)));
            builder.pop();
        }
        builder.push("transmission");
        TRANSMISSION_WIRE_REACH = builder.comment("How far a wire reaches from a Transmission Pole, in blocks.")
                .defineInRange("wireReach", TransmissionSpec.DEFAULT_WIRE_REACH, 1.0, 256.0);
        builder.pop();
        builder.push("solar_panel");
        SOLAR_PEAK_WATTS = builder.comment("The power at full daylight, in watts. 60000 is 30 FE/t.")
                .defineInRange("peak_watts", SolarPanelSpec.DEFAULT_PEAK_WATTS, 0L, 1_000_000_000L);
        builder.pop();
        builder.push("accumulator");
        ACCUMULATOR_CAPACITY_JOULES = builder.comment("What the accumulator holds, in joules. 5000000 is 50000 FE.")
                .defineInRange("capacity_joules", AccumulatorSpec.DEFAULT_CAPACITY_JOULES,
                        ForgeEnergy.JOULES_PER_FE, 1_000_000_000_000L);
        ACCUMULATOR_MAX_WATTS = builder.comment("The power it charges and discharges at, in watts. 300000 is 150 FE/t.")
                .defineInRange("max_watts", AccumulatorSpec.DEFAULT_MAX_WATTS,
                        ForgeEnergy.TICKS_PER_SECOND * ForgeEnergy.JOULES_PER_FE, 1_000_000_000_000L);
        builder.pop();
        SPEC = builder.build();
    }

    private WireworksConfig() {
    }

    static void register(ModContainer container, IEventBus modBus) {
        container.registerConfig(ModConfig.Type.SERVER, SPEC);
        modBus.addListener(ModConfigEvent.Loading.class, event -> apply(event.getConfig()));
        modBus.addListener(ModConfigEvent.Reloading.class, event -> apply(event.getConfig()));
        modBus.addListener(ModConfigEvent.Unloading.class, event -> {
            if (event.getConfig().getSpec() == SPEC) {
                TIERS.keySet().forEach(tier -> tier.configure(tier.defaultSupplySize(), tier.defaultWireReach()));
                TransmissionSpec.configure(TransmissionSpec.DEFAULT_WIRE_REACH);
                SolarPanelSpec.configure(SolarPanelSpec.DEFAULT_PEAK_WATTS);
                AccumulatorSpec.configure(AccumulatorSpec.DEFAULT_CAPACITY_JOULES, AccumulatorSpec.DEFAULT_MAX_WATTS);
            }
        });
    }

    private static void apply(ModConfig config) {
        if (config.getSpec() != SPEC) {
            return;
        }
        TIERS.forEach((tier, values) -> tier.configure(values.supplySize().get(), values.wireReach().get()));
        TransmissionSpec.configure(TRANSMISSION_WIRE_REACH.get());
        SolarPanelSpec.configure(SOLAR_PEAK_WATTS.get());
        AccumulatorSpec.configure(ACCUMULATOR_CAPACITY_JOULES.get(), ACCUMULATOR_MAX_WATTS.get());
    }
}

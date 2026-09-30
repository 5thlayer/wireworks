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
 * {@code wireworks-server.toml}: each pole tier's supply area and wire reach. A server config, so
 * the client draws the same areas and reaches the server powers and wires by.
 */
public final class WireworksConfig {

    private record Tier(ModConfigSpec.IntValue supplySize, ModConfigSpec.DoubleValue wireReach) {
    }

    private static final Map<PoleTier, Tier> TIERS = new EnumMap<>(PoleTier.class);
    private static final ModConfigSpec SPEC;

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
            }
        });
    }

    private static void apply(ModConfig config) {
        if (config.getSpec() != SPEC) {
            return;
        }
        TIERS.forEach((tier, values) -> tier.configure(values.supplySize().get(), values.wireReach().get()));
    }
}

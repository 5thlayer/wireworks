// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

/** The tags a Consumer fills to tell Wireworks about its own items, blocks and fluids. */
public final class WireworksTags {

    /** Items that wire and unwire poles by hand. */
    public static final TagKey<Item> WIRE_TOOLS = TagKey.create(Registries.ITEM, id("wire_tools"));

    /** Blocks a network draws FE from ahead of accumulators. */
    public static final TagKey<Block> GENERATORS = TagKey.create(Registries.BLOCK, id("generators"));

    /** Blocks a network discharges to cover a shortfall and charges from a generator surplus. */
    public static final TagKey<Block> ACCUMULATORS = TagKey.create(Registries.BLOCK, id("accumulators"));

    /** Plastic Resins, the items plastic is made from: tar among them. */
    public static final TagKey<Item> PLASTIC_RESINS = TagKey.create(Registries.ITEM, id("plastic_resins"));

    /** Crude: any fluid here whose source block touches lava sets as tar. Wireworks puts none in it. */
    public static final TagKey<Fluid> CRUDE_OIL =
            TagKey.create(Registries.FLUID, Identifier.fromNamespaceAndPath("c", "crude_oil"));

    private WireworksTags() {
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Wireworks.MOD_ID, path);
    }
}

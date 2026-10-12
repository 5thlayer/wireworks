// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/** The tags a Consumer fills to tell Wireworks about its own items and blocks. */
public final class WireworksTags {

    /** Items that wire and unwire poles by hand. */
    public static final TagKey<Item> WIRE_TOOLS = TagKey.create(Registries.ITEM, id("wire_tools"));

    /** Blocks a network draws FE from ahead of accumulators. */
    public static final TagKey<Block> GENERATORS = TagKey.create(Registries.BLOCK, id("generators"));

    /** Blocks a network discharges to cover a shortfall and charges from a generator surplus. */
    public static final TagKey<Block> ACCUMULATORS = TagKey.create(Registries.BLOCK, id("accumulators"));

    /** Plastic Resins, the items plastic is made from: vanilla's resin clump, bioresin, and tar from {@code c:tar} when a mod fills it. */
    public static final TagKey<Item> PLASTIC_RESINS = TagKey.create(Registries.ITEM, id("plastic_resins"));

    /** Cathodes, one of each battery's three parts: a copper ingot. */
    public static final TagKey<Item> CATHODES = TagKey.create(Registries.ITEM, id("cathodes"));

    /** Anodes, one of each battery's three parts: an iron ingot. */
    public static final TagKey<Item> ANODES = TagKey.create(Registries.ITEM, id("anodes"));

    /** Electrolytes, one of each battery's three parts: redstone, and sulfur from {@code c:dusts/sulfur} when a mod fills it. */
    public static final TagKey<Item> ELECTROLYTES = TagKey.create(Registries.ITEM, id("electrolytes"));

    /** Fission Fuel, what the Reactor burns: the Fuel Cell. */
    public static final TagKey<Item> FISSION_FUEL = TagKey.create(Registries.ITEM, id("fission_fuel"));

    /** Fissiles, what a Fuel Cell is crafted and refilled from: glowstone dust, and uranium from {@code c:ingots/uranium} when a mod fills it. */
    public static final TagKey<Item> FISSILE = TagKey.create(Registries.ITEM, id("fissile"));

    private WireworksTags() {
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Wireworks.MOD_ID, path);
    }
}

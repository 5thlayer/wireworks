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

    private WireworksTags() {
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Wireworks.MOD_ID, path);
    }
}

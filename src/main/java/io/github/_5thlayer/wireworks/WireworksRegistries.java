// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** The poles' blocks, items, block entity type and the held wire end. */
public final class WireworksRegistries {

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Wireworks.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Wireworks.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Wireworks.MOD_ID);
    private static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Wireworks.MOD_ID);

    private static final Map<PoleTier, DeferredBlock<SupplyAreaPoleBlock>> POLES = new EnumMap<>(PoleTier.class);
    private static final Map<PoleTier, DeferredItem<SupplyAreaPoleItem>> POLE_ITEMS = new EnumMap<>(PoleTier.class);

    static {
        for (PoleTier tier : PoleTier.values()) {
            POLES.put(tier, BLOCKS.registerBlock(tier.blockName(), props -> new SupplyAreaPoleBlock(tier, props)));
            POLE_ITEMS.put(tier, ITEMS.registerItem(tier.blockName(),
                    props -> new SupplyAreaPoleItem(POLES.get(tier).get(), props)));
        }
    }

    /** A pole that generates without limit, for trying a machine without a power chain. It has no recipe. */
    public static final DeferredBlock<CreativeSupplyAreaPoleBlock> CREATIVE_POLE =
            BLOCKS.registerBlock(CreativeSupplyAreaPoleBlock.BLOCK_NAME, CreativeSupplyAreaPoleBlock::new);
    public static final DeferredItem<SupplyAreaPoleItem> CREATIVE_POLE_ITEM =
            ITEMS.registerItem(CreativeSupplyAreaPoleBlock.BLOCK_NAME,
                    props -> new SupplyAreaPoleItem(CREATIVE_POLE.get(), props));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SupplyAreaPoleBlockEntity>>
            SUPPLY_AREA_POLE = BLOCK_ENTITIES.register("supply_area_pole",
                    () -> new BlockEntityType<>(SupplyAreaPoleBlockEntity::new, poleBlocks()));

    /** The first end of a wire a tool is holding, on the stack so it survives a relog and the client can draw it. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GlobalPos>>
            PENDING_WIRE = DATA_COMPONENTS.register("pending_wire",
                    () -> DataComponentType.<GlobalPos>builder()
                            .persistent(GlobalPos.CODEC)
                            .networkSynchronized(GlobalPos.STREAM_CODEC)
                            .build());

    private WireworksRegistries() {
    }

    public static DeferredBlock<SupplyAreaPoleBlock> pole(PoleTier tier) {
        return POLES.get(tier);
    }

    public static DeferredItem<SupplyAreaPoleItem> poleItem(PoleTier tier) {
        return POLE_ITEMS.get(tier);
    }

    private static Set<Block> poleBlocks() {
        return Stream.concat(POLES.values().stream(), Stream.of(CREATIVE_POLE))
                .map(DeferredBlock::get)
                .collect(Collectors.toUnmodifiableSet());
    }

    static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        DATA_COMPONENTS.register(modBus);
        modBus.addListener(WireworksRegistries::addToCreativeTabs);
    }

    private static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            List.copyOf(POLE_ITEMS.values()).forEach(item -> event.accept(item.get()));
            event.accept(CREATIVE_POLE_ITEM.get());
        }
    }
}

// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** The poles' blocks, items, block entity type, creative tab and the held wire end. */
public final class WireworksRegistries {

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Wireworks.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Wireworks.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Wireworks.MOD_ID);
    private static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Wireworks.MOD_ID);
    private static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Wireworks.MOD_ID);

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

    /** The Library's creative tab, {@code wireworks:items}: every pole. */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CREATIVE_TAB =
            CREATIVE_TABS.register("items", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.wireworks.items"))
                    .icon(() -> new ItemStack(poleItem(PoleTier.LARGE).get()))
                    .displayItems((parameters, output) -> poleItems().forEach(output::accept))
                    .build());

    private WireworksRegistries() {
    }

    public static DeferredBlock<SupplyAreaPoleBlock> pole(PoleTier tier) {
        return POLES.get(tier);
    }

    public static DeferredItem<SupplyAreaPoleItem> poleItem(PoleTier tier) {
        return POLE_ITEMS.get(tier);
    }

    /** Every pole's item, the tiers in order and the creative pole last. */
    private static Stream<Item> poleItems() {
        return Stream.concat(POLE_ITEMS.values().stream(), Stream.of(CREATIVE_POLE_ITEM)).map(DeferredItem::get);
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
        CREATIVE_TABS.register(modBus);
        modBus.addListener(WireworksRegistries::addToCreativeTabs);
    }

    private static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            poleItems().forEach(event::accept);
        }
    }
}

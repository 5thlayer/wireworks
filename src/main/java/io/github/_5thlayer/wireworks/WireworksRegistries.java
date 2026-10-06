// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import io.github._5thlayer.groundworks.Footprint;
import io.github._5thlayer.groundworks.FootprintItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** The poles', Solar Panel's and Accumulator's blocks, items and block entity types, the creative tab and the held wire end. */
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

    /** The Transmission Pole: no tier, no area, wires only. */
    public static final DeferredBlock<TransmissionPoleBlock> TRANSMISSION_POLE =
            BLOCKS.registerBlock(TransmissionPoleBlock.BLOCK_NAME, TransmissionPoleBlock::new);
    public static final DeferredItem<TransmissionPoleItem> TRANSMISSION_POLE_ITEM =
            ITEMS.registerItem(TransmissionPoleBlock.BLOCK_NAME,
                    props -> new TransmissionPoleItem(TRANSMISSION_POLE.get(), props));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TransmissionPoleBlockEntity>>
            TRANSMISSION_POLE_ENTITY = BLOCK_ENTITIES.register("transmission_pole",
                    () -> new BlockEntityType<>(TransmissionPoleBlockEntity::new, TRANSMISSION_POLE.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SupplyAreaPoleBlockEntity>>
            SUPPLY_AREA_POLE = BLOCK_ENTITIES.register("supply_area_pole",
                    () -> new BlockEntityType<>(SupplyAreaPoleBlockEntity::new, poleBlocks()));

    public static final DeferredBlock<SolarPanelBlock> SOLAR_PANEL =
            BLOCKS.registerBlock(SolarPanelBlock.BLOCK_NAME, SolarPanelBlock::new);
    public static final DeferredBlock<EnergyPartBlock> SOLAR_PANEL_PART = BLOCKS.registerBlock(
            "solar_panel_part", props -> new EnergyPartBlock(props, () -> WireworksRegistries.SOLAR_PANEL_FOOTPRINT));
    public static final DeferredItem<FootprintItem> SOLAR_PANEL_ITEM = ITEMS.registerItem(
            SolarPanelBlock.BLOCK_NAME, props -> new FootprintItem(WireworksRegistries.SOLAR_PANEL_FOOTPRINT, props));
    public static final Footprint SOLAR_PANEL_FOOTPRINT = Footprint.declare(
            EnergyFootprints.SOLAR_PANEL, SOLAR_PANEL, SOLAR_PANEL_PART, SOLAR_PANEL_ITEM);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SolarPanelBlockEntity>>
            SOLAR_PANEL_ENTITY = BLOCK_ENTITIES.register("solar_panel",
                    () -> new BlockEntityType<>(SolarPanelBlockEntity::new, SOLAR_PANEL.get()));

    public static final DeferredBlock<AccumulatorBlock> ACCUMULATOR =
            BLOCKS.registerBlock(AccumulatorBlock.BLOCK_NAME, AccumulatorBlock::new);
    public static final DeferredBlock<EnergyPartBlock> ACCUMULATOR_PART = BLOCKS.registerBlock(
            "accumulator_part", props -> new EnergyPartBlock(props, () -> WireworksRegistries.ACCUMULATOR_FOOTPRINT));
    public static final DeferredItem<FootprintItem> ACCUMULATOR_ITEM = ITEMS.registerItem(
            AccumulatorBlock.BLOCK_NAME, props -> new FootprintItem(WireworksRegistries.ACCUMULATOR_FOOTPRINT, props));
    public static final Footprint ACCUMULATOR_FOOTPRINT = Footprint.declare(
            EnergyFootprints.ACCUMULATOR, ACCUMULATOR, ACCUMULATOR_PART, ACCUMULATOR_ITEM);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AccumulatorBlockEntity>>
            ACCUMULATOR_ENTITY = BLOCK_ENTITIES.register("accumulator",
                    () -> new BlockEntityType<>(AccumulatorBlockEntity::new, ACCUMULATOR.get()));

    /** The first end of a wire a tool is holding, on the stack so it survives a relog and the client can draw it. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GlobalPos>>
            PENDING_WIRE = DATA_COMPONENTS.register("pending_wire",
                    () -> DataComponentType.<GlobalPos>builder()
                            .persistent(GlobalPos.CODEC)
                            .networkSynchronized(GlobalPos.STREAM_CODEC)
                            .build());

    /** The Library's creative tab, {@code wireworks:items}: every pole, the Solar Panel and the Accumulator. */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CREATIVE_TAB =
            CREATIVE_TABS.register("items", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.wireworks.items"))
                    .icon(() -> new ItemStack(poleItem(PoleTier.LARGE).get()))
                    .displayItems((parameters, output) -> items().forEach(output::accept))
                    .build());

    private WireworksRegistries() {
    }

    public static DeferredBlock<SupplyAreaPoleBlock> pole(PoleTier tier) {
        return POLES.get(tier);
    }

    public static DeferredItem<SupplyAreaPoleItem> poleItem(PoleTier tier) {
        return POLE_ITEMS.get(tier);
    }

    /** Every pole's item, the tiers in order, then the Transmission Pole, and the creative pole last. */
    private static Stream<Item> poleItems() {
        return Stream.concat(POLE_ITEMS.values().stream(), Stream.of(TRANSMISSION_POLE_ITEM, CREATIVE_POLE_ITEM))
                .map(DeferredItem::get);
    }

    /** Every item the Library adds: the poles, then the Solar Panel and the Accumulator. */
    private static Stream<Item> items() {
        return Stream.concat(poleItems(), Stream.of(SOLAR_PANEL_ITEM, ACCUMULATOR_ITEM).map(DeferredItem::get));
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
        modBus.addListener(WireworksRegistries::registerCapabilities);
    }

    /**
     * The energy face on the anchor and on every part of each footprint, answering with the anchor's
     * buffer, so a pole reaching any block of it finds it.
     */
    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        energyOnFootprint(event, SOLAR_PANEL, SOLAR_PANEL_PART, SolarPanelBlockEntity.class, SolarPanelBlockEntity::energy);
        energyOnFootprint(event, ACCUMULATOR, ACCUMULATOR_PART, AccumulatorBlockEntity.class, AccumulatorBlockEntity::energy);
    }

    private static <E extends BlockEntity> void energyOnFootprint(RegisterCapabilitiesEvent event,
            DeferredBlock<? extends Block> anchor, DeferredBlock<EnergyPartBlock> part, Class<E> anchorType,
            Function<E, EnergyHandler> face) {
        event.registerBlock(Capabilities.Energy.BLOCK,
                (level, pos, state, entity, side) -> anchorType.isInstance(entity) ? face.apply(anchorType.cast(entity)) : null,
                anchor.get());
        event.registerBlock(Capabilities.Energy.BLOCK, (level, pos, state, entity, side) -> {
            BlockPos at = part.get().energyOwner(pos, state);
            return anchorType.isInstance(level.getBlockEntity(at))
                    ? face.apply(anchorType.cast(level.getBlockEntity(at))) : null;
        }, part.get());
    }

    private static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            items().forEach(event::accept);
        }
    }
}

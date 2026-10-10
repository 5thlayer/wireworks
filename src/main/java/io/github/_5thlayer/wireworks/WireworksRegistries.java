// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import io.github._5thlayer.groundworks.Footprint;
import io.github._5thlayer.groundworks.FootprintItem;
import io.github._5thlayer.groundworks.FootprintPartBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.EventPriority;
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
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** The Library's blocks, items and block entity types, its creative tab and the held wire end. */
public final class WireworksRegistries {

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Wireworks.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Wireworks.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Wireworks.MOD_ID);
    private static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Wireworks.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Wireworks.MOD_ID);
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

    /** The Transformer: the joint between a Transmission Line and a District. One block, not a column. */
    public static final DeferredBlock<TransformerBlock> TRANSFORMER =
            BLOCKS.registerBlock(TransformerBlock.BLOCK_NAME, TransformerBlock::new);
    public static final DeferredItem<TransformerItem> TRANSFORMER_ITEM =
            ITEMS.registerItem(TransformerBlock.BLOCK_NAME,
                    props -> new TransformerItem(TRANSFORMER.get(), props));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TransformerBlockEntity>>
            TRANSFORMER_ENTITY = BLOCK_ENTITIES.register("transformer",
                    () -> new BlockEntityType<>(TransformerBlockEntity::new, TRANSFORMER.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SupplyAreaPoleBlockEntity>>
            SUPPLY_AREA_POLE = BLOCK_ENTITIES.register("supply_area_pole",
                    () -> new BlockEntityType<>(SupplyAreaPoleBlockEntity::new, poleBlocks()));

    public static final DeferredBlock<SolarPanelBlock> SOLAR_PANEL =
            BLOCKS.registerBlock(SolarPanelBlock.BLOCK_NAME, SolarPanelBlock::new);
    public static final DeferredBlock<FootprintPartBlock> SOLAR_PANEL_PART =
            part("solar_panel_part", () -> WireworksRegistries.SOLAR_PANEL_FOOTPRINT);
    public static final DeferredItem<FootprintItem> SOLAR_PANEL_ITEM = ITEMS.registerItem(
            SolarPanelBlock.BLOCK_NAME, props -> new FootprintItem(WireworksRegistries.SOLAR_PANEL_FOOTPRINT, props));
    public static final Footprint SOLAR_PANEL_FOOTPRINT = Footprint.declare(
            EnergyFootprints.SOLAR_PANEL, SOLAR_PANEL, SOLAR_PANEL_PART, SOLAR_PANEL_ITEM);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SolarPanelBlockEntity>>
            SOLAR_PANEL_ENTITY = BLOCK_ENTITIES.register("solar_panel",
                    () -> new BlockEntityType<>(SolarPanelBlockEntity::new, SOLAR_PANEL.get()));

    public static final DeferredBlock<AccumulatorBlock> ACCUMULATOR =
            BLOCKS.registerBlock(AccumulatorBlock.BLOCK_NAME, AccumulatorBlock::new);
    public static final DeferredBlock<FootprintPartBlock> ACCUMULATOR_PART =
            part("accumulator_part", () -> WireworksRegistries.ACCUMULATOR_FOOTPRINT);
    public static final DeferredItem<FootprintItem> ACCUMULATOR_ITEM = ITEMS.registerItem(
            AccumulatorBlock.BLOCK_NAME, props -> new FootprintItem(WireworksRegistries.ACCUMULATOR_FOOTPRINT, props));
    public static final Footprint ACCUMULATOR_FOOTPRINT = Footprint.declare(
            EnergyFootprints.ACCUMULATOR, ACCUMULATOR, ACCUMULATOR_PART, ACCUMULATOR_ITEM);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AccumulatorBlockEntity>>
            ACCUMULATOR_ENTITY = BLOCK_ENTITIES.register("accumulator",
                    () -> new BlockEntityType<>(AccumulatorBlockEntity::new, ACCUMULATOR.get()));

    public static final DeferredBlock<BoilerBlock> BOILER = BLOCKS.registerBlock(BoilerBlock.BLOCK_NAME, BoilerBlock::new);
    public static final DeferredBlock<FootprintPartBlock> BOILER_PART =
            part("boiler_part", () -> WireworksRegistries.BOILER_FOOTPRINT);
    public static final DeferredItem<BoilerItem> BOILER_ITEM =
            ITEMS.registerItem(BoilerBlock.BLOCK_NAME, BoilerItem::new);
    public static final Footprint BOILER_FOOTPRINT = Footprint.declare(
            SteamFootprints.BOILER, BOILER, BOILER_PART, BOILER_ITEM);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BoilerBlockEntity>>
            BOILER_ENTITY = BLOCK_ENTITIES.register("boiler",
                    () -> new BlockEntityType<>(BoilerBlockEntity::new, BOILER.get()));
    public static final Supplier<MenuType<BoilerMenu>> BOILER_MENU =
            MENUS.register("boiler", () -> new MenuType<>(BoilerMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredBlock<SteamEngineBlock> STEAM_ENGINE =
            BLOCKS.registerBlock(SteamEngineBlock.BLOCK_NAME, SteamEngineBlock::new);
    public static final DeferredBlock<FootprintPartBlock> STEAM_ENGINE_PART =
            part("steam_engine_part", () -> WireworksRegistries.STEAM_ENGINE_FOOTPRINT);
    public static final DeferredItem<SteamEngineItem> STEAM_ENGINE_ITEM =
            ITEMS.registerItem(SteamEngineBlock.BLOCK_NAME, SteamEngineItem::new);
    public static final Footprint STEAM_ENGINE_FOOTPRINT = Footprint.declare(
            SteamFootprints.STEAM_ENGINE, STEAM_ENGINE, STEAM_ENGINE_PART, STEAM_ENGINE_ITEM);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SteamEngineBlockEntity>>
            STEAM_ENGINE_ENTITY = BLOCK_ENTITIES.register("steam_engine",
                    () -> new BlockEntityType<>(SteamEngineBlockEntity::new, STEAM_ENGINE.get()));

    /** The first end of a wire a tool is holding, on the stack so it survives a relog and the client can draw it. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GlobalPos>>
            PENDING_WIRE = DATA_COMPONENTS.register("pending_wire",
                    () -> DataComponentType.<GlobalPos>builder()
                            .persistent(GlobalPos.CODEC)
                            .networkSynchronized(GlobalPos.STREAM_CODEC)
                            .build());

    /** The Library's creative tab, {@code wireworks:items}. */
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

    /** Every pole's item, the tiers in order, then the Transmission Pole and the Transformer, and the creative pole last. */
    private static Stream<Item> poleItems() {
        return Stream.concat(POLE_ITEMS.values().stream(), Stream.of(TRANSMISSION_POLE_ITEM, TRANSFORMER_ITEM, CREATIVE_POLE_ITEM))
                .map(DeferredItem::get);
    }

    /** Every item the Library adds, the poles first. */
    private static Stream<Item> items() {
        return Stream.concat(poleItems(), Stream.of(SOLAR_PANEL_ITEM, ACCUMULATOR_ITEM, BOILER_ITEM, STEAM_ENGINE_ITEM).map(DeferredItem::get));
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
        MENUS.register(modBus);
        DATA_COMPONENTS.register(modBus);
        CREATIVE_TABS.register(modBus);
        modBus.addListener(WireworksRegistries::addToCreativeTabs);
        modBus.addListener(WireworksRegistries::registerCapabilities);
        // Ahead of Groundworks' forwarding, which answers a part with its anchor's face and so cannot tell which port was reached.
        modBus.addListener(EventPriority.HIGHEST, RegisterCapabilitiesEvent.class, WireworksRegistries::registerBoilerPorts);
    }

    /**
     * The energy face on each footprint's anchor, answering with its buffer. Groundworks forwards a
     * part's lookup to its anchor, so a pole reaching any block of the footprint finds it.
     */
    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        energyOnAnchor(event, SOLAR_PANEL, SolarPanelBlockEntity.class, SolarPanelBlockEntity::energy);
        energyOnAnchor(event, ACCUMULATOR, AccumulatorBlockEntity.class, AccumulatorBlockEntity::energy);
        energyOnAnchor(event, STEAM_ENGINE, SteamEngineBlockEntity.class, SteamEngineBlockEntity::energy);
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, STEAM_ENGINE_ENTITY.get(),
                (engine, side) -> engine.steamFace());
        event.registerBlockEntity(Capabilities.Item.BLOCK, BOILER_ENTITY.get(), (boiler, side) -> boiler.itemFace());
    }

    /** The Boiler's part blocks answer for their own port (ADR-0011). */
    private static void registerBoilerPorts(RegisterCapabilitiesEvent event) {
        event.registerBlock(Capabilities.Fluid.BLOCK, (level, pos, state, entity, side) -> {
            BlockPos at = BOILER_FOOTPRINT.standingOrigin(level, pos, state);
            if (at == null || !(level.getBlockEntity(at) instanceof BoilerBlockEntity boiler)) {
                return null;
            }
            return boiler.fluidFace(SteamFootprints.partOf(state), state.getValue(Footprint.FACING), side);
        }, BOILER_PART.get());
    }

    /**
     * A footprint's part block, which drops nothing of its own: its origin drops the item. Groundworks
     * forwards its lookups to its origin, which is also the energy owner a pole counts it by.
     */
    private static DeferredBlock<FootprintPartBlock> part(String name, Supplier<Footprint> footprint) {
        return BLOCKS.registerBlock(name, props -> new FootprintPartBlock(props.noLootTable(), footprint));
    }

    private static <E extends BlockEntity> void energyOnAnchor(RegisterCapabilitiesEvent event,
            DeferredBlock<? extends Block> anchor, Class<E> anchorType, Function<E, EnergyHandler> face) {
        event.registerBlock(Capabilities.Energy.BLOCK,
                (level, pos, state, entity, side) -> anchorType.isInstance(entity) ? face.apply(anchorType.cast(entity)) : null,
                anchor.get());
    }

    private static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            items().forEach(event::accept);
        }
    }
}

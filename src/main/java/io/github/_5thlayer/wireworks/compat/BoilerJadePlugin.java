// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks.compat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.github._5thlayer.wireworks.BoilerBlock;
import io.github._5thlayer.wireworks.BoilerBlockEntity;
import io.github._5thlayer.wireworks.BoilerSpec;
import io.github._5thlayer.wireworks.BoilerStatus;
import io.github._5thlayer.wireworks.SteamFluids;
import io.github._5thlayer.wireworks.Wireworks;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;
import snownee.jade.api.Accessor;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.StreamServerDataProvider;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.fluid.JadeFluidObject;
import snownee.jade.api.ui.IDisplayHelper;
import snownee.jade.api.view.ClientViewGroup;
import snownee.jade.api.view.FluidView;
import snownee.jade.api.view.IClientExtensionProvider;
import snownee.jade.api.view.IServerExtensionProvider;
import snownee.jade.api.view.ItemView;
import snownee.jade.api.view.ViewGroup;

/**
 * A Boiler in Jade, built as Fieldworks' Harvester's is: its status, in the same words as its screen,
 * so a stopped Boiler says why; its slots as icons with their counts; and each tank as its own titled
 * bar, since its tanks answer on no face of the anchor Jade reads. The fuel it has banked is the
 * screen's.
 *
 * <p>Found by Jade's annotation scan and named nowhere else, so Jade stays a soft dependency. Every
 * block of the footprint shows this: Groundworks' own Jade plugin reads a part as its origin.
 */
@WailaPlugin
public class BoilerJadePlugin implements IWailaPlugin {

    private static final Identifier UID = Identifier.fromNamespaceAndPath(Wireworks.MOD_ID, "boiler");
    private static final Identifier TANKS = Identifier.fromNamespaceAndPath(Wireworks.MOD_ID, "boiler_tanks");
    private static final Identifier ITEMS = Identifier.fromNamespaceAndPath(Wireworks.MOD_ID, "boiler_items");
    private static final String WATER = "water";
    private static final String STEAM = "steam";

    private static final StreamCodec<RegistryFriendlyByteBuf, BoilerStatus> STATUS_CODEC =
            ByteBufCodecs.idMapper(BoilerStatus::of, BoilerStatus::ordinal).cast();

    private static class Report implements StreamServerDataProvider<BlockAccessor, BoilerStatus> {

        @Override
        public @Nullable BoilerStatus streamData(BlockAccessor accessor) {
            return accessor.getBlockEntity() instanceof BoilerBlockEntity boiler ? boiler.status() : null;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, BoilerStatus> streamCodec() {
            return STATUS_CODEC;
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    }

    /** Draws the status; apart from {@link Report} so that a dedicated server never loads Jade's drawing. */
    private static final class Client extends Report implements IBlockComponentProvider {

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            decodeFromData(accessor).ifPresent(status -> tooltip.add(status.line()));
        }
    }

    /** What the item capability holds, slot by slot. */
    private static final IServerExtensionProvider<ItemStack> ITEM_DATA = new IServerExtensionProvider<>() {
        @Override
        public @Nullable List<ViewGroup<ItemStack>> getGroups(Accessor<?> accessor) {
            if (!(accessor instanceof BlockAccessor block) || !(block.getBlockEntity() instanceof BoilerBlockEntity)) {
                return null;
            }
            ResourceHandler<ItemResource> items = block.getLevel().getCapability(Capabilities.Item.BLOCK, block.getPosition(), null);
            if (items == null) {
                return null;
            }
            List<ItemStack> stacks = new ArrayList<>();
            for (int index = 0; index < items.size(); index++) {
                if (!items.getResource(index).isEmpty()) {
                    stacks.add(items.getResource(index).toStack(items.getAmountAsInt(index)));
                }
            }
            return List.of(new ViewGroup<>(stacks));
        }

        @Override
        public Identifier getUid() {
            return ITEMS;
        }
    };

    /** Each stack its count as amount text, which makes Jade draw icons where a few stacks would get their names. */
    private static final IClientExtensionProvider<ItemStack, ItemView> ITEM_ICONS = new IClientExtensionProvider<>() {
        @Override
        public List<ClientViewGroup<ItemView>> getClientGroups(Accessor<?> accessor, List<ViewGroup<ItemStack>> groups) {
            return ClientViewGroup.map(groups, stack -> new ItemView(stack)
                    .amountText(IDisplayHelper.get().humanReadableNumber(stack.getCount(), "", false, null)), null);
        }

        @Override
        public Identifier getUid() {
            return ITEMS;
        }
    };

    /** Each tank its own titled group. */
    private static final IServerExtensionProvider<FluidView.Data> TANK_DATA = new IServerExtensionProvider<>() {
        @Override
        public @Nullable List<ViewGroup<FluidView.Data>> getGroups(Accessor<?> accessor) {
            if (!(accessor instanceof BlockAccessor block) || !(block.getBlockEntity() instanceof BoilerBlockEntity boiler)) {
                return null;
            }
            return List.of(tank(WATER, Fluids.WATER, boiler.water(), BoilerSpec.WATER_CAPACITY),
                    tank(STEAM, SteamFluids.STEAM_SOURCE.get(), boiler.steam(), BoilerSpec.STEAM_CAPACITY));
        }

        @Override
        public Identifier getUid() {
            return TANKS;
        }
    };

    private static ViewGroup<FluidView.Data> tank(String id, Fluid fluid, int amount, int capacity) {
        JadeFluidObject shown = amount <= 0 ? JadeFluidObject.empty() : JadeFluidObject.of(fluid, amount);
        return new ViewGroup<>(List.of(new FluidView.Data(shown, capacity)), Optional.of(id), Optional.empty());
    }

    private static final IClientExtensionProvider<FluidView.Data, FluidView> TANK_LINES = new IClientExtensionProvider<>() {
        @Override
        public List<ClientViewGroup<FluidView>> getClientGroups(Accessor<?> accessor, List<ViewGroup<FluidView.Data>> groups) {
            return ClientViewGroup.map(groups, FluidView::readDefault, (group, shown) ->
                    shown.title = Component.translatable("tooltip.wireworks.boiler.jade.tank." + group.id));
        }

        @Override
        public Identifier getUid() {
            return TANKS;
        }
    };

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(new Report(), BoilerBlockEntity.class);
        registration.registerFluidStorage(TANK_DATA, BoilerBlockEntity.class);
        registration.registerItemStorage(ITEM_DATA, BoilerBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(new Client(), BoilerBlock.class);
        registration.registerFluidStorageClient(TANK_LINES);
        registration.registerItemStorageClient(ITEM_ICONS);
    }
}

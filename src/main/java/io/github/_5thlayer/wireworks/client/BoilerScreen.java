// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks.client;

import java.util.List;
import java.util.Optional;

import io.github._5thlayer.libworks.ghost.Ghost;
import io.github._5thlayer.libworks.ghost.Ghosts;
import io.github._5thlayer.libworks.ghost.client.GhostDraw;
import io.github._5thlayer.libworks.machine.client.MachineDraw;
import io.github._5thlayer.wireworks.BoilerMenu;
import io.github._5thlayer.wireworks.BoilerSlots;
import io.github._5thlayer.wireworks.BoilerSpec;
import io.github._5thlayer.wireworks.SteamFluids;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * The Boiler's screen, a stand-in drawn from fills as Fieldworks' Harvester's is: down the left, each
 * input slot beside what it feeds, the fuel slot beside the fuel it has banked, the water slot beside
 * the water tank and the steam slot beside the steam tank; the output on the right; and below them the
 * status, which says why a stopped Boiler stopped. Its empty input slots show ghosts of what goes in
 * them, and EMI's recipe and usage keys work on those. The panel, bars and gauges are libworks-runtime's
 * {@link MachineDraw}, shared with the other machines' screens.
 */
public class BoilerScreen extends AbstractContainerScreen<BoilerMenu> {

    private static final int BURN = 0xFFD9822B;

    /** Each gauge stands in its slot's row, a little right of it and two pixels down to centre on it. */
    private static final int GAUGE_X = 28;
    private static final int GAUGE_WIDTH = 100;
    private static final int GAUGE_DROP = 2;
    private static final int STATUS_Y = 78;

    /** What the empty fuel, water and steam slots show, and what EMI's shortcuts ask of. */
    private final Ghosts ghosts = new Ghosts(() -> leftPos, () -> topPos);

    public BoilerScreen(BoilerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, BoilerMenu.INVENTORY_Y + 82);
        inventoryLabelY = BoilerMenu.INVENTORY_Y - 11;
        for (int slot : List.of(BoilerSlots.FUEL, BoilerSlots.WATER, BoilerSlots.STEAM)) {
            ghosts.add(menu.slots.get(slot));
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // The world dims behind the panel as it does behind the inventory; skipping super left it bright.
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        MachineDraw.panel(graphics, font, leftPos, topPos, imageWidth, imageHeight, menu.slots, ghosts, Util.getMillis());
        int left = leftPos + GAUGE_X;
        MachineDraw.bar(graphics, font, left, topPos + gaugeY(BoilerMenu.FUEL_Y), GAUGE_WIDTH, MachineDraw.BAR_HEIGHT,
                menu.fuelLevel(), BURN, burnLabel());
        MachineDraw.gauge(graphics, font, left, topPos + gaugeY(BoilerMenu.WATER_Y), GAUGE_WIDTH,
                new FluidStack(Fluids.WATER, menu.water()), menu.waterCapacity());
        MachineDraw.gauge(graphics, font, left, topPos + gaugeY(BoilerMenu.STEAM_Y), GAUGE_WIDTH,
                new FluidStack(SteamFluids.STEAM_SOURCE.get(), menu.steam()), menu.steamCapacity());
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        graphics.text(font, MachineDraw.fitted(font, menu.status().line(), statusWidth()), MachineDraw.MARGIN, STATUS_Y,
                MachineDraw.TEXT, false);
    }

    /** The ghost stack under the mouse, with the top-left of its slot on screen, for EMI's recipe and usage keys. */
    public Optional<Ghost> ghostAt(double mouseX, double mouseY) {
        return ghosts.at(mouseX, mouseY, Util.getMillis());
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        Optional<Ghost> ghost = ghostAt(mouseX, mouseY);
        if (ghost.isPresent()) {
            GhostDraw.tooltip(graphics, font, ghost.get(), mouseX, mouseY);
        } else if (overGauge(BoilerMenu.FUEL_Y, mouseX, mouseY)) {
            graphics.setComponentTooltipForNextFrame(font, fuelTooltip(), mouseX, mouseY);
        } else if (overGauge(BoilerMenu.WATER_Y, mouseX, mouseY)) {
            graphics.setComponentTooltipForNextFrame(font, List.of(Component.translatable(
                    "tooltip.wireworks.boiler.water", menu.water(), menu.waterCapacity())), mouseX, mouseY);
        } else if (overGauge(BoilerMenu.STEAM_Y, mouseX, mouseY)) {
            graphics.setComponentTooltipForNextFrame(font, List.of(Component.translatable(
                    "tooltip.wireworks.boiler.steam", menu.steam(), menu.steamCapacity())), mouseX, mouseY);
        } else if (MachineDraw.over(leftPos + MachineDraw.MARGIN, topPos + STATUS_Y, statusWidth(), font.lineHeight, mouseX, mouseY)) {
            MachineDraw.wholeOnHover(graphics, font, menu.status().line(), statusWidth(), mouseX, mouseY);
        }
    }

    private static int gaugeY(int slotY) {
        return slotY + GAUGE_DROP;
    }

    private boolean overGauge(int slotY, int mouseX, int mouseY) {
        return MachineDraw.over(leftPos + GAUGE_X, topPos + gaugeY(slotY), GAUGE_WIDTH, MachineDraw.BAR_HEIGHT, mouseX, mouseY);
    }

    /** The status line's room, the panel less a margin either side; a longer line is cut and shown whole on hover. */
    private int statusWidth() {
        return imageWidth - 2 * MachineDraw.MARGIN;
    }

    /** The seconds banked; nothing on an empty bar, since the status line already says the Boiler is out of fuel. */
    private Component burnLabel() {
        int joules = menu.fuelStored();
        return joules <= 0
                ? Component.empty()
                : Component.translatable("tooltip.wireworks.boiler.burn", BoilerSpec.secondsOfFuel(joules));
    }

    /** What is in the fuel buffer, in the ladder's own shape: what is held, and what it costs. */
    private List<Component> fuelTooltip() {
        int joules = menu.fuelStored();
        if (joules <= 0) {
            return List.of(Component.translatable("tooltip.wireworks.boiler.fuel.out")
                    .withStyle(ChatFormatting.GRAY));
        }
        return List.of(
                Component.translatable("tooltip.wireworks.boiler.fuel", joules,
                        menu.fuelCapacity()),
                Component.translatable("tooltip.wireworks.boiler.fuel.seconds",
                                BoilerSpec.secondsOfFuel(joules), BoilerSpec.JOULES_PER_TICK)
                        .withStyle(ChatFormatting.GRAY));
    }
}

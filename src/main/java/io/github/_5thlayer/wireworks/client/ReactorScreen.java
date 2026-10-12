// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks.client;

import java.util.List;
import java.util.Optional;

import io.github._5thlayer.libworks.ghost.Ghost;
import io.github._5thlayer.libworks.ghost.GhostScreen;
import io.github._5thlayer.libworks.ghost.Ghosts;
import io.github._5thlayer.libworks.ghost.client.GhostDraw;
import io.github._5thlayer.libworks.machine.client.MachineDraw;
import io.github._5thlayer.wireworks.ReactorMenu;
import io.github._5thlayer.wireworks.ReactorSlots;
import io.github._5thlayer.wireworks.ReactorSpec;
import io.github._5thlayer.wireworks.SteamFluids;
import io.github._5thlayer.wireworks.WireworksRegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * The Reactor's screen, the Boiler's look on libworks-runtime's {@link MachineDraw}: down the left, the
 * fuel slot beside the loaded cell's bar, the water slot beside the water gauge, and the steam gauge
 * below; the depleted slot and the bucket output on the right; and below them the status, which says
 * why a stopped Reactor stopped. Its empty fuel, water and depleted slots show ghosts of what goes in or
 * comes out, and EMI's recipe and usage keys work on those.
 */
public class ReactorScreen extends AbstractContainerScreen<ReactorMenu> implements GhostScreen {

    private static final int CELL = 0xFF5FBF3A;

    /** Each bar and gauge stands in its slot's row, a little right of it and two pixels down to centre on it. */
    private static final int GAUGE_X = 28;
    private static final int GAUGE_WIDTH = 100;
    private static final int GAUGE_DROP = 2;
    private static final int STATUS_Y = 78;

    /** What the empty fuel, water and depleted slots show, and what EMI's shortcuts ask of. */
    private final Ghosts ghosts = new Ghosts(() -> leftPos, () -> topPos);

    public ReactorScreen(ReactorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, ReactorMenu.INVENTORY_Y + 82);
        inventoryLabelY = ReactorMenu.INVENTORY_Y - 11;
        ghosts.add(menu.slots.get(ReactorSlots.FUEL));
        ghosts.add(menu.slots.get(ReactorSlots.WATER));
        ghosts.add(menu.slots.get(ReactorSlots.DEPLETED),
                () -> List.of(new ItemStack(WireworksRegistries.DEPLETED_FUEL_CELL.get())));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // The world dims behind the panel as it does behind the inventory.
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        MachineDraw.panel(graphics, font, leftPos, topPos, imageWidth, imageHeight, menu.slots, ghosts, Util.getMillis());
        int left = leftPos + GAUGE_X;
        MachineDraw.bar(graphics, font, left, topPos + gaugeY(ReactorMenu.FUEL_Y), GAUGE_WIDTH, MachineDraw.BAR_HEIGHT,
                menu.cellLevel(), CELL, cellLabel());
        MachineDraw.gauge(graphics, font, left, topPos + gaugeY(ReactorMenu.WATER_Y), GAUGE_WIDTH, water(), menu.waterCapacity());
        MachineDraw.gauge(graphics, font, left, topPos + gaugeY(ReactorMenu.STEAM_Y), GAUGE_WIDTH, steam(), menu.steamCapacity());
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        graphics.text(font, MachineDraw.fitted(font, menu.status().line(), statusWidth()), MachineDraw.MARGIN, STATUS_Y,
                MachineDraw.TEXT, false);
    }

    /** The slots' ghosts, which libworks-runtime offers to EMI as the hovered stack, not clickable. */
    @Override
    public Ghosts ghosts() {
        return ghosts;
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        Optional<Ghost> ghost = ghostAt(mouseX, mouseY);
        if (ghost.isPresent()) {
            GhostDraw.tooltip(graphics, font, ghost.get(), mouseX, mouseY);
        } else if (overGauge(ReactorMenu.FUEL_Y, mouseX, mouseY)) {
            graphics.setComponentTooltipForNextFrame(font, List.of(cellTooltip()), mouseX, mouseY);
        } else if (overGauge(ReactorMenu.WATER_Y, mouseX, mouseY)) {
            MachineDraw.fluidTooltip(graphics, font, Component.translatable("tooltip.wireworks.reactor.tank.water"),
                    water(), menu.waterCapacity(), mouseX, mouseY);
        } else if (overGauge(ReactorMenu.STEAM_Y, mouseX, mouseY)) {
            MachineDraw.fluidTooltip(graphics, font, Component.translatable("tooltip.wireworks.reactor.tank.steam"),
                    steam(), menu.steamCapacity(), mouseX, mouseY);
        } else if (MachineDraw.over(leftPos + MachineDraw.MARGIN, topPos + STATUS_Y, statusWidth(), font.lineHeight, mouseX, mouseY)) {
            MachineDraw.wholeOnHover(graphics, font, menu.status().line(), statusWidth(), mouseX, mouseY);
        }
    }

    private FluidStack water() {
        return new FluidStack(Fluids.WATER, menu.water());
    }

    private FluidStack steam() {
        return new FluidStack(SteamFluids.SUPERHEATED_STEAM_SOURCE.get(), menu.steam());
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

    /** The seconds of running the loaded cell has left; nothing on an empty bar, which the status line explains. */
    private Component cellLabel() {
        int ticks = menu.cellTicksLeft();
        return ticks <= 0
                ? Component.empty()
                : Component.translatable("tooltip.wireworks.reactor.cell", ticks / ReactorSpec.MINECRAFT_TICKS_PER_SECOND);
    }

    private Component cellTooltip() {
        int ticks = menu.cellTicksLeft();
        return ticks <= 0
                ? Component.translatable("tooltip.wireworks.reactor.no_cell").withStyle(ChatFormatting.GRAY)
                : Component.translatable("tooltip.wireworks.reactor.cell_ticks", ticks, ReactorSpec.CELL_TICKS);
    }
}

// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks.client;

import java.text.NumberFormat;
import java.util.List;
import java.util.Optional;

import io.github._5thlayer.libworks.ghost.Ghost;
import io.github._5thlayer.libworks.ghost.Ghosts;
import io.github._5thlayer.libworks.ghost.client.GhostDraw;
import io.github._5thlayer.wireworks.BoilerMenu;
import io.github._5thlayer.wireworks.BoilerSlots;
import io.github._5thlayer.wireworks.BoilerSpec;
import io.github._5thlayer.wireworks.SteamFluids;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * The Boiler's screen, a stand-in drawn from fills as Fieldworks' Harvester's is: down the left, each
 * input slot beside what it feeds, the fuel slot beside the fuel it has banked, the water slot beside
 * the water tank and the steam slot beside the steam tank; the output on the right; and below them the
 * status, which says why a stopped Boiler stopped. Its empty input slots show ghosts of what goes in
 * them, and EMI's recipe and usage keys work on those.
 */
public class BoilerScreen extends AbstractContainerScreen<BoilerMenu> {

    private static final int PANEL = 0xFFC6C6C6;
    private static final int SLOT = 0xFF8B8B8B;
    private static final int SLOT_DARK = 0xFF373737;
    private static final int SLOT_LIGHT = 0xFFFFFFFF;
    private static final int BURN = 0xFFD9822B;
    private static final int TEXT = 0xFF404040;
    private static final int WHITE = 0xFFFFFFFF;

    private static final int SLOT_SIZE = 16;
    private static final int BAR_HEIGHT = 12;
    /** The room a bar keeps either side of its label. */
    private static final int BAR_PADDING = 4;
    private static final int FLUID_TILE = 16;
    private static final int MARGIN = 8;

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
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL);
        for (Slot slot : menu.slots) {
            if (slot.isActive()) {
                recess(graphics, leftPos + slot.x, topPos + slot.y, SLOT_SIZE, SLOT_SIZE);
            }
        }
        // The slots' ghosts, over their recesses and under the real stacks and tooltips drawn after.
        for (Ghost ghost : ghosts.shown(Util.getMillis())) {
            GhostDraw.draw(graphics, font, ghost);
        }
        bar(graphics, gaugeY(BoilerMenu.FUEL_Y), menu.fuelLevel(), BURN, burnLabel());
        gauge(graphics, gaugeY(BoilerMenu.WATER_Y), Fluids.WATER, menu.water(), menu.waterCapacity());
        gauge(graphics, gaugeY(BoilerMenu.STEAM_Y), SteamFluids.STEAM_SOURCE.get(), menu.steam(), menu.steamCapacity());
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        graphics.text(font, menu.status().line(), MARGIN, STATUS_Y, TEXT, false);
    }

    /** The ghost stack under the mouse, with the top-left of its slot on screen, for EMI's recipe and usage keys. */
    public Optional<Ghost> ghostAt(double mouseX, double mouseY) {
        return ghosts.at(mouseX, mouseY, Util.getMillis());
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        int x = mouseX - leftPos;
        int y = mouseY - topPos;
        Optional<Ghost> ghost = ghostAt(mouseX, mouseY);
        if (ghost.isPresent()) {
            graphics.setTooltipForNextFrame(font, ghost.get().stack(), mouseX, mouseY);
        } else if (over(gaugeY(BoilerMenu.FUEL_Y), x, y)) {
            graphics.setComponentTooltipForNextFrame(font, fuelTooltip(), mouseX, mouseY);
        } else if (over(gaugeY(BoilerMenu.WATER_Y), x, y)) {
            graphics.setComponentTooltipForNextFrame(font, List.of(Component.translatable(
                    "tooltip.wireworks.boiler.water", menu.water(), menu.waterCapacity())), mouseX, mouseY);
        } else if (over(gaugeY(BoilerMenu.STEAM_Y), x, y)) {
            graphics.setComponentTooltipForNextFrame(font, List.of(Component.translatable(
                    "tooltip.wireworks.boiler.steam", menu.steam(), menu.steamCapacity())), mouseX, mouseY);
        }
    }

    private static int gaugeY(int slotY) {
        return slotY + GAUGE_DROP;
    }

    private static boolean over(int top, int x, int y) {
        return x >= GAUGE_X && x < GAUGE_X + GAUGE_WIDTH && y >= top && y < top + BAR_HEIGHT;
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

    /** A slot's bevel: dark above and left, light below and right, grey inside. */
    private static void recess(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        graphics.fill(x - 1, y - 1, x + width, y, SLOT_DARK);
        graphics.fill(x - 1, y, x, y + height, SLOT_DARK);
        graphics.fill(x, y + height, x + width + 1, y + height + 1, SLOT_LIGHT);
        graphics.fill(x + width, y, x + width + 1, y + height, SLOT_LIGHT);
        graphics.fill(x, y, x + width, y + height, SLOT);
    }

    /** A bar in its recess, {@code fraction} of it filled, with {@code label} written over it when it fits. */
    private void bar(GuiGraphicsExtractor graphics, int y, float fraction, int colour, Component label) {
        int left = leftPos + GAUGE_X;
        int top = topPos + y;
        recess(graphics, left, top, GAUGE_WIDTH, BAR_HEIGHT);
        graphics.fill(left, top, left + Math.round(GAUGE_WIDTH * fraction), top + BAR_HEIGHT, colour);
        if (font.width(label) <= GAUGE_WIDTH - BAR_PADDING) {
            graphics.text(font, label, left + (GAUGE_WIDTH - font.width(label)) / 2, top + 2, WHITE, true);
        }
    }

    /** A fluid gauge in its recess: the fluid's still texture, tinted as in the world, tiled over the part the tank fills. */
    private void gauge(GuiGraphicsExtractor graphics, int y, Fluid fluid, int amount, int capacity) {
        int left = leftPos + GAUGE_X;
        int top = topPos + y;
        recess(graphics, left, top, GAUGE_WIDTH, BAR_HEIGHT);
        if (amount > 0) {
            FluidModel model = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluid.defaultFluidState());
            int tint = model.fluidTintSource() == null ? WHITE : 0xFF000000 | model.fluidTintSource().color(fluid.defaultFluidState());
            int filled = Math.min(GAUGE_WIDTH, Math.round(GAUGE_WIDTH * (float) amount / Math.max(1, capacity)));
            graphics.enableScissor(left, top, left + filled, top + BAR_HEIGHT);
            for (int tileX = left; tileX < left + filled; tileX += FLUID_TILE) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, model.stillMaterial().sprite(), tileX, top, FLUID_TILE, FLUID_TILE, tint);
            }
            graphics.disableScissor();
        }
        Component label = Component.literal(NumberFormat.getIntegerInstance().format(amount) + " mB");
        graphics.text(font, label, left + 2, top + 2, WHITE, true);
    }
}

// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import java.util.Locale;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * Whether the Boiler boiled on its last tick, and if not, why not, in the order it asks: water, room
 * for the steam, then fuel, which it cannot light while the output has no room for what the fuel would
 * leave. Its screen and Jade both name it, in the same words.
 */
public enum BoilerState {
    BOILING,
    NO_WATER,
    STEAM_BACKED_UP,
    OUTPUT_FULL,
    NO_FUEL;

    /** "Boiling" in green, or why not in red. */
    public Component line() {
        return Component.translatable("tooltip.wireworks.boiler.state." + name().toLowerCase(Locale.ROOT))
                .withStyle(this == BOILING ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    /** The state a menu or Jade was sent, or {@link #NO_FUEL} for a number it does not know. */
    public static BoilerState of(int ordinal) {
        BoilerState[] states = values();
        return ordinal >= 0 && ordinal < states.length ? states[ordinal] : NO_FUEL;
    }
}

// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import java.util.Locale;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * Whether the Reactor ran on its last tick, and if not, why not, in the order {@link ReactorCycle} asks:
 * water, which a bucket waiting on a full bucket output does not reach, room for the steam, room in the
 * depleted slot, then fuel. Its screen and Jade both name it, in the same words.
 */
public enum ReactorStatus {
    RUNNING,
    NO_WATER,
    BUCKETS_FULL,
    STEAM_BACKED_UP,
    DEPLETED_FULL,
    NO_FUEL;

    /** "Running" in green, or why not in red. */
    public Component line() {
        return Component.translatable("tooltip.wireworks.reactor.status." + name().toLowerCase(Locale.ROOT))
                .withStyle(this == RUNNING ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    /** The status a menu or Jade was sent, or {@link #NO_FUEL} for a number it does not know. */
    public static ReactorStatus of(int ordinal) {
        ReactorStatus[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : NO_FUEL;
    }
}

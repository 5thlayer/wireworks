// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * Which way steam crosses a generator's row (ADR-0011): it goes on out of the end it did not come in
 * at. A pull leaves that end alone, and draws from one supply a tick, since steam coming in at a second
 * end would turn the way back at the first.
 *
 * <p>Pure: generic over the side, a {@code Direction} in the game.
 *
 * @param <S> a side of the row
 */
final class SteamRow<S> {

    private @Nullable S passOn;

    /** The end the steam goes on out of, or null while none has come in. */
    @Nullable S passOn() {
        return passOn;
    }

    /** Sets the way on, as a load restores it. */
    void restore(@Nullable S end) {
        passOn = end;
    }

    /** Steam came in at the end opposite {@code end}, so it goes on out of {@code end}. */
    void passesOnAt(S end) {
        passOn = end;
    }

    /** Starts one tick's pull. */
    Pull pull() {
        return new Pull(passOn);
    }

    /** One tick's pull, which remembers the way on as the tick began. */
    final class Pull {

        private final @Nullable S before;

        private Pull(@Nullable S before) {
            this.before = before;
        }

        /**
         * Whether the pull may draw at {@code end}: not the end the steam goes out of, and nowhere once a
         * supply this tick has turned the way.
         */
        boolean drawsAt(S end) {
            return Objects.equals(passOn, before) && !end.equals(passOn);
        }
    }
}

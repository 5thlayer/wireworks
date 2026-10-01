// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import io.github._5thlayer.groundworks.Refusal;

/** Why a pole would not place. */
public enum WireworksRefusal implements Refusal {
    /** A pole aimed at a column of another tier. */
    OTHER_TIER,
    /** A pole column already at {@link PoleColumn#MAX_SEGMENTS}. */
    COLUMN_FULL,
    /** A pole column whose next segment's position is occupied. */
    BLOCKED_TOP,
}

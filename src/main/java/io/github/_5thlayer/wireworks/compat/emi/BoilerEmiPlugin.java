// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks.compat.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.EmiStackInteraction;
import io.github._5thlayer.wireworks.client.BoilerScreen;

/**
 * The Boiler screen's ghosts in EMI: the recipe and usage keys work on the faded stack in an empty
 * input slot, as on a real one, as on Fieldworks' Harvester's.
 *
 * <p>EMI finds this by its annotation and loads it only when EMI is installed; nothing outside this
 * package names an EMI type, so the Library loads without it.
 */
@EmiEntrypoint
public final class BoilerEmiPlugin implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        registry.addStackProvider(BoilerScreen.class, (screen, x, y) -> screen.ghostAt(x, y)
                .map(ghost -> new EmiStackInteraction(EmiStack.of(ghost.stack()))).orElse(EmiStackInteraction.EMPTY));
    }
}

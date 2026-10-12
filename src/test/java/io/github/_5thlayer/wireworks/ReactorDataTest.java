// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * What the Reactor ships as data, read from the source tree (#40): the two tags' defaults, the three
 * recipes as the ticket gives them, and that every Wireworks item or tag they name exists. That they
 * load and craft in a world is the game tests'.
 */
class ReactorDataTest {

    private static final Path DATA = Path.of("src/main/resources/data/wireworks");
    private static final Path ITEMS = Path.of("src/main/resources/assets/wireworks/items");

    /** A Wireworks item, or with a leading {@code #} a Wireworks tag, as a recipe names it. */
    private static final Pattern NAMED = Pattern.compile("\"(#?)wireworks:([a-z_/]+)\"");

    private static JsonObject read(String path) throws IOException {
        return JsonParser.parseString(Files.readString(DATA.resolve(path))).getAsJsonObject();
    }

    /** A tag's entries, a {@code required: false} one marked with a trailing {@code ?}. */
    private static List<String> entries(String tag) throws IOException {
        List<String> entries = new ArrayList<>();
        for (JsonElement value : read("tags/item/" + tag + ".json").getAsJsonArray("values")) {
            if (value.isJsonObject()) {
                JsonObject entry = value.getAsJsonObject();
                boolean required = !entry.has("required") || entry.get("required").getAsBoolean();
                entries.add(entry.get("id").getAsString() + (required ? "" : "?"));
            } else {
                entries.add(value.getAsString());
            }
        }
        return entries;
    }

    private static Map<String, String> key(JsonObject recipe) {
        Map<String, String> key = new TreeMap<>();
        recipe.getAsJsonObject("key").entrySet().forEach(entry -> key.put(entry.getKey(), entry.getValue().getAsString()));
        return key;
    }

    private static List<String> pattern(JsonObject recipe) {
        return recipe.getAsJsonArray("pattern").asList().stream().map(JsonElement::getAsString).toList();
    }

    private static void expectResult(JsonObject recipe, String item) {
        JsonObject result = recipe.getAsJsonObject("result");
        assertEquals(item, result.get("id").getAsString());
        assertEquals(1, result.has("count") ? result.get("count").getAsInt() : 1);
    }

    @Test
    @DisplayName("Fission Fuel is the Fuel Cell by default")
    void fissionFuel() throws IOException {
        assertEquals(List.of("wireworks:fuel_cell"), entries("fission_fuel"));
    }

    @Test
    @DisplayName("a Fissile is glowstone dust, and a uranium ingot when a mod fills c:ingots/uranium")
    void fissile() throws IOException {
        assertEquals(List.of("#c:ingots/uranium?", "minecraft:glowstone_dust"), entries("fissile"));
    }

    @Test
    @DisplayName("the Reactor is crafted from iron blocks, plastic, netherite blocks and a blast furnace, and no battery")
    void reactorRecipe() throws IOException {
        JsonObject recipe = read("recipe/reactor.json");
        assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString());
        assertEquals(List.of("IPI", "NFN", "IPI"), pattern(recipe));
        assertEquals(Map.of("I", "#c:storage_blocks/iron", "P", "wireworks:plastic",
                "N", "#c:storage_blocks/netherite", "F", "minecraft:blast_furnace"), key(recipe));
        assertFalse(recipe.toString().contains("battery"), "the Reactor's recipe takes a battery");
        expectResult(recipe, "wireworks:reactor");
    }

    @Test
    @DisplayName("a Fuel Cell is crafted from eight iron ingots round a Fissile")
    void fuelCellRecipe() throws IOException {
        JsonObject recipe = read("recipe/fuel_cell.json");
        assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString());
        assertEquals(List.of("III", "IFI", "III"), pattern(recipe));
        assertEquals(Map.of("I", "#c:ingots/iron", "F", "#wireworks:fissile"), key(recipe));
        expectResult(recipe, "wireworks:fuel_cell");
    }

    @Test
    @DisplayName("a Depleted Fuel Cell and a Fissile refill one Fuel Cell")
    void refillRecipe() throws IOException {
        JsonObject recipe = read("recipe/fuel_cell_from_depleted_fuel_cell.json");
        assertEquals("minecraft:crafting_shapeless", recipe.get("type").getAsString());
        List<String> ingredients = recipe.getAsJsonArray("ingredients").asList().stream().map(JsonElement::getAsString).toList();
        assertEquals(List.of("wireworks:depleted_fuel_cell", "#wireworks:fissile"), ingredients);
        expectResult(recipe, "wireworks:fuel_cell");
    }

    @Test
    @DisplayName("every Wireworks item and tag the three recipes name is one the Library ships")
    void referencesResolve() throws IOException {
        for (String file : List.of("reactor", "fuel_cell", "fuel_cell_from_depleted_fuel_cell")) {
            String json = Files.readString(DATA.resolve("recipe/" + file + ".json"));
            Matcher named = NAMED.matcher(json);
            while (named.find()) {
                Path shipped = named.group(1).isEmpty()
                        ? ITEMS.resolve(named.group(2) + ".json")
                        : DATA.resolve("tags/item/" + named.group(2) + ".json");
                assertTrue(Files.isRegularFile(shipped), file + " names " + named.group() + ", which the Library does not ship");
            }
        }
    }
}

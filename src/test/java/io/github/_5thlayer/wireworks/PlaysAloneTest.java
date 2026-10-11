// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The mod plays alone on Groundworks (ADR-0127): nothing it ships names another suite mod's id or
 * puts files in its namespace, and its recipes consume no tag a vanilla item cannot fill. The tags its
 * code reads, and every {@code c:} tag, are resolved for real by the game tests in {@code PlaysAloneTests}.
 */
class PlaysAloneTest {

    private static final Path SOURCE = Path.of("src/main/java");
    private static final Path RESOURCES = Path.of("src/main/resources");

    @Test
    void theShippedFilesShipNothingInAnotherSuiteModsNamespace() throws IOException {
        assertEquals(List.of(), PlaysAlone.foreignNamespaces(RESOURCES, Wireworks.MOD_ID));
    }

    @Test
    void theShippedFilesNameNoOtherSuiteModsId() throws IOException {
        assertEquals(List.of(), PlaysAlone.foreignIds(List.of(SOURCE, RESOURCES), Wireworks.MOD_ID));
    }

    @Test
    void theShippedRecipesConsumeOnlyTagsWithAVanillaMember() throws IOException {
        assertEquals(List.of(), PlaysAlone.recipeTagsWithoutAVanillaMember(RESOURCES.resolve("data"), Wireworks.MOD_ID));
    }

    @Test
    void aForeignNamespaceFolderIsReported(@TempDir Path resources) throws IOException {
        write(resources.resolve("data/fieldworks/recipe/x.json"), "{}");
        write(resources.resolve("assets/beltworks/lang/en_us.json"), "{}");
        assertEquals(List.of("assets/beltworks", "data/fieldworks"), PlaysAlone.foreignNamespaces(resources, Wireworks.MOD_ID));
    }

    @Test
    void anIdInDataIsReported(@TempDir Path root) throws IOException {
        write(root.resolve("data/wireworks/recipe/x.json"), "{\"result\":{\"id\":\"factoryworks:crude_oil\"}}");
        assertEquals(List.of("data/wireworks/recipe/x.json names factoryworks:crude_oil"),
                PlaysAlone.foreignIds(List.of(root), Wireworks.MOD_ID));
    }

    @Test
    void aTagInDataIsReported(@TempDir Path root) throws IOException {
        write(root.resolve("data/wireworks/recipe/x.json"), "{\"key\":{\"a\":\"#factoryworks:belts\"}}");
        assertEquals(List.of("data/wireworks/recipe/x.json names factoryworks:belts"),
                PlaysAlone.foreignIds(List.of(root), Wireworks.MOD_ID));
    }

    @Test
    void anIdInJavaSourceIsReported(@TempDir Path root) throws IOException {
        write(root.resolve("io/x/Hook.java"), "class Hook { String id = \"craftworks:assembler\"; }");
        assertEquals(List.of("io/x/Hook.java names craftworks:assembler"), PlaysAlone.foreignIds(List.of(root), Wireworks.MOD_ID));
    }

    @Test
    void anIdSavedInAStructureIsReported(@TempDir Path root) throws IOException {
        Path nbt = root.resolve("data/wireworks/structure/x.nbt");
        Files.createDirectories(nbt.getParent());
        try (OutputStream out = new GZIPOutputStream(Files.newOutputStream(nbt))) {
            out.write("\u0008\u0000\u0004Name\u0000\u0015fieldworks:iron_patch".getBytes(StandardCharsets.ISO_8859_1));
        }
        assertEquals(List.of("data/wireworks/structure/x.nbt names fieldworks:iron_patch"),
                PlaysAlone.foreignIds(List.of(root), Wireworks.MOD_ID));
    }

    @Test
    void theModsOwnIdsAndTheSharedNamespacesAreAllowed(@TempDir Path root) throws IOException {
        write(root.resolve("META-INF/neoforge.mods.toml"), "modId=\"wireworks\"\n[[dependencies.wireworks]]\nmodId=\"groundworks\"");
        write(root.resolve("io/x/Hook.java"), "String id = \"wireworks:small_pole\";");
        write(root.resolve("data/groundworks/tags/item/dismantles.json"), "{}");
        write(root.resolve("data/wireworks/recipe/x.json"),
                "{\"a\":\"minecraft:glass\",\"b\":\"#c:glass_blocks\",\"c\":\"groundworks:x\",\"d\":\"neoforge:y\"}");
        assertEquals(List.of(), PlaysAlone.foreignNamespaces(root, Wireworks.MOD_ID));
        assertEquals(List.of(), PlaysAlone.foreignIds(List.of(root), Wireworks.MOD_ID));
    }

    @Test
    void anOwnTagWithoutAVanillaMemberIsReported(@TempDir Path data) throws IOException {
        write(data.resolve("wireworks/tags/item/rods.json"), "{\"values\":[\"wireworks:rod\"]}");
        write(data.resolve("wireworks/recipe/x.json"), "{\"key\":{\"a\":\"#wireworks:rods\"}}");
        assertEquals(List.of("wireworks/recipe/x.json consumes wireworks:rods, which has no vanilla member"),
                PlaysAlone.recipeTagsWithoutAVanillaMember(data, Wireworks.MOD_ID));
    }

    @Test
    void anOwnTagReachesAVanillaMemberThroughAnotherTag(@TempDir Path data) throws IOException {
        write(data.resolve("wireworks/tags/item/outer.json"), "{\"values\":[\"#wireworks:inner\"]}");
        write(data.resolve("wireworks/tags/item/inner.json"), "{\"values\":[\"minecraft:stick\"]}");
        write(data.resolve("wireworks/recipe/x.json"), "{\"key\":{\"a\":\"#wireworks:outer\"}}");
        assertEquals(List.of(), PlaysAlone.recipeTagsWithoutAVanillaMember(data, Wireworks.MOD_ID));
    }

    @Test
    void aCommonTagIsLeftToTheGameTest(@TempDir Path data) throws IOException {
        write(data.resolve("wireworks/recipe/x.json"), "{\"key\":{\"a\":{\"tag\":\"c:gems/ruby\"}}}");
        assertEquals(List.of(), PlaysAlone.recipeTagsWithoutAVanillaMember(data, Wireworks.MOD_ID));
    }

    private static void write(Path file, String text) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, text);
    }
}

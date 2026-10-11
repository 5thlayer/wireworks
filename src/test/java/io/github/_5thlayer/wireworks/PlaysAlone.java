// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

/** Scans what a mod ships for what stops it playing alone on Groundworks (ADR-0127), one rule per method. */
final class PlaysAlone {

    private static final Set<String> SUITE = Set.of("beltworks", "pipeworks", "wireworks", "craftworks", "fieldworks", "factoryworks");

    private static final Pattern ID = Pattern.compile("\\b(" + String.join("|", SUITE) + "):[a-z0-9_./-]+");
    private static final Pattern STRING_ID = Pattern.compile("\"(#?)([a-z0-9_.-]+):([a-z0-9_./-]+)\"");
    private static final Pattern TAG_FIELD = Pattern.compile("\"tag\"\\s*:\\s*\"([a-z0-9_.-]+:[a-z0-9_./-]+)\"");

    private PlaysAlone() {
    }

    /** Each {@code data/} or {@code assets/} folder under the resources in another suite mod's namespace. */
    static List<String> foreignNamespaces(Path resources, String own) throws IOException {
        List<String> found = new ArrayList<>();
        for (String kind : List.of("assets", "data")) {
            Path dir = resources.resolve(kind);
            if (!Files.isDirectory(dir)) {
                continue;
            }
            try (Stream<Path> namespaces = Files.list(dir)) {
                namespaces.map(p -> p.getFileName().toString())
                        .filter(ns -> SUITE.contains(ns) && !ns.equals(own))
                        .sorted()
                        .forEach(ns -> found.add(kind + "/" + ns));
            }
        }
        return found;
    }

    /** Every literal id in another suite mod's namespace, in any file under the roots. */
    static List<String> foreignIds(List<Path> roots, String own) throws IOException {
        List<String> found = new ArrayList<>();
        for (Path root : roots) {
            try (Stream<Path> files = Files.walk(root)) {
                for (Path file : files.filter(Files::isRegularFile).sorted().toList()) {
                    Matcher ids = ID.matcher(text(file));
                    while (ids.find()) {
                        if (!ids.group(1).equals(own)) {
                            found.add(root.relativize(file).toString().replace('\\', '/') + " names " + ids.group());
                        }
                    }
                }
            }
        }
        return found;
    }

    /**
     * Each tag a recipe consumes that reaches no vanilla item through the mod's own item tags. A {@code c:}
     * tag is left to the game test that resolves every ingredient, since only the game knows what NeoForge
     * puts in it.
     */
    static List<String> recipeTagsWithoutAVanillaMember(Path data, String own) throws IOException {
        List<String> found = new ArrayList<>();
        Path recipes = data.resolve(own).resolve("recipe");
        if (!Files.isDirectory(recipes)) {
            return found;
        }
        try (Stream<Path> files = Files.walk(recipes)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".json")).sorted().toList()) {
                for (String tag : consumedTags(Files.readString(file))) {
                    if (!tag.startsWith("c:") && !hasVanillaMember(data, tag, own, new HashSet<>())) {
                        found.add(data.relativize(file).toString().replace('\\', '/') + " consumes " + tag
                                + ", which has no vanilla member");
                    }
                }
            }
        }
        return found;
    }

    private static Set<String> consumedTags(String recipe) {
        Set<String> tags = new HashSet<>();
        Matcher ids = STRING_ID.matcher(recipe);
        while (ids.find()) {
            if (!ids.group(1).isEmpty()) {
                tags.add(ids.group(2) + ":" + ids.group(3));
            }
        }
        Matcher fields = TAG_FIELD.matcher(recipe);
        while (fields.find()) {
            tags.add(fields.group(1));
        }
        return tags;
    }

    private static boolean hasVanillaMember(Path data, String tag, String own, Set<String> seen) {
        String ns = tag.substring(0, tag.indexOf(':'));
        String path = tag.substring(tag.indexOf(':') + 1);
        if (ns.equals("minecraft")) {
            return true;
        }
        if (!ns.equals(own) || !seen.add(tag)) {
            return false;
        }
        Path file = data.resolve(ns).resolve("tags/item").resolve(path + ".json");
        if (!Files.isRegularFile(file)) {
            return false;
        }
        try {
            Matcher members = STRING_ID.matcher(Files.readString(file));
            while (members.find()) {
                String member = members.group(2) + ":" + members.group(3);
                if (members.group(1).isEmpty() ? members.group(2).equals("minecraft") : hasVanillaMember(data, member, own, seen)) {
                    return true;
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return false;
    }

    private static String text(Path file) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        if (file.toString().endsWith(".nbt")) {
            try (InputStream in = new GZIPInputStream(Files.newInputStream(file))) {
                bytes = in.readAllBytes();
            }
        }
        // Latin-1 maps every byte to a char, so a binary file still reads and an id in it still matches.
        return new String(bytes, StandardCharsets.ISO_8859_1);
    }
}

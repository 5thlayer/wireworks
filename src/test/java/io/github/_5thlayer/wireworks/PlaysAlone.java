// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.wireworks;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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

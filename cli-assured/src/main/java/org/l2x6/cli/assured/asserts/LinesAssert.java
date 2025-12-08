/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured.asserts;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

class LinesAssert<T> implements LineAssert {

    private final Collection<T> checks;
    private final Collection<T> hits;
    private final BiConsumer<String, Collection<T>> lineConsumer;

    LinesAssert(Collection<T> checks, Collection<T> hits, BiConsumer<String, Collection<T>> lineConsumer) {
        this.checks = Objects.requireNonNull(checks, "checks");
        this.hits = Objects.requireNonNull(hits, "hits");
        this.lineConsumer = lineConsumer;
    }

    static LineAssert containsMatching(Map<String, Pattern> checks) {
        return new LinesAssert<Pattern>(
                Collections.unmodifiableMap(checks).values(),
                new LinkedHashMap<>(checks).values(),
                (line, hits) -> {
                    for (Iterator<Pattern> i = hits.iterator(); i.hasNext();) {
                        Pattern p = i.next();
                        if (p.matcher(line).find()) {
                            i.remove();
                        }
                    }
                });
    }

    static LineAssert doesNotContainMatching(Map<String, Pattern> checks) {
        final Collection<Pattern> checksCopy = Collections.unmodifiableMap(checks).values();
        return new LinesAssert<Pattern>(
                checksCopy,
                new LinkedHashSet<>(),
                (line, hits) -> {
                    for (Iterator<Pattern> i = checksCopy.iterator(); i.hasNext();) {
                        Pattern p = i.next();
                        if (p.matcher(line).find()) {
                            hits.add(p);
                        }
                    }
                });
    }

    static Map<String, Pattern> toMap(Collection<Pattern> expectedPatterns) {
        Map<String, Pattern> pats = new LinkedHashMap<>();
        expectedPatterns.stream()
                .forEach(p -> pats.put(p.pattern(), p));
        return pats;
    }

    static Map<String, Pattern> toCompiledMap(Collection<String> expectedPatterns) {
        Map<String, Pattern> pats = new LinkedHashMap<>();
        expectedPatterns.stream()
                .map(Pattern::compile)
                .forEach(p -> pats.put(p.pattern(), p));
        return pats;
    }

    @Override
    public void assertSatisfied() {
        synchronized (hits) {
            if (!hits.isEmpty()) {
                throw new AssertionError("Expected lines " + checks + " to occur in any order, but lines "
                        + hits + " did not occur");
            }
        }
    }

    @Override
    public void line(String line) {
        synchronized (hits) {
            lineConsumer.accept(line, hits);
        }
    }

}

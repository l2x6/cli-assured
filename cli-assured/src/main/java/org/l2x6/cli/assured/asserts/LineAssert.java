/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured.asserts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * An assertion on a line of a command output.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 * @since  0.0.1
 */
public interface LineAssert extends Assert {

    /**
     * Assert that the given actual {@code line} fulfills the expectations
     *
     * @param line
     * @since      0.0.1
     */
    LineAssert line(String line);

    /**
     * Assert that the given lines are present in the underlying output stream among other lines in any order.
     *
     * @param  lines the whole lines to look for
     * @return       a new {@link LineAssert}
     * @since        0.0.1
     */
    static LineAssert contains(Collection<String> lines) {
        return new LinesAssert<String, String>(
                Collections.unmodifiableList(new ArrayList<>(lines)),
                new LinkedHashSet<>(lines),
                (line, hits) -> hits.remove(line),
                "Expected lines\n\n    %s\n\nto occur in any order, but lines\n\n    %s\n\ndid not occur");
    }

    /**
     * Assert that the given lines are not present in the underlying output stream.
     *
     * @param  lines the whole lines to look for
     * @return       a new {@link LineAssert}
     * @since        0.0.1
     */
    static LineAssert doesNotContain(Collection<String> lines) {
        final Set<String> checks = Collections.unmodifiableSet(new LinkedHashSet<>(lines));
        return new LinesAssert<String, String>(
                checks,
                new LinkedHashSet<>(),
                (line, hits) -> {
                    if (checks.contains(line)) {
                        hits.add(line);
                    }
                },
                "Expected none of the lines\n\n    %s\n\nto occur, but the following lines occurred:\n\n    %s\n\n");
    }

    /**
     * Assert that lines containing the given {@code substrings} are present in the underlying output stream among other
     * lines in any order.
     *
     * @param  substrings the substrings to look for in the associated output stream
     * @return            a new {@link LineAssert}
     * @since             0.0.1
     */
    static LineAssert containsSubstrings(Collection<String> substrings) {
        return new LinesAssert<String, String>(
                Collections.unmodifiableList(new ArrayList<>(substrings)),
                new LinkedHashSet<>(substrings),
                (line, hits) -> {
                    for (Iterator<String> i = hits.iterator(); i.hasNext();) {
                        String substr = i.next();
                        if (line.contains(substr)) {
                            i.remove();
                        }
                    }
                },
                "Expected lines containing\n\n    %s\n\nto occur, but the following substrings did not occur:\n\n    %s\n\n");
    }

    /**
     * Assert that lines containing the given {@code substrings} are not present in the underlying output stream.
     *
     * @param  substrings the substrings to look for in the associated output stream
     * @return            a new {@link LineAssert}
     * @since             0.0.1
     */
    static LineAssert doesNotContainSubstrings(Collection<String> unexpectedSubstrings) {
        final List<String> checks = Collections.unmodifiableList(new ArrayList<>(unexpectedSubstrings));
        return new LinesAssert<String, String>(
                checks,
                new LinkedHashSet<>(),
                (line, hits) -> {
                    for (Iterator<String> i = checks.iterator(); i.hasNext();) {
                        String substr = i.next();
                        if (line.contains(substr)) {
                            hits.add(line);
                        }
                    }
                },
                "Expected no lines containing\n\n    %s\n\nto occur, but some of the substrings occur in lines\n\n    %s\n\n");
    }

    /**
     * Assert that lines matching the given regular expressions are present in the underlying output stream among other
     * lines in any order.
     * The regular expression is evaluated using {@link Matcher#find()} rather than {@link Matcher#matches()}
     *
     * @param  regex the regular expressions to look for in the associated output stream
     * @return       a new {@link LineAssert}
     * @since        0.0.1
     */
    static LineAssert containsMatchingPatterns(Collection<Pattern> regex) {
        return LinesAssert.containsMatching(regex);
    }

    /**
     * Assert that lines matching the given regular expressions are present in the underlying output stream among other
     * lines in any order.
     * The regular expression is evaluated using {@link Matcher#find()} rather than {@link Matcher#matches()}
     *
     * @param  regex the regular expressions to look for in the associated output stream
     * @return       a new {@link LineAssert}
     * @since        0.0.1
     */
    static LineAssert containsMatching(Collection<String> regex) {
        return LinesAssert.containsMatching(LinesAssert.compile(regex));
    }

    /**
     * Assert that lines matching the given regular expressions are not present in the underlying output stream.
     * The regular expression is evaluated using {@link Matcher#find()} rather than {@link Matcher#matches()}
     *
     * @param  regex the regular expressions to look for in the associated output stream
     * @return       a new {@link LineAssert}
     * @since        0.0.1
     */
    static LineAssert doesNotContainMatchingPatterns(Collection<Pattern> regex) {
        return LinesAssert.doesNotContainMatching(regex);
    }

    /**
     * Assert that lines matching the given regular expressions are not present in the underlying output stream.
     * The regular expression is evaluated using {@link Matcher#find()} rather than {@link Matcher#matches()}
     *
     * @param  regex the regular expressions to look for in the associated output stream
     * @return       a new {@link LineAssert}
     * @since        0.0.1
     */
    static LineAssert doesNotContainMatching(Collection<String> regex) {
        return LinesAssert.doesNotContainMatching(LinesAssert.compile(regex));
    }

    /**
     * Assert that the underlying output stream has the given number of lines upon termination of the associated process.
     *
     * @param  expectedLineCount
     * @return                   a new {@link LineAssert}
     * @since                    0.0.1
     */
    static LineAssert hasCount(int expectedLineCount) {
        return new LineCountAssert(expectedLineCount);
    }

    static class LinesAssert<C, H> implements LineAssert {

        private final Collection<C> checks;
        private final Collection<H> hits;
        private final BiConsumer<String, Collection<H>> lineConsumer;
        private final String message;

        LinesAssert(
                Collection<C> checks,
                Collection<H> hits,
                BiConsumer<String, Collection<H>> lineConsumer,
                String message) {
            this.checks = Objects.requireNonNull(checks, "checks");
            this.hits = Objects.requireNonNull(hits, "hits");
            this.lineConsumer = lineConsumer;
            this.message = message;
        }

        static LineAssert containsMatching(Collection<Pattern> checks) {
            final Map<String, Pattern> hts = new LinkedHashMap<>();
            checks.forEach(p -> hts.put(p.pattern(), p));
            return new LinesAssert<Pattern, Pattern>(
                    checks,
                    hts.values(),
                    (line, hits) -> {
                        for (Iterator<Pattern> i = hits.iterator(); i.hasNext();) {
                            Pattern p = i.next();
                            if (p.matcher(line).find()) {
                                i.remove();
                            }
                        }
                    },
                    "Expected lines matching\n\n    %s\n\nto occur, but the following patterns did not match:\n\n    %s\n\n");
        }

        static LineAssert doesNotContainMatching(Collection<Pattern> checks) {
            return new LinesAssert<Pattern, String>(
                    checks,
                    new LinkedHashSet<>(),
                    (line, hits) -> {
                        for (Iterator<Pattern> i = checks.iterator(); i.hasNext();) {
                            Pattern p = i.next();
                            if (p.matcher(line).find()) {
                                hits.add(line);
                            }
                        }
                    },
                    "Expected no lines matching\n\n    %s\n\nto occur, but some of the patterns matched the lines\n\n    %s\n\n");
        }

        static Map<String, Pattern> toMap(Collection<Pattern> expectedPatterns) {
            Map<String, Pattern> pats = new LinkedHashMap<>();
            expectedPatterns.stream()
                    .forEach(p -> pats.put(p.pattern(), p));
            return pats;
        }

        static List<Pattern> compile(Collection<String> expectedPatterns) {
            return Collections.unmodifiableList(expectedPatterns.stream()
                    .map(Pattern::compile)
                    .collect(Collectors.toList()));
        }

        @Override
        public void assertSatisfied() {
            synchronized (hits) {
                if (!hits.isEmpty()) {
                    throw new AssertionError(String.format(message, list(checks), list(hits)));
                }
            }
        }

        String list(Collection<? extends Object> list) {
            return list.stream().map(Object::toString).collect(Collectors.joining("\n    "));
        }

        @Override
        public LinesAssert<C, H> line(String line) {
            synchronized (hits) {
                lineConsumer.accept(line, hits);
            }
            return this;
        }

    }

}

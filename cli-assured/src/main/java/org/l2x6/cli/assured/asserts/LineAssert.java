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
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.LongPredicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.l2x6.cli.assured.StreamExpectationsSpec;
import org.l2x6.cli.assured.asserts.Assert.Internal.ExcludeFromJacocoGeneratedReport;

/**
 * An assertion on a sequence of lines of a command output.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 * @since  0.0.1
 */
public interface LineAssert extends Assert {

    /**
     * Check the actual output {@code line}.
     * Any assertion failures should be reported via {@link #evaluate()} rather than by throwing an exception from this
     * method.
     *
     * @param line the line text without any trailing newline character to check
     * @since      0.0.1
     */
    LineAssert line(String line);

    /**
     * Assert that the given lines are present in the underlying output stream among other lines in any order.
     *
     * @param  stream the output stream to watch
     * @param  lines  the whole lines to look for
     * @return        a new {@link LineAssert}
     * @since         0.0.1
     */
    static LineAssert hasLines(StreamExpectationsSpec.ProcessOutput stream, Collection<String> lines) {
        return new Internal.LinesAssert<String, String>(
                Collections.unmodifiableList(new ArrayList<>(lines)),
                new LinkedHashSet<>(lines),
                (line, hits) -> hits.remove(line),
                "Expected lines\n\n    ${checks}\n\nto occur in ${stream} in any order, but lines\n\n    ${hits}\n\ndid not occur",
                stream);
    }

    /**
     * Assert that the given lines are not present in the underlying output stream.
     *
     * @param  stream the output stream to watch
     * @param  lines  the whole lines to look for
     * @return        a new {@link LineAssert}
     * @since         0.0.1
     */
    static LineAssert doesNotHaveLines(StreamExpectationsSpec.ProcessOutput stream, Collection<String> lines) {
        final Set<String> checks = Collections.unmodifiableSet(new LinkedHashSet<>(lines));
        return new Internal.LinesAssert<String, String>(
                checks,
                new LinkedHashSet<>(),
                (line, hits) -> {
                    if (checks.contains(line)) {
                        hits.add(line);
                    }
                },
                "Expected none of the lines\n\n    ${checks}\n\nto occur in ${stream}, but the following lines occurred:\n\n    ${hits}\n\n",
                stream);
    }

    /**
     * Assert that no content at all is produced by the associated output stream.
     *
     * @param  stream the output stream to watch
     *
     * @return        a new {@link LineAssert}
     * @since         0.0.1
     */
    static LineAssert doesNotHaveAnyLines(StreamExpectationsSpec.ProcessOutput stream) {
        return new Internal.LinesAssert<String, String>(
                Collections.emptyList(),
                new ArrayList<>(),
                (line, hits) -> {
                    hits.add(line);
                },
                "Expected no content to occur in ${stream}, but the following occurred:\n\n    ${hits}\n\n",
                stream);
    }

    /**
     * Assert that lines containing the given {@code substrings} are present in the underlying output stream among other
     * lines in any order.
     *
     * @param  stream     the output stream to watch
     * @param  substrings the substrings to look for in the associated output stream
     * @return            a new {@link LineAssert}
     * @since             0.0.1
     */
    static LineAssert hasLinesContaining(StreamExpectationsSpec.ProcessOutput stream, Collection<String> substrings) {
        return new Internal.LinesAssert<String, String>(
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
                "Expected lines containing\n\n    ${checks}\n\nto occur in ${stream}, but the following substrings did not occur:\n\n    ${hits}\n\n",
                stream);
    }

    /**
     * Assert that lines containing the given {@code substrings} are not present in the underlying output stream.
     *
     * @param  stream     the output stream to watch
     * @param  substrings the substrings to look for in the associated output stream
     * @return            a new {@link LineAssert}
     * @since             0.0.1
     */
    static LineAssert doesNotHaveLinesContaining(StreamExpectationsSpec.ProcessOutput stream, Collection<String> substrings) {
        final List<String> checks = Collections.unmodifiableList(new ArrayList<>(substrings));
        return new Internal.LinesAssert<String, String>(
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
                "Expected no lines containing\n\n    ${checks}\n\nto occur in ${stream}, but some of the substrings occur in lines\n\n    ${hits}\n\n",
                stream);
    }

    /**
     * Assert that lines containing the given {@code substrings} (using case insensitive comparison) are present in the
     * underlying output stream among other lines in any order.
     *
     * @param  stream     the output stream to watch
     * @param  substrings the substrings (must be in lower case already) to look for in the associated output stream
     * @return            a new {@link LineAssert}
     * @since             0.0.1
     */
    static LineAssert hasLinesContainingCaseInsensitive(StreamExpectationsSpec.ProcessOutput stream,
            Collection<String> substrings, Locale locale) {
        return new Internal.LinesAssert<String, String>(
                Collections.unmodifiableList(new ArrayList<>(substrings)),
                new LinkedHashSet<>(substrings),
                (line, hits) -> {
                    line = line.toLowerCase(locale);
                    for (Iterator<String> i = hits.iterator(); i.hasNext();) {
                        String substr = i.next();
                        if (line.contains(substr)) {
                            i.remove();
                        }
                    }
                },
                "Expected lines containing using case insensitive comparison\n\n    ${checks}\n\nto occur in ${stream}, but the following substrings did not occur:\n\n    ${hits}\n\n",
                stream);
    }

    /**
     * Assert that lines containing the given {@code substrings} (using case insensitive comparison) are not present in
     * the underlying output stream.
     *
     * @param  stream     the output stream to watch
     * @param  substrings the substrings (must be in lower case already) to look for in the associated output stream
     * @param  locale     the locale to use to transform to lower case
     * @return            a new {@link LineAssert}
     * @since             0.0.1
     */
    static LineAssert doesNotHaveLinesContainingCaseInsensitive(StreamExpectationsSpec.ProcessOutput stream,
            Collection<String> substrings, Locale locale) {
        final List<String> checks = Collections.unmodifiableList(new ArrayList<>(substrings));
        return new Internal.LinesAssert<String, String>(
                checks,
                new LinkedHashSet<>(),
                (line, hits) -> {
                    line = line.toLowerCase(locale);
                    for (Iterator<String> i = checks.iterator(); i.hasNext();) {
                        String substr = i.next();
                        if (line.contains(substr)) {
                            hits.add(line);
                        }
                    }
                },
                "Expected no lines containing using case insensitive comparison\n\n    ${checks}\n\nto occur in ${stream}, but some of the substrings occur in lines\n\n    ${hits}\n\n",
                stream);
    }

    /**
     * Assert that lines matching the given regular expressions are present in the underlying output stream among other
     * lines in any order.
     * The regular expression is evaluated using {@link Matcher#find()} rather than {@link Matcher#matches()}
     *
     * @param  stream the output stream to watch
     * @param  regex  the regular expressions to look for in the associated output stream
     * @param  locale the locale to use to transform to lower case
     * @return        a new {@link LineAssert}
     * @since         0.0.1
     */
    static LineAssert hasLinesMatchingPatterns(StreamExpectationsSpec.ProcessOutput stream, Collection<Pattern> regex) {
        return Internal.LinesAssert.containsMatching(stream, regex);
    }

    /**
     * Assert that lines matching the given regular expressions are present in the underlying output stream among other
     * lines in any order.
     * The regular expression is evaluated using {@link Matcher#find()} rather than {@link Matcher#matches()}
     *
     * @param  stream the output stream to watch
     * @param  regex  the regular expressions to look for in the associated output stream
     * @return        a new {@link LineAssert}
     * @since         0.0.1
     */
    static LineAssert hasLinesMatching(StreamExpectationsSpec.ProcessOutput stream, Collection<String> regex) {
        return Internal.LinesAssert.containsMatching(stream, Internal.LinesAssert.compile(regex));
    }

    /**
     * Assert that lines matching the given regular expressions are not present in the underlying output stream.
     * The regular expression is evaluated using {@link Matcher#find()} rather than {@link Matcher#matches()}
     *
     * @param  stream the output stream to watch
     * @param  regex  the regular expressions to look for in the associated output stream
     * @return        a new {@link LineAssert}
     * @since         0.0.1
     */
    static LineAssert doesNotHaveLinesMatchingPatterns(StreamExpectationsSpec.ProcessOutput stream, Collection<Pattern> regex) {
        return Internal.LinesAssert.doesNotContainMatching(stream, regex);
    }

    /**
     * Assert that lines matching the given regular expressions are not present in the underlying output stream.
     * The regular expression is evaluated using {@link Matcher#find()} rather than {@link Matcher#matches()}
     *
     * @param  stream the output stream to watch
     * @param  regex  the regular expressions to look for in the associated output stream
     * @return        a new {@link LineAssert}
     * @since         0.0.1
     */
    static LineAssert doesNotHaveLinesMatching(StreamExpectationsSpec.ProcessOutput stream, Collection<String> regex) {
        return Internal.LinesAssert.doesNotContainMatching(stream, Internal.LinesAssert.compile(regex));
    }

    /**
     * Assert that upon termination of the associated process, the underlying output stream has the given number of lines.
     *
     * @param  stream            the output stream to watch
     * @param  expectedLineCount
     * @return                   a new {@link LineAssert}
     * @since                    0.0.1
     */
    static LineAssert hasLineCount(StreamExpectationsSpec.ProcessOutput stream, long expectedLineCount) {
        return new Internal.LineCountAssert(actual -> actual == expectedLineCount,
                "Expected " + expectedLineCount + " lines in ${stream} but found ${actual} lines", stream);
    }

    /**
     * Assert that upon termination of the associated process, the underlying output stream's number of lines satisfies
     * the given {@link LongPredicate}.
     *
     * @param  stream      the output stream to watch
     * @param  expected    the condition the number of actual lines must satisfy
     * @param  description the description of a failure typically something like
     *                     {@code "Expected number of lines <condition> in ${stream} but found ${actual} lines"} where
     *                     {@code <condition>} is your human readable criteria, like {@code greater that 42},
     *                     <code>${stream}</code> is a placeholder that CLI Assured will replace by {@code stdout}
     *                     or {@code stderr} and <code>${actual}</code> is a placeholder that CLI Assured will replace
     *                     by the actual number of lines found in the associated output stream
     *
     * @return             a new {@link LineAssert}
     * @since              0.0.1
     */
    public static LineAssert hasLineCount(StreamExpectationsSpec.ProcessOutput stream, LongPredicate expected,
            String description) {
        return new Internal.LineCountAssert(expected, description, stream);
    }

    /**
     * Pass each line to the given {@link Consumer}. Handy for logging and other similar use cases.
     *
     * @param  stream   the output stream to watch
     * @param  consumer the consumer to which all lines should be passed
     * @return          a new {@link LineAssert}
     * @since           0.0.1
     */
    static LineAssert log(StreamExpectationsSpec.ProcessOutput stream, Consumer<String> consumer) {
        return new Internal.ConsumerLineAssert(consumer);
    }

    static final class Internal {

        @ExcludeFromJacocoGeneratedReport
        private Internal() {
        }

        static class ConsumerLineAssert implements LineAssert {
            private final Consumer<String> consumer;

            private ConsumerLineAssert(Consumer<String> consumer) {
                this.consumer = consumer;
            }

            @Override
            public FailureCollector evaluate(FailureCollector failureCollector) {
                return failureCollector;
            }

            @Override
            public LineAssert line(String line) {
                consumer.accept(line);
                return this;
            }

        }

        static class LinesAssert<C, H> implements LineAssert {

            private final Collection<C> checks;
            private final Collection<H> hits;
            private final BiConsumer<String, Collection<H>> lineConsumer;
            private final String description;
            private final StreamExpectationsSpec.ProcessOutput stream;

            private LinesAssert(
                    Collection<C> checks,
                    Collection<H> hits,
                    BiConsumer<String, Collection<H>> lineConsumer,
                    String description,
                    StreamExpectationsSpec.ProcessOutput stream) {
                this.checks = Objects.requireNonNull(checks, "checks");
                this.hits = Objects.requireNonNull(hits, "hits");
                this.lineConsumer = Objects.requireNonNull(lineConsumer, "lineConsumer");
                this.description = Objects.requireNonNull(description, "description");
                this.stream = Objects.requireNonNull(stream, "stream");
            }

            static LineAssert containsMatching(StreamExpectationsSpec.ProcessOutput stream, Collection<Pattern> checks) {
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
                        "Expected lines matching\n\n    ${checks}\n\nto occur in ${stream}, but the following patterns did not match:\n\n    ${hits}\n\n",
                        stream);
            }

            static LineAssert doesNotContainMatching(StreamExpectationsSpec.ProcessOutput stream, Collection<Pattern> checks) {
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
                        "Expected no lines matching\n\n    ${checks}\n\nto occur in ${stream}, but some of the patterns matched the lines\n\n    ${hits}\n\n",
                        stream);
            }

            @ExcludeFromJacocoGeneratedReport
            static Map<String, Pattern> toMap(Collection<Pattern> expectedPatterns) {
                Map<String, Pattern> pats = new LinkedHashMap<>();
                for (Pattern p : expectedPatterns) {
                    pats.put(p.pattern(), p);
                }
                return pats;
            }

            static List<Pattern> compile(Collection<String> expectedPatterns) {
                return Collections.unmodifiableList(expectedPatterns.stream()
                        .map(Pattern::compile)
                        .collect(Collectors.toList()));
            }

            @Override
            public FailureCollector evaluate(FailureCollector failureCollector) {
                synchronized (hits) {
                    if (!hits.isEmpty()) {
                        failureCollector.failure(Assert.Internal.formatMessage(description, this::eval));
                    }
                }
                return failureCollector;
            }

            @ExcludeFromJacocoGeneratedReport
            String eval(String key) {
                switch (key) {
                case "checks":
                    return Assert.Internal.list(checks);
                case "hits":
                    return Assert.Internal.list(hits);
                case "stream":
                    return stream.name();
                default:
                    throw new IllegalArgumentException("Unexpected placeholder '" + key + "' in " + LineAssert.class.getName()
                            + " description '" + description + "'.");
                }
            }

            @Override
            public LinesAssert<C, H> line(String line) {
                synchronized (hits) {
                    lineConsumer.accept(line, hits);
                }
                return this;
            }

        }

        static class LineCountAssert implements LineAssert {
            private final LongPredicate expected;
            private final AtomicLong actualCount = new AtomicLong();
            private final String description;
            private final StreamExpectationsSpec.ProcessOutput stream;

            private LineCountAssert(LongPredicate expected, String description,
                    StreamExpectationsSpec.ProcessOutput stream) {
                this.expected = Objects.requireNonNull(expected, "expected");
                this.description = Objects.requireNonNull(description, "description");
                this.stream = Objects.requireNonNull(stream, "stream");
            }

            @Override
            public FailureCollector evaluate(FailureCollector failureCollector) {
                if (!expected.test(actualCount.get())) {
                    failureCollector.failure(Assert.Internal.formatMessage(description, this::eval));
                }
                return failureCollector;
            }

            @ExcludeFromJacocoGeneratedReport
            String eval(String key) {
                switch (key) {
                case "actual":
                    return String.valueOf(actualCount.get());
                case "stream":
                    return stream.name();
                default:
                    throw new IllegalArgumentException("Unexpected placeholder '" + key + "' in " + LineAssert.class.getName()
                            + " description '" + description + "'.");
                }
            }

            @Override
            public LineAssert line(String line) {
                actualCount.incrementAndGet();
                return this;
            }
        }
    }

}

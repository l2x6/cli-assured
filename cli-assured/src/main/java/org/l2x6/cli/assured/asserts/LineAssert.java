/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured.asserts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An assertion on a line of a command output.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 * @since  0.0.1
 */
public interface LineAssert extends OutputAssert {

    /**
     * Assert that the given actual {@code line} fulfills the expectations
     *
     * @param line
     * @since      0.0.1
     */
    void line(String line);

    /**
     * Assert that the given lines are present in the underlying output stream among other lines in any order.
     *
     * @param  lines the whole lines to look for
     * @return       a new {@link LineAssert}
     * @since        0.0.1
     */
    static LineAssert contains(Collection<String> lines) {
        return new LinesAssert<String>(
                Collections.unmodifiableList(new ArrayList<>(lines)),
                new LinkedHashSet<>(lines),
                (line, hits) -> hits.remove(line));
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
        return new LinesAssert<String>(
                checks,
                new LinkedHashSet<>(),
                (line, hits) -> {
                    if (checks.contains(line)) {
                        hits.add(line);
                    }
                });
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
        return new LinesAssert<String>(
                Collections.unmodifiableList(new ArrayList<>(substrings)),
                new LinkedHashSet<>(substrings),
                (line, hits) -> {
                    for (Iterator<String> i = hits.iterator(); i.hasNext();) {
                        String substr = i.next();
                        if (line.contains(substr)) {
                            i.remove();
                        }
                    }
                });
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
        return new LinesAssert<String>(
                checks,
                new LinkedHashSet<>(),
                (line, hits) -> {
                    for (Iterator<String> i = checks.iterator(); i.hasNext();) {
                        String substr = i.next();
                        if (line.contains(substr)) {
                            hits.add(line);
                        }
                    }
                });
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
        Map<String, Pattern> pats = LinesAssert.toMap(regex);
        return LinesAssert.containsMatching(pats);
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
        Map<String, Pattern> pats = LinesAssert.toCompiledMap(regex);
        return LinesAssert.containsMatching(pats);
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
        Map<String, Pattern> pats = LinesAssert.toMap(regex);
        return LinesAssert.doesNotContainMatching(pats);
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
        Map<String, Pattern> pats = LinesAssert.toCompiledMap(regex);
        return LinesAssert.doesNotContainMatching(pats);
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
}

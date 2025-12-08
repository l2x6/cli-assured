/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.l2x6.cli.assured.asserts.LineAssert;

public class OutputLineAsserts implements LineAssert {

    private final List<LineAssert> asserts;
    private final Charset charset;

    OutputLineAsserts(List<LineAssert> asserts, Charset charset) {
        this.asserts = Objects.requireNonNull(asserts, "asserts");
        this.charset = Objects.requireNonNull(charset, "charset");
    }

    @Override
    public void assertSatisfied() {
        asserts.stream().forEach(LineAssert::assertSatisfied);
    }

    @Override
    public void line(String line) {
        asserts.stream().forEach(a -> a.line(line));
    }

    public Charset charset() {
        return charset;
    }

    public static class LinesConsumer extends OutputAsserts {
        private final OutputLineAsserts lineAsserts;

        LinesConsumer(InputStream inputStream, OutputLineAsserts lineAsserts) {
            super(inputStream);
            this.lineAsserts = lineAsserts;
        }

        @Override
        public void run() {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(in, lineAsserts.charset()))) {
                String line;
                while (!cancelled && (line = r.readLine()) != null) {
                    lineAsserts.line(line);
                }
            } catch (IOException e) {
                exception = e;
            }
        }

        public void assertSatisfied() {
            lineAsserts.assertSatisfied();
        }

        public void line(String line) {
            lineAsserts.line(line);
        }

        public int hashCode() {
            return lineAsserts.hashCode();
        }

        public boolean equals(Object obj) {
            return lineAsserts.equals(obj);
        }

        public String toString() {
            return lineAsserts.toString();
        }

    }

    public static class Builder {

        private final OutputAsserts.Builder outputAsserts;

        private List<LineAssert> asserts = new ArrayList<>();
        private Charset charset = StandardCharsets.UTF_8;

        Builder(OutputAsserts.Builder outputAsserts) {
            this.outputAsserts = outputAsserts;
        }

        /**
         * Assert that the given lines are present in the underlying output stream among other lines in any order.
         *
         * @param  lines the whole lines to look for
         * @return       this {@link Builder}
         * @since        0.0.1
         */
        public Builder contains(String... lines) {
            asserts.add(LineAssert.contains(Arrays.asList(lines)));
            return this;
        }

        /**
         * Assert that the given lines are present in the underlying output stream among other lines in any order.
         *
         * @param  lines the whole lines to look for
         * @return       this {@link Builder}
         * @since        0.0.1
         */
        public Builder contains(Collection<String> lines) {
            asserts.add(LineAssert.contains(lines));
            return this;
        }

        /**
         * Assert that the given lines are not present in the underlying output stream.
         *
         * @param  lines the whole lines to look for
         * @return       this {@link Builder}
         * @since        0.0.1
         */
        public Builder doesNotContain(String... lines) {
            asserts.add(LineAssert.doesNotContain(Arrays.asList(lines)));
            return this;
        }

        /**
         * Assert that the given lines are not present in the underlying output stream.
         *
         * @param  lines the whole lines to look for
         * @return       this {@link Builder}
         * @since        0.0.1
         */
        public Builder doesNotContain(Collection<String> lines) {
            asserts.add(LineAssert.doesNotContain(lines));
            return this;
        }

        /**
         * Assert that the underlying output stream has the given number of lines upon termination of the associated process.
         *
         * @param  expectedLineCount
         * @return                   this {@link Builder}
         * @since                    0.0.1
         */
        public Builder hasCount(int expectedLineCount) {
            asserts.add(LineAssert.hasCount(expectedLineCount));
            return this;
        }

        /**
         * Assert that lines containing the given {@code substrings} are present in the underlying output stream among other
         * lines in any order.
         *
         * @param  substrings the substrings to look for in the associated output stream
         * @return            this {@link Builder}
         * @since             0.0.1
         */
        public Builder containsSubstrings(String... substrings) {
            asserts.add(LineAssert.containsSubstrings(Arrays.asList(substrings)));
            return this;
        }

        /**
         * Assert that lines containing the given {@code substrings} are present in the underlying output stream among other
         * lines in any order.
         *
         * @param  substrings the substrings to look for in the associated output stream
         * @return            this {@link Builder}
         * @since             0.0.1
         */
        public Builder containsSubstrings(Collection<String> substrings) {
            asserts.add(LineAssert.containsSubstrings(substrings));
            return this;
        }

        /**
         * Assert that lines containing the given {@code substrings} are not present in the underlying output stream.
         *
         * @param  substrings the substrings to look for in the associated output stream
         * @return            this {@link Builder}
         * @since             0.0.1
         */
        public Builder doesNotContainSubstrings(String... substrings) {
            asserts.add(LineAssert.doesNotContainSubstrings(Arrays.asList(substrings)));
            return this;
        }

        /**
         * Assert that lines containing the given {@code substrings} are not present in the underlying output stream.
         *
         * @param  substrings the substrings to look for in the associated output stream
         * @return            this {@link Builder}
         * @since             0.0.1
         */
        public Builder doesNotContainSubstrings(Collection<String> substrings) {
            asserts.add(LineAssert.doesNotContainSubstrings(substrings));
            return this;
        }

        /**
         * Assert that lines matching the given regular expressions are present in the underlying output stream among other
         * lines in any order.
         * The regular expression is evaluated using {@link Matcher#find()} rather than {@link Matcher#matches()}
         *
         * @param  regex the regular expressions to look for in the associated output stream
         * @return       this {@link Builder}
         * @since        0.0.1
         */
        public Builder containsMatching(Collection<String> regex) {
            asserts.add(LineAssert.containsMatching(regex));
            return this;
        }

        /**
         * Assert that lines matching the given regular expressions are present in the underlying output stream among other
         * lines in any order.
         * The regular expression is evaluated using {@link Matcher#find()} rather than {@link Matcher#matches()}
         *
         * @param  regex the regular expressions to look for in the associated output stream
         * @return       this {@link Builder}
         * @since        0.0.1
         */
        public Builder containsMatching(String... regex) {
            asserts.add(LineAssert.containsMatching(Arrays.asList(regex)));
            return this;
        }

        /**
         * Assert that lines matching the given regular expressions are present in the underlying output stream among other
         * lines in any order.
         * The regular expression is evaluated using {@link Matcher#find()} rather than {@link Matcher#matches()}
         *
         * @param  regex the regular expressions to look for in the associated output stream
         * @return       this {@link Builder}
         * @since        0.0.1
         */
        public Builder containsMatching(Pattern... regex) {
            asserts.add(LineAssert.containsMatchingPatterns(Arrays.asList(regex)));
            return this;
        }

        /**
         * Assert that lines matching the given regular expressions are not present in the underlying output stream.
         * The regular expression is evaluated using {@link Matcher#find()} rather than {@link Matcher#matches()}
         *
         * @param  regex the regular expressions to look for in the associated output stream
         * @return       this {@link Builder}
         * @since        0.0.1
         */
        public Builder doesNotContainMatching(Collection<String> regex) {
            asserts.add(LineAssert.doesNotContainMatching(regex));
            return this;
        }

        /**
         * Assert that lines matching the given regular expressions are not present in the underlying output stream.
         * The regular expression is evaluated using {@link Matcher#find()} rather than {@link Matcher#matches()}
         *
         * @param  regex the regular expressions to look for in the associated output stream
         * @return       this {@link Builder}
         * @since        0.0.1
         */
        public Builder doesNotContainMatching(String... regex) {
            asserts.add(LineAssert.doesNotContainMatching(Arrays.asList(regex)));
            return this;
        }

        /**
         * Assert that lines matching the given regular expressions are not present in the underlying output stream.
         * The regular expression is evaluated using {@link Matcher#find()} rather than {@link Matcher#matches()}
         *
         * @param  regex the regular expressions to look for in the associated output stream
         * @return       this {@link Builder}
         * @since        0.0.1
         */
        public Builder doesNotContainMatching(Pattern... regex) {
            asserts.add(LineAssert.doesNotContainMatchingPatterns(Arrays.asList(regex)));
            return this;
        }

        /**
         * Read the underlying {@link InputStream} using the given {@code charset}
         *
         * @param  charset the character encoding to use when reading the underlying {@link InputStream}
         * @return         this {@link Builder}
         * @since          0.0.1
         */
        public Builder charset(Charset charset) {
            this.charset = charset;
            return this;
        }

        /**
         * @return the parent {@link OutputAsserts.Builder}
         */
        public OutputAsserts.Builder parent() {
            return outputAsserts.createConsumer(in -> new LinesConsumer(in, this.build()));
        }

        OutputLineAsserts build() {
            List<LineAssert> as = Collections.unmodifiableList(asserts);
            this.asserts = null;
            return new OutputLineAsserts(as, charset);
        }

        /**
         * @param  expectedExitCode
         * @return
         */
        public CommandProcess exitCode(int... expectedExitCodes) {
            return parent().exitCode(expectedExitCodes);
        }

        public CommandProcess start() {
            return parent().start();
        }

        public OutputAsserts.Builder stderr() {
            return parent().stderr();
        }

    }
}

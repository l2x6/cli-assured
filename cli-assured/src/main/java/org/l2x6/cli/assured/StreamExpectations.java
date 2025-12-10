/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.l2x6.cli.assured.asserts.Assert;
import org.l2x6.cli.assured.asserts.LineAssert;

public class StreamExpectations implements LineAssert {

    private final List<LineAssert> asserts;
    private final Charset charset;
    private final Supplier<OutputStream> redirect;

    StreamExpectations(List<LineAssert> asserts, Charset charset, Supplier<OutputStream> redirect) {
        this.asserts = Objects.requireNonNull(asserts, "asserts");
        this.charset = Objects.requireNonNull(charset, "charset");
        this.redirect = redirect;
    }

    @Override
    public void assertSatisfied() {
        asserts.stream().forEach(LineAssert::assertSatisfied);
    }

    @Override
    public StreamExpectations line(String line) {
        asserts.stream().forEach(a -> a.line(line));
        return this;
    }

    public Charset charset() {
        return charset;
    }

    public Supplier<OutputStream> redirect() {
        return redirect;
    }

    public boolean hasLineAsserts() {
        return asserts.size() > 0;
    }

    public static class Builder {
        private final Function<Function<InputStream, OutputConsumer>, Expectations.Builder> expectations;

        Builder(Function<Function<InputStream, OutputConsumer>, Expectations.Builder> expectations) {
            this.expectations = expectations;
        }

        private List<LineAssert> asserts = new ArrayList<>();
        private Charset charset = StandardCharsets.UTF_8;
        private Supplier<OutputStream> redirect;

        /**
         * Assert that the given {@link LineAssert}s are satisfied.
         *
         * @param  lines the whole lines to look for
         * @return       this {@link Builder}
         * @since        0.0.1
         */
        public Builder linesSatisfy(LineAssert... asserts) {
            for (LineAssert a : asserts) {
                this.asserts.add(a);
            }
            return this;
        }

        /**
         * Assert that the given {@link LineAssert}s are satisfied.
         *
         * @param  lines the whole lines to look for
         * @return       this {@link Builder}
         * @since        0.0.1
         */
        public Builder linesSatisfy(Collection<LineAssert> asserts) {
            for (LineAssert a : asserts) {
                this.asserts.add(a);
            }
            return this;
        }

        /**
         * Assert that the given lines are present in the underlying output stream among other lines in any order.
         *
         * @param  lines the whole lines to look for
         * @return       this {@link Builder}
         * @since        0.0.1
         */
        public Builder hasLines(String... lines) {
            asserts.add(LineAssert.hasLines(Arrays.asList(lines)));
            return this;
        }

        /**
         * Assert that the given lines are present in the underlying output stream among other lines in any order.
         *
         * @param  lines the whole lines to look for
         * @return       this {@link Builder}
         * @since        0.0.1
         */
        public Builder hasLines(Collection<String> lines) {
            asserts.add(LineAssert.hasLines(lines));
            return this;
        }

        /**
         * Assert that the given lines are not present in the underlying output stream.
         *
         * @param  lines the whole lines to look for
         * @return       this {@link Builder}
         * @since        0.0.1
         */
        public Builder doesNotHaveLines(String... lines) {
            asserts.add(LineAssert.doesNotHaveLines(Arrays.asList(lines)));
            return this;
        }

        /**
         * Assert that the given lines are not present in the underlying output stream.
         *
         * @param  lines the whole lines to look for
         * @return       this {@link Builder}
         * @since        0.0.1
         */
        public Builder doesNotHaveLines(Collection<String> lines) {
            asserts.add(LineAssert.doesNotHaveLines(lines));
            return this;
        }

        /**
         * Assert that the underlying output stream has the given number of lines upon termination of the associated process.
         *
         * @param  expectedLineCount
         * @return                   this {@link Builder}
         * @since                    0.0.1
         */
        public Builder hasLineCount(int expectedLineCount) {
            asserts.add(LineAssert.hasLineCount(expectedLineCount));
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
        public Builder hasLinesContaining(String... substrings) {
            asserts.add(LineAssert.hasLinesContaining(Arrays.asList(substrings)));
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
        public Builder hasLinesContaining(Collection<String> substrings) {
            asserts.add(LineAssert.hasLinesContaining(substrings));
            return this;
        }

        /**
         * Assert that lines containing the given {@code substrings} are not present in the underlying output stream.
         *
         * @param  substrings the substrings to look for in the associated output stream
         * @return            this {@link Builder}
         * @since             0.0.1
         */
        public Builder doesNotHaveLinesContaining(String... substrings) {
            asserts.add(LineAssert.doesNotHaveLinesContaining(Arrays.asList(substrings)));
            return this;
        }

        /**
         * Assert that lines containing the given {@code substrings} are not present in the underlying output stream.
         *
         * @param  substrings the substrings to look for in the associated output stream
         * @return            this {@link Builder}
         * @since             0.0.1
         */
        public Builder doesNotHaveLinesContaining(Collection<String> substrings) {
            asserts.add(LineAssert.doesNotHaveLinesContaining(substrings));
            return this;
        }

        /**
         * Assert that lines containing the given {@code substrings} (using case insensitive comparison) are present in the
         * underlying output stream among other
         * lines in any order.
         *
         * @param  substrings the substrings to look for in the associated output stream
         * @return            this {@link Builder}
         * @since             0.0.1
         */
        public Builder hasLinesContainingCaseInsensitive(String... substrings) {
            asserts.add(LineAssert.hasLinesContainingCaseInsensitive(
                    Stream.of(substrings).map(s -> s.toLowerCase(Locale.US)).collect(Collectors.toList()), Locale.US));
            return this;
        }

        /**
         * Assert that lines containing the given {@code substrings} (using case insensitive comparison) are present in the
         * underlying output stream among other
         * lines in any order.
         *
         * @param  substrings the substrings to look for in the associated output stream
         * @return            this {@link Builder}
         * @since             0.0.1
         */
        public Builder hasLinesContainingCaseInsensitive(Collection<String> substrings) {
            asserts.add(LineAssert.hasLinesContainingCaseInsensitive(
                    substrings.stream().map(s -> s.toLowerCase(Locale.US)).collect(Collectors.toList()), Locale.US));
            return this;
        }

        /**
         * Assert that lines containing the given {@code substrings} (using case insensitive comparison) are not present in the
         * underlying output stream.
         *
         * @param  substrings the substrings to look for in the associated output stream
         * @return            this {@link Builder}
         * @since             0.0.1
         */
        public Builder doesNotHaveLinesContainingCaseInsensitive(String... substrings) {
            asserts.add(LineAssert.doesNotHaveLinesContainingCaseInsensitive(
                    Stream.of(substrings).map(s -> s.toLowerCase(Locale.US)).collect(Collectors.toList()), Locale.US));
            return this;
        }

        /**
         * Assert that lines containing the given {@code substrings} (using case insensitive comparison) are not present in the
         * underlying output stream.
         *
         * @param  substrings the substrings to look for in the associated output stream
         * @return            this {@link Builder}
         * @since             0.0.1
         */
        public Builder doesNotHaveLinesContainingCaseInsensitive(Collection<String> substrings) {
            asserts.add(LineAssert.doesNotHaveLinesContainingCaseInsensitive(
                    substrings.stream().map(s -> s.toLowerCase(Locale.US)).collect(Collectors.toList()), Locale.US));
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
        public Builder hasLinesMatching(Collection<String> regex) {
            asserts.add(LineAssert.hasLinesMatching(regex));
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
        public Builder hasLinesMatching(String... regex) {
            asserts.add(LineAssert.hasLinesMatching(Arrays.asList(regex)));
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
        public Builder hasLinesMatching(Pattern... regex) {
            asserts.add(LineAssert.hasLinesMatchingPatterns(Arrays.asList(regex)));
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
        public Builder doesNotHaveLinesMatching(Collection<String> regex) {
            asserts.add(LineAssert.doesNotHaveLinesMatching(regex));
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
        public Builder doesNotHaveLinesMatching(String... regex) {
            asserts.add(LineAssert.doesNotHaveLinesMatching(Arrays.asList(regex)));
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
        public Builder doesNotHaveLinesMatching(Pattern... regex) {
            asserts.add(LineAssert.doesNotHaveLinesMatchingPatterns(Arrays.asList(regex)));
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
         * Redirect the output to the given {@code file}.
         *
         * @param  file where to store the output of the underlying command
         * @return      this {@link Builder}
         * @since       0.0.1
         */
        public Builder redirect(Path file) {
            this.redirect = () -> {
                try {
                    return Files.newOutputStream(file);
                } catch (IOException e) {
                    throw new UncheckedIOException("Could not open " + file + " for writing", e);
                }
            };
            return this;
        }

        /**
         * Redirect the output to the given {@code outputStream}.
         * Note that CLI Assured will not close the given {@code outputStream}.
         * The caller should take care to do so.
         *
         * @param  outputStream where to redirect the output of the underlying command
         * @return              this {@link Builder}
         * @since               0.0.1
         */
        public Builder redirect(OutputStream outputStream) {
            this.redirect = () -> new NonClosingOut(outputStream);
            return this;
        }

        Function<InputStream, OutputConsumer> build() {
            List<LineAssert> as = Collections.unmodifiableList(asserts);
            this.asserts = null;
            final StreamExpectations streamExpectations = new StreamExpectations(as, charset, redirect);
            return in -> new OutputConsumer.OutputAsserts(in, streamExpectations);
        }

        /**
         * Build new {@link StreamExpectations} from this {@link Builder} and pass it to the parent
         * {@link Expectations.Builder}.
         *
         * @return the parent {@link Expectations.Builder}
         * @since  0.0.1
         */
        Expectations.Builder parent() {
            return expectations.apply(build());
        }

        /**
         * Assert that the process exits with any the given {@code expectedExitCodes}
         * and start the command.
         *
         * @param  expectedExitCodes the exit codes to assert
         * @return                   a new {@link CommandProcess}
         * @since                    0.0.1
         */
        public CommandProcess exitCode(int... expectedExitCodes) {
            return parent().exitCode(expectedExitCodes);
        }

        /**
         * A shorthand for {@link #parent()}..{@link Expectations.Builder#start() start()}
         *
         * @return a started {@link CommandProcess}
         * @since  0.0.1
         */
        public CommandProcess start() {
            return parent().start();
        }

        /**
         * A shorthand for {@link #parent()}..{@link Expectations.Builder#stderr() stderr()}
         *
         * @return a new {@link OutputConsumer.Builder} to configure assertions for stderr
         * @since  0.0.1
         */
        public Builder stderr() {
            return parent().stderr();
        }
    }

    static class NonClosingOut extends FilterOutputStream {

        public NonClosingOut(OutputStream out) {
            super(out);
        }

        @Override
        public void close() throws IOException {
            /* The caller is responsible for closing */
        }
    }
}

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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.l2x6.cli.assured.asserts.Assert;
import org.l2x6.cli.assured.asserts.ByteCountAssert;
import org.l2x6.cli.assured.asserts.LineAssert;
import org.slf4j.LoggerFactory;

public class StreamExpectations implements LineAssert {

    private final List<LineAssert> lineAsserts;
    private final ByteCountAssert byteCountAssert;
    final Charset charset;
    final Supplier<OutputStream> redirect;
    final OutputConsumer.Stream stream;

    StreamExpectations(
            List<LineAssert> lineAsserts,
            ByteCountAssert byteCountAssert,
            Charset charset,
            Supplier<OutputStream> redirect,
            OutputConsumer.Stream stream) {
        this.lineAsserts = Objects.requireNonNull(lineAsserts, "lineAsserts");
        this.byteCountAssert = byteCountAssert;
        this.charset = Objects.requireNonNull(charset, "charset");
        this.redirect = redirect;
        this.stream = stream;
    }

    @Override
    public void assertSatisfied() {
        lineAsserts.stream().forEach(LineAssert::assertSatisfied);
    }

    public void assertSatisfied(long byteCount) {
        lineAsserts.stream().forEach(LineAssert::assertSatisfied);
        if (byteCountAssert != null) {
            byteCountAssert.byteCount(byteCount).assertSatisfied();
        }
    }

    @Override
    public StreamExpectations line(String line) {
        lineAsserts.stream().forEach(a -> a.line(line));
        return this;
    }

    public Charset charset() {
        return charset;
    }

    public Supplier<OutputStream> redirect() {
        return redirect;
    }

    public boolean hasLineAsserts() {
        return lineAsserts.size() > 0;
    }

    public static class Builder {
        private final Function<Function<InputStream, OutputConsumer>, Expectations.Builder> expectations;
        private final OutputConsumer.Stream stream;

        Builder(Function<Function<InputStream, OutputConsumer>, Expectations.Builder> expectations,
                OutputConsumer.Stream stream) {
            this.expectations = expectations;
            this.stream = stream;
        }

        private List<LineAssert> asserts = new ArrayList<>();
        private ByteCountAssert byteCountAssert;
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
         * Assert that upon termination of the associated process, the underlying output stream has produced the given
         * number of lines.
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
         * Assert that upon termination of the associated process, the underlying output stream's number of lines satisfies
         * the given {@link Predicate}.
         *
         * @param  expected    the condition the number of actual lines must satisfy
         * @param  description the description of a failure typically something like
         *                     {@code "Expected number of lines <condition> but found %d lines"}
         * @return             this {@link Builder}
         * @since              0.0.1
         */
        public Builder hasLineCount(Predicate<Integer> expected, String description) {
            asserts.add(LineAssert.hasLineCount(expected, description));
            return this;
        }

        /**
         * Assert that upon termination of the associated process, the underlying output stream has produced the given number of
         * bytes.
         *
         * @param  expectedByteCount the number of bytes to enforce
         * @return                   this {@link Builder}
         * @since                    0.0.1
         */
        public Builder hasByteCount(long expectedByteCount) {
            this.byteCountAssert = ByteCountAssert.hasByteCount(expectedByteCount);
            return this;
        }

        /**
         * Assert that upon termination of the associated process, the underlying output stream's number of produced byted
         * satisfies the given {@link Predicate}.
         *
         * @param  expected    the condition the number of actual bytes must satisfy
         * @param  description the description of a failure, typically something like
         *                     {@code "Expected number of bytes <condition> but found %d bytes"}
         * @return             this {@link Builder}
         * @since              0.0.1
         */
        public Builder hasByteCount(Predicate<Long> expected, String description) {
            this.byteCountAssert = ByteCountAssert.hasByteCount(expected, description);
            return this;
        }

        /**
         * Assert that upon termination of the associated process, the underlying output stream has produced zero bytes.
         * An equivalent of {@link #hasByteCount(long) hasByteCount(0)}.
         *
         * @param  expectedLineCount
         * @return                   this {@link Builder}
         * @since                    0.0.1
         */
        public Builder isEmpty() {
            this.byteCountAssert = ByteCountAssert.hasByteCount(0);
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

        /**
         * Log each line of the output at {@code INFO} level using {@code org.l2x6.cli.assured.[stdout|stderr]} logger.
         *
         * @return this {@link Builder}
         * @since  0.0.1
         */
        public Builder log() {
            this.asserts.add(LineAssert.log(LoggerFactory.getLogger("org.l2x6.cli.assured." + stream.name())::info));
            return this;
        }

        /**
         * Log each line of the output using the given {@code logger}. Note that the {@link Consumer#accept(Object)}
         * method will be called from an output consuming thread.
         *
         * @param  logger a {@link Consumer} to notify about every new line.
         * @return        this {@link Builder}
         * @since         0.0.1
         */
        public Builder log(Consumer<String> logger) {
            this.asserts.add(LineAssert.log(logger));
            return this;
        }

        Function<InputStream, OutputConsumer> build() {
            List<LineAssert> as = Collections.unmodifiableList(asserts);
            this.asserts = null;
            final StreamExpectations streamExpectations = new StreamExpectations(as, byteCountAssert, charset, redirect,
                    stream);
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
         * Assert that the process exits with any the given {@code expectedExitCodes},
         * build new {@link StreamExpectations} and set it on the parent {@link Expectations.Builder}
         *
         * @param  expectedExitCodes the exit codes to assert
         * @return                   the parent {@link Expectations.Builder}
         * @since                    0.0.1
         */
        public Expectations.Builder exitCode(int... expectedExitCodes) {
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

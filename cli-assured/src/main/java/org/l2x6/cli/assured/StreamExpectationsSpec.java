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
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.LongPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.l2x6.cli.assured.CliAssertUtils.ExcludeFromJacocoGeneratedReport;
import org.l2x6.cli.assured.asserts.Assert;
import org.l2x6.cli.assured.asserts.ByteCountAssert;
import org.l2x6.cli.assured.asserts.LineAssert;
import org.slf4j.LoggerFactory;

/**
 * Assertions applicable to {@code stdout} or {@code stderr} of a {@link CommandProcess}.
 *
 * @since  0.0.1
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class StreamExpectationsSpec {

    private final Function<Function<InputStream, OutputConsumer>, ExpectationsSpec> expectations;
    private final StreamExpectationsSpec.ProcessOutput stream;

    private final List<LineAssert> asserts;
    private final ByteCountAssert byteCountAssert;
    private final Charset charset;
    private final Supplier<OutputStream> redirect;
    private final int threadIndex;

    StreamExpectationsSpec(
            Function<Function<InputStream, OutputConsumer>, ExpectationsSpec> expectations,
            ProcessOutput stream, int threadIndex) {
        this.expectations = expectations;
        this.stream = stream;
        this.asserts = Collections.emptyList();
        this.byteCountAssert = null;
        this.charset = StandardCharsets.UTF_8;
        this.redirect = null;
        this.threadIndex = threadIndex;
    }

    StreamExpectationsSpec(
            Function<Function<InputStream, OutputConsumer>, ExpectationsSpec> expectations,
            StreamExpectationsSpec.ProcessOutput stream,
            List<LineAssert> asserts,
            ByteCountAssert byteCountAssert,
            Charset charset,
            Supplier<OutputStream> redirect, int threadIndex) {
        this.expectations = expectations;
        this.stream = stream;
        this.asserts = asserts;
        this.byteCountAssert = byteCountAssert;
        this.charset = charset;
        this.redirect = redirect;
        this.threadIndex = threadIndex;
    }

    /**
     * Assert that the given {@link LineAssert}s are satisfied.
     *
     * @param  lines the whole lines to look for
     * @return       an adjusted copy of this {@link StreamExpectationsSpec}
     * @since        0.0.1
     */
    public StreamExpectationsSpec linesSatisfy(LineAssert... asserts) {
        return new StreamExpectationsSpec(expectations, stream, CliAssertUtils.join(this.asserts, asserts), byteCountAssert,
                charset, redirect, threadIndex);
    }

    /**
     * Assert that the given {@link LineAssert}s are satisfied.
     *
     * @param  lines the whole lines to look for
     * @return       an adjusted copy of this {@link StreamExpectationsSpec}
     * @since        0.0.1
     */
    public StreamExpectationsSpec linesSatisfy(Collection<LineAssert> asserts) {
        return new StreamExpectationsSpec(expectations, stream, CliAssertUtils.join(this.asserts, asserts), byteCountAssert,
                charset, redirect, threadIndex);
    }

    /**
     * Assert that the given lines are present in the underlying output stream among other lines in any order.
     *
     * @param  lines the whole lines to look for
     * @return       an adjusted copy of this {@link StreamExpectationsSpec}
     * @since        0.0.1
     */
    public StreamExpectationsSpec hasLines(String... lines) {
        return new StreamExpectationsSpec(expectations, stream,
                CliAssertUtils.join(this.asserts, LineAssert.hasLines(stream, Arrays.asList(lines))), byteCountAssert, charset,
                redirect, threadIndex);
    }

    /**
     * Assert that the given lines are present in the underlying output stream among other lines in any order.
     *
     * @param  lines the whole lines to look for
     * @return       an adjusted copy of this {@link StreamExpectationsSpec}
     * @since        0.0.1
     */
    public StreamExpectationsSpec hasLines(Collection<String> lines) {
        return new StreamExpectationsSpec(expectations, stream,
                CliAssertUtils.join(this.asserts, LineAssert.hasLines(stream, lines)), byteCountAssert, charset, redirect,
                threadIndex);
    }

    /**
     * Assert that the given lines are not present in the underlying output stream.
     *
     * @param  lines the whole lines to look for
     * @return       an adjusted copy of this {@link StreamExpectationsSpec}
     * @since        0.0.1
     */
    public StreamExpectationsSpec doesNotHaveLines(String... lines) {
        return new StreamExpectationsSpec(expectations, stream,
                CliAssertUtils.join(this.asserts, LineAssert.doesNotHaveLines(stream, Arrays.asList(lines))), byteCountAssert,
                charset,
                redirect, threadIndex);
    }

    /**
     * Assert that the given lines are not present in the underlying output stream.
     *
     * @param  lines the whole lines to look for
     * @return       an adjusted copy of this {@link StreamExpectationsSpec}
     * @since        0.0.1
     */
    public StreamExpectationsSpec doesNotHaveLines(Collection<String> lines) {
        return new StreamExpectationsSpec(expectations, stream,
                CliAssertUtils.join(this.asserts, LineAssert.doesNotHaveLines(stream, lines)), byteCountAssert, charset,
                redirect, threadIndex);
    }

    /**
     * Assert that upon termination of the associated process, the underlying output stream has produced the given
     * number of lines.
     *
     * @param  expectedLineCount
     * @return                   an adjusted copy of this {@link StreamExpectationsSpec}
     * @since                    0.0.1
     */
    public StreamExpectationsSpec hasLineCount(int expectedLineCount) {
        return new StreamExpectationsSpec(expectations, stream,
                CliAssertUtils.join(this.asserts, LineAssert.hasLineCount(stream, expectedLineCount)), byteCountAssert, charset,
                redirect, threadIndex);
    }

    /**
     * Assert that upon termination of the associated process, the underlying output stream's number of lines satisfies
     * the given {@link LongPredicate}.
     *
     * @param  expected    the condition the number of actual lines must satisfy
     * @param  description the description of a failure typically something like
     *                     {@code "Expected number of lines <condition> in ${stream} but found ${actual} lines"} where
     *                     {@code <condition>} is your human readable criteria, like {@code greater that 42},
     *                     <code>${stream}</code> is a placeholder that CLI Assured will replace by {@code stdout}
     *                     or {@code stderr} and <code>${actual}</code> is a placeholder that CLI Assured will replace
     *                     by the actual number of lines found in the associated output stream
     * @return             an adjusted copy of this {@link StreamExpectationsSpec}
     * @since              0.0.1
     */
    public StreamExpectationsSpec hasLineCount(LongPredicate expected, String description) {
        return new StreamExpectationsSpec(expectations, stream,
                CliAssertUtils.join(this.asserts, LineAssert.hasLineCount(stream, expected, description)), byteCountAssert,
                charset,
                redirect, threadIndex);
    }

    /**
     * Assert that upon termination of the associated process, the underlying output stream has produced the given number of
     * bytes.
     *
     * @param  expectedByteCount the number of bytes to enforce
     * @return                   an adjusted copy of this {@link StreamExpectationsSpec}
     * @since                    0.0.1
     */
    public StreamExpectationsSpec hasByteCount(long expectedByteCount) {
        return new StreamExpectationsSpec(expectations, stream, asserts,
                ByteCountAssert.hasByteCount(stream, expectedByteCount),
                charset, redirect, threadIndex);
    }

    /**
     * Assert that upon termination of the associated process, the underlying output stream's number of produced byted
     * satisfies the given {@link Predicate}.
     *
     * @param  expected    the condition the number of actual bytes must satisfy
     * @param  description the description of a failure, typically something like
     *                     {@code "Expected number of bytes <condition> but found %d bytes"}
     * @return             an adjusted copy of this {@link StreamExpectationsSpec}
     * @since              0.0.1
     */
    public StreamExpectationsSpec hasByteCount(LongPredicate expected, String description) {
        return new StreamExpectationsSpec(
                expectations,
                stream,
                asserts,
                ByteCountAssert.hasByteCount(stream, expected, description),
                charset, redirect, threadIndex);
    }

    /**
     * Assert that upon termination of the associated process, the underlying output stream has produced zero bytes.
     * An equivalent of {@link #hasByteCount(long) hasByteCount(0)}.
     *
     * @param  expectedLineCount
     * @return                   an adjusted copy of this {@link StreamExpectationsSpec}
     * @since                    0.0.1
     */
    public StreamExpectationsSpec isEmpty() {
        return new StreamExpectationsSpec(expectations, stream, asserts, ByteCountAssert.hasByteCount(stream, 0), charset,
                redirect, threadIndex);
    }

    /**
     * Assert that lines containing the given {@code substrings} are present in the underlying output stream among other
     * lines in any order.
     *
     * @param  substrings the substrings to look for in the associated output stream
     * @return            an adjusted copy of this {@link StreamExpectationsSpec}
     * @since             0.0.1
     */
    public StreamExpectationsSpec hasLinesContaining(String... substrings) {
        return new StreamExpectationsSpec(expectations, stream,
                CliAssertUtils.join(this.asserts, LineAssert.hasLinesContaining(stream, Arrays.asList(substrings))),
                byteCountAssert,
                charset, redirect, threadIndex);
    }

    /**
     * Assert that lines containing the given {@code substrings} are present in the underlying output stream among other
     * lines in any order.
     *
     * @param  substrings the substrings to look for in the associated output stream
     * @return            an adjusted copy of this {@link StreamExpectationsSpec}
     * @since             0.0.1
     */
    public StreamExpectationsSpec hasLinesContaining(Collection<String> substrings) {
        return new StreamExpectationsSpec(expectations, stream,
                CliAssertUtils.join(this.asserts, LineAssert.hasLinesContaining(stream, substrings)), byteCountAssert, charset,
                redirect, threadIndex);
    }

    /**
     * Assert that lines containing the given {@code substrings} are not present in the underlying output stream.
     *
     * @param  substrings the substrings to look for in the associated output stream
     * @return            an adjusted copy of this {@link StreamExpectationsSpec}
     * @since             0.0.1
     */
    public StreamExpectationsSpec doesNotHaveLinesContaining(String... substrings) {
        return new StreamExpectationsSpec(expectations, stream,
                CliAssertUtils.join(this.asserts, LineAssert.doesNotHaveLinesContaining(stream, Arrays.asList(substrings))),
                byteCountAssert, charset, redirect, threadIndex);
    }

    /**
     * Assert that lines containing the given {@code substrings} are not present in the underlying output stream.
     *
     * @param  substrings the substrings to look for in the associated output stream
     * @return            an adjusted copy of this {@link StreamExpectationsSpec}
     * @since             0.0.1
     */
    public StreamExpectationsSpec doesNotHaveLinesContaining(Collection<String> substrings) {
        return new StreamExpectationsSpec(expectations, stream,
                CliAssertUtils.join(this.asserts, LineAssert.doesNotHaveLinesContaining(stream, substrings)), byteCountAssert,
                charset,
                redirect, threadIndex);
    }

    /**
     * Assert that lines containing the given {@code substrings} (using case insensitive comparison) are present in the
     * underlying output stream among other
     * lines in any order.
     *
     * @param  substrings the substrings to look for in the associated output stream
     * @return            an adjusted copy of this {@link StreamExpectationsSpec}
     * @since             0.0.1
     */
    public StreamExpectationsSpec hasLinesContainingCaseInsensitive(String... substrings) {
        return new StreamExpectationsSpec(expectations, stream,
                CliAssertUtils.join(this.asserts, LineAssert.hasLinesContainingCaseInsensitive(stream,
                        Stream.of(substrings).map(s -> s.toLowerCase(Locale.US)).collect(Collectors.toList()), Locale.US)),
                byteCountAssert, charset, redirect, threadIndex);
    }

    /**
     * Assert that lines containing the given {@code substrings} (using case insensitive comparison) are present in the
     * underlying output stream among other
     * lines in any order.
     *
     * @param  substrings the substrings to look for in the associated output stream
     * @return            an adjusted copy of this {@link StreamExpectationsSpec}
     * @since             0.0.1
     */
    public StreamExpectationsSpec hasLinesContainingCaseInsensitive(Collection<String> substrings) {
        return new StreamExpectationsSpec(expectations, stream,
                CliAssertUtils.join(this.asserts, LineAssert.hasLinesContainingCaseInsensitive(stream,
                        substrings.stream().map(s -> s.toLowerCase(Locale.US)).collect(Collectors.toList()), Locale.US)),
                byteCountAssert, charset, redirect, threadIndex);
    }

    /**
     * Assert that lines containing the given {@code substrings} (using case insensitive comparison) are not present in the
     * underlying output stream.
     *
     * @param  substrings the substrings to look for in the associated output stream
     * @return            an adjusted copy of this {@link StreamExpectationsSpec}
     * @since             0.0.1
     */
    public StreamExpectationsSpec doesNotHaveLinesContainingCaseInsensitive(String... substrings) {
        return new StreamExpectationsSpec(expectations, stream,
                CliAssertUtils.join(this.asserts, LineAssert.doesNotHaveLinesContainingCaseInsensitive(stream,
                        Stream.of(substrings).map(s -> s.toLowerCase(Locale.US)).collect(Collectors.toList()), Locale.US)),
                byteCountAssert, charset, redirect, threadIndex);
    }

    /**
     * Assert that lines containing the given {@code substrings} (using case insensitive comparison) are not present in the
     * underlying output stream.
     *
     * @param  substrings the substrings to look for in the associated output stream
     * @return            an adjusted copy of this {@link StreamExpectationsSpec}
     * @since             0.0.1
     */
    public StreamExpectationsSpec doesNotHaveLinesContainingCaseInsensitive(Collection<String> substrings) {
        return new StreamExpectationsSpec(expectations, stream,
                CliAssertUtils.join(this.asserts, LineAssert.doesNotHaveLinesContainingCaseInsensitive(stream,
                        substrings.stream().map(s -> s.toLowerCase(Locale.US)).collect(Collectors.toList()), Locale.US)),
                byteCountAssert, charset, redirect, threadIndex);
    }

    /**
     * Assert that lines matching the given regular expressions are present in the underlying output stream among other
     * lines in any order.
     * The regular expression is evaluated using {@link Matcher#find()} rather than {@link Matcher#matches()}
     *
     * @param  regex the regular expressions to look for in the associated output stream
     * @return       an adjusted copy of this {@link StreamExpectationsSpec}
     * @since        0.0.1
     */
    public StreamExpectationsSpec hasLinesMatching(Collection<String> regex) {
        return new StreamExpectationsSpec(expectations, stream,
                CliAssertUtils.join(this.asserts, LineAssert.hasLinesMatching(stream, regex)), byteCountAssert, charset,
                redirect, threadIndex);
    }

    /**
     * Assert that lines matching the given regular expressions are present in the underlying output stream among other
     * lines in any order.
     * The regular expression is evaluated using {@link Matcher#find()} rather than {@link Matcher#matches()}
     *
     * @param  regex the regular expressions to look for in the associated output stream
     * @return       an adjusted copy of this {@link StreamExpectationsSpec}
     * @since        0.0.1
     */
    public StreamExpectationsSpec hasLinesMatching(String... regex) {
        return new StreamExpectationsSpec(expectations, stream,
                CliAssertUtils.join(this.asserts, LineAssert.hasLinesMatching(stream, Arrays.asList(regex))), byteCountAssert,
                charset,
                redirect, threadIndex);
    }

    /**
     * Assert that lines matching the given regular expressions are present in the underlying output stream among other
     * lines in any order.
     * The regular expression is evaluated using {@link Matcher#find()} rather than {@link Matcher#matches()}
     *
     * @param  regex the regular expressions to look for in the associated output stream
     * @return       an adjusted copy of this {@link StreamExpectationsSpec}
     * @since        0.0.1
     */
    public StreamExpectationsSpec hasLinesMatching(Pattern... regex) {
        return new StreamExpectationsSpec(expectations, stream,
                CliAssertUtils.join(this.asserts, LineAssert.hasLinesMatchingPatterns(stream, Arrays.asList(regex))),
                byteCountAssert,
                charset, redirect, threadIndex);
    }

    /**
     * Assert that lines matching the given regular expressions are not present in the underlying output stream.
     * The regular expression is evaluated using {@link Matcher#find()} rather than {@link Matcher#matches()}
     *
     * @param  regex the regular expressions to look for in the associated output stream
     * @return       an adjusted copy of this {@link StreamExpectationsSpec}
     * @since        0.0.1
     */
    public StreamExpectationsSpec doesNotHaveLinesMatching(Collection<String> regex) {
        return new StreamExpectationsSpec(expectations, stream,
                CliAssertUtils.join(this.asserts, LineAssert.doesNotHaveLinesMatching(stream, regex)), byteCountAssert, charset,
                redirect, threadIndex);
    }

    /**
     * Assert that lines matching the given regular expressions are not present in the underlying output stream.
     * The regular expression is evaluated using {@link Matcher#find()} rather than {@link Matcher#matches()}
     *
     * @param  regex the regular expressions to look for in the associated output stream
     * @return       an adjusted copy of this {@link StreamExpectationsSpec}
     * @since        0.0.1
     */
    public StreamExpectationsSpec doesNotHaveLinesMatching(String... regex) {
        return new StreamExpectationsSpec(expectations, stream,
                CliAssertUtils.join(this.asserts, LineAssert.doesNotHaveLinesMatching(stream, Arrays.asList(regex))),
                byteCountAssert,
                charset, redirect, threadIndex);
    }

    /**
     * Assert that lines matching the given regular expressions are not present in the underlying output stream.
     * The regular expression is evaluated using {@link Matcher#find()} rather than {@link Matcher#matches()}
     *
     * @param  regex the regular expressions to look for in the associated output stream
     * @return       an adjusted copy of this {@link StreamExpectationsSpec}
     * @since        0.0.1
     */
    public StreamExpectationsSpec doesNotHaveLinesMatching(Pattern... regex) {
        return new StreamExpectationsSpec(expectations, stream,
                CliAssertUtils.join(this.asserts, LineAssert.doesNotHaveLinesMatchingPatterns(stream, Arrays.asList(regex))),
                byteCountAssert, charset, redirect, threadIndex);
    }

    /**
     * Read the underlying {@link InputStream} using the given {@code charset}
     *
     * @param  charset the character encoding to use when reading the underlying {@link InputStream}
     * @return         an adjusted copy of this {@link StreamExpectationsSpec}
     * @since          0.0.1
     */
    public StreamExpectationsSpec charset(Charset charset) {
        return new StreamExpectationsSpec(expectations, stream, asserts, byteCountAssert, charset, redirect, threadIndex);
    }

    /**
     * Redirect the output to the given {@code file}.
     *
     * @param  file where to store the output of the underlying command
     * @return      an adjusted copy of this {@link StreamExpectationsSpec}
     * @since       0.0.1
     */
    public StreamExpectationsSpec redirect(Path file) {
        return new StreamExpectationsSpec(expectations, stream, asserts, byteCountAssert, charset, () -> {
            return openFile(file);
        }, threadIndex);
    }

    @ExcludeFromJacocoGeneratedReport
    static OutputStream openFile(Path file) {
        try {
            return Files.newOutputStream(file);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not open " + file + " for writing", e);
        }
    }

    /**
     * Redirect the output to the given {@code outputStream}.
     * Note that CLI Assured will not close the given {@code outputStream}.
     * The caller should take care to do so.
     *
     * @param  outputStream where to redirect the output of the underlying command
     * @return              an adjusted copy of an adjusted copy of this {@link StreamExpectationsSpec}
     * @since               0.0.1
     */
    public StreamExpectationsSpec redirect(OutputStream outputStream) {
        return new StreamExpectationsSpec(expectations, stream, asserts, byteCountAssert, charset,
                () -> new StreamExpectationsSpec.NonClosingOut(outputStream), threadIndex);
    }

    /**
     * Log each line of the output at {@code INFO} level using {@code org.l2x6.cli.assured.[stdout|stderr]} logger.
     *
     * @return an adjusted copy of this {@link StreamExpectationsSpec}
     * @since  0.0.1
     */
    public StreamExpectationsSpec log() {
        return new StreamExpectationsSpec(expectations, stream,
                CliAssertUtils.join(this.asserts,
                        LineAssert.log(stream, LoggerFactory.getLogger("org.l2x6.cli.assured." + stream.name())::info)),
                byteCountAssert, charset, redirect, threadIndex);
    }

    /**
     * Log each line of the output using the given {@code logger}. Note that the {@link Consumer#accept(Object)}
     * method will be called from an output consuming thread.
     *
     * @param  logger a {@link Consumer} to notify about every new line.
     * @return        an adjusted copy of this {@link StreamExpectationsSpec}
     * @since         0.0.1
     */
    public StreamExpectationsSpec log(Consumer<String> logger) {
        return new StreamExpectationsSpec(expectations, stream,
                CliAssertUtils.join(this.asserts, LineAssert.log(stream, logger)),
                byteCountAssert, charset, redirect, threadIndex);
    }

    /**
     * Build new {@link StreamExpectations} and set it on the parent {@link ExpectationsSpec} and
     * assert that the process exits with the given {@code expectedExitCode}.
     *
     * @param  expectedExitCode the exit code to assert
     * @return                  an adjusted copy of this {@link ExpectationsSpec}
     * @since                   0.0.1
     */
    public ExpectationsSpec exitCodeIs(int expectedExitCode) {
        return parent().exitCodeIs(expectedExitCode);
    }

    /**
     * Build new {@link StreamExpectations} and set it on the parent {@link ExpectationsSpec} and
     * assert that the process exits with any of the given {@code expectedExitCodes},
     *
     *
     * @param  expectedExitCodes the exit codes to assert
     * @return                   the parent {@link ExpectationsSpec}
     * @since                    0.0.1
     */
    public ExpectationsSpec exitCodeIsAnyOf(int... expectedExitCodes) {
        return parent().exitCodeIsAnyOf(expectedExitCodes);
    }

    /**
     * Build new {@link StreamExpectations} and set it on the parent {@link ExpectationsSpec} and
     * assert that the process exits with an exit code that satisfies the given {@link IntPredicate}.
     *
     * @param  expected    the condition actual exit code must satisfy
     * @param  description the description of a failure typically something like
     *                     {@code "Expected exit code <condition> but actually terminated with exit code ${actual}"} where
     *                     {@code <condition>} is your human readable criteria, like {@code greater that 42},
     *                     and <code>${actual}</code> is a placeholder that CLI Assured will replace
     *                     by the actual exit code of the process
     * @return             an adjusted copy of this {@link ExpectationsSpec}
     * @since              0.0.1
     */
    public ExpectationsSpec exitCodeSatisfies(IntPredicate expected, String description) {
        return parent().exitCodeSatisfies(expected, description);
    }

    /**
     * A shorthand for {@link #parent()}..{@link ExpectationsSpec#stderr() stderr()}
     *
     * @return a new {@link StreamExpectationsSpec} to configure assertions for {@code stderr}
     * @since  0.0.1
     */
    public StreamExpectationsSpec stderr() {
        return parent().stderr();
    }

    /**
     * Build new {@link StreamExpectations} from this {@link StreamExpectationsSpec}, pass it to the parent
     * {@link ExpectationsSpec}, pass them to the parent {@link CommandSpec}
     * and start the {@link CommandProcess}.
     *
     * @return a started {@link CommandProcess}
     * @since  0.0.1
     * @see    #execute()
     */
    public CommandProcess start() {
        return parent().start();
    }

    /**
     * Build new {@link StreamExpectations} from this {@link StreamExpectationsSpec}, pass it to the parent
     * {@link ExpectationsSpec}, pass them to the parent {@link CommandSpec}, start
     * the {@link CommandProcess} and await (potentially indefinitely) its termination.
     * A shorthand for {@link #start()}.{@link CommandProcess#awaitTermination() awaitTermination()}
     *
     * @return a {@link CommandResult}
     * @since  0.0.1
     */
    public CommandResult execute() {
        return parent().execute();
    }

    /**
     * Build new {@link StreamExpectations} from this {@link StreamExpectationsSpec}, pass it to the parent
     * {@link ExpectationsSpec}, pass them to the parent {@link CommandSpec}, start
     * the {@link CommandProcess} and await its termination at most for the specified
     * duration.
     * A shorthand for {@link #start()}.{@link CommandProcess#awaitTermination(Duration) awaitTermination(Duration)}
     *
     * @param  timeout maximum time to wait for the underlying process to terminate
     *
     * @return         a {@link CommandResult}
     * @since          0.0.1
     */
    public CommandResult execute(Duration timeout) {
        return parent().execute(timeout);
    }

    /**
     * Build new {@link StreamExpectations} from this {@link StreamExpectationsSpec}, pass it to the parent
     * {@link ExpectationsSpec}, pass them to the parent {@link CommandSpec}, start
     * the {@link CommandProcess} and await its termination at most for the specified
     * timeout in milliseconds.
     * A shorthand for {@link #start()}.{@link CommandProcess#awaitTermination(long) awaitTermination(long)}
     *
     * @param  timeoutMs maximum time in milliseconds to wait for the underlying process to terminate
     *
     * @return           a {@link CommandResult}
     * @since            0.0.1
     */
    public CommandResult execute(long timeoutMs) {
        return parent().execute(timeoutMs);
    }

    Function<InputStream, OutputConsumer> build() {
        final StreamExpectations streamExpectations = new StreamExpectations(
                asserts,
                byteCountAssert,
                charset,
                redirect,
                stream);
        return in -> new OutputConsumer.OutputAsserts(in, streamExpectations, threadIndex);
    }

    /**
     * Build new {@link StreamExpectations} from this {@link StreamExpectationsSpec} and pass it to the parent
     * {@link ExpectationsSpec}.
     *
     * @return the parent {@link ExpectationsSpec}
     * @since  0.0.1
     */
    ExpectationsSpec parent() {
        return expectations.apply(build());
    }

    static class StreamExpectations {

        private final List<LineAssert> lineAsserts;
        private final ByteCountAssert byteCountAssert;
        final Charset charset;
        final Supplier<OutputStream> redirect;
        final StreamExpectationsSpec.ProcessOutput stream;

        static StreamExpectations hasNoLines(StreamExpectationsSpec.ProcessOutput stream) {
            return new StreamExpectations(
                    Collections.singletonList(LineAssert.doesNotHaveAnyLines(stream)),
                    null,
                    StandardCharsets.UTF_8,
                    null,
                    stream);
        }

        StreamExpectations(
                List<LineAssert> lineAsserts,
                ByteCountAssert byteCountAssert,
                Charset charset,
                Supplier<OutputStream> redirect,
                StreamExpectationsSpec.ProcessOutput stream) {
            this.lineAsserts = Objects.requireNonNull(lineAsserts, "lineAsserts");
            this.byteCountAssert = byteCountAssert;
            this.charset = Objects.requireNonNull(charset, "charset");
            this.redirect = redirect;
            this.stream = stream;
        }

        public void assertSatisfied(long byteCount, Assert.FailureCollector failureCollector) {
            lineAsserts.stream().forEach(a -> a.evaluate(failureCollector));
            if (byteCountAssert != null) {
                byteCountAssert.byteCount(byteCount).evaluate(failureCollector);
            }
        }

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

    public static enum ProcessOutput {
        stdout, stderr
    }

}

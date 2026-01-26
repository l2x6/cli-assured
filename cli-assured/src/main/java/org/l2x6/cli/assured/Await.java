/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.l2x6.cli.assured.Await.Internal.ExcludeFromJacocoGeneratedReport;
import org.l2x6.cli.assured.Await.Internal.PatternPredicateMapper;

/**
 * A utility for awaiting some condition satisfied by a line output of a {@link Process}.
 *
 * @author     <a href="https://github.com/ppalaga">Peter Palaga</a>
 * @param  <T> the type if the data extracted from the {@link Process} output
 * @since      0.0.1
 */
public interface Await<T> extends Consumer<String> {

    /**
     * Wait (potentially indefinitely) for the fulfillment of the associated condition and return the result data.
     *
     * @return the result data associated with this {@link Await}
     */
    T await();

    /**
     * Wait at most for the given {@code timeout} duration for the fulfillment of the associated condition and return the
     * result data.
     *
     * @param  timeout the time to wait at most for the fulfillment of the associated condition
     * @return         the result data associated with this {@link Await}
     */
    T await(Duration timeout);

    /**
     * Create a new {@link LineAwait} completed when the given {@code line} occurs for the first time.
     * Unless a custom mapper {@link Function} is set via {@link LineAwait#map(Function)}, the {@link #await() await(*)}
     * methods return the matching line.
     *
     * @param  line the text of a complete line to look for in the output of the {@link Process}
     * @return      a new {@link LineAwait}
     * @since       0.0.1
     */
    static LineAwait<String> line(String line) {
        return new LineAwait<>("line '" + line + "'", line::equals, Function.identity());
    }

    /**
     * Create a new {@link LineAwait} completed when the given {@code substring} occurs in some line in the output
     * of the associated command for the first time.
     * Unless a custom mapper {@link Function} is set via {@link LineAwait#map(Function)}, the {@link #await() await(*)}
     * methods return the matching line.
     *
     * @param  substring text to look for in the lines of the output of the {@link Process}
     * @return           a new {@link LineAwait}
     * @since            0.0.1
     */
    static LineAwait<String> lineContaining(String substring) {
        return new LineAwait<>("line containing '" + substring + "'", l -> l.contains(substring), Function.identity());
    }

    /**
     * Create a new {@link LineAwait} completed when the given {@code substring} occurs in some line in the output
     * of the associated command for the first time.
     * Unless a custom mapper {@link Function} is set via {@link LineAwait#map(Function)}, the {@link #await() await(*)}
     * methods return the matching line.
     *
     * @param  substring text to look for (using case insensitive comparison) in the lines of the output of the
     *                   {@link Process}
     * @return           a new {@link LineAwait}
     * @since            0.0.1
     */
    static LineAwait<String> lineContainingCaseInsensitive(String substring) {
        final String lcSubstring = substring.toLowerCase(Locale.US);
        return new LineAwait<>("line containing case insensitive '" + substring + "'",
                l -> l.toLowerCase().contains(lcSubstring), Function.identity());
    }

    /**
     * Await the first line matching the given {@code pattern}. If the pattern has at least one
     * <a href="https://docs.oracle.com/javase/tutorial/essential/regex/groups.html">capturing group</a> defined
     * then the {@link LineAwait#map(Function) mapper} is set to a function extracting the first capturing group;
     * otherwise the mapper is set to {@link Function#identity()}.
     * <p>
     * This might be handy for extracting some substring, such as port number from the matching line.
     * <p>
     * Example:
     *
     * <pre>{@code
     * LineAwait<Integer> await = Await
     *         .lineMatching("listening on port: (\\d+)")
     *         .map(Integer::parseInt);
     *
     * try (CommandProcess proc = CliAssured.command("start-server ...")
     *         .then()
     *         .stdout()
     *         .log()
     *         .await(await)
     *         .stderr()
     *         .log()
     *         .start()) {
     *
     *     final int port = await.await(Duration.ofSeconds(10));
     *     // Connect and test
     *     RestAssured.get("http://localhost:" + port)
     *             .then()
     *             .statusCode(200);
     * } // CommandProcess proc gets auto-closed here
     * }</pre>
     *
     * @param  pattern the pattern to match in the output of the {@link Process}
     * @return         a new {@link LineAwait} completed when the the first line is consumer, for which
     *                 {@code pattern.matcher(line).find()} returns {@code true}
     * @since          0.0.1
     */
    static LineAwait<String> lineMatching(String pattern) {
        return lineMatching(Pattern.compile(pattern));
    }

    /**
     * Await the first line matching the given {@code pattern}. If the pattern has at least one
     * <a href="https://docs.oracle.com/javase/tutorial/essential/regex/groups.html">capturing group</a> defined
     * then the {@link LineAwait#map(Function) mapper} is set to a function extracting the first capturing group;
     * otherwise the mapper is set to {@link Function#identity()}.
     * <p>
     * This might be handy for extracting some substring, such as port number from the matching line.
     * <p>
     * Example:
     *
     * <pre>{@code
     * LineAwait<Integer> await = Await
     *         .lineMatching("listening on port: (\\d+)")
     *         .map(Integer::parseInt);
     *
     * try (CommandProcess proc = CliAssured.command("start-server ...")
     *         .then()
     *         .stdout()
     *         .await(await)
     *         .log()
     *         .stderr()
     *         .log()
     *         .start()) {
     *
     *     final int port = await.await(Duration.ofSeconds(10));
     *     // Connect and test
     *     RestAssured.get("http://localhost:" + port)
     *             .then()
     *             .statusCode(200);
     * } // CommandProcess proc gets auto-closed here
     * }</pre>
     *
     * @param  pattern the pattern to match in the output of the {@link Process}
     * @return         a new {@link LineAwait} completed when the the first line is consumer, for which
     *                 {@code pattern.matcher(line).find()} returns {@code true}
     * @since          0.0.1
     */
    static LineAwait<String> lineMatching(Pattern pattern) {
        final PatternPredicateMapper pm = new PatternPredicateMapper(pattern);
        return new LineAwait<>("line matching '" + pattern.pattern() + "'", pm, pm);
    }

    /**
     * Create a new {@link LineAwait} completed when the given {@code lineCount} is reached.
     * Unless a custom mapper {@link Function} is set via {@link LineAwait#map(Function)}, the {@link #await() await(*)}
     * methods return the line triggering the completion of the returned {@link LineAwait}.
     *
     * @param  lineCount the number of lines after consuming of which will the returned {@link LineAwait} be completed
     * @return           a new {@link LineAwait}
     * @since            0.0.1
     */
    static LineAwait<String> lineCount(int lineCount) {
        return new LineAwait<>("line count " + lineCount, new Internal.LineCountingPredicate(lineCount), Function.identity());
    }

    /**
     * Create a new {@link LineAwait} completed by a line satisfying the given {@code predicate}.
     * Unless a custom mapper {@link Function} is set via {@link LineAwait#map(Function)}, the {@link #await() await(*)}
     * methods return the line triggering the completion of the returned {@link LineAwait}.
     *
     * @param  predicate the {@link Predicate} to test
     * @return           a new {@link LineAwait}
     * @since            0.0.1
     */
    static LineAwait<String> lineSatifying(String description, Predicate<String> predicate) {
        return new LineAwait<>(description, predicate, Function.identity());
    }

    /**
     * An {@link Await} suitable for awaiting some message in {@code stdin} or {@code stdout} of a process.
     *
     * @author     <a href="https://github.com/ppalaga">Peter Palaga</a>
     *
     * @param  <T> the type of the result data
     */
    static final class LineAwait<T> implements Await<T> {

        private final Predicate<String> predicate;
        private final Function<String, T> mapper;
        private final CompletableFuture<T> result;
        private final String description;

        private LineAwait(
                final String description,
                Predicate<String> predicate,
                Function<String, T> mapper) {
            this.description = Objects.requireNonNull(description, "description");
            this.predicate = Objects.requireNonNull(predicate, "predicate");
            this.mapper = Objects.requireNonNull(mapper, "mapper");
            this.result = new CompletableFuture<>();
        }

        @Override
        public void accept(String line) {
            try {
                if (!result.isDone() && predicate.test(line)) {
                    result.complete(mapper.apply(line));
                }
            } catch (Throwable e) {
                result.completeExceptionally(e);
            }
        }

        @Override
        @ExcludeFromJacocoGeneratedReport
        public T await() {
            try {
                return result.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError("Exception thrown when awaiting " + description, e);
            } catch (ExecutionException e) {
                throw new AssertionError("Exception thrown when awaiting " + description, e);
            }
        }

        @Override
        @ExcludeFromJacocoGeneratedReport
        public T await(Duration timeout) {
            try {
                return result.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError("Exception thrown when awaiting " + description, e);
            } catch (ExecutionException e) {
                throw new AssertionError("Exception thrown when awaiting " + description, e);
            } catch (TimeoutException e) {
                throw new TimeoutAssertionError(
                        "Awaiting " + description + " has not finished within " + timeout.toMillis() + " ms");
            }
        }

        /**
         * Transform the result data by applying the given {@code mapper} {@link Function} on the result of applying
         * {@code this.mapper}
         *
         * @param  <U>    new type of the result data
         * @param  mapper the {@code mapper} {@link Function} to chain after {@code this.mapper}
         * @return        a new {@link LineAwait} with a mapper created via {@code this.mapper.andThen(mapper)} - see
         *                {@link Function#andThen(Function)}
         *
         * @since         0.0.1
         */
        public <U> LineAwait<U> map(Function<T, U> mapper) {
            return new LineAwait<>(description, predicate, this.mapper.andThen(mapper));
        }

    }

    static final class Internal {
        @Retention(RetentionPolicy.RUNTIME)
        @Target({ ElementType.CONSTRUCTOR, ElementType.METHOD })
        public @interface ExcludeFromJacocoGeneratedReport {
        }

        @ExcludeFromJacocoGeneratedReport
        private Internal() {
        }

        static final class PatternPredicateMapper implements Predicate<String>, Function<String, String> {

            private final Pattern pattern;

            PatternPredicateMapper(Pattern pattern) {
                this.pattern = pattern;
            }

            private Matcher matcher;

            @Override
            public boolean test(String l) {
                final Matcher m = pattern.matcher(l);
                final boolean result = m.find();
                if (result) {
                    this.matcher = m;
                }
                return result;
            }

            @Override
            public String apply(String l) {
                final Matcher m = this.matcher;
                return m.groupCount() > 0 ? m.group(1) : l;
            }

        }

        static final class LineCountingPredicate implements Predicate<String> {
            private final int expectedCount;
            private int count = 0;

            private LineCountingPredicate(int expectedCount) {
                this.expectedCount = expectedCount;
            }

            @Override
            public boolean test(String t) {
                return ++count >= expectedCount;
            }
        }

    }

}

/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured.asserts;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.l2x6.cli.assured.StreamExpectationsSpec.ProcessOutput;

/**
 * An abstract assertion.
 *
 * @since  0.0.1
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public interface Assert {
    /**
     * Creates a new aggregate {@link Assert} failing if any of the component {@link Assert}s fails.
     *
     * @param  asserts the {@link Assert}s to aggregate.
     * @return         a new aggregate {@link Assert}
     * @since          0.0.1
     */
    static Assert all(Assert... asserts) {
        final List<Assert> copy = Collections.unmodifiableList(new ArrayList<>(Arrays.asList(asserts)));
        return failureCollector -> {
            copy.stream().filter(a -> a != null).forEach(a -> a.evaluate(failureCollector));
            return failureCollector;
        };
    }

    /**
     * Evaluate this {@link Assert} and pass any failures to the given {@link FailureCollector}.
     *
     * @param failureCollector for reporting any assertion failures or exceptions that occurred while executing the command
     * @since                  0.0.1
     */
    FailureCollector evaluate(FailureCollector failureCollector);

    /**
     * A utility for collecting assertion failure messages and exceptions and for assembling the aggregated failure message.
     *
     * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
     * @since  0.0.1
     */
    static class FailureCollector {
        private final String command;

        private final Map<ProcessOutput, StreamFailureCollector> streamCollectors;

        public FailureCollector(String command) {
            this.command = Objects.requireNonNull(command, "command");

            Map<ProcessOutput, StreamFailureCollector> m = new LinkedHashMap<>();
            m.put(null, new StreamFailureCollector());
            m.put(ProcessOutput.stdout, new StreamFailureCollector());
            m.put(ProcessOutput.stderr, new StreamFailureCollector());
            this.streamCollectors = Collections.unmodifiableMap(m);
        }

        /**
         * Add the specified failure to this {@link FailureCollector}.
         *
         * @param  stream      the {@link ProcessOutput} the failure is related to; can be {@code null}
         * @param  description the description of the failure to add to this {@link FailureCollector}
         * @return             this {@link FailureCollector}
         * @since              0.0.1
         */
        public FailureCollector failure(ProcessOutput stream, String description) {
            streamCollectors.get(stream).failures.add(description);
            return this;
        }

        /**
         * Add the specified {@code exception} to this {@link FailureCollector}.
         *
         * @param  stream    the {@link ProcessOutput} the exception is related to; can be {@code null}
         * @param  exception the exception to add to this {@link FailureCollector}
         * @return           this {@link FailureCollector}
         * @since            0.0.1
         */
        public FailureCollector exception(ProcessOutput stream, Throwable exception) {
            streamCollectors.get(stream).exceptions.add(exception);
            return this;
        }

        /**
         * Add the {@code capture} report to this {@link FailureCollector}.
         *
         * @param  stream  the {@link ProcessOutput} the capture is related to; can be {@code null}
         * @param  capture the capture to add to this {@link FailureCollector}
         * @return         this {@link FailureCollector}
         * @since          0.0.1
         */
        public FailureCollector capture(ProcessOutput stream, Consumer<StringBuilder> capture) {
            streamCollectors.get(stream).capture = capture;
            return this;
        }

        /**
         * @param  stream the {@link ProcessOutput} for which the sum should be computed
         * @return        the sum of the failures and errors counts
         */
        public int sum(ProcessOutput stream) {
            return streamCollectors.get(stream).sum();
        }

        /**
         * @throws AssertionError if any failures or exceptions were added to this {@link FailureCollector}
         */
        public void assertSatisfied() {
            int exceptionsCount = 0;
            int failuresCount = 0;
            for (StreamFailureCollector c : streamCollectors.values()) {
                exceptionsCount += c.exceptions.size();
                failuresCount += c.failures.size();
            }
            if (exceptionsCount + failuresCount > 0) {
                final StringJoiner exceptionsAndFailures = new StringJoiner(" and ");
                if (exceptionsCount > 0) {
                    exceptionsAndFailures.add("" + exceptionsCount + " exceptions");
                }
                if (failuresCount > 0) {
                    exceptionsAndFailures.add("" + failuresCount + " assertion failures");
                }
                StringBuilder message = new StringBuilder()
                        .append(exceptionsAndFailures.toString())
                        .append(" occurred while executing\n\n    ")
                        .append(command);
                final AtomicInteger exceptionsIndex = new AtomicInteger(1);
                final AtomicInteger failuresIndex = new AtomicInteger(1);
                for (StreamFailureCollector c : streamCollectors.values()) {
                    c.append(message, exceptionsIndex, exceptionsCount, failuresIndex, failuresCount);
                }

                throw new AssertionError(message);
            }
        }

        @org.l2x6.cli.assured.asserts.Assert.Internal.ExcludeFromJacocoGeneratedReport
        static void assertTwoTrailingNewLines(StringBuilder message) {
            final int len = message.length();
            if (len >= 2) {
                if (message.charAt(len - 1) == '\n') {
                    if (message.charAt(len - 2) == '\n') {
                        /* nothing to do */
                        return;
                    }
                    message.append('\n');
                    return;
                }
                message.append("\n\n");
                return;
            }
            if (len == 1) {
                if (message.charAt(len - 1) == '\n') {
                    message.append('\n');
                    return;
                }
                message.append("\n\n");
                return;
            }
            if (len == 0) {
                message.append("\n\n");
                return;
            }

        }

        /**
         * Failures, exceptions and the capture for a {@link ProcessOutput}.
         */
        static class StreamFailureCollector {
            private final List<String> failures = new ArrayList<>();
            private final List<Throwable> exceptions = new ArrayList<>();
            private Consumer<StringBuilder> capture;

            /**
             * @return the sum of the failures and errors counts
             */
            public int sum() {
                return failures.size() + exceptions.size();
            }

            void append(StringBuilder message,
                    AtomicInteger exceptionsIndex, int exceptionsCount,
                    AtomicInteger failuresIndex, int failuresCount) {
                if (!exceptions.isEmpty()) {
                    AppendableWriter w = new AppendableWriter(message);
                    for (Throwable e : exceptions) {
                        assertTwoTrailingNewLines(message);
                        message.append("Exception ").append(exceptionsIndex.getAndIncrement()).append('/')
                                .append(exceptionsCount).append(": ");
                        e.printStackTrace(w);
                    }
                }
                if (!failures.isEmpty()) {
                    for (String f : failures) {
                        assertTwoTrailingNewLines(message);
                        message.append("Failure ").append(failuresIndex.getAndIncrement()).append('/').append(failuresCount)
                                .append(": ")
                                .append(f);
                    }
                }
                if (capture != null) {
                    assertTwoTrailingNewLines(message);
                    capture.accept(message);
                }
            }

        }

        static class AppendableWriter extends PrintWriter {
            private final StringBuilder delegate;

            private AppendableWriter(StringBuilder delegate) {
                super(new StringWriter());
                this.delegate = delegate;
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }

            @Override
            public boolean checkError() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void write(int c) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void write(char[] buf, int off, int len) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void write(char[] buf) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void write(String s, int off, int len) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void write(String s) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void print(boolean b) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void print(char c) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void print(int i) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void print(long l) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void print(float f) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void print(double d) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void print(char[] s) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void print(String s) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void print(Object obj) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void println() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void println(boolean x) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void println(char x) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void println(int x) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void println(long x) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void println(float x) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void println(double x) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void println(char[] x) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void println(String x) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void println(Object x) {
                delegate.append(x).append('\n');
            }

            @Override
            public PrintWriter printf(String format, Object... args) {
                throw new UnsupportedOperationException();
            }

            @Override
            public PrintWriter printf(Locale l, String format, Object... args) {
                throw new UnsupportedOperationException();
            }

            @Override
            public PrintWriter format(String format, Object... args) {
                throw new UnsupportedOperationException();
            }

            @Override
            public PrintWriter format(Locale l, String format, Object... args) {
                throw new UnsupportedOperationException();
            }

            @Override
            public PrintWriter append(CharSequence csq) {
                throw new UnsupportedOperationException();
            }

            @Override
            public PrintWriter append(CharSequence csq, int start, int end) {
                throw new UnsupportedOperationException();
            }

            @Override
            public PrintWriter append(char c) {
                throw new UnsupportedOperationException();
            }

            @Override
            public int hashCode() {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean equals(Object obj) {
                throw new UnsupportedOperationException();
            }

            @Override
            protected Object clone() throws CloneNotSupportedException {
                throw new UnsupportedOperationException();
            }

            @Override
            public String toString() {
                return delegate.toString();
            }

            @Override
            protected void finalize() throws Throwable {
                throw new UnsupportedOperationException();
            }
        }
    }

    static final class Internal {
        @Retention(RetentionPolicy.RUNTIME)
        @Target({ ElementType.CONSTRUCTOR, ElementType.METHOD })
        public @interface ExcludeFromJacocoGeneratedReport {
        }

        private static final Pattern PLACE_HOLDER_PATTERN = Pattern.compile("\\$\\{([^\\}]+)\\}");

        @ExcludeFromJacocoGeneratedReport
        private Internal() {
        }

        static String formatMessage(String message, Function<String, String> eval) {
            Matcher m = PLACE_HOLDER_PATTERN.matcher(message);
            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                m.appendReplacement(sb, Matcher.quoteReplacement(eval.apply(m.group(1))));
            }
            m.appendTail(sb);
            return sb.toString();
        }

        static String list(Collection<? extends Object> list) {
            return list.stream()
                    .map(Object::toString)
                    .collect(Collectors.joining("\n    "));
        }
    }
}

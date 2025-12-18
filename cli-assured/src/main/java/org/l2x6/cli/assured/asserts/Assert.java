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
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
            copy.stream().forEach(a -> a.evaluate(failureCollector));
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
        private final List<String> failures = new ArrayList<>();
        private final List<Throwable> exceptions = new ArrayList<>();

        public FailureCollector(String command) {
            this.command = Objects.requireNonNull(command, "command");
        }

        /**
         * Add the specified failure to this {@link FailureCollector}.
         *
         * @param  description the description of the failure to add to this {@link FailureCollector}
         * @return             this {@link FailureCollector}
         * @since              0.0.1
         */
        public FailureCollector failure(String description) {
            failures.add(description);
            return this;
        }

        /**
         * Add the specified {@code exception} to this {@link FailureCollector}.
         *
         * @param  exception the exception to add to this {@link FailureCollector}
         * @return           this {@link FailureCollector}
         * @since            0.0.1
         */
        public FailureCollector exception(Throwable exception) {
            exceptions.add(exception);
            return this;
        }

        /**
         * @throws AssertionError if any failures or exceptions were added to this {@link FailureCollector}
         */
        public void assertSatisfied() {
            if (!failures.isEmpty() || !exceptions.isEmpty()) {
                final StringJoiner exceptionsAndFailures = new StringJoiner(" and ");
                if (!exceptions.isEmpty()) {
                    exceptionsAndFailures.add("" + exceptions.size() + " exceptions");
                }
                if (!failures.isEmpty()) {
                    exceptionsAndFailures.add("" + failures.size() + " assertion failures");
                }
                StringBuilder message = new StringBuilder()
                        .append(exceptionsAndFailures.toString())
                        .append(" occurred while executing\n\n    ")
                        .append(command);
                if (!exceptions.isEmpty()) {
                    int i = 1;
                    AppendableWriter w = new AppendableWriter(message);
                    for (Throwable e : exceptions) {
                        assertTwoTrailingNewLines(message);
                        message.append("Exception ").append(i++).append('/').append(exceptions.size()).append(": ");
                        e.printStackTrace(w);
                    }
                }
                if (!failures.isEmpty()) {
                    int i = 1;
                    for (String f : failures) {
                        assertTwoTrailingNewLines(message);
                        message.append("Failure ").append(i++).append('/').append(failures.size()).append(": ")
                                .append(f);
                    }
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
                m.appendReplacement(sb, eval.apply(m.group(1)));
            }
            m.appendTail(sb);
            return sb.toString();
        }

        static String list(Collection<? extends Object> list) {
            return list.stream().map(Object::toString).collect(Collectors.joining("\n    "));
        }
    }
}

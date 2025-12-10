/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured;

import java.io.BufferedReader;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import java.util.Objects;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.l2x6.cli.assured.asserts.LineAssert;

public class OutputStreamAsserts implements LineAssert {

    private final List<LineAssert> asserts;
    private final Charset charset;
    private final Supplier<OutputStream> redirect;

    OutputStreamAsserts(List<LineAssert> asserts, Charset charset, Supplier<OutputStream> redirect) {
        this.asserts = Objects.requireNonNull(asserts, "asserts");
        this.charset = Objects.requireNonNull(charset, "charset");
        this.redirect = redirect;
    }

    @Override
    public void assertSatisfied() {
        asserts.stream().forEach(LineAssert::assertSatisfied);
    }

    @Override
    public OutputStreamAsserts line(String line) {
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

    public static class OutputAsserts extends OutputConsumer {
        private final OutputStreamAsserts lineAsserts;

        OutputAsserts(InputStream inputStream, OutputStreamAsserts lineAsserts) {
            super(inputStream);
            this.lineAsserts = lineAsserts;
        }

        @Override
        public void run() {
            if (lineAsserts.hasLineAsserts()) {
                try (BufferedReader r = new BufferedReader(
                        new InputStreamReader(redirect(in, lineAsserts.redirect()), lineAsserts.charset()))) {
                    String line;
                    while (!cancelled && (line = r.readLine()) != null) {
                        lineAsserts.line(line);
                    }
                } catch (IOException e) {
                    exception = e;
                }
            } else {
                try (InputStream wrappedIn = redirect(in, lineAsserts.redirect())) {
                    byte[] buff = new byte[8192];
                    while (wrappedIn.read(buff) > 0) {
                    }
                } catch (IOException e) {
                    exception = e;
                }
            }
        }

        static InputStream redirect(InputStream in, Supplier<OutputStream> redirect) {
            if (redirect == null) {
                return in;
            }
            return new RedirectInputStream(in, redirect.get());
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

    static class RedirectInputStream extends FilterInputStream {

        private final OutputStream out;

        protected RedirectInputStream(InputStream in, OutputStream out) {
            super(in);
            this.out = out;
        }

        @Override
        public int read() throws IOException {
            final int c = super.read();
            if (c >= 0) {
                out.write(c);
            }
            return c;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            final int cnt = super.read(b, off, len);
            if (cnt > 0) {
                out.write(b, off, cnt);
            }
            return cnt;
        }

        @Override
        public void close() throws IOException {
            try {
                super.close();
            } finally {
                out.close();
            }
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

    public static class Builder {

        private final OutputConsumer.Builder outputAsserts;

        private List<LineAssert> asserts = new ArrayList<>();
        private Charset charset = StandardCharsets.UTF_8;
        private Supplier<OutputStream> redirect;

        Builder(OutputConsumer.Builder outputAsserts) {
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
         * @return the parent {@link OutputConsumer.Builder}
         */
        public OutputConsumer.Builder parent() {
            return outputAsserts.createConsumer(in -> new OutputAsserts(in, this.build()));
        }

        OutputStreamAsserts build() {
            List<LineAssert> as = Collections.unmodifiableList(asserts);
            this.asserts = null;
            return new OutputStreamAsserts(as, charset, redirect);
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

        public OutputConsumer.Builder stderr() {
            return parent().stderr();
        }

    }
}

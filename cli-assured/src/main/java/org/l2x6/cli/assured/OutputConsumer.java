/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured;

import java.io.BufferedReader;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.function.Supplier;
import org.l2x6.cli.assured.asserts.Assert;

public abstract class OutputConsumer extends Thread implements Assert {
    protected volatile boolean cancelled;
    protected IOException exception;
    protected final InputStream in;

    OutputConsumer(InputStream in) {
        this.in = in;
        start();
    }

    @Override
    public abstract void run();

    public void cancel() {
        this.cancelled = true;
    }

    public static class DevNull extends OutputConsumer {

        public DevNull(InputStream in) {
            super(in);
        }

        @Override
        public void assertSatisfied() {
        }

        @Override
        public void run() {
            byte[] bytes = new byte[1024];
            try {
                while (!cancelled && in.read(bytes) >= 0) {
                }
            } catch (IOException e) {
                exception = e;
            }
        }

    }

    public static class OutputAsserts extends OutputConsumer {
        private final StreamExpectations lineAsserts;

        OutputAsserts(InputStream inputStream, StreamExpectations lineAsserts) {
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

}

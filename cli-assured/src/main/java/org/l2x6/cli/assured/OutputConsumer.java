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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.l2x6.cli.assured.asserts.Assert;

public abstract class OutputConsumer extends Thread implements Assert {
    protected volatile boolean cancelled;
    protected List<Throwable> exceptions = new ArrayList<>();
    protected final InputStream in;
    protected final AtomicInteger byteCount = new AtomicInteger();

    OutputConsumer(InputStream in) {
        this.in = in;
    }

    @Override
    public void assertSatisfied() {
        synchronized (exceptions) {
            if (!exceptions.isEmpty()) {
                final AssertionError ae = new AssertionError(
                        "There were exceptions caught while processing the output stream:");
                exceptions.forEach(ae::addSuppressed);
                throw ae;
            }
        }
    }

    @Override
    public abstract void run();

    public void cancel() {
        this.cancelled = true;
    }

    public long byteCount() {
        return byteCount.get();
    }

    public static class DevNull extends OutputConsumer {

        public DevNull(InputStream in) {
            super(in);
        }

        @Override
        public void run() {
            byte[] bytes = new byte[1024];
            try {
                int cnt;
                while (!cancelled && (cnt = in.read(bytes)) >= 0) {
                    byteCount.addAndGet(cnt);
                }
            } catch (Throwable e) {
                exceptions.add(e);
            }
        }

    }

    public static class OutputAsserts extends OutputConsumer {
        private final StreamExpectations streamExpectations;

        OutputAsserts(InputStream inputStream, StreamExpectations streamExpectations) {
            super(inputStream);
            this.streamExpectations = Objects.requireNonNull(streamExpectations, "streamExpectations");
        }

        @Override
        public void run() {
            if (streamExpectations.hasLineAsserts()) {
                try (BufferedReader r = new BufferedReader(
                        new InputStreamReader(redirect(in, streamExpectations.redirect()), streamExpectations.charset()))) {
                    String line;
                    while (!cancelled && (line = r.readLine()) != null) {
                        streamExpectations.line(line);
                    }
                } catch (Exception e) {
                    exceptions.add(e);
                }
            } else {
                try (InputStream wrappedIn = redirect(in, streamExpectations.redirect())) {
                    byte[] buff = new byte[8192];
                    while (wrappedIn.read(buff) >= 0) {
                    }
                } catch (Exception e) {
                    exceptions.add(e);
                }
            }
        }

        InputStream redirect(InputStream in, Supplier<OutputStream> redirect) {
            if (redirect == null) {
                return new CountedInputStream(in, byteCount);
            }
            return new RedirectInputStream(in, redirect.get(), byteCount);
        }

        public void assertSatisfied() {
            super.assertSatisfied();
            streamExpectations.assertSatisfied(byteCount());
        }

    }

    static class CountedInputStream extends FilterInputStream {
        private final AtomicInteger byteCount;

        protected CountedInputStream(InputStream in, AtomicInteger byteCount) {
            super(in);
            this.byteCount = byteCount;
        }

        @Override
        public int read() throws IOException {
            final int c = super.read();
            if (c >= 0) {
                byteCount.incrementAndGet();
            }
            return c;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            final int cnt = super.read(b, off, len);
            if (cnt > 0) {
                byteCount.addAndGet(cnt);
            }
            return cnt;
        }
    }

    static class RedirectInputStream extends FilterInputStream {

        private final OutputStream out;
        private final AtomicInteger byteCount;

        protected RedirectInputStream(InputStream in, OutputStream out, AtomicInteger byteCount) {
            super(in);
            this.out = out;
            this.byteCount = byteCount;
        }

        @Override
        public int read() throws IOException {
            final int c = super.read();
            if (c >= 0) {
                out.write(c);
            }
            if (c > 0) {
                byteCount.incrementAndGet();
            }
            return c;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            final int cnt = super.read(b, off, len);
            if (cnt > 0) {
                out.write(b, off, cnt);
                byteCount.addAndGet(cnt);
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

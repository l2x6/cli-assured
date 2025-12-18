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
import org.l2x6.cli.assured.CliAssertUtils.ExcludeFromJacocoGeneratedReport;
import org.l2x6.cli.assured.StreamExpectationsSpec.StreamExpectations;
import org.l2x6.cli.assured.asserts.Assert;

/**
 * Hosts a thread for consuming {@code stdout} or {@code stderr} of a process.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 * @since  0.0.1
 */
abstract class OutputConsumer implements Assert {
    private final Thread thread;
    volatile boolean cancelled;
    final List<Throwable> exceptions = new ArrayList<>();
    final InputStream in;
    final StreamExpectationsSpec.ProcessOutput stream;
    final AtomicInteger byteCount = new AtomicInteger();

    OutputConsumer(InputStream in, StreamExpectationsSpec.ProcessOutput stream, int threadIndex) {
        this.thread = new Thread(this::run, "CliAssured-" + stream + "-" + threadIndex);
        this.in = in;
        this.stream = stream;
    }

    @Override
    public FailureCollector evaluate(FailureCollector failureCollector) {
        synchronized (exceptions) {
            exceptions.forEach(failureCollector::exception);
        }
        return failureCollector;
    }

    void start() {
        thread.start();
    }

    void join() throws InterruptedException {
        thread.join();
    }

    abstract void run();

    void cancel() {
        this.cancelled = true;
    }

    long byteCount() {
        return byteCount.get();
    }

    /**
     * Consumes the output of the {@link Process} ignoring the content but still counting the produced bytes.
     *
     * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
     * @since  0.0.1
     */
    static class DevNull extends OutputConsumer {

        public DevNull(InputStream in, StreamExpectationsSpec.ProcessOutput stream, int threadIndex) {
            super(in, stream, threadIndex);
        }

        @Override
        @ExcludeFromJacocoGeneratedReport
        void run() {
            byte[] bytes = new byte[1024];
            try {
                int cnt;
                while (!cancelled && (cnt = in.read(bytes)) >= 0) {
                    byteCount.addAndGet(cnt);
                }
            } catch (Throwable e) {
                synchronized (exceptions) {
                    exceptions.add(new RuntimeException("Exception caught while consuming " + stream, e));
                }
            }
        }

    }

    /**
     * Consumes the output of the {@link Process} passing the content to {@link StreamExpectations} and counting the
     * produced bytes.
     *
     * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
     * @since  0.0.1
     */
    static class OutputAsserts extends OutputConsumer {
        private final StreamExpectations streamExpectations;

        OutputAsserts(InputStream inputStream, StreamExpectations streamExpectations, int threadIndex) {
            super(inputStream, streamExpectations.stream, threadIndex);
            this.streamExpectations = Objects.requireNonNull(streamExpectations, "streamExpectations");
        }

        @Override
        @ExcludeFromJacocoGeneratedReport
        void run() {
            if (streamExpectations.hasLineAsserts()) {
                try (BufferedReader r = new BufferedReader(
                        new InputStreamReader(redirect(in, streamExpectations.redirect()), streamExpectations.charset()))) {
                    String line;
                    while (!cancelled && (line = r.readLine()) != null) {
                        streamExpectations.line(line);
                    }
                } catch (Exception e) {
                    synchronized (exceptions) {
                        exceptions.add(new RuntimeException("Exception caught while consuming " + stream, e));
                    }
                }
            } else {
                try (InputStream wrappedIn = redirect(in, streamExpectations.redirect())) {
                    byte[] buff = new byte[8192];
                    while (wrappedIn.read(buff) >= 0) {
                    }
                } catch (Exception e) {
                    synchronized (exceptions) {
                        exceptions.add(new RuntimeException("Exception caught while consuming " + stream, e));
                    }
                }
            }
        }

        InputStream redirect(InputStream in, Supplier<OutputStream> redirect) {
            if (redirect == null) {
                return new CountedInputStream(in, byteCount);
            }
            return new RedirectInputStream(in, redirect.get(), byteCount);
        }

        public FailureCollector evaluate(FailureCollector failureCollector) {
            super.evaluate(failureCollector);
            streamExpectations.assertSatisfied(byteCount(), failureCollector);
            return failureCollector;
        }

    }

    static class CountedInputStream extends FilterInputStream {
        private final AtomicInteger byteCount;

        protected CountedInputStream(InputStream in, AtomicInteger byteCount) {
            super(in);
            this.byteCount = byteCount;
        }

        @Override
        @ExcludeFromJacocoGeneratedReport
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
        @ExcludeFromJacocoGeneratedReport
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

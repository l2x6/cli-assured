/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import org.l2x6.cli.assured.CliAssertUtils.ExcludeFromJacocoGeneratedReport;
import org.l2x6.cli.assured.asserts.Assert;

/**
 * Writes to {@code stdin} of a process on a dedicated thread.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 * @since  0.0.1
 */
class InputProducer implements Assert {
    private final CancellableOutputStream stdin;
    private final Consumer<OutputStream> consumer;
    final List<Throwable> exceptions = new ArrayList<>();

    InputProducer(OutputStream stdin, Consumer<OutputStream> consumer) {
        this.stdin = new CancellableOutputStream(stdin);
        this.consumer = consumer;
    }

    void cancel() {
        stdin.cancel();
    }

    void start(ExecutorService executor) {
        executor.submit(this::run);
    }

    @Override
    public FailureCollector evaluate(FailureCollector failureCollector) {
        synchronized (exceptions) {
            exceptions.forEach(e -> failureCollector.exception(null, e));
        }
        return failureCollector;
    }

    @ExcludeFromJacocoGeneratedReport
    void run() {
        try {
            consumer.accept(stdin);
        } catch (CancellationException e) {
            synchronized (exceptions) {
                exceptions.add(e);
            }
        } catch (Throwable e) {
            synchronized (exceptions) {
                exceptions.add(new RuntimeException("Exception caught while writing to stdin", e));
            }
        } finally {
            try {
                stdin.close();
            } catch (IOException e) {
                synchronized (exceptions) {
                    exceptions.add(new RuntimeException("Exception caught while closing stdin", e));
                }
            }
        }
    }

    class CancellableOutputStream extends OutputStream {
        private final OutputStream delegate;
        volatile boolean cancelled;

        private CancellableOutputStream(OutputStream delegate) {
            this.delegate = delegate;
        }

        @ExcludeFromJacocoGeneratedReport
        void cancel() {
            this.cancelled = true;
            try {
                delegate.close();
            } catch (IOException e) {
                synchronized (exceptions) {
                    exceptions.add(new RuntimeException("Exception caught while closing to stdin on cancel", e));
                }
            }
        }

        @ExcludeFromJacocoGeneratedReport
        public void write(int b) throws IOException {
            if (cancelled) {
                throw new CancellationException("The process was cancelled");
            }
            delegate.write(b);
        }

        public void write(byte[] b) throws IOException {
            if (cancelled) {
                throw new CancellationException("The process was cancelled");
            }
            delegate.write(b);
        }

        @ExcludeFromJacocoGeneratedReport
        public void write(byte[] b, int off, int len) throws IOException {
            if (cancelled) {
                throw new CancellationException("The process was cancelled");
            }
            delegate.write(b, off, len);
        }

        @ExcludeFromJacocoGeneratedReport
        public void flush() throws IOException {
            if (cancelled) {
                throw new CancellationException("The process was cancelled");
            }
            delegate.flush();
        }

        public void close() throws IOException {
            if (!cancelled) {
                delegate.close();
            }
        }
    }

}

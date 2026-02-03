/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.l2x6.cli.assured.CliAssertUtils.ExcludeFromJacocoGeneratedReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mutable (unlike the rest of CLI Assured API) global thread pool configuration.
 * The global thread pool must be configured before {@link GlobalThreadPoolSpec#getOrCreateExecutorService()} is called
 * for the first time.
 * Otherwise an {@link IllegalStateException} is thrown.
 *
 * @since 0.0.1
 */
public class GlobalThreadPoolSpec {
    private static final Logger log = LoggerFactory.getLogger(GlobalThreadPoolSpec.class);
    static final Duration DEFAULT_KEEP_ALIVE = Duration.ofSeconds(60);
    static final int DEFAULT_MAX_SIZE = Integer.MAX_VALUE;
    static final int DEFAULT_CORE_SIZE = 0;
    private int coreSize = DEFAULT_CORE_SIZE;
    private int maximumSize = DEFAULT_MAX_SIZE;
    private Duration keepAlive = DEFAULT_KEEP_ALIVE;

    private final Object lock = new Object();
    private ExecutorService executor;

    /**
     * @return a new or already created {@link ExecutorService}
     * @since  0.0.1
     */
    @ExcludeFromJacocoGeneratedReport
    public ExecutorService getOrCreateExecutorService() {
        synchronized (lock) {
            if (executor == null) {
                if (log.isTraceEnabled()) {
                    log.trace("Creating global ThreadPoolExecutor",
                            new RuntimeException("Creating global ThreadPoolExecutor"));
                }
                executor = new GlobalExecutorService(new ThreadPoolExecutor(
                        coreSize,
                        maximumSize,
                        keepAlive.toMillis(), TimeUnit.MILLISECONDS,
                        new SynchronousQueue<Runnable>(),
                        new CliAssuredThreadFactory("cli-assert-io-")));
            }
            return executor;
        }
    }

    /**
     * @param  coreSize the number of threads to keep in the pool, even if they are idle
     * @return          this {@link GlobalThreadPoolSpec}
     * @since           0.0.1
     */
    @ExcludeFromJacocoGeneratedReport
    public GlobalThreadPoolSpec coreSize(int coreSize) {
        synchronized (lock) {
            if (this.coreSize != coreSize) {
                assertMutable();
                this.coreSize = coreSize;
            }
        }
        return this;
    }

    /**
     * @param  maxSize the number of threads to keep in the pool, even if they are idle
     * @return         this {@link GlobalThreadPoolSpec}
     * @since          0.0.1
     */
    @ExcludeFromJacocoGeneratedReport
    public GlobalThreadPoolSpec maxSize(int maxSize) {
        synchronized (lock) {
            if (this.maximumSize != maxSize) {
                assertMutable();
                this.maximumSize = maxSize;
            }
        }
        return this;
    }

    /**
     * @param  keepAlive a time duration after which non-core idle threads are removed from the pool.
     * @return           this {@link GlobalThreadPoolSpec}
     * @since            0.0.1
     */
    @ExcludeFromJacocoGeneratedReport
    public GlobalThreadPoolSpec keepAlive(Duration keepAlive) {
        synchronized (lock) {
            if (!Objects.equals(this.keepAlive, keepAlive)) {
                assertMutable();
                this.keepAlive = keepAlive;
            }
        }
        return this;
    }

    /**
     * @return a {@link CommandSpec} with the java exectuable of the current JVM set as
     *         {@link CommandSpec#executable(String)}
     * @since  0.0.1
     */
    public CommandSpec java() {
        return new CommandSpec(CommandSpec::javaExecutable, Collections.emptyList());
    }

    /**
     * @param  executable the executable to set on the returned {@link CommandSpec}
     * @param  args       the arguments to set on the returned {@link CommandSpec}
     * @return            a {@link CommandSpec} with the specified {@code executable} and {@code args} set
     * @since             0.0.1
     */
    public CommandSpec command(String executable, String... args) {
        return new CommandSpec(() -> executable, Collections.unmodifiableList(new ArrayList<>(Arrays.asList(args))));
    }

    @ExcludeFromJacocoGeneratedReport
    void assertMutable() {
        synchronized (lock) {
            if (executor != null) {
                throw new IllegalStateException(GlobalThreadPoolSpec.class.getSimpleName()
                        + " is not mutable once the global ThreadPoolExecutor was created. You may want to set TRACE level on "
                        + GlobalThreadPoolSpec.class.getName() + " to see the stack trace of its creation.");
            }
        }
    }

    static class GlobalExecutorService implements ExecutorService {
        private final ExecutorService delegate;

        private GlobalExecutorService(ExecutorService delegate) {
            this.delegate = delegate;
        }

        @ExcludeFromJacocoGeneratedReport
        public void execute(Runnable command) {
            delegate.execute(command);
        }

        public void shutdown() {
            /* Do not shut down */
        }

        @ExcludeFromJacocoGeneratedReport
        public List<Runnable> shutdownNow() {
            return delegate.shutdownNow();
        }

        @ExcludeFromJacocoGeneratedReport
        public boolean isShutdown() {
            return delegate.isShutdown();
        }

        @ExcludeFromJacocoGeneratedReport
        public boolean isTerminated() {
            return delegate.isTerminated();
        }

        @ExcludeFromJacocoGeneratedReport
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return delegate.awaitTermination(timeout, unit);
        }

        @ExcludeFromJacocoGeneratedReport
        public <T> Future<T> submit(Callable<T> task) {
            return delegate.submit(task);
        }

        @ExcludeFromJacocoGeneratedReport
        public <T> Future<T> submit(Runnable task, T result) {
            return delegate.submit(task, result);
        }

        public Future<?> submit(Runnable task) {
            return delegate.submit(task);
        }

        @ExcludeFromJacocoGeneratedReport
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
            return delegate.invokeAll(tasks);
        }

        @ExcludeFromJacocoGeneratedReport
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
                throws InterruptedException {
            return delegate.invokeAll(tasks, timeout, unit);
        }

        @ExcludeFromJacocoGeneratedReport
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
            return delegate.invokeAny(tasks);
        }

        @ExcludeFromJacocoGeneratedReport
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
                throws InterruptedException, ExecutionException, TimeoutException {
            return delegate.invokeAny(tasks, timeout, unit);
        }
    }
}

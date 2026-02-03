/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Unlike the {@link CliAssured#globalThreadPool() global thread pool}), the {@link LocalThreadPoolSpec} is immutable
 * - i.e. each customization method returns a new adjusted copy.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 * @since  0.0.1
 */
public class LocalThreadPoolSpec {
    private static final AtomicInteger poolNumber = new AtomicInteger(1);

    private final int coreSize;
    private final int maximumSize;
    private final Duration keepAlive;
    private final CommandSpec command;

    LocalThreadPoolSpec(CommandSpec command) {
        this.command = command;
        this.coreSize = GlobalThreadPoolSpec.DEFAULT_CORE_SIZE;
        this.maximumSize = GlobalThreadPoolSpec.DEFAULT_MAX_SIZE;
        this.keepAlive = GlobalThreadPoolSpec.DEFAULT_KEEP_ALIVE;
    }

    LocalThreadPoolSpec(CommandSpec command, int coreSize, int maximumSize, Duration keepAlive) {
        this.command = command;
        this.coreSize = coreSize;
        this.maximumSize = maximumSize;
        this.keepAlive = keepAlive;
    }

    /**
     * @return each time a new {@link ExecutorService}
     * @since  0.0.1
     */
    public ExecutorService createExecutorService() {
        return new ThreadPoolExecutor(
                coreSize,
                maximumSize,
                keepAlive.toMillis(), TimeUnit.MILLISECONDS,
                new SynchronousQueue<Runnable>(),
                new CliAssuredThreadFactory("cli-assert-io-" + poolNumber.getAndIncrement() + "-"));
    }

    /**
     * @param  coreSize the number of threads to keep in the pool, even if they are idle
     * @return          an adjusted copy of this {@link LocalThreadPoolSpec}
     * @since           0.0.1
     */
    public LocalThreadPoolSpec coreSize(int coreSize) {
        return new LocalThreadPoolSpec(command, coreSize, maximumSize, keepAlive);
    }

    /**
     * @param  maxSize the number of threads to keep in the pool, even if they are idle
     * @return         an adjusted copy of this {@link LocalThreadPoolSpec}
     * @since          0.0.1
     */
    public LocalThreadPoolSpec maxSize(int maxSize) {
        return new LocalThreadPoolSpec(command, coreSize, maxSize, keepAlive);
    }

    /**
     * @param  keepAlive a time duration after which non-core idle threads are removed from the pool.
     * @return           an adjusted copy of this {@link LocalThreadPoolSpec}
     * @since            0.0.1
     */
    public LocalThreadPoolSpec keepAlive(Duration keepAlive) {
        return new LocalThreadPoolSpec(command, coreSize, maximumSize, keepAlive);
    }

    /**
     * @return a new {@link ExpectationsSpec}
     * @since  0.0.1
     */
    public ExpectationsSpec then() {
        return parent().then();
    }

    /**
     * Pass this {@link ExpectationsSpec} to the parent {@link CommandSpec} and start the {@link CommandProcess}.
     *
     * @return a new {@link CommandProcess}
     * @since  0.0.1
     * @see    #execute()
     */
    public CommandProcess start() {
        return parent().start();
    }

    /**
     * Pass this {@link ExpectationsSpec} to the parent {@link CommandSpec},
     * start the {@link CommandProcess} and awaits (potentially indefinitely) its termination.
     * A shorthand for {@link #start()}.{@link CommandProcess#awaitTermination() awaitTermination()}
     *
     * @return a {@link CommandResult}
     * @since  0.0.1
     */
    public CommandResult execute() {
        return parent().execute();
    }

    /**
     * Pass this {@link ExpectationsSpec} to the parent {@link CommandSpec},
     * start the {@link CommandProcess} and await its termination at most for the specified
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
     * Pass this {@link ExpectationsSpec} to the parent {@link CommandSpec},
     * start the {@link CommandProcess} and await its termination at most for the specified
     * timeout in milliseconds.
     * A shorthand for {@link #start()}.{@link CommandProcess#awaitTermination(Duration) awaitTermination(Duration)}
     *
     * @param  timeoutMs maximum time in milliseconds to wait for the underlying process to terminate
     *
     * @return           a {@link CommandResult}
     * @since            0.0.1
     */
    public CommandResult execute(long timeoutMs) {
        return parent().execute(timeoutMs);
    }

    CommandSpec parent() {
        return command.threadPool(this::createExecutorService);
    }
}

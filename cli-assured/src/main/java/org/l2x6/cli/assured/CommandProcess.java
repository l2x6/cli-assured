/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured;

import java.time.Duration;
import java.util.Objects;
import org.l2x6.cli.assured.CliAssertUtils.ExcludeFromJacocoGeneratedReport;
import org.l2x6.cli.assured.asserts.Assert;
import org.l2x6.cli.assured.asserts.ExitCodeAssert;

/**
 * A wrapper around {@link Process} that manages its destroying and offers
 * {@link #awaitTermination()} with an optional timeout.
 *
 * @since  0.0.1
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class CommandProcess implements AutoCloseable {

    private final String cmdString;
    private final Process process;
    private final Thread shutDownHook;
    private final InputProducer stdin;
    private final OutputConsumer out;
    private final OutputConsumer err;
    private final boolean autoCloseForcibly;
    private final Duration autoCloseTimeout;

    private volatile boolean closed = false;
    private final Assert asserts;
    private final ExitCodeAssert exitCodeAssert;
    private long pid;

    CommandProcess(
            String cmdArrayString,
            Process process,
            Assert asserts,
            ExitCodeAssert exitCodeAssert,
            InputProducer stdin,
            OutputConsumer out,
            OutputConsumer err,
            boolean autoCloseForcibly,
            Duration autoCloseTimeout) {
        super();
        this.cmdString = Objects.requireNonNull(cmdArrayString, "cmdArrayString");
        this.process = Objects.requireNonNull(process, "process");
        this.asserts = Objects.requireNonNull(asserts, "asserts");
        this.exitCodeAssert = Objects.requireNonNull(exitCodeAssert, "exitCodeAssert");
        this.stdin = stdin;
        this.out = Objects.requireNonNull(out, "out");
        this.err = Objects.requireNonNull(err, "err");
        this.autoCloseForcibly = autoCloseForcibly;
        this.autoCloseTimeout = autoCloseTimeout;
        this.pid = PidLookup.getPid(process);
        this.shutDownHook = new Thread(new Runnable() {
            @Override
            public void run() {
                kill(false);
            }
        });
        Runtime.getRuntime().addShutdownHook(shutDownHook);
    }

    /**
     * Calls {@link OutputConsumer#cancel()} on both {@link #out} and {@link #err} and kills the underlying process.
     *
     * @param forcibly if {@code true} will call {@link Process#destroyForcibly()}; otherwise will call
     *                 {@link Process#destroy()}
     * @since          0.0.1
     */
    @ExcludeFromJacocoGeneratedReport
    public void kill(boolean forcibly) {
        if (!closed) {
            this.closed = true;
            out.cancel();
            if (err != null) {
                err.cancel();
            }
            if (stdin != null) {
                stdin.cancel();
            }
        }

        if (process != null && process.isAlive()) {
            if (forcibly) {
                process.destroy();
            } else {
                process.destroyForcibly();
            }
        }

    }

    /**
     * Awaits (potentially indefinitely) the termination of the underlying {@link Process}.
     *
     * @return a {@link CommandResult}
     * @since  0.0.1
     */
    @ExcludeFromJacocoGeneratedReport
    public CommandResult awaitTermination() {
        final long startMillisTime = System.currentTimeMillis();
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted", e);
        }
        return terminated(startMillisTime);
    }

    /**
     * Awaits the termination of the underlying {@link Process} at most for the specified time duration.
     *
     * @param  timeout maximum time to wait for the underlying process to terminate
     *
     * @return         a {@link CommandResult}
     * @since          0.0.1
     */
    public CommandResult awaitTermination(Duration timeout) {
        return awaitTermination(timeout.toMillis());
    }

    /**
     * Awaits the termination of the underlying {@link Process} at most for the specified amount of milliseconds.
     *
     * @param  timeoutMs maximum time in milliseconds to wait for the underlying process to terminate
     *
     * @return           a {@link CommandResult}
     * @since            0.0.1
     */
    @ExcludeFromJacocoGeneratedReport
    public CommandResult awaitTermination(long timeoutMs) {
        final long startMillisTime = System.currentTimeMillis();

        do {
            try {
                return terminated(startMillisTime);
            } catch (IllegalThreadStateException ex) {
                final long duration = System.currentTimeMillis() - startMillisTime;
                if (duration < timeoutMs) {
                    try {
                        Thread.sleep(Math.min(timeoutMs - duration, 100));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted", e);
                    }
                }
            }
        } while (System.currentTimeMillis() - startMillisTime <= timeoutMs);
        return new CommandResult(
                cmdString,
                -1,
                Duration.ofMillis(System.currentTimeMillis() - startMillisTime),
                out.byteCount(),
                err.byteCount(),
                new TimeoutAssertionError("Command has not terminated within " + timeoutMs + " ms"),
                asserts);
    }

    @ExcludeFromJacocoGeneratedReport
    CommandResult terminated(long startMillisTime) {
        int exitCode = process.exitValue();
        try {
            Runtime.getRuntime().removeShutdownHook(shutDownHook);
        } catch (Exception ignored) {
        }

        try {
            out.join();

            if (err != null) {
                err.join();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted", e);
        }

        exitCodeAssert.exitCode(exitCode);
        return new CommandResult(
                cmdString,
                exitCode,
                Duration.ofMillis(System.currentTimeMillis() - startMillisTime),
                out.byteCount(),
                err != null ? err.byteCount() : 0,
                null,
                asserts);
    }

    public String toString() {
        return cmdString;
    }

    /**
     * Kills the underlying process and awaits its termination.
     * The default behavior is to call {@link #kill(boolean) kill(false)} and {@link #awaitTermination()} without timeout.
     * You can change this by calling {@link CommandSpec#autoCloseForcibly()} and/or
     * {@link CommandSpec#autoCloseTimeout(Duration)}.
     * <p>
     * Use {@code try(CommandProcess proc = ...)} only if you do not call any of
     * {@link CommandProcess#awaitTermination() awaitTermination(*)} methods direcly or
     * indirectly via {@link CommandSpec#execute() CommandSpec.execute(*)},
     * {@link ExpectationsSpec#execute() ExpectationsSpec.execute(*)} or
     * {@link StreamExpectationsSpec#execute() StreamExpectationsSpec.execute(*)}.
     * <p>
     * An example of a situation in which you should definitely use {@code try(CommandProcess proc = ...)}
     * is when you perform some interactive actions against a process that won't exit by itself:
     *
     * <pre>
     * <code>
     * CountDownLatch started = new CountDownLatch(1);
     * try (CommandProcess mvn = CliAssured
     *         .command("mvn", "quarkus:dev")
     *         .cd(..)
     *         .then()
     *         .stdout()
     *         .log(line -> {
     *             if (line.contains("Installed features: [")) {
     *                 started.countDown();
     *             }
     *         })
     *         .start()) {
     *
     *     started.await(20, TimeUnit.SECONDS);
     *
     *     // Do some testing here, e.g. send a request
     *     RestAssured.get("http:/localhost:8080/rest")
     *         .then()
     *         .statusCode(200);
     *
     * } // mvn process is auto-closed here even if some of the assertions failed
     * </code>
     * </pre>
     *
     * @since 0.0.1
     */
    @Override
    public void close() {
        kill(autoCloseForcibly);
        if (autoCloseTimeout != null) {
            awaitTermination(autoCloseTimeout);
        } else {
            awaitTermination();
        }
    }

    /**
     * @return                               the process ID (PID) of the underlying operating system process
     * @throws UnsupportedOperationException on Java versions lower than 9
     * @since                                0.0.1
     */
    @ExcludeFromJacocoGeneratedReport
    public long pid() {
        if (pid < 0) {
            throw new UnsupportedOperationException(
                    Process.class.getName() + ".pid() is not supported before Java version 9; current Java version: "
                            + System.getProperty("java.version"));
        }
        return pid;
    }

}

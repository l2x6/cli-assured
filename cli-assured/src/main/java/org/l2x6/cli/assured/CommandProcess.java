/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured;

import java.io.Closeable;
import java.time.Duration;
import org.l2x6.cli.assured.asserts.OutputAssert;

/**
 * A wrapper around {@link Process} that manages its destroying and offers
 * {@link #awaitTermination()} honoring the timeout passed via {@link Command#timeoutMs}.
 *
 * @since  0.0.1
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class CommandProcess implements Closeable {

    private final Command command;
    private final Process process;
    private final Thread shutDownHook;
    private final OutputAsserts out;
    private final OutputAsserts err;

    private volatile boolean closed = false;

    CommandProcess(Command command, Process process) {
        super();
        this.command = command;
        this.process = process;

        out = command.expectations.stdout().apply(process.getInputStream());

        if (command.stderrToStdout) {
            err = null;
        } else {
            err = command.expectations.stderr().apply(process.getErrorStream());
        }

        this.shutDownHook = new Thread(new Runnable() {
            @Override
            public void run() {
                close();
            }
        });
        Runtime.getRuntime().addShutdownHook(shutDownHook);
    }

    /**
     * A shorthand for {@link #kill(boolean) kill(false)}
     *
     * @since 0.0.1
     */
    @Override
    public void close() {
        kill(false);
    }

    /**
     * Calls {@link OutputAsserts#cancel()} on both {@link #out} and {@link #err} and kills the underlying process.
     *
     * @param forcibly if {@code true} will call {@link Process#destroyForcibly()}; otherwise will call
     *                 {@link Process#destroy()}
     * @since          0.0.1
     */
    public void kill(boolean forcibly) {
        if (!closed) {
            this.closed = true;
            out.cancel();
            if (err != null) {
                err.cancel();
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
                command,
                -1,
                Duration.ofMillis(System.currentTimeMillis() - startMillisTime),
                new TimeoutAssertionError(
                        String.format("Command has not terminated within %d ms: %s", timeoutMs, command.cmdArrayString)),
                joinAsserts());
    }

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

        return new CommandResult(
                command,
                exitCode,
                Duration.ofMillis(System.currentTimeMillis() - startMillisTime),
                null,
                joinAsserts());
    }

    OutputAssert joinAsserts() {
        return err == null ? out : OutputAssert.all(out, err);
    }

}

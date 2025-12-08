/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured;

import java.time.Duration;
import org.l2x6.cli.assured.asserts.OutputAssert;

/**
 * A result of a {@link CommandProcess}'s execution.
 *
 * @since  0.0.1
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class CommandResult {
    private final Command command;
    private final int exitCode;
    private final Duration duration;
    private final Throwable exception;
    private final OutputAssert outputAssert;

    CommandResult(Command command, int exitCode, Duration runtimeMs, Throwable exception, OutputAssert outputAssert) {
        super();
        this.command = command;
        this.exitCode = exitCode;
        this.duration = runtimeMs;
        this.exception = exception;
        this.outputAssert = outputAssert;
    }

    /**
     * Assert that the execution of the command was successful, namely that
     * <ul>
     * <li>The exit code was {@code 0}
     * <li>No exception was thrown
     * </ul>
     *
     * @return this {@link CommandResult}
     *
     * @since  0.0.1
     */
    public CommandResult assertSuccess() {
        if (exception != null) {
            throw new AssertionError("Exception was thrown when running " + command.cmdArrayString + ": "
                    + exception.getMessage() + "\nCommand output\n" + outputAssert.toString(), exception);
        }
        outputAssert.assertSatisfied();
        return this;
    }

    /**
     * Assert that the execution of the command timed out as defined by the passed in {@link Command#timeoutMs}
     *
     * @return this {@link CommandResult}
     *
     * @since  0.0.1
     */
    public CommandResult assertTimeout() {
        if (!(exception instanceof TimeoutAssertionError)) {
            throw new AssertionError(
                    "Expected a timeout when running " + command.cmdArrayString + ": but got exit code " + exitCode
                            + "\nCommand output\n" + outputAssert.toString(),
                    exception);
        }
        return this;
    }

    /**
     * @return the exit code if the underlying {@link Process}
     * @since  0.0.1
     */
    public int exitCode() {
        return exitCode;
    }

    /**
     * @return the duration of the command execution
     * @since  0.0.1
     */
    public Duration durationMs() {
        return duration;
    }

}

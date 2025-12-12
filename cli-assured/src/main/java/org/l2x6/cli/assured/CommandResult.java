/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured;

import java.time.Duration;
import org.l2x6.cli.assured.asserts.Assert;

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
    private final long byteCountStdout;
    private final long byteCountStderr;
    private final Throwable exception;
    private final Assert outputAssert;

    CommandResult(
            Command command,
            int exitCode,
            Duration runtimeMs,
            long byteCountStdout,
            long byteCountStderr,
            Throwable exception,
            Assert outputAssert) {
        super();
        this.command = command;
        this.exitCode = exitCode;
        this.duration = runtimeMs;
        this.byteCountStdout = byteCountStdout;
        this.byteCountStderr = byteCountStderr;
        this.exception = exception;
        this.outputAssert = outputAssert;
    }

    /**
     * Assert that the execution of the command was successful, namely that
     * <ul>
     * <li>No exception was thrown
     * <li>All assertions defined via {@link CommandBuilder#expect()} are satisfied
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

    /**
     * @return the number of bytes produced on {@code stdout}
     * @since  0.0.1
     */
    public long byteCountStdout() {
        return byteCountStdout;
    }

    /**
     * @return the number of bytes produced on {@code stderr}
     * @since  0.0.1
     */
    public long byteCountStderr() {
        return byteCountStderr;
    }

}

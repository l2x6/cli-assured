/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured;

import java.util.function.Consumer;
import org.assertj.core.api.Assertions;

/**
 * A result of a {@link CommandProcess}'s execution.
 *
 * @since  0.0.1
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class CommandResult {
    private final Command command;
    private final int exitCode;
    private final long duration;
    private final Throwable exception;
    private final CommandOutput output;

    CommandResult(Command command, int exitCode, long runtimeMs, Throwable exception, CommandOutput out) {
        super();
        this.command = command;
        this.exitCode = exitCode;
        this.duration = runtimeMs;
        this.exception = exception;
        this.output = out;
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
                    + exception.getMessage() + "\nCommand output\n" + output.toString(), exception);
        }
        if (exitCode != 0) {
            throw new AssertionError(String.format("Command returned exit code %d: %s\nCommand output:\n%s", exitCode,
                    command.cmdArrayString, output));
        }
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
                            + "\nCommand output\n" + output.toString(),
                    exception);
        }
        return this;
    }

    /**
     * Assert that the exit code of the command satisfies the given {@link Consumer}. Handy with AssertJ assertions.
     *
     * @param  consumer
     * @return          this {@link CommandResult}
     * @since           0.0.1
     */
    public CommandResult exitCode(Consumer<? super Integer> consumer) {
        Assertions.assertThat(exitCode)
                .satisfies(consumer);
        return this;
    }

    /**
     * Assert that the command exited with the expected exit code
     *
     * @param  expected the expected exit code
     * @return          this {@link CommandResult}
     * @since           0.0.1
     */
    public CommandResult exitCode(int expected) {
        Assertions.assertThat(exitCode).withFailMessage("Expecting exit code " + expected + "but got " + exitCode)
                .isEqualTo(expected);
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
     * @return the duration of the command execution in milliseconds
     * @since  0.0.1
     */
    public long durationMs() {
        return duration;
    }

    /**
     * Assert that the duration of the command execution satisfies the given {@link Consumer}. Handy with AssertJ
     * assertions.
     *
     * @param  consumer
     * @return          this {@link CommandResult}
     * @since           0.0.1
     */
    public CommandResult duration(Consumer<? super Long> consumer) {
        Assertions.assertThat(duration).satisfies(consumer);
        return this;
    }

    public CommandOutput output() {
        return output;
    }
}

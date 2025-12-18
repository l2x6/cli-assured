/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured;

import java.io.InputStream;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.IntPredicate;
import org.l2x6.cli.assured.OutputConsumer.DevNull;
import org.l2x6.cli.assured.OutputConsumer.OutputAsserts;
import org.l2x6.cli.assured.StreamExpectationsSpec.ProcessOutput;
import org.l2x6.cli.assured.StreamExpectationsSpec.StreamExpectations;
import org.l2x6.cli.assured.asserts.ExitCodeAssert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Assertions applicable to {@code stdout}, {@code stderr} or an exit code of a {@link CommandProcess}.
 *
 * @since  0.0.1
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class ExpectationsSpec {
    static final Logger log = LoggerFactory.getLogger(ExpectationsSpec.class);
    private final CommandSpec command;
    private final boolean stderrToStdout;
    final Function<InputStream, OutputConsumer> stdout;
    final Function<InputStream, OutputConsumer> stderr;
    final ExitCodeAssert exitCodeAssert;

    ExpectationsSpec(CommandSpec command, boolean stderrToStdout) {
        this.command = command;
        this.stderrToStdout = stderrToStdout;
        this.stdout = in -> new DevNull(in, ProcessOutput.stdout);
        this.stderr = in -> new OutputAsserts(in, StreamExpectations.hasNoLines(ProcessOutput.stderr));
        this.exitCodeAssert = ExitCodeAssert.exitCodeIs(0);
    }

    ExpectationsSpec(
            CommandSpec command,
            Function<InputStream, OutputConsumer> stdout,
            Function<InputStream, OutputConsumer> stderr,
            ExitCodeAssert exitCodeAssert,
            boolean stderrToStdout) {
        this.command = command;
        this.stdout = Objects.requireNonNull(stdout, "stdout");
        this.stderr = Objects.requireNonNull(stderr, "stderr");
        this.exitCodeAssert = Objects.requireNonNull(exitCodeAssert, "exitCodeAssert");
        this.stderrToStdout = stderrToStdout;
    }

    /**
     * @return new {@link StreamExpectationsSpec} for defining assertions on {@code stdout}
     * @since  0.0.1
     */
    public StreamExpectationsSpec stdout() {
        return new StreamExpectationsSpec(this::stdout, StreamExpectationsSpec.ProcessOutput.stdout);
    }

    /**
     * @return new {@link StreamExpectationsSpec} for defining assertions on {@code stderr}
     * @since  0.0.1
     */
    public StreamExpectationsSpec stderr() {
        if (stderrToStdout) {
            throw new IllegalStateException(
                    "You cannot set any assertions on stderr while you are redirecting stderr to stdout");
        }
        return new StreamExpectationsSpec(this::stderr, StreamExpectationsSpec.ProcessOutput.stderr);
    }

    /**
     * Assert that the process exits with the given {@code expectedExitCode}.
     *
     * @param  expectedExitCode the exit code to assert
     * @return                  an adjusted copy of this {@link ExpectationsSpec}
     * @since                   0.0.1
     */
    public ExpectationsSpec exitCodeIs(int expectedExitCode) {
        return new ExpectationsSpec(command, stdout, stderr, ExitCodeAssert.exitCodeIs(expectedExitCode), stderrToStdout);
    }

    /**
     * Assert that the process exits with any of the given {@code expectedExitCodes}.
     *
     * @param  expectedExitCodes the exit codes to assert
     * @return                   an adjusted copy of this {@link ExpectationsSpec}
     * @since                    0.0.1
     */
    public ExpectationsSpec exitCodeIsAnyOf(int... expectedExitCodes) {
        return new ExpectationsSpec(command, stdout, stderr, ExitCodeAssert.exitCodeIsAnyOf(expectedExitCodes), stderrToStdout);
    }

    /**
     * Assert that the process exits with an exit code that satisfies the given {@link IntPredicate}.
     *
     * @param  expected    the condition actual exit code must satisfy
     * @param  description the description of a failure typically something like
     *                     {@code "Expected exit code <condition> but actually terminated with exit code ${actual}"} where
     *                     {@code <condition>} is your human readable criteria, like {@code greater that 42},
     *                     and <code>${actual}</code> is a placeholder that CLI Assured will replace
     *                     by the actual exit code of the process
     * @return             an adjusted copy of this {@link ExpectationsSpec}
     * @since              0.0.1
     */
    public ExpectationsSpec exitCodeSatisfies(IntPredicate expected, String description) {
        return new ExpectationsSpec(command, stdout, stderr, ExitCodeAssert.exitCodeSatisfies(expected, description),
                stderrToStdout);
    }

    ExpectationsSpec stdout(Function<InputStream, OutputConsumer> stdoutAsserts) {
        return new ExpectationsSpec(command, stdoutAsserts, stderr, exitCodeAssert, stderrToStdout);
    }

    ExpectationsSpec stderr(Function<InputStream, OutputConsumer> stderrAsserts) {
        return new ExpectationsSpec(command, stdout, stderrAsserts, exitCodeAssert, stderrToStdout);
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
        return command.expect(this);
    }

}

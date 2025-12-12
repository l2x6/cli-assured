/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured;

import java.io.InputStream;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;
import org.l2x6.cli.assured.OutputConsumer.DevNull;
import org.l2x6.cli.assured.OutputConsumer.Stream;
import org.l2x6.cli.assured.asserts.ExitCodeAssert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Assertions applicable to an output of a {@link CommandProcess} or its exit code.
 *
 * @since  0.0.1
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class Expectations {
    private static final Logger log = LoggerFactory.getLogger(Expectations.class);
    final Function<InputStream, OutputConsumer> stdout;
    final Function<InputStream, OutputConsumer> stderr;
    final ExitCodeAssert exitCodeAssert;

    Expectations(
            Function<InputStream, OutputConsumer> stdout,
            Function<InputStream, OutputConsumer> stderr,
            ExitCodeAssert exitCodeAssert) {
        this.stdout = Objects.requireNonNull(stdout, "stdout");
        this.stderr = stderr;
        this.exitCodeAssert = Objects.requireNonNull(exitCodeAssert, "exitCodeAssert");
    }

    public static class ExpectationsBuilder {
        private static final Pattern MATCH_ANY_PATTERN = Pattern.compile(".*");

        private final Command.CommandBuilder command;
        private Function<InputStream, OutputConsumer> stdoutAsserts;
        private Function<InputStream, OutputConsumer> stderrAsserts;
        private ExitCodeAssert exitCodeAssert;

        private boolean stderrToStdout;

        ExpectationsBuilder(org.l2x6.cli.assured.Command.CommandBuilder command) {
            this.command = command;
        }

        /**
         * @return new {@link OutputConsumer.CommandBuilder}
         * @since  0.0.1
         */
        public StreamExpectations.StreamExpectationsBuilder stdout() {
            return new StreamExpectations.StreamExpectationsBuilder(this::stdout, Stream.stdout);
        }

        /**
         * @return new {@link OutputConsumer.CommandBuilder}
         * @since  0.0.1
         */
        public StreamExpectations.StreamExpectationsBuilder stderr() {
            if (stderrToStdout) {
                throw new IllegalStateException(
                        "You cannot set any assertions on stderr while you are redirecting stderr to stdout");
            }
            return new StreamExpectations.StreamExpectationsBuilder(this::stderr, Stream.stderr);
        }

        /**
         * Enable the redirection of {@code stderr} to {@code stdout}
         *
         * @return this {@link ExpectationsBuilder}
         * @since  0.0.1
         */
        public ExpectationsBuilder stderrToStdout() {
            this.stderrToStdout |= true;
            return this;
        }

        /**
         * Assert that the process exits with any the given {@code expectedExitCodes}.
         *
         * @param  expectedExitCodes the exit codes to assert
         * @return                   this {@link ExpectationsBuilder}
         * @since                    0.0.1
         */
        public ExpectationsBuilder exitCode(int... expectedExitCodes) {
            this.exitCodeAssert = ExitCodeAssert.any(expectedExitCodes);
            return this;
        }

        ExpectationsBuilder stdout(Function<InputStream, OutputConsumer> stdoutAsserts) {
            this.stdoutAsserts = stdoutAsserts;
            return this;
        }

        ExpectationsBuilder stderr(Function<InputStream, OutputConsumer> stderrAsserts) {
            this.stderrAsserts = stderrAsserts;
            return this;
        }

        Expectations build() {
            if (stdoutAsserts == null) {
                log.debug("stdout will be ignored because no consumer was specified for it");
                stdoutAsserts = in -> new DevNull(in, Stream.stdout);
            }
            if (stderrAsserts == null && !stderrToStdout) {
                log.debug("Any output to stderr will cause an error because no consumer was specified for it");
                stderrAsserts = stderr().doesNotHaveLinesMatching(MATCH_ANY_PATTERN).build();
            }
            if (exitCodeAssert == null) {
                exitCodeAssert = ExitCodeAssert.of(0);
                log.debug("Adding default exit code assert ExitCodeAssert.of(0) because no exit code assert was specified");
            }
            return new Expectations(stdoutAsserts, stderrAsserts, exitCodeAssert);
        }

        /**
         * Build new {@link Expectations}, pass them to the parent {@link Command.CommandBuilder}
         * and start the command.
         *
         * @return a new {@link CommandProcess}
         * @since  0.0.1
         * @see    #execute()
         */
        public CommandProcess start() {
            return parent().start();
        }

        /**
         * Build new {@link Expectations}, pass them to the parent {@link Command.CommandBuilder},
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
         * Build new {@link Expectations}, pass them to the parent {@link Command.CommandBuilder} and the {@link CommandProcess}
         * and
         * awaits (potentially indefinitely) its termination at most for the specified
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
         * Build new {@link Expectations}, pass them to the parent {@link Command.CommandBuilder} and the {@link CommandProcess}
         * and
         * awaits (potentially indefinitely) its termination at most for the specified
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

        Command.CommandBuilder parent() {
            return command.expect(build());
        }

    }
}

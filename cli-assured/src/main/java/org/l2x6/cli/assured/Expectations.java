/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured;

import java.io.InputStream;
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

    public static class Builder {
        private static final Pattern MATCH_ANY_PATTERN = Pattern.compile(".*");

        private final Command.Builder command;
        private Function<InputStream, OutputConsumer> stdoutAsserts;
        private Function<InputStream, OutputConsumer> stderrAsserts;
        private ExitCodeAssert exitCodeAssert;

        private boolean stderrToStdout;

        Builder(org.l2x6.cli.assured.Command.Builder command) {
            this.command = command;
        }

        /**
         * @return new {@link OutputConsumer.Builder}
         * @since  0.0.1
         */
        public StreamExpectations.Builder stdout() {
            return new StreamExpectations.Builder(this::stdout, Stream.stdout);
        }

        /**
         * @return new {@link OutputConsumer.Builder}
         * @since  0.0.1
         */
        public StreamExpectations.Builder stderr() {
            if (stderrToStdout) {
                throw new IllegalStateException(
                        "You cannot set any assertions on stderr while you are redirecting stderr to stdout");
            }
            return new StreamExpectations.Builder(this::stderr, Stream.stderr);
        }

        /**
         * Enable the redirection of {@code stderr} to {@code stdout}
         *
         * @return this {@link Builder}
         * @since  0.0.1
         */
        public Builder stderrToStdout() {
            this.stderrToStdout |= true;
            return this;
        }

        /**
         * Assert that the process exits with any the given {@code expectedExitCodes}.
         *
         * @param  expectedExitCodes the exit codes to assert
         * @return                   this {@link Builder}
         * @since                    0.0.1
         */
        public Builder exitCode(int... expectedExitCodes) {
            this.exitCodeAssert = ExitCodeAssert.any(expectedExitCodes);
            return this;
        }

        Builder stdout(Function<InputStream, OutputConsumer> stdoutAsserts) {
            this.stdoutAsserts = stdoutAsserts;
            return this;
        }

        Builder stderr(Function<InputStream, OutputConsumer> stderrAsserts) {
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
         * Build new {@link Expectations}, pass them to the parent {@link Command.Builder} and start the process.
         * and start the command.
         *
         * @return a new {@link CommandProcess}
         * @since  0.0.1
         */
        public CommandProcess start() {
            return command.expect(build()).start();
        }

    }
}

/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;
import org.l2x6.cli.assured.OutputConsumer.DevNull;
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
    final List<ExitCodeAssert> exitCodeAsserts;

    Expectations(
            Function<InputStream, OutputConsumer> stdout,
            Function<InputStream, OutputConsumer> stderr,
            List<ExitCodeAssert> exitCodeAsserts) {
        this.stdout = Objects.requireNonNull(stdout, "stdout");
        this.stderr = stderr;
        this.exitCodeAsserts = Objects.requireNonNull(exitCodeAsserts, "exitCodeAsserts");
    }

    public static class Builder {
        private static final Pattern MATCH_ANY_PATTERN = Pattern.compile(".*");

        private final Command.Builder command;
        private Function<InputStream, OutputConsumer> stdoutAsserts;
        private Function<InputStream, OutputConsumer> stderrAsserts;
        private List<ExitCodeAssert> exitCodeAsserts = new ArrayList<>();

        private boolean stderrToStdout;

        Builder(org.l2x6.cli.assured.Command.Builder command) {
            this.command = command;
        }

        /**
         * @return new {@link OutputConsumer.Builder}
         * @since  0.0.1
         */
        public OutputConsumer.Builder stdout() {
            return new OutputConsumer.Builder(this::stdout);
        }

        /**
         * @return new {@link OutputConsumer.Builder}
         * @since  0.0.1
         */
        public OutputConsumer.Builder stderr() {
            if (stderrToStdout) {
                throw new IllegalStateException(
                        "You cannot set any assertions on stderr while you are redirecting stderr to stdout");
            }
            return new OutputConsumer.Builder(this::stderr);
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
         * Assert that the process exits with any the given {@code expectedExitCodes}
         *
         * @param  expectedExitCodes the exit codes to assert
         * @return                   this {@link Builder}
         * @since                    0.0.1
         */
        public Builder exitCode(int... expectedExitCodes) {
            this.exitCodeAsserts.add(ExitCodeAssert.any(expectedExitCodes));
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
            final Function<InputStream, OutputConsumer> stdo;
            if (stdoutAsserts == null) {
                log.debug("stdout will be ignored because no consumer was specified for it");
                stdo = DevNull::new;
            } else {
                stdo = stdoutAsserts;
            }
            if (stderrAsserts == null && !stderrToStdout) {
                log.debug("Any output to stderr will cause an error because no consumer was specified for it");
                stderrAsserts = stderr().lines().doesNotContainMatching(MATCH_ANY_PATTERN).parent().build();
            }
            final List<ExitCodeAssert> eca;
            if (exitCodeAsserts.isEmpty()) {
                eca = Collections.singletonList(ExitCodeAssert.of(0));
                log.debug("Adding default exit code assert ExitCodeAssert.of(0) because no exit code assert was specified");
            } else {
                eca = Collections.unmodifiableList(exitCodeAsserts);
                exitCodeAsserts = null;
            }
            return new Expectations(stdo, stderrAsserts, eca);
        }

        public CommandProcess start() {
            return command.expect(this).start();
        }

    }
}

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
import org.l2x6.cli.assured.OutputAsserts.DummyOutputConsumer;
import org.l2x6.cli.assured.asserts.ExitCodeAssert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Expectations {
    private static final Logger log = LoggerFactory.getLogger(Expectations.class);
    final Function<InputStream, OutputAsserts> stdout;
    final Function<InputStream, OutputAsserts> stderr;
    final List<ExitCodeAssert> exitCodeAsserts;

    private Expectations(
            Function<InputStream, OutputAsserts> stdout,
            Function<InputStream, OutputAsserts> stderr,
            List<ExitCodeAssert> exitCodeAsserts) {
        this.stdout = Objects.requireNonNull(stdout, "stdout");
        this.stderr = stderr;
        this.exitCodeAsserts = Objects.requireNonNull(exitCodeAsserts, "exitCodeAsserts");
    }

    public static Builder builder(Command.Builder command) {
        return new Builder(command);
    }

    public static class Builder {
        private static final Pattern MATCH_ANY_PATTERN = Pattern.compile(".*");

        private final Command.Builder command;
        private Function<InputStream, OutputAsserts> stdoutAsserts;
        private Function<InputStream, OutputAsserts> stderrAsserts;
        private List<ExitCodeAssert> exitCodeAsserts = new ArrayList<>();

        private boolean stderrToStdout;

        private Builder(org.l2x6.cli.assured.Command.Builder command) {
            this.command = command;
        }

        /**
         * @return new {@link OutputAsserts.Builder}
         * @since  0.0.1
         */
        public OutputAsserts.Builder stdout() {
            return new OutputAsserts.Builder(this::stdout);
        }

        /**
         * @return new {@link OutputAsserts.Builder}
         * @since  0.0.1
         */
        public OutputAsserts.Builder stderr() {
            if (stderrToStdout) {
                throw new IllegalStateException(
                        "You cannot set any assertions on stderr while you are redirecting stderr to stdout");
            }
            return new OutputAsserts.Builder(this::stderr);
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

        Builder stdout(Function<InputStream, OutputAsserts> stdoutAsserts) {
            this.stdoutAsserts = stdoutAsserts;
            return this;
        }

        Builder stderr(Function<InputStream, OutputAsserts> stderrAsserts) {
            this.stderrAsserts = stderrAsserts;
            return this;
        }

        Expectations build() {
            final Function<InputStream, OutputAsserts> stdo;
            if (stdoutAsserts == null) {
                log.debug("stdout will be ignored because no consumer was specified for it");
                stdo = DummyOutputConsumer::new;
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

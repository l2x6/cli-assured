/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.l2x6.cli.assured.asserts.Assert;
import org.slf4j.LoggerFactory;

/**
 * A command that can be executed.
 *
 * @since  0.0.1
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
class Command {
    static final org.slf4j.Logger log = LoggerFactory.getLogger(Command.class);

    private final Map<String, String> env;
    private final Path cd;
    private final String[] cmdArray;
    final String cmdArrayString;
    final Expectations expectations;

    Command(
            String executable,
            List<String> arguments,
            Map<String, String> environment,
            Path cd,
            Expectations expectations) {
        this.cmdArray = asCmdArray(Objects.requireNonNull(executable, "executable"),
                Objects.requireNonNull(arguments, "arguments"));
        this.env = Objects.requireNonNull(environment, "environment");
        this.cd = Objects.requireNonNull(cd, "cd");
        this.expectations = expectations;
        this.cmdArrayString = Arrays.stream(cmdArray).collect(Collectors.joining(" "));
    }

    /**
     * Start this {@link Command} and return a running {@link CommandProcess}.
     *
     * @return a {@link CommandProcess}
     *
     * @since  0.0.1
     * @see    #execute()
     */
    public CommandProcess start() {
        final boolean stderrToStdout = expectations.stderr == null;
        log.info(
                "Executing\n\n    cd {} && {}{}\n\nwith env {}",
                cd,
                cmdArrayString,
                stderrToStdout ? " 2>&1" : "",
                env);
        ProcessBuilder builder = new ProcessBuilder(cmdArray) //
                .directory(cd.toFile()) //
                .redirectErrorStream(stderrToStdout);
        if (!env.isEmpty()) {
            builder.environment().putAll(env);
        }
        try {
            final Process process = builder.start();
            final OutputConsumer out = expectations.stdout.apply(process.getInputStream());
            out.start();

            final OutputConsumer err;
            final Function<InputStream, OutputConsumer> stde = expectations.stderr;
            if (stde == null) {
                err = null;
            } else {
                err = stde.apply(process.getErrorStream());
                err.start();
            }

            return new CommandProcess(
                    cmdArrayString,
                    process,
                    joinAsserts(out, err),
                    expectations.exitCodeAssert,
                    out,
                    err);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not execute " + cmdArrayString, e);
        }
    }

    /**
     * Starts the command {@link Process} and awaits (potentially indefinitely) its termination.
     * A shorthand for {@link #start()}.{@link CommandProcess#awaitTermination() awaitTermination()}
     *
     * @return a {@link CommandResult}
     * @since  0.0.1
     */
    public CommandResult execute() {
        return start().awaitTermination();
    }

    /**
     * Starts the command {@link Process} and awaits (potentially indefinitely) its termination at most for the specified
     * duration.
     * A shorthand for {@link #start()}.{@link CommandProcess#awaitTermination(Duration) awaitTermination(Duration)}
     *
     * @param  timeout maximum time to wait for the underlying process to terminate
     *
     * @return         a {@link CommandResult}
     * @since          0.0.1
     */
    public CommandResult execute(Duration timeout) {
        return start().awaitTermination(timeout);
    }

    /**
     * Starts the command {@link Process} and awaits (potentially indefinitely) its termination at most for the specified
     * timeout in milliseconds.
     * A shorthand for {@link #start()}.{@link CommandProcess#awaitTermination(Duration) awaitTermination(Duration)}
     *
     * @param  timeoutMs maximum time in milliseconds to wait for the underlying process to terminate
     *
     * @return           a {@link CommandResult}
     * @since            0.0.1
     */
    public CommandResult execute(long timeoutMs) {
        return start().awaitTermination(timeoutMs);
    }

    /**
     * @return an array containing the executable and its arguments that can be passed e.g. to
     *         {@link ProcessBuilder#command(String...)}
     */
    String[] asCmdArray(String executable, List<String> args) {
        String[] result = new String[args.size() + 1];
        int i = 0;
        result[i++] = executable;
        for (String arg : args) {
            result[i++] = arg;
        }
        return result;
    }

    Assert joinAsserts(OutputConsumer out,
            OutputConsumer err) {
        return err == null ? Assert.all(out, expectations.exitCodeAssert)
                : Assert.all(out, err, expectations.exitCodeAssert);
    }
}

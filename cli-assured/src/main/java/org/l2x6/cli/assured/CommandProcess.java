/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.l2x6.cli.assured.CommandOutput.Stream;

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
    private final StreamGobbler stdOut;
    private final StreamGobbler stdErr;
    private final CommandOutput out;

    private volatile boolean closed = false;

    CommandProcess(Command command, Process process) {
        super();
        this.command = command;
        this.process = process;

        out = new CommandOutput();
        stdOut = new StreamGobbler(process.getInputStream(), Stream.stdout, out);
        stdOut.start();

        if (command.stderrToStdout) {
            stdErr = null;
        } else {
            stdErr = new StreamGobbler(process.getErrorStream(), Stream.stderr, out);
            stdErr.start();
        }

        this.shutDownHook = new Thread(new Runnable() {
            @Override
            public void run() {
                close();
            }
        });
        Runtime.getRuntime().addShutdownHook(shutDownHook);
    }

    @Override
    public void close() {
        if (!closed) {
            this.closed = true;
            process.destroy();
            stdOut.cancel();
            if (stdErr != null) {
                stdErr.cancel();
            }
            process.destroy();
        }
    }

    /**
     * Awaits the termination of the underlying {@link Process} honoring the timeout passed via {@link Command#timeoutMs}.
     *
     * @return a {@link CommandResult}
     * @since  0.0.1
     */
    public CommandResult awaitTermination() {
        final long startMillisTime = System.currentTimeMillis();

        do {
            try {
                int exitCode = process.exitValue();
                try {
                    Runtime.getRuntime().removeShutdownHook(shutDownHook);
                } catch (Exception ignored) {
                }

                try {
                    stdOut.join();
                    stdOut.assertSuccess();

                    if (stdErr != null) {
                        stdErr.join();
                        stdErr.assertSuccess();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted", e);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }

                return new CommandResult(
                        command,
                        exitCode,
                        System.currentTimeMillis() - startMillisTime,
                        null,
                        out);
            } catch (IllegalThreadStateException ex) {
                final long duration = System.currentTimeMillis() - startMillisTime;
                if (duration < command.timeoutMs) {
                    try {
                        Thread.sleep(Math.min(command.timeoutMs - duration, 100));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted", e);
                    }
                }
            }
        } while (System.currentTimeMillis() - startMillisTime <= command.timeoutMs);
        return new CommandResult(
                command,
                -1,
                System.currentTimeMillis() - startMillisTime,
                new TimeoutAssertionError(
                        String.format("Command has not finished within %d ms: %s", command.timeoutMs, command.cmdArrayString)),
                out);
    }

    /**
     * The usual friend of {@link Process#getInputStream()} / {@link Process#getErrorStream()}.
     *
     * @since 0.0.1
     */
    static class StreamGobbler extends Thread {
        private volatile boolean cancelled;
        private IOException exception;
        private final InputStream in;
        private final CommandOutput out;
        private final Stream stream;

        private StreamGobbler(InputStream in, Stream stream, CommandOutput out) {
            this.in = in;
            this.stream = stream;
            this.out = out;
        }

        public void assertSuccess() throws IOException {
            if (exception != null) {
                throw exception;
            }
        }

        public void cancel() {
            this.cancelled = true;
        }

        @Override
        public void run() {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while (!cancelled && (line = r.readLine()) != null) {
                    out.add(stream, line);
                }
            } catch (IOException e) {
                exception = e;
            }
        }
    }
}

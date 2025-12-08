/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.LoggerFactory;

/**
 * A command that can be executed.
 *
 * @since  0.0.1
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class Command {
    static final org.slf4j.Logger log = LoggerFactory.getLogger(Command.class);

    private final Map<String, String> env;
    private final Path cd;
    final String[] cmdArray;
    final String cmdArrayString;
    final Expectations expectations;

    public static Builder builder() {
        return new Builder();
    }

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
     * Execute this {@link Command} and return a {@link CommandProcess}.
     *
     * @return a {@link CommandProcess}
     *
     * @since  0.0.1
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
            return new CommandProcess(this, builder.start());
        } catch (IOException e) {
            throw new UncheckedIOException("Could not execute " + cmdArrayString, e);
        }
    }

    /**
     * Starts the command {@link Process} and awaits (potentially indefinitely) its termination.
     *
     * @return a {@link CommandResult}
     * @since  0.0.1
     */
    public CommandResult awaitTermination() {
        return start().awaitTermination();
    }

    /**
     * Starts the command {@link Process} and awaits its termination at most for the specified duration.
     *
     * @param  timeout maximum time to wait for the underlying process to terminate
     *
     * @return         a {@link CommandResult}
     * @since          0.0.1
     */
    public CommandResult awaitTermination(Duration timeout) {
        return start().awaitTermination(timeout);
    }

    /**
     * Starts the command {@link Process} and awaits its termination at most for the specified amount of milliseconds.
     *
     * @param  timeoutMs maximum time in milliseconds to wait for the underlying process to terminate
     *
     * @return           a {@link CommandResult}
     * @since            0.0.1
     */
    public CommandResult awaitTermination(long timeoutMs) {
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

    public static class Builder {
        private String executable;
        private List<String> args = new ArrayList<>();
        private Map<String, String> env = new LinkedHashMap<>();
        private Path cd;
        private Expectations.Builder expectations;

        /**
         * Set the executable of the command and its arguments
         *
         * @param  executable an absolute or relative (to the current directory) path to the executable or a plain command name
         *                    if the given command can be found in {@code PATH} environment variable
         * @param  arguments  the command arguments
         *
         * @return            this {@link Builder}
         * @since             0.0.1
         */
        public Builder command(String executable, String... arguments) {
            this.executable = executable;
            for (String a : arguments) {
                this.args.add(a);
            }
            return this;
        }

        /**
         * @param  executable an absolute or relative (to the current directory) path to the executable or a plain command name
         *                    if the given command can be found in {@code PATH} environment variable
         * @return            this {@link Builder}
         * @since             0.0.1
         */
        public Builder executable(String executable) {
            this.executable = executable;
            return this;
        }

        /**
         * Sets the java command of the current JVM set as the {@link #executable}
         *
         * @return this {@link Builder}
         * @since  0.0.1
         */
        public Builder java() {
            Path javaHome = Paths.get(System.getProperty("java.home"));
            Path java = javaHome.resolve("bin/java");
            if (Files.isRegularFile(java)) {
                this.executable = java.toString();
            } else if (Files.isRegularFile(java = javaHome.resolve("bin/java.exe"))) {
                this.executable = java.toString();
            } else {
                throw new IllegalStateException("Could not locate java or java.exe in " + javaHome.resolve("bin"));
            }
            return this;
        }

        /**
         * Add a single command argument
         *
         * @param  arg the argument to add
         * @return     this {@link Builder}
         * @since      0.0.1
         */
        public Builder arg(String arg) {
            this.args.add(arg);
            return this;
        }

        /**
         * Add multiple command arguments
         *
         * @param  args the arguments to add
         * @return      this {@link Builder}
         * @since       0.0.1
         */
        public Builder args(String... args) {
            for (String arg : args) {
                this.args.add(arg);
            }
            return this;
        }

        /**
         * Add multiple command arguments
         *
         * @param  args the arguments to add
         * @return      this {@link Builder}
         * @since       0.0.1
         */
        public Builder args(Collection<String> args) {
            this.args.addAll(args);
            return this;
        }

        /**
         * Set a single environment variable for the command
         *
         * @param  name  name of the variable to add
         * @param  value value of the variable to add
         * @return       this {@link Builder}
         * @since        0.0.1
         */
        public Builder envVar(String name, String value) {
            this.env.put(name, value);
            return this;
        }

        /**
         * Set multiple environment variables for the command
         *
         * @param  env the variables to add
         * @return     this {@link Builder}
         * @since      0.0.1
         */
        public Builder env(Map<String, String> env) {
            this.env.putAll(env);
            return this;
        }

        /**
         * Set multiple environment variables for the command
         *
         * @param  env the variables to add
         * @return     this {@link Builder}
         * @since      0.0.1
         */
        public Builder env(String... env) {
            int cnt = env.length;
            if (cnt % 2 != 0) {
                throw new IllegalArgumentException("env(String[]) accepts only even number of arguments");
            }
            int i = 0;
            while (i < cnt) {
                this.env.put(env[i++], env[i++]);
            }
            return this;
        }

        /**
         * Set the given {@code workDirectory} to the undelying {@link Process}
         *
         * @param  workDirectory the work directory of the undelying {@link Process}
         * @return               this {@link Builder}
         * @since                0.0.1
         */
        public Builder cd(Path workDirectory) {
            this.cd = workDirectory;
            return this;
        }

        /**
         * @return a new {@link Expectations.Builder}
         * @since  0.0.1
         */
        public Expectations.Builder expect() {
            return Expectations.builder(this);
        }

        /**
         * Impose the given {@link Expectations} on the command execution.
         *
         * @param  expectations the Expectations to set
         * @return              this {@link Builder}
         * @since               0.0.1
         */
        public Builder expect(Expectations.Builder expectations) {
            this.expectations = expectations;
            return this;
        }

        /**
         * Create a new {@link Command} and call {@link Command#start()}
         *
         * @return a {@link CommandProcess}
         * @since  0.0.1
         */
        public CommandProcess start() {
            final List<String> args = Collections.unmodifiableList(this.args);
            this.args = null;
            final Map<String, String> env = Collections.unmodifiableMap(this.env);
            this.env = null;
            Command cmd = new Command(
                    executable,
                    args,
                    env,
                    cd == null ? Paths.get(".").toAbsolutePath().normalize() : cd,
                    expectations.build());
            return cmd.start();
        }

        /**
         * A shorthand for {@link #start()}.{@link CommandProcess#awaitTermination() awaitTermination()}.
         *
         * @return a {@link CommandResult}
         * @since  0.0.1
         */
        public CommandResult awaitTermination() {
            return start().awaitTermination();
        }

        /**
         * A shorthand for {@link #start()}.{@link CommandProcess#awaitTermination(Duration) awaitTermination(timeout)}.
         *
         * @param  timeout maximum time to wait for the underlying process to terminate
         *
         * @return         a {@link CommandResult}
         * @since          0.0.1
         */
        public CommandResult awaitTermination(Duration timeout) {
            return start().awaitTermination(timeout);
        }

        /**
         * A shorthand for {@link #start()}.{@link CommandProcess#awaitTermination(long) awaitTermination(timeoutMs)}.
         *
         * @param  timeoutMs maximum time in milliseconds to wait for the underlying process to terminate
         *
         * @return           a {@link CommandResult}
         * @since            0.0.1
         */
        public CommandResult awaitTermination(long timeoutMs) {
            return start().awaitTermination(timeoutMs);
        }

    }

}

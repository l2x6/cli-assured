/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CommandBuilder {
    private String executable;
    private List<String> args = new ArrayList<>();
    private Map<String, String> env = new LinkedHashMap<>();
    private Path cd;
    private Expectations expectations;

    CommandBuilder() {
    }

    /**
     * Set the executable of the command and its arguments
     *
     * @param  executable an absolute or relative (to the current directory) path to the executable or a plain command name
     *                    if the given command can be found in {@code PATH} environment variable
     * @param  arguments  the command arguments
     *
     * @return            this {@link CommandBuilder}
     * @since             0.0.1
     */
    public CommandBuilder command(String executable, String... arguments) {
        this.executable = executable;
        for (String a : arguments) {
            this.args.add(a);
        }
        return this;
    }

    /**
     * @param  executable an absolute or relative (to the current directory) path to the executable or a plain command name
     *                    if the given command can be found in {@code PATH} environment variable
     * @return            this {@link CommandBuilder}
     * @since             0.0.1
     */
    public CommandBuilder executable(String executable) {
        this.executable = executable;
        return this;
    }

    /**
     * Sets the java command of the current JVM set as the {@link #executable}
     *
     * @return this {@link CommandBuilder}
     * @since  0.0.1
     */
    public CommandBuilder java() {
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
     * @return     this {@link CommandBuilder}
     * @since      0.0.1
     */
    public CommandBuilder arg(String arg) {
        this.args.add(arg);
        return this;
    }

    /**
     * Add multiple command arguments
     *
     * @param  args the arguments to add
     * @return      this {@link CommandBuilder}
     * @since       0.0.1
     */
    public CommandBuilder args(String... args) {
        for (String arg : args) {
            this.args.add(arg);
        }
        return this;
    }

    /**
     * Add multiple command arguments
     *
     * @param  args the arguments to add
     * @return      this {@link CommandBuilder}
     * @since       0.0.1
     */
    public CommandBuilder args(Collection<String> args) {
        this.args.addAll(args);
        return this;
    }

    /**
     * Set a single environment variable for the command
     *
     * @param  name  name of the variable to add
     * @param  value value of the variable to add
     * @return       this {@link CommandBuilder}
     * @since        0.0.1
     */
    public CommandBuilder envVar(String name, String value) {
        this.env.put(name, value);
        return this;
    }

    /**
     * Set multiple environment variables for the command
     *
     * @param  env the variables to add
     * @return     this {@link CommandBuilder}
     * @since      0.0.1
     */
    public CommandBuilder env(Map<String, String> env) {
        this.env.putAll(env);
        return this;
    }

    /**
     * Set multiple environment variables for the command
     *
     * @param  env the variables to add
     * @return     this {@link CommandBuilder}
     * @since      0.0.1
     */
    public CommandBuilder env(String... env) {
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
     * @return               this {@link CommandBuilder}
     * @since                0.0.1
     */
    public CommandBuilder cd(Path workDirectory) {
        this.cd = workDirectory;
        return this;
    }

    /**
     * @return a new {@link ExpectationsBuilder}
     * @since  0.0.1
     */
    public ExpectationsBuilder expect() {
        return new ExpectationsBuilder(this);
    }

    /**
     * Impose the given {@link Expectations} on the command execution.
     *
     * @param  expectations the Expectations to set
     * @return              this {@link CommandBuilder}
     * @since               0.0.1
     */
    CommandBuilder expect(Expectations expectations) {
        this.expectations = expectations;
        return this;
    }

    /**
     * Create a new {@link Command} and call {@link Command#start()}
     *
     * @return a {@link CommandProcess}
     * @since  0.0.1
     * @see    #execute()
     */
    public CommandProcess start() {
        return build().start();
    }

    /**
     * Start the {@link CommandProcess} and await (potentially indefinitely) its termination.
     * A shorthand for {@link #start()}.{@link CommandProcess#awaitTermination() awaitTermination()}
     *
     * @return a {@link CommandResult}
     * @since  0.0.1
     */
    public CommandResult execute() {
        return build().execute();
    }

    /**
     * Start the {@link CommandProcess} and await (potentially indefinitely) its termination at most for the specified
     * duration.
     * A shorthand for {@link #start()}.{@link CommandProcess#awaitTermination(Duration) awaitTermination(Duration)}
     *
     * @param  timeout maximum time to wait for the underlying process to terminate
     *
     * @return         a {@link CommandResult}
     * @since          0.0.1
     */
    public CommandResult execute(Duration timeout) {
        return build().execute(timeout);
    }

    /**
     * Start the {@link CommandProcess} and await (potentially indefinitely) its termination at most for the specified
     * timeout in milliseconds.
     * A shorthand for {@link #start()}.{@link CommandProcess#awaitTermination(Duration) awaitTermination(Duration)}
     *
     * @param  timeoutMs maximum time in milliseconds to wait for the underlying process to terminate
     *
     * @return           a {@link CommandResult}
     * @since            0.0.1
     */
    public CommandResult execute(long timeoutMs) {
        return build().execute(timeoutMs);
    }

    Command build() {
        final List<String> args = Collections.unmodifiableList(this.args);
        this.args = null;
        final Map<String, String> env = Collections.unmodifiableMap(this.env);
        this.env = null;
        if (expectations == null) {
            expect().parent();
        }
        Command cmd = new Command(
                executable,
                args,
                env,
                cd == null ? Paths.get(".").toAbsolutePath().normalize() : cd,
                expectations);
        return cmd;
    }

}

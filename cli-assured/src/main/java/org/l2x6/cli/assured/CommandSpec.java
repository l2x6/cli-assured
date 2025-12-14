/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.l2x6.cli.assured.asserts.Assert;
import org.slf4j.LoggerFactory;

/**
 * A command that can be executed.
 *
 * @since  0.0.1
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class CommandSpec {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(CommandSpec.class);
    private static final Pattern WS_PATTERN = Pattern.compile("\\s");

    private final String executable;
    private final List<String> arguments;
    private final Map<String, String> env;
    private final Path cd;
    private final ExpectationsSpec expectations;
    private final boolean stderrToStdout;

    CommandSpec(
            String executable,
            List<String> arguments) {
        this.executable = executable;
        this.arguments = arguments;
        this.env = Collections.emptyMap();
        this.cd = Paths.get(".").toAbsolutePath().normalize();
        this.stderrToStdout = false;
        this.expectations = new ExpectationsSpec(this, stderrToStdout);
    }

    CommandSpec(
            String executable,
            List<String> arguments,
            Map<String, String> environment,
            Path cd,
            ExpectationsSpec expectations,
            boolean stderrToStdout) {
        this.executable = executable;
        this.arguments = arguments;
        this.env = Objects.requireNonNull(environment, "environment");
        this.cd = Objects.requireNonNull(cd, "cd");
        this.stderrToStdout = stderrToStdout;
        this.expectations = expectations;
    }

    /**
     * Set the executable of the command and its arguments
     *
     * @param  executable an absolute or relative (to the current directory) path to the executable or a plain command name
     *                    if the given command can be found in {@code PATH} environment variable
     * @param  arguments  the command arguments
     *
     * @return            an adjusted copy of this {@link CommandSpec}
     * @since             0.0.1
     */
    public CommandSpec command(String executable, String... arguments) {
        return new CommandSpec(executable, CliAssertUtils.join(this.arguments, arguments), env, cd, expectations,
                stderrToStdout);
    }

    /**
     * @param  executable an absolute or relative (to the current directory) path to the executable or a plain command name
     *                    if the given command can be found in {@code PATH} environment variable
     * @return            an adjusted copy of this {@link CommandSpec}
     * @since             0.0.1
     */
    public CommandSpec executable(String executable) {
        return new CommandSpec(executable, arguments, env, cd, expectations, stderrToStdout);
    }

    /**
     * Sets the java command of the current JVM set as the {@link #executable}
     *
     * @return an adjusted copy of this {@link CommandSpec}
     * @since  0.0.1
     */
    public CommandSpec java() {
        final String exec = javaExecutable();
        return new CommandSpec(exec, arguments, env, cd, expectations, stderrToStdout);
    }

    static String javaExecutable() {
        final Path javaHome = Paths.get(System.getProperty("java.home"));
        Path java = javaHome.resolve("bin/java");
        final String exec;
        if (Files.isRegularFile(java)) {
            exec = java.toString();
        } else if (Files.isRegularFile(java = javaHome.resolve("bin/java.exe"))) {
            exec = java.toString();
        } else {
            throw new IllegalStateException("Could not locate java or java.exe in " + javaHome.resolve("bin"));
        }
        return exec;
    }

    /**
     * Add a single command argument
     *
     * @param  arg the argument to add
     * @return     an adjusted copy of this {@link CommandSpec}
     * @since      0.0.1
     */
    public CommandSpec arg(String arg) {
        return new CommandSpec(executable, CliAssertUtils.join(this.arguments, arg), env, cd, expectations, stderrToStdout);
    }

    /**
     * Add multiple command arguments
     *
     * @param  args the arguments to add
     * @return      an adjusted copy of this {@link CommandSpec}
     * @since       0.0.1
     */
    public CommandSpec args(String... args) {
        return new CommandSpec(executable, CliAssertUtils.join(this.arguments, args), env, cd, expectations, stderrToStdout);
    }

    /**
     * Add multiple command arguments
     *
     * @param  args the arguments to add
     * @return      an adjusted copy of this {@link CommandSpec}
     * @since       0.0.1
     */
    public CommandSpec args(Collection<String> arguments) {
        return new CommandSpec(executable, CliAssertUtils.join(this.arguments, arguments), env, cd, expectations,
                stderrToStdout);
    }

    /**
     * Set multiple environment variables for the command
     *
     * @param  env the variables to add
     * @return     an adjusted copy of this {@link CommandSpec}
     * @since      0.0.1
     */
    public CommandSpec env(Map<String, String> env) {
        Map<String, String> e = new LinkedHashMap<>(this.env);
        e.putAll(env);
        return new CommandSpec(executable, arguments, Collections.unmodifiableMap(e), cd, expectations, stderrToStdout);
    }

    /**
     * Set one or more environment variables for the command
     *
     * @param  name  name of the variable to add
     * @param  value value of the variable to add
     * @param  more  (optional) more variables to add; even elements are variable names, odd elements are values
     * @return       an adjusted copy of this {@link CommandSpec}
     * @since        0.0.1
     */
    public CommandSpec env(String name, String value, String... more) {
        int cnt = more.length;
        if (cnt % 2 != 0) {
            throw new IllegalArgumentException("env(String[]) accepts only even number of arguments");
        }

        final Map<String, String> e = new LinkedHashMap<>(this.env);
        e.put(name, value);

        int i = 0;
        while (i < cnt) {
            e.put(more[i++], more[i++]);
        }
        return new CommandSpec(executable, arguments, Collections.unmodifiableMap(e), cd, expectations, stderrToStdout);
    }

    /**
     * Set the given {@code workDirectory} to the undelying {@link Process}
     *
     * @param  workDirectory the work directory of the undelying {@link Process}
     * @return               an adjusted copy of this {@link CommandSpec}
     * @since                0.0.1
     */
    public CommandSpec cd(Path workDirectory) {
        return new CommandSpec(executable, arguments, env, workDirectory, expectations, stderrToStdout);
    }

    /**
     * Enable the redirection of {@code stderr} to {@code stdout}
     *
     * @return an adjusted copy of this {@link CommandSpec}
     * @since  0.0.1
     */
    public CommandSpec stderrToStdout() {
        return new CommandSpec(executable, arguments, env, cd, expectations, true);
    }

    // @formatter:off
    /**
     * Syntactic sugar to make fans of Behavior-Driven testing happy. Allows writing
     * <pre>{@code
     * CliAssured
     *     .given()
     *         .env("GREETING", "CLI Assured rocks!")
     *     .when()
     *         .command("sh", "-c", "echo $GREETING")
     *     .then()
     *         .stdout()
     *             .hasLines("CLI Assured rocks!")
     *             .hasLineCount(1)
     *         .exitCode(0)
     *     .execute()
     *     .assertSuccess();
     * }</pre>
     *
     * @return this {@link CommandSpec}
     * @since  0.0.1
     */
    // @formatter:on
    public CommandSpec when() {
        return this;
    }

    /**
     * @return a new {@link ExpectationsSpec}
     * @since  0.0.1
     */
    public ExpectationsSpec then() {
        return new ExpectationsSpec(this, stderrToStdout);
    }

    /**
     * Impose the given {@link ExpectationsSpec} on the command execution.
     *
     * @param  expectations the Expectations to set
     * @return              an adjusted copy of this {@link CommandSpec}
     * @since               0.0.1
     */
    CommandSpec expect(ExpectationsSpec expectations) {
        return new CommandSpec(executable, arguments, env, cd, expectations, stderrToStdout);
    }

    /**
     * Start this {@link CommandSpec} and return a running {@link CommandProcess}.
     *
     * @return a {@link CommandProcess}
     *
     * @since  0.0.1
     * @see    #execute()
     */
    public CommandProcess start() {
        String[] cmdArray = asCmdArray(executable, arguments);
        String cmdArrayString = Arrays.stream(cmdArray).map(CommandSpec::quote).collect(Collectors.joining(" "));
        log.info(
                "Executing\n\n    cd {} &&{} {}{}\n",
                cd,
                envString(env),
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
        if (executable == null) {
            throw new IllegalStateException("executable must be specified before starting the command process."
                    + " You may want to call CommandSpec.executable(String) or CommandSpec.command(String, String...)");
        }
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

    static String envString(Map<String, String> env) {
        if (env.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Entry<String, String> en : env.entrySet()) {
            sb.append(' ').append(en.getKey()).append('=').append(quote(en.getValue()));
        }
        return sb.toString();
    }

    static String quote(String string) {
        if (WS_PATTERN.matcher(string).find()) {
            return "\"" + string.replace("\"", "\\\"") + "\"";
        }
        return string;
    }
}

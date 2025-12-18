/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.l2x6.cli.assured.CliAssertUtils.ExcludeFromJacocoGeneratedReport;
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
    private static AtomicInteger threadCounter = new AtomicInteger();

    private final String executable;
    private final List<String> arguments;
    private final Map<String, String> env;
    private final Path cd;
    private final ExpectationsSpec expectations;
    private final boolean stderrToStdout;
    private final Consumer<OutputStream> stdin;
    private final int threadIndex;

    CommandSpec(
            String executable,
            List<String> arguments) {
        this.executable = executable;
        this.arguments = arguments;
        this.env = Collections.emptyMap();
        this.cd = Paths.get(".").toAbsolutePath().normalize();
        this.stderrToStdout = false;
        this.threadIndex = threadCounter.getAndIncrement();
        this.expectations = new ExpectationsSpec(this, stderrToStdout, threadIndex);
        this.stdin = null;
    }

    CommandSpec(
            String executable,
            List<String> arguments,
            Map<String, String> environment,
            Path cd,
            ExpectationsSpec expectations,
            boolean stderrToStdout,
            Consumer<OutputStream> stdin,
            int threadIndex) {
        this.executable = executable;
        this.arguments = arguments;
        this.env = Objects.requireNonNull(environment, "environment");
        this.cd = Objects.requireNonNull(cd, "cd");
        this.stderrToStdout = stderrToStdout;
        this.expectations = expectations;
        this.stdin = stdin;
        this.threadIndex = threadIndex;
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
                stderrToStdout, stdin, threadIndex);
    }

    /**
     * @param  executable an absolute or relative (to the current directory) path to the executable or a plain command name
     *                    if the given command can be found in {@code PATH} environment variable
     * @return            an adjusted copy of this {@link CommandSpec}
     * @since             0.0.1
     */
    public CommandSpec executable(String executable) {
        return new CommandSpec(executable, arguments, env, cd, expectations, stderrToStdout, stdin, threadIndex);
    }

    /**
     * Sets the java command of the current JVM set as the {@link #executable}
     *
     * @return an adjusted copy of this {@link CommandSpec}
     * @since  0.0.1
     */
    public CommandSpec java() {
        final String exec = javaExecutable();
        return new CommandSpec(exec, arguments, env, cd, expectations, stderrToStdout, stdin, threadIndex);
    }

    @ExcludeFromJacocoGeneratedReport
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
        return new CommandSpec(executable, CliAssertUtils.join(this.arguments, arg), env, cd, expectations, stderrToStdout,
                stdin, threadIndex);
    }

    /**
     * Add multiple command arguments
     *
     * @param  args the arguments to add
     * @return      an adjusted copy of this {@link CommandSpec}
     * @since       0.0.1
     */
    public CommandSpec args(String... args) {
        return new CommandSpec(executable, CliAssertUtils.join(this.arguments, args), env, cd, expectations, stderrToStdout,
                stdin, threadIndex);
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
                stderrToStdout, stdin, threadIndex);
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
        return new CommandSpec(executable, arguments, Collections.unmodifiableMap(e), cd, expectations, stderrToStdout, stdin,
                threadIndex);
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
        return new CommandSpec(executable, arguments, Collections.unmodifiableMap(e), cd, expectations, stderrToStdout, stdin,
                threadIndex);
    }

    /**
     * Set the given {@code workDirectory} to the undelying {@link Process}
     *
     * @param  workDirectory the work directory of the undelying {@link Process}
     * @return               an adjusted copy of this {@link CommandSpec}
     * @since                0.0.1
     */
    public CommandSpec cd(Path workDirectory) {
        return new CommandSpec(executable, arguments, env, workDirectory, expectations, stderrToStdout, stdin, threadIndex);
    }

    /**
     * Enable the redirection of {@code stderr} to {@code stdout}
     *
     * @return an adjusted copy of this {@link CommandSpec}
     * @since  0.0.1
     */
    public CommandSpec stderrToStdout() {
        return new CommandSpec(executable, arguments, env, cd, expectations, true, stdin, threadIndex);
    }

    /**
     * Set a {@link Consumer} that will receive an {@link OutputStream} connected to the underlying process'es
     * {@code stdin}.
     * <p>
     * CLI Assert will attempt to close the passed-in {@link OutputStream} right after {@link Consumer#accept(Object)}
     * returns.
     * <p>
     * {@link Consumer#accept(Object)} will be called on a dedicated thread.
     * It is the responsibility of the caller of this method to address any thread safety concerns.
     * <p>
     * A {@link CancellationException} can be thrown from any method called on the {@link OutputStream} passed-in to
     * {@link Consumer#accept(Object)}, if {@link CommandProcess#kill(boolean)} is called while
     * {@link Consumer#accept(Object)} is running.
     * You may want to catch that exception and stop attempting to write to the passed-in {@link OutputStream} after that.
     * <p>
     * Exceptions thrown from {@link Consumer#accept(Object)} will be caught and reported upon calling
     * {@link CommandResult#assertSuccess()}.
     * <p>
     * You may call only one of {@code stdin(...)} methods for the given {@link CommandSpec} chain.
     *
     * @param  stdin a handler that may write to the passed-in {@link OutputStream}
     * @return       an adjusted copy of this {@link CommandSpec}
     * @since        0.0.1
     */
    public CommandSpec stdin(Consumer<OutputStream> stdin) {
        if (this.stdin != null) {
            throw new IllegalStateException("stdin was already defined for this " + CommandSpec.class.getName()
                    + ". You may want to keep ony one stdin(...) call for the given CommandSpec chain");
        }
        return new CommandSpec(executable, arguments, env, cd, expectations, stderrToStdout, stdin, threadIndex);
    }

    /**
     * Pass the given {@link String} to the {@code stdin} of the command using {@code utf-8 encoding}.
     * <p>
     * You may call only one of {@code stdin(...)} methods for the given {@link CommandSpec} chain.
     *
     * @param  stdin a {@link String} to pass to the {@code stdin} of the command using {@code utf-8 encoding}
     * @return       an adjusted copy of this {@link CommandSpec}
     * @since        0.0.1
     */
    public CommandSpec stdin(String stdin) {
        if (this.stdin != null) {
            throw new IllegalStateException("stdin was already defined for this " + CommandSpec.class.getName()
                    + ". You may want to keep ony one stdin(...) call for the given CommandSpec chain");
        }
        return new CommandSpec(executable, arguments, env, cd, expectations, stderrToStdout,
                new StringPipe(stdin),
                threadIndex);
    }

    /**
     * Pass the given {@code file} to the {@code stdin} of the command.
     * <p>
     * You may call only one of {@code stdin(...)} methods for the given {@link CommandSpec} chain.
     *
     * @param  file a handler that may write to the passed-in {@link OutputStream}
     * @return      an adjusted copy of this {@link CommandSpec}
     * @since       0.0.1
     */
    public CommandSpec stdin(Path file) {
        if (this.stdin != null) {
            throw new IllegalStateException("stdin was already defined for this " + CommandSpec.class.getName()
                    + ". You may want to keep ony one stdin(...) call for the given CommandSpec chain");
        }
        return new CommandSpec(executable, arguments, env, cd, expectations, stderrToStdout,
                new FilePipe(file),
                threadIndex);
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
        return new ExpectationsSpec(this, stderrToStdout, threadIndex);
    }

    /**
     * Impose the given {@link ExpectationsSpec} on the command execution.
     *
     * @param  expectations the Expectations to set
     * @return              an adjusted copy of this {@link CommandSpec}
     * @since               0.0.1
     */
    CommandSpec expect(ExpectationsSpec expectations) {
        return new CommandSpec(executable, arguments, env, cd, expectations, stderrToStdout, stdin, threadIndex);
    }

    /**
     * Start this {@link CommandSpec} and return a running {@link CommandProcess}.
     *
     * @return a {@link CommandProcess}
     *
     * @since  0.0.1
     * @see    #execute()
     */
    @ExcludeFromJacocoGeneratedReport
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

            final InputProducer stdinProcess;
            if (stdin != null) {
                stdinProcess = new InputProducer(process.getOutputStream(), stdin, threadIndex);
                stdinProcess.start();
            } else {
                stdinProcess = null;
            }

            return new CommandProcess(
                    cmdArrayString,
                    process,
                    Assert.all(out, err, stdinProcess, expectations.exitCodeAssert),
                    expectations.exitCodeAssert,
                    stdinProcess,
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
            throw new IllegalStateException("The executable must be specified before starting the command process."
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

    static class FilePipe implements Consumer<OutputStream> {
        private final Path file;

        FilePipe(Path file) {
            this.file = file;
        }

        @Override
        @ExcludeFromJacocoGeneratedReport
        public void accept(OutputStream out) {
            try {
                Files.copy(file, out);
            } catch (IOException e) {
                throw new UncheckedIOException("Could not write to stdin", e);
            }
        }
    }

    static class StringPipe implements Consumer<OutputStream> {
        private final String payload;

        StringPipe(String payload) {
            this.payload = payload;
        }

        @Override
        @ExcludeFromJacocoGeneratedReport
        public void accept(OutputStream out) {
            try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
                w.write(payload);
            } catch (IOException e) {
                throw new UncheckedIOException("Could not write to stdin", e);
            }
        }
    }

}

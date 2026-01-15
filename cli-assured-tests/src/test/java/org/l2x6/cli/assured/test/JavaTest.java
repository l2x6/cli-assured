/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured.test;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.l2x6.cli.assured.CliAssured;
import org.l2x6.cli.assured.CommandProcess;
import org.l2x6.cli.assured.CommandResult;
import org.l2x6.cli.assured.CommandSpec;
import org.l2x6.cli.assured.StreamExpectationsSpec;
import org.l2x6.cli.assured.test.app.TestApp;

public class JavaTest {

    @Test
    void stdout() {

        run("hello", "Joe")
                .hasLines("Hello Joe")
                .hasLines(Collections.singleton("Hello Joe"))
                .start()
                .awaitTermination()
                .assertSuccess();

        Assertions
                .assertThatThrownBy(
                        command("hello", "Joe")
                                .then()
                                .stderr()
                                .hasLines("Hello Joe")
                                .start()
                                .awaitTermination()::assertSuccess)
                .isInstanceOf(AssertionError.class)
                .message().endsWith("Failure 1/1: Expected lines\n"
                        + "\n"
                        + "    Hello Joe\n"
                        + "\n"
                        + "to occur in stderr in any order, but none of them occurred\n"
                        + "\n"
                        + "stderr: <no output>");

    }

    @Test
    void stderrToStdout() {

        command("helloErr", "Joe")
                .stderrToStdout()
                .then()
                .stdout()
                .hasLines("Hello stderr Joe")
                .start()
                .awaitTermination()
                .assertSuccess();

        Assertions.assertThatThrownBy(command("helloErr", "Joe")
                .stderrToStdout()
                .then()::stderr)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("You cannot set any assertions on stderr while you are redirecting stderr to stdout");

    }

    @Test
    void stderr() {

        CommandResult result = command("helloErr", "Joe")
                .then()
                .stderr()
                .hasLines("Hello stderr Joe")
                .start()
                .awaitTermination()
                .assertSuccess();

        Assertions.assertThat(result.byteCountStderr()).isBetween(17L, 18L);
        Assertions.assertThat(result.duration()).isGreaterThan(Duration.ofMillis(0));

        Assertions
                .assertThatThrownBy(
                        run("helloErr", "Joe")
                                .start()
                                .awaitTermination()::assertSuccess)
                .isInstanceOf(AssertionError.class)
                .message().endsWith("Failure 1/1: Expected no content to occur in stderr\n"
                        + "\n"
                        + "stderr:\n"
                        + "\n"
                        + "    Hello stderr Joe");

        Assertions
                .assertThatThrownBy(
                        run("helloErr", "Joe")
                                .hasLines("Hello stderr Joe")
                                .start()
                                .awaitTermination()::assertSuccess)
                .isInstanceOf(AssertionError.class)
                .message().endsWith("Failure 1/2: Expected lines\n"
                        + "\n"
                        + "    Hello stderr Joe\n"
                        + "\n"
                        + "to occur in stdout in any order, but none of them occurred\n"
                        + "\n"
                        + "stdout: <no output>\n"
                        + "\n"
                        + "Failure 2/2: Expected no content to occur in stderr\n"
                        + "\n"
                        + "stderr:\n"
                        + "\n"
                        + "    Hello stderr Joe");

    }

    @Test
    void hasLinesContaining() {
        run("hello", "Joe")
                .hasLinesContaining("lo J")
                .hasLinesContaining(Collections.singleton("Hello"))
                .hasLinesContainingCaseInsensitive("JOE")
                .hasLinesContainingCaseInsensitive(Collections.singleton("hel"))
                .hasLineCount(1)
                .start()
                .awaitTermination()
                .assertSuccess();

        Assertions
                .assertThatThrownBy(
                        command("hello", "Joe")
                                .then()
                                .stderr()
                                .hasLinesContaining("lo J")
                                .hasLinesContainingCaseInsensitive("JOE")
                                .start()
                                .awaitTermination()::assertSuccess)
                .isInstanceOf(AssertionError.class)
                .message().endsWith("Failure 1/2: Expected lines containing\n"
                        + "\n"
                        + "    lo J\n"
                        + "\n"
                        + "to occur in stderr, but none of them occurred\n"
                        + "\n"
                        + "Failure 2/2: Expected lines containing\n"
                        + "\n"
                        + "    joe\n"
                        + "\n"
                        + "using case insensitive comparison to occur in stderr, but none of them occurred\n"
                        + "\n"
                        + "stderr: <no output>");

    }

    @Test
    void hasLinesMatching() {
        run("hello", "Joe")
                .hasLinesMatching("lo J.e")
                .hasLinesMatching(Pattern.compile("joe", Pattern.CASE_INSENSITIVE))
                .hasLinesMatching(Collections.singletonList("Hel+o"))
                .hasLineCount(1)
                .start()
                .awaitTermination()
                .assertSuccess();

        Assertions
                .assertThatThrownBy(
                        command("hello", "Joe")
                                .then()
                                .stderr()
                                .hasLinesMatching("lo J.e")
                                .start()
                                .awaitTermination()::assertSuccess)
                .isInstanceOf(AssertionError.class)
                .message().endsWith("Failure 1/1: Expected lines matching\n"
                        + "\n"
                        + "    lo J.e\n"
                        + "\n"
                        + "to occur in stderr, but none of them matched\n"
                        + "\n"
                        + "stderr: <no output>");
    }

    @Test
    void doesNotHaveLines() {
        run("hello", "Joe")
                .doesNotHaveLines("Hello John")
                .doesNotHaveLines(Collections.singletonList("Foo"))
                .hasLineCount(1)
                .start()
                .awaitTermination()
                .assertSuccess();

        runErr("helloErr", "Joe")
                .doesNotHaveLines("Hello John")
                .hasLineCount(1)
                .start()
                .awaitTermination()
                .assertSuccess();

        Assertions
                .assertThatThrownBy(
                        run("hello", "Joe")
                                .doesNotHaveLines("Hello Joe")
                                .start()
                                .awaitTermination()::assertSuccess)
                .isInstanceOf(AssertionError.class)
                .message().endsWith("Failure 1/1: Expected none of the lines\n"
                        + "\n"
                        + "    Hello Joe\n"
                        + "\n"
                        + "to occur in stdout, but all of them occurred\n"
                        + "\n"
                        + "stdout:\n"
                        + "\n"
                        + "    Hello Joe");

        Assertions
                .assertThatThrownBy(
                        runErr("helloErr", "Joe")
                                .doesNotHaveLines("Hello stderr Joe")
                                .start()
                                .awaitTermination()::assertSuccess)
                .isInstanceOf(AssertionError.class)
                .message().endsWith("Failure 1/1: Expected none of the lines\n"
                        + "\n"
                        + "    Hello stderr Joe\n"
                        + "\n"
                        + "to occur in stderr, but all of them occurred\n"
                        + "\n"
                        + "stderr:\n"
                        + "\n"
                        + "    Hello stderr Joe");

    }

    @Test
    void doesNotHaveLinesContaining() {
        run("hello", "Joe")
                .doesNotHaveLinesContaining("John")
                .doesNotHaveLinesContaining(Collections.singletonList("foo"))
                .doesNotHaveLinesContainingCaseInsensitive("DOLLY")
                .doesNotHaveLinesContainingCaseInsensitive(Collections.singletonList("bar"))
                .hasLineCount(1)
                .start()
                .awaitTermination()
                .assertSuccess();

        Assertions
                .assertThatThrownBy(
                        run("hello", "Joe")
                                .doesNotHaveLinesContaining("Joe")
                                .start()
                                .awaitTermination()::assertSuccess)
                .isInstanceOf(AssertionError.class)
                .message().endsWith("Failure 1/1: Expected no lines containing\n"
                        + "\n"
                        + "    Joe\n"
                        + "\n"
                        + "to occur in stdout, but some of the substrings occur in lines\n"
                        + "\n"
                        + "    Hello >>Joe<<\n"
                        + "\n"
                        + "stdout:\n"
                        + "\n"
                        + "    Hello Joe");

        Assertions
                .assertThatThrownBy(
                        runErr("helloErr", "Joe")
                                .doesNotHaveLinesContaining("Joe")
                                .start()
                                .awaitTermination()::assertSuccess)
                .isInstanceOf(AssertionError.class)
                .message().endsWith("Failure 1/1: Expected no lines containing\n"
                        + "\n"
                        + "    Joe\n"
                        + "\n"
                        + "to occur in stderr, but some of the substrings occur in lines\n"
                        + "\n"
                        + "    Hello stderr >>Joe<<\n"
                        + "\n"
                        + "stderr:\n"
                        + "\n"
                        + "    Hello stderr Joe");
    }

    @Test
    void doesNotHaveLinesMatching() {
        run("hello", "Joe")
                .doesNotHaveLinesMatching("Hello M.*")
                .doesNotHaveLinesMatching(Pattern.compile("joe"))
                .doesNotHaveLinesMatching(Collections.singleton("FOO"))
                .hasLineCount(1)
                .start()
                .awaitTermination()
                .assertSuccess();

        Assertions
                .assertThatThrownBy(
                        run("hello", "Joe")
                                .doesNotHaveLinesMatching("lo Jo.*")
                                .start()
                                .awaitTermination()::assertSuccess)
                .isInstanceOf(AssertionError.class)
                .message().endsWith("Failure 1/1: Expected no lines matching\n"
                        + "\n"
                        + "    lo Jo.*\n"
                        + "\n"
                        + "to occur in stdout, but some of the patterns matched the lines\n"
                        + "\n"
                        + "    Hel>>lo Joe<<\n"
                        + "\n"
                        + "stdout:\n"
                        + "\n"
                        + "    Hello Joe");

        Assertions
                .assertThatThrownBy(
                        runErr("helloErr", "Joe")
                                .doesNotHaveLinesMatching("lo stderr Jo.*")
                                .start()
                                .awaitTermination()::assertSuccess)
                .isInstanceOf(AssertionError.class)
                .message().endsWith("Failure 1/1: Expected no lines matching\n"
                        + "\n"
                        + "    lo stderr Jo.*\n"
                        + "\n"
                        + "to occur in stderr, but some of the patterns matched the lines\n"
                        + "\n"
                        + "    Hel>>lo stderr Joe<<\n"
                        + "\n"
                        + "stderr:\n"
                        + "\n"
                        + "    Hello stderr Joe");

    }

    @Test
    void hasLineCount() {
        run("hello", "Joe")
                .hasLineCount(cnt -> cnt > 0 && cnt < 2,
                        "Expected number of lines > 0 && < 2 in ${stream} but found ${actual} lines")
                .start()
                .awaitTermination()
                .assertSuccess();

        Assertions
                .assertThatThrownBy(
                        command("hello", "Joe")
                                .then()
                                .stderr()
                                .hasLineCount(cnt -> cnt > 0 && cnt < 2,
                                        "Expected number of lines > 0 && < 2 in ${stream} but found ${actual} lines")
                                .start()
                                .awaitTermination()::assertSuccess)
                .isInstanceOf(AssertionError.class)
                .message().endsWith("Failure 1/1: Expected number of lines > 0 && < 2 in stderr but found 0 lines\n"
                        + "\n"
                        + "stderr: <no output>");
    }

    @Test
    void isEmpty() {

        Assertions
                .assertThatThrownBy(
                        run("hello", "Joe")
                                .isEmpty()
                                .start()
                                .awaitTermination()::assertSuccess)
                .isInstanceOf(AssertionError.class)
                .message().contains("Failure 1/1: Expected 0 bytes in stdout but found");
    }

    @Test
    void cd() throws IOException {
        Path cd = Paths.get("target/JavaTest-" + UUID.randomUUID());
        Files.createDirectories(cd);
        command("write", "Hello Dolly", "hello.txt")
                .cd(cd)
                .then()
                .stdout()
                .hasLineCount(0)
                .stderr()
                .hasLineCount(0)
                .start()
                .awaitTermination()
                .assertSuccess();
        Assertions.assertThat(cd.resolve("hello.txt")).isRegularFile().hasContent("Hello Dolly");
    }

    @Test
    void redirect() {

        UUID uuid = UUID.randomUUID();
        Path out = Paths.get("target/" + JavaTest.class.getSimpleName() + "." + uuid + "-stdout.txt");
        Path outErr = Paths.get("target/" + JavaTest.class.getSimpleName() + "." + uuid + "-stderr.txt");
        CommandProcess proc = run("hello", "Joe")
                .redirect(out)
                .stderr()
                .redirect(outErr)
                .start();
        Assertions.assertThat(proc.toString())
                .isEqualTo("cd " + Paths.get(".").toAbsolutePath().normalize()
                        + " && " + javaExecutable() + " -cp " + testAppJar()
                        + " org.l2x6.cli.assured.test.app.TestApp hello Joe > " + Paths.get("target/JavaTest." + uuid
                                + "-stdout.txt")
                        + " 2> " + Paths.get("target/JavaTest." + uuid + "-stderr.txt"));
        proc.awaitTermination()
                .assertSuccess();
        Assertions.assertThat(out).content(StandardCharsets.UTF_8).matches("^Hello Joe\r?\n$");

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

    @Test
    void redirectStream() throws IOException {

        /* Append the output of two command to a single file
         * Thus making sure that we do not close the underlying stream */
        Path out = Paths.get("target/" + JavaTest.class.getSimpleName() + ".redirectStream-" + UUID.randomUUID() + ".txt");
        try (OutputStream os = Files.newOutputStream(out, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            run("hello", "Joe")
                    .redirect(os)
                    .start()
                    .awaitTermination()
                    .assertSuccess();
            run("hello", "Dolly")
                    .redirect(os)
                    .start()
                    .awaitTermination()
                    .assertSuccess();
        }
        Assertions.assertThat(out).content(StandardCharsets.UTF_8)
                .matches("^Hello Joe\r?\nHello Dolly\r?\n$");

    }

    @Test
    void exitCode() throws IOException {

        {
            CommandResult result = run("hello", "Joe")
                    .hasLines("Hello Joe")
                    .exitCodeIsAnyOf(0)
                    .start()
                    .awaitTermination()
                    .assertSuccess();
            Assertions.assertThat(result.exitCode()).isEqualTo(0);
        }

        {
            CommandResult result = run("exitCode", "1")
                    .exitCodeIsAnyOf(1)
                    .start()
                    .awaitTermination()
                    .assertSuccess();
            Assertions.assertThat(result.exitCode()).isEqualTo(1);
        }
        {
            CommandResult result = run("exitCode", "1")
                    .start()
                    .awaitTermination();

            Assertions.assertThatThrownBy(result::assertSuccess).isInstanceOf(AssertionError.class)
                    .message().endsWith("Failure 1/1: Expected exit code 0 but actually terminated with exit code 1");

            Assertions.assertThat(result.exitCode()).isEqualTo(1);
        }

        {
            CommandResult result = run("exitCode", "1")
                    .exitCodeSatisfies(i -> i == 42, "Expected 42 but got ${actual}")
                    .start()
                    .awaitTermination();

            Assertions.assertThatThrownBy(result::assertSuccess).isInstanceOf(AssertionError.class)
                    .message().endsWith("Failure 1/1: Expected 42 but got 1");

            Assertions.assertThat(result.exitCode()).isEqualTo(1);
        }

    }

    @Test
    void byteCount() throws IOException {

        {
            CommandResult result = run("hello", "Joe")
                    .hasLines("Hello Joe")
                    .hasByteCount(cnt -> cnt == 10 || cnt == 11, "Expected 10 or 11 bytes but found ${actual} bytes")
                    .start()
                    .awaitTermination()
                    .assertSuccess();
            Assertions.assertThat(result.byteCountStdout()).isBetween(10L, 11L);
        }

        {
            CommandResult result = run("hello", "Joe")
                    .hasLines("Hello Joe")
                    .hasByteCount(20)
                    .start()
                    .awaitTermination();

            Assertions.assertThatThrownBy(result::assertSuccess).isInstanceOf(AssertionError.class)
                    .hasMessageMatching(
                            Pattern.compile(".*Failure 1/1: Expected 20 bytes in stdout but found 1[01] bytes",
                                    Pattern.DOTALL));

            Assertions.assertThat(result.byteCountStdout())
                    .isBetween(10L, 11L); // it is 10 on Linux and 11 on Windows
        }

        {
            CommandResult result = run("hello", "Joel")
                    .hasLines("Hello Joel")
                    .hasByteCount(cnt -> cnt > 20, "Expected bytes > 20 in ${stream} but found ${actual} bytes")
                    .start()
                    .awaitTermination();

            Assertions.assertThatThrownBy(result::assertSuccess).isInstanceOf(AssertionError.class)
                    .hasMessageMatching(
                            Pattern.compile(".*Failure 1/1: Expected bytes > 20 in stdout but found 1[1-2] bytes",
                                    Pattern.DOTALL));

            Assertions.assertThat(result.byteCountStdout()).isBetween(11L, 12L);
        }

    }

    @Test
    void log() throws IOException {

        List<String> lines = Collections.synchronizedList(new ArrayList<>());
        run("hello", "Joe")
                .log(lines::add)
                .start()
                .awaitTermination()
                .assertSuccess();
        Assertions.assertThat(lines).hasSize(1).contains("Hello Joe");

    }

    @Test
    void capture() throws IOException {
        StringBuilder expected = new StringBuilder("Failure 1/1: Expected lines\n"
                + "\n"
                + "    Foo\n"
                + "\n"
                + "to occur in stdout in any order, but none of them occurred\n"
                + "\n"
                + "stdout:\n");
        for (int i = 0; i < 35; i++) {
            expected.append("\n    Line ").append(i);
        }

        Assertions.assertThatThrownBy(run("outputLines", "35")
                .captureAll()
                .hasLines("Foo")
                .start()
                .awaitTermination()::assertSuccess).isInstanceOf(AssertionError.class)
                .hasMessageEndingWith(expected.toString());

        Assertions.assertThatThrownBy(run("outputLines", "35")
                .capture(3, 3)
                .hasLines("Foo")
                .start()
                .awaitTermination()::assertSuccess).isInstanceOf(AssertionError.class)
                .hasMessageEndingWith("Failure 1/1: Expected lines\n"
                        + "\n"
                        + "    Foo\n"
                        + "\n"
                        + "to occur in stdout in any order, but none of them occurred\n"
                        + "\n"
                        + "stdout:\n"
                        + "\n"
                        + "    Line 0\n"
                        + "    Line 1\n"
                        + "    Line 2\n"
                        + "    ...\n"
                        + "    [29 lines omitted; set stdout().capture(maxHeadLines, maxTailLines) or stdout().captureAll() to capure more lines]\n"
                        + "    ...\n"
                        + "    Line 32\n"
                        + "    Line 33\n"
                        + "    Line 34");

        Assertions.assertThatThrownBy(run("outputLines", "35")
                .capture(0, 0)
                .hasLines("Foo")
                .start()
                .awaitTermination()::assertSuccess).isInstanceOf(AssertionError.class)
                .hasMessageEndingWith("Failure 1/1: Expected lines\n"
                        + "\n"
                        + "    Foo\n"
                        + "\n"
                        + "to occur in stdout in any order, but none of them occurred\n"
                        + "\n"
                        + "stdout: <no lines captured>");

    }

    @Test
    void execute() throws IOException {
        Path cd = Paths.get("target/JavaTest-minimalExecute-" + UUID.randomUUID());
        Files.createDirectories(cd);
        command("write", "Hello minimalExecute", "hello.txt")
                .cd(cd)
                .execute()
                .assertSuccess();
        Assertions.assertThat(cd.resolve("hello.txt")).isRegularFile().hasContent("Hello minimalExecute");

        run("sleep", "500")
                .hasLines("About to sleep for 500 ms")
                .hasLineCount(1)
                .execute(200)
                .assertTimeout();

        run("sleep", "500")
                .hasLines("About to sleep for 500 ms")
                .hasLineCount(1)
                .execute(Duration.ofMillis(200))
                .assertTimeout();

    }

    @Test
    void awaitTermination() {

        run("hello", "Joe")
                .hasLines("Hello Joe")
                .start()
                .awaitTermination(Duration.ofMillis(10000))
                .assertSuccess();

        run("hello", "Joe")
                .hasLines("Hello Joe")
                .start()
                .awaitTermination(10000)
                .assertSuccess();

    }

    @Test
    void start() {
        Assertions.assertThatThrownBy(CliAssured.given()::start)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "The executable must be specified before starting the command process. You may want to call CommandSpec.executable(String) or CommandSpec.command(String, String...)");
    }

    @Test
    void expectExecute() throws IOException {
        Path cd = Paths.get("target/JavaTest-minimalExecute-" + UUID.randomUUID());
        Files.createDirectories(cd);
        command("write", "Hello minimalExecute", "hello.txt")
                .cd(cd)
                .then()
                .execute()
                .assertSuccess();
        Assertions.assertThat(cd.resolve("hello.txt")).isRegularFile().hasContent("Hello minimalExecute");
    }

    @Test
    void expectStdoutExecute() throws IOException {
        Path cd = Paths.get("target/JavaTest-minimalExecute-" + UUID.randomUUID());
        Files.createDirectories(cd);
        command("write", "Hello minimalExecute", "hello.txt")
                .cd(cd)
                .then()
                .stdout()
                .execute()
                .assertSuccess();
        Assertions.assertThat(cd.resolve("hello.txt")).isRegularFile().hasContent("Hello minimalExecute");
    }

    @Test
    void expectStderrExecute() throws IOException {
        Path cd = Paths.get("target/JavaTest-minimalExecute-" + UUID.randomUUID());
        Files.createDirectories(cd);
        command("write", "Hello minimalExecute", "hello.txt")
                .cd(cd)
                .then()
                .stderr()
                .execute()
                .assertSuccess();
        Assertions.assertThat(cd.resolve("hello.txt")).isRegularFile().hasContent("Hello minimalExecute");
    }

    static StreamExpectationsSpec run(String... args) {
        return command(args)
                .then()
                .stdout();
    }

    static StreamExpectationsSpec runErr(String... args) {
        return command(args)
                .then()
                .stderr();
    }

    public static CommandSpec command(String... args) {
        Path testAppJar = testAppJar();

        return CliAssured
                .java()
                .args("-cp", testAppJar.toString(), TestApp.class.getName())
                .args(args);
    }

    static Path testAppJar() {
        final String testAppArtifactId = "cli-assured-test-app";
        final String version = System.getProperty("project.version");
        Path testAppJar = Paths.get("../" + testAppArtifactId + "/target/" + testAppArtifactId + "-" + version + ".jar")
                .toAbsolutePath().normalize();
        if (!Files.isRegularFile(testAppJar)) {

            Path localRepo = Paths.get(System.getProperty("settings.localRepository"));
            Assertions.assertThat(localRepo).isDirectory();
            final String groupId = System.getProperty("project.groupId");
            final Path testAppJarMavenRepo = localRepo.resolve(groupId.replace('.', '/'))
                    .resolve(testAppArtifactId)
                    .resolve(version)
                    .resolve(testAppArtifactId + "-" + version + ".jar");
            if (!Files.isRegularFile(testAppJarMavenRepo)) {
                throw new IllegalStateException("Either " + testAppJar + " or " + testAppJarMavenRepo + " must exist");
            }
            testAppJar = testAppJarMavenRepo;
        }
        return testAppJar;
    }
}

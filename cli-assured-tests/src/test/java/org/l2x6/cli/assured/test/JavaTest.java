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
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.l2x6.cli.assured.CliAssured;
import org.l2x6.cli.assured.Command;
import org.l2x6.cli.assured.StreamExpectations;
import org.l2x6.cli.assured.test.app.TestApp;

public class JavaTest {

    @Test
    void stdout() {

        run("hello", "Joe")
                .contains("Hello Joe")
                .start()
                .awaitTermination()
                .assertSuccess();

        try {
            command("hello", "Joe")
                    .expect()
                    .stderr()
                    .contains("Hello Joe")
                    .start()
                    .awaitTermination()
                    .assertSuccess();
            Assertions.fail("AssertionError expected");
        } catch (AssertionError expected) {
        }

    }

    @Test
    void stderrToStdout() {

        command("helloErr", "Joe")
                .expect()
                .stderrToStdout()
                .stdout()
                .contains("Hello stderr Joe")
                .start()
                .awaitTermination()
                .assertSuccess();

        Assertions.assertThatThrownBy(command("helloErr", "Joe")
                .expect()
                .stderrToStdout()::stderr)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "You cannot set any assertions on stderr while you are redirecting stderr to stdout");

    }

    @Test
    void stderr() {

        command("helloErr", "Joe")
                .expect()
                .stderr()
                .contains("Hello stderr Joe")
                .start()
                .awaitTermination()
                .assertSuccess();

        try {
            run("helloErr", "Joe")
                    .contains("Hello stderr Joe")
                    .start()
                    .awaitTermination()
                    .assertSuccess();
            Assertions.fail("AssertionError expected");
        } catch (AssertionError expected) {
        }

    }

    @Test
    void timeout() {

        run("sleep", "500")
                .contains("About to sleep for 500 ms")
                .hasCount(1)
                .start()
                .awaitTermination(200)
                .assertTimeout();
    }

    @Test
    void hasLineContaining() {
        run("hello", "Joe")
                .containsSubstrings("lo J")
                .hasCount(1)
                .start()
                .awaitTermination()
                .assertSuccess();

        try {
            command("hello", "Joe")
                    .expect()
                    .stderr()
                    .containsSubstrings("lo J")
                    .start()
                    .awaitTermination()
                    .assertSuccess();
            Assertions.fail("AssertionError expected");
        } catch (AssertionError expected) {
        }

    }

    @Test
    void hasLineMatching() {
        run("hello", "Joe")
                .containsMatching("lo J.e")
                .hasCount(1)
                .start()
                .awaitTermination()
                .assertSuccess();

        try {
            command("hello", "Joe")
                    .expect()
                    .stderr()
                    .containsMatching("lo J.e")
                    .start()
                    .awaitTermination()
                    .assertSuccess();
            Assertions.fail("AssertionError expected");
        } catch (AssertionError expected) {
        }
    }

    @Test
    void doesNotHaveLine() {
        run("hello", "Joe")
                .doesNotContain("Hello John")
                .hasCount(1)
                .start()
                .awaitTermination()
                .assertSuccess();

        runErr("helloErr", "Joe")
                .doesNotContain("Hello John")
                .hasCount(1)
                .start()
                .awaitTermination()
                .assertSuccess();

        try {
            run("hello", "Joe")
                    .doesNotContain("Hello Joe")
                    .start()
                    .awaitTermination()
                    .assertSuccess();
            Assertions.fail("AssertionError expected");
        } catch (AssertionError expected) {
        }
        try {
            runErr("helloErr", "Joe")
                    .doesNotContain("Hello Joe")
                    .start()
                    .awaitTermination()
                    .assertSuccess();
            Assertions.fail("AssertionError expected");
        } catch (AssertionError expected) {
        }

    }

    @Test
    void doesNotHaveLineContaining() {
        run("hello", "Joe")
                .doesNotContainSubstrings("John")
                .hasCount(1)
                .start()
                .awaitTermination()
                .assertSuccess();

        try {
            run("hello", "Joe")
                    .doesNotContainSubstrings("Joe")
                    .start()
                    .awaitTermination()
                    .assertSuccess();
            Assertions.fail("AssertionError expected");
        } catch (AssertionError expected) {
        }

        try {
            runErr("helloErr", "Joe")
                    .doesNotContainSubstrings("Joe")
                    .start()
                    .awaitTermination()
                    .assertSuccess();
            Assertions.fail("AssertionError expected");
        } catch (AssertionError expected) {
        }
    }

    @Test
    void doesNotHaveLineMatching() {
        run("hello", "Joe")
                .doesNotContainMatching("Hello M.*")
                .hasCount(1)
                .start()
                .awaitTermination()
                .assertSuccess();

        try {
            run("hello", "Joe")
                    .doesNotContainMatching("lo Jo.*")
                    .start()
                    .awaitTermination()
                    .assertSuccess();
            ;
            Assertions.fail("AssertionError expected");
        } catch (AssertionError expected) {
        }

        try {
            runErr("helloErr", "Joe")
                    .doesNotContainMatching("lo Jo.*")
                    .start()
                    .awaitTermination()
                    .assertSuccess();
            ;
            Assertions.fail("AssertionError expected");
        } catch (AssertionError expected) {
        }

    }

    @Test
    void cd() throws IOException {
        Path cd = Paths.get("target/JavaTest-" + UUID.randomUUID());
        Files.createDirectories(cd);
        command("write", "Hello Dolly", "hello.txt")
                .cd(cd)
                .expect()
                .stdout()
                .hasCount(0)
                .stderr()
                .hasCount(0)
                .start()
                .awaitTermination()
                .assertSuccess();
        Assertions.assertThat(cd.resolve("hello.txt")).isRegularFile().hasContent("Hello Dolly");
    }

    @Test
    void redirect() {

        Path out = Paths.get("target/" + JavaTest.class.getSimpleName() + ".redirect-" + UUID.randomUUID() + ".txt");
        run("hello", "Joe")
                .redirect(out)
                .start()
                .awaitTermination()
                .assertSuccess();
        Assertions.assertThat(out).content(StandardCharsets.UTF_8).isEqualTo("Hello Joe\n");

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
        Assertions.assertThat(out).content(StandardCharsets.UTF_8).isEqualTo("Hello Joe\nHello Dolly\n");

    }

    static StreamExpectations.Builder run(String... args) {
        return command(args)
                .expect()
                .stdout();
    }

    static StreamExpectations.Builder runErr(String... args) {
        return command(args)
                .expect()
                .stderr();
    }

    static Command.Builder command(String... args) {
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

        return CliAssured
                .java()
                .args("-cp", testAppJar.toString(), TestApp.class.getName())
                .args(args);
    }
}

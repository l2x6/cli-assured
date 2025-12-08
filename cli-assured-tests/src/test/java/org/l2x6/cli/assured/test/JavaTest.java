/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.l2x6.cli.assured.CliAssured;
import org.l2x6.cli.assured.Command;
import org.l2x6.cli.assured.OutputLineAsserts;
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
                    .lines()
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
                .lines()
                .contains("Hello stderr Joe")
                .start()
                .awaitTermination()
                .assertSuccess();

    }

    @Test
    void stderr() {

        command("helloErr", "Joe")
                .expect()
                .stderr()
                .lines()
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
                    .lines()
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
                    .lines()
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
                .lines()
                .hasCount(0)
                .stderr()
                .lines()
                .hasCount(0)
                .start()
                .awaitTermination()
                .assertSuccess();
        Assertions.assertThat(cd.resolve("hello.txt")).isRegularFile().hasContent("Hello Dolly");
    }

    static OutputLineAsserts.Builder run(String... args) {
        return command(args)
                .expect()
                .stdout()
                .lines();
    }

    static OutputLineAsserts.Builder runErr(String... args) {
        return command(args)
                .expect()
                .stderr()
                .lines();
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

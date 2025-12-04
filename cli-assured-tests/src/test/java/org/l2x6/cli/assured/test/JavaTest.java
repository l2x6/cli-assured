/*
 * SPDX-FileCopyrightText: Copyright (c) 2025 CLI Assured contributors as indicated by the @author tags
 * SPDX-License-Identifier: Apache-2.0
 */
package org.l2x6.cli.assured.test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.l2x6.cli.assured.CliAssured;
import org.l2x6.cli.assured.Command;
import org.l2x6.cli.assured.CommandOutput.Stream;
import org.l2x6.cli.assured.CommandResult;
import org.l2x6.cli.assured.test.app.TestApp;

public class JavaTest {

    @Test
    void stdout() {

        run("hello", "Joe")
                .assertSuccess()
                .output()
                .hasLine("Hello Joe");

        run("hello", "Joe")
                .assertSuccess()
                .output()
                .hasLine(Stream.stdout, "Hello Joe");

        run("hello", "Joe")
                .assertSuccess()
                .output()
                .hasLine(Stream.any, "Hello Joe");

        try {
            run("hello", "Joe")
                    .assertSuccess()
                    .output()
                    .hasLine(Stream.stderr, "Hello Joe");
            Assertions.fail("AssertionError expected");
        } catch (AssertionError expected) {
        }

    }

    @Test
    void stderr() {

        run("helloErr", "Joe")
                .assertSuccess()
                .output()
                .hasLine("Hello stderr Joe");

        run("helloErr", "Joe")
                .assertSuccess()
                .output()
                .hasLine(Stream.stderr, "Hello stderr Joe");

        command("helloErr", "Joe")
                .stderrToStdout()
                .start()
                .awaitTermination()
                .assertSuccess()
                .output()
                .hasLine(Stream.stdout, "Hello stderr Joe");

        run("helloErr", "Joe")
                .assertSuccess()
                .output()
                .hasLine(Stream.any, "Hello stderr Joe");

        try {
            run("helloErr", "Joe")
                    .assertSuccess()
                    .output()
                    .hasLine(Stream.stdout, "Hello stderr Joe");
            Assertions.fail("AssertionError expected");
        } catch (AssertionError expected) {
        }

    }

    @Test
    void timeout() {

        command("sleep", "500")
                .start()
                .awaitTermination(200)
                .exitCode(-1)
                .assertTimeout()
                .output()
                .hasLine("About to sleep for 500 ms")
                .hasLineCount(1);
    }

    static CommandResult run(String... args) {
        return command(args)
                .awaitTermination();
    }

    static Command.Builder command(String... args) {
        final String testAppArtifactId = "cli-assured-test-app";
        final String version = System.getProperty("project.version");
        Path testAppJar = Paths.get("../" + testAppArtifactId + "/target/" + testAppArtifactId + "-" + version + ".jar");
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
